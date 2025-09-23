package com.ticketsystem.order.service;

import com.ticketsystem.order.entity.SeatLock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 座位锁定服务接口
 */
public interface SeatLockService {

    /**
     * 锁定座位
     * @param seatId 座位ID
     * @param sessionId 场次ID
     * @param venueId 场馆ID
     * @param userId 用户ID
     * @param lockType 锁定类型
     * @param expireMinutes 锁定时长（分钟）
     * @return 是否锁定成功
     */
    boolean lockSeat(Long seatId, Long sessionId, Long venueId, Long userId, Integer lockType, Integer expireMinutes);

    /**
     * 释放座位锁定
     * @param seatId 座位ID
     * @param sessionId 场次ID
     * @param userId 用户ID
     * @return 是否释放成功
     */
    boolean releaseSeatLock(Long seatId, Long sessionId, Long userId);

    /**
     * 确认座位锁定（订单确认时调用）
     * @param seatId 座位ID
     * @param sessionId 场次ID
     * @param userId 用户ID
     * @return 是否确认成功
     */
    boolean confirmSeatLock(Long seatId, Long sessionId, Long userId);

    /**
     * 批量锁定座位
     * @param seatIds 座位ID列表
     * @param sessionId 场次ID
     * @param venueId 场馆ID
     * @param userId 用户ID
     * @param lockType 锁定类型
     * @param expireMinutes 锁定时长（分钟）
     * @return 锁定成功的座位ID列表
     */
    List<Long> batchLockSeats(List<Long> seatIds, Long sessionId, Long venueId, Long userId, Integer lockType, Integer expireMinutes);

    /**
     * 批量释放座位锁定
     * @param seatIds 座位ID列表
     * @param sessionId 场次ID
     * @param userId 用户ID
     * @return 是否全部释放成功
     */
    boolean batchReleaseSeatLock(List<Long> seatIds, Long sessionId, Long userId);

    /**
     * 检查座位是否可用
     * @param seatId 座位ID
     * @param sessionId 场次ID
     * @return 是否可用
     */
    boolean isSeatAvailable(Long seatId, Long sessionId);

    /**
     * 获取用户锁定的座位
     * @param userId 用户ID
     * @param sessionId 场次ID
     * @return 锁定的座位列表
     */
    List<SeatLock> getUserLockedSeats(Long userId, Long sessionId);

    /**
     * 清理过期的座位锁定
     * @return 清理的数量
     */
    int cleanExpiredLocks();

    /**
     * 延长座位锁定时间
     * @param seatId 座位ID
     * @param sessionId 场次ID
     * @param userId 用户ID
     * @param extendMinutes 延长时间（分钟）
     * @return 是否延长成功
     */
    boolean extendSeatLock(Long seatId, Long sessionId, Long userId, Integer extendMinutes);
}