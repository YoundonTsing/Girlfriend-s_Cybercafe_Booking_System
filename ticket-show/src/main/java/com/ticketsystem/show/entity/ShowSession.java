package com.ticketsystem.show.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 演出场次实体类
 */
@Data
@TableName("t_show_session")
public class ShowSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的演出ID
     */
    private Long showId;

    /**
     * 场次名称
     */
    private String name;

    /**
     * 场次开始时间
     */
    private LocalDateTime startTime;

    /**
     * 场次结束时间
     */
    private LocalDateTime endTime;

    /**
     * 场次状态：0-未开售，1-售票中，2-已售罄，3-已结束
     */
    private Integer status;

    /**
     * 总座位数
     */
    private Integer totalSeats;

    /**
     * 已售座位数
     */
    private Integer soldSeats;

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