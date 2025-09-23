package com.ticketsystem.show.service;

import com.ticketsystem.show.service.RedisStockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Redis库存服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class RedisStockServiceTest {

    @Mock
    private RedissonClient redissonClient;
    
    @Mock
    private RScript rScript;
    
    @InjectMocks
    private RedisStockService redisStockService;
    
    private static final Long TICKET_ID = 1L;
    private static final Integer QUANTITY = 2;
    private static final Integer STOCK_AMOUNT = 100;
    private static final Integer EXPIRE_SECONDS = 3600;
    
    @BeforeEach
    void setUp() {
        when(redissonClient.getScript()).thenReturn(rScript);
    }
    
    @Test
    void testInitStock_Success() {
        // 模拟Lua脚本返回1（初始化成功）
        when(rScript.eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER), 
                any(List.class), any(Object[].class))).thenReturn(1L);
        
        Boolean result = redisStockService.initStock(TICKET_ID, STOCK_AMOUNT, false);
        
        assertTrue(result);
        verify(rScript).eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER), 
                any(List.class), any(Object[].class));
    }
    
    @Test
    void testInitStock_AlreadyExists() {
        // 模拟Lua脚本返回0（库存已存在）
        when(rScript.eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER), 
                any(List.class), any(Object[].class))).thenReturn(0L);
        
        Boolean result = redisStockService.initStock(TICKET_ID, STOCK_AMOUNT, false);
        
        assertFalse(result);
    }
    
    @Test
    void testPredeductStock_Success() {
        // 模拟Lua脚本返回1（预减成功）
        when(rScript.eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER), 
                any(List.class), any(Object[].class))).thenReturn(1L);
        
        Integer result = redisStockService.predeductStock(TICKET_ID, QUANTITY);
        
        assertEquals(1, result);
        verify(rScript).eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER), 
                any(List.class), any(Object[].class));
    }
    
    @Test
    void testPredeductStock_InsufficientStock() {
        // 模拟Lua脚本返回0（库存不足）
        when(rScript.eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER), 
                any(List.class), any(Object[].class))).thenReturn(0L);
        
        Integer result = redisStockService.predeductStock(TICKET_ID, QUANTITY);
        
        assertFalse(result);
    }
    
    @Test
    void testPredeductStock_StockNotExists() {
        // 模拟Lua脚本返回-1（库存不存在）
        when(rScript.eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER), 
                any(List.class), any(Object[].class))).thenReturn(-1L);
        
        Integer result = redisStockService.predeductStock(TICKET_ID, QUANTITY);
        
        assertEquals(-1, result);
    }
    
    @Test
    void testRollbackStock_Success() {
        // 模拟Lua脚本返回1（回滚成功）
        when(rScript.eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER), 
                any(List.class), any(Object[].class))).thenReturn(1L);
        
        Integer result = redisStockService.rollbackStock(TICKET_ID, QUANTITY, 100);
        
        assertEquals(1, result);
        verify(rScript).eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER), 
                any(List.class), any(Object[].class));
    }
    
    @Test
    void testRollbackStock_ExceedsMaxStock() {
        // 模拟Lua脚本返回0（超过最大库存）
        when(rScript.eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER), 
                any(List.class), any(Object[].class))).thenReturn(0L);
        
        Integer result = redisStockService.rollbackStock(TICKET_ID, QUANTITY, 100);
        
        assertEquals(0, result);
    }
    
    @Test
    void testRollbackStock_StockNotExists() {
        // 模拟Lua脚本返回-1（库存不存在）
        when(rScript.eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER), 
                any(List.class), any(Object[].class))).thenReturn(-1L);
        
        Integer result = redisStockService.rollbackStock(TICKET_ID, QUANTITY, 100);
        
        assertEquals(-1, result);
    }
    
    @Test
    void testPredeductStock_WithException() {
        // 模拟Redis异常
        when(rScript.eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER), 
                any(List.class), any(Object[].class))).thenThrow(new RuntimeException("Redis连接异常"));
        
        Integer result = redisStockService.predeductStock(TICKET_ID, QUANTITY);
        
        assertEquals(-1, result);
    }
    
    @Test
    void testRollbackStock_WithException() {
        // 模拟Redis异常
        when(rScript.eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER), 
                any(List.class), any(Object[].class))).thenThrow(new RuntimeException("Redis连接异常"));
        
        Integer result = redisStockService.rollbackStock(TICKET_ID, QUANTITY, 100);
        
        assertEquals(0, result);
    }
    
    @Test
    void testInitStock_WithForceUpdate() {
        // 模拟强制更新，Lua脚本返回1（更新成功）
        when(rScript.eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER), 
                any(List.class), any(Object[].class))).thenReturn(1L);
        
        Boolean result = redisStockService.initStock(TICKET_ID, STOCK_AMOUNT, true);
        
        assertTrue(result);
        verify(rScript).eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER), 
                any(List.class), any(Object[].class));
    }
}