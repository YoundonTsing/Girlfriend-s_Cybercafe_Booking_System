package com.ticketsystem.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单VO
 */
@Data
public class OrderVO {
    
    private Long id;
    
    private String orderNo;
    
    private Long userId;
    
    private Long showId;
    
    private String showName;
    
    private Long sessionId;
    
    private String sessionName;
    
    private LocalDateTime showTime;
    
    private String venue;
    
    private Long ticketId;
    
    private String ticketName;
    
    private BigDecimal price;
    
    private Integer quantity;
    
    private BigDecimal totalAmount;
    
    private BigDecimal payAmount;
    
    private BigDecimal discountAmount;
    
    private Integer status;
    
    private String statusName;
    
    private LocalDateTime payTime;
    
    private Integer payType;
    
    private String payTypeName;
    
    private String payNo;
    
    private LocalDateTime expireTime;
    
    private LocalDateTime createTime;
    
    private String remark;
    
    private LocalDateTime bookingDate;
    
    private LocalDateTime bookingEndTime;
    
    private Integer bookingDuration;
    
    private String contactPhone;
    
    /**
     * 订单项列表
     */
    private List<OrderItemVO> items;
}