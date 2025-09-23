package com.ticketsystem.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类
 */
@Data
@TableName("t_order")
public class Order {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 演出ID
     */
    private Long showId;

    /**
     * 场次ID
     */
    private Long sessionId;

    /**
     * 票档ID
     */
    private Long ticketId;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 实际支付金额
     */
    private BigDecimal payAmount;

    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;

    /**
     * 订单状态：0-待支付，1-已支付，2-已取消，3-已完成，4-已退款
     */
    private Integer status;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 支付方式：1-支付宝，2-微信，3-银行卡
     */
    private Integer payType;

    /**
     * 支付流水号
     */
    private String payNo;

    /**
     * 订单过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 备注信息
     */
    private String remark;

    /**
     * 预约时间（网咖场景）
     */
    private LocalDateTime bookingDate;

    /**
     * 预约结束时间（网咖场景）
     */
    private LocalDateTime bookingEndTime;

    /**
     * 预约时长（小时）（网咖场景）
     */
    private Integer bookingDuration;

    /**
     * 联系电话（网咖场景）
     */
    private String contactPhone;

    /**
     * 是否需要选座：0-不需要，1-需要
     */
    private Integer needSeatSelection;

    /**
     * 座位选择状态：0-未选座，1-已选座，2-座位已锁定
     */
    private Integer seatSelectionStatus;

    /**
     * 选座截止时间
     */
    private LocalDateTime seatSelectionDeadline;

    /**
     * 座位锁定时间
     */
    private LocalDateTime seatLockTime;

    /**
     * 座位锁定过期时间
     */
    private LocalDateTime seatLockExpireTime;

    /**
     * 座位ID（单个座位场景）
     */
    private Long seatId;

    /**
     * 座位信息（座位描述，如"A区1排3座"）
     */
    private String seatInfo;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer isDeleted;
}