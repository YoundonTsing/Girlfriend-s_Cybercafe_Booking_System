package com.ticketsystem.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * TraceId过滤器 - 生成并透传traceId
 */
@Component
@Slf4j
public class TraceIdFilter implements GlobalFilter, Ordered {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // 获取或生成traceId
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (!StringUtils.hasText(traceId)) {
            traceId = generateTraceId();
        }
        
        // 将traceId添加到请求头中传递给下游服务
        ServerHttpRequest newRequest = request.mutate()
                .header(TRACE_ID_HEADER, traceId)
                .build();
        
        // 设置MDC用于日志记录
        try {
            org.slf4j.MDC.put("traceId", traceId);
            log.info("[gateway] Request {} {} traceId={}", 
                    request.getMethodValue(), request.getURI().getPath(), traceId);
            
            return chain.filter(exchange.mutate().request(newRequest).build());
        } finally {
            // 清理MDC
            org.slf4j.MDC.remove("traceId");
        }
    }

    @Override
    public int getOrder() {
        return -1; // 比AuthFilter优先级更高
    }

    /**
     * 生成traceId
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}