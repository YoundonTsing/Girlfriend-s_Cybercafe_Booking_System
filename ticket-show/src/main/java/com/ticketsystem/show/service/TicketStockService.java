package com.ticketsystem.show.service;

import com.ticketsystem.show.entity.TicketStock;

/**
 * 票档库存服务接口
 * 提供基于乐观锁的高并发库存管理
 */
public interface TicketStockService {

    /**
     * 锁定库存（使用乐观锁机制）
     * @param ticketId 票档ID
     * @param quantity 锁定数量
     * @return 是否成功
     */
    Boolean lockStock(Long ticketId, Integer quantity);

    /**
     * 释放锁定的库存
     * @param ticketId 票档ID
     * @param quantity 释放数量
     * @return 是否成功
     */
    Boolean unlockStock(Long ticketId, Integer quantity);

    /**
     * 确认库存（从锁定转为已售）
     * @param ticketId 票档ID
     * @param quantity 确认数量
     * @return 是否成功
     */
    Boolean confirmStock(Long ticketId, Integer quantity);

    /**
     * 获取库存信息
     * @param ticketId 票档ID
     * @return 库存信息
     */
    TicketStock getStockInfo(Long ticketId);

    /**
     * 获取可用库存
     * @param ticketId 票档ID
     * @return 可用库存数量
     */
    Integer getAvailableStock(Long ticketId);
    
    /**
     * 初始化票档库存记录
     * @param ticketId 票档ID
     * @param totalStock 总库存
     * @return 初始化结果
     */
    Boolean initializeStock(Long ticketId, Integer totalStock);
    
    /**
     * Redis预减库存（原子操作）
     * @param ticketId 票档ID
     * @param quantity 扣减数量
     * @return 预减结果：1-成功，0-库存不足，-1-库存不存在
     */
    Integer predeductStockFromRedis(Long ticketId, Integer quantity);
    
    /**
     * 回滚Redis库存
     * @param ticketId 票档ID
     * @param quantity 回滚数量
     * @return 是否成功
     */
    Boolean rollbackStockToRedis(Long ticketId, Integer quantity);
    
    /**
     * 同步数据库库存到Redis
     * @param ticketId 票档ID
     * @return 是否成功
     */
    Boolean syncStockToRedis(Long ticketId);
    
    /**
     * 数据库库存确认（乐观锁）
     * @param ticketId 票档ID
     * @param quantity 确认数量
     * @return 是否成功
     */
    Boolean confirmStockFromDatabase(Long ticketId, Integer quantity);
}