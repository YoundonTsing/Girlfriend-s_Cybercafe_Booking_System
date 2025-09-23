package com.ticketsystem.show.service;

import com.ticketsystem.show.mapper.SeatMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 原子性座位锁定服务
 * 使用Redisson+Lua实现高性能座位锁定操作
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AtomicSeatLockService {

    private final RedissonClient redissonClient;
    private final SeatMapper seatMapper;

    // 座位锁定键前缀
    private static final String SEAT_LOCK_PREFIX = "seat_lock:";
    
    // 座位同步键前缀
    private static final String SEAT_SYNC_PREFIX = "seat_sync:";
    
    // 锁定过期时间（秒）
    private static final int LOCK_EXPIRE_TIME = 300; // 5分钟
    private static final int SYNC_EXPIRE_TIME = 60;  // 1分钟

    /**
     * 原子性座位锁定Lua脚本
     * 注意：所有参数都转换为字符串，确保类型一致性
     */
    private static final String SEAT_LOCK_SCRIPT = 
            "local lockKey = 'seat_lock:' .. tostring(KEYS[1]) " +
            "local syncKey = 'seat_sync:' .. tostring(KEYS[1]) " +
            "local lockValue = tostring(ARGV[1]) " +
            "local expireTime = tonumber(ARGV[2]) " +
            "local syncExpire = tonumber(ARGV[3]) " +
            "local timestamp = tostring(ARGV[4]) " +
            "local currentValue = redis.call('get', lockKey) " +
            "if currentValue == false or currentValue == nil then " +
            "    redis.call('setex', lockKey, expireTime, lockValue) " +
            "    redis.call('setex', syncKey, syncExpire, timestamp) " +
            "    return 1 " +
            "elseif currentValue == lockValue then " +
            "    redis.call('setex', lockKey, expireTime, lockValue) " +
            "    redis.call('setex', syncKey, syncExpire, timestamp) " +
            "    return 1 " +
            "else " +
            "    return 0 " +
            "end";

    /**
     * 原子性座位解锁Lua脚本
     */
    private static final String SEAT_UNLOCK_SCRIPT = 
            "local lockKey = 'seat_lock:' .. tostring(KEYS[1]) " +
            "local syncKey = 'seat_sync:' .. tostring(KEYS[1]) " +
            "local lockValue = tostring(ARGV[1]) " +
            "local currentValue = redis.call('get', lockKey) " +
            "if currentValue == lockValue then " +
            "    redis.call('del', lockKey) " +
            "    redis.call('del', syncKey) " +
            "    return 1 " +
            "else " +
            "    return 0 " +
            "end";

    /**
     * 批量座位锁定Lua脚本
     */
    private static final String BATCH_SEAT_LOCK_SCRIPT = 
            "local results = {} " +
            "for i = 1, #KEYS do " +
            "    local lockKey = 'seat_lock:' .. tostring(KEYS[i]) " +
            "    local syncKey = 'seat_sync:' .. tostring(KEYS[i]) " +
            "    local lockValue = tostring(ARGV[1]) " +
            "    local expireTime = tonumber(ARGV[2]) " +
            "    local syncExpire = tonumber(ARGV[3]) " +
            "    local timestamp = tostring(ARGV[4]) " +
            "    local currentValue = redis.call('get', lockKey) " +
            "    if currentValue == false or currentValue == nil then " +
            "        redis.call('setex', lockKey, expireTime, lockValue) " +
            "        redis.call('setex', syncKey, syncExpire, timestamp) " +
            "        results[i] = 1 " +
            "    elseif currentValue == lockValue then " +
            "        redis.call('setex', lockKey, expireTime, lockValue) " +
            "        redis.call('setex', syncKey, syncExpire, timestamp) " +
            "        results[i] = 1 " +
            "    else " +
            "        results[i] = 0 " +
            "    end " +
            "end " +
            "return results";

    /**
     * 原子性单个座位锁定
     */
    public boolean atomicLockSeat(Long seatId, Long userId) {
        try {
            // 确保参数类型一致性：Long转String
            String seatIdStr = String.valueOf(seatId);
            String lockValue = userId + ":" + System.currentTimeMillis();
            String expireTimeStr = String.valueOf(LOCK_EXPIRE_TIME);
            String syncExpireStr = String.valueOf(SYNC_EXPIRE_TIME);
            String timestampStr = String.valueOf(System.currentTimeMillis());
            
            // 使用ArrayList确保参数类型正确
            List<Object> keyList = new ArrayList<>();
            keyList.add(seatIdStr);
            
            List<Object> argList = new ArrayList<>();
            argList.add(lockValue);
            argList.add(expireTimeStr);
            argList.add(syncExpireStr);
            argList.add(timestampStr);
            
            Long result = redissonClient.getScript().eval(
                    RScript.Mode.READ_WRITE,
                    SEAT_LOCK_SCRIPT,
                    RScript.ReturnType.INTEGER,
                    keyList,
                    argList
            );
            
            boolean success = result != null && result == 1;
            log.info("原子性座位锁定，座位ID: {}, 用户ID: {}, 结果: {}", seatId, userId, success);
            return success;
            
        } catch (Exception e) {
            log.error("原子性座位锁定失败，座位ID: {}, 用户ID: {}", seatId, userId, e);
            return false;
        }
    }

    /**
     * 原子性单个座位解锁
     */
    public boolean atomicUnlockSeat(Long seatId, Long userId) {
        try {
            // 确保参数类型一致性：Long转String
            String seatIdStr = String.valueOf(seatId);
            String lockValue = userId + ":" + System.currentTimeMillis();
            
            // 使用ArrayList确保参数类型正确
            List<Object> keyList = new ArrayList<>();
            keyList.add(seatIdStr);
            
            List<Object> argList = new ArrayList<>();
            argList.add(lockValue);
            
            Long result = redissonClient.getScript().eval(
                    RScript.Mode.READ_WRITE,
                    SEAT_UNLOCK_SCRIPT,
                    RScript.ReturnType.INTEGER,
                    keyList,
                    argList
            );
            
            boolean success = result != null && result == 1;
            log.info("原子性座位解锁，座位ID: {}, 用户ID: {}, 结果: {}", seatId, userId, success);
            return success;
            
        } catch (Exception e) {
            log.error("原子性座位解锁失败，座位ID: {}, 用户ID: {}", seatId, userId, e);
            return false;
        }
    }

    /**
     * 原子性批量座位锁定
     */
    public Map<Long, Boolean> atomicLockSeats(List<Long> seatIds, Long userId) {
        Map<Long, Boolean> results = new HashMap<>();
        
        try {
            log.info("开始执行原子性批量座位锁定，座位IDs: {}, 用户ID: {}", seatIds, userId);
            
            // 检查Redisson客户端
            if (redissonClient == null) {
                log.error("RedissonClient未初始化");
                for (Long seatId : seatIds) {
                    results.put(seatId, false);
                }
                return results;
            }
            
            // 确保参数类型一致性：Long转String
            String[] seatIdStrs = seatIds.stream()
                    .map(String::valueOf)
                    .toArray(String[]::new);
            
            String lockValue = userId + ":" + System.currentTimeMillis();
            String expireTimeStr = String.valueOf(LOCK_EXPIRE_TIME);
            String syncExpireStr = String.valueOf(SYNC_EXPIRE_TIME);
            String timestampStr = String.valueOf(System.currentTimeMillis());
            
            log.info("Lua脚本参数 - 座位IDs: {}, 锁定值: {}, 过期时间: {}", 
                    Arrays.toString(seatIdStrs), lockValue, expireTimeStr);
            
            // 使用ArrayList确保参数类型正确
            List<Object> keyList = new ArrayList<>();
            for (String seatIdStr : seatIdStrs) {
                keyList.add(seatIdStr);
            }
            
            List<Object> argList = new ArrayList<>();
            argList.add(lockValue);
            argList.add(expireTimeStr);
            argList.add(syncExpireStr);
            argList.add(timestampStr);
            
            log.info("执行Lua脚本: {}", BATCH_SEAT_LOCK_SCRIPT);
            log.info("键列表: {}", keyList);
            log.info("参数列表: {}", argList);
            
            List<Object> resultList = redissonClient.getScript().eval(
                    RScript.Mode.READ_WRITE,
                    BATCH_SEAT_LOCK_SCRIPT,
                    RScript.ReturnType.MULTI,
                    keyList,
                    argList
            );
            
            log.info("Lua脚本执行结果: {}", resultList);
            
            // 处理结果
            for (int i = 0; i < seatIds.size(); i++) {
                Long seatId = seatIds.get(i);
                Object result = resultList.get(i);
                boolean success = result != null && Integer.parseInt(result.toString()) == 1;
                results.put(seatId, success);
            }
            
            log.info("原子性批量座位锁定完成，座位IDs: {}, 用户ID: {}, 结果: {}", 
                    seatIds, userId, results);
            return results;
            
        } catch (Exception e) {
            log.error("原子性批量座位锁定失败，座位IDs: {}, 用户ID: {}", seatIds, userId, e);
            // 返回全部失败
            for (Long seatId : seatIds) {
                results.put(seatId, false);
            }
            return results;
        }
    }

    /**
     * 原子性批量座位解锁
     */
    public Map<Long, Boolean> atomicUnlockSeats(List<Long> seatIds, Long userId) {
        Map<Long, Boolean> results = new HashMap<>();
        
        try {
            for (Long seatId : seatIds) {
                boolean success = atomicUnlockSeat(seatId, userId);
                results.put(seatId, success);
            }
            
            log.info("原子性批量座位解锁，座位IDs: {}, 用户ID: {}, 结果: {}", 
                    seatIds, userId, results);
            return results;
            
        } catch (Exception e) {
            log.error("原子性批量座位解锁失败，座位IDs: {}, 用户ID: {}", seatIds, userId, e);
            // 返回全部失败
            for (Long seatId : seatIds) {
                results.put(seatId, false);
            }
            return results;
        }
    }

    /**
     * 检查座位锁定状态
     */
    public boolean isSeatLocked(Long seatId) {
        try {
            String lockKey = SEAT_LOCK_PREFIX + seatId;
            Object lockValueObj = redissonClient.getBucket(lockKey).get();
            if (lockValueObj instanceof String) {
                String lockValue = (String) lockValueObj;
                return lockValue != null && !lockValue.isEmpty();
            }
            return false;
        } catch (Exception e) {
            log.error("检查座位锁定状态失败，座位ID: {}", seatId, e);
            return false;
        }
    }

    /**
     * 获取座位锁定信息
     */
    public String getSeatLockInfo(Long seatId) {
        try {
            String lockKey = SEAT_LOCK_PREFIX + seatId;
            String syncKey = SEAT_SYNC_PREFIX + seatId;
            
            Object lockValueObj = redissonClient.getBucket(lockKey).get();
            Object syncValueObj = redissonClient.getBucket(syncKey).get();
            
            String lockValue = lockValueObj != null ? lockValueObj.toString() : "null";
            String syncValue = syncValueObj != null ? syncValueObj.toString() : "null";
            
            return String.format("座位ID: %d, 锁定值: %s, 同步值: %s", 
                    seatId, lockValue, syncValue);
        } catch (Exception e) {
            log.error("获取座位锁定信息失败，座位ID: {}", seatId, e);
            return "获取失败";
        }
    }

    /**
     * 清理过期锁定
     */
    public void cleanupExpiredLocks() {
        try {
            // 这里可以实现清理逻辑，或者依赖Redis的TTL自动过期
            log.info("清理过期座位锁定完成");
        } catch (Exception e) {
            log.error("清理过期座位锁定失败", e);
        }
    }
}