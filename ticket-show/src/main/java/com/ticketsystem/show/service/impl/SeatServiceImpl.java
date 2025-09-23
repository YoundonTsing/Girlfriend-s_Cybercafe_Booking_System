package com.ticketsystem.show.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticketsystem.show.entity.Seat;
import com.ticketsystem.show.entity.SeatArea;
import com.ticketsystem.show.mapper.SeatMapper;
import com.ticketsystem.show.mapper.SeatAreaMapper;
import com.ticketsystem.show.service.SeatService;
import com.ticketsystem.show.service.impl.SeatConsistencyService;
import com.ticketsystem.show.service.AtomicSeatLockService;
import com.ticketsystem.show.service.RedissonSeatLockService;
import com.ticketsystem.show.service.DataSyncService;
import com.ticketsystem.show.vo.SeatAreaVO;
import com.ticketsystem.show.vo.SeatLayoutVO;
import com.ticketsystem.show.vo.SeatVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 座位服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeatServiceImpl implements SeatService {

    private final SeatMapper seatMapper;
    private final SeatAreaMapper seatAreaMapper;
    private final SeatConsistencyService seatConsistencyService;
    private final AtomicSeatLockService atomicSeatLockService;
    private final RedissonSeatLockService redissonSeatLockService;
    private final DataSyncService dataSyncService;

    @Override
    public List<SeatAreaVO> getAvailableAreas(Integer showType, Long showId) {
        log.info("获取座位区域列表，机位类型: {}, 演出ID: {}", showType, showId);
        
        try {
            // 使用SeatAreaMapper根据机位类型获取有权限的座位区域
            List<SeatArea> seatAreas = seatAreaMapper.selectAreasByShowType(showType);
            log.info("根据机位类型{}查询到的座位区域数量: {}", showType, seatAreas.size());
            
            // 转换为SeatAreaVO并设置座位统计信息
            List<SeatAreaVO> areas = seatAreas.stream()
                    .map(seatArea -> {
                        SeatAreaVO areaVO = new SeatAreaVO();
                        areaVO.setId(seatArea.getId());
                        areaVO.setName(seatArea.getName());
                        areaVO.setAreaCode(seatArea.getAreaCode());
                        areaVO.setFloorLevel(seatArea.getFloorLevel());
                        areaVO.setAreaType(seatArea.getAreaType());
                        areaVO.setPrice(seatArea.getPrice());
                        areaVO.setNightPrice(seatArea.getPrice().add(seatArea.getNightPriceAddon()));
                        
                        // 查询该区域的座位统计信息
                        int totalSeats = seatMapper.countAvailableSeatsByAreaId(seatArea.getId());
                        areaVO.setTotalSeats(totalSeats);
                        areaVO.setAvailableSeats(totalSeats); // 简化处理，实际可用座位数
                        areaVO.setSelectable(totalSeats > 0);
                        
                        return areaVO;
                    })
                    .collect(Collectors.toList());
            
            log.info("返回座位区域数量: {}", areas.size());
            return areas;
            
        } catch (Exception e) {
            log.error("获取座位区域列表失败，机位类型: {}, 演出ID: {}", showType, showId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public SeatLayoutVO getSeatLayout(Long areaId, Long showId, Long sessionId, Long currentUserId) {
        log.info("获取座位布局，区域ID: {}, 演出ID: {}, 场次ID: {}, 当前用户ID: {}", 
                 areaId, showId, sessionId, currentUserId);
        
        // 查询座位数据
        LambdaQueryWrapper<Seat> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Seat::getAreaId, areaId)
                   .eq(Seat::getIsDeleted, 0)
                   .orderByAsc(Seat::getRowNum, Seat::getSeatNum);
        
        List<Seat> seats = seatMapper.selectList(queryWrapper);
        log.info("查询到座位数量: {}", seats.size());
        
        // 转换为VO
        List<SeatVO> seatVOs = seats.stream().map((Seat seat) -> {
            SeatVO vo = new SeatVO();
            BeanUtils.copyProperties(seat, vo);
            
            // 设置锁定状态（重要：确保lockStatus字段被正确设置）
            vo.setLockStatus(seat.getLockStatus());
            
            // 设置锁定时间（转换为字符串）
            if (seat.getLockTime() != null) {
                vo.setLockTime(seat.getLockTime().toString());
            }
            if (seat.getLockExpireTime() != null) {
                vo.setLockExpireTime(seat.getLockExpireTime().toString());
            }
            
            // 设置是否为当前用户锁定
            if (currentUserId != null && seat.getLockUserId() != null) {
                vo.setLockedByCurrentUser(seat.getLockUserId().equals(currentUserId));
            } else {
                vo.setLockedByCurrentUser(false);
            }
            
            return vo;
        }).collect(Collectors.toList());
        
        // 构建返回结果
        SeatLayoutVO layoutVO = new SeatLayoutVO();
        layoutVO.setSeats(seatVOs);
        layoutVO.setTotalSeats(seats.size());
        layoutVO.setAvailableSeats((int) seatVOs.stream().filter(vo -> vo.getLockStatus() == 0).count());
        
        log.info("座位布局构建完成，总座位数: {}, 可用座位数: {}", 
                 layoutVO.getTotalSeats(), layoutVO.getAvailableSeats());
        
        return layoutVO;
    }

    @Override
    @Transactional
    public boolean lockSeats(List<Long> seatIds, Long userId) {
        log.info("开始锁定座位，用户ID: {}, 座位IDs: {}", userId, seatIds);
        
        try {
            // 检查服务是否可用
            if (redissonSeatLockService == null) {
                log.error("RedissonSeatLockService未注入，使用数据库锁定");
                return lockSeatsDatabaseOnly(seatIds, userId);
            }
            
            if (dataSyncService == null) {
                log.error("DataSyncService未注入，使用数据库锁定");
                return lockSeatsDatabaseOnly(seatIds, userId);
            }
            
            // 使用Redisson RLock原子化锁定
            log.info("尝试使用Redisson RLock原子化锁定座位");
            Map<Long, Boolean> results = redissonSeatLockService.atomicLockSeats(seatIds, userId);
            log.info("Redisson RLock锁定结果: {}", results);
            
            // 检查是否所有座位都锁定成功
            boolean allSuccess = results.values().stream().allMatch(Boolean::booleanValue);
            
            if (allSuccess) {
                // 异步同步到数据库
                dataSyncService.batchSyncSeatLockToDatabase(seatIds, userId);
                log.info("座位锁定成功，用户ID: {}, 座位IDs: {}", userId, seatIds);
            } else {
                // 部分失败，释放已锁定的座位
                for (Map.Entry<Long, Boolean> entry : results.entrySet()) {
                    if (entry.getValue()) {
                        redissonSeatLockService.atomicUnlockSeat(entry.getKey(), userId);
                    }
                }
                log.warn("座位锁定部分失败，用户ID: {}, 座位IDs: {}, 结果: {}", userId, seatIds, results);
            }
            
            return allSuccess;
        } catch (Exception e) {
            log.error("Redisson RLock锁定失败，回退到数据库锁定，用户ID: {}, 座位IDs: {}", userId, seatIds, e);
            // 回退到数据库锁定
            return lockSeatsDatabaseOnly(seatIds, userId);
        }
    }

    @Override
    @Transactional
    public boolean releaseSeats(List<Long> seatIds, Long userId) {
        log.info("开始释放座位锁定，用户ID: {}, 座位IDs: {}", userId, seatIds);
        
        try {
            // 使用Redisson RLock原子化解锁
            Map<Long, Boolean> results = redissonSeatLockService.atomicUnlockSeats(seatIds, userId);
            
            // 检查是否所有座位都解锁成功
            boolean allSuccess = results.values().stream().allMatch(Boolean::booleanValue);
            
            if (allSuccess) {
                // 异步同步到数据库
                for (Long seatId : seatIds) {
                    dataSyncService.syncSeatUnlockToDatabase(seatId);
                }
                log.info("座位解锁成功，用户ID: {}, 座位IDs: {}", userId, seatIds);
            } else {
                log.warn("座位解锁部分失败，用户ID: {}, 座位IDs: {}, 结果: {}", userId, seatIds, results);
            }
            
            return allSuccess;
        } catch (Exception e) {
            log.error("释放座位锁定失败，用户ID: {}, 座位IDs: {}", userId, seatIds, e);
            return false;
        }
    }

    @Override
    public void clearExpiredLocks() {
        log.info("开始清理过期锁定");
        
        try {
            int clearedCount = seatMapper.clearExpiredLocks();
            log.info("清理过期锁定完成，清理数量: {}", clearedCount);
        } catch (Exception e) {
            log.error("清理过期锁定失败", e);
        }
    }

    /**
     * 临时解决方案：直接数据库锁定座位
     */
    @Transactional
    public boolean lockSeatsDatabaseOnly(List<Long> seatIds, Long userId) {
        log.info("直接数据库锁定座位，用户ID: {}, 座位IDs: {}", userId, seatIds);
        
        try {
            // 1. 检查座位是否可锁定
            for (Long seatId : seatIds) {
                Seat seat = seatMapper.selectById(seatId);
                if (seat == null) {
                    log.warn("座位不存在，座位ID: {}", seatId);
                    return false;
                }
                
                if (seat.getLockStatus() != 0 || seat.getStatus() != 1 || seat.getIsDeleted() != 0) {
                    log.warn("座位不可锁定，座位ID: {}, 状态: {}, 锁定状态: {}, 删除状态: {}", 
                             seatId, seat.getStatus(), seat.getLockStatus(), seat.getIsDeleted());
                    return false;
                }
            }
            
            // 2. 执行数据库锁定
            int lockedCount = seatMapper.lockSeats(seatIds, userId);
            
            if (lockedCount != seatIds.size()) {
                log.warn("数据库锁定失败，期望锁定: {}, 实际锁定: {}", seatIds.size(), lockedCount);
                return false;
            }
            
            log.info("数据库锁定成功，用户ID: {}, 座位IDs: {}, 锁定数量: {}", userId, seatIds, lockedCount);
            return true;
            
        } catch (Exception e) {
            log.error("数据库锁定异常，用户ID: {}, 座位IDs: {}", userId, seatIds, e);
            return false;
        }
    }

    /**
     * 临时解决方案：直接数据库释放座位
     */
    @Transactional
    public boolean releaseSeatsDatabaseOnly(List<Long> seatIds, Long userId) {
        log.info("直接数据库释放座位，用户ID: {}, 座位IDs: {}", userId, seatIds);
        
        try {
            int releasedCount = seatMapper.releaseSeats(seatIds, userId);
            
            if (releasedCount != seatIds.size()) {
                log.warn("数据库释放失败，期望释放: {}, 实际释放: {}", seatIds.size(), releasedCount);
                return false;
            }
            
            log.info("数据库释放成功，用户ID: {}, 座位IDs: {}, 释放数量: {}", userId, seatIds, releasedCount);
            return true;
            
        } catch (Exception e) {
            log.error("数据库释放异常，用户ID: {}, 座位IDs: {}", userId, seatIds, e);
            return false;
        }
    }

    /**
     * 计算座位状态
     */
    private int calculateSeatStatus(Seat seat) {
        if (seat.getStatus() == 0) {
            return 0; // 维护中
        }
        
        if (seat.getLockStatus() == 1) {
            return 1; // 已锁定
        }
        
        if (seat.getLockStatus() == 2) {
            return 2; // 已占用
        }
        
        return 0; // 空闲
    }
}