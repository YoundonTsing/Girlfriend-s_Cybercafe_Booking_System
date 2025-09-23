package com.ticketsystem.show.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 演出详情VO
 */
@Data
public class ShowDetailVO {
    
    private Long id;
    
    private String name;
    
    private Integer type;
    
    private String typeName;
    
    private String posterUrl;
    
    private String description;
    
    private String venue;
    
    private String city;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private BigDecimal minPrice;
    
    private BigDecimal maxPrice;
    
    private Integer status;
    
    private String statusName;
    
    // 场次信息
    private List<SessionVO> sessions;
}