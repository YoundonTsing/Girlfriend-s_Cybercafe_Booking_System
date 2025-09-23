package com.ticketsystem.show.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketsystem.show.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 票档信息Mapper接口
 */
@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {

    /**
     * 锁定票档库存（使用乐观锁防止超卖）
     * @param ticketId 票档ID
     * @param quantity 锁定数量
     * @return 影响行数
     */
    @Update("UPDATE t_ticket SET remain_count = remain_count - #{quantity}, " +
            "update_time = CURRENT_TIMESTAMP " +
            "WHERE id = #{ticketId} AND remain_count >= #{quantity}")
    int lockStock(@Param("ticketId") Long ticketId, @Param("quantity") Integer quantity);

    /**
     * 释放票档库存
     * @param ticketId 票档ID
     * @param quantity 释放数量
     * @return 影响行数
     */
    @Update("UPDATE t_ticket SET remain_count = remain_count + #{quantity} " +
            "WHERE id = #{ticketId}")
    int unlockStock(@Param("ticketId") Long ticketId, @Param("quantity") Integer quantity);

    /**
     * 扣减票档库存（最终确认扣减，不再检查库存）
     * @param ticketId 票档ID
     * @param quantity 扣减数量
     * @return 影响行数
     */
    @Update("UPDATE t_ticket SET update_time = CURRENT_TIMESTAMP " +
            "WHERE id = #{ticketId}")
    int deductStock(@Param("ticketId") Long ticketId, @Param("quantity") Integer quantity);
}