package com.ticketsystem.order.service.impl;

import com.ticketsystem.order.entity.OrderSeat;
import com.ticketsystem.order.mapper.OrderSeatMapper;
import com.ticketsystem.order.service.SeatStatusSyncService;
import com.ticketsystem.order.service.SeatStatusSyncService.SeatStatus;
import com.ticketsystem.order.service.SeatStatusSyncService.SeatStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 座位状态同步服务实现类
 */
@Slf4j
@Service
public class SeatStatusSyncServiceImpl implements SeatStatusSyncService {

    @Autowired
    private OrderSeatMapper orderSeatMapper;

    @Autowired
    private RestTemplate restTemplate;

    // 座位状态变更监听器缓存
    private final Map<Long, SeatStatusChangeCallback> callbackMap = new ConcurrentHashMap<>();

    @Override
    public boolean syncSeatStatusToVenue(List<Long> seatIds, Long sessionId, Integer status) {
        try {
            // 构建同步请求
            Map<String, Object> syncRequest = new HashMap<>();
            syncRequest.put("seatIds", seatIds);
            syncRequest.put("sessionId", sessionId);
            syncRequest.put("status", status);

            // 调用场馆系统API同步座位状态
            String venueApiUrl = "http://venue-service/api/seats/sync-status";
            Map<String, Object> response = restTemplate.postForObject(venueApiUrl, syncRequest, Map.class);

            boolean success = response != null && Boolean.TRUE.equals(response.get("success"));
            
            if (success) {
                log.info("座位状态同步到场馆系统成功，seatIds: {}, sessionId: {}, status: {}", 
                        seatIds, sessionId, status);
            } else {
                log.error("座位状态同步到场馆系统失败，seatIds: {}, sessionId: {}, status: {}", 
                        seatIds, sessionId, status);
            }
            
            return success;
        } catch (Exception e) {
            log.error("座位状态同步到场馆系统异常，seatIds: {}, sessionId: {}, status: {}", 
                    seatIds, sessionId, status, e);
            return false;
        }
    }

    @Override
    public Map<Long, Integer> getSeatStatusFromVenue(List<Long> seatIds, Long sessionId) {
        try {
            // 构建查询请求
            Map<String, Object> queryRequest = new HashMap<>();
            queryRequest.put("seatIds", seatIds);
            queryRequest.put("sessionId", sessionId);

            // 调用场馆系统API获取座位状态
            String venueApiUrl = "http://venue-service/api/seats/status";
            Map<String, Object> response = restTemplate.postForObject(venueApiUrl, queryRequest, Map.class);

            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                Map<String, Integer> data = (Map<String, Integer>) response.get("data");
                Map<Long, Integer> result = new HashMap<>();
                
                // 转换数据类型
                for (Map.Entry<String, Integer> entry : data.entrySet()) {
                    result.put(Long.valueOf(entry.getKey()), entry.getValue());
                }
                
                log.info("从场馆系统获取座位状态成功，seatIds: {}, sessionId: {}", seatIds, sessionId);
                return result;
            } else {
                log.error("从场馆系统获取座位状态失败，seatIds: {}, sessionId: {}", seatIds, sessionId);
                return new HashMap<>();
            }
        } catch (Exception e) {
            log.error("从场馆系统获取座位状态异常，seatIds: {}, sessionId: {}", seatIds, sessionId, e);
            return new HashMap<>();
        }
    }

    @Override
    public boolean syncOrderSeatStatus(String orderNo, Integer status) {
        try {
            // 查询订单相关的座位
            List<OrderSeat> orderSeats = orderSeatMapper.selectByOrderNo(orderNo);
            if (orderSeats.isEmpty()) {
                log.warn("订单没有关联座位，orderNo: {}", orderNo);
                return true;
            }

            // 提取座位ID和场次ID
            List<Long> seatIds = orderSeats.stream().map(OrderSeat::getSeatId).toList();
            Long sessionId = orderSeats.get(0).getSessionId();

            // 同步座位状态
            return syncSeatStatusToVenue(seatIds, sessionId, status);
        } catch (Exception e) {
            log.error("同步订单座位状态异常，orderNo: {}, status: {}", orderNo, status, e);
            return false;
        }
    }

    @Override
    public int checkAndRepairSeatStatus(Long sessionId) {
        try {
            // 获取该场次所有订单座位
            List<OrderSeat> orderSeats = orderSeatMapper.selectBySessionId(sessionId);
            if (orderSeats.isEmpty()) {
                return 0;
            }

            // 提取座位ID
            List<Long> seatIds = orderSeats.stream().map(OrderSeat::getSeatId).toList();

            // 从场馆系统获取当前状态
            Map<Long, Integer> venueStatus = getSeatStatusFromVenue(seatIds, sessionId);

            int repairedCount = 0;
            for (OrderSeat orderSeat : orderSeats) {
                Long seatId = orderSeat.getSeatId();
                Integer venueStatusValue = venueStatus.get(seatId);
                Integer orderStatusValue = orderSeat.getLockStatus();

                // 状态映射转换（订单系统 -> 场馆系统）
                Integer expectedVenueStatus = mapOrderStatusToVenueStatus(orderStatusValue);

                if (venueStatusValue != null && !venueStatusValue.equals(expectedVenueStatus)) {
                    // 状态不一致，进行修复
                    if (syncSeatStatusToVenue(List.of(seatId), sessionId, expectedVenueStatus)) {
                        repairedCount++;
                        log.info("修复座位状态不一致，seatId: {}, sessionId: {}, 订单状态: {}, 场馆状态: {}, 修复为: {}", 
                                seatId, sessionId, orderStatusValue, venueStatusValue, expectedVenueStatus);
                    }
                }
            }

            log.info("座位状态一致性检查完成，sessionId: {}, 修复数量: {}", sessionId, repairedCount);
            return repairedCount;
        } catch (Exception e) {
            log.error("检查并修复座位状态异常，sessionId: {}", sessionId, e);
            return 0;
        }
    }

    @Override
    public void pushSeatStatusChange(Long seatId, Long sessionId, Integer oldStatus, Integer newStatus) {
        try {
            // 构建状态变更消息
            Map<String, Object> changeMessage = new HashMap<>();
            changeMessage.put("seatId", seatId);
            changeMessage.put("sessionId", sessionId);
            changeMessage.put("oldStatus", oldStatus);
            changeMessage.put("newStatus", newStatus);
            changeMessage.put("timestamp", System.currentTimeMillis());

            // 推送到消息队列或WebSocket
            // 这里可以集成RabbitMQ、Kafka或WebSocket等实时通信机制
            
            // 触发本地回调
            SeatStatusChangeCallback callback = callbackMap.get(sessionId);
            if (callback != null) {
                callback.onStatusChange(seatId, sessionId, oldStatus, newStatus);
            }

            log.info("推送座位状态变更，seatId: {}, sessionId: {}, {} -> {}", 
                    seatId, sessionId, oldStatus, newStatus);
        } catch (Exception e) {
            log.error("推送座位状态变更异常，seatId: {}, sessionId: {}", seatId, sessionId, e);
        }
    }

    @Override
    public void subscribeSeatStatusChange(Long sessionId, SeatStatusChangeCallback callback) {
        callbackMap.put(sessionId, callback);
        log.info("订阅座位状态变更，sessionId: {}", sessionId);
    }

    /**
     * 订单状态映射到场馆状态
     * @param orderStatus 订单系统状态
     * @return 场馆系统状态
     */
    private Integer mapOrderStatusToVenueStatus(Integer orderStatus) {
        if (orderStatus == null) {
            return SeatStatus.AVAILABLE.getCode();
        }
        
        switch (orderStatus) {
            case 0: // 未锁定
                return SeatStatus.AVAILABLE.getCode();
            case 1: // 临时锁定
                return SeatStatus.LOCKED.getCode();
            case 2: // 已确认
                return SeatStatus.SOLD.getCode();
            default:
                return SeatStatus.AVAILABLE.getCode();
        }
    }
}