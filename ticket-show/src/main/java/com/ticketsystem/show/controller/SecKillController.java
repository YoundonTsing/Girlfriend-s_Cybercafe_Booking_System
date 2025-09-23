package com.ticketsystem.show.controller;

import com.ticketsystem.common.result.Result;
import com.ticketsystem.show.annotation.RateLimit;
import com.ticketsystem.show.component.RateLimiter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 秒杀抢票专用控制器
 */
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
@Tag(name = "秒杀抢票", description = "秒杀抢票专用接口")
@Slf4j
public class SecKillController {

    private final RateLimiter rateLimiter;

    // TODO: 秒杀相关功能应该在ticket-order服务中实现
    // 这里暂时保留Controller结构，但移除具体实现
}