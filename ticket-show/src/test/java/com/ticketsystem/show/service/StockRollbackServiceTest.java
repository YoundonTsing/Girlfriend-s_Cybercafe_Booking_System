package com.ticketsystem.show.service;

import com.ticketsystem.show.service.StockRollbackService;
import com.ticketsystem.show.service.TicketStockService;
import com.ticketsystem.show.service.RedisStockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 库存回滚服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class StockRollbackServiceTest {

    @Mock
    private TicketStockService ticketStockService;
    
    @Mock
    private RedisStockService redisStockService;
    
    @InjectMocks
    private StockRollbackService stockRollbackService;
    
    private static final Long TICKET_ID = 1L;
    private static final Integer QUANTITY = 2;
    private static final Long ORDER_ID = 100L;
    
    @Test
    void testRollbackForOrderCancel_Success() {
        // 模拟Redis回滚成功
        when(ticketStockService.rollbackStockToRedis(TICKET_ID, QUANTITY)).thenReturn(true);
        // 模拟数据库库存释放成功
        when(ticketStockService.unlockStock(TICKET_ID, QUANTITY)).thenReturn(true);
        // 模拟库存同步成功
        when(ticketStockService.syncStockToRedis(TICKET_ID)).thenReturn(true);
        
        Boolean result = stockRollbackService.rollbackForOrderCancel(TICKET_ID, QUANTITY, ORDER_ID);
        
        assertTrue(result);
        verify(ticketStockService).rollbackStockToRedis(TICKET_ID, QUANTITY);
        verify(ticketStockService).unlockStock(TICKET_ID, QUANTITY);
        verify(ticketStockService).syncStockToRedis(TICKET_ID);
    }
    
    @Test
    void testRollbackForOrderCancel_RedisRollbackFailed() {
        // 模拟Redis回滚失败
        when(ticketStockService.rollbackStockToRedis(TICKET_ID, QUANTITY)).thenReturn(false);
        // 模拟数据库库存释放成功
        when(ticketStockService.unlockStock(TICKET_ID, QUANTITY)).thenReturn(true);
        // 模拟库存同步成功
        when(ticketStockService.syncStockToRedis(TICKET_ID)).thenReturn(true);
        
        Boolean result = stockRollbackService.rollbackForOrderCancel(TICKET_ID, QUANTITY, ORDER_ID);
        
        assertTrue(result); // Redis失败不影响整体结果，只要数据库操作成功
        verify(ticketStockService).rollbackStockToRedis(TICKET_ID, QUANTITY);
        verify(ticketStockService).unlockStock(TICKET_ID, QUANTITY);
        verify(ticketStockService).syncStockToRedis(TICKET_ID);
    }
    
    @Test
    void testRollbackForOrderCancel_DatabaseRollbackFailed() {
        // 模拟Redis回滚成功
        when(ticketStockService.rollbackStockToRedis(TICKET_ID, QUANTITY)).thenReturn(true);
        // 模拟数据库库存释放失败
        when(ticketStockService.unlockStock(TICKET_ID, QUANTITY)).thenReturn(false);
        
        Boolean result = stockRollbackService.rollbackForOrderCancel(TICKET_ID, QUANTITY, ORDER_ID);
        
        assertFalse(result);
        verify(ticketStockService).rollbackStockToRedis(TICKET_ID, QUANTITY);
        verify(ticketStockService).unlockStock(TICKET_ID, QUANTITY);
        verify(ticketStockService, never()).syncStockToRedis(TICKET_ID);
    }
    
    @Test
    void testRollbackForOrderCancel_WithException() {
        // 模拟异常
        when(ticketStockService.rollbackStockToRedis(TICKET_ID, QUANTITY))
                .thenThrow(new RuntimeException("Redis连接异常"));
        
        Boolean result = stockRollbackService.rollbackForOrderCancel(TICKET_ID, QUANTITY, ORDER_ID);
        
        assertFalse(result);
        verify(ticketStockService).rollbackStockToRedis(TICKET_ID, QUANTITY);
        verify(ticketStockService, never()).unlockStock(any(), any());
        verify(ticketStockService, never()).syncStockToRedis(any());
    }
    
    @Test
    void testRollbackForPaymentFailed_Success() {
        // 模拟订单取消回滚成功（支付失败使用相同逻辑）
        when(ticketStockService.rollbackStockToRedis(TICKET_ID, QUANTITY)).thenReturn(true);
        when(ticketStockService.unlockStock(TICKET_ID, QUANTITY)).thenReturn(true);
        when(ticketStockService.syncStockToRedis(TICKET_ID)).thenReturn(true);
        
        Boolean result = stockRollbackService.rollbackForPaymentFailed(TICKET_ID, QUANTITY, ORDER_ID);
        
        assertTrue(result);
        verify(ticketStockService).rollbackStockToRedis(TICKET_ID, QUANTITY);
        verify(ticketStockService).unlockStock(TICKET_ID, QUANTITY);
        verify(ticketStockService).syncStockToRedis(TICKET_ID);
    }
    
    @Test
    void testRollbackForPaymentFailed_Failed() {
        // 模拟数据库库存释放失败
        when(ticketStockService.rollbackStockToRedis(TICKET_ID, QUANTITY)).thenReturn(true);
        when(ticketStockService.unlockStock(TICKET_ID, QUANTITY)).thenReturn(false);
        
        Boolean result = stockRollbackService.rollbackForPaymentFailed(TICKET_ID, QUANTITY, ORDER_ID);
        
        assertFalse(result);
    }
    
    @Test
    void testRollbackForOrderTimeout_Success() {
        // 模拟订单取消回滚成功（订单超时使用相同逻辑）
        when(ticketStockService.rollbackStockToRedis(TICKET_ID, QUANTITY)).thenReturn(true);
        when(ticketStockService.unlockStock(TICKET_ID, QUANTITY)).thenReturn(true);
        when(ticketStockService.syncStockToRedis(TICKET_ID)).thenReturn(true);
        
        Boolean result = stockRollbackService.rollbackForOrderTimeout(TICKET_ID, QUANTITY, ORDER_ID);
        
        assertTrue(result);
        verify(ticketStockService).rollbackStockToRedis(TICKET_ID, QUANTITY);
        verify(ticketStockService).unlockStock(TICKET_ID, QUANTITY);
        verify(ticketStockService).syncStockToRedis(TICKET_ID);
    }
    
    @Test
    void testRollbackForOrderTimeout_Failed() {
        // 模拟数据库库存释放失败
        when(ticketStockService.rollbackStockToRedis(TICKET_ID, QUANTITY)).thenReturn(true);
        when(ticketStockService.unlockStock(TICKET_ID, QUANTITY)).thenReturn(false);
        
        Boolean result = stockRollbackService.rollbackForOrderTimeout(TICKET_ID, QUANTITY, ORDER_ID);
        
        assertFalse(result);
    }
    
    @Test
    void testBatchRollback_AllSuccess() {
        // 创建回滚项目列表
        StockRollbackService.RollbackItem item1 = new StockRollbackService.RollbackItem(1L, 2);
        
        StockRollbackService.RollbackItem item2 = new StockRollbackService.RollbackItem(2L, 3);
        
        List<StockRollbackService.RollbackItem> rollbackItems = Arrays.asList(item1, item2);
        
        // 模拟所有回滚都成功
        when(ticketStockService.rollbackStockToRedis(1L, 2)).thenReturn(true);
        when(ticketStockService.unlockStock(1L, 2)).thenReturn(true);
        when(ticketStockService.syncStockToRedis(1L)).thenReturn(true);
        
        when(ticketStockService.rollbackStockToRedis(2L, 3)).thenReturn(true);
        when(ticketStockService.unlockStock(2L, 3)).thenReturn(true);
        when(ticketStockService.syncStockToRedis(2L)).thenReturn(true);
        
        Integer successCount = stockRollbackService.batchRollback(rollbackItems, ORDER_ID);
        
        assertEquals(2, successCount);
        verify(ticketStockService).rollbackStockToRedis(1L, 2);
        verify(ticketStockService).unlockStock(1L, 2);
        verify(ticketStockService).syncStockToRedis(1L);
        verify(ticketStockService).rollbackStockToRedis(2L, 3);
        verify(ticketStockService).unlockStock(2L, 3);
        verify(ticketStockService).syncStockToRedis(2L);
    }
    
    @Test
    void testBatchRollback_PartialSuccess() {
        // 创建回滚项目列表
        StockRollbackService.RollbackItem item1 = new StockRollbackService.RollbackItem(1L, 2);
        
        StockRollbackService.RollbackItem item2 = new StockRollbackService.RollbackItem(2L, 3);
        
        List<StockRollbackService.RollbackItem> rollbackItems = Arrays.asList(item1, item2);
        
        // 模拟第一个成功，第二个失败
        when(ticketStockService.rollbackStockToRedis(1L, 2)).thenReturn(true);
        when(ticketStockService.unlockStock(1L, 2)).thenReturn(true);
        when(ticketStockService.syncStockToRedis(1L)).thenReturn(true);
        
        when(ticketStockService.rollbackStockToRedis(2L, 3)).thenReturn(true);
        when(ticketStockService.unlockStock(2L, 3)).thenReturn(false); // 第二个失败
        
        Integer successCount = stockRollbackService.batchRollback(rollbackItems, ORDER_ID);
        
        assertEquals(1, successCount);
        verify(ticketStockService).rollbackStockToRedis(1L, 2);
        verify(ticketStockService).unlockStock(1L, 2);
        verify(ticketStockService).syncStockToRedis(1L);
        verify(ticketStockService).rollbackStockToRedis(2L, 3);
        verify(ticketStockService).unlockStock(2L, 3);
        verify(ticketStockService, never()).syncStockToRedis(2L);
    }
    
    @Test
    void testBatchRollback_WithException() {
        // 创建回滚项目列表
        StockRollbackService.RollbackItem item1 = new StockRollbackService.RollbackItem(1L, 2);
        
        StockRollbackService.RollbackItem item2 = new StockRollbackService.RollbackItem(2L, 3);
        
        List<StockRollbackService.RollbackItem> rollbackItems = Arrays.asList(item1, item2);
        
        // 模拟第一个成功，第二个异常
        when(ticketStockService.rollbackStockToRedis(1L, 2)).thenReturn(true);
        when(ticketStockService.unlockStock(1L, 2)).thenReturn(true);
        when(ticketStockService.syncStockToRedis(1L)).thenReturn(true);
        
        when(ticketStockService.rollbackStockToRedis(2L, 3))
                .thenThrow(new RuntimeException("Redis连接异常"));
        
        Integer successCount = stockRollbackService.batchRollback(rollbackItems, ORDER_ID);
        
        assertEquals(1, successCount);
        verify(ticketStockService).rollbackStockToRedis(1L, 2);
        verify(ticketStockService).unlockStock(1L, 2);
        verify(ticketStockService).syncStockToRedis(1L);
        verify(ticketStockService).rollbackStockToRedis(2L, 3);
    }
    
    @Test
    void testBatchRollback_EmptyList() {
        List<StockRollbackService.RollbackItem> rollbackItems = Arrays.asList();
        
        Integer successCount = stockRollbackService.batchRollback(rollbackItems, ORDER_ID);
        
        assertEquals(0, successCount);
        verify(ticketStockService, never()).rollbackStockToRedis(any(), any());
        verify(ticketStockService, never()).unlockStock(any(), any());
        verify(ticketStockService, never()).syncStockToRedis(any());
    }
}