package com.ticketsystem.show.controller;

import com.ticketsystem.show.service.AtomicSeatLockService;
import com.ticketsystem.show.service.AtomicTicketStockService;
import com.ticketsystem.show.service.DataSyncService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 原子化操作测试控制器
 */
@RestController
@RequestMapping("/api/atomic-test")
@RequiredArgsConstructor
@Slf4j
public class AtomicTestController {

    private final AtomicSeatLockService atomicSeatLockService;
    private final AtomicTicketStockService atomicTicketStockService;
    private final DataSyncService dataSyncService;
    private final RedissonClient redissonClient;

    @GetMapping("/health")
    @Operation(summary = "检查原子化服务健康状态")
    public Map<String, Object> checkHealth() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 检查Redis连接
            boolean redisConnected = redissonClient.getKeys().count() >= 0;
            result.put("redisConnected", redisConnected);
            
            // 检查服务注入
            result.put("atomicSeatLockService", atomicSeatLockService != null);
            result.put("atomicTicketStockService", atomicTicketStockService != null);
            result.put("dataSyncService", dataSyncService != null);
            
            // 测试Redis操作
            try {
                redissonClient.getBucket("test:atomic").set("test");
                Object testValueObj = redissonClient.getBucket("test:atomic").get();
                String testValue = testValueObj != null ? testValueObj.toString() : null;
                result.put("redisOperation", "test".equals(testValue));
                redissonClient.getBucket("test:atomic").delete();
            } catch (Exception e) {
                result.put("redisOperation", false);
                result.put("redisError", e.getMessage());
            }
            
            result.put("status", "healthy");
            result.put("message", "原子化服务运行正常");
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "原子化服务异常: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
        }
        
        return result;
    }

    @GetMapping("/test-seat-lock")
    @Operation(summary = "测试座位锁定")
    public Map<String, Object> testSeatLock() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long seatId = 31L;
            Long userId = 2L;
            
            // 测试座位锁定
            boolean lockResult = atomicSeatLockService.atomicLockSeat(seatId, userId);
            result.put("lockResult", lockResult);
            
            if (lockResult) {
                // 测试座位解锁
                boolean unlockResult = atomicSeatLockService.atomicUnlockSeat(seatId, userId);
                result.put("unlockResult", unlockResult);
            }
            
            result.put("status", "success");
            result.put("message", "座位锁定测试完成");
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "座位锁定测试失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            log.error("座位锁定测试失败", e);
        }
        
        return result;
    }

    @GetMapping("/test-stock-deduct")
    @Operation(summary = "测试库存扣减")
    public Map<String, Object> testStockDeduct() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long ticketId = 1L;
            Integer quantity = 1;
            
            // 测试库存扣减
            boolean deductResult = atomicTicketStockService.atomicDeductStock(ticketId, quantity);
            result.put("deductResult", deductResult);
            
            if (deductResult) {
                // 测试库存回滚
                boolean rollbackResult = atomicTicketStockService.atomicRollbackStock(ticketId, quantity);
                result.put("rollbackResult", rollbackResult);
            }
            
            result.put("status", "success");
            result.put("message", "库存扣减测试完成");
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "库存扣减测试失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            log.error("库存扣减测试失败", e);
        }
        
        return result;
    }
}