package com.ticketsystem.show.service;

import com.ticketsystem.show.entity.TicketStock;
import com.ticketsystem.show.mapper.TicketStockMapper;
import com.ticketsystem.show.service.RedisStockService;
import com.ticketsystem.show.service.impl.TicketStockServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 票档库存服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class TicketStockServiceTest {

    @Mock
    private TicketStockMapper ticketStockMapper;
    
    @Mock
    private RedisStockService redisStockService;
    
    @InjectMocks
    private TicketStockServiceImpl ticketStockService;
    
    private static final Long TICKET_ID = 1L;
    private static final Integer QUANTITY = 2;
    private static final Integer TOTAL_STOCK = 100;
    private static final Integer AVAILABLE_STOCK = 80;
    
    private TicketStock mockTicketStock;
    
    @BeforeEach
    void setUp() {
        mockTicketStock = new TicketStock();
        mockTicketStock.setTicketId(TICKET_ID);
        mockTicketStock.setTotalStock(TOTAL_STOCK);
        mockTicketStock.setAvailableStock(AVAILABLE_STOCK);
        mockTicketStock.setLockedStock(20);
        mockTicketStock.setSoldStock(0);
        mockTicketStock.setVersion(1);
    }
    
    @Test
    void testPredeductStockFromRedis_Success() {
        // 模拟Redis预减成功
        when(redisStockService.predeductStock(eq(TICKET_ID), eq(QUANTITY))).thenReturn(1);
        
        Integer result = ticketStockService.predeductStockFromRedis(TICKET_ID, QUANTITY);
        
        assertEquals(1, result);
        verify(redisStockService).predeductStock(eq(TICKET_ID), eq(QUANTITY));
    }
    
    @Test
    void testPredeductStockFromRedis_Failed() {
        // 模拟Redis预减失败（库存不足）
        when(redisStockService.predeductStock(eq(TICKET_ID), eq(QUANTITY))).thenReturn(0);
        
        Integer result = ticketStockService.predeductStockFromRedis(TICKET_ID, QUANTITY);
        
        assertEquals(0, result);
        verify(redisStockService).predeductStock(eq(TICKET_ID), eq(QUANTITY));
    }
    
    @Test
    void testRollbackStockToRedis_Success() {
        // 模拟数据库查询
        when(ticketStockMapper.selectByTicketId(TICKET_ID)).thenReturn(mockTicketStock);
        // 模拟Redis回滚成功
        when(redisStockService.rollbackStock(eq(TICKET_ID), eq(QUANTITY), eq(TOTAL_STOCK))).thenReturn(1);
        
        Boolean result = ticketStockService.rollbackStockToRedis(TICKET_ID, QUANTITY);
        
        assertTrue(result);
        verify(ticketStockMapper).selectByTicketId(TICKET_ID);
        verify(redisStockService).rollbackStock(eq(TICKET_ID), eq(QUANTITY), eq(TOTAL_STOCK));
    }
    
    @Test
    void testRollbackStockToRedis_TicketNotFound() {
        // 模拟票档不存在
        when(ticketStockMapper.selectByTicketId(TICKET_ID)).thenReturn(null);
        
        Boolean result = ticketStockService.rollbackStockToRedis(TICKET_ID, QUANTITY);
        
        assertFalse(result);
        verify(ticketStockMapper).selectByTicketId(TICKET_ID);
        verify(redisStockService, never()).rollbackStock(any(), any(), any());
    }
    
    @Test
    void testRollbackStockToRedis_RedisFailed() {
        // 模拟数据库查询成功
        when(ticketStockMapper.selectByTicketId(TICKET_ID)).thenReturn(mockTicketStock);
        // 模拟Redis回滚失败
        when(redisStockService.rollbackStock(eq(TICKET_ID), eq(QUANTITY), eq(TOTAL_STOCK))).thenReturn(0);
        
        Boolean result = ticketStockService.rollbackStockToRedis(TICKET_ID, QUANTITY);
        
        assertFalse(result);
        verify(ticketStockMapper).selectByTicketId(TICKET_ID);
        verify(redisStockService).rollbackStock(eq(TICKET_ID), eq(QUANTITY), eq(TOTAL_STOCK), anyInt());
    }
    
    @Test
    void testSyncStockToRedis_Success() {
        // 模拟数据库查询
        when(ticketStockMapper.selectByTicketId(TICKET_ID)).thenReturn(mockTicketStock);
        // 模拟Redis初始化成功
        when(redisStockService.initStock(eq(TICKET_ID), eq(AVAILABLE_STOCK), eq(true))).thenReturn(true);
        
        Boolean result = ticketStockService.syncStockToRedis(TICKET_ID);
        
        assertTrue(result);
        verify(ticketStockMapper).selectByTicketId(TICKET_ID);
        verify(redisStockService).initStock(eq(TICKET_ID), eq(AVAILABLE_STOCK), eq(true));
    }
    
    @Test
    void testSyncStockToRedis_TicketNotFound() {
        // 模拟票档不存在
        when(ticketStockMapper.selectByTicketId(TICKET_ID)).thenReturn(null);
        
        Boolean result = ticketStockService.syncStockToRedis(TICKET_ID);
        
        assertFalse(result);
        verify(ticketStockMapper).selectByTicketId(TICKET_ID);
        verify(redisStockService, never()).initStock(any(), any(), anyBoolean());
    }
    
    @Test
    void testSyncStockToRedis_RedisFailed() {
        // 模拟数据库查询成功
        when(ticketStockMapper.selectByTicketId(TICKET_ID)).thenReturn(mockTicketStock);
        // 模拟Redis初始化失败
        when(redisStockService.initStock(eq(TICKET_ID), eq(AVAILABLE_STOCK), eq(true))).thenReturn(false);
        
        Boolean result = ticketStockService.syncStockToRedis(TICKET_ID);
        
        assertFalse(result);
        verify(ticketStockMapper).selectByTicketId(TICKET_ID);
        verify(redisStockService).initStock(eq(TICKET_ID), eq(AVAILABLE_STOCK), eq(true));
    }
    
    @Test
    void testPredeductStockFromRedis_WithException() {
        // 模拟Redis异常
        when(redisStockService.predeductStock(eq(TICKET_ID), eq(QUANTITY)))
                .thenThrow(new RuntimeException("Redis连接异常"));
        
        Boolean result = ticketStockService.predeductStockFromRedis(TICKET_ID, QUANTITY);
        
        assertFalse(result);
        verify(redisStockService).predeductStock(eq(TICKET_ID), eq(QUANTITY));
    }
    
    @Test
    void testRollbackStockToRedis_WithDatabaseException() {
        // 模拟数据库异常
        when(ticketStockMapper.selectByTicketId(TICKET_ID))
                .thenThrow(new RuntimeException("数据库连接异常"));
        
        Boolean result = ticketStockService.rollbackStockToRedis(TICKET_ID, QUANTITY);
        
        assertFalse(result);
        verify(ticketStockMapper).selectByTicketId(TICKET_ID);
        verify(redisStockService, never()).rollbackStock(any(), any(), any(), anyInt());
    }
    
    @Test
    void testSyncStockToRedis_WithDatabaseException() {
        // 模拟数据库异常
        when(ticketStockMapper.selectByTicketId(TICKET_ID))
                .thenThrow(new RuntimeException("数据库连接异常"));
        
        Boolean result = ticketStockService.syncStockToRedis(TICKET_ID);
        
        assertFalse(result);
        verify(ticketStockMapper).selectByTicketId(TICKET_ID);
        verify(redisStockService, never()).initStock(any(), any(), anyInt(), anyBoolean());
    }
    
    @Test
    void testPredeductStockFromRedis_WithNullParameters() {
        // 测试空参数
        Boolean result1 = ticketStockService.predeductStockFromRedis(null, QUANTITY);
        Boolean result2 = ticketStockService.predeductStockFromRedis(TICKET_ID, null);
        Boolean result3 = ticketStockService.predeductStockFromRedis(TICKET_ID, 0);
        Boolean result4 = ticketStockService.predeductStockFromRedis(TICKET_ID, -1);
        
        assertFalse(result1);
        assertFalse(result2);
        assertFalse(result3);
        assertFalse(result4);
        
        verify(redisStockService, never()).predeductStock(any(), any(), anyInt());
    }
    
    @Test
    void testRollbackStockToRedis_WithNullParameters() {
        // 测试空参数
        Boolean result1 = ticketStockService.rollbackStockToRedis(null, QUANTITY);
        Boolean result2 = ticketStockService.rollbackStockToRedis(TICKET_ID, null);
        Boolean result3 = ticketStockService.rollbackStockToRedis(TICKET_ID, 0);
        Boolean result4 = ticketStockService.rollbackStockToRedis(TICKET_ID, -1);
        
        assertFalse(result1);
        assertFalse(result2);
        assertFalse(result3);
        assertFalse(result4);
        
        verify(ticketStockMapper, never()).selectByTicketId(any());
        verify(redisStockService, never()).rollbackStock(any(), any(), any(), anyInt());
    }
    
    @Test
    void testSyncStockToRedis_WithNullParameter() {
        // 测试空参数
        Boolean result = ticketStockService.syncStockToRedis(null);
        
        assertFalse(result);
        verify(ticketStockMapper, never()).selectByTicketId(any());
        verify(redisStockService, never()).initStock(any(), any(), anyInt(), anyBoolean());
    }
}