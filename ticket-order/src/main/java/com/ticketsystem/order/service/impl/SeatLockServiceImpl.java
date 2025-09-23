package com.ticketsystem.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ticketsystem.order.entity.SeatLock;
import com.ticketsystem.order.mapper.SeatLockMapper;
import com.ticketsystem.order.service.SeatLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 座位锁定服务实现类
 */
@Slf4j
@Service
public class SeatLockServiceImpl implements SeatLockService {

    @Autowired
    private SeatLockMapper seatLockMapper;

    @Override
    @Transactional
    public boolean lockSeat(Long seatId, Long sessionId, Long venueId, Long userId, Integer lockType, Integer expireMinutes) {
        try {
            // 检查座位是否已被锁定
            if (!isSeatAvailable(seatId, sessionId)) {
                log.warn("座位已被锁定，seatId: {}, sessionId: {}", seatId, sessionId);
                return false;
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expireTime = now.plusMinutes(expireMinutes);

            SeatLock seatLock = new SeatLock()
                    .setSeatId(seatId)
                    .setSessionId(sessionId)
                    .setVenueId(venueId)
                    .setUserId(userId)
                    .setLockType(lockType)
                    .setLockTime(now)
                    .setExpireTime(expireTime)
                    .setStatus(SeatLock.Status.LOCKING.getCode());

            int result = seatLockMapper.insert(seatLock);
            log.info("座位锁定成功，seatId: {}, sessionId: {}, userId: {}", seatId, sessionId, userId);
            return result > 0;
        } catch (Exception e) {
            log.error("座位锁定失败，seatId: {}, sessionId: {}, userId: {}", seatId, sessionId, userId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean releaseSeatLock(Long seatId, Long sessionId, Long userId) {
        try {
            LambdaUpdateWrapper<SeatLock> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(SeatLock::getSeatId, seatId)
                    .eq(SeatLock::getSessionId, sessionId)
                    .eq(SeatLock::getUserId, userId)
                    .eq(SeatLock::getStatus, SeatLock.Status.LOCKING.getCode())
                    .set(SeatLock::getStatus, SeatLock.Status.RELEASED.getCode())
                    .set(SeatLock::getUpdateTime, LocalDateTime.now());

            int result = seatLockMapper.update(null, updateWrapper);
            log.info("座位锁定释放成功，seatId: {}, sessionId: {}, userId: {}", seatId, sessionId, userId);
            return result > 0;
        } catch (Exception e) {
            log.error("座位锁定释放失败，seatId: {}, sessionId: {}, userId: {}", seatId, sessionId, userId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean confirmSeatLock(Long seatId, Long sessionId, Long userId) {
        try {
            LambdaUpdateWrapper<SeatLock> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(SeatLock::getSeatId, seatId)
                    .eq(SeatLock::getSessionId, sessionId)
                    .eq(SeatLock::getUserId, userId)
                    .eq(SeatLock::getStatus, SeatLock.Status.LOCKING.getCode())
                    .set(SeatLock::getStatus, SeatLock.Status.CONFIRMED.getCode())
                    .set(SeatLock::getUpdateTime, LocalDateTime.now());

            int result = seatLockMapper.update(null, updateWrapper);
            log.info("座位锁定确认成功，seatId: {}, sessionId: {}, userId: {}", seatId, sessionId, userId);
            return result > 0;
        } catch (Exception e) {
            log.error("座位锁定确认失败，seatId: {}, sessionId: {}, userId: {}", seatId, sessionId, userId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public List<Long> batchLockSeats(List<Long> seatIds, Long sessionId, Long venueId, Long userId, Integer lockType, Integer expireMinutes) {
        List<Long> successSeatIds = new ArrayList<>();
        
        for (Long seatId : seatIds) {
            if (lockSeat(seatId, sessionId, venueId, userId, lockType, expireMinutes)) {
                successSeatIds.add(seatId);
            }
        }
        
        log.info("批量锁定座位完成，总数: {}, 成功: {}, sessionId: {}, userId: {}", 
                seatIds.size(), successSeatIds.size(), sessionId, userId);
        return successSeatIds;
    }

    @Override
    @Transactional
    public boolean batchReleaseSeatLock(List<Long> seatIds, Long sessionId, Long userId) {
        boolean allSuccess = true;
        
        for (Long seatId : seatIds) {
            if (!releaseSeatLock(seatId, sessionId, userId)) {
                allSuccess = false;
            }
        }
        
        log.info("批量释放座位锁定完成，总数: {}, 全部成功: {}, sessionId: {}, userId: {}", 
                seatIds.size(), allSuccess, sessionId, userId);
        return allSuccess;
    }

    @Override
    public boolean isSeatAvailable(Long seatId, Long sessionId) {
        LambdaQueryWrapper<SeatLock> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SeatLock::getSeatId, seatId)
                .eq(SeatLock::getSessionId, sessionId)
                .eq(SeatLock::getStatus, SeatLock.Status.LOCKING.getCode())
                .gt(SeatLock::getExpireTime, LocalDateTime.now());

        Long count = seatLockMapper.selectCount(queryWrapper);
        return count == 0;
    }

    @Override
    public List<SeatLock> getUserLockedSeats(Long userId, Long sessionId) {
        LambdaQueryWrapper<SeatLock> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SeatLock::getUserId, userId)
                .eq(SeatLock::getSessionId, sessionId)
                .eq(SeatLock::getStatus, SeatLock.Status.LOCKING.getCode())
                .gt(SeatLock::getExpireTime, LocalDateTime.now())
                .orderByDesc(SeatLock::getCreateTime);

        return seatLockMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional
    public int cleanExpiredLocks() {
        try {
            LambdaUpdateWrapper<SeatLock> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(SeatLock::getStatus, SeatLock.Status.LOCKING.getCode())
                    .lt(SeatLock::getExpireTime, LocalDateTime.now())
                    .set(SeatLock::getStatus, SeatLock.Status.RELEASED.getCode())
                    .set(SeatLock::getUpdateTime, LocalDateTime.now());

            int result = seatLockMapper.update(null, updateWrapper);
            log.info("清理过期座位锁定完成，清理数量: {}", result);
            return result;
        } catch (Exception e) {
            log.error("清理过期座位锁定失败", e);
            return 0;
        }
    }

    @Override
    @Transactional
    public boolean extendSeatLock(Long seatId, Long sessionId, Long userId, Integer extendMinutes) {
        try {
            LambdaUpdateWrapper<SeatLock> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(SeatLock::getSeatId, seatId)
                    .eq(SeatLock::getSessionId, sessionId)
                    .eq(SeatLock::getUserId, userId)
                    .eq(SeatLock::getStatus, SeatLock.Status.LOCKING.getCode())
                    .setSql("expire_time = DATE_ADD(expire_time, INTERVAL " + extendMinutes + " MINUTE)")
                    .set(SeatLock::getUpdateTime, LocalDateTime.now());

            int result = seatLockMapper.update(null, updateWrapper);
            log.info("延长座位锁定时间成功，seatId: {}, sessionId: {}, userId: {}, extendMinutes: {}", 
                    seatId, sessionId, userId, extendMinutes);
            return result > 0;
        } catch (Exception e) {
            log.error("延长座位锁定时间失败，seatId: {}, sessionId: {}, userId: {}", seatId, sessionId, userId, e);
            return false;
        }
    }
}