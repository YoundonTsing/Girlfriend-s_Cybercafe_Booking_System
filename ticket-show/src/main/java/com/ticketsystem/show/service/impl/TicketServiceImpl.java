package com.ticketsystem.show.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ticketsystem.common.exception.BusinessException;
import com.ticketsystem.show.entity.Ticket;
import com.ticketsystem.show.entity.TicketStock;
import com.ticketsystem.show.mapper.TicketMapper;
import com.ticketsystem.show.service.TicketService;
import com.ticketsystem.show.service.TicketStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 票档服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketServiceImpl extends ServiceImpl<TicketMapper, Ticket> implements TicketService {

    private final TicketMapper ticketMapper;
    private final TicketStockService ticketStockService;

    @Override
    public BigDecimal getTicketPrice(Long ticketId) {
        Ticket ticket = getById(ticketId);
        if (ticket == null) {
            throw new BusinessException("票档不存在");
        }
        return ticket.getPrice();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean lockTicketStock(Long ticketId, Integer quantity) {
        if (quantity <= 0) {
            throw new BusinessException("锁定数量必须大于0");
        }
        
        // 检查票档是否存在
        Ticket ticket = getById(ticketId);
        if (ticket == null) {
            throw new BusinessException("票档不存在");
        }
        
        // 使用新的乐观锁库存服务
        Boolean lockResult = ticketStockService.lockStock(ticketId, quantity);
        if (lockResult) {
            // 同时更新票档表的剩余数量（保持数据一致性）
            int result = ticketMapper.lockStock(ticketId, quantity);
            if (result > 0) {
                log.info("成功锁定票档库存，票档ID：{}，锁定数量：{}", ticketId, quantity);
                return true;
            } else {
                // 如果票档表更新失败，需要回滚库存锁定
                ticketStockService.unlockStock(ticketId, quantity);
                log.warn("票档表更新失败，已回滚库存锁定，票档ID：{}，锁定数量：{}", ticketId, quantity);
                return false;
            }
        } else {
            log.warn("锁定票档库存失败，票档ID：{}，锁定数量：{}", ticketId, quantity);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean unlockTicketStock(Long ticketId, Integer quantity) {
        if (quantity <= 0) {
            throw new BusinessException("释放数量必须大于0");
        }
        
        // 检查票档是否存在
        Ticket ticket = getById(ticketId);
        if (ticket == null) {
            throw new BusinessException("票档不存在");
        }
        
        // 使用新的乐观锁库存服务释放库存
        Boolean unlockResult = ticketStockService.unlockStock(ticketId, quantity);
        if (unlockResult) {
            // 同时更新票档表的剩余数量（保持数据一致性）
            int result = ticketMapper.unlockStock(ticketId, quantity);
            if (result > 0) {
                log.info("成功释放票档库存，票档ID：{}，释放数量：{}", ticketId, quantity);
                return true;
            } else {
                log.warn("票档表更新失败，但库存已释放，票档ID：{}，释放数量：{}", ticketId, quantity);
                return true; // 库存服务已成功，不影响整体结果
            }
        } else {
            log.warn("释放票档库存失败，票档ID：{}，释放数量：{}", ticketId, quantity);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deductTicketStock(Long ticketId, Integer quantity) {
        if (quantity <= 0) {
            throw new BusinessException("扣减数量必须大于0");
        }
        
        // 检查票档是否存在
        Ticket ticket = getById(ticketId);
        if (ticket == null) {
            throw new BusinessException("票档不存在");
        }
        
        // 使用新的乐观锁库存服务确认库存（从锁定转为已售）
        Boolean confirmResult = ticketStockService.confirmStock(ticketId, quantity);
        if (confirmResult) {
            // 更新票档表的时间戳（保持数据一致性）
            int result = ticketMapper.deductStock(ticketId, quantity);
            if (result > 0) {
                log.info("成功扣减票档库存，票档ID：{}，扣减数量：{}", ticketId, quantity);
                return true;
            } else {
                log.warn("票档表更新失败，但库存已确认，票档ID：{}，扣减数量：{}", ticketId, quantity);
                return true; // 库存服务已成功，不影响整体结果
            }
        } else {
            log.warn("扣减票档库存失败，票档ID：{}，扣减数量：{}", ticketId, quantity);
            return false;
        }
    }

    @Override
    public List<Ticket> getTicketsByShowAndSession(Long showId, Long sessionId) {
        LambdaQueryWrapper<Ticket> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Ticket::getShowId, showId)
                .eq(Ticket::getSessionId, sessionId)
                .eq(Ticket::getStatus, 1) // 只查询售票中的票档
                .orderByAsc(Ticket::getPrice);
        return list(queryWrapper);
    }

    @Override
    public Object getTicketStockInfo(Long ticketId) {
        // 获取票档基本信息
        Ticket ticket = getById(ticketId);
        if (ticket == null) {
            throw new BusinessException("票档不存在");
        }
        
        // 获取库存信息
        Object stockInfo = ticketStockService.getStockInfo(ticketId);
        
        // 组合返回信息
        Map<String, Object> result = new HashMap<>();
        result.put("ticket", ticket);
        result.put("stock", stockInfo);
        
        return result;
    }

    @Override
    public List<Object> getAllTicketStockInfo() {
        // 获取所有票档
        List<Ticket> allTickets = list();
        
        List<Object> result = new ArrayList<>();
        for (Ticket ticket : allTickets) {
            try {
                TicketStock stockInfo = ticketStockService.getStockInfo(ticket.getId());
                if (stockInfo != null) {
                    // 计算并设置可用库存
                    stockInfo.setAvailableStock(stockInfo.getTotalStock() - stockInfo.getLockedStock() - stockInfo.getSoldStock());
                }
                Map<String, Object> ticketStockInfo = new HashMap<>();
                ticketStockInfo.put("ticket", ticket);
                ticketStockInfo.put("stock", stockInfo);
                result.add(ticketStockInfo);
            } catch (Exception e) {
                log.warn("获取票档{}库存信息失败: {}", ticket.getId(), e.getMessage());
                // 如果库存信息获取失败，仍然返回票档信息，但库存为null
                Map<String, Object> ticketStockInfo = new HashMap<>();
                ticketStockInfo.put("ticket", ticket);
                ticketStockInfo.put("stock", null);
                result.add(ticketStockInfo);
            }
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean initializeTicketStock(Long ticketId, Integer totalStock) {
        // 添加调试日志
        log.info("=== 初始化库存调试信息 ===");
        log.info("接收到的票档ID: {}", ticketId);
        log.info("接收到的总库存: {}", totalStock);
        log.info("总库存参数类型: {}", totalStock != null ? totalStock.getClass().getSimpleName() : "null");
        log.info("=========================");
        
        if (totalStock < 0) {
            throw new BusinessException("总库存数量不能为负数");
        }
        
        // 检查票档是否存在
        Ticket ticket = getById(ticketId);
        if (ticket == null) {
            throw new BusinessException("票档不存在");
        }
        
        try {
            // 调用库存服务初始化库存
            Boolean result = ticketStockService.initializeStock(ticketId, totalStock);
            if (result) {
                log.info("成功初始化票档库存，票档ID：{}，总库存：{}", ticketId, totalStock);
            } else {
                log.warn("初始化票档库存失败，票档ID：{}，总库存：{}", ticketId, totalStock);
            }
            return result;
        } catch (Exception e) {
            log.error("初始化票档{}库存异常: {}", ticketId, e.getMessage(), e);
            throw new BusinessException("初始化库存失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTicket(Ticket ticket) {
        // 初始化票档数据
        if (ticket.getTotalCount() == null) {
            ticket.setTotalCount(0);
        }
        ticket.setRemainCount(ticket.getTotalCount());
        if (ticket.getLimitCount() == null) {
            ticket.setLimitCount(4); // 默认限购4张
        }
        
        if (ticket.getStatus() == null) {
            ticket.setStatus(1); // 默认售票中
        }
        
        // 保存票档信息
        save(ticket);
        
        // 初始化库存记录
        if (ticket.getTotalCount() > 0) {
            Boolean stockInitResult = ticketStockService.initializeStock(ticket.getId(), ticket.getTotalCount());
            if (!stockInitResult) {
                log.warn("初始化票档库存失败，票档ID：{}，总库存：{}", ticket.getId(), ticket.getTotalCount());
                // 注意：这里不抛异常，因为票档已创建，库存可以后续补充
            } else {
                log.info("成功初始化票档库存，票档ID：{}，总库存：{}", ticket.getId(), ticket.getTotalCount());
            }
        }
        
        return ticket.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTicket(Ticket ticket) {
        Ticket existTicket = getById(ticket.getId());
        if (existTicket == null) {
            throw new BusinessException("票档不存在");
        }
        
        updateById(ticket);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTicket(Long id) {
        Ticket ticket = getById(id);
        if (ticket == null) {
            throw new BusinessException("票档不存在");
        }
        
        // 检查是否有已售出的票（通过总票数和剩余票数计算）
        int soldCount = ticket.getTotalCount() - ticket.getRemainCount();
        if (soldCount > 0) {
            throw new BusinessException("票档已有售出记录，无法删除");
        }
        
        removeById(id);
    }
}