package com.ticketsystem.show.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    // RedisTemplate配置已移除，统一使用RedissonClient
    
    @Bean
    public RedissonClient redissonClient(
            @Value("${spring.redis.host:localhost}") String host,
            @Value("${spring.redis.port:6379}") int port,
            @Value("${spring.redis.database:0}") int database,
            @Value("${spring.redis.password:}") String password,
            @Value("${spring.redis.timeout:5000}") long timeout) {
        Config config = new Config();
        // 设置序列化编解码器 - 修复KryoException问题
        config.setCodec(new JsonJacksonCodec());
        
        // 单节点模式
        String address = "redis://" + host + ":" + port;
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(database)
                .setTimeout((int) timeout)               // 响应超时：5秒，避免长时间阻塞
                .setConnectTimeout(3000)                 // 连接超时：3秒，快速失败机制
                .setIdleConnectionTimeout(10000)        // 空闲连接超时：10秒，及时释放资源
                // 连接池配置 - 解决高并发连接瓶颈
                .setConnectionPoolSize(32)              // 连接池大小：适中配置，平衡性能与资源消耗
                .setConnectionMinimumIdleSize(8)        // 最小空闲连接：保持基础连接，避免冷启动
                // 重试机制配置 - 提升系统容错能力
                .setRetryAttempts(3)                    // 重试次数：覆盖95%临时网络问题
                .setRetryInterval(1500);                // 重试间隔：1.5秒，平衡恢复时间与用户体验
        
        // 如果配置了密码则设置密码
        if (password != null && !password.trim().isEmpty()) {
            config.useSingleServer().setPassword(password);
        }
        
        return Redisson.create(config);
    }
}