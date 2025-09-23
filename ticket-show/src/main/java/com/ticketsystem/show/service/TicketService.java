package com.ticketsystem.show.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ticketsystem.show.entity.Ticket;

import java.math.BigDecimal;
import java.util.List;

/**
 * 票档服务接口
 */
public interface TicketService extends IService<Ticket> {

    /**
     * 获取票档价格
     * @param ticketId 票档ID
     * @return 票档价格
     */
    BigDecimal getTicketPrice(Long ticketId);

    /**
     * 锁定票档库存
     * @param ticketId 票档ID
     * @param quantity 锁定数量
     * @return 是否成功
     */
    Boolean lockTicketStock(Long ticketId, Integer quantity);

    /**
     * 释放票档库存
     * @param ticketId 票档ID
     * @param quantity 释放数量
     * @return 是否成功
     */
    Boolean unlockTicketStock(Long ticketId, Integer quantity);

    /**
     * 扣减票档库存
     * @param ticketId 票档ID
     * @param quantity 扣减数量
     * @return 是否成功
     */
    Boolean deductTicketStock(Long ticketId, Integer quantity);

    /**
     * 根据演出ID和场次ID获取票档列表
     * @param showId 演出ID
     * @param sessionId 场次ID
     * @return 票档列表
     */
    List<Ticket> getTicketsByShowAndSession(Long showId, Long sessionId);

    /**
     * 获取票档库存信息
     * @param ticketId 票档ID
     * @return 库存信息
     */
    Object getTicketStockInfo(Long ticketId);

    /**
     * 获取所有票档库存信息
     * @return 所有库存信息列表
     */
    List<Object> getAllTicketStockInfo();

    /**
     * 初始化票档库存
     * @param ticketId 票档ID
     * @param totalStock 总库存数量
     * @return 是否成功
     */
    Boolean initializeTicketStock(Long ticketId, Integer totalStock);

    /**
     * 创建票档
     * @param ticket 票档信息
     * @return 票档ID
     */
    Long createTicket(Ticket ticket);

    /**
     * 更新票档信息
     * @param ticket 票档信息
     */
    void updateTicket(Ticket ticket);

    /**
     * 删除票档
     * @param id 票档ID
     */
    void deleteTicket(Long id);
}