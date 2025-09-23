# 库存监控告警快速实施方案

## 🚀 2小时内完成的监控告警实现

### 第一步：添加监控依赖 (5分钟)

在 `ticket-show/pom.xml` 中添加：

```xml
<!-- 在现有dependencies中添加 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### 第二步：创建监控配置 (10分钟)

创建 `StockMonitoringConfig.java`：

```java
package com.ticketsystem.show.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StockMonitoringConfig {
    
    @Bean
    public Counter stockOperationCounter(MeterRegistry meterRegistry) {
        return Counter.builder("stock_operations_total")
                .description("Total number of stock operations")
                .register(meterRegistry);
    }
    
    @Bean
    public Timer stockOperationTimer(MeterRegistry meterRegistry) {
        return Timer.builder("stock_operation_duration_seconds")
                .description("Duration of stock operations")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter stockAlertCounter(MeterRegistry meterRegistry) {
        return Counter.builder("stock_alerts_total")
                .description("Total number of stock alerts")
                .register(meterRegistry);
    }
}
```

### 第三步：创建监控切面 (20分钟)

创建 `StockMonitoringAspect.java`：

```java
package com.ticketsystem.show.aspect;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class StockMonitoringAspect {
    
    private final Counter stockOperationCounter;
    private final Timer stockOperationTimer;
    private final Counter stockAlertCounter;
    
    @Around("execution(* com.ticketsystem.show.service.impl.TicketStockServiceImpl.*(..))")
    public Object monitorStockOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        Timer.Sample sample = Timer.Sample.start();
        
        try {
            Object result = joinPoint.proceed();
            
            // 记录成功操作
            sample.stop(stockOperationTimer.tag("method", methodName).tag("status", "success"));
            stockOperationCounter.tag("method", methodName).tag("status", "success").increment();
            
            // 检查操作结果并触发告警
            checkOperationResult(methodName, args, result);
            
            return result;
            
        } catch (Exception e) {
            // 记录失败操作
            sample.stop(stockOperationTimer.tag("method", methodName).tag("status", "error"));
            stockOperationCounter.tag("method", methodName).tag("status", "error")
                    .tag("error_type", e.getClass().getSimpleName()).increment();
            
            // 触发错误告警
            triggerErrorAlert(methodName, args, e);
            
            throw e;
        }
    }
    
    private void checkOperationResult(String methodName, Object[] args, Object result) {
        // 检查库存锁定失败
        if ("lockStock".equals(methodName) && Boolean.FALSE.equals(result)) {
            Long ticketId = (Long) args[0];
            Integer quantity = (Integer) args[1];
            
            log.warn("🔒 库存锁定失败告警 - 票档ID: {}, 请求数量: {}", ticketId, quantity);
            stockAlertCounter.tag("type", "lock_failed").tag("ticket_id", ticketId.toString()).increment();
        }
        
        // 检查库存确认失败
        if ("confirmStock".equals(methodName) && Boolean.FALSE.equals(result)) {
            Long ticketId = (Long) args[0];
            Integer quantity = (Integer) args[1];
            
            log.warn("✅ 库存确认失败告警 - 票档ID: {}, 确认数量: {}", ticketId, quantity);
            stockAlertCounter.tag("type", "confirm_failed").tag("ticket_id", ticketId.toString()).increment();
        }
    }
    
    private void triggerErrorAlert(String methodName, Object[] args, Exception e) {
        Long ticketId = args.length > 0 ? (Long) args[0] : null;
        
        log.error("❌ 库存操作异常告警 - 方法: {}, 票档ID: {}, 异常: {}", 
                methodName, ticketId, e.getMessage());
        
        stockAlertCounter.tag("type", "operation_error")
                .tag("method", methodName)
                .tag("error_type", e.getClass().getSimpleName())
                .increment();
    }
}
```

### 第四步：创建库存水位监控 (30分钟)

创建 `StockLevelMonitor.java`：

```java
package com.ticketsystem.show.monitor;

import com.ticketsystem.show.service.TicketStockService;
import com.ticketsystem.show.entity.TicketStock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockLevelMonitor {
    
    private final TicketStockService ticketStockService;
    private final MeterRegistry meterRegistry;
    private final Counter stockAlertCounter;
    
    // 缓存当前监控的票档库存信息
    private final ConcurrentMap<Long, Double> currentStockRatios = new ConcurrentHashMap<>();
    
    // 测试用的票档ID列表（实际应该从数据库获取活跃票档）
    private final List<Long> monitoredTickets = Arrays.asList(1L, 2L, 3L, 4L, 5L);
    
    @PostConstruct
    public void initGauges() {
        // 注册库存比例指标
        Gauge.builder("stock_ratio")
                .description("Current stock ratio (available/total)")
                .register(meterRegistry, this, StockLevelMonitor::getAverageStockRatio);
    }
    
    @Scheduled(fixedRate = 30000) // 每30秒检查一次
    public void monitorStockLevels() {
        log.debug("开始库存水位检查...");
        
        for (Long ticketId : monitoredTickets) {
            try {
                checkTicketStockLevel(ticketId);
            } catch (Exception e) {
                log.error("检查票档 {} 库存水位时发生异常", ticketId, e);
            }
        }
    }
    
    private void checkTicketStockLevel(Long ticketId) {
        TicketStock stock = ticketStockService.getStockInfo(ticketId);
        if (stock == null) {
            return; // 票档不存在，跳过
        }
        
        Integer totalStock = stock.getTotalStock();
        Integer availableStock = stock.getAvailableStock();
        
        if (totalStock == null || totalStock <= 0) {
            return; // 无效库存数据
        }
        
        double stockRatio = (double) availableStock / totalStock;
        currentStockRatios.put(ticketId, stockRatio);
        
        // 注册单个票档的库存比例指标
        Gauge.builder("ticket_stock_ratio")
                .tag("ticket_id", ticketId.toString())
                .description("Stock ratio for specific ticket")
                .register(meterRegistry, stockRatio, ratio -> ratio);
        
        // 库存告警检查
        checkStockAlerts(ticketId, availableStock, totalStock, stockRatio);
        
        log.debug("票档 {} 库存检查 - 总库存: {}, 可用库存: {}, 库存比例: {:.2%}", 
                ticketId, totalStock, availableStock, stockRatio);
    }
    
    private void checkStockAlerts(Long ticketId, Integer availableStock, Integer totalStock, double stockRatio) {
        // 库存严重不足告警 (< 5%)
        if (stockRatio < 0.05) {
            log.error("🚨 库存严重不足告警 - 票档ID: {}, 剩余库存: {}/{} ({:.1%})", 
                    ticketId, availableStock, totalStock, stockRatio);
            stockAlertCounter.tag("type", "critical_low_stock")
                    .tag("ticket_id", ticketId.toString()).increment();
        }
        // 库存不足告警 (< 10%)
        else if (stockRatio < 0.10) {
            log.warn("⚠️ 库存不足告警 - 票档ID: {}, 剩余库存: {}/{} ({:.1%})", 
                    ticketId, availableStock, totalStock, stockRatio);
            stockAlertCounter.tag("type", "low_stock")
                    .tag("ticket_id", ticketId.toString()).increment();
        }
        // 库存预警 (< 20%)
        else if (stockRatio < 0.20) {
            log.info("📊 库存预警 - 票档ID: {}, 剩余库存: {}/{} ({:.1%})", 
                    ticketId, availableStock, totalStock, stockRatio);
            stockAlertCounter.tag("type", "stock_warning")
                    .tag("ticket_id", ticketId.toString()).increment();
        }
        
        // 库存耗尽告警
        if (availableStock <= 0) {
            log.error("💥 库存耗尽告警 - 票档ID: {}, 库存已售罄！", ticketId);
            stockAlertCounter.tag("type", "stock_exhausted")
                    .tag("ticket_id", ticketId.toString()).increment();
        }
    }
    
    // 计算平均库存比例（用于Gauge指标）
    private double getAverageStockRatio() {
        if (currentStockRatios.isEmpty()) {
            return 0.0;
        }
        return currentStockRatios.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }
    
    // 手动触发库存检查（用于测试）
    public void triggerManualCheck() {
        log.info("手动触发库存水位检查");
        monitorStockLevels();
    }
}
```

### 第五步：配置监控端点 (10分钟)

在 `application.yml` 中添加：

```yaml
# 监控配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
    metrics:
      enabled: true
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ticket-show
      service: stock-service

# 启用定时任务
spring:
  task:
    scheduling:
      enabled: true
```

### 第六步：创建监控测试接口 (15分钟)

创建 `StockMonitorController.java`：

```java
package com.ticketsystem.show.controller;

import com.ticketsystem.show.monitor.StockLevelMonitor;
import com.ticketsystem.show.service.TicketStockService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class StockMonitorController {
    
    private final StockLevelMonitor stockLevelMonitor;
    private final TicketStockService ticketStockService;
    private final MeterRegistry meterRegistry;
    
    /**
     * 手动触发库存检查
     */
    @PostMapping("/stock/check")
    public Map<String, Object> triggerStockCheck() {
        stockLevelMonitor.triggerManualCheck();
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "库存检查已触发");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
    
    /**
     * 获取监控指标概览
     */
    @GetMapping("/metrics/overview")
    public Map<String, Object> getMetricsOverview() {
        Map<String, Object> metrics = new HashMap<>();
        
        // 获取库存操作计数
        meterRegistry.getMeters().stream()
                .filter(meter -> meter.getId().getName().startsWith("stock_"))
                .forEach(meter -> {
                    String name = meter.getId().getName();
                    if (meter instanceof io.micrometer.core.instrument.Counter) {
                        metrics.put(name, ((io.micrometer.core.instrument.Counter) meter).count());
                    }
                });
        
        return metrics;
    }
    
    /**
     * 模拟库存操作（用于测试监控）
     */
    @PostMapping("/test/stock-operation/{ticketId}")
    public Map<String, Object> testStockOperation(@PathVariable Long ticketId, 
                                                  @RequestParam(defaultValue = "1") Integer quantity) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 测试库存锁定
            Boolean lockResult = ticketStockService.lockStock(ticketId, quantity);
            result.put("lockResult", lockResult);
            
            if (lockResult) {
                // 测试库存确认
                Boolean confirmResult = ticketStockService.confirmStock(ticketId, quantity);
                result.put("confirmResult", confirmResult);
            }
            
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}
```

## 🧪 测试验证

### 1. 启动服务后访问监控端点

```bash
# 查看健康状态
curl http://localhost:8081/actuator/health

# 查看所有指标
curl http://localhost:8081/actuator/metrics

# 查看Prometheus格式指标
curl http://localhost:8081/actuator/prometheus

# 查看库存相关指标
curl http://localhost:8081/actuator/metrics/stock_operations_total
```

### 2. 测试监控功能

```bash
# 手动触发库存检查
curl -X POST http://localhost:8081/api/monitor/stock/check

# 查看监控指标概览
curl http://localhost:8081/api/monitor/metrics/overview

# 模拟库存操作
curl -X POST "http://localhost:8081/api/monitor/test/stock-operation/1?quantity=5"
```

### 3. 观察日志输出

启动服务后，你将看到类似的日志：

```
2024-01-15 10:30:00.123 INFO  --- StockLevelMonitor: 📊 库存预警 - 票档ID: 1, 剩余库存: 15/100 (15.0%)
2024-01-15 10:30:30.456 WARN  --- StockLevelMonitor: ⚠️ 库存不足告警 - 票档ID: 2, 剩余库存: 8/100 (8.0%)
2024-01-15 10:31:00.789 ERROR --- StockLevelMonitor: 🚨 库存严重不足告警 - 票档ID: 3, 剩余库存: 3/100 (3.0%)
```

## 📊 监控指标说明

### 核心指标

1. **stock_operations_total** - 库存操作总数
   - 标签：method (lockStock/unlockStock/confirmStock), status (success/error)

2. **stock_operation_duration_seconds** - 库存操作耗时
   - 标签：method, status

3. **stock_alerts_total** - 库存告警总数
   - 标签：type (low_stock/critical_low_stock/stock_exhausted)

4. **stock_ratio** - 平均库存比例

5. **ticket_stock_ratio** - 单个票档库存比例
   - 标签：ticket_id

### 告警类型

- 📊 **库存预警** (< 20%): 提醒关注库存情况
- ⚠️ **库存不足** (< 10%): 需要及时补充库存
- 🚨 **库存严重不足** (< 5%): 紧急处理
- 💥 **库存耗尽** (= 0): 立即停售

## 🎯 预期效果

实施后你将获得：

✅ **实时监控**: 每30秒自动检查库存水位  
✅ **自动告警**: 基于库存比例的多级告警  
✅ **性能监控**: 库存操作的成功率和响应时间  
✅ **指标收集**: 标准的Prometheus指标格式  
✅ **可视化就绪**: 可直接接入Grafana等监控面板  

## 🔄 后续扩展

基于这个基础，你可以轻松扩展：

- 集成钉钉/企业微信告警
- 添加更多业务指标
- 实现告警规则配置
- 集成Grafana仪表板
- 添加告警静默和升级机制