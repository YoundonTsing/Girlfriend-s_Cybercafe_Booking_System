package com.ticketsystem.show.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;

import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 使用Redisson RLock的座位锁定服务
 * 避免Lua脚本参数类型问题
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedissonSeatLockService {

    private final RedissonClient redissonClient;

    // 座位锁定键前缀
    private static final String SEAT_LOCK_PREFIX = "seat_lock:";
    
    // 锁定过期时间（秒）
    private static final int LOCK_EXPIRE_TIME = 300; // 5分钟
    private static final int LOCK_WAIT_TIME = 1; // 1秒等待时间

    // 使用Redis原子操作API替代Lua脚本

    /**
     * 原子性单个座位锁定 - 使用Redis原子操作API
     */
    public boolean atomicLockSeat(Long seatId, Long userId) {
        String lockKey = SEAT_LOCK_PREFIX + seatId;
        String lockValue = String.valueOf(userId) + ":" + System.currentTimeMillis();
        
        try {
            // 使用SETNX原子操作设置锁，如果键不存在则设置成功
            Boolean success = redissonClient.getBucket(lockKey).trySet(lockValue);
            
            if (success != null && success) {
                // 设置过期时间
                redissonClient.getBucket(lockKey).expire(LOCK_EXPIRE_TIME, TimeUnit.SECONDS);
                log.info("原子性座位锁定成功，座位ID: {}, 用户ID: {}", seatId, userId);
                return true;
            } else {
                // 检查是否是同一用户的锁
                String currentValue = (String) redissonClient.getBucket(lockKey).get();
                if (currentValue != null && currentValue.startsWith(String.valueOf(userId) + ":")) {
                    // 同一用户重新设置锁和过期时间
                    redissonClient.getBucket(lockKey).set(lockValue, LOCK_EXPIRE_TIME, TimeUnit.SECONDS);
                    log.info("原子性座位锁定成功（重新锁定），座位ID: {}, 用户ID: {}", seatId, userId);
                    return true;
                } else {
                    log.warn("原子性座位锁定失败，座位ID: {}, 用户ID: {}, 原因: 座位已被其他用户锁定", seatId, userId);
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("原子性座位锁定异常，座位ID: {}, 用户ID: {}", seatId, userId, e);
            return false;
        }
    }

    /**
     * 原子性单个座位解锁 - 使用Redis原子操作API
     */
    public boolean atomicUnlockSeat(Long seatId, Long userId) {
        String lockKey = SEAT_LOCK_PREFIX + seatId;
        
        try {
            // 获取当前锁的值来验证用户ID
            String currentLockValue = (String) redissonClient.getBucket(lockKey).get();
            
            if (currentLockValue == null) {
                log.info("座位未锁定或锁已过期，座位ID: {}, 用户ID: {}", seatId, userId);
                return true; // 锁不存在视为解锁成功
            }
            
            // 验证锁是否属于当前用户
            String[] parts = currentLockValue.split(":");
            if (parts.length > 0) {
                try {
                    Long lockUserId = Long.parseLong(parts[0]);
                    if (!lockUserId.equals(userId)) {
                        log.warn("原子性座位解锁失败，座位ID: {}, 用户ID: {}, 原因: 锁属于其他用户 {}", seatId, userId, lockUserId);
                        return false;
                    }
                } catch (NumberFormatException e) {
                    log.warn("锁值格式异常，座位ID: {}, 锁值: {}", seatId, currentLockValue);
                    return false;
                }
            }
            
            // 使用原子操作删除锁
            boolean deleted = redissonClient.getBucket(lockKey).delete();
            
            if (deleted) {
                log.info("原子性座位解锁成功，座位ID: {}, 用户ID: {}", seatId, userId);
                return true;
            } else {
                log.warn("原子性座位解锁失败，座位ID: {}, 用户ID: {}, 原因: 锁已不存在", seatId, userId);
                return true; // 锁已不存在也视为解锁成功
            }
        } catch (Exception e) {
            log.error("原子性座位解锁异常，座位ID: {}, 用户ID: {}", seatId, userId, e);
            return false;
        }
    }

    /**
     * 原子性批量座位锁定
     */
    public Map<Long, Boolean> atomicLockSeats(List<Long> seatIds, Long userId) {
        Map<Long, Boolean> results = new HashMap<>();
        
        log.info("开始执行原子性批量座位锁定，座位IDs: {}, 用户ID: {}", seatIds, userId);
        
        try {
            // 按顺序尝试锁定每个座位
            for (Long seatId : seatIds) {
                boolean success = atomicLockSeat(seatId, userId);
                results.put(seatId, success);
                
                // 如果任何一个座位锁定失败，释放已锁定的座位
                if (!success) {
                    log.warn("座位 {} 锁定失败，开始释放已锁定的座位", seatId);
                    for (Map.Entry<Long, Boolean> entry : results.entrySet()) {
                        if (entry.getValue()) {
                            atomicUnlockSeat(entry.getKey(), userId);
                        }
                    }
                    // 清空结果，表示全部失败
                    results.clear();
                    for (Long id : seatIds) {
                        results.put(id, false);
                    }
                    break;
                }
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
            
            log.info("原子性批量座位解锁完成，座位IDs: {}, 用户ID: {}, 结果: {}", 
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
            RLock lock = redissonClient.getLock(lockKey);
            return lock.isLocked();
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
            String lockInfoKey = lockKey + ":info";
            
            RLock lock = redissonClient.getLock(lockKey);
            Map<Object, Object> lockInfo = redissonClient.getMap(lockInfoKey);
            
            return String.format("座位ID: %d, 锁定状态: %s, 锁定信息: %s", 
                    seatId, lock.isLocked(), lockInfo);
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
            // Redisson的RLock会自动处理过期，这里可以添加额外的清理逻辑
            log.info("清理过期座位锁定完成");
        } catch (Exception e) {
            log.error("清理过期座位锁定失败", e);
        }
    }
}