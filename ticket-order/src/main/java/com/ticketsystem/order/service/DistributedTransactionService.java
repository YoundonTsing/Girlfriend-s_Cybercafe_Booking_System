package com.ticketsystem.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticketsystem.order.entity.Order;
import com.ticketsystem.order.mapper.OrderMapper;
import com.ticketsystem.order.feign.ShowFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 分布式事务管理服务
 * 确保订单和库存操作的最终一致性
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedTransactionService {

    private final RedissonClient redissonClient;
    private final ShowFeignClient showFeignClient;
    private final OrderMapper orderMapper;
    private final CompensationService compensationService;
    
    private static final String TRANSACTION_LOCK_PREFIX = "tx:lock:";
    private static final int LOCK_WAIT_TIME = 10; // 秒
    private static final int LOCK_LEASE_TIME = 30; // 秒

    /**
     * 执行分布式事务：创建订单并扣减库存
     * @param order 订单信息
     * @param ticketId 票档ID
     * @param quantity 数量
     * @return 事务执行结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean executeOrderTransaction(Order order, Long ticketId, Integer quantity) {
        String lockKey = TRANSACTION_LOCK_PREFIX + "order:" + order.getOrderNo();
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 获取分布式锁
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                log.error("获取分布式事务锁失败，订单号：{}", order.getOrderNo());
                return false;
            }
            
            log.info("开始执行分布式事务，订单号：{}，票档ID：{}，数量：{}", 
                order.getOrderNo(), ticketId, quantity);
            
            // 1. 预减Redis库存
            boolean stockPrededucted = false;
            try {
                var result = showFeignClient.predeductStockFromRedis(ticketId, quantity);
                if (result.getCode().equals(200) && Integer.valueOf(1).equals(result.getData())) {
                    stockPrededucted = true;
                    log.info("Redis库存预减成功，订单号：{}，票档ID：{}，数量：{}", 
                        order.getOrderNo(), ticketId, quantity);
                } else {
                    log.error("Redis库存预减失败，订单号：{}，票档ID：{}，数量：{}，结果：{}", 
                        order.getOrderNo(), ticketId, quantity, result.getData());
                    return false;
                }
            } catch (Exception e) {
                log.error("Redis库存预减异常，订单号：{}，票档ID：{}，数量：{}", 
                    order.getOrderNo(), ticketId, quantity, e);
                return false;
            }
            
            // 2. 保存订单到数据库
            try {
                order.setCreateTime(LocalDateTime.now());
                order.setUpdateTime(LocalDateTime.now());
                orderMapper.insert(order);
                log.info("订单保存成功，订单号：{}", order.getOrderNo());
            } catch (Exception e) {
                log.error("订单保存失败，订单号：{}", order.getOrderNo(), e);
                // 回滚Redis库存
                rollbackRedisStock(ticketId, quantity, order.getOrderNo());
                throw e;
            }
            
            // 3. 注册事务同步回调，确保最终一致性
            registerTransactionSynchronization(order, ticketId, quantity, stockPrededucted);
            
            return true;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("分布式事务执行被中断，订单号：{}", order.getOrderNo(), e);
            return false;
        } catch (Exception e) {
            log.error("分布式事务执行异常，订单号：{}", order.getOrderNo(), e);
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("释放分布式事务锁，订单号：{}", order.getOrderNo());
            }
        }
    }
    
    /**
     * 执行分布式事务：取消订单并回滚库存
     * @param orderNo 订单号
     * @return 事务执行结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean executeCancelTransaction(String orderNo) {
        String lockKey = TRANSACTION_LOCK_PREFIX + "cancel:" + orderNo;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 获取分布式锁
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                log.error("获取分布式取消事务锁失败，订单号：{}", orderNo);
                return false;
            }
            
            log.info("开始执行分布式取消事务，订单号：{}", orderNo);
            
            // 1. 查询订单信息
            LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Order::getOrderNo, orderNo);
            Order order = orderMapper.selectOne(queryWrapper);
            if (order == null) {
                log.error("订单不存在，订单号：{}", orderNo);
                return false;
            }
            
            // 2. 更新订单状态为已取消
            try {
                order.setStatus(2); // 2-已取消
                order.setUpdateTime(LocalDateTime.now());
                orderMapper.updateById(order);
                log.info("订单状态更新成功，订单号：{}，状态：已取消", orderNo);
            } catch (Exception e) {
                log.error("订单状态更新失败，订单号：{}", orderNo, e);
                throw e;
            }
            
            // 3. 回滚库存
            boolean stockRollbacked = false;
            try {
                var result = showFeignClient.rollbackStockToRedis(order.getTicketId(), order.getQuantity());
                if (result.getCode().equals(200) && Boolean.TRUE.equals(result.getData())) {
                    stockRollbacked = true;
                    log.info("库存回滚成功，订单号：{}，票档ID：{}，数量：{}", 
                        orderNo, order.getTicketId(), order.getQuantity());
                } else {
                    log.error("库存回滚失败，订单号：{}，票档ID：{}，数量：{}，结果：{}", 
                        orderNo, order.getTicketId(), order.getQuantity(), result.getData());
                    // 启动异步补偿
                    compensationService.asyncCompensateStockRollback(
                        order.getTicketId(), order.getQuantity(), orderNo);
                }
            } catch (Exception e) {
                log.error("库存回滚异常，订单号：{}，票档ID：{}，数量：{}", 
                    orderNo, order.getTicketId(), order.getQuantity(), e);
                // 启动异步补偿
                compensationService.asyncCompensateStockRollback(
                    order.getTicketId(), order.getQuantity(), orderNo);
            }
            
            // 4. 注册事务同步回调
            registerCancelTransactionSynchronization(order, stockRollbacked);
            
            return true;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("分布式取消事务执行被中断，订单号：{}", orderNo, e);
            return false;
        } catch (Exception e) {
            log.error("分布式取消事务执行异常，订单号：{}", orderNo, e);
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("释放分布式取消事务锁，订单号：{}", orderNo);
            }
        }
    }
    
    /**
     * 注册事务同步回调
     */
    private void registerTransactionSynchronization(Order order, Long ticketId, Integer quantity, boolean stockPrededucted) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("订单事务提交成功，订单号：{}", order.getOrderNo());
                    // 异步验证最终一致性
                    CompletableFuture.runAsync(() -> {
                        try {
                            Thread.sleep(5000); // 等待5秒后验证
                            verifyTransactionConsistency(order.getOrderNo(), ticketId, quantity);
                        } catch (Exception e) {
                            log.error("验证事务一致性异常，订单号：{}", order.getOrderNo(), e);
                        }
                    });
                }
                
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        log.warn("订单事务回滚，订单号：{}", order.getOrderNo());
                        if (stockPrededucted) {
                            // 异步回滚Redis库存
                            CompletableFuture.runAsync(() -> {
                                rollbackRedisStock(ticketId, quantity, order.getOrderNo());
                            });
                        }
                    }
                }
            });
        }
    }
    
    /**
     * 注册取消事务同步回调
     */
    private void registerCancelTransactionSynchronization(Order order, boolean stockRollbacked) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("取消事务提交成功，订单号：{}", order.getOrderNo());
                    // 异步验证最终一致性
                    CompletableFuture.runAsync(() -> {
                        try {
                            Thread.sleep(3000); // 等待3秒后验证
                            verifyCancelConsistency(order.getOrderNo(), order.getTicketId(), order.getQuantity());
                        } catch (Exception e) {
                            log.error("验证取消一致性异常，订单号：{}", order.getOrderNo(), e);
                        }
                    });
                }
                
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        log.warn("取消事务回滚，订单号：{}", order.getOrderNo());
                        // 如果库存已回滚，需要重新扣减
                        if (stockRollbacked) {
                            CompletableFuture.runAsync(() -> {
                                try {
                                    showFeignClient.predeductStockFromRedis(order.getTicketId(), order.getQuantity());
                                    log.info("取消事务回滚后重新扣减库存，订单号：{}", order.getOrderNo());
                                } catch (Exception e) {
                                    log.error("取消事务回滚后重新扣减库存失败，订单号：{}", order.getOrderNo(), e);
                                    compensationService.recordCompensationFailure(order.getOrderNo(), 
                                        order.getTicketId(), order.getQuantity(), 
                                        "REDEDUCT_AFTER_CANCEL_ROLLBACK", e.getMessage());
                                }
                            });
                        }
                    }
                }
            });
        }
    }
    
    /**
     * 回滚Redis库存
     */
    private void rollbackRedisStock(Long ticketId, Integer quantity, String orderNo) {
        try {
            var result = showFeignClient.rollbackStockToRedis(ticketId, quantity);
            if (result.getCode().equals(200) && Boolean.TRUE.equals(result.getData())) {
                log.info("Redis库存回滚成功，票档ID：{}，数量：{}，订单号：{}", ticketId, quantity, orderNo);
            } else {
                log.error("Redis库存回滚失败，票档ID：{}，数量：{}，订单号：{}，结果：{}", 
                    ticketId, quantity, orderNo, result.getData());
                compensationService.recordCompensationFailure(orderNo, ticketId, quantity, 
                    "ROLLBACK_REDIS_STOCK", "回滚失败：" + result.getMessage());
            }
        } catch (Exception e) {
            log.error("Redis库存回滚异常，票档ID：{}，数量：{}，订单号：{}", ticketId, quantity, orderNo, e);
            compensationService.recordCompensationFailure(orderNo, ticketId, quantity, 
                "ROLLBACK_REDIS_STOCK_EXCEPTION", e.getMessage());
        }
    }
    
    /**
     * 验证事务一致性
     */
    private void verifyTransactionConsistency(String orderNo, Long ticketId, Integer quantity) {
        try {
            // 检查订单是否存在
            LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Order::getOrderNo, orderNo);
            Order order = orderMapper.selectOne(queryWrapper);
            if (order == null) {
                log.error("一致性验证失败：订单不存在，订单号：{}", orderNo);
                compensationService.recordCompensationFailure(orderNo, ticketId, quantity, 
                    "CONSISTENCY_CHECK_ORDER_MISSING", "订单不存在");
                return;
            }
            
            // 检查库存状态（这里可以添加更多检查逻辑）
            log.info("事务一致性验证通过，订单号：{}", orderNo);
            
        } catch (Exception e) {
            log.error("验证事务一致性异常，订单号：{}", orderNo, e);
            compensationService.recordCompensationFailure(orderNo, ticketId, quantity, 
                "CONSISTENCY_CHECK_EXCEPTION", e.getMessage());
        }
    }
    
    /**
     * 验证取消一致性
     */
    private void verifyCancelConsistency(String orderNo, Long ticketId, Integer quantity) {
        try {
            // 检查订单状态
            LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Order::getOrderNo, orderNo);
            Order order = orderMapper.selectOne(queryWrapper);
            if (order == null || !Integer.valueOf(2).equals(order.getStatus())) {
                log.error("取消一致性验证失败：订单状态异常，订单号：{}，状态：{}", 
                    orderNo, order != null ? order.getStatus() : "null");
                compensationService.recordCompensationFailure(orderNo, ticketId, quantity, 
                    "CANCEL_CONSISTENCY_CHECK_FAILED", "订单状态异常");
                return;
            }
            
            log.info("取消一致性验证通过，订单号：{}", orderNo);
            
        } catch (Exception e) {
            log.error("验证取消一致性异常，订单号：{}", orderNo, e);
            compensationService.recordCompensationFailure(orderNo, ticketId, quantity, 
                "CANCEL_CONSISTENCY_CHECK_EXCEPTION", e.getMessage());
        }
    }
}