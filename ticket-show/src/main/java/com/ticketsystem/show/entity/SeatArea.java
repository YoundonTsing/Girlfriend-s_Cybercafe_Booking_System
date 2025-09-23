package com.ticketsystem.show.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 座位区域实体类
 */
@Data
@TableName("t_seat_area")
public class SeatArea {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 场馆ID
     */
    private Long venueId;

    /**
     * 区域名称
     */
    private String name;

    /**
     * 区域编码
     */
    private String areaCode;

    /**
     * 楼层
     */
    private Integer floorLevel;

    /**
     * 区域类型：HALL-大厅，VIP_ROOM-包厢，SVIP-SVIP区，CHARTER-包场
     */
    private String areaType;

    /**
     * 允许访问的机位类型，逗号分隔：1,2,3
     */
    private String showTypeAccess;

    /**
     * 基础价格
     */
    private BigDecimal price;

    /**
     * 深夜时段加价
     */
    private BigDecimal nightPriceAddon;

    /**
     * 座位容量
     */
    private Integer capacity;

    /**
     * 最大座位数
     */
    private Integer maxSeats;

    /**
     * X坐标位置
     */
    private Integer xPosition;

    /**
     * Y坐标位置
     */
    private Integer yPosition;

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