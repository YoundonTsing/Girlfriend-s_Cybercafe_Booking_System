package com.ticketsystem.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单项VO
 */
@Data
public class OrderItemVO {
    
    private Long id;
    
    private Long orderId;
    
    private String orderNo;
    
    private Long showId;
    
    private String showTitle;
    
    private Long sessionId;
    
    private LocalDateTime sessionTime;
    
    private Long venueId;
    
    private String venueName;
    
    private Long ticketTypeId;
    
    private String ticketTypeName;
    
    private Long seatId;
    
    private String seatInfo;
    
    private BigDecimal price;
    
    private Integer quantity;
    
    private BigDecimal subtotal;
}