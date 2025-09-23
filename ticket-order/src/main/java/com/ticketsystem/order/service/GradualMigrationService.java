package com.ticketsystem.order.service;

import com.ticketsystem.order.dto.CreateOrderDTO;
import com.ticketsystem.order.service.impl.OrderServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 渐进式迁移服务
 * 解决多方案冲突，实现平滑迁移
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GradualMigrationService {

    private final UnifiedOrderService redisBasedOrderService;
    private final UnifiedOrderService unifiedOrderService;
    private final OrderServiceImpl orderServiceImpl;
    
    // 迁移配置
    @Value("${app.migration.redis-percentage:0}")
    private int redisPercentage;
    
    @Value("${app.migration.unified-percentage:0}")
    private int unifiedPercentage;
    
    @Value("${app.migration.database-percentage:100}")
    private int databasePercentage;
    
    @Value("${app.migration.enable-gradual:false}")
    private boolean enableGradual;

    /**
     * 渐进式创建订单 - 根据配置选择不同的实现
     */
    public String createOrder(CreateOrderDTO dto) {
        if (!enableGradual) {
            // 如果未启用渐进式迁移，使用统一服务
            return unifiedOrderService.createOrder(dto);
        }
        
        // 根据配置比例选择实现
        int random = ThreadLocalRandom.current().nextInt(100);
        
        if (random < redisPercentage) {
            // 使用Redis实现
            log.info("使用Redis实现创建订单: userId={}, random={}", dto.getUserId(), random);
            return redisBasedOrderService.createOrder(dto);
        } else if (random < redisPercentage + unifiedPercentage) {
            // 使用统一实现
            log.info("使用统一实现创建订单: userId={}, random={}", dto.getUserId(), random);
            return unifiedOrderService.createOrder(dto);
        } else {
            // 使用数据库实现
            log.info("使用数据库实现创建订单: userId={}, random={}", dto.getUserId(), random);
            return orderServiceImpl.createOrder(dto);
        }
    }

    /**
     * 渐进式支付订单
     */
    public boolean payOrder(String orderNo, Long userId, Integer payType) {
        if (!enableGradual) {
            return unifiedOrderService.payOrder(orderNo, userId, payType);
        }
        
        // 根据订单号判断使用哪种实现
        if (orderNo.startsWith("REDIS_")) {
            return redisBasedOrderService.payOrder(orderNo, userId, payType);
        } else if (orderNo.startsWith("UNIFIED_")) {
            return unifiedOrderService.payOrder(orderNo, userId, payType);
        } else {
            return orderServiceImpl.payOrder(orderNo, payType);
        }
    }

    /**
     * 渐进式取消订单
     */
    public boolean cancelOrder(String orderNo, Long userId, Long ticketId, Integer quantity) {
        if (!enableGradual) {
            return unifiedOrderService.cancelOrder(orderNo, userId, ticketId, quantity);
        }
        
        // 根据订单号判断使用哪种实现
        if (orderNo.startsWith("REDIS_")) {
            return redisBasedOrderService.cancelOrder(orderNo, userId, ticketId, quantity);
        } else if (orderNo.startsWith("UNIFIED_")) {
            return unifiedOrderService.cancelOrder(orderNo, userId, ticketId, quantity);
        } else {
            return orderServiceImpl.cancelOrder(orderNo, userId);
        }
    }

    /**
     * 更新迁移配置
     */
    public void updateMigrationConfig(int redisPercentage, int unifiedPercentage, int databasePercentage) {
        if (redisPercentage + unifiedPercentage + databasePercentage != 100) {
            throw new IllegalArgumentException("迁移配置百分比总和必须为100");
        }
        
        this.redisPercentage = redisPercentage;
        this.unifiedPercentage = unifiedPercentage;
        this.databasePercentage = databasePercentage;
        
        log.info("更新迁移配置: Redis={}%, Unified={}%, Database={}%", 
                redisPercentage, unifiedPercentage, databasePercentage);
    }

    /**
     * 启用/禁用渐进式迁移
     */
    public void setGradualMigrationEnabled(boolean enabled) {
        this.enableGradual = enabled;
        log.info("渐进式迁移状态: {}", enabled ? "启用" : "禁用");
    }

    /**
     * 获取当前迁移状态
     */
    public String getMigrationStatus() {
        return String.format("迁移状态 - Redis: %d%%, Unified: %d%%, Database: %d%%, 启用: %s",
                redisPercentage, unifiedPercentage, databasePercentage, enableGradual);
    }
}