package com.ticketsystem.show.controller;

import com.ticketsystem.show.monitor.StockLevelMonitor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 库存监控控制器
 * 提供监控数据查询和手动触发检查的API接口
 */
@RestController
@RequestMapping("/api/stock/monitor")
@Api(tags = "库存监控API")
@RequiredArgsConstructor
@Slf4j
public class StockMonitorController {
    
    private final StockLevelMonitor stockLevelMonitor;
    private final MeterRegistry meterRegistry;
    
    /**
     * 手动触发库存水位检查
     */
    @PostMapping("/check")
    @ApiOperation("手动触发库存水位检查")
    public Map<String, Object> manualCheck() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("📋 API触发库存水位检查");
            
            long startTime = System.currentTimeMillis();
            stockLevelMonitor.manualCheck();
            long duration = System.currentTimeMillis() - startTime;
            
            result.put("success", true);
            result.put("message", "库存水位检查已完成");
            result.put("duration_ms", duration);
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("✅ 手动库存检查完成，耗时: {}ms", duration);
            
        } catch (Exception e) {
            log.error("❌ 手动库存检查异常: {}", e.getMessage(), e);
            
            result.put("success", false);
            result.put("message", "库存水位检查失败: " + e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
        }
        
        return result;
    }
    
    /**
     * 获取库存水位统计
     */
    @GetMapping("/stats")
    @ApiOperation("获取库存水位统计")
    public Map<String, Object> getStockStats() {
        try {
            log.debug("📊 获取库存水位统计");
            return stockLevelMonitor.getStockLevelStats();
        } catch (Exception e) {
            log.error("❌ 获取库存统计异常: {}", e.getMessage(), e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "获取库存统计失败: " + e.getMessage());
            errorResult.put("timestamp", System.currentTimeMillis());
            return errorResult;
        }
    }
    
    /**
     * 获取操作成功率统计
     */
    @GetMapping("/success-rate")
    @ApiOperation("获取操作成功率统计")
    public Map<String, Object> getSuccessRateStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 获取各类操作的成功率
            stats.put("lock_success_rate", calculateSuccessRate("stock_lock_success", "stock_lock_failure"));
            stats.put("confirm_success_rate", calculateSuccessRate("stock_confirm_success", "stock_confirm_failure"));
            stats.put("rollback_success_rate", calculateSuccessRate("stock_rollback_success", "stock_rollback_failure"));
            
            // 获取总体操作统计
            stats.put("total_operations", getTotalOperations());
            stats.put("error_operations", getErrorOperations());
            stats.put("overall_success_rate", calculateOverallSuccessRate());
            
            stats.put("timestamp", System.currentTimeMillis());
            stats.put("success", true);
            
            log.debug("📈 操作成功率统计获取完成");
            
        } catch (Exception e) {
            log.error("❌ 获取成功率统计异常: {}", e.getMessage(), e);
            
            stats.put("success", false);
            stats.put("message", "获取成功率统计失败: " + e.getMessage());
            stats.put("timestamp", System.currentTimeMillis());
        }
        
        return stats;
    }
    
    /**
     * 获取性能指标统计
     */
    @GetMapping("/performance")
    @ApiOperation("获取性能指标统计")
    public Map<String, Object> getPerformanceStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 获取操作响应时间统计
            Timer operationTimer = meterRegistry.find("stock_operation_timer").timer();
            if (operationTimer != null) {
                stats.put("avg_response_time_ms", operationTimer.mean(TimeUnit.MILLISECONDS));
                stats.put("max_response_time_ms", operationTimer.max(TimeUnit.MILLISECONDS));
                stats.put("total_operations", operationTimer.count());
            }
            
            // 获取QPS统计（基于最近1分钟的操作数）
            double qps = calculateQPS();
            stats.put("qps", qps);
            
            // 获取错误率
            double errorRate = calculateErrorRate();
            stats.put("error_rate", errorRate);
            
            // 获取并发操作监控（简化实现）
            stats.put("concurrent_operations", getCurrentConcurrentOperations());
            
            stats.put("timestamp", System.currentTimeMillis());
            stats.put("success", true);
            
            log.debug("⚡ 性能指标统计获取完成");
            
        } catch (Exception e) {
            log.error("❌ 获取性能统计异常: {}", e.getMessage(), e);
            
            stats.put("success", false);
            stats.put("message", "获取性能统计失败: " + e.getMessage());
            stats.put("timestamp", System.currentTimeMillis());
        }
        
        return stats;
    }
    
    /**
     * 获取异常操作统计
     */
    @GetMapping("/errors")
    @ApiOperation("获取异常操作统计")
    public Map<String, Object> getErrorStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Redis连接异常统计
            stats.put("redis_connection_errors", getCounterValue("redis_connection_error"));
            
            // Lua脚本执行异常统计
            stats.put("lua_script_errors", getCounterValue("lua_script_error"));
            
            // 数据库操作异常统计
            stats.put("database_errors", getCounterValue("database_error"));
            
            // 业务逻辑异常统计
            stats.put("business_logic_errors", getCounterValue("operation_error"));
            
            // 告警统计
            stats.put("total_alerts", getCounterValue("stock_alert"));
            
            // 按严重程度分类的告警
            stats.put("critical_alerts", getCounterValueWithTag("stock_alert", "severity", "critical"));
            stats.put("warning_alerts", getCounterValueWithTag("stock_alert", "severity", "warning"));
            stats.put("notice_alerts", getCounterValueWithTag("stock_alert", "severity", "notice"));
            
            stats.put("timestamp", System.currentTimeMillis());
            stats.put("success", true);
            
            log.debug("🚨 异常操作统计获取完成");
            
        } catch (Exception e) {
            log.error("❌ 获取异常统计失败: {}", e.getMessage(), e);
            
            stats.put("success", false);
            stats.put("message", "获取异常统计失败: " + e.getMessage());
            stats.put("timestamp", System.currentTimeMillis());
        }
        
        return stats;
    }
    
    /**
     * 获取监控健康状态
     */
    @GetMapping("/health")
    @ApiOperation("获取监控系统健康状态")
    public Map<String, Object> getMonitorHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // 检查监控组件状态
            boolean meterRegistryHealthy = meterRegistry != null;
            boolean stockMonitorHealthy = stockLevelMonitor != null;
            
            // 检查最近是否有异常
            double recentErrorRate = calculateRecentErrorRate();
            boolean lowErrorRate = recentErrorRate < 0.05; // 错误率低于5%
            
            // 检查响应时间
            Timer operationTimer = meterRegistry.find("stock_operation_timer").timer();
            boolean goodPerformance = operationTimer == null || 
                    operationTimer.mean(TimeUnit.MILLISECONDS) < 1000; // 平均响应时间小于1秒
            
            boolean overallHealthy = meterRegistryHealthy && stockMonitorHealthy && 
                    lowErrorRate && goodPerformance;
            
            health.put("status", overallHealthy ? "UP" : "DOWN");
            health.put("meter_registry", meterRegistryHealthy ? "UP" : "DOWN");
            health.put("stock_monitor", stockMonitorHealthy ? "UP" : "DOWN");
            health.put("error_rate", recentErrorRate);
            health.put("low_error_rate", lowErrorRate);
            health.put("good_performance", goodPerformance);
            health.put("timestamp", System.currentTimeMillis());
            
            log.debug("💚 监控健康状态检查完成: {}", overallHealthy ? "健康" : "异常");
            
        } catch (Exception e) {
            log.error("❌ 监控健康检查异常: {}", e.getMessage(), e);
            
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", System.currentTimeMillis());
        }
        
        return health;
    }
    
    // ========== 私有辅助方法 ==========
    
    private double calculateSuccessRate(String successCounterName, String failureCounterName) {
        double successCount = getCounterValue(successCounterName);
        double failureCount = getCounterValue(failureCounterName);
        double totalCount = successCount + failureCount;
        
        return totalCount > 0 ? (successCount / totalCount) * 100 : 0.0;
    }
    
    private double getTotalOperations() {
        Counter operationCounter = meterRegistry.find("stock_operation").counter();
        return operationCounter != null ? operationCounter.count() : 0.0;
    }
    
    private double getErrorOperations() {
        Counter operationCounter = meterRegistry.find("stock_operation")
                .tag("status", "error")
                .counter();
        return operationCounter != null ? operationCounter.count() : 0.0;
    }
    
    private double calculateOverallSuccessRate() {
        double totalOps = getTotalOperations();
        double errorOps = getErrorOperations();
        
        return totalOps > 0 ? ((totalOps - errorOps) / totalOps) * 100 : 0.0;
    }
    
    private double calculateQPS() {
        // 简化实现：基于总操作数估算QPS
        Timer operationTimer = meterRegistry.find("stock_operation_timer").timer();
        if (operationTimer != null && operationTimer.count() > 0) {
            // 假设系统运行时间为操作总数除以平均QPS的估算
            return operationTimer.count() / 60.0; // 简化为每分钟操作数
        }
        return 0.0;
    }
    
    private double calculateErrorRate() {
        double totalOps = getTotalOperations();
        double errorOps = getErrorOperations();
        
        return totalOps > 0 ? (errorOps / totalOps) * 100 : 0.0;
    }
    
    private int getCurrentConcurrentOperations() {
        // 简化实现：返回固定值，实际应该通过线程池或其他方式监控
        return 0;
    }
    
    private double calculateRecentErrorRate() {
        // 简化实现：返回总体错误率
        return calculateErrorRate();
    }
    
    private double getCounterValue(String counterName) {
        Counter counter = meterRegistry.find(counterName).counter();
        return counter != null ? counter.count() : 0.0;
    }
    
    private double getCounterValueWithTag(String counterName, String tagKey, String tagValue) {
        Counter counter = meterRegistry.find(counterName)
                .tag(tagKey, tagValue)
                .counter();
        return counter != null ? counter.count() : 0.0;
    }
}