package com.ticketsystem.show.vo;

import lombok.Data;

import java.util.List;

/**
 * 座位布局VO
 */
@Data
public class SeatLayoutVO {

    /**
     * 区域ID
     */
    private Long areaId;

    /**
     * 区域名称
     */
    private String areaName;

    /**
     * 楼层
     */
    private Integer floorLevel;

    /**
     * 区域类型
     */
    private String areaType;

    /**
     * 总行数
     */
    private Integer totalRows;

    /**
     * 总列数
     */
    private Integer totalCols;

    /**
     * 座位列表
     */
    private List<SeatVO> seats;

    /**
     * 总座位数
     */
    private Integer totalSeats;

    /**
     * 可用座位数
     */
    private Integer availableSeats;

    /**
     * 布局配置（JSON格式，用于前端渲染）
     */
    private String layoutConfig;

    /**
     * 区域描述
     */
    private String description;
}