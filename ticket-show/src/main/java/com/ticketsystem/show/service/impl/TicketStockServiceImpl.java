package com.ticketsystem.show.service.impl;

import com.ticketsystem.common.exception.BusinessException;
import com.ticketsystem.show.entity.TicketStock;
import com.ticketsystem.show.mapper.TicketStockMapper;
import com.ticketsystem.show.service.RedisStockService;
import com.ticketsystem.show.service.TicketStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 票档库存服务实现类
 * 基于乐观锁机制实现高并发库存管理
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketStockServiceImpl implements TicketStockService {

    private final TicketStockMapper ticketStockMapper;
    private final RedisStockService redisStockService;
    
    // 乐观锁重试次数
    private static final int MAX_RETRY_TIMES = 3;
    // 重试间隔（毫秒）
    private static final long RETRY_INTERVAL = 50;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean lockStock(Long ticketId, Integer quantity) {
        if (quantity <= 0) {
            throw new BusinessException("锁定数量必须大于0");
        }

        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            try {
                // 获取当前库存信息
                TicketStock stock = ticketStockMapper.selectByTicketId(ticketId);
                if (stock == null) {
                    throw new BusinessException("票档库存信息不存在");
                }

                // 检查可用库存
                if (!stock.hasEnoughStock(quantity)) {
                    log.warn("库存不足，票档ID：{}，可用库存：{}，请求锁定：{}", 
                            ticketId, stock.getAvailableStock(), quantity);
                    return false;
                }

                // 使用乐观锁锁定库存
                int result = ticketStockMapper.lockStockWithOptimisticLock(
                        ticketId, quantity, stock.getVersion());
                
                if (result > 0) {
                    log.info("成功锁定库存，票档ID：{}，锁定数量：{}，重试次数：{}", 
                            ticketId, quantity, i);
                    return true;
                } else {
                    // 乐观锁冲突，准备重试
                    log.debug("乐观锁冲突，准备重试，票档ID：{}，重试次数：{}", ticketId, i + 1);
                    if (i < MAX_RETRY_TIMES - 1) {
                        Thread.sleep(RETRY_INTERVAL);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException("锁定库存被中断");
            } catch (Exception e) {
                log.error("锁定库存异常，票档ID：{}，重试次数：{}", ticketId, i, e);
                if (i == MAX_RETRY_TIMES - 1) {
                    throw e;
                }
            }
        }

        log.warn("锁定库存失败，已达到最大重试次数，票档ID：{}，锁定数量：{}", ticketId, quantity);
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean unlockStock(Long ticketId, Integer quantity) {
        if (quantity <= 0) {
            throw new BusinessException("释放数量必须大于0");
        }

        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            try {
                // 获取当前库存信息
                TicketStock stock = ticketStockMapper.selectByTicketId(ticketId);
                if (stock == null) {
                    throw new BusinessException("票档库存信息不存在");
                }

                // 检查锁定库存是否足够
                if (stock.getLockedStock() < quantity) {
                    log.warn("锁定库存不足，票档ID：{}，锁定库存：{}，请求释放：{}", 
                            ticketId, stock.getLockedStock(), quantity);
                    return false;
                }

                // 使用乐观锁释放库存
                int result = ticketStockMapper.unlockStockWithOptimisticLock(
                        ticketId, quantity, stock.getVersion());
                
                if (result > 0) {
                    log.info("成功释放库存，票档ID：{}，释放数量：{}，重试次数：{}", 
                            ticketId, quantity, i);
                    return true;
                } else {
                    // 乐观锁冲突，准备重试
                    log.debug("乐观锁冲突，准备重试，票档ID：{}，重试次数：{}", ticketId, i + 1);
                    if (i < MAX_RETRY_TIMES - 1) {
                        Thread.sleep(RETRY_INTERVAL);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException("释放库存被中断");
            } catch (Exception e) {
                log.error("释放库存异常，票档ID：{}，重试次数：{}", ticketId, i, e);
                if (i == MAX_RETRY_TIMES - 1) {
                    throw e;
                }
            }
        }

        log.warn("释放库存失败，已达到最大重试次数，票档ID：{}，释放数量：{}", ticketId, quantity);
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean confirmStock(Long ticketId, Integer quantity) {
        if (quantity <= 0) {
            throw new BusinessException("确认数量必须大于0");
        }

        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            try {
                // 获取当前库存信息
                TicketStock stock = ticketStockMapper.selectByTicketId(ticketId);
                if (stock == null) {
                    throw new BusinessException("票档库存信息不存在");
                }

                // 检查锁定库存是否足够
                if (stock.getLockedStock() < quantity) {
                    log.warn("锁定库存不足，票档ID：{}，锁定库存：{}，请求确认：{}", 
                            ticketId, stock.getLockedStock(), quantity);
                    return false;
                }

                // 使用乐观锁确认库存
                int result = ticketStockMapper.confirmStockWithOptimisticLock(
                        ticketId, quantity, stock.getVersion());
                
                if (result > 0) {
                    log.info("成功确认库存，票档ID：{}，确认数量：{}，重试次数：{}", 
                            ticketId, quantity, i);
                    return true;
                } else {
                    // 乐观锁冲突，准备重试
                    log.debug("乐观锁冲突，准备重试，票档ID：{}，重试次数：{}", ticketId, i + 1);
                    if (i < MAX_RETRY_TIMES - 1) {
                        Thread.sleep(RETRY_INTERVAL);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException("确认库存被中断");
            } catch (Exception e) {
                log.error("确认库存异常，票档ID：{}，重试次数：{}", ticketId, i, e);
                if (i == MAX_RETRY_TIMES - 1) {
                    throw e;
                }
            }
        }

        log.warn("确认库存失败，已达到最大重试次数，票档ID：{}，确认数量：{}", ticketId, quantity);
        return false;
    }

    @Override
    public TicketStock getStockInfo(Long ticketId) {
        try {
            TicketStock stock = ticketStockMapper.selectByTicketId(ticketId);
            if (stock == null) {
                log.warn("票档库存信息不存在，票档ID：{}", ticketId);
                // 返回null而不是抛异常，让调用方决定如何处理
                return null;
            }
            log.debug("获取票档库存信息成功，票档ID：{}，总库存：{}，锁定库存：{}，已售库存：{}", 
                    ticketId, stock.getTotalStock(), stock.getLockedStock(), stock.getSoldStock());
            return stock;
        } catch (Exception e) {
            log.error("获取票档库存信息异常，票档ID：{}，错误：{}", ticketId, e.getMessage(), e);
            // 返回null而不是抛异常，提高系统健壮性
            return null;
        }
    }

    @Override
    public Integer getAvailableStock(Long ticketId) {
        Integer availableStock = ticketStockMapper.getAvailableStock(ticketId);
        return availableStock != null ? availableStock : 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean initializeStock(Long ticketId, Integer totalStock) {
        try {
            // 添加存在性检查，防止重复初始化
            TicketStock existingStock = ticketStockMapper.selectByTicketId(ticketId);
            if (existingStock != null) {
                log.warn("票档库存已存在，跳过初始化，票档ID：{}，现有总库存：{}", ticketId, existingStock.getTotalStock());
                return true; // 已存在则返回成功，避免重复初始化
            }
            
            // 添加调试日志
            log.info("=== TicketStockService 初始化库存调试 ===");
            log.info("准备执行SQL，票档ID: {}, 总库存: {}", ticketId, totalStock);
            
            int result = ticketStockMapper.initializeStock(ticketId, totalStock);
            
            log.info("SQL执行结果: {}", result);
            log.info("============================================");
            
            if (result > 0) {
                log.info("成功初始化票档库存，票档ID：{}，总库存：{}", ticketId, totalStock);
                return true;
            } else {
                log.warn("初始化票档库存失败，票档ID：{}，总库存：{}", ticketId, totalStock);
                return false;
            }
        } catch (Exception e) {
            log.error("初始化票档库存异常，票档ID：{}，总库存：{}，错误：{}", ticketId, totalStock, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public Integer predeductStockFromRedis(Long ticketId, Integer quantity) {
        if (quantity <= 0) {
            throw new BusinessException("扣减数量必须大于0");
        }
        
        // 先尝试从Redis预减
        Integer result = redisStockService.predeductStock(ticketId, quantity);
        
        // 如果Redis中不存在库存信息，从数据库同步
        if (result == -1) {
            log.info("Redis中不存在库存信息，从数据库同步，票档ID：{}", ticketId);
            if (syncStockToRedis(ticketId)) {
                // 同步成功后重新尝试预减
                result = redisStockService.predeductStock(ticketId, quantity);
            }
        }
        
        log.info("Redis预减库存结果，票档ID：{}，扣减数量：{}，结果：{}", ticketId, quantity, result);
        return result;
    }
    
    @Override
    public Boolean rollbackStockToRedis(Long ticketId, Integer quantity) {
        if (quantity <= 0) {
            throw new BusinessException("回滚数量必须大于0");
        }
        
        // 获取数据库中的总库存作为最大限制
        TicketStock stock = ticketStockMapper.selectByTicketId(ticketId);
        if (stock == null) {
            log.warn("票档库存信息不存在，无法回滚，票档ID：{}", ticketId);
            return false;
        }
        
        Integer maxStock = stock.getTotalStock();
        Integer result = redisStockService.rollbackStock(ticketId, quantity, maxStock);
        
        boolean success = result != null && result == 1;
        log.info("Redis库存回滚结果，票档ID：{}，回滚数量：{}，结果：{}", ticketId, quantity, success);
        return success;
    }
    
    @Override
    public Boolean syncStockToRedis(Long ticketId) {
        try {
            // 从数据库获取当前库存信息
            TicketStock stock = ticketStockMapper.selectByTicketId(ticketId);
            if (stock == null) {
                log.warn("票档库存信息不存在，无法同步到Redis，票档ID：{}", ticketId);
                return false;
            }
            
            // 计算可用库存
            Integer availableStock = stock.getAvailableStock();
            
            // 同步到Redis（强制更新）
            Boolean result = redisStockService.initStock(ticketId, availableStock, true);
            
            log.info("同步库存到Redis，票档ID：{}，可用库存：{}，结果：{}", ticketId, availableStock, result);
            return result;
        } catch (Exception e) {
            log.error("同步库存到Redis异常，票档ID：{}", ticketId, e);
            return false;
        }
    }

    @Override
    public Boolean confirmStockFromDatabase(Long ticketId, Integer quantity) {
        log.info("开始数据库库存确认，票档ID：{}，确认数量：{}", ticketId, quantity);
        
        // 直接调用现有的confirmStock方法，它已经实现了乐观锁机制
        // 这里不再捕获异常，而是直接向上抛出，以便分布式事务能够感知并回滚
        // 如果confirmStock内部的重试机制失败，它会抛出异常，这是我们期望的行为
        Boolean result = confirmStock(ticketId, quantity);
        
        if (result) {
            log.info("数据库库存确认成功，票档ID：{}，确认数量：{}", ticketId, quantity);
        } else {
            log.warn("数据库库存确认失败，票档ID：{}，确认数量：{}", ticketId, quantity);
        }
        
        return result;
    }
}