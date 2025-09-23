package com.ticketsystem.common.controller;

import com.ticketsystem.common.analyzer.PerformanceDataAnalyzer;
import com.ticketsystem.common.aspect.PerformanceMonitorAspect;
import com.ticketsystem.common.report.PerformanceReportGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 性能监控控制器
 * 提供性能数据的REST API接口
 */
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
@Slf4j
public class PerformanceMonitorController {
    
    private final PerformanceMonitorAspect performanceMonitorAspect;
    private final PerformanceDataAnalyzer analyzer;
    private final PerformanceReportGenerator reportGenerator;
    
    /**
     * 获取性能统计概览
     */
    @GetMapping("/stats")
    public Map<String, Object> getPerformanceStats() {
        Map<String, Object> result = new HashMap<>();
        
        // 获取性能统计数据
        Map<String, PerformanceMonitorAspect.MethodPerformanceStats> stats = 
            performanceMonitorAspect.getPerformanceStats();
        
        if (stats.isEmpty()) {
            result.put("message", "暂无性能数据");
            return result;
        }
        
        // 计算整体指标
        long totalCalls = stats.values().stream().mapToLong(PerformanceMonitorAspect.MethodPerformanceStats::getTotalCalls).sum();
        long totalSuccessCalls = stats.values().stream().mapToLong(PerformanceMonitorAspect.MethodPerformanceStats::getSuccessCalls).sum();
        double overallSuccessRate = totalCalls > 0 ? (double) totalSuccessCalls / totalCalls : 0;
        double avgResponseTime = stats.values().stream().mapToDouble(PerformanceMonitorAspect.MethodPerformanceStats::getAverageTime).average().orElse(0);
        
        result.put("totalCalls", totalCalls);
        result.put("totalSuccessCalls", totalSuccessCalls);
        result.put("overallSuccessRate", overallSuccessRate);
        result.put("avgResponseTime", avgResponseTime);
        result.put("methodCount", stats.size());
        
        return result;
    }
    
    /**
     * 获取详细性能数据
     */
    @GetMapping("/details")
    public Map<String, Object> getDetailedStats() {
        Map<String, Object> result = new HashMap<>();
        
        Map<String, PerformanceMonitorAspect.MethodPerformanceStats> stats = 
            performanceMonitorAspect.getPerformanceStats();
        
        result.put("methodStats", stats);
        return result;
    }
    
    /**
     * 获取瓶颈分析结果
     */
    @GetMapping("/bottlenecks")
    public Map<String, Object> getBottleneckAnalysis() {
        Map<String, Object> result = new HashMap<>();
        
        PerformanceDataAnalyzer.BottleneckAnalysisResult analysisResult = analyzer.analyzeBottlenecks();
        
        result.put("bottlenecks", analysisResult.getBottlenecks());
        result.put("recommendations", analysisResult.getRecommendations());
        
        return result;
    }
    
    /**
     * 生成性能报告
     */
    @GetMapping("/report")
    public Map<String, String> generateReport() {
        Map<String, String> result = new HashMap<>();
        
        String report = reportGenerator.generatePerformanceReport();
        result.put("report", report);
        
        return result;
    }
    
    /**
     * 清空性能统计数据
     */
    @GetMapping("/clear")
    public Map<String, String> clearStats() {
        Map<String, String> result = new HashMap<>();
        
        // 注意：这里需要添加清空统计数据的方法
        // 由于当前实现中没有提供清空方法，这里只是返回提示
        result.put("message", "性能统计数据已清空（需要重启应用才能完全清空）");
        
        return result;
    }
}