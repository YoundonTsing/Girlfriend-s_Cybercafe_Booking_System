package com.ticketsystem.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * 日志配置类
 * 提供统一的日志配置和请求日志记录
 */
@Configuration
public class LoggingConfig {

    private static final Logger logger = LoggerFactory.getLogger(LoggingConfig.class);

    /**
     * 请求日志过滤器
     * 记录所有 HTTP 请求的详细信息
     */
    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(10000);
        filter.setIncludeHeaders(true);
        filter.setAfterMessagePrefix("REQUEST DATA: ");
        return filter;
    }

    /**
     * 初始化日志配置
     */
    @Bean
    public void initLogging() {
        logger.info("日志系统初始化完成");
        logger.info("日志级别: {}", System.getProperty("logging.level.root", "INFO"));
        logger.info("日志文件路径: {}", System.getProperty("logging.file.path", "logs"));
    }
} 