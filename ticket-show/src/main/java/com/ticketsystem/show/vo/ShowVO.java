package com.ticketsystem.show.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 演出列表展示VO
 */
@Data
public class ShowVO {
    
    private Long id;
    
    private String name;
    
    private Integer type;
    
    private String typeName;
    
    private String posterUrl;
    
    private String venue;
    
    private String city;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private BigDecimal minPrice;
    
    private BigDecimal maxPrice;
    
    private Integer status;
    
    private String statusName;
    
    private Integer isHot;
    
    private Integer isRecommend;
}