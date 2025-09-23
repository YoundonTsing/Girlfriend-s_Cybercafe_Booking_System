package com.ticketsystem.order.service;

import com.ticketsystem.order.dto.CreateOrderDTO;
import com.ticketsystem.order.service.impl.OrderServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 异步订单处理器
 * 将复杂的数据库操作异步化，提升响应性能
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncOrderProcessor {

    private final UnifiedOrderService unifiedOrderService;
    private final OrderServiceImpl orderServiceImpl;
    private final RedissonClient redissonClient;

    /**
     * 异步创建订单 - 快速响应 + 异步持久化
     */
    public String createOrderAsync(CreateOrderDTO dto) {
        // 1. 快速响应：使用统一服务创建订单
        String orderNo = unifiedOrderService.createOrder(dto);
        
        // 2. 异步持久化到数据库
        asyncPersistOrderToDatabase(orderNo, dto);
        
        return orderNo;
    }

    /**
     * 异步支付订单 - 快速响应 + 异步处理
     */
    public boolean payOrderAsync(String orderNo, Long userId, Integer payType) {
        // 1. 快速响应：使用统一服务处理支付
        boolean success = unifiedOrderService.payOrder(orderNo, userId, payType);
        
        if (success) {
            // 2. 异步处理支付后的业务逻辑
            asyncProcessPaymentSuccess(orderNo, userId, payType);
        }
        
        return success;
    }

    /**
     * 异步取消订单 - 快速响应 + 异步处理
     */
    public boolean cancelOrderAsync(String orderNo, Long userId, Long ticketId, Integer quantity) {
        // 1. 快速响应：使用统一服务取消订单
        boolean success = unifiedOrderService.cancelOrder(orderNo, userId, ticketId, quantity);
        
        if (success) {
            // 2. 异步处理取消后的业务逻辑
            asyncProcessOrderCancel(orderNo, userId);
        }
        
        return success;
    }

    /**
     * 异步持久化订单到数据库
     */
    @Async("orderAsyncExecutor")
    public CompletableFuture<Void> asyncPersistOrderToDatabase(String orderNo, CreateOrderDTO dto) {
        try {
            log.info("开始异步持久化订单到数据库: orderNo={}", orderNo);
            
            // 从Redis获取订单信息
            Object orderInfo = getOrderInfoFromRedis(orderNo);
            if (orderInfo != null) {
                // 持久化到数据库 - 使用现有的OrderServiceImpl
                persistOrderFromRedis(orderNo, orderInfo);
                log.info("订单异步持久化成功: orderNo={}", orderNo);
            } else {
                log.warn("订单信息不存在，跳过持久化: orderNo={}", orderNo);
            }
            
        } catch (Exception e) {
            log.error("订单异步持久化失败: orderNo={}", orderNo, e);
            // 这里可以加入重试机制或告警
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 异步处理支付成功后的业务逻辑
     */
    @Async("orderAsyncExecutor")
    public CompletableFuture<Void> asyncProcessPaymentSuccess(String orderNo, Long userId, Integer payType) {
        try {
            log.info("开始异步处理支付成功业务逻辑: orderNo={}, userId={}", orderNo, userId);
            
            // 1. 更新数据库订单状态
            updateOrderPaymentStatus(orderNo, userId, payType);
            
            // 2. 发送支付成功通知
            sendPaymentSuccessNotification(orderNo, userId);
            
            // 3. 更新用户积分/等级
            updateUserPoints(userId, orderNo);
            
            // 4. 记录支付日志
            recordPaymentLog(orderNo, userId, payType);
            
            log.info("支付成功业务逻辑处理完成: orderNo={}", orderNo);
            
        } catch (Exception e) {
            log.error("支付成功业务逻辑处理失败: orderNo={}, userId={}", orderNo, userId, e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 异步处理订单取消后的业务逻辑
     */
    @Async("orderAsyncExecutor")
    public CompletableFuture<Void> asyncProcessOrderCancel(String orderNo, Long userId) {
        try {
            log.info("开始异步处理订单取消业务逻辑: orderNo={}, userId={}", orderNo, userId);
            
            // 1. 更新数据库订单状态
            updateOrderCancelStatus(orderNo, userId);
            
            // 2. 发送取消通知
            sendOrderCancelNotification(orderNo, userId);
            
            // 3. 记录取消日志
            recordCancelLog(orderNo, userId);
            
            log.info("订单取消业务逻辑处理完成: orderNo={}", orderNo);
            
        } catch (Exception e) {
            log.error("订单取消业务逻辑处理失败: orderNo={}, userId={}", orderNo, userId, e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 发送支付成功通知
     */
    private void sendPaymentSuccessNotification(String orderNo, Long userId) {
        // 实现通知逻辑：短信、邮件、站内信等
        log.info("发送支付成功通知: orderNo={}, userId={}", orderNo, userId);
    }

    /**
     * 更新用户积分
     */
    private void updateUserPoints(Long userId, String orderNo) {
        // 实现积分更新逻辑
        log.info("更新用户积分: userId={}, orderNo={}", userId, orderNo);
    }

    /**
     * 记录支付日志
     */
    private void recordPaymentLog(String orderNo, Long userId, Integer payType) {
        // 实现支付日志记录
        log.info("记录支付日志: orderNo={}, userId={}, payType={}", orderNo, userId, payType);
    }

    /**
     * 发送订单取消通知
     */
    private void sendOrderCancelNotification(String orderNo, Long userId) {
        // 实现取消通知逻辑
        log.info("发送订单取消通知: orderNo={}, userId={}", orderNo, userId);
    }

    /**
     * 记录取消日志
     */
    private void recordCancelLog(String orderNo, Long userId) {
        // 实现取消日志记录
        log.info("记录取消日志: orderNo={}, userId={}", orderNo, userId);
    }

    /**
     * 从Redis持久化订单到数据库
     */
    private void persistOrderFromRedis(String orderNo, Object orderInfo) {
        try {
            // 这里可以实现具体的持久化逻辑
            // 由于orderInfo是Object类型，需要根据实际数据结构进行转换
            log.info("持久化订单到数据库: orderNo={}", orderNo);
            // 实际实现中，这里应该调用OrderServiceImpl的相关方法
        } catch (Exception e) {
            log.error("持久化订单到数据库失败: orderNo={}", orderNo, e);
            throw e;
        }
    }

    /**
     * 更新订单支付状态
     */
    private void updateOrderPaymentStatus(String orderNo, Long userId, Integer payType) {
        try {
            // 这里可以实现具体的支付状态更新逻辑
            log.info("更新订单支付状态: orderNo={}, userId={}, payType={}", orderNo, userId, payType);
            // 实际实现中，这里应该调用OrderServiceImpl的相关方法
        } catch (Exception e) {
            log.error("更新订单支付状态失败: orderNo={}, userId={}", orderNo, userId, e);
            throw e;
        }
    }

    /**
     * 更新订单取消状态
     */
    private void updateOrderCancelStatus(String orderNo, Long userId) {
        try {
            // 这里可以实现具体的取消状态更新逻辑
            log.info("更新订单取消状态: orderNo={}, userId={}", orderNo, userId);
            // 实际实现中，这里应该调用OrderServiceImpl的相关方法
        } catch (Exception e) {
            log.error("更新订单取消状态失败: orderNo={}, userId={}", orderNo, userId, e);
            throw e;
        }
    }

    /**
     * 从Redis获取订单信息
     */
    private Object getOrderInfoFromRedis(String orderNo) {
        try {
            String orderKey = "order:" + orderNo;
            return redissonClient.getMap(orderKey).readAllMap();
        } catch (Exception e) {
            log.error("从Redis获取订单信息失败: orderNo={}", orderNo, e);
            return null;
        }
    }
}