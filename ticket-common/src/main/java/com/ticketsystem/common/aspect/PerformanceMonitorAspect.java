package com.ticketsystem.common.aspect;

import com.ticketsystem.common.annotation.PerformanceMonitor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控切面
 * 自动监控标记了@PerformanceMonitor注解的方法
 */
@Aspect
@Component
@Slf4j
public class PerformanceMonitorAspect {
    
    // 性能统计数据
    private final ConcurrentHashMap<String, MethodPerformanceStats> performanceStats = new ConcurrentHashMap<>();
    
    @Around("@annotation(performanceMonitor)")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint, PerformanceMonitor performanceMonitor) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        String monitorName = performanceMonitor.value().isEmpty() ? methodName : performanceMonitor.value();
        
        long startTime = System.currentTimeMillis();
        boolean success = true;
        Exception exception = null;
        
        try {
            Object result = joinPoint.proceed();
            
            // 记录参数和返回值（如果配置了）
            if (performanceMonitor.recordArgs()) {
                log.debug("方法 {} 参数: {}", methodName, joinPoint.getArgs());
            }
            if (performanceMonitor.recordResult()) {
                log.debug("方法 {} 返回值: {}", methodName, result);
            }
            
            return result;
        } catch (Exception e) {
            success = false;
            exception = e;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // 更新性能统计
            updatePerformanceStats(monitorName, duration, success);
            
            // 记录慢查询
            if (duration > performanceMonitor.slowQueryThreshold()) {
                log.warn("慢查询检测 - 方法: {}, 耗时: {}ms, 成功: {}", methodName, duration, success);
            }
            
            // 记录性能日志
            log.info("性能监控 - 方法: {}, 耗时: {}ms, 成功: {}", methodName, duration, success);
        }
    }
    
    private void updatePerformanceStats(String methodName, long duration, boolean success) {
        performanceStats.computeIfAbsent(methodName, k -> new MethodPerformanceStats())
                .update(duration, success);
    }
    
    /**
     * 获取性能统计报告
     */
    public ConcurrentHashMap<String, MethodPerformanceStats> getPerformanceStats() {
        return new ConcurrentHashMap<>(performanceStats);
    }
    
    /**
     * 方法性能统计类
     */
    public static class MethodPerformanceStats {
        private final AtomicLong totalCalls = new AtomicLong(0);
        private final AtomicLong successCalls = new AtomicLong(0);
        private final AtomicLong totalTime = new AtomicLong(0);
        private final AtomicLong maxTime = new AtomicLong(0);
        private final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
        
        public void update(long duration, boolean success) {
            totalCalls.incrementAndGet();
            if (success) {
                successCalls.incrementAndGet();
            }
            totalTime.addAndGet(duration);
            
            // 更新最大时间
            long currentMax = maxTime.get();
            while (duration > currentMax && !maxTime.compareAndSet(currentMax, duration)) {
                currentMax = maxTime.get();
            }
            
            // 更新最小时间
            long currentMin = minTime.get();
            while (duration < currentMin && !minTime.compareAndSet(currentMin, duration)) {
                currentMin = minTime.get();
            }
        }
        
        public double getAverageTime() {
            long calls = totalCalls.get();
            return calls > 0 ? (double) totalTime.get() / calls : 0;
        }
        
        public double getSuccessRate() {
            long total = totalCalls.get();
            return total > 0 ? (double) successCalls.get() / total : 0;
        }
        
        public long getTotalCalls() { return totalCalls.get(); }
        public long getSuccessCalls() { return successCalls.get(); }
        public long getTotalTime() { return totalTime.get(); }
        public long getMaxTime() { return maxTime.get(); }
        public long getMinTime() { return minTime.get() == Long.MAX_VALUE ? 0 : minTime.get(); }
    }
}