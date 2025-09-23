package com.ticketsystem.show.annotation;

import java.lang.annotation.*;

/**
 * 限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流key
     */
    String key() default "";

    /**
     * 限流时间窗口，单位秒
     */
    int period() default 1;

    /**
     * 限流次数
     */
    int limit() default 10;

    /**
     * 限流提示消息
     */
    String message() default "请求过于频繁，请稍后再试";
}