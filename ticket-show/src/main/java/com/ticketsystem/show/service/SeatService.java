package com.ticketsystem.show.service;

import com.ticketsystem.show.vo.SeatAreaVO;
import com.ticketsystem.show.vo.SeatLayoutVO;

import java.util.List;

/**
 * 座位服务接口
 */
public interface SeatService {

    /**
     * 根据机位类型获取可访问的座位区域
     */
    List<SeatAreaVO> getAvailableAreas(Integer showType, Long showId);

    /**
     * 获取座位布局
     */
    SeatLayoutVO getSeatLayout(Long areaId, Long showId, Long sessionId, Long currentUserId);

    /**
     * 锁定座位
     */
    boolean lockSeats(List<Long> seatIds, Long userId);

    /**
     * 释放座位锁定
     */
    boolean releaseSeats(List<Long> seatIds, Long userId);

    /**
     * 清理过期锁定（定时任务调用）
     */
    void clearExpiredLocks();
}