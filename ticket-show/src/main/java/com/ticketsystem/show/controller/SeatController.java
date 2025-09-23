package com.ticketsystem.show.controller;

import com.ticketsystem.common.result.Result;
import com.ticketsystem.show.dto.SeatLockRequest;
import com.ticketsystem.show.service.SeatService;
import com.ticketsystem.show.vo.SeatAreaVO;
import com.ticketsystem.show.vo.SeatLayoutVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 座位管理控制器
 */
@RestController
@RequestMapping("/api/seat")
@RequiredArgsConstructor
@Tag(name = "座位管理", description = "座位选择相关接口")
@Slf4j
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/areas")
    @Operation(summary = "根据机位类型获取可选座位区域")
    public Result<List<SeatAreaVO>> getSeatAreas(
            @RequestParam Integer showType,
            @RequestParam Long showId) {
        
        log.info("获取座位区域列表，机位类型: {}, 演出ID: {}", showType, showId);
        List<SeatAreaVO> areas = seatService.getAvailableAreas(showType, showId);
        return Result.success(areas);
    }

    @GetMapping("/layout/{areaId}")
    @Operation(summary = "获取座位区域的布局信息")
    public Result<SeatLayoutVO> getSeatLayout(
            @PathVariable Long areaId,
            @RequestParam Long showId,
            @RequestParam Long sessionId,
            HttpServletRequest request) {
        
        // 获取当前用户ID
        Long currentUserId = getCurrentUserId(request);
        
        log.info("获取座位布局，区域ID: {}, 演出ID: {}, 场次ID: {}, 用户ID: {}", 
                 areaId, showId, sessionId, currentUserId);
        
        SeatLayoutVO layout = seatService.getSeatLayout(areaId, showId, sessionId, currentUserId);
        return Result.success(layout);
    }

    @PostMapping("/lock")
    @Operation(summary = "锁定选中的座位")
    public Result<Boolean> lockSeats(
            @RequestBody @Valid SeatLockRequest request,
            HttpServletRequest httpRequest) {
        
        Long userId = getCurrentUserId(httpRequest);
        
        log.info("锁定座位，用户ID: {}, 座位IDs: {}", userId, request.getSeatIds());
        
        boolean success = seatService.lockSeats(request.getSeatIds(), userId);
        
        if (success) {
            return Result.success("座位锁定成功", true);
        } else {
            return Result.fail("座位锁定失败，可能已被其他用户选择");
        }
    }

    /**
     * 单个座位锁定接口（供订单服务调用）
     */
    @PostMapping("/lock/single")
    @Operation(summary = "锁定单个座位")
    public Result<Boolean> lockSeat(
            @RequestParam("seatId") Long seatId,
            @RequestParam("userId") Long userId,
            @RequestParam("sessionId") Long sessionId) {
        
        log.info("锁定单个座位，用户ID: {}, 座位ID: {}, 场次ID: {}", userId, seatId, sessionId);
        
        boolean success = seatService.lockSeats(List.of(seatId), userId);
        
        if (success) {
            return Result.success("座位锁定成功", true);
        } else {
            return Result.fail("座位锁定失败，可能已被其他用户选择");
        }
    }

    @PostMapping("/release")
    @Operation(summary = "释放座位锁定")
    public Result<Boolean> releaseSeats(
            @RequestBody @Valid SeatLockRequest request,
            HttpServletRequest httpRequest) {
        
        Long userId = getCurrentUserId(httpRequest);
        
        log.info("释放座位锁定，用户ID: {}, 座位IDs: {}", userId, request.getSeatIds());
        
        boolean success = seatService.releaseSeats(request.getSeatIds(), userId);
        return Result.success(success);
    }

    /**
     * 单个座位释放接口（供订单服务调用）
     */
    @PostMapping("/release/single")
    @Operation(summary = "释放单个座位锁定")
    public Result<Boolean> releaseSeat(
            @RequestParam("seatId") Long seatId,
            @RequestParam("userId") Long userId,
            @RequestParam("sessionId") Long sessionId) {
        
        log.info("释放单个座位锁定，用户ID: {}, 座位ID: {}, 场次ID: {}", userId, seatId, sessionId);
        
        boolean success = seatService.releaseSeats(List.of(seatId), userId);
        return Result.success(success);
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        String authHeader = request.getHeader("Authorization");
        
        log.info("获取用户ID - X-User-Id: {}, Authorization: {}", userIdHeader, authHeader);
        
        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            log.error("用户ID头部为空，所有请求头: {}", java.util.Collections.list(request.getHeaderNames()));
            throw new RuntimeException("用户未登录");
        }
        return Long.valueOf(userIdHeader);
    }
}