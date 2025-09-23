package com.ticketsystem.show.controller;

import com.ticketsystem.show.service.impl.SeatConsistencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 座位数据一致性管理控制器
 * 提供座位锁定状态查询和一致性管理功能
 */
@RestController
@RequestMapping("/api/seat-consistency")
@RequiredArgsConstructor
@Slf4j
public class SeatConsistencyController {

    private final SeatConsistencyService seatConsistencyService;

    /**
     * 查询座位锁定状态
     */
    @GetMapping("/status/{seatId}")
    public Map<String, Object> getSeatLockStatus(@PathVariable Long seatId) {
        Map<String, Object> result = new HashMap<>();
        try {
            String status = seatConsistencyService.getSeatLockStatus(seatId);
            result.put("success", true);
            result.put("data", status);
            result.put("message", "查询成功");
        } catch (Exception e) {
            log.error("查询座位锁定状态失败，座位ID: {}", seatId, e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 批量查询座位锁定状态
     */
    @PostMapping("/status/batch")
    public Map<String, Object> getBatchSeatLockStatus(@RequestBody List<Long> seatIds) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<Long, String> statusMap = new HashMap<>();
            for (Long seatId : seatIds) {
                String status = seatConsistencyService.getSeatLockStatus(seatId);
                statusMap.put(seatId, status);
            }
            result.put("success", true);
            result.put("data", statusMap);
            result.put("message", "批量查询成功");
        } catch (Exception e) {
            log.error("批量查询座位锁定状态失败，座位IDs: {}", seatIds, e);
            result.put("success", false);
            result.put("message", "批量查询失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 手动触发数据一致性验证
     */
    @PostMapping("/verify/{seatId}")
    public Map<String, Object> verifyConsistency(@PathVariable Long seatId, 
                                                 @RequestParam(required = false) Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            seatConsistencyService.verifyAndRepairConsistency(seatId, userId);
            result.put("success", true);
            result.put("message", "数据一致性验证完成");
        } catch (Exception e) {
            log.error("数据一致性验证失败，座位ID: {}, 用户ID: {}", seatId, userId, e);
            result.put("success", false);
            result.put("message", "验证失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 批量验证数据一致性
     */
    @PostMapping("/verify/batch")
    public Map<String, Object> verifyBatchConsistency(@RequestBody List<Long> seatIds, 
                                                      @RequestParam(required = false) Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            for (Long seatId : seatIds) {
                seatConsistencyService.verifyAndRepairConsistency(seatId, userId);
            }
            result.put("success", true);
            result.put("message", "批量数据一致性验证完成");
        } catch (Exception e) {
            log.error("批量数据一致性验证失败，座位IDs: {}, 用户ID: {}", seatIds, userId, e);
            result.put("success", false);
            result.put("message", "批量验证失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 手动清理过期锁定
     */
    @PostMapping("/cleanup")
    public Map<String, Object> cleanupExpiredLocks() {
        Map<String, Object> result = new HashMap<>();
        try {
            seatConsistencyService.cleanupExpiredLocks();
            result.put("success", true);
            result.put("message", "过期锁定清理完成");
        } catch (Exception e) {
            log.error("手动清理过期锁定失败", e);
            result.put("success", false);
            result.put("message", "清理失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 原子性锁定座位（测试接口）
     */
    @PostMapping("/lock")
    public Map<String, Object> atomicLockSeats(@RequestBody List<Long> seatIds, 
                                               @RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = seatConsistencyService.atomicLockSeats(seatIds, userId);
            result.put("success", success);
            result.put("message", success ? "座位锁定成功" : "座位锁定失败");
        } catch (Exception e) {
            log.error("原子性锁定座位失败，座位IDs: {}, 用户ID: {}", seatIds, userId, e);
            result.put("success", false);
            result.put("message", "锁定失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 原子性释放座位（测试接口）
     */
    @PostMapping("/release")
    public Map<String, Object> atomicReleaseSeats(@RequestBody List<Long> seatIds, 
                                                  @RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = seatConsistencyService.atomicReleaseSeats(seatIds, userId);
            result.put("success", success);
            result.put("message", success ? "座位释放成功" : "座位释放失败");
        } catch (Exception e) {
            log.error("原子性释放座位失败，座位IDs: {}, 用户ID: {}", seatIds, userId, e);
            result.put("success", false);
            result.put("message", "释放失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取系统健康状态
     */
    @GetMapping("/health")
    public Map<String, Object> getSystemHealth() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 这里可以添加更多的健康检查逻辑
            result.put("success", true);
            result.put("status", "healthy");
            result.put("timestamp", System.currentTimeMillis());
            result.put("message", "座位一致性服务运行正常");
        } catch (Exception e) {
            log.error("获取系统健康状态失败", e);
            result.put("success", false);
            result.put("status", "unhealthy");
            result.put("message", "健康检查失败: " + e.getMessage());
        }
        return result;
    }
}