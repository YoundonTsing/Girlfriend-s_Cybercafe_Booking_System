package com.ticketsystem.common.monitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 系统资源监控
 * 定期收集和记录系统资源使用情况
 */
@Component
@Slf4j
public class SystemResourceMonitor {
    
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    
    private final AtomicLong lastCpuTime = new AtomicLong(0);
    private final AtomicLong lastUpTime = new AtomicLong(0);
    
    /**
     * 每30秒收集一次系统资源信息
     */
    @Scheduled(fixedRate = 30000)
    public void collectSystemResources() {
        try {
            // 内存使用情况
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            
            // CPU使用情况
            double cpuUsage = getCpuUsage();
            
            // 线程情况
            int threadCount = threadBean.getThreadCount();
            int peakThreadCount = threadBean.getPeakThreadCount();
            
            // 记录资源使用情况
            log.info("系统资源监控 - 内存使用: {:.2f}% ({}MB/{}MB), CPU使用: {:.2f}%, 线程数: {}/{}", 
                memoryUsagePercent, 
                usedMemory / 1024 / 1024, 
                maxMemory / 1024 / 1024,
                cpuUsage,
                threadCount,
                peakThreadCount);
            
            // 资源告警
            if (memoryUsagePercent > 80) {
                log.warn("内存使用率过高: {:.2f}%", memoryUsagePercent);
            }
            if (cpuUsage > 80) {
                log.warn("CPU使用率过高: {:.2f}%", cpuUsage);
            }
            if (threadCount > 500) {
                log.warn("线程数过多: {}", threadCount);
            }
            
        } catch (Exception e) {
            log.error("收集系统资源信息失败", e);
        }
    }
    
    private double getCpuUsage() {
        try {
            // 使用系统负载平均值作为CPU使用率的近似值
            double systemLoadAverage = osBean.getSystemLoadAverage();
            
            // 如果系统负载平均值不可用，返回0
            if (systemLoadAverage < 0) {
                return 0.0;
            }
            
            // 将系统负载平均值转换为百分比（假设单核系统）
            return Math.min(100.0, systemLoadAverage * 100);
            
        } catch (Exception e) {
            log.warn("获取CPU使用率失败", e);
        }
        return 0.0;
    }
}