package com.ticketsystem.order.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

/**
 * Feign配置
 */
@Configuration
public class FeignConfig {

    /**
     * Feign请求拦截器，用于传递请求头信息
     * @return RequestInterceptor
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                Enumeration<String> headerNames = request.getHeaderNames();
                if (headerNames != null) {
                    while (headerNames.hasMoreElements()) {
                        String name = headerNames.nextElement();
                        // 传递用户ID和认证信息
                        if ("user-id".equalsIgnoreCase(name) || "authorization".equalsIgnoreCase(name)) {
                            String value = request.getHeader(name);
                            requestTemplate.header(name, value);
                        }
                    }
                }
            }
        };
    }
}