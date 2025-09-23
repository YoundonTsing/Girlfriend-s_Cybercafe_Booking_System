package com.ticketsystem.show.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 场次信息VO
 */
@Data
public class SessionVO {
    
    private Long id;
    
    private Long showId;
    
    private String name;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private Integer status;
    
    private String statusName;
    
    private Integer totalSeats;
    
    private Integer soldSeats;
    
    private Integer remainSeats;
}