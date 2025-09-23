package com.ticketsystem.show.service.impl;

import com.ticketsystem.show.entity.Seat;
import com.ticketsystem.show.mapper.SeatMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 座位数据一致性保障服务
 * 确保Redis分布式锁与数据库座位状态的强一致性
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeatConsistencyService {

    private final RedissonClient redissonClient;
    private final SeatMapper seatMapper;

    private static final String SEAT_LOCK_PREFIX = "seat_lock:";
    private static final String SEAT_SYNC_PREFIX = "seat_sync:";
    private static final int LOCK_EXPIRE_TIME = 300; // 5分钟
    private static final int SYNC_EXPIRE_TIME = 60; // 1分钟同步锁

    // 增强版Lua脚本：原子性锁定座位并记录同步状态
    private static final String ENHANCED_LOCK_SCRIPT = 
            "local lockKey = KEYS[1] " +
            "local syncKey = KEYS[2] " +
            "local lockValue = ARGV[1] " +
            "local expireTime = tonumber(ARGV[2]) " +
            "local syncExpire = tonumber(ARGV[3]) " +
            "local timestamp = ARGV[4] " +
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

    // 增强版Lua脚本：原子性释放座位锁并清理同步状态
    private static final String ENHANCED_UNLOCK_SCRIPT = 
            "local lockKey = KEYS[1] " +
            "local syncKey = KEYS[2] " +
            "local lockValue = ARGV[1] " +
            "local currentValue = redis.call('get', lockKey) " +
            "if currentValue == lockValue then " +
            "    redis.call('del', lockKey) " +
            "    redis.call('del', syncKey) " +
            "    return 1 " +
            "else " +
            "    return 0 " +
            "end";

    // 数据一致性检查脚本
    private static final String CONSISTENCY_CHECK_SCRIPT = 
            "local lockKey = KEYS[1] " +
            "local syncKey = KEYS[2] " +
            "local lockExists = redis.call('exists', lockKey) " +
            "local syncExists = redis.call('exists', syncKey) " +
            "local lockValue = redis.call('get', lockKey) " +
            "local syncValue = redis.call('get', syncKey) " +
            "return {lockExists, syncExists, lockValue or '', syncValue or ''}";

    /**
     * 原子性锁定座位（Redis + 数据库双重保障）
     */
    @Transactional
    public boolean atomicLockSeats(List<Long> seatIds, Long userId) {
        String lockValue = userId + ":" + System.currentTimeMillis();
        String timestamp = String.valueOf(System.currentTimeMillis());
        
        try {
            // 1. 先尝试Redis原子锁定
            for (Long seatId : seatIds) {
                if (!tryRedisLockWithSync(seatId, lockValue, timestamp)) {
                    // 锁定失败，回滚已锁定的座位
                    rollbackRedisLocks(seatIds, seatId, lockValue);
                    return false;
                }
            }
            
            // 2. Redis锁定成功后，进行数据库锁定
            int lockedCount = seatMapper.lockSeats(seatIds, userId);
            
            if (lockedCount != seatIds.size()) {
                // 数据库锁定失败，释放所有Redis锁
                releaseAllRedisLocks(seatIds, lockValue);
                log.warn("数据库锁定失败，用户ID: {}, 座位IDs: {}, 锁定数量: {}", userId, seatIds, lockedCount);
                return false;
            }
            
            // 3. 异步验证数据一致性
            asyncVerifyConsistency(seatIds, userId);
            
            log.info("原子性锁定座位成功，用户ID: {}, 座位IDs: {}", userId, seatIds);
            return true;
            
        } catch (Exception e) {
            log.error("原子性锁定座位失败，用户ID: {}, 座位IDs: {}", userId, seatIds, e);
            // 异常时释放所有Redis锁
            releaseAllRedisLocks(seatIds, lockValue);
            return false;
        }
    }

    /**
     * 原子性释放座位锁定
     */
    @Transactional
    public boolean atomicReleaseSeats(List<Long> seatIds, Long userId) {
        String lockValue = userId + ":";
        
        try {
            // 1. 释放数据库锁定
            int releasedCount = seatMapper.releaseSeats(seatIds, userId);
            
            // 2. 释放Redis锁（即使数据库释放失败也要释放Redis锁）
            releaseAllRedisLocksWithPrefix(seatIds, lockValue);
            
            log.info("原子性释放座位锁定，用户ID: {}, 座位IDs: {}, 释放数量: {}", userId, seatIds, releasedCount);
            return true;
            
        } catch (Exception e) {
            log.error("原子性释放座位锁定失败，用户ID: {}, 座位IDs: {}", userId, seatIds, e);
            return false;
        }
    }

    /**
     * 使用增强Lua脚本尝试Redis锁定
     */
    private boolean tryRedisLockWithSync(Long seatId, String lockValue, String timestamp) {
        String lockKey = SEAT_LOCK_PREFIX + seatId;
        String syncKey = SEAT_SYNC_PREFIX + seatId;
        
        // 确保所有参数都是字符串类型
        String lockValueStr = String.valueOf(lockValue);
        String expireTimeStr = String.valueOf(LOCK_EXPIRE_TIME);
        String syncExpireStr = String.valueOf(SYNC_EXPIRE_TIME);
        String timestampStr = String.valueOf(timestamp);
        
        Long result = redissonClient.getScript().eval(
                RScript.Mode.READ_WRITE,
                ENHANCED_LOCK_SCRIPT,
                RScript.ReturnType.INTEGER,
                Arrays.asList(lockKey, syncKey),
                Arrays.asList(lockValueStr, expireTimeStr, syncExpireStr, timestampStr)
        );
        
        return result != null && result == 1;
    }

    /**
     * 回滚部分Redis锁定
     */
    private void rollbackRedisLocks(List<Long> seatIds, Long failedSeatId, String lockValue) {
        for (Long seatId : seatIds) {
            if (seatId.equals(failedSeatId)) {
                break;
            }
            releaseRedisLockWithSync(seatId, lockValue);
        }
    }

    /**
     * 释放所有Redis锁
     */
    private void releaseAllRedisLocks(List<Long> seatIds, String lockValue) {
        for (Long seatId : seatIds) {
            releaseRedisLockWithSync(seatId, lockValue);
        }
    }

    /**
     * 使用前缀匹配释放Redis锁
     */
    private void releaseAllRedisLocksWithPrefix(List<Long> seatIds, String lockValuePrefix) {
        for (Long seatId : seatIds) {
            String lockKey = SEAT_LOCK_PREFIX + seatId;
            String syncKey = SEAT_SYNC_PREFIX + seatId;
            
            try {
                // 使用前缀匹配的释放脚本
                String prefixUnlockScript = 
                        "local lockKey = KEYS[1] " +
                        "local syncKey = KEYS[2] " +
                        "local lockValuePrefix = ARGV[1] " +
                        "local currentValue = redis.call('get', lockKey) " +
                        "if currentValue and string.sub(currentValue, 1, string.len(lockValuePrefix)) == lockValuePrefix then " +
                        "    redis.call('del', lockKey) " +
                        "    redis.call('del', syncKey) " +
                        "    return 1 " +
                        "else " +
                        "    return 0 " +
                        "end";
                
                // 确保lockValuePrefix是字符串类型
                String lockValuePrefixStr = String.valueOf(lockValuePrefix);
                
                redissonClient.getScript().eval(
                        RScript.Mode.READ_WRITE,
                        prefixUnlockScript,
                        RScript.ReturnType.INTEGER,
                        Arrays.asList(lockKey, syncKey),
                        Arrays.asList(lockValuePrefixStr)
                );
            } catch (Exception e) {
                log.warn("释放Redis锁失败，座位ID: {}, 用户前缀: {}", seatId, lockValuePrefix, e);
            }
        }
    }

    /**
     * 释放单个Redis锁
     */
    private void releaseRedisLockWithSync(Long seatId, String lockValue) {
        String lockKey = SEAT_LOCK_PREFIX + seatId;
        String syncKey = SEAT_SYNC_PREFIX + seatId;
        
        try {
            // 确保lockValue是字符串类型
            String lockValueStr = String.valueOf(lockValue);
            
            redissonClient.getScript().eval(
                    RScript.Mode.READ_WRITE,
                    ENHANCED_UNLOCK_SCRIPT,
                    RScript.ReturnType.INTEGER,
                    Arrays.asList(lockKey, syncKey),
                    Arrays.asList(lockValueStr)
            );
        } catch (Exception e) {
            log.warn("释放Redis锁失败，座位ID: {}, lockValue: {}", seatId, lockValue, e);
        }
    }

    /**
     * 异步验证数据一致性
     */
    @Async
    public void asyncVerifyConsistency(List<Long> seatIds, Long userId) {
        try {
            // 延迟100ms后验证，确保数据库事务已提交
            Thread.sleep(100);
            
            for (Long seatId : seatIds) {
                verifyAndRepairConsistency(seatId, userId);
            }
        } catch (Exception e) {
            log.error("异步验证数据一致性失败，座位IDs: {}, 用户ID: {}", seatIds, userId, e);
        }
    }

    /**
     * 验证并修复单个座位的数据一致性
     */
    public void verifyAndRepairConsistency(Long seatId, Long userId) {
        String lockKey = SEAT_LOCK_PREFIX + seatId;
        String syncKey = SEAT_SYNC_PREFIX + seatId;
        
        try {
            // 1. 检查Redis状态
            List<Object> redisStatus = redissonClient.getScript().eval(
                    RScript.Mode.READ_ONLY,
                    CONSISTENCY_CHECK_SCRIPT,
                    RScript.ReturnType.MULTI,
                    Arrays.asList(lockKey, syncKey)
            );
            
            boolean redisLockExists = (Long) redisStatus.get(0) == 1;
            boolean redisSyncExists = (Long) redisStatus.get(1) == 1;
            String redisLockValue = (String) redisStatus.get(2);
            
            // 2. 检查数据库状态
            Seat dbSeat = seatMapper.selectSeatById(seatId);
            
            // 3. 数据一致性修复逻辑
            boolean dbLocked = (dbSeat != null && dbSeat.getLockStatus() != null && dbSeat.getLockStatus() == 1);
            Long dbLockUserId = (dbSeat != null) ? dbSeat.getLockUserId() : null;
            
            // 检查各种不一致情况并修复
            if (redisLockExists && !dbLocked) {
                // Redis有锁但数据库无锁 - 释放Redis锁
                log.warn("数据不一致修复：Redis有锁但数据库无锁，释放Redis锁，座位ID: {}", seatId);
                releaseRedisLockWithSync(seatId, redisLockValue);
            } else if (!redisLockExists && dbLocked) {
                // 数据库有锁但Redis无锁 - 释放数据库锁（可能是Redis锁过期）
                log.warn("数据不一致修复：数据库有锁但Redis无锁，释放数据库锁，座位ID: {}, 数据库锁用户: {}", 
                        seatId, dbLockUserId);
                if (dbLockUserId != null) {
                    seatMapper.releaseSeat(seatId, dbLockUserId);
                }
            } else if (redisLockExists && dbLocked) {
                // 双方都有锁 - 验证用户ID是否一致
                String[] lockParts = redisLockValue.split(":");
                if (lockParts.length > 0) {
                    try {
                        Long redisUserId = Long.parseLong(lockParts[0]);
                        if (!redisUserId.equals(dbLockUserId)) {
                            log.warn("数据不一致修复：Redis和数据库锁定用户不一致，座位ID: {}, Redis用户: {}, 数据库用户: {}", 
                                    seatId, redisUserId, dbLockUserId);
                            // 以数据库为准，更新Redis锁
                            releaseRedisLockWithSync(seatId, redisLockValue);
                            if (dbLockUserId != null) {
                                String newLockValue = dbLockUserId + ":" + System.currentTimeMillis();
                                tryRedisLockWithSync(seatId, newLockValue, String.valueOf(System.currentTimeMillis()));
                            }
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Redis锁值格式异常，座位ID: {}, 锁值: {}", seatId, redisLockValue);
                    }
                }
            }
            
            if (redisLockExists && !redisSyncExists) {
                // Redis锁存在但同步标记不存在，重新设置同步标记
                log.warn("发现数据不一致：Redis锁存在但同步标记缺失，座位ID: {}", seatId);
                redissonClient.getBucket(syncKey).set(String.valueOf(System.currentTimeMillis()), 
                                                     SYNC_EXPIRE_TIME, TimeUnit.SECONDS);
            }
            
            log.debug("数据一致性验证完成，座位ID: {}, Redis锁: {}, 数据库锁: {}, 同步标记: {}", 
                     seatId, redisLockExists, dbLocked, redisSyncExists);
            
        } catch (Exception e) {
            log.error("验证数据一致性失败，座位ID: {}", seatId, e);
        }
    }

    /**
     * 清理过期的Redis锁和同步标记
     */
    public void cleanupExpiredLocks() {
        try {
            // 1. 清理数据库过期锁定
            int dbClearedCount = seatMapper.clearExpiredLocks();
            
            // 2. Redis的过期锁会自动清理，但我们可以主动检查不一致的情况
            if (dbClearedCount > 0) {
                log.info("清理数据库过期座位锁定，数量: {}", dbClearedCount);
            }
            
        } catch (Exception e) {
            log.error("清理过期锁定失败", e);
        }
    }

    /**
     * 获取座位锁定状态（用于监控和调试）
     */
    public String getSeatLockStatus(Long seatId) {
        String lockKey = SEAT_LOCK_PREFIX + seatId;
        String syncKey = SEAT_SYNC_PREFIX + seatId;
        
        try {
            List<Object> status = redissonClient.getScript().eval(
                    RScript.Mode.READ_ONLY,
                    CONSISTENCY_CHECK_SCRIPT,
                    RScript.ReturnType.MULTI,
                    Arrays.asList(lockKey, syncKey)
            );
            
            return String.format("座位ID: %d, Redis锁: %s, 同步标记: %s, 锁值: %s, 同步值: %s",
                    seatId, status.get(0), status.get(1), status.get(2), status.get(3));
            
        } catch (Exception e) {
            log.error("获取座位锁定状态失败，座位ID: {}", seatId, e);
            return "状态获取失败";
        }
    }
}