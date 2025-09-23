package com.ticketsystem.show.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 座位区域VO
 */
@Data
public class SeatAreaVO {

    /**
     * 区域ID
     */
    private Long id;

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
     * 区域类型
     */
    private String areaType;

    /**
     * 区域类型名称
     */
    private String areaTypeName;

    /**
     * 基础价格
     */
    private BigDecimal price;

    /**
     * 深夜时段价格
     */
    private BigDecimal nightPrice;

    /**
     * 总座位数
     */
    private Integer totalSeats;

    /**
     * 可用座位数
     */
    private Integer availableSeats;

    /**
     * 描述信息
     */
    private String description;

    /**
     * 是否可选（基于权限判断）
     */
    private Boolean selectable;
}