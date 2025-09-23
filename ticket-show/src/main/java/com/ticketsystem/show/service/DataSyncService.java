package com.ticketsystem.show.service;

import com.ticketsystem.show.mapper.SeatMapper;
import com.ticketsystem.show.mapper.TicketStockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 数据同步服务
 * 负责Redis与数据库之间的数据同步
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataSyncService {

    private final RedissonClient redissonClient;
    private final TicketStockMapper ticketStockMapper;
    private final SeatMapper seatMapper;

    // 键前缀
    private static final String STOCK_KEY_PREFIX = "ticket_stock:";
    private static final String SEAT_LOCK_PREFIX = "seat_lock:";
    private static final String SEAT_SYNC_PREFIX = "seat_sync:";

    /**
     * 异步同步库存到数据库
     */
    @Async
    public void syncStockToDatabase(Long ticketId) {
        try {
            String stockKey = STOCK_KEY_PREFIX + ticketId;
            Object remainStockObj = redissonClient.getBucket(stockKey).get();
            
            if (remainStockObj != null) {
                Integer stock;
                if (remainStockObj instanceof String) {
                    stock = Integer.parseInt((String) remainStockObj);
                } else if (remainStockObj instanceof Integer) {
                    stock = (Integer) remainStockObj;
                } else {
                    stock = Integer.parseInt(remainStockObj.toString());
                }
                ticketStockMapper.updateRemainStock(ticketId, stock);
                log.info("异步同步库存到数据库，票档ID: {}, 库存: {}", ticketId, stock);
            }
        } catch (Exception e) {
            log.error("异步同步库存到数据库失败，票档ID: {}", ticketId, e);
        }
    }

    /**
     * 异步同步座位锁定到数据库
     */
    @Async
    public void syncSeatLockToDatabase(Long seatId, Long userId) {
        try {
            String lockKey = SEAT_LOCK_PREFIX + seatId;
            String lockInfoKey = lockKey + ":info";
            
            // 检查RLock是否存在
            if (redissonClient.getLock(lockKey).isLocked()) {
                // 从锁定信息中获取用户ID
                Map<Object, Object> lockInfo = redissonClient.getMap(lockInfoKey);
                if (lockInfo != null && !lockInfo.isEmpty()) {
                    Object userIdObj = lockInfo.get("userId");
                    if (userIdObj != null) {
                        Long lockUserId = Long.parseLong(userIdObj.toString());
                        seatMapper.updateLockStatus(seatId, 1, lockUserId);
                        log.info("异步同步座位锁定到数据库，座位ID: {}, 用户ID: {}", seatId, lockUserId);
                    }
                } else {
                    // 如果锁定信息不存在，使用传入的用户ID
                    seatMapper.updateLockStatus(seatId, 1, userId);
                    log.info("异步同步座位锁定到数据库（使用传入用户ID），座位ID: {}, 用户ID: {}", seatId, userId);
                }
            } else {
                log.warn("座位锁定已过期或不存在，座位ID: {}", seatId);
            }
        } catch (Exception e) {
            log.error("异步同步座位锁定到数据库失败，座位ID: {}, 用户ID: {}", seatId, userId, e);
        }
    }

    /**
     * 异步同步座位解锁到数据库
     */
    @Async
    public void syncSeatUnlockToDatabase(Long seatId) {
        try {
            seatMapper.updateLockStatus(seatId, 0, null);
            log.info("异步同步座位解锁到数据库，座位ID: {}", seatId);
        } catch (Exception e) {
            log.error("异步同步座位解锁到数据库失败，座位ID: {}", seatId, e);
        }
    }

    /**
     * 批量同步库存到数据库
     */
    @Async
    public void batchSyncStockToDatabase(List<Long> ticketIds) {
        try {
            for (Long ticketId : ticketIds) {
                syncStockToDatabase(ticketId);
            }
            log.info("批量同步库存到数据库完成，票档数量: {}", ticketIds.size());
        } catch (Exception e) {
            log.error("批量同步库存到数据库失败", e);
        }
    }

    /**
     * 批量同步座位锁定到数据库
     */
    @Async
    public void batchSyncSeatLockToDatabase(List<Long> seatIds, Long userId) {
        try {
            for (Long seatId : seatIds) {
                syncSeatLockToDatabase(seatId, userId);
            }
            log.info("批量同步座位锁定到数据库完成，座位数量: {}", seatIds.size());
        } catch (Exception e) {
            log.error("批量同步座位锁定到数据库失败", e);
        }
    }

    /**
     * 检查数据一致性
     */
    public boolean checkDataConsistency(Long ticketId) {
        try {
            // 检查Redis库存与数据库库存是否一致
            String stockKey = STOCK_KEY_PREFIX + ticketId;
            Object redisStockObj = redissonClient.getBucket(stockKey).get();
            
            if (redisStockObj != null) {
                Integer redisStockInt;
                if (redisStockObj instanceof String) {
                    redisStockInt = Integer.parseInt((String) redisStockObj);
                } else if (redisStockObj instanceof Integer) {
                    redisStockInt = (Integer) redisStockObj;
                } else {
                    redisStockInt = Integer.parseInt(redisStockObj.toString());
                }
                
                Integer dbStock = ticketStockMapper.getRemainStock(ticketId);
                
                boolean consistent = redisStockInt.equals(dbStock);
                log.info("数据一致性检查，票档ID: {}, Redis库存: {}, 数据库库存: {}, 一致: {}", 
                        ticketId, redisStockInt, dbStock, consistent);
                return consistent;
            }
            return true;
        } catch (Exception e) {
            log.error("数据一致性检查失败，票档ID: {}", ticketId, e);
            return false;
        }
    }

    /**
     * 修复数据不一致
     */
    public void repairDataInconsistency(Long ticketId) {
        try {
            // 以数据库为准，同步到Redis
            Integer dbStock = ticketStockMapper.getRemainStock(ticketId);
            if (dbStock != null) {
                String stockKey = STOCK_KEY_PREFIX + ticketId;
                redissonClient.getBucket(stockKey).set(dbStock);
                log.info("修复数据不一致，票档ID: {}, 数据库库存: {}", ticketId, dbStock);
            }
        } catch (Exception e) {
            log.error("修复数据不一致失败，票档ID: {}", ticketId, e);
        }
    }

    /**
     * 初始化所有库存到Redis
     */
    public void initAllStockToRedis() {
        try {
            List<Long> ticketIds = ticketStockMapper.getAllTicketIds();
            for (Long ticketId : ticketIds) {
                Integer remainStock = ticketStockMapper.getRemainStock(ticketId);
                if (remainStock != null) {
                    String stockKey = STOCK_KEY_PREFIX + ticketId;
                    redissonClient.getBucket(stockKey).set(remainStock);
                }
            }
            log.info("初始化所有库存到Redis完成，票档数量: {}", ticketIds.size());
        } catch (Exception e) {
            log.error("初始化所有库存到Redis失败", e);
        }
    }
}