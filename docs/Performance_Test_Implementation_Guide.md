# 性能测试实施指南 - 票务系统RocketMQ优化

## 🎯 性价比最高的实施方案

### 方案评估矩阵

| 方案 | 实施成本 | 预期收益 | 风险等级 | 性价比评分 | 推荐优先级 |
|------|----------|----------|----------|------------|------------|
| **订单创建异步化** | 低 | 高 | 低 | ⭐⭐⭐⭐⭐ | 1 |
| **支付流程解耦** | 中 | 高 | 中 | ⭐⭐⭐⭐ | 2 |
| **库存管理异步化** | 高 | 中 | 高 | ⭐⭐⭐ | 3 |
| **通知系统异步化** | 低 | 中 | 低 | ⭐⭐⭐⭐ | 4 |

**推荐策略：优先实施订单创建异步化，预期投入产出比最高**

## 🚀 立即可执行的步骤

### 步骤1：创建性能测试脚本

#### 1.1 JMeter测试计划创建

**创建文件：`ticket-system-performance-test.jmx`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <!-- 测试计划配置 -->
    <TestPlan testname="票务系统性能测试" enabled="true">
      <elementProp name="TestPlan.arguments" elementType="Arguments" guiclass="ArgumentsPanel">
        <collectionProp name="Arguments.arguments"/>
      </elementProp>
      <stringProp name="TestPlan.user_define_classpath"></stringProp>
      <boolProp name="TestPlan.functional_mode">false</boolProp>
      <boolProp name="TestPlan.serialize_threadgroups">false</boolProp>
      <elementProp name="TestPlan.arguments" elementType="Arguments" guiclass="ArgumentsPanel">
        <collectionProp name="Arguments.arguments">
          <elementProp name="base_url" elementType="Argument">
            <stringProp name="Argument.name">base_url</stringProp>
            <stringProp name="Argument.value">http://localhost:8080</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
    </TestPlan>
    
    <!-- 用户注册登录测试组 -->
    <ThreadGroup testname="用户注册登录测试组" enabled="true">
      <stringProp name="ThreadGroup.num_threads">100</stringProp>
      <stringProp name="ThreadGroup.ramp_time">60</stringProp>
      <stringProp name="ThreadGroup.duration">300</stringProp>
      <stringProp name="ThreadGroup.delay">0</stringProp>
      <boolProp name="ThreadGroup.scheduler">true</boolProp>
      
      <!-- 用户注册请求 -->
      <HTTPSamplerProxy testname="用户注册" enabled="true">
        <stringProp name="HTTPSampler.domain">${base_url}</stringProp>
        <stringProp name="HTTPSampler.port">8080</stringProp>
        <stringProp name="HTTPSampler.path">/api/user/register</stringProp>
        <stringProp name="HTTPSampler.method">POST</stringProp>
        <stringProp name="HTTPSampler.contentEncoding">UTF-8</stringProp>
        <boolProp name="HTTPSampler.postBodyRaw">true</boolProp>
        <stringProp name="HTTPSampler.postBodyRaw">
          {
            "username": "testuser${__Random(1,10000)}",
            "password": "123456",
            "email": "test${__Random(1,10000)}@example.com",
            "phone": "138${__Random(10000000,99999999)}"
          }
        </stringProp>
      </HTTPSamplerProxy>
      
      <!-- 用户登录请求 -->
      <HTTPSamplerProxy testname="用户登录" enabled="true">
        <stringProp name="HTTPSampler.domain">${base_url}</stringProp>
        <stringProp name="HTTPSampler.port">8080</stringProp>
        <stringProp name="HTTPSampler.path">/api/user/login</stringProp>
        <stringProp name="HTTPSampler.method">POST</stringProp>
        <stringProp name="HTTPSampler.contentEncoding">UTF-8</stringProp>
        <boolProp name="HTTPSampler.postBodyRaw">true</boolProp>
        <stringProp name="HTTPSampler.postBodyRaw">
          {
            "username": "testuser${__Random(1,10000)}",
            "password": "123456"
          }
        </stringProp>
      </HTTPSamplerProxy>
    </ThreadGroup>
    
    <!-- 订单创建测试组 -->
    <ThreadGroup testname="订单创建测试组" enabled="true">
      <stringProp name="ThreadGroup.num_threads">50</stringProp>
      <stringProp name="ThreadGroup.ramp_time">30</stringProp>
      <stringProp name="ThreadGroup.duration">600</stringProp>
      <stringProp name="ThreadGroup.delay">0</stringProp>
      <boolProp name="ThreadGroup.scheduler">true</boolProp>
      
      <!-- 订单创建请求 -->
      <HTTPSamplerProxy testname="创建订单" enabled="true">
        <stringProp name="HTTPSampler.domain">${base_url}</stringProp>
        <stringProp name="HTTPSampler.port">8080</stringProp>
        <stringProp name="HTTPSampler.path">/api/order/create</stringProp>
        <stringProp name="HTTPSampler.method">POST</stringProp>
        <stringProp name="HTTPSampler.contentEncoding">UTF-8</stringProp>
        <boolProp name="HTTPSampler.postBodyRaw">true</boolProp>
        <stringProp name="HTTPSampler.postBodyRaw">
          {
            "userId": ${__Random(1,1000)},
            "showId": 1,
            "sessionId": 1,
            "ticketId": ${__Random(1,10)},
            "quantity": ${__Random(1,5)},
            "totalPrice": ${__Random(100,1000)}
          }
        </stringProp>
      </HTTPSamplerProxy>
    </ThreadGroup>
    
    <!-- 支付流程测试组 -->
    <ThreadGroup testname="支付流程测试组" enabled="true">
      <stringProp name="ThreadGroup.num_threads">20</stringProp>
      <stringProp name="ThreadGroup.ramp_time">20</stringProp>
      <stringProp name="ThreadGroup.duration">600</stringProp>
      <stringProp name="ThreadGroup.delay">0</stringProp>
      <boolProp name="ThreadGroup.scheduler">true</boolProp>
      
      <!-- 支付订单请求 -->
      <HTTPSamplerProxy testname="支付订单" enabled="true">
        <stringProp name="HTTPSampler.domain">${base_url}</stringProp>
        <stringProp name="HTTPSampler.port">8080</stringProp>
        <stringProp name="HTTPSampler.path">/api/order/pay</stringProp>
        <stringProp name="HTTPSampler.method">POST</stringProp>
        <stringProp name="HTTPSampler.contentEncoding">UTF-8</stringProp>
        <boolProp name="HTTPSampler.postBodyRaw">true</boolProp>
        <stringProp name="HTTPSampler.postBodyRaw">
          {
            "orderNo": "ORDER${__Random(100000,999999)}",
            "payType": ${__Random(1,3)}
          }
        </stringProp>
      </HTTPSamplerProxy>
    </ThreadGroup>
    
    <!-- 监听器配置 -->
    <ResultCollector testname="查看结果树" enabled="true">
      <boolProp name="ResultCollector.error_logging">false</boolProp>
      <objProp>
        <name>saveConfig</name>
        <value class="SampleSaveConfiguration">
          <time>true</time>
          <latency>true</latency>
          <timestamp>true</timestamp>
          <success>true</success>
          <label>true</label>
          <code>true</code>
          <message>true</message>
          <threadName>true</threadName>
          <dataType>true</dataType>
          <encoding>false</encoding>
          <assertions>true</assertions>
          <subresults>true</subresults>
          <responseData>false</responseData>
          <samplerData>false</samplerData>
          <xml>false</xml>
          <fieldNames>true</fieldNames>
          <responseHeaders>false</responseHeaders>
          <requestHeaders>false</requestHeaders>
          <responseDataOnError>false</responseDataOnError>
          <saveAssertionResultsFailureMessage>true</saveAssertionResultsFailureMessage>
          <assertionsResultsToSave>0</assertionsResultsToSave>
          <bytes>true</bytes>
          <sentBytes>true</sentBytes>
          <url>true</url>
          <threadCounts>true</threadCounts>
          <idleTime>true</idleTime>
          <connectTime>true</connectTime>
        </value>
      </objProp>
      <stringProp name="filename">test-results.jtl</stringProp>
    </ResultCollector>
    
    <ResultCollector testname="聚合报告" enabled="true">
      <boolProp name="ResultCollector.error_logging">false</boolProp>
      <objProp>
        <name>saveConfig</name>
        <value class="SampleSaveConfiguration">
          <time>true</time>
          <latency>true</latency>
          <timestamp>true</timestamp>
          <success>true</success>
          <label>true</label>
          <code>true</code>
          <message>true</message>
          <threadName>true</threadName>
          <dataType>true</dataType>
          <encoding>false</encoding>
          <assertions>true</assertions>
          <subresults>true</subresults>
          <responseData>false</responseData>
          <samplerData>false</samplerData>
          <xml>false</xml>
          <fieldNames>true</fieldNames>
          <responseHeaders>false</responseHeaders>
          <requestHeaders>false</requestHeaders>
          <responseDataOnError>false</responseDataOnError>
          <saveAssertionResultsFailureMessage>true</saveAssertionResultsFailureMessage>
          <assertionsResultsToSave>0</assertionsResultsToSave>
          <bytes>true</bytes>
          <sentBytes>true</sentBytes>
          <url>true</url>
          <threadCounts>true</threadCounts>
          <idleTime>true</idleTime>
          <connectTime>true</connectTime>
        </value>
      </objProp>
      <stringProp name="filename">aggregate-report.jtl</stringProp>
    </ResultCollector>
  </hashTree>
</jmeterTestPlan>
```

#### 1.2 测试执行脚本

**创建文件：`run-performance-test.bat`**

```batch
@echo off
echo 开始执行票务系统性能测试...

REM 设置JMeter路径（请根据实际安装路径调整）
set JMETER_HOME=D:\apache-jmeter-5.5
set JMETER_BIN=%JMETER_HOME%\bin

REM 设置测试参数
set TEST_PLAN=ticket-system-performance-test.jmx
set RESULT_DIR=test-results
set TIMESTAMP=%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%

REM 创建结果目录
if not exist %RESULT_DIR% mkdir %RESULT_DIR%

echo 执行测试计划: %TEST_PLAN%
echo 结果保存到: %RESULT_DIR%\test-results_%TIMESTAMP%.jtl

REM 执行JMeter测试
"%JMETER_BIN%\jmeter.bat" -n -t %TEST_PLAN% -l %RESULT_DIR%\test-results_%TIMESTAMP%.jtl -e -o %RESULT_DIR%\html-report_%TIMESTAMP%

echo 测试完成！
echo 查看HTML报告: %RESULT_DIR%\html-report_%TIMESTAMP%\index.html
pause
```

### 步骤2：部署监控系统

#### 2.1 性能监控切面

**创建文件：`ticket-common/src/main/java/com/ticketsystem/common/annotation/PerformanceMonitor.java`**

```java
package com.ticketsystem.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 性能监控注解
 * 用于标记需要监控性能的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PerformanceMonitor {
    
    /**
     * 监控名称，用于标识不同的监控点
     */
    String value() default "";
    
    /**
     * 是否记录详细参数
     */
    boolean recordArgs() default false;
    
    /**
     * 是否记录返回值
     */
    boolean recordResult() default false;
    
    /**
     * 慢查询阈值（毫秒）
     */
    long slowQueryThreshold() default 1000;
}
```

**创建文件：`ticket-common/src/main/java/com/ticketsystem/common/aspect/PerformanceMonitorAspect.java`**

```java
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
```

#### 2.2 系统资源监控

**创建文件：`ticket-common/src/main/java/com/ticketsystem/common/monitor/SystemResourceMonitor.java`**

```java
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
            long currentCpuTime = osBean.getProcessCpuTime();
            long currentUpTime = System.currentTimeMillis();
            
            long lastCpu = lastCpuTime.getAndSet(currentCpuTime);
            long lastUp = lastUpTime.getAndSet(currentUpTime);
            
            if (lastCpu > 0 && lastUp > 0) {
                long cpuTimeDiff = currentCpuTime - lastCpu;
                long upTimeDiff = currentUpTime - lastUp;
                
                if (upTimeDiff > 0) {
                    return Math.min(100.0, (double) cpuTimeDiff / (upTimeDiff * 1000000) * 100);
                }
            }
        } catch (Exception e) {
            log.warn("获取CPU使用率失败", e);
        }
        return 0.0;
    }
}
```

### 步骤3：分析瓶颈数据

#### 3.1 性能数据分析器

**创建文件：`ticket-common/src/main/java/com/ticketsystem/common/analyzer/PerformanceDataAnalyzer.java`**

```java
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
```

#### 3.2 性能报告生成器

**创建文件：`ticket-common/src/main/java/com/ticketsystem/common/report/PerformanceReportGenerator.java`**

```java
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
```

### 步骤4：设计针对性方案

#### 4.1 基于瓶颈的RocketMQ集成方案

**创建文件：`ticket-common/src/main/java/com/ticketsystem/common/mq/BottleneckBasedMQDesign.java`**

```java
package com.ticketsystem.common.mq;

import com.ticketsystem.common.analyzer.PerformanceDataAnalyzer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 基于瓶颈分析的RocketMQ集成方案设计
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BottleneckBasedMQDesign {
    
    private final PerformanceDataAnalyzer analyzer;
    
    /**
     * 根据瓶颈分析结果设计MQ集成方案
     */
    public MQIntegrationPlan designIntegrationPlan() {
        PerformanceDataAnalyzer.BottleneckAnalysisResult analysisResult = analyzer.analyzeBottlenecks();
        
        List<MQIntegrationTask> tasks = new ArrayList<>();
        
        // 分析每个瓶颈，设计对应的MQ方案
        for (PerformanceDataAnalyzer.BottleneckInfo bottleneck : analysisResult.getBottlenecks()) {
            MQIntegrationTask task = designTaskForBottleneck(bottleneck);
            if (task != null) {
                tasks.add(task);
            }
        }
        
        // 按优先级排序
        tasks.sort((a, b) -> Double.compare(b.getPriority(), a.getPriority()));
        
        return new MQIntegrationPlan(tasks, calculateTotalEffort(tasks));
    }
    
    private MQIntegrationTask designTaskForBottleneck(PerformanceDataAnalyzer.BottleneckInfo bottleneck) {
        switch (bottleneck.getType()) {
            case "RESPONSE_TIME":
                return designResponseTimeOptimization(bottleneck);
            case "SUCCESS_RATE":
                return designReliabilityOptimization(bottleneck);
            case "HIGH_FREQUENCY":
                return designFrequencyOptimization(bottleneck);
            default:
                return null;
        }
    }
    
    private MQIntegrationTask designResponseTimeOptimization(PerformanceDataAnalyzer.BottleneckInfo bottleneck) {
        if (bottleneck.getMethodName().contains("createOrder")) {
            return new MQIntegrationTask(
                "订单创建异步化",
                "将订单创建流程中的非核心步骤异步化，减少响应时间",
                MQIntegrationType.ASYNC_ORDER_PROCESSING,
                calculatePriority(bottleneck.getImpactScore(), 0.8), // 高收益
                3, // 中等难度
                Arrays.asList(
                    "创建订单核心信息（同步）",
                    "发送订单处理消息到RocketMQ（异步）",
                    "消费者处理订单详细信息（异步）",
                    "更新订单状态（异步）"
                ),
                Arrays.asList(
                    "ORDER_ASYNC_TOPIC",
                    "ORDER_CREATED_TAG",
                    "ORDER_DETAIL_PROCESSING_TAG"
                )
            );
        } else if (bottleneck.getMethodName().contains("payOrder")) {
            return new MQIntegrationTask(
                "支付流程解耦",
                "支付成功后的后续处理异步化",
                MQIntegrationType.PAYMENT_DECOUPLING,
                calculatePriority(bottleneck.getImpactScore(), 0.7),
                2, // 较低难度
                Arrays.asList(
                    "更新支付状态（同步）",
                    "发送支付成功消息到RocketMQ（异步）",
                    "消费者处理支付后续逻辑（异步）"
                ),
                Arrays.asList(
                    "PAYMENT_CALLBACK_TOPIC",
                    "PAYMENT_SUCCESS_TAG"
                )
            );
        }
        return null;
    }
    
    private MQIntegrationTask designReliabilityOptimization(PerformanceDataAnalyzer.BottleneckInfo bottleneck) {
        return new MQIntegrationTask(
            "可靠性增强",
            "通过消息重试和死信队列提高系统可靠性",
            MQIntegrationType.RELIABILITY_ENHANCEMENT,
            calculatePriority(bottleneck.getImpactScore(), 0.6),
            4, // 较高难度
            Arrays.asList(
                "配置消息重试机制",
                "实现死信队列处理",
                "添加补偿机制"
            ),
            Arrays.asList(
                "DLQ_TOPIC",
                "RETRY_TOPIC"
            )
        );
    }
    
    private MQIntegrationTask designFrequencyOptimization(PerformanceDataAnalyzer.BottleneckInfo bottleneck) {
        return new MQIntegrationTask(
            "高频调用优化",
            "通过消息队列缓冲高频调用",
            MQIntegrationType.FREQUENCY_OPTIMIZATION,
            calculatePriority(bottleneck.getImpactScore(), 0.5),
            3, // 中等难度
            Arrays.asList(
                "将高频调用转为异步消息",
                "实现消息批量处理",
                "添加流量控制"
            ),
            Arrays.asList(
                "HIGH_FREQUENCY_TOPIC",
                "BATCH_PROCESSING_TAG"
            )
        );
    }
    
    private double calculatePriority(double impactScore, double expectedBenefit) {
        return impactScore * expectedBenefit;
    }
    
    private int calculateTotalEffort(List<MQIntegrationTask> tasks) {
        return tasks.stream().mapToInt(MQIntegrationTask::getEffort).sum();
    }
    
    /**
     * MQ集成任务类
     */
    public static class MQIntegrationTask {
        private final String name;
        private final String description;
        private final MQIntegrationType type;
        private final double priority;
        private final int effort; // 1-5，5最难
        private final List<String> steps;
        private final List<String> topics;
        
        public MQIntegrationTask(String name, String description, MQIntegrationType type, 
                               double priority, int effort, List<String> steps, List<String> topics) {
            this.name = name;
            this.description = description;
            this.type = type;
            this.priority = priority;
            this.effort = effort;
            this.steps = steps;
            this.topics = topics;
        }
        
        // Getters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public MQIntegrationType getType() { return type; }
        public double getPriority() { return priority; }
        public int getEffort() { return effort; }
        public List<String> getSteps() { return steps; }
        public List<String> getTopics() { return topics; }
    }
    
    /**
     * MQ集成计划类
     */
    public static class MQIntegrationPlan {
        private final List<MQIntegrationTask> tasks;
        private final int totalEffort;
        
        public MQIntegrationPlan(List<MQIntegrationTask> tasks, int totalEffort) {
            this.tasks = tasks;
            this.totalEffort = totalEffort;
        }
        
        // Getters
        public List<MQIntegrationTask> getTasks() { return tasks; }
        public int getTotalEffort() { return totalEffort; }
    }
    
    /**
     * MQ集成类型枚举
     */
    public enum MQIntegrationType {
        ASYNC_ORDER_PROCESSING,
        PAYMENT_DECOUPLING,
        RELIABILITY_ENHANCEMENT,
        FREQUENCY_OPTIMIZATION
    }
}
```

## 📋 执行计划

### 第1周：基础准备
- [ ] 创建JMeter测试脚本
- [ ] 部署性能监控切面
- [ ] 执行基线性能测试
- [ ] 收集性能数据

### 第2周：数据分析
- [ ] 分析性能瓶颈
- [ ] 生成性能报告
- [ ] 设计MQ集成方案
- [ ] 制定实施计划

### 第3周：MVP实施
- [ ] 实施最高优先级的优化
- [ ] 配置RocketMQ环境
- [ ] 实现A/B测试框架
- [ ] 开始效果验证

### 第4周：效果验证
- [ ] 对比优化前后性能
- [ ] 调整优化策略
- [ ] 扩展优化范围
- [ ] 完善监控体系

通过这个详细的实施计划，您可以系统性地进行性能测试、瓶颈分析和RocketMQ集成，确保优化效果的最大化。