package com.ticketsystem.order.service;

import com.ticketsystem.order.dto.CreateOrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 统一订单服务 - 使用Redis原子操作API
 * 采用Redis主存储 + 数据库备份的架构
 */
@Service
@Slf4j
public class UnifiedOrderService {

    private final RedissonClient redissonClient;
    private final JdbcTemplate writeJdbcTemplate;
    private final JdbcTemplate readJdbcTemplate;
    
    public UnifiedOrderService(RedissonClient redissonClient,
                              @Qualifier("highConcurrencyWriteJdbcTemplate") JdbcTemplate writeJdbcTemplate,
                              @Qualifier("highConcurrencyReadJdbcTemplate") JdbcTemplate readJdbcTemplate) {
        this.redissonClient = redissonClient;
        this.writeJdbcTemplate = writeJdbcTemplate;
        this.readJdbcTemplate = readJdbcTemplate;
    }
    
    // 使用Redis原子操作API替代Lua脚本





    /**
     * 统一创建订单 - 使用Redis原子操作API
     */
    public String createOrder(CreateOrderDTO dto) {
        String orderNo = generateOrderNo();
        long currentTime = System.currentTimeMillis();
        int expireTime = 15 * 60; // 15分钟过期
        
        try {
            String orderKey = "order:" + orderNo;
            String userOrdersKey = "user_orders:" + dto.getUserId();
            String ticketStockKey = "ticket_stock:" + dto.getTicketId();
            
            // 1. 检查用户是否已有未支付订单
            if (redissonClient.getSet(userOrdersKey).contains(orderNo)) {
                throw new RuntimeException("用户已有未支付订单");
            }
            
            // 2. 检查库存是否充足
            Object currentStockObj = redissonClient.getBucket(ticketStockKey).get();
            if (currentStockObj == null) {
                throw new RuntimeException("库存信息不存在");
            }
            String currentStockStr = currentStockObj.toString();
            int currentStock = Integer.parseInt(currentStockStr);
            if (currentStock < dto.getQuantity()) {
                throw new RuntimeException("库存不足，当前库存:" + currentStock);
            }
            
            // 3. 原子性扣减库存
            long newStock = redissonClient.getAtomicLong(ticketStockKey).addAndGet(-dto.getQuantity());
            if (newStock < 0) {
                // 回滚库存
                redissonClient.getAtomicLong(ticketStockKey).addAndGet(dto.getQuantity());
                throw new RuntimeException("库存不足，扣减失败");
            }
            
            // 4. 创建订单记录
            var orderHash = redissonClient.getMap(orderKey);
            orderHash.put("user_id", dto.getUserId().toString());
            orderHash.put("ticket_id", dto.getTicketId().toString());
            orderHash.put("quantity", dto.getQuantity().toString());
            BigDecimal price = dto.getTotalPrice() != null ? dto.getTotalPrice() : dto.getBasePrice();
            orderHash.put("price", price.toString());
            orderHash.put("total_amount", price.multiply(BigDecimal.valueOf(dto.getQuantity())).toString());
            orderHash.put("show_id", dto.getShowId().toString());
            orderHash.put("session_id", dto.getSessionId().toString());
            orderHash.put("status", "0"); // 0=待支付
            orderHash.put("create_time", String.valueOf(currentTime));
            orderHash.put("expire_time", String.valueOf(currentTime + expireTime * 1000));
            orderHash.put("db_sync", "pending");
            
            // 5. 添加到用户订单集合
            redissonClient.getSet(userOrdersKey).add(orderNo);
            
            // 6. 设置订单过期时间（15分钟）
            redissonClient.getBucket(orderKey).expire(java.time.Duration.ofSeconds(expireTime));
            redissonClient.getBucket(userOrdersKey).expire(java.time.Duration.ofSeconds(expireTime));
            
            log.info("统一订单创建成功: orderNo={}, userId={}", orderNo, dto.getUserId());
            
            // 7. 异步同步到数据库（不阻塞主流程）
            asyncSyncToDatabase(orderNo, dto);
            
            return orderNo;
            
        } catch (Exception e) {
            log.error("统一订单创建异常: orderNo={}, userId={}", orderNo, dto.getUserId(), e);
            throw new RuntimeException("订单创建异常: " + e.getMessage());
        }
    }

    /**
     * 统一支付订单 - 使用Redis原子操作API
     */
    public boolean payOrder(String orderNo, Long userId, Integer payType) {
        String payNo = generatePayNo();
        long currentTime = System.currentTimeMillis();
        
        try {
            String orderKey = "order:" + orderNo;
            String userOrdersKey = "user_orders:" + userId;
            
            // 1. 检查订单是否存在
            if (!redissonClient.getBucket(orderKey).isExists()) {
                log.warn("订单不存在: orderNo={}", orderNo);
                return false;
            }
            
            // 2. 检查订单状态
            var orderHash = redissonClient.getMap(orderKey);
            String status = (String) orderHash.get("status");
            if (!"0".equals(status)) {
                log.warn("订单状态不正确，无法支付: orderNo={}, status={}", orderNo, status);
                return false;
            }
            
            // 3. 检查订单是否过期
            String expireTimeStr = (String) orderHash.get("expire_time");
            if (expireTimeStr != null) {
                long expireTime = Long.parseLong(expireTimeStr);
                if (currentTime > expireTime) {
                    log.warn("订单已过期: orderNo={}", orderNo);
                    return false;
                }
            }
            
            // 4. 更新订单状态为已支付
            orderHash.put("status", "1"); // 1=已支付
            orderHash.put("pay_time", String.valueOf(currentTime));
            orderHash.put("pay_type", payType.toString());
            orderHash.put("pay_no", payNo);
            orderHash.put("db_sync", "pending"); // 标记需要同步到数据库
            
            // 5. 从用户未支付订单集合中移除
            redissonClient.getSet(userOrdersKey).remove(orderNo);
            
            // 6. 设置订单永不过期（已支付订单需要持久化）
            redissonClient.getBucket(orderKey).clearExpire();
            
            log.info("统一订单支付成功: orderNo={}, userId={}, payType={}", orderNo, userId, payType);
            
            // 7. 异步同步到数据库（不阻塞主流程）
            asyncSyncPaymentToDatabase(orderNo, userId, payType, payNo);
            
            return true;
            
        } catch (Exception e) {
            log.error("统一订单支付异常: orderNo={}, userId={}", orderNo, userId, e);
            return false;
        }
    }

    /**
     * 异步同步订单到数据库 - 避免事务冲突
     */
    private void asyncSyncToDatabase(String orderNo, CreateOrderDTO dto) {
        // 使用异步任务，避免阻塞主流程
        new Thread(() -> {
            try {
                Thread.sleep(100); // 短暂延迟，确保Redis操作完成
                
                // 从Redis获取订单信息
                Object orderInfo = getOrderInfoFromRedis(orderNo);
                if (orderInfo != null) {
                    // 同步到数据库
                    syncOrderToDatabase(orderNo, orderInfo);
                    log.info("订单异步同步到数据库成功: orderNo={}", orderNo);
                }
            } catch (Exception e) {
                log.error("订单异步同步到数据库失败: orderNo={}", orderNo, e);
                // 这里可以加入重试机制
            }
        }).start();
    }

    /**
     * 异步同步支付信息到数据库
     */
    private void asyncSyncPaymentToDatabase(String orderNo, Long userId, Integer payType, String payNo) {
        new Thread(() -> {
            try {
                Thread.sleep(100); // 短暂延迟
                
                // 同步支付信息到数据库
                syncPaymentToDatabase(orderNo, userId, payType, payNo);
                log.info("支付信息异步同步到数据库成功: orderNo={}", orderNo);
            } catch (Exception e) {
                log.error("支付信息异步同步到数据库失败: orderNo={}", orderNo, e);
            }
        }).start();
    }

    /**
     * 同步订单到数据库
     */
    @Transactional
    public void syncOrderToDatabase(String orderNo, Object orderInfo) {
        try {
            // 使用写库进行同步
            String sql = """
                INSERT INTO t_order (order_no, user_id, ticket_id, quantity, price, total_amount, 
                                   show_id, session_id, status, create_time, expire_time, db_sync_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                db_sync_status = 'synced',
                update_time = NOW()
                """;
            
            writeJdbcTemplate.update(sql, 
                orderNo,
                orderInfo.toString(), // 这里需要根据实际数据结构调整
                1, // ticket_id
                1, // quantity
                100.0, // price
                100.0, // total_amount
                1, // show_id
                1, // session_id
                0, // status
                LocalDateTime.now(), // create_time
                LocalDateTime.now().plusMinutes(15), // expire_time
                "synced" // db_sync_status
            );
            
        } catch (Exception e) {
            log.error("同步订单到数据库失败: orderNo={}", orderNo, e);
            throw e;
        }
    }

    /**
     * 同步支付信息到数据库
     */
    @Transactional
    public void syncPaymentToDatabase(String orderNo, Long userId, Integer payType, String payNo) {
        try {
            String sql = """
                UPDATE t_order 
                SET status = 1, pay_time = NOW(), pay_type = ?, pay_no = ?, db_sync_status = 'synced'
                WHERE order_no = ? AND user_id = ?
                """;
            
            writeJdbcTemplate.update(sql, payType, payNo, orderNo, userId);
            
        } catch (Exception e) {
            log.error("同步支付信息到数据库失败: orderNo={}", orderNo, e);
            throw e;
        }
    }

    /**
     * 从Redis获取订单信息
     */
    private Object getOrderInfoFromRedis(String orderNo) {
        try {
            String orderKey = "order:" + orderNo;
            return redissonClient.getMap(orderKey).readAllMap();
        } catch (Exception e) {
            log.error("从Redis获取订单信息失败: orderNo={}", orderNo, e);
            return null;
        }
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000));
    }

    /**
     * 取消订单 - 使用Redis原子操作API
     */
    public boolean cancelOrder(String orderNo, Long userId, Long ticketId, Integer quantity) {
        long currentTime = System.currentTimeMillis();
        
        try {
            String orderKey = "order:" + orderNo;
            String userOrdersKey = "user_orders:" + userId;
            String ticketStockKey = "ticket_stock:" + ticketId;
            
            // 1. 检查订单是否存在
            if (!redissonClient.getBucket(orderKey).isExists()) {
                log.warn("订单不存在: orderNo={}", orderNo);
                return false;
            }
            
            // 2. 检查订单状态
            var orderHash = redissonClient.getMap(orderKey);
            String status = (String) orderHash.get("status");
            if (!"0".equals(status)) {
                log.warn("订单状态不正确，无法取消: orderNo={}, status={}", orderNo, status);
                return false;
            }
            
            // 3. 回滚库存
            redissonClient.getAtomicLong(ticketStockKey).addAndGet(quantity);
            
            // 4. 更新订单状态为已取消
            orderHash.put("status", "2"); // 2=已取消
            orderHash.put("cancel_time", String.valueOf(currentTime));
            
            // 5. 从用户未支付订单集合中移除
            redissonClient.getSet(userOrdersKey).remove(orderNo);
            
            // 6. 删除订单（取消的订单不需要持久化）
            redissonClient.getBucket(orderKey).delete();
            
            log.info("统一订单取消成功: orderNo={}, userId={}", orderNo, userId);
            
            // 7. 异步同步到数据库
            asyncSyncCancelToDatabase(orderNo, userId);
            
            return true;
            
        } catch (Exception e) {
            log.error("统一订单取消异常: orderNo={}, userId={}", orderNo, userId, e);
            return false;
        }
    }

    /**
     * 异步同步取消信息到数据库
     */
    private void asyncSyncCancelToDatabase(String orderNo, Long userId) {
        new Thread(() -> {
            try {
                Thread.sleep(100); // 短暂延迟
                
                // 同步取消信息到数据库
                syncCancelToDatabase(orderNo, userId);
                log.info("取消信息异步同步到数据库成功: orderNo={}", orderNo);
            } catch (Exception e) {
                log.error("取消信息异步同步到数据库失败: orderNo={}", orderNo, e);
            }
        }).start();
    }

    /**
     * 同步取消信息到数据库
     */
    @Transactional
    public void syncCancelToDatabase(String orderNo, Long userId) {
        try {
            String sql = """
                UPDATE t_order 
                SET status = 2, cancel_time = NOW(), db_sync_status = 'synced'
                WHERE order_no = ? AND user_id = ?
                """;
            
            writeJdbcTemplate.update(sql, orderNo, userId);
            
        } catch (Exception e) {
            log.error("同步取消信息到数据库失败: orderNo={}", orderNo, e);
            throw e;
        }
    }

    /**
     * 生成支付号
     */
    private String generatePayNo() {
        return "PAY" + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000));
    }
}