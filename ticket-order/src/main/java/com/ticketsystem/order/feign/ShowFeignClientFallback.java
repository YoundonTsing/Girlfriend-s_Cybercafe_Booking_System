package com.ticketsystem.order.feign;

import com.ticketsystem.common.result.Result;
import com.ticketsystem.order.feign.dto.ShowInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 演出服务Feign客户端降级处理
 */
@Component
@Slf4j
public class ShowFeignClientFallback implements ShowFeignClient {

    @Override
    public Result<BigDecimal> getTicketPrice(Long ticketId) {
        log.error("获取票档价格失败，进入降级处理，ticketId: {}", ticketId);
        return Result.fail("获取票档价格失败，请稍后再试");
    }

    @Override
    public Result<Boolean> lockTicketStock(Long ticketId, Integer quantity) {
        log.error("锁定票档库存失败，进入降级处理，ticketId: {}, quantity: {}", ticketId, quantity);
        return Result.fail("锁定票档库存失败，请稍后再试");
    }

    @Override
    public Result<Boolean> unlockTicketStock(Long ticketId, Integer quantity) {
        log.error("释放票档库存失败，进入降级处理，ticketId: {}, quantity: {}", ticketId, quantity);
        return Result.fail("释放票档库存失败，请稍后再试");
    }

    @Override
    public Result<Boolean> deductTicketStock(Long ticketId, Integer quantity) {
        log.error("扣减票档库存失败，进入降级处理，ticketId: {}, quantity: {}", ticketId, quantity);
        return Result.fail("扣减票档库存失败，请稍后再试");
    }

    @Override
    public Result<Integer> predeductStockFromRedis(Long ticketId, Integer quantity) {
        log.error("Redis预减库存失败，进入降级处理，ticketId: {}, quantity: {}", ticketId, quantity);
        return Result.fail("Redis预减库存失败，请稍后再试");
    }

    @Override
    public Result<Boolean> rollbackStockToRedis(Long ticketId, Integer quantity) {
        log.error("Redis回滚库存失败，进入降级处理，ticketId: {}, quantity: {}", ticketId, quantity);
        return Result.fail("Redis回滚库存失败，请稍后再试");
    }

    @Override
    public Result<Boolean> syncStockToRedis(Long ticketId) {
        log.error("同步库存到Redis失败，进入降级处理，ticketId: {}", ticketId);
        return Result.fail("同步库存到Redis失败，请稍后再试");
    }

    @Override
    public Result<Boolean> confirmStockFromDatabase(Long ticketId, Integer quantity) {
        log.error("数据库库存确认失败，进入降级处理，ticketId: {}, quantity: {}", ticketId, quantity);
        return Result.fail("数据库库存确认失败，请稍后再试");
    }

    @Override
    public Result<ShowInfoDTO> getShowInfo(Long showId, Long sessionId) {
        log.error("获取演出信息失败，进入降级处理，showId: {}, sessionId: {}", showId, sessionId);
        return Result.fail("获取演出信息失败，请稍后再试");
    }

    @Override
    public Result<String> getSeatInfo(Long seatId) {
        log.error("获取座位信息失败，进入降级处理，seatId: {}", seatId);
        return Result.fail("获取座位信息失败，请稍后再试");
    }

    @Override
    public Result<Boolean> lockSeat(Long seatId, Long userId, Long sessionId) {
        log.error("锁定座位失败，进入降级处理，seatId: {}, userId: {}, sessionId: {}", seatId, userId, sessionId);
        return Result.fail("锁定座位失败，请稍后再试");
    }

    @Override
    public Result<Boolean> releaseSeat(Long seatId, Long userId, Long sessionId) {
        log.error("释放座位锁定失败，进入降级处理，seatId: {}, userId: {}, sessionId: {}", seatId, userId, sessionId);
        return Result.fail("释放座位锁定失败，请稍后再试");
    }

    @Override
    public Result<ShowInfoDTO> getShowDetail(Long showId) {
        log.error("获取演出详细信息失败，进入降级处理，showId: {}", showId);
        return Result.fail("获取演出详细信息失败，请稍后再试");
    }
}