package com.ticketsystem.show.controller;

import com.ticketsystem.common.annotation.PerformanceMonitor;
import com.ticketsystem.common.result.Result;
import com.ticketsystem.show.entity.Ticket;
import com.ticketsystem.show.service.TicketService;
import com.ticketsystem.show.service.TicketStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 票档管理控制器
 */
@RestController
@RequestMapping("/api/ticket")
@RequiredArgsConstructor
@Tag(name = "票档管理", description = "票档相关接口")
@Slf4j
public class TicketController {

    private final TicketService ticketService;
    private final TicketStockService ticketStockService;

    @GetMapping("/{ticketId}")
    @Operation(summary = "获取票档详情")
    public Result<Ticket> getTicketDetail(@PathVariable Long ticketId) {
        log.info("获取票档详情，票档ID：{}", ticketId);
        Ticket ticket = ticketService.getById(ticketId);
        return Result.success(ticket);
    }

    @GetMapping("/price/{ticketId}")
    @Operation(summary = "获取票档价格")
    public Result<BigDecimal> getTicketPrice(@PathVariable Long ticketId) {
        log.info("获取票档价格，票档ID：{}", ticketId);
        BigDecimal price = ticketService.getTicketPrice(ticketId);
        return Result.success(price);
    }

    @PutMapping("/lock")
    @Operation(summary = "锁定票档库存")
    @PerformanceMonitor(value = "lockTicketStock", slowQueryThreshold = 2000)
    public Result<Boolean> lockTicketStock(@RequestParam Long ticketId, 
                                          @RequestParam Integer quantity) {
        log.info("锁定票档库存，票档ID：{}，数量：{}", ticketId, quantity);
        Boolean result = ticketService.lockTicketStock(ticketId, quantity);
        return Result.success(result);
    }

    @PutMapping("/unlock")
    @Operation(summary = "释放票档库存")
    public Result<Boolean> unlockTicketStock(@RequestParam Long ticketId, 
                                            @RequestParam Integer quantity) {
        log.info("释放票档库存，票档ID：{}，数量：{}", ticketId, quantity);
        Boolean result = ticketService.unlockTicketStock(ticketId, quantity);
        return Result.success(result);
    }

    @PutMapping("/deduct")
    @Operation(summary = "扣减票档库存")
    @PerformanceMonitor(value = "deductTicketStock", slowQueryThreshold = 3000)
    public Result<Boolean> deductTicketStock(@RequestParam Long ticketId, 
                                            @RequestParam Integer quantity) {
        log.info("扣减票档库存，票档ID：{}，数量：{}", ticketId, quantity);
        Boolean result = ticketService.deductTicketStock(ticketId, quantity);
        return Result.success(result);
    }

    @GetMapping("/stock/{ticketId}")
    @Operation(summary = "获取票档库存信息")
    public Result<Object> getTicketStock(@PathVariable Long ticketId) {
        log.info("获取票档库存信息，票档ID：{}", ticketId);
        Object stockInfo = ticketService.getTicketStockInfo(ticketId);
        return Result.success(stockInfo);
    }

    @GetMapping("/stock/all")
    @Operation(summary = "获取所有票档库存信息")
    public Result<List<Object>> getAllTicketStock() {
        log.info("获取所有票档库存信息");
        List<Object> allStockInfo = ticketService.getAllTicketStockInfo();
        return Result.success(allStockInfo);
    }

    @PostMapping("/stock/init")
    @Operation(summary = "初始化票档库存")
    public Result<Boolean> initializeStock(@RequestBody Map<String, Object> request) {
        Long ticketId = Long.valueOf(request.get("ticketId").toString());
        Integer totalStock = Integer.valueOf(request.get("totalStock").toString());
        log.info("初始化票档库存，票档ID：{}，库存数量：{}", ticketId, totalStock);
        Boolean result = ticketService.initializeTicketStock(ticketId, totalStock);
        return Result.success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "获取票档列表")
    public Result<List<Ticket>> getTicketList(@RequestParam Long showId, 
                                              @RequestParam Long sessionId) {
        log.info("获取票档列表，演出ID：{}，场次ID：{}", showId, sessionId);
        List<Ticket> tickets = ticketService.getTicketsByShowAndSession(showId, sessionId);
        return Result.success(tickets);
    }

    @PostMapping
    @Operation(summary = "创建票档")
    public Result<Long> createTicket(@RequestBody Ticket ticket) {
        log.info("创建票档，票档信息：{}", ticket);
        Long ticketId = ticketService.createTicket(ticket);
        return Result.success(ticketId);
    }

    @PutMapping
    @Operation(summary = "更新票档信息")
    public Result<Void> updateTicket(@RequestBody Ticket ticket) {
        log.info("更新票档信息，票档ID：{}", ticket.getId());
        ticketService.updateTicket(ticket);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除票档")
    public Result<Void> deleteTicket(@PathVariable Long id) {
        log.info("删除票档，票档ID：{}", id);
        ticketService.deleteTicket(id);
        return Result.success();
    }

    @PostMapping("/redis/prededuct")
    @Operation(summary = "Redis预减库存")
    public Result<Integer> predeductStockFromRedis(@RequestParam Long ticketId, @RequestParam Integer quantity) {
        log.info("Redis预减库存，票档ID：{}，数量：{}", ticketId, quantity);
        Integer result = ticketStockService.predeductStockFromRedis(ticketId, quantity);
        return Result.success(result);
    }

    @PostMapping("/redis/rollback")
    @Operation(summary = "Redis回滚库存")
    public Result<Boolean> rollbackStockToRedis(@RequestParam Long ticketId, @RequestParam Integer quantity) {
        log.info("Redis回滚库存，票档ID：{}，数量：{}", ticketId, quantity);
        boolean result = ticketStockService.rollbackStockToRedis(ticketId, quantity);
        return Result.success(result);
    }

    @PostMapping("/redis/sync")
    @Operation(summary = "同步库存到Redis")
    public Result<Boolean> syncStockToRedis(@RequestParam Long ticketId) {
        log.info("同步库存到Redis，票档ID：{}", ticketId);
        boolean result = ticketStockService.syncStockToRedis(ticketId);
        return Result.success(result);
    }

    @PutMapping("/confirm")
    @Operation(summary = "数据库库存确认（乐观锁）")
    @PerformanceMonitor(value = "confirmStockFromDatabase", slowQueryThreshold = 2000)
    public Result<Boolean> confirmStockFromDatabase(@RequestParam Long ticketId, @RequestParam Integer quantity) {
        log.info("数据库库存确认，票档ID：{}，数量：{}", ticketId, quantity);
        boolean result = ticketStockService.confirmStockFromDatabase(ticketId, quantity);
        if (result) {
            return Result.success(true);
        } else {
            return Result.error("库存确认失败：锁定库存不足或乐观锁冲突");
        }
    }
}