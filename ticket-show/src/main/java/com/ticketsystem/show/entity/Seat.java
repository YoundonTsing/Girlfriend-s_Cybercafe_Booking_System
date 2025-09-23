package com.ticketsystem.show.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 座位实体类
 */
@Data
@TableName("t_seat")
public class Seat {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 座位区域ID
     */
    private Long areaId;

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
     * 座位类型：1-普通，2-VIP，3-SVIP
     */
    private Integer seatType;

    /**
     * 座位状态：0-维护中，1-可用
     */
    private Integer status;

    /**
     * 锁定状态：0-空闲，1-已锁定，2-已占用
     */
    private Integer lockStatus;

    /**
     * 锁定时间
     */
    private LocalDateTime lockTime;

    /**
     * 锁定用户ID
     */
    private Long lockUserId;

    /**
     * 锁定过期时间
     */
    private LocalDateTime lockExpireTime;

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