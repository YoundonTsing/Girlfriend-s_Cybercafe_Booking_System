package com.ticketsystem.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticketsystem.common.annotation.PerformanceMonitor;
import com.ticketsystem.common.result.Result;
import com.ticketsystem.common.exception.BusinessException;
import com.ticketsystem.order.dto.CreateOrderDTO;
import com.ticketsystem.order.service.OrderService;
import com.ticketsystem.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Tag(name = "订单管理", description = "订单相关接口")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    @Operation(summary = "创建订单")
    @PerformanceMonitor(value = "createOrderController", slowQueryThreshold = 3000)
    public Result<String> createOrder(@RequestBody @Valid CreateOrderDTO createOrderDTO, HttpServletRequest request) {
        // 从请求头获取用户ID，确保安全性
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null) {
            throw new BusinessException("用户未登录");
        }
        Long userId = Long.valueOf(userIdHeader);
        
        // 覆盖前端传递的用户ID，使用认证后的用户ID
        createOrderDTO.setUserId(userId);
        
        String orderNo = orderService.createOrder(createOrderDTO);
        return Result.success(orderNo);
    }

    @PostMapping("/pay")
    @Operation(summary = "支付订单")
    @PerformanceMonitor(value = "payOrderController", slowQueryThreshold = 5000)
    public Result<Boolean> payOrder(@RequestParam String orderNo, @RequestParam Integer payType) {
        boolean result = orderService.payOrder(orderNo, payType);
        return Result.success(result);
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消订单")
    public Result<Boolean> cancelOrder(@RequestParam String orderNo, HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null) {
            throw new BusinessException("用户未登录");
        }
        Long userId = Long.valueOf(userIdHeader);
        boolean result = orderService.cancelOrder(orderNo, userId);
        return Result.success(result);
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "获取订单详情（通过订单ID）")
    public Result<OrderVO> getOrderDetail(@PathVariable Long id, HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null) {
            throw new BusinessException("用户未登录");
        }
        Long userId = Long.valueOf(userIdHeader);
        OrderVO orderVO = orderService.getOrderDetailById(id, userId);
        return Result.success(orderVO);
    }

    @GetMapping("/detail/by-order-no/{orderNo}")
    @Operation(summary = "获取订单详情（通过订单号）")
    public Result<OrderVO> getOrderDetailByOrderNo(@PathVariable String orderNo, HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null) {
            throw new BusinessException("用户未登录");
        }
        Long userId = Long.valueOf(userIdHeader);
        OrderVO orderVO = orderService.getOrderDetail(orderNo, userId);
        return Result.success(orderVO);
    }

    @GetMapping("/user/page")
    @Operation(summary = "分页查询用户订单列表")
    @PerformanceMonitor(value = "pageUserOrdersController", slowQueryThreshold = 2000)
    public Result<Page<OrderVO>> pageUserOrders(
            HttpServletRequest request,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null) {
            throw new BusinessException("用户未登录");
        }
        Long userId = Long.valueOf(userIdHeader);
        Page<OrderVO> result = orderService.pageUserOrders(userId, status, page, size);
        return Result.success(result);
    }
}