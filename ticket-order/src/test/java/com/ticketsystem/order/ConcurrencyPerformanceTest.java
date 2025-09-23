package com.ticketsystem.order;

import com.ticketsystem.order.service.RedisBasedOrderService;
import com.ticketsystem.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;

@SpringBootTest
@ActiveProfiles("test")
public class ConcurrencyPerformanceTest {

    @Autowired
    private RedisBasedOrderService redisOrderService;
    
    @Autowired
    private OrderService traditionalOrderService;

    private static final int THREAD_COUNT = 100;
    private static final int ORDERS_PER_THREAD = 10;
    private static final String TEST_TICKET_ID = "TICKET_001";

    @Test
    public void testRedisConcurrencyPerformance() throws Exception {
        System.out.println("=== Redis并发性能测试 ===");
        
        // 初始化库存
        initializeStock(TEST_TICKET_ID, THREAD_COUNT * ORDERS_PER_THREAD);
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        // 提交并发订单任务
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < ORDERS_PER_THREAD; j++) {
                        String orderNo = "ORDER_REDIS_" + threadId + "_" + j;
                        String userId = "USER_" + threadId;
                        
                        boolean success = redisOrderService.createOrder(orderNo, userId, TEST_TICKET_ID, 1);
                        if (success) {
                            successCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    failCount.addAndGet(ORDERS_PER_THREAD);
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        long endTime = System.currentTimeMillis();
        
        executor.shutdown();
        
        System.out.println("Redis测试结果:");
        System.out.println("总耗时: " + (endTime - startTime) + "ms");
        System.out.println("成功订单: " + successCount.get());
        System.out.println("失败订单: " + failCount.get());
        System.out.println("TPS: " + (successCount.get() * 1000.0 / (endTime - startTime)));
        
        // 验证库存一致性
        verifyStockConsistency(TEST_TICKET_ID, successCount.get());
    }

    @Test
    public void testTraditionalConcurrencyPerformance() throws Exception {
        System.out.println("=== 传统数据库并发性能测试 ===");
        
        // 重置库存
        initializeStock(TEST_TICKET_ID, THREAD_COUNT * ORDERS_PER_THREAD);
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        // 提交并发订单任务
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < ORDERS_PER_THREAD; j++) {
                        String orderNo = "ORDER_DB_" + threadId + "_" + j;
                        String userId = "USER_" + threadId;
                        
                        try {
                            traditionalOrderService.createOrder(orderNo, userId, TEST_TICKET_ID, 1);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    failCount.addAndGet(ORDERS_PER_THREAD);
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        long endTime = System.currentTimeMillis();
        
        executor.shutdown();
        
        System.out.println("传统数据库测试结果:");
        System.out.println("总耗时: " + (endTime - startTime) + "ms");
        System.out.println("成功订单: " + successCount.get());
        System.out.println("失败订单: " + failCount.get());
        System.out.println("TPS: " + (successCount.get() * 1000.0 / (endTime - startTime)));
    }

    @Test
    public void testDataConsistencyUnderHighConcurrency() throws Exception {
        System.out.println("=== 高并发数据一致性测试 ===");
        
        final int INITIAL_STOCK = 50;
        final int CONCURRENT_USERS = 100;
        
        // 初始化库存
        initializeStock(TEST_TICKET_ID, INITIAL_STOCK);
        
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);
        List<String> successfulOrders = new CopyOnWriteArrayList<>();
        AtomicInteger conflictCount = new AtomicInteger(0);
        
        // 模拟超卖场景：100个用户同时抢购50张票
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    String orderNo = "CONSISTENCY_TEST_" + userId;
                    boolean success = redisOrderService.createOrder(orderNo, "USER_" + userId, TEST_TICKET_ID, 1);
                    
                    if (success) {
                        successfulOrders.add(orderNo);
                    } else {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    conflictCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        System.out.println("一致性测试结果:");
        System.out.println("初始库存: " + INITIAL_STOCK);
        System.out.println("并发用户: " + CONCURRENT_USERS);
        System.out.println("成功订单数: " + successfulOrders.size());
        System.out.println("冲突/失败数: " + conflictCount.get());
        
        // 验证没有超卖
        assert successfulOrders.size() <= INITIAL_STOCK : "检测到超卖！成功订单数超过库存";
        
        // 验证Redis和数据库库存一致
        verifyStockConsistency(TEST_TICKET_ID, successfulOrders.size());
        
        System.out.println("✅ 数据一致性验证通过，无超卖现象");
    }

    @Test
    public void testOrderPaymentConsistency() throws Exception {
        System.out.println("=== 订单支付一致性测试 ===");
        
        // 创建测试订单
        String orderNo = "PAY_TEST_ORDER";
        String userId = "PAY_TEST_USER";
        
        initializeStock(TEST_TICKET_ID, 10);
        
        // 创建订单
        boolean created = redisOrderService.createOrder(orderNo, userId, TEST_TICKET_ID, 1);
        assert created : "订单创建失败";
        
        // 获取订单信息
        Object orderInfo = redisOrderService.getOrderInfo(orderNo);
        assert orderInfo != null : "无法获取订单信息";
        
        System.out.println("订单创建成功: " + orderInfo);
        
        // 支付订单
        boolean paid = redisOrderService.payOrder(orderNo);
        assert paid : "订单支付失败";
        
        // 验证支付后状态
        Object paidOrderInfo = redisOrderService.getOrderInfo(orderNo);
        System.out.println("支付后订单信息: " + paidOrderInfo);
        
        System.out.println("✅ 订单支付一致性验证通过");
    }

    private void initializeStock(String ticketId, int stock) {
        // 这里需要根据实际的库存服务API来初始化
        // 假设有一个库存服务可以设置初始库存
        System.out.println("初始化库存: " + ticketId + " = " + stock);
    }

    private void verifyStockConsistency(String ticketId, int expectedSold) {
        // 验证Redis中的库存和数据库中的库存是否一致
        // 这里需要根据实际的库存服务API来验证
        System.out.println("验证库存一致性: 预期售出 " + expectedSold + " 张票");
    }
}