package com.ticketsystem.show.aspect;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 库存监控切面
 * 监控TicketStockServiceImpl的所有方法调用，记录性能指标和异常情况
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class StockMonitoringAspect {
    
    private final Counter stockOperationCounter;
    private final Timer stockOperationTimer;
    private final Counter stockAlertCounter;
    private final Counter stockLockSuccessCounter;
    private final Counter stockLockFailureCounter;
    private final Counter stockConfirmSuccessCounter;
    private final Counter stockConfirmFailureCounter;
    private final Counter stockRollbackSuccessCounter;
    private final Counter stockRollbackFailureCounter;
    private final Counter redisConnectionErrorCounter;
    private final Counter luaScriptErrorCounter;
    private final Counter databaseErrorCounter;
    private final MeterRegistry meterRegistry;
    
    /**
     * 监控TicketStockServiceImpl的所有方法调用
     */
    @Around("execution(* com.ticketsystem.show.service.impl.TicketStockServiceImpl.*(..))")
    public Object monitorStockOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            Object result = joinPoint.proceed();
            
            // 记录成功操作
            sample.stop(Timer.builder("stock.operation.duration")
                    .tag("method", methodName)
                    .tag("status", "success")
                    .register(meterRegistry));
            Counter.builder("stock.operation.count")
                    .tag("method", methodName)
                    .tag("status", "success")
                    .register(meterRegistry)
                    .increment();
            
            // 检查操作结果并记录成功率统计
            recordOperationResult(methodName, args, result, true);
            
            // 检查操作结果并触发告警
            checkOperationResult(methodName, args, result);
            
            return result;
            
        } catch (Exception e) {
            // 记录失败操作
            sample.stop(Timer.builder("stock.operation.duration")
                    .tag("method", methodName)
                    .tag("status", "error")
                    .register(meterRegistry));
            Counter.builder("stock.operation.count")
                    .tag("method", methodName)
                    .tag("status", "error")
                    .tag("error_type", e.getClass().getSimpleName())
                    .register(meterRegistry)
                    .increment();
            
            // 记录失败率统计
            recordOperationResult(methodName, args, null, false);
            
            // 触发错误告警
            triggerErrorAlert(methodName, args, e);
            
            throw e;
        }
    }
    
    /**
     * 记录操作结果统计
     */
    private void recordOperationResult(String methodName, Object[] args, Object result, boolean isSuccess) {
        Long ticketId = args.length > 0 ? (Long) args[0] : null;
        String ticketIdTag = ticketId != null ? ticketId.toString() : "unknown";
        
        switch (methodName) {
            case "lockStock":
                if (isSuccess && Boolean.TRUE.equals(result)) {
                    Counter.builder("stock.lock.success")
                            .tag("ticket_id", ticketIdTag)
                            .register(meterRegistry)
                            .increment();
                    log.debug("✅ 库存锁定成功 - 票档ID: {}", ticketId);
                } else {
                    Counter.builder("stock.lock.failure")
                            .tag("ticket_id", ticketIdTag)
                            .register(meterRegistry)
                            .increment();
                    if (isSuccess) {
                        log.warn("🔒 库存锁定失败 - 票档ID: {}, 库存不足", ticketId);
                    }
                }
                break;
                
            case "confirmStock":
                if (isSuccess && Boolean.TRUE.equals(result)) {
                    Counter.builder("stock.confirm.success")
                            .tag("ticket_id", ticketIdTag)
                            .register(meterRegistry)
                            .increment();
                    log.debug("✅ 库存确认成功 - 票档ID: {}", ticketId);
                } else {
                    Counter.builder("stock.confirm.failure")
                            .tag("ticket_id", ticketIdTag)
                            .register(meterRegistry)
                            .increment();
                    if (isSuccess) {
                        log.warn("❌ 库存确认失败 - 票档ID: {}", ticketId);
                    }
                }
                break;
                
            case "unlockStock":
            case "rollbackStockToRedis":
                if (isSuccess && Boolean.TRUE.equals(result)) {
                    Counter.builder("stock.rollback.success")
                            .tag("ticket_id", ticketIdTag)
                            .register(meterRegistry)
                            .increment();
                    log.debug("🔄 库存回滚成功 - 票档ID: {}", ticketId);
                } else {
                    Counter.builder("stock.rollback.failure")
                            .tag("ticket_id", ticketIdTag)
                            .register(meterRegistry)
                            .increment();
                    if (isSuccess) {
                        log.warn("⚠️ 库存回滚失败 - 票档ID: {}", ticketId);
                    }
                }
                break;
        }
    }
    
    /**
     * 检查操作结果并触发告警
     */
    private void checkOperationResult(String methodName, Object[] args, Object result) {
        // 检查库存锁定失败
        if ("lockStock".equals(methodName) && Boolean.FALSE.equals(result)) {
            Long ticketId = (Long) args[0];
            Integer quantity = (Integer) args[1];
            
            log.warn("🔒 库存锁定失败告警 - 票档ID: {}, 请求数量: {}", ticketId, quantity);
            Counter.builder("stock.alert")
                    .tag("type", "lock_failed")
                    .tag("ticket_id", ticketId.toString())
                    .tag("severity", "warning")
                    .register(meterRegistry)
                    .increment();
        }
        
        // 检查库存确认失败
        if ("confirmStock".equals(methodName) && Boolean.FALSE.equals(result)) {
            Long ticketId = (Long) args[0];
            Integer quantity = (Integer) args[1];
            
            log.warn("✅ 库存确认失败告警 - 票档ID: {}, 确认数量: {}", ticketId, quantity);
            Counter.builder("stock.alert")
                    .tag("type", "confirm_failed")
                    .tag("ticket_id", ticketId.toString())
                    .tag("severity", "warning")
                    .register(meterRegistry)
                    .increment();
        }
        
        // 检查库存回滚失败
        if (("unlockStock".equals(methodName) || "rollbackStockToRedis".equals(methodName)) 
                && Boolean.FALSE.equals(result)) {
            Long ticketId = (Long) args[0];
            
            log.error("🔄 库存回滚失败告警 - 票档ID: {}, 方法: {}", ticketId, methodName);
            Counter.builder("stock.alert")
                    .tag("type", "rollback_failed")
                    .tag("ticket_id", ticketId.toString())
                    .tag("severity", "error")
                    .register(meterRegistry)
                    .increment();
        }
    }
    
    /**
     * 触发错误告警
     */
    private void triggerErrorAlert(String methodName, Object[] args, Exception e) {
        Long ticketId = args.length > 0 ? (Long) args[0] : null;
        String ticketIdTag = ticketId != null ? ticketId.toString() : "unknown";
        
        log.error("❌ 库存操作异常告警 - 方法: {}, 票档ID: {}, 异常: {}", 
                methodName, ticketId, e.getMessage());
        
        // 根据异常类型分类统计
        String errorType = e.getClass().getSimpleName();
        String severity = "error";
        
        if (isRedisConnectionError(e)) {
            Counter.builder("redis.connection.error")
                    .tag("ticket_id", ticketIdTag)
                    .register(meterRegistry)
                    .increment();
            Counter.builder("stock.alert")
                    .tag("type", "redis_connection_error")
                    .tag("ticket_id", ticketIdTag)
                    .tag("severity", "critical")
                    .register(meterRegistry)
                    .increment();
            severity = "critical";
            log.error("🚨 Redis连接异常 - 票档ID: {}, 异常: {}", ticketId, e.getMessage());
        } else if (isLuaScriptError(e)) {
            Counter.builder("lua.script.error")
                    .tag("ticket_id", ticketIdTag)
                    .register(meterRegistry)
                    .increment();
            Counter.builder("stock.alert")
                    .tag("type", "lua_script_error")
                    .tag("ticket_id", ticketIdTag)
                    .tag("severity", "error")
                    .register(meterRegistry)
                    .increment();
            log.error("📜 Lua脚本执行异常 - 票档ID: {}, 异常: {}", ticketId, e.getMessage());
        } else if (isDatabaseError(e)) {
            Counter.builder("database.error")
                    .tag("ticket_id", ticketIdTag)
                    .register(meterRegistry)
                    .increment();
            Counter.builder("stock.alert")
                    .tag("type", "database_error")
                    .tag("ticket_id", ticketIdTag)
                    .tag("severity", "error")
                    .register(meterRegistry)
                    .increment();
            log.error("🗄️ 数据库操作异常 - 票档ID: {}, 异常: {}", ticketId, e.getMessage());
        } else {
            Counter.builder("stock.alert")
                    .tag("type", "operation_error")
                    .tag("method", methodName)
                    .tag("error_type", errorType)
                    .tag("ticket_id", ticketIdTag)
                    .tag("severity", severity)
                    .register(meterRegistry)
                    .increment();
        }
    }
    
    /**
     * 判断是否为Redis连接异常
     */
    private boolean isRedisConnectionError(Exception e) {
        String message = e.getMessage();
        String className = e.getClass().getSimpleName();
        
        return className.contains("Redis") || 
               className.contains("Connection") ||
               (message != null && (message.contains("connection") || 
                                   message.contains("redis") || 
                                   message.contains("timeout")));
    }
    
    /**
     * 判断是否为Lua脚本执行异常
     */
    private boolean isLuaScriptError(Exception e) {
        String message = e.getMessage();
        String className = e.getClass().getSimpleName();
        
        return className.contains("Script") || 
               className.contains("Lua") ||
               (message != null && (message.contains("script") || 
                                   message.contains("lua") || 
                                   message.contains("eval")));
    }
    
    /**
     * 判断是否为数据库操作异常
     */
    private boolean isDatabaseError(Exception e) {
        String message = e.getMessage();
        String className = e.getClass().getSimpleName();
        
        return className.contains("SQL") || 
               className.contains("Database") ||
               className.contains("DataAccess") ||
               className.contains("MyBatis") ||
               (message != null && (message.contains("sql") || 
                                   message.contains("database") || 
                                   message.contains("table")));
    }
}