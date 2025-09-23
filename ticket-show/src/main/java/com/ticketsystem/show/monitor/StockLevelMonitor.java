package com.ticketsystem.show.monitor;

import com.ticketsystem.show.service.RedisStockService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 库存水位监控定时任务
 * 每30秒检查库存水位并触发告警
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StockLevelMonitor {
    
    private final RedissonClient redissonClient;
    private final Counter stockAlertCounter;
    private final MeterRegistry meterRegistry;
    
    // 库存水位缓存，用于Gauge指标
    private final Map<String, AtomicInteger> stockLevelCache = new ConcurrentHashMap<>();
    
    // 告警阈值配置
    private static final double CRITICAL_THRESHOLD = 0.05; // 5%严重告警
    private static final double WARNING_THRESHOLD = 0.10;  // 10%告警
    private static final double NOTICE_THRESHOLD = 0.20;   // 20%预警
    
    @PostConstruct
    public void init() {
        // 注册库存水位Gauge指标
        Gauge.builder("stock_level_percentage", this, monitor -> {
                    return stockLevelCache.values().stream()
                            .mapToInt(AtomicInteger::get)
                            .average()
                            .orElse(0.0);
                })
                .description("当前库存水位百分比")
                .tag("type", "percentage")
                .register(meterRegistry);
        
        log.info("📊 库存水位监控器已启动，检查间隔: 30秒");
    }
    
    /**
     * 每30秒执行一次库存水位检查
     */
    @Scheduled(fixedRate = 30000) // 30秒
    public void checkStockLevels() {
        try {
            log.debug("🔍 开始库存水位检查...");
            
            // 获取所有库存key
            List<String> stockKeys = getStockKeys();
            
            if (stockKeys.isEmpty()) {
                log.debug("📦 未发现库存数据");
                return;
            }
            
            int totalChecked = 0;
            int alertTriggered = 0;
            
            for (String stockKey : stockKeys) {
                try {
                    StockLevelInfo levelInfo = checkSingleStockLevel(stockKey);
                    if (levelInfo != null) {
                        totalChecked++;
                        
                        // 更新缓存用于Gauge指标
                        String ticketId = extractTicketIdFromKey(stockKey);
                        stockLevelCache.put(ticketId, new AtomicInteger((int)(levelInfo.percentage * 100)));
                        
                        // 检查是否需要告警
                        if (triggerAlertIfNeeded(levelInfo)) {
                            alertTriggered++;
                        }
                    }
                } catch (Exception e) {
                    log.error("❌ 检查库存水位异常 - Key: {}, 异常: {}", stockKey, e.getMessage());
                }
            }
            
            log.debug("✅ 库存水位检查完成 - 检查数量: {}, 触发告警: {}", totalChecked, alertTriggered);
            
        } catch (Exception e) {
            log.error("❌ 库存水位监控异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取所有库存key
     */
    private List<String> getStockKeys() {
        try {
            // 使用Redis KEYS命令获取所有库存key（生产环境建议使用SCAN）
            Iterable<String> keys = redissonClient.getKeys().getKeysByPattern("stock:*");
            // 修复类型转换异常：正确处理Redisson返回的Iterable类型
            List<String> keyList = new ArrayList<>();
            for (String key : keys) {
                keyList.add(key);
            }
            return keyList;
        } catch (Exception e) {
            log.error("❌ 获取库存keys异常: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * 检查单个库存水位
     */
    private StockLevelInfo checkSingleStockLevel(String stockKey) {
        try {
            // 获取当前库存
            Object stockObj = redissonClient.getBucket(stockKey).get();
            Integer currentStock = null;
            if (stockObj != null) {
                if (stockObj instanceof Integer) {
                    currentStock = (Integer) stockObj;
                } else if (stockObj instanceof String) {
                    try {
                        currentStock = Integer.parseInt((String) stockObj);
                    } catch (NumberFormatException e) {
                        log.warn("⚠️ 库存值格式错误 - Key: {}, Value: {}", stockKey, stockObj);
                        return null;
                    }
                } else {
                    log.warn("⚠️ 库存值类型不支持 - Key: {}, Type: {}", stockKey, stockObj.getClass().getSimpleName());
                    return null;
                }
            }
            if (currentStock == null) {
                return null;
            }
            
            // 获取初始库存（从数据库或配置中获取，这里简化处理）
            Integer initialStock = getInitialStock(stockKey);
            if (initialStock == null || initialStock <= 0) {
                return null;
            }
            
            double percentage = (double) currentStock / initialStock;
            String ticketId = extractTicketIdFromKey(stockKey);
            
            return new StockLevelInfo(ticketId, stockKey, currentStock, initialStock, percentage);
            
        } catch (Exception e) {
            log.error("❌ 检查库存水位异常 - Key: {}, 异常: {}", stockKey, e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取初始库存数量（简化实现，实际应从数据库获取）
     */
    private Integer getInitialStock(String stockKey) {
        try {
            // 尝试从Redis获取初始库存配置
            String initialStockKey = stockKey + ":initial";
            Object initialStockObj = redissonClient.getBucket(initialStockKey).get();
            Integer initialStock = null;
            if (initialStockObj != null) {
                if (initialStockObj instanceof Integer) {
                    initialStock = (Integer) initialStockObj;
                } else if (initialStockObj instanceof String) {
                    try {
                        initialStock = Integer.parseInt((String) initialStockObj);
                    } catch (NumberFormatException e) {
                        log.warn("⚠️ 初始库存值格式错误 - Key: {}, Value: {}", initialStockKey, initialStockObj);
                    }
                }
            }
            
            // 如果没有配置，使用默认值1000（实际应从数据库查询）
            return initialStock != null ? initialStock : 1000;
        } catch (Exception e) {
            log.warn("⚠️ 获取初始库存失败，使用默认值 - Key: {}", stockKey);
            return 1000;
        }
    }
    
    /**
     * 从库存key中提取票档ID
     */
    private String extractTicketIdFromKey(String stockKey) {
        // 假设key格式为 "stock:票档ID"
        if (stockKey.startsWith("stock:")) {
            return stockKey.substring(6);
        }
        return stockKey;
    }
    
    /**
     * 根据库存水位触发告警
     */
    private boolean triggerAlertIfNeeded(StockLevelInfo levelInfo) {
        String severity = null;
        String alertType = null;
        
        // 库存耗尽检测
        if (levelInfo.currentStock == 0) {
            severity = "critical";
            alertType = "stock_exhausted";
            log.error("🚨 库存耗尽告警 - 票档ID: {}, 当前库存: 0", levelInfo.ticketId);
        }
        // 严重告警 < 5%
        else if (levelInfo.percentage < CRITICAL_THRESHOLD) {
            severity = "critical";
            alertType = "stock_critical";
            log.error("🔴 库存严重告警 - 票档ID: {}, 当前库存: {}, 水位: {:.1f}%", 
                    levelInfo.ticketId, levelInfo.currentStock, levelInfo.percentage * 100);
        }
        // 告警 < 10%
        else if (levelInfo.percentage < WARNING_THRESHOLD) {
            severity = "warning";
            alertType = "stock_warning";
            log.warn("🟠 库存告警 - 票档ID: {}, 当前库存: {}, 水位: {:.1f}%", 
                    levelInfo.ticketId, levelInfo.currentStock, levelInfo.percentage * 100);
        }
        // 预警 < 20%
        else if (levelInfo.percentage < NOTICE_THRESHOLD) {
            severity = "notice";
            alertType = "stock_notice";
            log.info("🟡 库存预警 - 票档ID: {}, 当前库存: {}, 水位: {:.1f}%", 
                    levelInfo.ticketId, levelInfo.currentStock, levelInfo.percentage * 100);
        }
        
        // 触发告警指标
        if (severity != null && alertType != null) {
            Counter.builder("stock_alert_counter")
                    .tag("type", alertType)
                    .tag("ticket_id", levelInfo.ticketId)
                    .tag("severity", severity)
                    .tag("stock_level", String.format("%.1f", levelInfo.percentage * 100))
                    .register(meterRegistry)
                    .increment();
            return true;
        }
        
        return false;
    }
    
    /**
     * 手动触发库存水位检查（供API调用）
     */
    public void manualCheck() {
        log.info("🔍 手动触发库存水位检查");
        checkStockLevels();
    }
    
    /**
     * 获取当前库存水位统计
     */
    public Map<String, Object> getStockLevelStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        try {
            List<String> stockKeys = getStockKeys();
            
            int totalStocks = 0;
            int criticalStocks = 0;
            int warningStocks = 0;
            int noticeStocks = 0;
            int exhaustedStocks = 0;
            
            for (String stockKey : stockKeys) {
                StockLevelInfo levelInfo = checkSingleStockLevel(stockKey);
                if (levelInfo != null) {
                    totalStocks++;
                    
                    if (levelInfo.currentStock == 0) {
                        exhaustedStocks++;
                    } else if (levelInfo.percentage < CRITICAL_THRESHOLD) {
                        criticalStocks++;
                    } else if (levelInfo.percentage < WARNING_THRESHOLD) {
                        warningStocks++;
                    } else if (levelInfo.percentage < NOTICE_THRESHOLD) {
                        noticeStocks++;
                    }
                }
            }
            
            stats.put("total_stocks", totalStocks);
            stats.put("exhausted_stocks", exhaustedStocks);
            stats.put("critical_stocks", criticalStocks);
            stats.put("warning_stocks", warningStocks);
            stats.put("notice_stocks", noticeStocks);
            stats.put("healthy_stocks", totalStocks - exhaustedStocks - criticalStocks - warningStocks - noticeStocks);
            stats.put("check_time", System.currentTimeMillis());
            
        } catch (Exception e) {
            log.error("❌ 获取库存水位统计异常: {}", e.getMessage());
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * 库存水位信息
     */
    private static class StockLevelInfo {
        final String ticketId;
        final String stockKey;
        final Integer currentStock;
        final Integer initialStock;
        final double percentage;
        
        StockLevelInfo(String ticketId, String stockKey, Integer currentStock, Integer initialStock, double percentage) {
            this.ticketId = ticketId;
            this.stockKey = stockKey;
            this.currentStock = currentStock;
            this.initialStock = initialStock;
            this.percentage = percentage;
        }
    }
}