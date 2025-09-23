package com.ticketsystem.order.feign;

import com.ticketsystem.common.result.Result;
import com.ticketsystem.order.feign.dto.ShowInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * 演出服务Feign客户端
 */
@FeignClient(name = "ticket-show", url = "http://localhost:8002")
public interface ShowFeignClient {

    /**
     * 获取票档价格
     */
    @GetMapping("/api/ticket/price/{ticketId}")
    Result<BigDecimal> getTicketPrice(@PathVariable("ticketId") Long ticketId);

    /**
     * 锁定票档库存
     */
    @PutMapping("/api/ticket/lock")
    Result<Boolean> lockTicketStock(@RequestParam("ticketId") Long ticketId, 
                                   @RequestParam("quantity") Integer quantity);

    /**
     * 释放票档库存
     */
    @PutMapping("/api/ticket/unlock")
    Result<Boolean> unlockTicketStock(@RequestParam("ticketId") Long ticketId, 
                                     @RequestParam("quantity") Integer quantity);

    /**
     * 扣减票档库存
     */
    @PutMapping("/api/ticket/deduct")
    Result<Boolean> deductTicketStock(@RequestParam("ticketId") Long ticketId, 
                                     @RequestParam("quantity") Integer quantity);

    /**
     * Redis预减库存（原子操作）
     */
    @PostMapping("/api/ticket/redis/prededuct")
    Result<Integer> predeductStockFromRedis(@RequestParam("ticketId") Long ticketId, 
                                           @RequestParam("quantity") Integer quantity);

    /**
     * Redis回滚库存
     */
    @PostMapping("/api/ticket/redis/rollback")
    Result<Boolean> rollbackStockToRedis(@RequestParam("ticketId") Long ticketId, 
                                        @RequestParam("quantity") Integer quantity);

    /**
     * 同步库存到Redis
     */
    @PostMapping("/api/ticket/redis/sync")
    Result<Boolean> syncStockToRedis(@RequestParam("ticketId") Long ticketId);

    /**
     * 数据库库存确认（乐观锁）
     */
    @PutMapping("/api/ticket/confirm")
    Result<Boolean> confirmStockFromDatabase(@RequestParam("ticketId") Long ticketId, 
                                            @RequestParam("quantity") Integer quantity);

    /**
     * 获取演出信息
     */
    @GetMapping("/api/show/info")
    Result<ShowInfoDTO> getShowInfo(@RequestParam("showId") Long showId, 
                                   @RequestParam("sessionId") Long sessionId);

    /**
     * 获取演出详细信息
     */
    @GetMapping("/api/show/detail/{showId}")
    Result<ShowInfoDTO> getShowDetail(@PathVariable("showId") Long showId);

    /**
     * 获取座位信息
     */
    @GetMapping("/api/seat/info/{seatId}")
    Result<String> getSeatInfo(@PathVariable("seatId") Long seatId);

    /**
     * 锁定座位
     */
    @PostMapping("/api/seat/lock/single")
    Result<Boolean> lockSeat(@RequestParam("seatId") Long seatId,
                            @RequestParam("userId") Long userId,
                            @RequestParam("sessionId") Long sessionId);

    /**
     * 释放座位锁定
     */
    @PostMapping("/api/seat/release/single")
    Result<Boolean> releaseSeat(@RequestParam("seatId") Long seatId,
                               @RequestParam("userId") Long userId,
                               @RequestParam("sessionId") Long sessionId);
}