package com.ticketsystem.gateway.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 全局异常处理器
 */
@Component
@Order(-1)
@Slf4j
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        
        if (response.isCommitted()) {
            return Mono.error(ex);
        }
        
        // 设置响应头
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        // 根据异常类型设置状态码
        if (ex instanceof NotFoundException) {
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        } else if (ex instanceof ResponseStatusException) {
            response.setStatusCode(((ResponseStatusException) ex).getStatus());
        } else {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        // 构建响应内容
        String message = buildMessage(ex);
        String body = String.format("{\"code\":%d,\"message\":\"%s\",\"data\":null}",
                response.getStatusCode().value(), message);
        
        // 记录日志
        log.error("网关异常: {}", message, ex);
        
        // 返回响应
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
    
    /**
     * 构建错误消息
     */
    private String buildMessage(Throwable ex) {
        if (ex instanceof NotFoundException) {
            return "服务不可用";
        } else if (ex instanceof ResponseStatusException) {
            return ((ResponseStatusException) ex).getReason();
        } else {
            return "系统内部错误";
        }
    }
}