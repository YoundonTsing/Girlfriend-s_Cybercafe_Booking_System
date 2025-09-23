package com.ticketsystem.show.aspect;

import com.ticketsystem.common.exception.BusinessException;
import com.ticketsystem.show.annotation.RateLimit;
import com.ticketsystem.show.component.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * 限流切面
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitAspect {

    private final RateLimiter rateLimiter;

    @Around("@annotation(com.ticketsystem.show.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        
        // 获取注解
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return point.proceed();
        }
        
        // 获取限流key
        String key = rateLimit.key();
        if (key.isEmpty()) {
            key = method.getDeclaringClass().getName() + ":" + method.getName();
        }
        
        // 获取IP地址
        String ip = getIpAddress(request);
        key = key + ":" + ip;
        
        // 尝试获取令牌
        boolean acquired = rateLimiter.tryAcquire(key, rateLimit.limit(), rateLimit.period());
        if (!acquired) {
            log.warn("接口访问频率超限: {}, IP: {}", key, ip);
            throw new BusinessException(rateLimit.message());
        }
        
        return point.proceed();
    }
    
    /**
     * 获取IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}