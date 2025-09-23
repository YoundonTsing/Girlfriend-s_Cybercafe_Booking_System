package com.ticketsystem.show.controller;

import com.ticketsystem.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 */
@RestController
@RequestMapping("/api/show")
@Tag(name = "健康检查", description = "服务健康检查接口")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "服务健康检查")
    public Result<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "ticket-show");
        health.put("timestamp", System.currentTimeMillis());
        return Result.success(health);
    }
}