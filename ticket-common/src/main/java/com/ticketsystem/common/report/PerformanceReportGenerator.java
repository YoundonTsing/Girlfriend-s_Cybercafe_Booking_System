package com.ticketsystem.common.report;

import com.ticketsystem.common.analyzer.PerformanceDataAnalyzer;
import com.ticketsystem.common.aspect.PerformanceMonitorAspect;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 性能报告生成器
 * 生成详细的性能分析报告
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PerformanceReportGenerator {
    
    private final PerformanceDataAnalyzer analyzer;
    private final PerformanceMonitorAspect performanceMonitorAspect;
    
    /**
     * 生成性能分析报告
     */
    public String generatePerformanceReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("=== 票务系统性能分析报告 ===\n");
        report.append("生成时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        
        // 1. 整体性能概览
        report.append("## 1. 整体性能概览\n");
        Map<String, PerformanceMonitorAspect.MethodPerformanceStats> stats = 
            performanceMonitorAspect.getPerformanceStats();
        
        if (stats.isEmpty()) {
            report.append("暂无性能数据，请先运行性能测试\n\n");
            return report.toString();
        }
        
        // 计算整体指标
        long totalCalls = stats.values().stream().mapToLong(PerformanceMonitorAspect.MethodPerformanceStats::getTotalCalls).sum();
        long totalSuccessCalls = stats.values().stream().mapToLong(PerformanceMonitorAspect.MethodPerformanceStats::getSuccessCalls).sum();
        double overallSuccessRate = totalCalls > 0 ? (double) totalSuccessCalls / totalCalls : 0;
        double avgResponseTime = stats.values().stream().mapToDouble(PerformanceMonitorAspect.MethodPerformanceStats::getAverageTime).average().orElse(0);
        
        report.append(String.format("总调用次数: %d\n", totalCalls));
        report.append(String.format("总成功次数: %d\n", totalSuccessCalls));
        report.append(String.format("整体成功率: %.2f%%\n", overallSuccessRate * 100));
        report.append(String.format("平均响应时间: %.2fms\n\n", avgResponseTime));
        
        // 2. 方法性能详情
        report.append("## 2. 方法性能详情\n");
        report.append("| 方法名 | 调用次数 | 成功次数 | 成功率 | 平均响应时间(ms) | 最大响应时间(ms) | 最小响应时间(ms) |\n");
        report.append("|--------|----------|----------|--------|------------------|------------------|------------------|\n");
        
        stats.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue().getAverageTime(), a.getValue().getAverageTime()))
            .forEach(entry -> {
                PerformanceMonitorAspect.MethodPerformanceStats methodStats = entry.getValue();
                report.append(String.format("| %s | %d | %d | %.2f%% | %.2f | %d | %d |\n",
                    entry.getKey(),
                    methodStats.getTotalCalls(),
                    methodStats.getSuccessCalls(),
                    methodStats.getSuccessRate() * 100,
                    methodStats.getAverageTime(),
                    methodStats.getMaxTime(),
                    methodStats.getMinTime()
                ));
            });
        
        report.append("\n");
        
        // 3. 瓶颈分析
        report.append("## 3. 瓶颈分析\n");
        PerformanceDataAnalyzer.BottleneckAnalysisResult analysisResult = analyzer.analyzeBottlenecks();
        
        if (analysisResult.getBottlenecks().isEmpty()) {
            report.append("未发现明显性能瓶颈\n\n");
        } else {
            report.append("| 方法名 | 瓶颈类型 | 问题描述 | 影响程度 | 建议措施 |\n");
            report.append("|--------|----------|----------|----------|----------|\n");
            
            analysisResult.getBottlenecks().forEach(bottleneck -> {
                report.append(String.format("| %s | %s | %s | %.2f | %s |\n",
                    bottleneck.getMethodName(),
                    bottleneck.getType(),
                    bottleneck.getDescription(),
                    bottleneck.getImpactScore(),
                    bottleneck.getSuggestion()
                ));
            });
            
            report.append("\n");
        }
        
        // 4. 优化建议
        report.append("## 4. 优化建议\n");
        if (analysisResult.getRecommendations().isEmpty()) {
            report.append("系统性能良好，无需特别优化\n");
        } else {
            for (int i = 0; i < analysisResult.getRecommendations().size(); i++) {
                report.append(String.format("%d. %s\n", i + 1, analysisResult.getRecommendations().get(i)));
            }
        }
        
        report.append("\n=== 报告结束 ===\n");
        
        return report.toString();
    }
}