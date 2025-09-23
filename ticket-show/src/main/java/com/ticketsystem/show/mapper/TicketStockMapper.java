package com.ticketsystem.show.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketsystem.show.entity.TicketStock;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
/**
 * 票档库存Mapper接口
 * 使用乐观锁机制防止高并发下的超卖问题
 */
@Mapper
public interface TicketStockMapper extends BaseMapper<TicketStock> {

    /**
     * 使用乐观锁锁定库存
     * @param ticketId 票档ID
     * @param quantity 锁定数量
     * @param version 当前版本号
     * @return 影响行数（成功返回1，失败返回0）
     */
    @Update("UPDATE t_ticket_stock SET " +
            "locked_stock = locked_stock + #{quantity}, " +
            "version = version + 1, " +
            "update_time = CURRENT_TIMESTAMP " +
            "WHERE ticket_id = #{ticketId} " +
            "AND version = #{version} " +
            "AND (total_stock - locked_stock - sold_stock) >= #{quantity}")
    int lockStockWithOptimisticLock(@Param("ticketId") Long ticketId, 
                                   @Param("quantity") Integer quantity, 
                                   @Param("version") Integer version);

    /**
     * 释放锁定的库存
     * @param ticketId 票档ID
     * @param quantity 释放数量
     * @param version 当前版本号
     * @return 影响行数
     */
    @Update("UPDATE t_ticket_stock SET " +
            "locked_stock = locked_stock - #{quantity}, " +
            "version = version + 1, " +
            "update_time = CURRENT_TIMESTAMP " +
            "WHERE ticket_id = #{ticketId} " +
            "AND version = #{version} " +
            "AND locked_stock >= #{quantity}")
    int unlockStockWithOptimisticLock(@Param("ticketId") Long ticketId, 
                                     @Param("quantity") Integer quantity, 
                                     @Param("version") Integer version);

    /**
     * 从锁定库存转为已售库存（最终确认）
     * @param ticketId 票档ID
     * @param quantity 确认数量
     * @param version 当前版本号
     * @return 影响行数
     */
    @Update("UPDATE t_ticket_stock SET " +
            "locked_stock = locked_stock - #{quantity}, " +
            "sold_stock = sold_stock + #{quantity}, " +
            "version = version + 1, " +
            "update_time = CURRENT_TIMESTAMP " +
            "WHERE ticket_id = #{ticketId} " +
            "AND version = #{version} " +
            "AND locked_stock >= #{quantity}")
    int confirmStockWithOptimisticLock(@Param("ticketId") Long ticketId, 
                                      @Param("quantity") Integer quantity, 
                                      @Param("version") Integer version);

    /**
     * 根据票档ID查询库存信息
     * @param ticketId 票档ID
     * @return 库存信息
     */
    @Select("SELECT * FROM t_ticket_stock WHERE ticket_id = #{ticketId}")
    TicketStock selectByTicketId(@Param("ticketId") Long ticketId);

    /**
     * 获取可用库存数量
     * @param ticketId 票档ID
     * @return 可用库存数量
     */
    @Select("SELECT (total_stock - locked_stock - sold_stock) as available_stock " +
            "FROM t_ticket_stock WHERE ticket_id = #{ticketId}")
    Integer getAvailableStock(@Param("ticketId") Long ticketId);
    
    /**
     * 初始化票档库存记录
     */
    @Insert("INSERT INTO t_ticket_stock (ticket_id, total_stock, locked_stock, sold_stock, version) " +
            "VALUES (#{ticketId}, #{totalStock}, 0, 0, 0) " +
            "ON DUPLICATE KEY UPDATE total_stock = #{totalStock}, locked_stock = 0, sold_stock = 0, version = 0")
    int initializeStock(@Param("ticketId") Long ticketId, @Param("totalStock") Integer totalStock);

    /**
     * 获取剩余库存数量
     */
    @Select("SELECT (total_stock - locked_stock - sold_stock) as remain_stock " +
            "FROM t_ticket_stock WHERE ticket_id = #{ticketId}")
    Integer getRemainStock(@Param("ticketId") Long ticketId);

    /**
     * 更新剩余库存数量
     */
    @Update("UPDATE t_ticket_stock SET " +
            "locked_stock = total_stock - #{remainStock} - sold_stock, " +
            "version = version + 1, " +
            "update_time = CURRENT_TIMESTAMP " +
            "WHERE ticket_id = #{ticketId}")
    int updateRemainStock(@Param("ticketId") Long ticketId, @Param("remainStock") Integer remainStock);

    /**
     * 获取所有票档ID
     */
    @Select("SELECT DISTINCT ticket_id FROM t_ticket_stock")
    List<Long> getAllTicketIds();
}