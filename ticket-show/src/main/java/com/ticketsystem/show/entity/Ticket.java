package com.ticketsystem.show.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 票档信息实体类
 */
@Data
@TableName("t_ticket")
public class Ticket {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的演出ID
     */
    private Long showId;

    /**
     * 关联的场次ID
     */
    private Long sessionId;

    /**
     * 票档名称（如：VIP区、普通区、学生票等）
     */
    private String name;

    /**
     * 票档价格
     */
    private BigDecimal price;

    /**
     * 总票数
     */
    @TableField("total_count")
    private Integer totalCount;

    /**
     * 剩余票数
     */
    @TableField("remain_count")
    private Integer remainCount;

    /**
     * 限购数量
     */
    @TableField("limit_count")
    private Integer limitCount;

    /**
     * 票档状态：0-未开售，1-售票中，2-已售罄，3-停售
     */
    private Integer status;

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