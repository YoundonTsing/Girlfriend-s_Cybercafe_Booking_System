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