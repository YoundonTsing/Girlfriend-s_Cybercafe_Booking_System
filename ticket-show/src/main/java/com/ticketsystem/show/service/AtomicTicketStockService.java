package com.ticketsystem.show.service;

import com.ticketsystem.show.mapper.TicketStockMapper;
import com.ticketsystem.show.entity.TicketStock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 原子性票档库存服务
 * 使用Redisson+Lua实现高性能库存操作
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AtomicTicketStockService {

    private final RedissonClient redissonClient;
    private final TicketStockMapper ticketStockMapper;

    // 库存键前缀
    private static final String STOCK_KEY_PREFIX = "ticket_stock:";
    
    // 锁定库存键前缀
    private static final String LOCKED_STOCK_KEY_PREFIX = "ticket_locked:";

    /**
     * 原子性库存扣减Lua脚本
     * 注意：所有参数都转换为字符串，确保类型一致性
     */
    private static final String STOCK_DEDUCT_SCRIPT = 
            "local stockKey = 'ticket_stock:' .. tostring(KEYS[1]) " +
            "local quantity = tonumber(ARGV[1]) " +
            "local currentStock = redis.call('get', stockKey) " +
            "if currentStock == false or currentStock == nil then " +
            "    return 0 " +
            "end " +
            "local stock = tonumber(currentStock) " +
            "if stock >= quantity then " +
            "    redis.call('decrby', stockKey, quantity) " +
            "    return 1 " +
            "else " +
            "    return 0 " +
            "end";

    /**
     * 原子性库存回滚Lua脚本
     */
    private static final String STOCK_ROLLBACK_SCRIPT = 
            "local stockKey = 'ticket_stock:' .. tostring(KEYS[1]) " +
            "local quantity = tonumber(ARGV[1]) " +
            "redis.call('incrby', stockKey, quantity) " +
            "return 1";

    /**
     * 原子性库存锁定Lua脚本
     */
    private static final String STOCK_LOCK_SCRIPT = 
            "local stockKey = 'ticket_stock:' .. tostring(KEYS[1]) " +
            "local lockKey = 'ticket_locked:' .. tostring(KEYS[1]) .. ':' .. tostring(KEYS[2]) " +
            "local quantity = tonumber(ARGV[1]) " +
            "local expireTime = tonumber(ARGV[2]) " +
            "local currentStock = redis.call('get', stockKey) " +
            "if currentStock == false or currentStock == nil then " +
            "    return 0 " +
            "end " +
            "local stock = tonumber(currentStock) " +
            "if stock >= quantity then " +
            "    redis.call('decrby', stockKey, quantity) " +
            "    redis.call('setex', lockKey, expireTime, quantity) " +
            "    return 1 " +
            "else " +
            "    return 0 " +
            "end";

    /**
     * 原子性库存锁定释放Lua脚本
     */
    private static final String STOCK_UNLOCK_SCRIPT = 
            "local stockKey = 'ticket_stock:' .. tostring(KEYS[1]) " +
            "local lockKey = 'ticket_locked:' .. tostring(KEYS[1]) .. ':' .. tostring(KEYS[2]) " +
            "local lockedQuantity = redis.call('get', lockKey) " +
            "if lockedQuantity ~= false and lockedQuantity ~= nil then " +
            "    local quantity = tonumber(lockedQuantity) " +
            "    redis.call('incrby', stockKey, quantity) " +
            "    redis.call('del', lockKey) " +
            "    return 1 " +
            "else " +
            "    return 0 " +
            "end";

    /**
     * 初始化库存到Redis
     */
    public void initStockToRedis(Long ticketId) {
        try {
            TicketStock stock = ticketStockMapper.selectByTicketId(ticketId);
            if (stock != null) {
                String stockKey = STOCK_KEY_PREFIX + ticketId;
                Integer remainStock = ticketStockMapper.getRemainStock(ticketId);
                if (remainStock != null) {
                    redissonClient.getBucket(stockKey).set(remainStock);
                    log.info("初始化库存到Redis，票档ID: {}, 库存: {}", ticketId, remainStock);
                }
            }
        } catch (Exception e) {
            log.error("初始化库存到Redis失败，票档ID: {}", ticketId, e);
        }
    }

    /**
     * 原子性库存扣减
     */
    public boolean atomicDeductStock(Long ticketId, Integer quantity) {
        try {
            // 确保参数类型一致性：Long转String
            String ticketIdStr = String.valueOf(ticketId);
            String quantityStr = String.valueOf(quantity);
            
            // 使用ArrayList确保参数类型正确
            List<Object> keyList = new ArrayList<>();
            keyList.add(ticketIdStr);
            
            List<Object> argList = new ArrayList<>();
            argList.add(quantityStr);
            
            Long result = redissonClient.getScript().eval(
                    RScript.Mode.READ_WRITE,
                    STOCK_DEDUCT_SCRIPT,
                    RScript.ReturnType.INTEGER,
                    keyList,
                    argList
            );
            
            boolean success = result != null && result == 1;
            log.info("原子性库存扣减，票档ID: {}, 数量: {}, 结果: {}", ticketId, quantity, success);
            return success;
            
        } catch (Exception e) {
            log.error("原子性库存扣减失败，票档ID: {}, 数量: {}", ticketId, quantity, e);
            return false;
        }
    }

    /**
     * 原子性库存回滚
     */
    public boolean atomicRollbackStock(Long ticketId, Integer quantity) {
        try {
            // 确保参数类型一致性：Long转String
            String ticketIdStr = String.valueOf(ticketId);
            String quantityStr = String.valueOf(quantity);
            
            // 使用ArrayList确保参数类型正确
            List<Object> keyList = new ArrayList<>();
            keyList.add(ticketIdStr);
            
            List<Object> argList = new ArrayList<>();
            argList.add(quantityStr);
            
            Long result = redissonClient.getScript().eval(
                    RScript.Mode.READ_WRITE,
                    STOCK_ROLLBACK_SCRIPT,
                    RScript.ReturnType.INTEGER,
                    keyList,
                    argList
            );
            
            boolean success = result != null && result == 1;
            log.info("原子性库存回滚，票档ID: {}, 数量: {}, 结果: {}", ticketId, quantity, success);
            return success;
            
        } catch (Exception e) {
            log.error("原子性库存回滚失败，票档ID: {}, 数量: {}", ticketId, quantity, e);
            return false;
        }
    }

    /**
     * 原子性库存锁定
     */
    public boolean atomicLockStock(Long ticketId, Long userId, Integer quantity) {
        try {
            // 确保参数类型一致性：Long转String
            String ticketIdStr = String.valueOf(ticketId);
            String userIdStr = String.valueOf(userId);
            String quantityStr = String.valueOf(quantity);
            String expireTimeStr = String.valueOf(300); // 5分钟过期
            
            // 使用ArrayList确保参数类型正确
            List<Object> keyList = new ArrayList<>();
            keyList.add(ticketIdStr);
            keyList.add(userIdStr);
            
            List<Object> argList = new ArrayList<>();
            argList.add(quantityStr);
            argList.add(expireTimeStr);
            
            Long result = redissonClient.getScript().eval(
                    RScript.Mode.READ_WRITE,
                    STOCK_LOCK_SCRIPT,
                    RScript.ReturnType.INTEGER,
                    keyList,
                    argList
            );
            
            boolean success = result != null && result == 1;
            log.info("原子性库存锁定，票档ID: {}, 用户ID: {}, 数量: {}, 结果: {}", 
                    ticketId, userId, quantity, success);
            return success;
            
        } catch (Exception e) {
            log.error("原子性库存锁定失败，票档ID: {}, 用户ID: {}, 数量: {}", 
                    ticketId, userId, quantity, e);
            return false;
        }
    }

    /**
     * 原子性库存锁定释放
     */
    public boolean atomicUnlockStock(Long ticketId, Long userId) {
        try {
            // 确保参数类型一致性：Long转String
            String ticketIdStr = String.valueOf(ticketId);
            String userIdStr = String.valueOf(userId);
            
            // 使用ArrayList确保参数类型正确
            List<Object> keyList = new ArrayList<>();
            keyList.add(ticketIdStr);
            keyList.add(userIdStr);
            
            List<Object> argList = new ArrayList<>();
            
            Long result = redissonClient.getScript().eval(
                    RScript.Mode.READ_WRITE,
                    STOCK_UNLOCK_SCRIPT,
                    RScript.ReturnType.INTEGER,
                    keyList,
                    argList
            );
            
            boolean success = result != null && result == 1;
            log.info("原子性库存锁定释放，票档ID: {}, 用户ID: {}, 结果: {}", 
                    ticketId, userId, success);
            return success;
            
        } catch (Exception e) {
            log.error("原子性库存锁定释放失败，票档ID: {}, 用户ID: {}", ticketId, userId, e);
            return false;
        }
    }

    /**
     * 获取Redis中的库存数量
     */
    public Integer getRedisStock(Long ticketId) {
        try {
            String stockKey = STOCK_KEY_PREFIX + ticketId;
            Object stockObj = redissonClient.getBucket(stockKey).get();
            if (stockObj instanceof String) {
                return Integer.parseInt((String) stockObj);
            } else if (stockObj instanceof Integer) {
                return (Integer) stockObj;
            }
            return 0;
        } catch (Exception e) {
            log.error("获取Redis库存失败，票档ID: {}", ticketId, e);
            return 0;
        }
    }

    /**
     * 同步Redis库存到数据库
     */
    public void syncStockToDatabase(Long ticketId) {
        try {
            Integer redisStock = getRedisStock(ticketId);
            if (redisStock != null) {
                ticketStockMapper.updateRemainStock(ticketId, redisStock);
                log.info("同步库存到数据库，票档ID: {}, 库存: {}", ticketId, redisStock);
            }
        } catch (Exception e) {
            log.error("同步库存到数据库失败，票档ID: {}", ticketId, e);
        }
    }
}