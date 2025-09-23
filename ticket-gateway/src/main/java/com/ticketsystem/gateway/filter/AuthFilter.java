package com.ticketsystem.gateway.filter;

import com.ticketsystem.gateway.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 认证过滤器
 */
@Component
@Slf4j
public class AuthFilter implements GlobalFilter, Ordered {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    // 白名单路径
    private final List<String> whiteList = Arrays.asList(
            "/api/user/login",
            "/api/user/register",
            "/api/show/list",
            "/api/show/hot",
            "/api/show/recommend",
            "/api/show/detail/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethodValue();
        String traceId = request.getHeaders().getFirst("X-Trace-Id");
        // 如果没有traceId，生成一个新的
        if (!StringUtils.hasText(traceId)) {
            traceId = generateTraceId();
        }
        
        // 白名单放行，但仍需要传递traceId
        if (isWhitePath(path)) {
            ServerHttpRequest newRequest = request.mutate()
                    .header("X-Trace-Id", traceId)
                    .header("X-Request-Source", "gateway")
                    .build();
            log.info("[gateway] White path {} {} traceId={}", method, path, traceId);
            return chain.filter(exchange.mutate().request(newRequest).build());
        }
        
        // 获取token
        String token = request.getHeaders().getFirst("Authorization");
        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange);
        }
        
        // 验证token
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // 验证token
            Long userId = JwtUtil.getUserId(token);
            String username = JwtUtil.getUsername(token);
            if (userId == null) {
                return unauthorized(exchange);
            }
            
            // 将用户信息、traceId和其他上下文信息传递给下游服务
            ServerHttpRequest newRequest = request.mutate()
                    .header("X-User-Id", String.valueOf(userId))
                    .header("X-Username", username != null ? username : "")
                    .header("X-Trace-Id", traceId)
                    .header("X-Request-Source", "gateway")
                    .header("X-Request-Time", String.valueOf(System.currentTimeMillis()))
                    .build();
            
            log.info("[gateway] Auth success {} {} userId={} traceId={}", method, path, userId, traceId);
            
            return chain.filter(exchange.mutate().request(newRequest).build());
        } catch (Exception e) {
            log.error("Token验证失败 traceId={}", traceId, e);
            return unauthorized(exchange);
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
    
    /**
     * 判断是否为白名单路径
     */
    private boolean isWhitePath(String path) {
        return whiteList.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
    
    /**
     * 生成traceId
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    /**
     * 返回未授权响应
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        
        String body = "{\"code\":401,\"message\":\"未授权\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        
        return response.writeWith(Mono.just(buffer));
    }
}