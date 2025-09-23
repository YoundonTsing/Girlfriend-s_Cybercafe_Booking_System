package com.ticketsystem.show.service;

/**
 * 数据一致性服务接口
 * 用于检查和修复Redis与数据库之间的数据一致性问题
 */
public interface DataConsistencyService {

    /**
     * 检查Redis与数据库库存一致性
     * @param ticketId 票档ID
     * @return 是否一致
     */
    Boolean checkStockConsistency(Long ticketId);

    /**
     * 修复Redis与数据库库存不一致问题
     * @param ticketId 票档ID
     * @return 修复结果
     */
    Boolean repairStockConsistency(Long ticketId);

    /**
     * 批量检查所有票档的库存一致性
     * @return 不一致的票档数量
     */
    Integer checkAllStockConsistency();

    /**
     * 批量修复所有票档的库存一致性问题
     * @return 修复成功的票档数量
     */
    Integer repairAllStockConsistency();
}