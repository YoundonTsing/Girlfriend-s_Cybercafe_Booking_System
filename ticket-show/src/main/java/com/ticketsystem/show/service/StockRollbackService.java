package com.ticketsystem.show.service;

import com.ticketsystem.show.entity.TicketStock;
import com.ticketsystem.show.mapper.TicketStockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存回滚服务
 * 处理订单取消、支付失败等场景的库存恢复
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockRollbackService {

    private final TicketStockService ticketStockService;
    private final RedisStockService redisStockService;
    private final TicketStockMapper ticketStockMapper;
    
    /**
     * 订单取消时的库存回滚
     * @param ticketId 票档ID
     * @param quantity 回滚数量
     * @param orderId 订单ID（用于日志追踪）
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean rollbackForOrderCancel(Long ticketId, Integer quantity, Long orderId) {
        log.info("开始处理订单取消库存回滚，订单ID：{}，票档ID：{}，数量：{}", orderId, ticketId, quantity);
        
        // 参数验证
        if (ticketId == null || ticketId <= 0) {
            log.error("票档ID无效，订单ID：{}，票档ID：{}", orderId, ticketId);
            return false;
        }
        if (quantity == null || quantity <= 0) {
            log.error("回滚数量无效，订单ID：{}，数量：{}", orderId, quantity);
            return false;
        }
        
        Boolean redisResult = false;
        Boolean dbResult = false;
        Boolean syncResult = false;
        
        try {
            // 1. 先回滚Redis库存
            redisResult = ticketStockService.rollbackStockToRedis(ticketId, quantity);
            if (!redisResult) {
                log.warn("Redis库存回滚失败，订单ID：{}，票档ID：{}，数量：{}", orderId, ticketId, quantity);
            }
            
            // 2. 回滚数据库锁定库存（释放锁定状态）- 关键操作
            dbResult = ticketStockService.unlockStock(ticketId, quantity);
            if (!dbResult) {
                log.error("数据库库存回滚失败，订单ID：{}，票档ID：{}，数量：{}", orderId, ticketId, quantity);
                // 如果数据库回滚失败，需要补偿Redis操作
                if (redisResult) {
                    try {
                        // 尝试重新扣减Redis库存以保持一致性
                        redisStockService.predeductStock(ticketId, quantity);
                        log.info("已补偿Redis库存扣减，订单ID：{}，票档ID：{}，数量：{}", orderId, ticketId, quantity);
                    } catch (Exception compensateEx) {
                        log.error("Redis补偿操作失败，订单ID：{}，票档ID：{}，数量：{}", orderId, ticketId, quantity, compensateEx);
                    }
                }
                return false;
            }
            
            // 3. 同步数据库库存到Redis（确保一致性）
            try {
                syncResult = ticketStockService.syncStockToRedis(ticketId);
                if (!syncResult) {
                    log.warn("库存同步到Redis失败，但不影响回滚结果，订单ID：{}，票档ID：{}", orderId, ticketId);
                }
            } catch (Exception syncEx) {
                log.error("库存同步异常，订单ID：{}，票档ID：{}", orderId, ticketId, syncEx);
                // 同步失败不影响整体回滚结果
            }
            
            log.info("订单取消库存回滚成功，订单ID：{}，票档ID：{}，数量：{}，Redis结果：{}，DB结果：{}，同步结果：{}", 
                orderId, ticketId, quantity, redisResult, dbResult, syncResult);
            return true;
            
        } catch (Exception e) {
            log.error("订单取消库存回滚异常，订单ID：{}，票档ID：{}，数量：{}", orderId, ticketId, quantity, e);
            // 记录详细的异常信息供后续分析
            log.error("回滚状态 - Redis：{}，DB：{}，同步：{}", redisResult, dbResult, syncResult);
            return false;
        }
    }
    
    /**
     * 支付失败时的库存回滚
     * @param ticketId 票档ID
     * @param quantity 回滚数量
     * @param orderId 订单ID（用于日志追踪）
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean rollbackForPaymentFailed(Long ticketId, Integer quantity, Long orderId) {
        log.info("开始处理支付失败库存回滚，订单ID：{}，票档ID：{}，数量：{}", orderId, ticketId, quantity);
        
        // 参数验证
        if (ticketId == null || ticketId <= 0) {
            log.error("支付失败回滚-票档ID无效，订单ID：{}，票档ID：{}", orderId, ticketId);
            return false;
        }
        if (quantity == null || quantity <= 0) {
            log.error("支付失败回滚-回滚数量无效，订单ID：{}，数量：{}", orderId, quantity);
            return false;
        }
        
        try {
            // 支付失败的回滚逻辑与订单取消相同，但需要记录不同的业务场景
            Boolean result = rollbackForOrderCancel(ticketId, quantity, orderId);
            if (result) {
                log.info("支付失败库存回滚成功，订单ID：{}，票档ID：{}，数量：{}", orderId, ticketId, quantity);
            } else {
                log.error("支付失败库存回滚失败，订单ID：{}，票档ID：{}，数量：{}", orderId, ticketId, quantity);
            }
            return result;
            
        } catch (Exception e) {
            log.error("支付失败库存回滚异常，订单ID：{}，票档ID：{}，数量：{}", orderId, ticketId, quantity, e);
            return false;
        }
    }
    
    /**
     * 订单超时时的库存回滚
     * @param ticketId 票档ID
     * @param quantity 回滚数量
     * @param orderId 订单ID（用于日志追踪）
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean rollbackForOrderTimeout(Long ticketId, Integer quantity, Long orderId) {
        log.info("开始处理订单超时库存回滚，订单ID：{}，票档ID：{}，数量：{}", orderId, ticketId, quantity);
        
        try {
            // 订单超时的回滚逻辑与订单取消相同
            return rollbackForOrderCancel(ticketId, quantity, orderId);
            
        } catch (Exception e) {
            log.error("订单超时库存回滚异常，订单ID：{}，票档ID：{}，数量：{}", orderId, ticketId, quantity, e);
            return false;
        }
    }
    
    /**
     * 批量库存回滚（用于批量订单处理）
     * @param rollbackItems 回滚项目列表
     * @param orderId 订单ID（用于日志追踪）
     * @return 成功回滚的数量
     */
    @Transactional(rollbackFor = Exception.class)
    public Integer batchRollback(java.util.List<RollbackItem> rollbackItems, Long orderId) {
        log.info("开始批量库存回滚，订单ID：{}，回滚项目数：{}", orderId, rollbackItems.size());
        
        int successCount = 0;
        for (RollbackItem item : rollbackItems) {
            try {
                Boolean result = rollbackForOrderCancel(item.getTicketId(), item.getQuantity(), orderId);
                if (result) {
                    successCount++;
                } else {
                    log.warn("批量回滚中单项失败，订单ID：{}，票档ID：{}，数量：{}", 
                            orderId, item.getTicketId(), item.getQuantity());
                }
            } catch (Exception e) {
                log.error("批量回滚中单项异常，订单ID：{}，票档ID：{}，数量：{}", 
                        orderId, item.getTicketId(), item.getQuantity(), e);
            }
        }
        
        log.info("批量库存回滚完成，订单ID：{}，成功数：{}，总数：{}", orderId, successCount, rollbackItems.size());
        return successCount;
    }
    
    /**
     * 回滚项目数据类
     */
    public static class RollbackItem {
        private Long ticketId;
        private Integer quantity;
        
        public RollbackItem(Long ticketId, Integer quantity) {
            this.ticketId = ticketId;
            this.quantity = quantity;
        }
        
        public Long getTicketId() {
            return ticketId;
        }
        
        public void setTicketId(Long ticketId) {
            this.ticketId = ticketId;
        }
        
        public Integer getQuantity() {
            return quantity;
        }
        
        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}