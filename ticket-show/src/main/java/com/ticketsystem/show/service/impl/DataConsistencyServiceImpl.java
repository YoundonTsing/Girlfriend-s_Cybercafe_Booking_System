package com.ticketsystem.show.service.impl;

import com.ticketsystem.show.entity.TicketStock;
import com.ticketsystem.show.mapper.TicketStockMapper;
import com.ticketsystem.show.service.DataConsistencyService;
import com.ticketsystem.show.service.RedisStockService;
import com.ticketsystem.show.service.TicketStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据一致性服务实现
 * 用于检查和修复Redis与数据库之间的数据一致性问题
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataConsistencyServiceImpl implements DataConsistencyService {

    private final TicketStockMapper ticketStockMapper;
    private final RedisStockService redisStockService;
    private final TicketStockService ticketStockService;

    @Override
    public Boolean checkStockConsistency(Long ticketId) {
        try {
            log.debug("开始检查库存一致性，票档ID：{}", ticketId);
            
            // 获取数据库库存信息
            TicketStock dbStock = ticketStockMapper.selectByTicketId(ticketId);
            if (dbStock == null) {
                log.warn("数据库中没有找到票档库存信息，票档ID：{}", ticketId);
                return false;
            }
            
            // 计算数据库可用库存
            Integer dbAvailableStock = dbStock.getTotalStock() - dbStock.getLockedStock() - dbStock.getSoldStock();
            
            // 获取Redis库存信息
            Integer redisStock = redisStockService.getStock(ticketId);
            
            // 比较库存是否一致
            boolean isConsistent = dbAvailableStock.equals(redisStock);
            
            if (!isConsistent) {
                log.warn("库存不一致，票档ID：{}，数据库可用库存：{}，Redis库存：{}", 
                    ticketId, dbAvailableStock, redisStock);
            } else {
                log.debug("库存一致，票档ID：{}，可用库存：{}", ticketId, dbAvailableStock);
            }
            
            return isConsistent;
        } catch (Exception e) {
            log.error("检查库存一致性异常，票档ID：{}", ticketId, e);
            return false;
        }
    }

    @Override
    public Boolean repairStockConsistency(Long ticketId) {
        try {
            log.info("开始修复库存一致性，票档ID：{}", ticketId);
            
            // 获取数据库库存信息
            TicketStock dbStock = ticketStockMapper.selectByTicketId(ticketId);
            if (dbStock == null) {
                log.warn("数据库中没有找到票档库存信息，无法修复，票档ID：{}", ticketId);
                return false;
            }
            
            // 计算数据库可用库存
            Integer dbAvailableStock = dbStock.getTotalStock() - dbStock.getLockedStock() - dbStock.getSoldStock();
            
            // 以数据库为准，同步到Redis
            Boolean syncResult = redisStockService.initStock(ticketId, dbAvailableStock, true);
            
            if (syncResult) {
                log.info("库存一致性修复成功，票档ID：{}，同步库存：{}", ticketId, dbAvailableStock);
            } else {
                log.error("库存一致性修复失败，票档ID：{}，同步库存：{}", ticketId, dbAvailableStock);
            }
            
            return syncResult;
        } catch (Exception e) {
            log.error("修复库存一致性异常，票档ID：{}", ticketId, e);
            return false;
        }
    }

    @Override
    public Integer checkAllStockConsistency() {
        try {
            log.info("开始批量检查所有票档库存一致性");
            
            // 获取所有票档库存信息
            List<TicketStock> allStocks = ticketStockMapper.selectList(null);
            int inconsistentCount = 0;
            
            for (TicketStock stock : allStocks) {
                if (!checkStockConsistency(stock.getTicketId())) {
                    inconsistentCount++;
                }
            }
            
            log.info("批量检查完成，总票档数：{}，不一致票档数：{}", allStocks.size(), inconsistentCount);
            return inconsistentCount;
        } catch (Exception e) {
            log.error("批量检查库存一致性异常", e);
            return -1; // 返回-1表示检查失败
        }
    }

    @Override
    public Integer repairAllStockConsistency() {
        try {
            log.info("开始批量修复所有票档库存一致性");
            
            // 获取所有票档库存信息
            List<TicketStock> allStocks = ticketStockMapper.selectList(null);
            int repairedCount = 0;
            
            for (TicketStock stock : allStocks) {
                if (!checkStockConsistency(stock.getTicketId())) {
                    if (repairStockConsistency(stock.getTicketId())) {
                        repairedCount++;
                    }
                }
            }
            
            log.info("批量修复完成，总票档数：{}，修复成功数：{}", allStocks.size(), repairedCount);
            return repairedCount;
        } catch (Exception e) {
            log.error("批量修复库存一致性异常", e);
            return -1; // 返回-1表示修复失败
        }
    }
}