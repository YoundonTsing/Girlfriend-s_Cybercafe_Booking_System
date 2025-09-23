package com.ticketsystem.show.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 座位VO
 */
@Data
public class SeatVO {

    /**
     * 座位ID
     */
    private Long id;

    /**
     * 座位编码
     */
    private String seatCode;

    /**
     * 排号
     */
    private String rowNum;

    /**
     * 座位号
     */
    private String seatNum;

    /**
     * X坐标
     */
    private Integer xCoordinate;

    /**
     * Y坐标
     */
    private Integer yCoordinate;

    /**
     * 座位类型
     */
    private Integer seatType;

    /**
     * 座位状态：0-维护，1-可选，2-已锁定，3-已占用
     */
    private Integer status;

    /**
     * 锁定状态：0-空闲，1-已锁定，2-已占用
     */
    private Integer lockStatus;

    /**
     * 价格（根据时段计算）
     */
    private BigDecimal price;

    /**
     * 锁定用户ID（当前锁定用户）
     */
    private Long lockUserId;

    /**
     * 锁定时间
     */
    private String lockTime;

    /**
     * 锁定过期时间
     */
    private String lockExpireTime;

    /**
     * 是否为当前用户锁定
     */
    private Boolean lockedByCurrentUser;
}