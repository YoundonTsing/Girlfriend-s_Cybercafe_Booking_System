package com.ticketsystem.show.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 库存监控配置类
 * 定义各种监控指标的Bean
 */
@Configuration
public class StockMonitoringConfig {
    
    /**
     * 库存操作计数器
     * 统计各种库存操作的总次数
     */
    @Bean
    public Counter stockOperationCounter(MeterRegistry meterRegistry) {
        return Counter.builder("stock_operations_total")
                .description("Total number of stock operations")
                .register(meterRegistry);
    }
    
    /**
     * 库存操作耗时计时器
     * 统计库存操作的响应时间分布
     */
    @Bean
    public Timer stockOperationTimer(MeterRegistry meterRegistry) {
        return Timer.builder("stock_operation_duration_seconds")
                .description("Duration of stock operations")
                .register(meterRegistry);
    }
    
    /**
     * 库存告警计数器
     * 统计各种库存告警的总数
     */
    @Bean
    public Counter stockAlertCounter(MeterRegistry meterRegistry) {
        return Counter.builder("stock_alerts_total")
                .description("Total number of stock alerts")
                .register(meterRegistry);
    }
    
    /**
     * 库存锁定成功率计数器
     * 统计库存锁定操作的成功次数
     */
    @Bean
    public Counter stockLockSuccessCounter(MeterRegistry meterRegistry) {
        return Counter.builder("stock_lock_success_total")
                .description("Total number of successful stock lock operations")
                .register(meterRegistry);
    }
    
    /**
     * 库存锁定失败率计数器
     * 统计库存锁定操作的失败次数
     */
    @Bean
    public Counter stockLockFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("stock_lock_failure_total")
                .description("Total number of failed stock lock operations")
                .register(meterRegistry);
    }
    
    /**
     * 库存确认成功率计数器
     * 统计库存确认操作的成功次数
     */
    @Bean
    public Counter stockConfirmSuccessCounter(MeterRegistry meterRegistry) {
        return Counter.builder("stock_confirm_success_total")
                .description("Total number of successful stock confirm operations")
                .register(meterRegistry);
    }
    
    /**
     * 库存确认失败率计数器
     * 统计库存确认操作的失败次数
     */
    @Bean
    public Counter stockConfirmFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("stock_confirm_failure_total")
                .description("Total number of failed stock confirm operations")
                .register(meterRegistry);
    }
    
    /**
     * 库存回滚成功率计数器
     * 统计库存回滚操作的成功次数
     */
    @Bean
    public Counter stockRollbackSuccessCounter(MeterRegistry meterRegistry) {
        return Counter.builder("stock_rollback_success_total")
                .description("Total number of successful stock rollback operations")
                .register(meterRegistry);
    }
    
    /**
     * 库存回滚失败率计数器
     * 统计库存回滚操作的失败次数
     */
    @Bean
    public Counter stockRollbackFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("stock_rollback_failure_total")
                .description("Total number of failed stock rollback operations")
                .register(meterRegistry);
    }
    
    /**
     * Redis连接异常计数器
     * 统计Redis连接异常的次数
     */
    @Bean
    public Counter redisConnectionErrorCounter(MeterRegistry meterRegistry) {
        return Counter.builder("redis_connection_errors_total")
                .description("Total number of Redis connection errors")
                .register(meterRegistry);
    }
    
    /**
     * Lua脚本执行异常计数器
     * 统计Lua脚本执行失败的次数
     */
    @Bean
    public Counter luaScriptErrorCounter(MeterRegistry meterRegistry) {
        return Counter.builder("lua_script_errors_total")
                .description("Total number of Lua script execution errors")
                .register(meterRegistry);
    }
    
    /**
     * 数据库操作异常计数器
     * 统计数据库操作异常的次数
     */
    @Bean
    public Counter databaseErrorCounter(MeterRegistry meterRegistry) {
        return Counter.builder("database_errors_total")
                .description("Total number of database operation errors")
                .register(meterRegistry);
    }
}