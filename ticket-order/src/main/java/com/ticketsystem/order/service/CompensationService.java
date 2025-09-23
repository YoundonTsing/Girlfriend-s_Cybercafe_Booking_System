package com.ticketsystem.order.service;

import com.ticketsystem.common.result.Result;
import com.ticketsystem.order.feign.ShowFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 补偿机制服务
 * 处理跨服务调用失败的补偿和重试策略
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompensationService {

    private final ShowFeignClient showFeignClient;

    /**
 * 带重试的库存回滚补偿
 * @param ticketId 票档ID
 * @param quantity 回滚数量
 * @param orderNo 订单号
 * @return 补偿结果
 */
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public Boolean compensateStockRollback(Long ticketId, Integer quantity, String orderNo) {
        log.info("开始库存回滚补偿，订单号：{}，票档ID：{}，数量：{}", orderNo, ticketId, quantity);
        
        try {
            // 参数验证
            if (ticketId == null || ticketId <= 0) {
                log.error("补偿失败-票档ID无效，订单号：{}，票档ID：{}", orderNo, ticketId);
                return false;
            }
            if (quantity == null || quantity <= 0) {
                log.error("补偿失败-回滚数量无效，订单号：{}，数量：{}", orderNo, quantity);
                return false;
            }
            
            Result<Boolean> rollbackResult = showFeignClient.rollbackStockToRedis(ticketId, quantity);
            
            if (rollbackResult != null && rollbackResult.getCode().equals(200) && Boolean.TRUE.equals(rollbackResult.getData())) {
                log.info("库存回滚补偿成功，订单号：{}，票档ID：{}，数量：{}", orderNo, ticketId, quantity);
                return true;
            } else {
                String errorMsg = rollbackResult != null ? rollbackResult.getMessage() : "未知错误";
                log.error("库存回滚补偿失败，订单号：{}，票档ID：{}，数量：{}，错误：{}", 
                    orderNo, ticketId, quantity, errorMsg);
                throw new RuntimeException("库存回滚补偿失败: " + errorMsg);
            }
            
        } catch (Exception e) {
            log.error("库存回滚补偿异常，订单号：{}，票档ID：{}，数量：{}", orderNo, ticketId, quantity, e);
            throw e; // 重新抛出异常以触发重试
        }
    }

    /**
     * 异步库存回滚补偿
     * @param ticketId 票档ID
     * @param quantity 回滚数量
     * @param orderNo 订单号
     * @return CompletableFuture
     */
    public CompletableFuture<Boolean> asyncCompensateStockRollback(Long ticketId, Integer quantity, String orderNo) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 延迟执行，避免立即重试
                TimeUnit.SECONDS.sleep(2);
                return compensateStockRollback(ticketId, quantity, orderNo);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("异步补偿被中断，订单号：{}，票档ID：{}，数量：{}", orderNo, ticketId, quantity);
                return false;
            } catch (Exception e) {
                log.error("异步补偿失败，订单号：{}，票档ID：{}，数量：{}", orderNo, ticketId, quantity, e);
                return false;
            }
        });
    }

    /**
     * 带重试的Redis预减库存补偿
     * @param ticketId 票档ID
     * @param quantity 预减数量
     * @param orderNo 订单号
     * @return 补偿结果
     */
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 500, multiplier = 1.5)
    )
    public Integer compensateStockPrededuct(Long ticketId, Integer quantity, String orderNo) {
        log.info("开始Redis预减库存补偿，订单号：{}，票档ID：{}，数量：{}", orderNo, ticketId, quantity);
        
        try {
            // 参数验证
            if (ticketId == null || ticketId <= 0) {
                log.error("预减补偿失败-票档ID无效，订单号：{}，票档ID：{}", orderNo, ticketId);
                return -1;
            }
            if (quantity == null || quantity <= 0) {
                log.error("预减补偿失败-预减数量无效，订单号：{}，数量：{}", orderNo, quantity);
                return -1;
            }
            
            Result<Integer> predeductResult = showFeignClient.predeductStockFromRedis(ticketId, quantity);
            
            if (predeductResult != null && predeductResult.getCode().equals(200)) {
                Integer result = predeductResult.getData();
                log.info("Redis预减库存补偿完成，订单号：{}，票档ID：{}，数量：{}，结果：{}", 
                    orderNo, ticketId, quantity, result);
                return result;
            } else {
                String errorMsg = predeductResult != null ? predeductResult.getMessage() : "未知错误";
                log.error("Redis预减库存补偿失败，订单号：{}，票档ID：{}，数量：{}，错误：{}", 
                    orderNo, ticketId, quantity, errorMsg);
                throw new RuntimeException("Redis预减库存补偿失败: " + errorMsg);
            }
            
        } catch (Exception e) {
            log.error("Redis预减库存补偿异常，订单号：{}，票档ID：{}，数量：{}", orderNo, ticketId, quantity, e);
            throw e; // 重新抛出异常以触发重试
        }
    }

    /**
     * 记录补偿失败事件（用于后续人工处理或定时任务重试）
     * @param orderNo 订单号
     * @param ticketId 票档ID
     * @param quantity 数量
     * @param operation 操作类型
     * @param errorMsg 错误信息
     */
    public void recordCompensationFailure(String orderNo, Long ticketId, Integer quantity, 
                                         String operation, String errorMsg) {
        log.error("记录补偿失败事件 - 订单号：{}，票档ID：{}，数量：{}，操作：{}，错误：{}", 
            orderNo, ticketId, quantity, operation, errorMsg);
        
        // TODO: 这里可以将失败事件记录到数据库或消息队列中
        // 供后续的定时任务或人工处理
        // 例如：compensationFailureRepository.save(new CompensationFailure(...));
        // 或者：messageProducer.sendCompensationFailureMessage(...);
    }
}