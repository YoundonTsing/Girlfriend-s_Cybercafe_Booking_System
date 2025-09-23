package com.ticketsystem.order.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 创建订单DTO
 */
@Data
public class CreateOrderDTO {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 演出ID（网咖场景下为机位类型ID）
     */
    @NotNull(message = "演出ID不能为空")
    private Long showId;

    /**
     * 场次ID（网咖场景下为时段ID）
     */
    @NotNull(message = "场次ID不能为空")
    private Long sessionId;

    /**
     * 票档ID（网咖场景下为套餐ID）
     */
    @NotNull(message = "票档ID不能为空")
    private Long ticketId;

    /**
     * 购买数量（网咖场景下为预约机位数量）
     */
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于0")
    private Integer quantity;

    /**
     * 预约时间（网咖场景新增字段）
     */
    private String bookingDate;

    /**
     * 预约结束时间（网咖场景新增字段）
     */
    private String bookingEndTime;

    /**
     * 预约时长（小时）（网咖场景新增字段）
     */
    private Integer bookingDuration;

    /**
     * 联系电话（网咖场景新增字段）
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的手机号码")
    private String contactPhone;

    /**
     * 备注信息（网咖场景新增字段）
     */
    private String remark;

    /**
     * 总价格（网咖场景新增字段，包含夜间加价等）
     */
    private java.math.BigDecimal totalPrice;

    /**
     * 基础价格（网咖场景新增字段）
     */
    private java.math.BigDecimal basePrice;

    /**
     * 夜间加价（网咖场景新增字段）
     */
    private java.math.BigDecimal nightSurcharge;

    /**
     * 座位ID（可选，用于指定座位的场景）
     */
    private Long seatId;
}