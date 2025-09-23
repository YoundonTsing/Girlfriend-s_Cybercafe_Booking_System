package com.ticketsystem.show.service;

import com.ticketsystem.show.mapper.TicketStockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 库存同步初始化器
 * 系统启动时自动同步数据库库存到Redis
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StockSyncInitializer implements ApplicationRunner {

    private final RedissonClient redissonClient;
    private final TicketStockMapper ticketStockMapper;
    private final RedisStockService redisStockService;
    
    private static final String STOCK_KEY_PREFIX = "stock:ticket:";
    private static final int DEFAULT_EXPIRE_TIME = 86400; // 24小时

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始初始化库存数据到Redis...");
        
        try {
            // 检查Redis连接
            if (!isRedisConnected()) {
                log.error("Redis连接失败，跳过库存初始化");
                return;
            }
            
            // 同步所有票档库存
            syncAllStockToRedis();
            
            log.info("库存数据初始化完成");
        } catch (Exception e) {
            log.error("库存数据初始化失败", e);
        }
    }
    
    /**
     * 检查Redis连接状态
     */
    private boolean isRedisConnected() {
        try {
            redissonClient.getKeys().count();
            return true;
        } catch (Exception e) {
            log.error("Redis连接检查失败", e);
            return false;
        }
    }
    
    /**
     * 同步所有票档库存到Redis
     */
    private void syncAllStockToRedis() {
        try {
            // 获取所有有效票档ID
            List<Long> ticketIds = ticketStockMapper.getAllTicketIds();
            
            if (ticketIds == null || ticketIds.isEmpty()) {
                log.warn("没有找到有效的票档数据");
                return;
            }
            
            int successCount = 0;
            int errorCount = 0;
            
            for (Long ticketId : ticketIds) {
                try {
                    // 获取数据库中的剩余库存
                    Integer remainStock = ticketStockMapper.getRemainStock(ticketId);
                    
                    if (remainStock != null && remainStock >= 0) {
                        // 使用RedisStockService的初始化方法
                        Boolean result = redisStockService.initStock(ticketId, remainStock, true);
                        
                        if (result) {
                            successCount++;
                            log.debug("票档库存同步成功 - ID: {}, 库存: {}", ticketId, remainStock);
                        } else {
                            errorCount++;
                            log.warn("票档库存同步失败 - ID: {}, 库存: {}", ticketId, remainStock);
                        }
                    } else {
                        log.warn("票档库存数据无效 - ID: {}, 库存: {}", ticketId, remainStock);
                        errorCount++;
                    }
                } catch (Exception e) {
                    errorCount++;
                    log.error("同步票档库存异常 - ID: {}", ticketId, e);
                }
            }
            
            log.info("库存同步完成 - 总数: {}, 成功: {}, 失败: {}", 
                    ticketIds.size(), successCount, errorCount);
                    
        } catch (Exception e) {
            log.error("获取票档列表失败", e);
        }
    }
    
    /**
     * 验证库存同步结果
     */
    private void verifyStockSync(List<Long> ticketIds) {
        log.info("开始验证库存同步结果...");
        
        int verifyCount = 0;
        for (Long ticketId : ticketIds) {
            try {
                String stockKey = STOCK_KEY_PREFIX + ticketId;
                Object redisStock = redissonClient.getBucket(stockKey).get();
                
                if (redisStock != null) {
                    verifyCount++;
                    log.debug("库存验证成功 - 票档ID: {}, Redis库存: {}", ticketId, redisStock);
                } else {
                    log.warn("库存验证失败 - 票档ID: {}, Redis中不存在", ticketId);
                }
            } catch (Exception e) {
                log.error("库存验证异常 - 票档ID: {}", ticketId, e);
            }
        }
        
        log.info("库存验证完成 - 验证成功: {}/{}", verifyCount, ticketIds.size());
    }
}