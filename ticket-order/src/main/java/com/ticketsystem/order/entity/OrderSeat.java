package com.ticketsystem.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单座位关联实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_order_seat")
public class OrderSeat implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单ID
     */
    @TableField("order_id")
    private Long orderId;

    /**
     * 订单号
     */
    @TableField("order_no")
    private String orderNo;

    /**
     * 座位ID
     */
    @TableField("seat_id")
    private Long seatId;

    /**
     * 座位行号
     */
    @TableField("seat_row")
    private String seatRow;

    /**
     * 座位列号
     */
    @TableField("seat_col")
    private String seatCol;

    /**
     * 座位区域
     */
    @TableField("seat_area")
    private String seatArea;

    /**
     * 场馆ID
     */
    @TableField("venue_id")
    private Long venueId;

    /**
     * 场次ID
     */
    @TableField("session_id")
    private Long sessionId;

    /**
     * 锁定状态：0-未锁定，1-临时锁定，2-已确认
     */
    @TableField("lock_status")
    private Integer lockStatus;

    /**
     * 锁定时间
     */
    @TableField("lock_time")
    private LocalDateTime lockTime;

    /**
     * 锁定过期时间
     */
    @TableField("lock_expire_time")
    private LocalDateTime lockExpireTime;

    /**
     * 锁定用户ID
     */
    @TableField("lock_user_id")
    private Long lockUserId;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 锁定状态枚举
     */
    public enum LockStatus {
        UNLOCKED(0, "未锁定"),
        TEMP_LOCKED(1, "临时锁定"),
        CONFIRMED(2, "已确认");

        private final Integer code;
        private final String desc;

        LockStatus(Integer code, String desc) {
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