package com.ticketsystem.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 座位锁定实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_seat_lock")
public class SeatLock implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 座位ID
     */
    @TableField("seat_id")
    private Long seatId;

    /**
     * 场次ID
     */
    @TableField("session_id")
    private Long sessionId;

    /**
     * 场馆ID
     */
    @TableField("venue_id")
    private Long venueId;

    /**
     * 锁定用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 锁定类型：1-选座锁定，2-订单锁定
     */
    @TableField("lock_type")
    private Integer lockType;

    /**
     * 锁定时间
     */
    @TableField("lock_time")
    private LocalDateTime lockTime;

    /**
     * 过期时间
     */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /**
     * 状态：1-锁定中，2-已释放，3-已确认
     */
    @TableField("status")
    private Integer status;

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
     * 锁定类型枚举
     */
    public enum LockType {
        SEAT_SELECTION(1, "选座锁定"),
        ORDER_LOCK(2, "订单锁定");

        private final Integer code;
        private final String desc;

        LockType(Integer code, String desc) {
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

    /**
     * 锁定状态枚举
     */
    public enum Status {
        LOCKING(1, "锁定中"),
        RELEASED(2, "已释放"),
        CONFIRMED(3, "已确认");

        private final Integer code;
        private final String desc;

        Status(Integer code, String desc) {
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