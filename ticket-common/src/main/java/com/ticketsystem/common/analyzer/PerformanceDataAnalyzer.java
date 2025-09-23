package com.ticketsystem.common.analyzer;

import com.ticketsystem.common.aspect.PerformanceMonitorAspect;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 性能数据分析器
 * 分析性能监控数据，识别瓶颈
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PerformanceDataAnalyzer {
    
    private final PerformanceMonitorAspect performanceMonitorAspect;
    
    /**
     * 分析性能瓶颈
     */
    public BottleneckAnalysisResult analyzeBottlenecks() {
        Map<String, PerformanceMonitorAspect.MethodPerformanceStats> stats = 
            performanceMonitorAspect.getPerformanceStats();
        
        List<BottleneckInfo> bottlenecks = new ArrayList<>();
        
        for (Map.Entry<String, PerformanceMonitorAspect.MethodPerformanceStats> entry : stats.entrySet()) {
            String methodName = entry.getKey();
            PerformanceMonitorAspect.MethodPerformanceStats methodStats = entry.getValue();
            
            // 分析响应时间瓶颈
            if (methodStats.getAverageTime() > 2000) {
                bottlenecks.add(new BottleneckInfo(
                    methodName,
                    "RESPONSE_TIME",
                    "平均响应时间过长: " + String.format("%.2f", methodStats.getAverageTime()) + "ms",
                    calculateImpactScore(methodStats.getAverageTime(), 2000),
                    "考虑异步化处理"
                ));
            }
            
            // 分析成功率瓶颈
            if (methodStats.getSuccessRate() < 0.95) {
                bottlenecks.add(new BottleneckInfo(
                    methodName,
                    "SUCCESS_RATE",
                    "成功率过低: " + String.format("%.2f", methodStats.getSuccessRate() * 100) + "%",
                    calculateImpactScore(1 - methodStats.getSuccessRate(), 0.05),
                    "检查异常处理和重试机制"
                ));
            }
            
            // 分析调用频率瓶颈
            if (methodStats.getTotalCalls() > 1000) {
                bottlenecks.add(new BottleneckInfo(
                    methodName,
                    "HIGH_FREQUENCY",
                    "调用频率过高: " + methodStats.getTotalCalls() + " 次",
                    calculateImpactScore(methodStats.getTotalCalls(), 1000),
                    "考虑缓存或异步处理"
                ));
            }
        }
        
        // 按影响程度排序
        bottlenecks.sort((a, b) -> Double.compare(b.getImpactScore(), a.getImpactScore()));
        
        return new BottleneckAnalysisResult(bottlenecks, generateRecommendations(bottlenecks));
    }
    
    private double calculateImpactScore(double actual, double threshold) {
        return Math.max(0, (actual - threshold) / threshold);
    }
    
    private List<String> generateRecommendations(List<BottleneckInfo> bottlenecks) {
        List<String> recommendations = new ArrayList<>();
        
        // 基于瓶颈类型生成建议
        Map<String, Long> bottleneckTypes = bottlenecks.stream()
            .collect(Collectors.groupingBy(BottleneckInfo::getType, Collectors.counting()));
        
        if (bottleneckTypes.getOrDefault("RESPONSE_TIME", 0L) > 0) {
            recommendations.add("响应时间瓶颈较多，建议优先实施订单创建异步化");
        }
        
        if (bottleneckTypes.getOrDefault("SUCCESS_RATE", 0L) > 0) {
            recommendations.add("成功率问题较多，建议加强异常处理和重试机制");
        }
        
        if (bottleneckTypes.getOrDefault("HIGH_FREQUENCY", 0L) > 0) {
            recommendations.add("高频调用较多，建议实施缓存和异步处理");
        }
        
        return recommendations;
    }
    
    /**
     * 瓶颈信息类
     */
    public static class BottleneckInfo {
        private final String methodName;
        private final String type;
        private final String description;
        private final double impactScore;
        private final String suggestion;
        
        public BottleneckInfo(String methodName, String type, String description, double impactScore, String suggestion) {
            this.methodName = methodName;
            this.type = type;
            this.description = description;
            this.impactScore = impactScore;
            this.suggestion = suggestion;
        }
        
        // Getters
        public String getMethodName() { return methodName; }
        public String getType() { return type; }
        public String getDescription() { return description; }
        public double getImpactScore() { return impactScore; }
        public String getSuggestion() { return suggestion; }
    }
    
    /**
     * 瓶颈分析结果类
     */
    public static class BottleneckAnalysisResult {
        private final List<BottleneckInfo> bottlenecks;
        private final List<String> recommendations;
        
        public BottleneckAnalysisResult(List<BottleneckInfo> bottlenecks, List<String> recommendations) {
            this.bottlenecks = bottlenecks;
            this.recommendations = recommendations;
        }
        
        // Getters
        public List<BottleneckInfo> getBottlenecks() { return bottlenecks; }
        public List<String> getRecommendations() { return recommendations; }
    }
}