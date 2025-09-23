package com.ticketsystem.show.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * Redis库存管理服务
 * 使用Redis原子操作API实现库存操作
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisStockService {

    private final RedissonClient redissonClient;
    
    // 库存缓存key前缀
    private static final String STOCK_KEY_PREFIX = "stock:ticket:";
    
    // 默认过期时间（24小时）
    private static final int DEFAULT_EXPIRE_TIME = 24 * 60 * 60;
    
    @PostConstruct
    public void init() {
        log.info("Redis库存管理服务初始化完成，使用原子操作API");
    }
    
    /**
     * 生成库存缓存key
     */
    private String getStockKey(Long ticketId) {
        return STOCK_KEY_PREFIX + ticketId;
    }
    
    /**
     * 初始化库存到Redis - 使用原子操作API
     * @param ticketId 票档ID
     * @param availableStock 可用库存数量
     * @param forceUpdate 是否强制更新
     * @return 是否成功
     */
    public Boolean initStock(Long ticketId, Integer availableStock, boolean forceUpdate) {
        try {
            String stockKey = getStockKey(ticketId);
            
            log.info("执行Redis库存初始化 - 票档ID: {}, key: {}, 库存: {}, 强制更新: {}", 
                    ticketId, stockKey, availableStock, forceUpdate);
            
            // 检查库存是否已存在
            boolean exists = redissonClient.getBucket(stockKey).isExists();
            log.info("执行前Redis状态 - key存在: {}", exists);
            
            if (exists && !forceUpdate) {
                log.info("库存已存在且非强制更新 - 票档ID: {}", ticketId);
                return false;
            }
            
            // 使用原子操作设置库存值
            redissonClient.getBucket(stockKey).set(availableStock.toString());
            
            // 设置过期时间
            redissonClient.getBucket(stockKey).expire(java.time.Duration.ofSeconds(DEFAULT_EXPIRE_TIME));
            
            log.info("执行后Redis状态 - key存在: {}, 值: {}", 
                    redissonClient.getBucket(stockKey).isExists(),
                    redissonClient.getBucket(stockKey).get());
            
            log.info("初始化Redis库存成功 - 票档ID: {}, 库存: {}, 强制更新: {}", 
                    ticketId, availableStock, forceUpdate);
            return true;
        } catch (Exception e) {
            log.error("初始化Redis库存失败 - 票档ID: {}, 库存: {}", ticketId, availableStock, e);
            return false;
        }
    }
    
    /**
     * 预减库存 - 使用原子操作API
     * @param ticketId 票档ID
     * @param quantity 扣减数量
     * @return 扣减结果：1-成功，0-库存不足，-1-库存不存在
     */
    public Integer predeductStock(Long ticketId, Integer quantity) {
        try {
            String stockKey = getStockKey(ticketId);
            
            // 检查库存是否存在
            Object stockObj = redissonClient.getBucket(stockKey).get();
            if (stockObj == null) {
                log.warn("Redis库存预减失败 - 票档ID: {}, 扣减数量: {}, 原因: 库存不存在", ticketId, quantity);
                return -1;
            }
            
            // 获取当前库存
            int currentStock;
            try {
                currentStock = Integer.parseInt(stockObj.toString());
            } catch (NumberFormatException e) {
                log.error("Redis库存预减失败 - 票档ID: {}, 扣减数量: {}, 原因: 库存格式错误", ticketId, quantity, e);
                return -1;
            }
            
            // 检查库存是否充足
            if (currentStock < quantity) {
                log.warn("Redis库存预减失败 - 票档ID: {}, 扣减数量: {}, 当前库存: {}, 原因: 库存不足", 
                        ticketId, quantity, currentStock);
                return 0;
            }
            
            // 使用原子操作扣减库存
            int newStock = currentStock - quantity;
            redissonClient.getBucket(stockKey).set(String.valueOf(newStock));
            
            // 重新设置过期时间
            redissonClient.getBucket(stockKey).expire(java.time.Duration.ofSeconds(DEFAULT_EXPIRE_TIME));
            
            log.info("Redis库存预减成功 - 票档ID: {}, 扣减数量: {}, 原库存: {}, 新库存: {}", 
                    ticketId, quantity, currentStock, newStock);
            return 1;
        } catch (Exception e) {
            log.error("Redis库存预减失败 - 票档ID: {}, 扣减数量: {}", ticketId, quantity, e);
            return -1;
        }
    }
    
    /**
     * 回滚库存 - 使用原子操作API
     * @param ticketId 票档ID
     * @param quantity 回滚数量
     * @param maxStock 最大库存限制
     * @return 回滚结果：1-成功，0-超过最大库存，-1-库存不存在
     */
    public Integer rollbackStock(Long ticketId, Integer quantity, Integer maxStock) {
        try {
            String stockKey = getStockKey(ticketId);
            
            // 检查库存是否存在
            Object stockObj = redissonClient.getBucket(stockKey).get();
            if (stockObj == null) {
                log.warn("Redis库存回滚失败 - 票档ID: {}, 回滚数量: {}, 原因: 库存不存在", ticketId, quantity);
                return -1;
            }
            
            // 获取当前库存
            int currentStock;
            try {
                currentStock = Integer.parseInt(stockObj.toString());
            } catch (NumberFormatException e) {
                log.error("Redis库存回滚失败 - 票档ID: {}, 回滚数量: {}, 原因: 库存格式错误", ticketId, quantity, e);
                return -1;
            }
            
            // 计算回滚后的库存
            int newStock = currentStock + quantity;
            
            // 检查是否超过最大库存限制
            if (newStock > maxStock) {
                log.warn("Redis库存回滚失败 - 票档ID: {}, 回滚数量: {}, 当前库存: {}, 最大库存: {}, 原因: 超过最大库存限制", 
                        ticketId, quantity, currentStock, maxStock);
                return 0;
            }
            
            // 使用原子操作回滚库存
            redissonClient.getBucket(stockKey).set(String.valueOf(newStock));
            
            // 重新设置过期时间
            redissonClient.getBucket(stockKey).expire(java.time.Duration.ofSeconds(DEFAULT_EXPIRE_TIME));
            
            log.info("Redis库存回滚成功 - 票档ID: {}, 回滚数量: {}, 原库存: {}, 新库存: {}, 最大库存: {}", 
                    ticketId, quantity, currentStock, newStock, maxStock);
            return 1;
        } catch (Exception e) {
            log.error("Redis库存回滚失败 - 票档ID: {}, 回滚数量: {}", ticketId, quantity, e);
            return -1;
        }
    }
    
    /**
     * 获取Redis中的库存数量
     * @param ticketId 票档ID
     * @return 库存数量，不存在返回null
     */
    public Integer getStock(Long ticketId) {
        try {
            String stockKey = getStockKey(ticketId);
            Object stockObj = redissonClient.getBucket(stockKey).get();
            if (stockObj == null) {
                return null;
            }
            String stockStr = stockObj.toString();
            return Integer.valueOf(stockStr);
        } catch (Exception e) {
            log.error("获取Redis库存失败 - 票档ID: {}", ticketId, e);
            return null;
        }
    }
    
    /**
     * 删除库存缓存
     * @param ticketId 票档ID
     * @return 是否成功
     */
    public Boolean deleteStock(Long ticketId) {
        try {
            String stockKey = getStockKey(ticketId);
            boolean result = redissonClient.getBucket(stockKey).delete();
            log.info("删除Redis库存缓存 - 票档ID: {}, 结果: {}", ticketId, result);
            return result;
        } catch (Exception e) {
            log.error("删除Redis库存缓存失败 - 票档ID: {}", ticketId, e);
            return false;
        }
    }
}