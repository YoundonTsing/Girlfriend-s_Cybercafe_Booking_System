package com.ticketsystem.show.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 演出信息DTO
 */
@Data
public class ShowInfoDTO {
    
    private Long showId;
    
    private String showName;
    
    private Long sessionId;
    
    private String sessionName;
    
    private LocalDateTime showTime;
    
    private String venue;
    
    private String city;
}