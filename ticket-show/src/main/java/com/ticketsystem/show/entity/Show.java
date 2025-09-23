package com.ticketsystem.show.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 演出信息实体类
 */
@Data
@TableName("t_show")
public class Show {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 演出名称
     */
    private String name;

    /**
     * 演出类型：1-演唱会，2-话剧，3-音乐会，4-展览，5-体育赛事
     */
    private Integer type;

    /**
     * 演出海报图片URL
     */
    private String posterUrl;

    /**
     * 演出详情描述
     */
    private String description;

    /**
     * 演出地点
     */
    private String venue;

    /**
     * 演出城市
     */
    private String city;

    /**
     * 演出开始时间
     */
    private LocalDateTime startTime;

    /**
     * 演出结束时间
     */
    private LocalDateTime endTime;

    /**
     * 最低票价
     */
    private BigDecimal minPrice;

    /**
     * 最高票价
     */
    private BigDecimal maxPrice;

    /**
     * 演出状态：0-未开售，1-售票中，2-已售罄，3-已结束
     */
    private Integer status;

    /**
     * 是否热门：0-否，1-是
     */
    private Integer isHot;

    /**
     * 是否推荐：0-否，1-是
     */
    private Integer isRecommend;

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