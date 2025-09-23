package com.ticketsystem.show.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Redis实现的分布式限流器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimiter {

    private final RedissonClient redissonClient;
    
    private static final String RATE_LIMITER_SCRIPT = 
            "local key = KEYS[1] " +
            "local limit = tonumber(ARGV[1]) " +
            "local expire = tonumber(ARGV[2]) " +
            "local current = tonumber(redis.call('get', key) or '0') " +
            "if current + 1 > limit then " +
            "    return 0 " +
            "else " +
            "    redis.call('incrby', key, 1) " +
            "    redis.call('expire', key, expire) " +
            "    return 1 " +
            "end";
    
    /**
     * 尝试获取令牌
     * @param key 限流key
     * @param limit 限流阈值
     * @param expire 过期时间（秒）
     * @return 是否获取成功
     */
    public boolean tryAcquire(String key, int limit, int expire) {
        try {
            List<Object> keys = Collections.singletonList(key);
            Object[] args = {String.valueOf(limit), String.valueOf(expire)};
            
            Long result = redissonClient.getScript()
                    .eval(RScript.Mode.READ_WRITE, RATE_LIMITER_SCRIPT, RScript.ReturnType.INTEGER, keys, args);
            
            return result != null && result == 1L;
        } catch (Exception e) {
            log.error("限流器异常", e);
            // 发生异常时，保守策略，不允许通过
            return false;
        }
    }
    
    /**
     * 尝试获取令牌（默认过期时间1秒）
     * @param key 限流key
     * @param limit 限流阈值
     * @return 是否获取成功
     */
    public boolean tryAcquire(String key, int limit) {
        return tryAcquire(key, limit, 1);
    }
}