package com.ticketsystem.order.service;

import java.util.List;

/**
 * 座位状态同步服务接口
 */
public interface SeatStatusSyncService {

    /**
     * 同步座位状态到场馆系统
     * @param seatIds 座位ID列表
     * @param sessionId 场次ID
     * @param status 状态：1-可用，2-锁定，3-已售
     * @return 是否同步成功
     */
    boolean syncSeatStatusToVenue(List<Long> seatIds, Long sessionId, Integer status);

    /**
     * 从场馆系统获取座位状态
     * @param seatIds 座位ID列表
     * @param sessionId 场次ID
     * @return 座位状态映射 Map<seatId, status>
     */
    java.util.Map<Long, Integer> getSeatStatusFromVenue(List<Long> seatIds, Long sessionId);

    /**
     * 批量同步订单相关座位状态
     * @param orderNo 订单号
     * @param status 状态
     * @return 是否同步成功
     */
    boolean syncOrderSeatStatus(String orderNo, Integer status);

    /**
     * 检查并修复座位状态不一致
     * @param sessionId 场次ID
     * @return 修复的座位数量
     */
    int checkAndRepairSeatStatus(Long sessionId);

    /**
     * 实时推送座位状态变更
     * @param seatId 座位ID
     * @param sessionId 场次ID
     * @param oldStatus 原状态
     * @param newStatus 新状态
     */
    void pushSeatStatusChange(Long seatId, Long sessionId, Integer oldStatus, Integer newStatus);

    /**
     * 订阅座位状态变更事件
     * @param sessionId 场次ID
     * @param callback 回调处理器
     */
    void subscribeSeatStatusChange(Long sessionId, SeatStatusChangeCallback callback);

    /**
     * 座位状态变更回调接口
     */
    interface SeatStatusChangeCallback {
        void onStatusChange(Long seatId, Long sessionId, Integer oldStatus, Integer newStatus);
    }

    /**
     * 座位状态枚举
     */
    enum SeatStatus {
        AVAILABLE(1, "可用"),
        LOCKED(2, "锁定"),
        SOLD(3, "已售");

        private final Integer code;
        private final String desc;

        SeatStatus(Integer code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public Integer getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }
    }
}