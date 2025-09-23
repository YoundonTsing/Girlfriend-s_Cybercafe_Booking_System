package com.ticketsystem.order.config;

import com.ticketsystem.order.util.SnowflakeIdWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
public class IdGeneratorConfig {

    private static final Logger log = LoggerFactory.getLogger(IdGeneratorConfig.class);

    @Value("${snowflake.datacenter-id:1}")
    private long datacenterId;

    @Bean
    public SnowflakeIdWorker snowflakeIdWorker() {
        long workerId = getWorkerId();
        log.info("Initializing SnowflakeIdWorker with datacenterId: {}, workerId: {}", datacenterId, workerId);
        return new SnowflakeIdWorker(workerId, datacenterId);
    }

    /**
     * 动态获取工作节点ID。在容器化环境中，可以从环境变量中获取Pod名称或IP地址来生成唯一ID。
     * 这里使用主机名的哈希值作为简化的示例。
     * @return workerId
     */
    private long getWorkerId() {
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            // 从主机名中提取数字部分，例如 pod-0 -> 0
            String[] parts = hostName.split("-");
            if (parts.length > 1) {
                try {
                    long id = Long.parseLong(parts[parts.length - 1]);
                    // 确保ID在合法范围内 (0-31)
                    if (id >= 0 && id < 32) {
                        return id;
                    }
                } catch (NumberFormatException e) {
                    // 忽略，继续使用哈希方法
                }
            }
            // Fallback: 如果无法从主机名解析，则使用主机名的哈希值
            // 确保哈希值在 0-31 的范围内
            return (long) (Math.abs(hostName.hashCode()) % 32);
        } catch (UnknownHostException e) {
            log.warn("Unable to get hostname, using random worker id.", e);
            // 极端情况下的回退策略，返回一个随机ID
            return (long) (Math.random() * 31);
        }
    }
}
