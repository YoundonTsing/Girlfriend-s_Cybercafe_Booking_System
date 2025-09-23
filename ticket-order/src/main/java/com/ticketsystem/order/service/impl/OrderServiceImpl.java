package com.ticketsystem.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ticketsystem.common.annotation.PerformanceMonitor;
import com.ticketsystem.common.exception.BusinessException;
import com.ticketsystem.common.result.Result;
import com.ticketsystem.order.dto.CreateOrderDTO;
import com.ticketsystem.order.entity.Order;
import com.ticketsystem.order.feign.ShowFeignClient;
import com.ticketsystem.order.feign.dto.ShowInfoDTO;
import com.ticketsystem.order.mapper.OrderMapper;
import com.ticketsystem.order.service.OrderService;
import com.ticketsystem.order.service.CompensationService;
import com.ticketsystem.order.util.SnowflakeIdWorker;
import com.ticketsystem.order.vo.OrderVO;
// import io.seata.spring.annotation.GlobalTransactional; // 暂时注释
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final ShowFeignClient showFeignClient;
    private final RedissonClient redissonClient;
    private final CompensationService compensationService;
    private final Environment env;
    private final SnowflakeIdWorker snowflakeIdWorker;
    
    // Redis分布式锁相关常量
    private static final String ORDER_LOCK_PREFIX = "order:lock:";
    private static final int LOCK_EXPIRE_TIME = 30; // 30秒，增加锁超时时间
    private static final int LOCK_WAIT_TIME = 3; // 等待获取锁的时间
    private static final int MAX_RETRY_TIMES = 3; // 最大重试次数

    private static final Map<Integer, String> ORDER_STATUS_MAP = Map.of(
            0, "待支付",
            1, "已支付",
            2, "已取消",
            3, "已完成",
            4, "已退款"
    );

    private static final Map<Integer, String> PAY_TYPE_MAP = Map.of(
            1, "支付宝",
            2, "微信",
            3, "银行卡"
    );

    @Override
    @PerformanceMonitor(value = "createOrder", slowQueryThreshold = 2000)
    // @GlobalTransactional // 暂时注释
    public String createOrder(CreateOrderDTO createOrderDTO) {
        // 分布式锁，防止同一用户重复下单
        // 根据配置决定锁的粒度
        boolean lockByTicketId = env.getProperty("app.order.lock-by-ticketid", Boolean.class, false);
        String lockKey;
        if (lockByTicketId) {
            lockKey = ORDER_LOCK_PREFIX + "create:" + createOrderDTO.getUserId() + ":" + createOrderDTO.getTicketId();
            log.info("使用 ticketId 粒度的分布式锁, lockKey: {}", lockKey);
        } else {
            lockKey = ORDER_LOCK_PREFIX + "create:" + createOrderDTO.getUserId();
            log.info("使用 userId 粒度的分布式锁, lockKey: {}", lockKey);
        }
        
        try {
            // 尝试获取Redisson分布式锁，Fail-Fast：不可用立即失败
            // 使用带重试的分布式锁，提高并发成功率
            if (!tryLockWithRedissonRetry(lockKey, LOCK_WAIT_TIME, LOCK_EXPIRE_TIME, MAX_RETRY_TIMES)) {
                throw new BusinessException("系统繁忙，请稍后重试");
            }
            
            // 入参校验与关键参数日志
            if (createOrderDTO.getTicketId() == null || createOrderDTO.getTicketId() <= 0) {
                log.error("非法票档ID: {}，请求参数: userId={}, showId={}, sessionId={}, quantity={}",
                        createOrderDTO.getTicketId(), createOrderDTO.getUserId(), createOrderDTO.getShowId(),
                        createOrderDTO.getSessionId(), createOrderDTO.getQuantity());
                throw new BusinessException("非法票档ID");
            }
            if (createOrderDTO.getQuantity() == null || createOrderDTO.getQuantity() <= 0) {
                log.error("非法购买数量: {}，ticketId={}, userId={}", createOrderDTO.getQuantity(), createOrderDTO.getTicketId(), createOrderDTO.getUserId());
                throw new BusinessException("非法购买数量");
            }
            log.info("CreateOrder 入参校验通过 - userId={}, ticketId={}, quantity={}, showId={}, sessionId={}",
                    createOrderDTO.getUserId(), createOrderDTO.getTicketId(), createOrderDTO.getQuantity(),
                    createOrderDTO.getShowId(), createOrderDTO.getSessionId());
            
            // 查询票档价格（带预检重试与可选开发兜底）
            BigDecimal price = fetchTicketPriceWithRetry(createOrderDTO.getTicketId());
            if (price == null) {
                boolean allowDevFallback = env.getProperty("app.order.allow-dev-fallback-price", Boolean.class, false);
                if (allowDevFallback) {
                    BigDecimal fallback = new BigDecimal(env.getProperty("app.order.dev-fallback-price", "100.00"));
                    log.warn("DEV ONLY: 票价获取失败，启用开发兜底价 {}，ticketId={}", fallback, createOrderDTO.getTicketId());
                    price = fallback;
                } else {
                    log.error("获取票价失败(预检重试后仍失败), ticketId={}, userId={}, showId={}, sessionId={}",
                            createOrderDTO.getTicketId(), createOrderDTO.getUserId(), createOrderDTO.getShowId(), createOrderDTO.getSessionId());
                    throw new BusinessException("获取票价失败: 票档不存在或不可售");
                }
            }
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("票价异常，票档ID: {}, 价格: {}", createOrderDTO.getTicketId(), price);
                throw new BusinessException("票价信息异常");
            }
            
            // 先生成订单号，用于补偿记录
            String tempOrderNo = generateOrderNo();
            
            // Redis预减库存（带补偿机制）
            Integer stockResult = null;
            try {
                Result<Integer> predeductResult = showFeignClient.predeductStockFromRedis(
                        createOrderDTO.getTicketId(), createOrderDTO.getQuantity());
                if (!predeductResult.getCode().equals(200) || predeductResult.getData() == null) {
                    log.error("Redis预减库存调用失败，票档ID: {}, 数量: {}, 错误信息: {}", 
                        createOrderDTO.getTicketId(), createOrderDTO.getQuantity(), predeductResult.getMessage());
                    
                    // 尝试补偿重试
                    try {
                        stockResult = compensationService.compensateStockPrededuct(
                            createOrderDTO.getTicketId(), createOrderDTO.getQuantity(), tempOrderNo);
                        log.info("Redis预减库存补偿成功，票档ID: {}, 数量: {}, 结果: {}", 
                            createOrderDTO.getTicketId(), createOrderDTO.getQuantity(), stockResult);
                    } catch (Exception compensateEx) {
                        log.error("Redis预减库存补偿失败，票档ID: {}, 数量: {}", 
                            createOrderDTO.getTicketId(), createOrderDTO.getQuantity(), compensateEx);
                        compensationService.recordCompensationFailure(tempOrderNo, 
                            createOrderDTO.getTicketId(), createOrderDTO.getQuantity(), 
                            "PREDEDUCT_STOCK", compensateEx.getMessage());
                        throw new BusinessException("库存操作失败: " + predeductResult.getMessage());
                    }
                } else {
                    stockResult = predeductResult.getData();
                }
            } catch (Exception feignEx) {
                log.error("Redis预减库存Feign调用异常，票档ID: {}, 数量: {}", 
                    createOrderDTO.getTicketId(), createOrderDTO.getQuantity(), feignEx);
                
                // 尝试补偿重试
                try {
                    stockResult = compensationService.compensateStockPrededuct(
                        createOrderDTO.getTicketId(), createOrderDTO.getQuantity(), tempOrderNo);
                    log.info("Redis预减库存异常后补偿成功，票档ID: {}, 数量: {}, 结果: {}", 
                        createOrderDTO.getTicketId(), createOrderDTO.getQuantity(), stockResult);
                } catch (Exception compensateEx) {
                    log.error("Redis预减库存异常后补偿失败，票档ID: {}, 数量: {}", 
                        createOrderDTO.getTicketId(), createOrderDTO.getQuantity(), compensateEx);
                    compensationService.recordCompensationFailure(tempOrderNo, 
                        createOrderDTO.getTicketId(), createOrderDTO.getQuantity(), 
                        "PREDEDUCT_STOCK_EXCEPTION", feignEx.getMessage());
                    throw new BusinessException("库存预减异常: " + feignEx.getMessage());
                }
            }
            
            if (stockResult != 1) {
                String errorMsg = stockResult == 0 ? "库存不足" : "库存信息异常";
                log.warn("Redis预减库存失败，票档ID: {}, 数量: {}, 结果: {}", 
                    createOrderDTO.getTicketId(), createOrderDTO.getQuantity(), stockResult);
                throw new BusinessException(errorMsg);
            }
            
            // 锁定数据库库存（在Redis预减成功后）
            try {
                Result<Boolean> lockResult = showFeignClient.lockTicketStock(
                        createOrderDTO.getTicketId(), createOrderDTO.getQuantity());
                if (!lockResult.getCode().equals(200) || !Boolean.TRUE.equals(lockResult.getData())) {
                    log.error("数据库库存锁定失败，票档ID: {}, 数量: {}, 错误信息: {}", 
                        createOrderDTO.getTicketId(), createOrderDTO.getQuantity(), lockResult.getMessage());
                    
                    // 回滚Redis库存
                    try {
                        Result<Boolean> rollbackResult = showFeignClient.rollbackStockToRedis(
                            createOrderDTO.getTicketId(), createOrderDTO.getQuantity());
                        if (!rollbackResult.getCode().equals(200) || !Boolean.TRUE.equals(rollbackResult.getData())) {
                            log.error("Redis库存回滚失败，票档ID: {}, 数量: {}, 错误: {}", 
                                createOrderDTO.getTicketId(), createOrderDTO.getQuantity(), rollbackResult.getMessage());
                        } else {
                            log.info("Redis库存回滚成功，票档ID: {}, 数量: {}", 
                                createOrderDTO.getTicketId(), createOrderDTO.getQuantity());
                        }
                    } catch (Exception rollbackEx) {
                        log.error("Redis库存回滚异常，票档ID: {}, 数量: {}", 
                            createOrderDTO.getTicketId(), createOrderDTO.getQuantity(), rollbackEx);
                    }
                    
                    throw new BusinessException("数据库库存锁定失败: " + lockResult.getMessage());
                }
                log.info("数据库库存锁定成功，票档ID: {}, 数量: {}", 
                    createOrderDTO.getTicketId(), createOrderDTO.getQuantity());
            } catch (Exception lockEx) {
                log.error("数据库库存锁定异常，票档ID: {}, 数量: {}", 
                    createOrderDTO.getTicketId(), createOrderDTO.getQuantity(), lockEx);
                
                // 回滚Redis库存
                try {
                    Result<Boolean> rollbackResult = showFeignClient.rollbackStockToRedis(
                        createOrderDTO.getTicketId(), createOrderDTO.getQuantity());
                    if (!rollbackResult.getCode().equals(200) || !Boolean.TRUE.equals(rollbackResult.getData())) {
                        log.error("Redis库存回滚失败，票档ID: {}, 数量: {}, 错误: {}", 
                            createOrderDTO.getTicketId(), createOrderDTO.getQuantity(), rollbackResult.getMessage());
                    } else {
                        log.info("Redis库存回滚成功，票档ID: {}, 数量: {}", 
                            createOrderDTO.getTicketId(), createOrderDTO.getQuantity());
                    }
                } catch (Exception rollbackEx) {
                    log.error("Redis库存回滚异常，票档ID: {}, 数量: {}", 
                        createOrderDTO.getTicketId(), createOrderDTO.getQuantity(), rollbackEx);
                }
                
                throw new BusinessException("数据库库存锁定异常: " + lockEx.getMessage());
            }
            
            // 获取演出信息
            Result<ShowInfoDTO> showInfoResult = showFeignClient.getShowInfo(
                    createOrderDTO.getShowId(), createOrderDTO.getSessionId());
            if (!showInfoResult.getCode().equals(200) || showInfoResult.getData() == null) {
                log.error("获取演出信息失败，演出ID: {}, 场次ID: {}, 错误信息: {}", 
                    createOrderDTO.getShowId(), createOrderDTO.getSessionId(), showInfoResult.getMessage());
                // 回滚Redis库存
                try {
                    Result<Boolean> rollbackResult = showFeignClient.rollbackStockToRedis(
                        createOrderDTO.getTicketId(), createOrderDTO.getQuantity());
                    if (!rollbackResult.getCode().equals(200) || !Boolean.TRUE.equals(rollbackResult.getData())) {
                        log.error("库存回滚失败，票档ID: {}, 数量: {}, 错误: {}", 
                            createOrderDTO.getTicketId(), createOrderDTO.getQuantity(), rollbackResult.getMessage());
                        // 发送告警或记录到补偿队列
                    } else {
                        log.info("库存回滚成功，票档ID: {}, 数量: {}", 
                            createOrderDTO.getTicketId(), createOrderDTO.getQuantity());
                    }
                } catch (Exception rollbackEx) {
                    log.error("库存回滚异常，票档ID: {}, 数量: {}", 
                        createOrderDTO.getTicketId(), createOrderDTO.getQuantity(), rollbackEx);
                    // 启动异步补偿
                    compensationService.asyncCompensateStockRollback(
                        createOrderDTO.getTicketId(), createOrderDTO.getQuantity(), tempOrderNo)
                        .whenComplete((result, ex) -> {
                            if (ex != null || !Boolean.TRUE.equals(result)) {
                                compensationService.recordCompensationFailure(tempOrderNo, 
                                    createOrderDTO.getTicketId(), createOrderDTO.getQuantity(), 
                                    "ROLLBACK_AFTER_SHOW_FETCH_FAILED", 
                                    ex != null ? ex.getMessage() : "异步补偿失败");
                            }
                        });
                }
                throw new BusinessException("获取演出信息失败: " + showInfoResult.getMessage());
            }
            
            // 创建订单
            Order order = new Order();
            order.setOrderNo(tempOrderNo);
            order.setUserId(createOrderDTO.getUserId());
            order.setShowId(createOrderDTO.getShowId());
            order.setSessionId(createOrderDTO.getSessionId());
            order.setTicketId(createOrderDTO.getTicketId());
            order.setQuantity(createOrderDTO.getQuantity());
            
            BigDecimal totalAmount;
            if (createOrderDTO.getTotalPrice() != null) {
                // 使用前端传递的价格（包含夜间加价等复杂计算）
                totalAmount = createOrderDTO.getTotalPrice();
                log.info("使用前端传递的价格: {}, 基础价格: {}, 夜间加价: {}", 
                    totalAmount, createOrderDTO.getBasePrice(), createOrderDTO.getNightSurcharge());
            } else {
                // 回退到简单的价格计算
                totalAmount = price.multiply(new BigDecimal(createOrderDTO.getQuantity()));
                log.info("使用简单价格计算: {} * {} = {}", price, createOrderDTO.getQuantity(), totalAmount);
            }
            
            order.setTotalAmount(totalAmount);
            order.setPayAmount(totalAmount); // 实际支付金额等于总金额（暂无优惠）
            order.setDiscountAmount(BigDecimal.ZERO); // 优惠金额为0
            order.setStatus(0); // 待支付
            order.setExpireTime(LocalDateTime.now().plusMinutes(15)); // 15分钟内支付
            
            // 网咖预约场景的新字段
            if (createOrderDTO.getRemark() != null) {
                order.setRemark(createOrderDTO.getRemark());
            }
            if (createOrderDTO.getContactPhone() != null) {
                order.setContactPhone(createOrderDTO.getContactPhone());
            }
            if (createOrderDTO.getBookingDate() != null) {
                // 将字符串转换为LocalDateTime
                try {
                    order.setBookingDate(LocalDateTime.parse(createOrderDTO.getBookingDate().replace(" ", "T")));
                } catch (Exception e) {
                    log.warn("预约时间格式转换失败: {}", createOrderDTO.getBookingDate(), e);
                }
            }
            if (createOrderDTO.getBookingEndTime() != null) {
                try {
                    order.setBookingEndTime(LocalDateTime.parse(createOrderDTO.getBookingEndTime().replace(" ", "T")));
                } catch (Exception e) {
                    log.warn("预约结束时间格式转换失败: {}", createOrderDTO.getBookingEndTime(), e);
                }
            }
            if (createOrderDTO.getBookingDuration() != null) {
                order.setBookingDuration(createOrderDTO.getBookingDuration());
            }
            
            // 处理座位信息（如果前端传递了座位ID）
            if (createOrderDTO.getSeatId() != null) {
                order.setSeatId(createOrderDTO.getSeatId());
                
                // 从演出服务获取座位信息
                try {
                    Result<String> seatInfoResult = showFeignClient.getSeatInfo(createOrderDTO.getSeatId());
                    if (seatInfoResult != null && seatInfoResult.getCode().equals(200) && seatInfoResult.getData() != null) {
                        order.setSeatInfo(seatInfoResult.getData());
                        log.info("获取座位信息成功，座位ID: {}, 座位信息: {}", createOrderDTO.getSeatId(), seatInfoResult.getData());
                    } else {
                        // 如果获取座位信息失败，使用默认格式
                        order.setSeatInfo("座位" + createOrderDTO.getSeatId());
                        log.warn("获取座位信息失败，使用默认格式，座位ID: {}", createOrderDTO.getSeatId());
                    }
                } catch (Exception e) {
                    log.warn("获取座位信息异常，座位ID: {}", createOrderDTO.getSeatId(), e);
                    order.setSeatInfo("座位" + createOrderDTO.getSeatId());
                }
                
                // 调用演出服务锁定座位，确保lock_user_id正确写入
                try {
                    Result<Boolean> lockResult = showFeignClient.lockSeat(createOrderDTO.getSeatId(), createOrderDTO.getUserId(), createOrderDTO.getSessionId());
                    if (lockResult == null || !lockResult.getCode().equals(200) || !Boolean.TRUE.equals(lockResult.getData())) {
                        log.error("座位锁定失败，座位ID: {}, 用户ID: {}, 场次ID: {}, 错误信息: {}", 
                            createOrderDTO.getSeatId(), createOrderDTO.getUserId(), createOrderDTO.getSessionId(),
                            lockResult != null ? lockResult.getMessage() : "锁定服务调用失败");
                        throw new BusinessException("座位锁定失败，请重新选择座位");
                    }
                    log.info("座位锁定成功，座位ID: {}, 用户ID: {}, 场次ID: {}", createOrderDTO.getSeatId(), createOrderDTO.getUserId(), createOrderDTO.getSessionId());
                } catch (Exception e) {
                    log.error("座位锁定异常，座位ID: {}, 用户ID: {}, 场次ID: {}", createOrderDTO.getSeatId(), createOrderDTO.getUserId(), createOrderDTO.getSessionId(), e);
                    throw new BusinessException("座位锁定失败: " + e.getMessage());
                }
            }
            
            // 保存订单
            save(order);
            
            return order.getOrderNo();
            
        } catch (Exception e) {
            log.error("创建订单失败，用户ID: {}", createOrderDTO.getUserId(), e);
            throw e;
        } finally {
            // 释放Redisson分布式锁
            releaseLockWithRedisson(lockKey);
        }
    }

    @Override
    @PerformanceMonitor(value = "payOrder", slowQueryThreshold = 3000)
    // @GlobalTransactional // 暂时注释
    public boolean payOrder(String orderNo, Integer payType) {
        // 分布式锁，防止重复支付
        String lockKey = ORDER_LOCK_PREFIX + "pay:" + orderNo;
        
        try {
            // 尝试获取Redisson分布式锁
            if (!tryLockWithRedisson(lockKey, LOCK_EXPIRE_TIME)) {
                throw new BusinessException("支付处理中，请稍后再试");
            }
            
            // 查询订单
            LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Order::getOrderNo, orderNo);
            Order order = getOne(queryWrapper);
        
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        if (order.getStatus() == 1) { // 订单已支付
                log.info("订单已支付，无需重复处理，订单号: {}", orderNo);
                return true;
            }
            if (order.getStatus() != 0) { // 订单状态不正确
                log.warn("订单状态不正确，无法支付，订单号: {}, 当前状态: {}", orderNo, order.getStatus());
                throw new BusinessException("订单状态不正确，无法支付");
            }
        
        if (order.getExpireTime().isBefore(LocalDateTime.now())) {
                throw new BusinessException("订单已过期");
            }
        
        // 模拟支付流程
        String payNo = UUID.randomUUID().toString().replace("-", "");
        
        // 更新订单状态
        order.setStatus(1); // 已支付
        order.setPayTime(LocalDateTime.now());
        order.setPayType(payType);
        order.setPayNo(payNo);
        
        boolean updated = updateById(order);
        
        if (updated) {
            // 支付成功，需要确认数据库库存（乐观锁）
            try {
                Result<Boolean> confirmResult = showFeignClient.confirmStockFromDatabase(
                    order.getTicketId(), order.getQuantity());
                
                if (!confirmResult.getCode().equals(200) || !Boolean.TRUE.equals(confirmResult.getData())) {
                    log.error("数据库库存确认失败，订单号: {}, 票档ID: {}, 数量: {}, 错误: {}", 
                        orderNo, order.getTicketId(), order.getQuantity(), confirmResult.getMessage());
                    
                    // 数据库确认失败，需要回滚Redis库存
                    try {
                        Result<Boolean> rollbackResult = showFeignClient.rollbackStockToRedis(
                            order.getTicketId(), order.getQuantity());
                        if (!rollbackResult.getCode().equals(200) || !Boolean.TRUE.equals(rollbackResult.getData())) {
                            log.error("回滚Redis库存失败，订单号: {}, 票档ID: {}, 数量: {}", 
                                orderNo, order.getTicketId(), order.getQuantity());
                        } else {
                            log.info("回滚Redis库存成功，订单号: {}, 票档ID: {}, 数量: {}", 
                                orderNo, order.getTicketId(), order.getQuantity());
                        }
                    } catch (Exception rollbackEx) {
                        log.error("回滚Redis库存异常，订单号: {}, 票档ID: {}, 数量: {}", 
                            orderNo, order.getTicketId(), order.getQuantity(), rollbackEx);
                    }
                    
                    // 回滚订单状态
                    order.setStatus(0); // 恢复为待支付状态
                    updateById(order);
                    
                    throw new BusinessException("库存确认失败，订单已回滚");
                }
                
                log.info("订单支付成功，数据库库存已确认扣减，订单号：{}", orderNo);
                
                // 同步库存到Redis确保一致性
                try {
                    showFeignClient.syncStockToRedis(order.getTicketId());
                    log.debug("库存同步到Redis完成，票档ID: {}", order.getTicketId());
                } catch (Exception syncEx) {
                    log.warn("库存同步到Redis失败，票档ID: {}, 错误: {}", order.getTicketId(), syncEx.getMessage());
                    // 同步失败不影响主流程，只记录警告
                }
                
            } catch (Exception confirmEx) {
                log.error("数据库库存确认异常，订单号: {}, 票档ID: {}, 数量: {}", 
                    orderNo, order.getTicketId(), order.getQuantity(), confirmEx);
                
                // 回滚Redis库存
                try {
                    showFeignClient.rollbackStockToRedis(order.getTicketId(), order.getQuantity());
                } catch (Exception rollbackEx) {
                    log.error("回滚Redis库存异常，订单号: {}, 票档ID: {}, 数量: {}", 
                        orderNo, order.getTicketId(), order.getQuantity(), rollbackEx);
                }
                
                // 回滚订单状态
                order.setStatus(0);
                updateById(order);
                
                throw new BusinessException("库存确认异常: " + confirmEx.getMessage());
            }
        }
        
            return updated;
            
        } catch (Exception e) {
            log.error("支付订单失败，订单号: {}", orderNo, e);
            throw e;
        } finally {
            // 释放Redisson分布式锁
            releaseLockWithRedisson(lockKey);
        }
    }

    @Override
    @PerformanceMonitor(value = "cancelOrder", slowQueryThreshold = 1000)
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(String orderNo, Long userId) {
        // 查询订单
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getOrderNo, orderNo)
                .eq(Order::getUserId, userId);
        Order order = getOne(queryWrapper);
        
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态不正确，只能取消待支付订单");
        }
        
        // 更新订单状态
        order.setStatus(2); // 已取消
        boolean updated = updateById(order);
        
        if (updated) {
            // Redis库存回滚
            try {
                Result<Boolean> rollbackResult = showFeignClient.rollbackStockToRedis(
                    order.getTicketId(), order.getQuantity());
                if (!rollbackResult.getCode().equals(200) || !Boolean.TRUE.equals(rollbackResult.getData())) {
                    log.error("订单取消后库存回滚失败，订单号: {}, 票档ID: {}, 数量: {}, 错误: {}", 
                        orderNo, order.getTicketId(), order.getQuantity(), rollbackResult.getMessage());
                    // 虽然回滚失败，但订单已取消，记录异常供后续补偿处理
                } else {
                    log.info("订单取消库存回滚成功，订单号: {}, 票档ID: {}, 数量: {}", 
                        orderNo, order.getTicketId(), order.getQuantity());
                }
            } catch (Exception rollbackEx) {
                log.error("订单取消库存回滚异常，订单号: {}, 票档ID: {}, 数量: {}", 
                    orderNo, order.getTicketId(), order.getQuantity(), rollbackEx);
                // 启动异步补偿
                compensationService.asyncCompensateStockRollback(
                    order.getTicketId(), order.getQuantity(), orderNo)
                    .whenComplete((result, ex) -> {
                        if (ex != null || !Boolean.TRUE.equals(result)) {
                            compensationService.recordCompensationFailure(orderNo, 
                                order.getTicketId(), order.getQuantity(), 
                                "ROLLBACK_AFTER_ORDER_CANCEL", 
                                ex != null ? ex.getMessage() : "异步补偿失败");
                        }
                    });
            }
        }
        
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(String orderNo) {
        // 查询订单（系统自动取消，不需要验证用户ID）
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getOrderNo, orderNo);
        Order order = getOne(queryWrapper);
        
        if (order == null) {
            log.warn("订单不存在，订单号：{}", orderNo);
            return false;
        }
        
        if (order.getStatus() != 0) {
            log.warn("订单状态不正确，只能取消待支付订单，订单号：{}，当前状态：{}", orderNo, order.getStatus());
            return false;
        }
        
        // 更新订单状态
        order.setStatus(2); // 已取消
        boolean updated = updateById(order);
        
        if (updated) {
            // Redis库存回滚
            try {
                Result<Boolean> rollbackResult = showFeignClient.rollbackStockToRedis(
                    order.getTicketId(), order.getQuantity());
                if (!rollbackResult.getCode().equals(200) || !Boolean.TRUE.equals(rollbackResult.getData())) {
                    log.error("系统取消订单后库存回滚失败，订单号: {}, 票档ID: {}, 数量: {}, 错误: {}", 
                        orderNo, order.getTicketId(), order.getQuantity(), rollbackResult.getMessage());
                    // 虽然回滚失败，但订单已取消，记录异常供后续补偿处理
                } else {
                    log.info("系统取消订单库存回滚成功，订单号: {}, 票档ID: {}, 数量: {}", 
                        orderNo, order.getTicketId(), order.getQuantity());
                }
            } catch (Exception rollbackEx) {
                log.error("系统取消订单库存回滚异常，订单号: {}, 票档ID: {}, 数量: {}", 
                    orderNo, order.getTicketId(), order.getQuantity(), rollbackEx);
                // 启动异步补偿
                compensationService.asyncCompensateStockRollback(
                    order.getTicketId(), order.getQuantity(), orderNo)
                    .whenComplete((result, ex) -> {
                        if (ex != null || !Boolean.TRUE.equals(result)) {
                            compensationService.recordCompensationFailure(orderNo, 
                                order.getTicketId(), order.getQuantity(), 
                                "ROLLBACK_AFTER_SYSTEM_CANCEL", 
                                ex != null ? ex.getMessage() : "异步补偿失败");
                        }
                    });
            }
        }

        return updated;
    }

    @Override
    public OrderVO getOrderDetail(String orderNo, Long userId) {
        // 查询订单
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getOrderNo, orderNo)
                .eq(Order::getUserId, userId);
        Order order = getOne(queryWrapper);
        
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        // 获取演出信息
        Result<ShowInfoDTO> showInfoResult = showFeignClient.getShowInfo(
                order.getShowId(), order.getSessionId());
        
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);
        
        // 设置状态名称和支付方式名称
        orderVO.setStatusName(ORDER_STATUS_MAP.getOrDefault(order.getStatus(), "未知"));
        if (order.getPayType() != null) {
            orderVO.setPayTypeName(PAY_TYPE_MAP.getOrDefault(order.getPayType(), "未知"));
        }
        
        // 设置演出信息
        if (showInfoResult.getCode().equals(200)) {
            ShowInfoDTO showInfo = showInfoResult.getData();
            orderVO.setShowName(showInfo.getShowName());
            orderVO.setSessionName(showInfo.getSessionName());
            orderVO.setShowTime(showInfo.getShowTime());
            orderVO.setVenue(showInfo.getVenue());
        }
        
        return orderVO;
    }

    @Override
    public OrderVO getOrderDetailById(Long id, Long userId) {
        // 查询订单
        Order order = getById(id);
        
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权限访问该订单");
        }
        
        // 转换为VO（复用pageUserOrders中的逻辑）
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);
        
        // 设置状态名称和支付方式名称
        orderVO.setStatusName(ORDER_STATUS_MAP.getOrDefault(order.getStatus(), "未知"));
        if (order.getPayType() != null) {
            orderVO.setPayTypeName(PAY_TYPE_MAP.getOrDefault(order.getPayType(), "未知"));
        }
        
        // 从演出服务获取真实的演出信息
        if (order.getShowId() != null && order.getSessionId() != null) {
            try {
                Result<ShowInfoDTO> showInfoResult = showFeignClient.getShowInfo(
                        order.getShowId(), order.getSessionId());
                
                if (showInfoResult != null && showInfoResult.getCode().equals(200) && showInfoResult.getData() != null) {
                    ShowInfoDTO showInfo = showInfoResult.getData();
                    orderVO.setShowName(showInfo.getShowName());
                    orderVO.setSessionName(showInfo.getSessionName());
                    orderVO.setShowTime(showInfo.getShowTime());
                    orderVO.setVenue(showInfo.getVenue());
                } else {
                    // Feign调用失败的fallback处理
                    orderVO.setShowName("演出信息获取失败");
                    orderVO.setVenue("--");
                    orderVO.setShowTime(null);
                }
            } catch (Exception e) {
                log.error("获取演出信息失败，showId: {}, sessionId: {}", order.getShowId(), order.getSessionId(), e);
                // 异常情况的fallback处理
                orderVO.setShowName("演出信息获取异常");
                orderVO.setVenue("--");
                orderVO.setShowTime(null);
            }
        } else {
            // 如果订单没有演出ID，使用默认值
            orderVO.setShowName("演出信息缺失");
            orderVO.setVenue("--");
            orderVO.setShowTime(null);
        }
        
        return orderVO;
    }

    @Override
    public Page<OrderVO> pageUserOrders(Long userId, Integer status, Integer page, Integer size) {
        // 查询订单
        Page<Order> orderPage = new Page<>(page, size);
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getUserId, userId);
        
        if (status != null) {
            queryWrapper.eq(Order::getStatus, status);
        }
        
        queryWrapper.orderByDesc(Order::getCreateTime);
        
        Page<Order> result = page(orderPage, queryWrapper);
        
        // 转换为VO
        Page<OrderVO> voPage = new Page<>();
        BeanUtils.copyProperties(result, voPage, "records");
        
        // 获取演出信息
        List<OrderVO> voList = result.getRecords().stream().map(order -> {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order, orderVO);
            
            // 设置状态名称和支付方式名称
            orderVO.setStatusName(ORDER_STATUS_MAP.getOrDefault(order.getStatus(), "未知"));
            if (order.getPayType() != null) {
                orderVO.setPayTypeName(PAY_TYPE_MAP.getOrDefault(order.getPayType(), "未知"));
            }
            
            // 从演出服务获取真实的演出信息
            if (order.getShowId() != null && order.getSessionId() != null) {
                try {
                    Result<ShowInfoDTO> showInfoResult = showFeignClient.getShowInfo(
                            order.getShowId(), order.getSessionId());
                    
                    if (showInfoResult != null && showInfoResult.getCode().equals(200) && showInfoResult.getData() != null) {
                        ShowInfoDTO showInfo = showInfoResult.getData();
                        orderVO.setShowName(showInfo.getShowName());
                        orderVO.setSessionName(showInfo.getSessionName());
                        orderVO.setShowTime(showInfo.getShowTime());
                        orderVO.setVenue(showInfo.getVenue());
                    } else {
                        // Feign调用失败的fallback处理
                        orderVO.setShowName("演出信息获取失败");
                        orderVO.setVenue("--");
                        orderVO.setShowTime(null);
                    }
                } catch (Exception e) {
                    log.error("获取演出信息失败，showId: {}, sessionId: {}", order.getShowId(), order.getSessionId(), e);
                    // 异常情况的fallback处理
                    orderVO.setShowName("演出信息获取异常");
                    orderVO.setVenue("--");
                    orderVO.setShowTime(null);
                }
            } else {
                // 如果订单没有演出ID，使用默认值
                orderVO.setShowName("演出信息缺失");
                orderVO.setVenue("--");
                orderVO.setShowTime(null);
            }
            
            return orderVO;
        }).collect(Collectors.toList());
        
        voPage.setRecords(voList);
        return voPage;
    }
    
    /**
     * 票价获取：带一次重试，并打印 show 侧返回详情（用于快速联调定位）
     */
    private BigDecimal fetchTicketPriceWithRetry(Long ticketId) {
        for (int i = 0; i < 2; i++) {
            Result<BigDecimal> priceResult = showFeignClient.getTicketPrice(ticketId);
            if (priceResult != null && Integer.valueOf(200).equals(priceResult.getCode()) && priceResult.getData() != null) {
                return priceResult.getData();
            }
            log.warn("票价获取失败(第{}次)，ticketId={}, code={}, message={}, data={}",
                    i + 1,
                    ticketId,
                    priceResult != null ? priceResult.getCode() : null,
                    priceResult != null ? priceResult.getMessage() : null,
                    priceResult != null ? priceResult.getData() : null);
            try {
                Thread.sleep(50L * (i + 1));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        return null;
    }

    /**
     * 生成订单号
     */
        /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return String.valueOf(snowflakeIdWorker.nextId());
    }
    
    /**
     * 使用Redisson获取分布式锁
     */
    private boolean tryLockWithRedisson(String lockKey, int expireTime) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(0, expireTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("获取分布式锁被中断，lockKey: {}", lockKey, e);
            return false;
        }
    }
    
    /**
     * 带重试机制的分布式锁获取
     */
    private boolean tryLockWithRedissonRetry(String lockKey, int waitTime, int expireTime, int maxRetryTimes) {
        RLock lock = redissonClient.getLock(lockKey);
        
        for (int i = 0; i < maxRetryTimes; i++) {
            try {
                boolean acquired = lock.tryLock(waitTime, expireTime, TimeUnit.SECONDS);
                if (acquired) {
                    log.debug("成功获取分布式锁，lockKey: {}, 重试次数: {}", lockKey, i);
                    return true;
                }
                
                if (i < maxRetryTimes - 1) {
                    log.debug("获取分布式锁失败，准备重试，lockKey: {}, 当前重试次数: {}", lockKey, i + 1);
                    Thread.sleep(100 * (i + 1)); // 递增等待时间
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("获取分布式锁被中断，lockKey: {}, 重试次数: {}", lockKey, i, e);
                return false;
            } catch (Exception e) {
                log.warn("获取分布式锁异常，lockKey: {}, 重试次数: {}", lockKey, i, e);
                if (i < maxRetryTimes - 1) {
                    try {
                        Thread.sleep(200 * (i + 1)); // 异常情况下等待更长时间
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }
        
        log.warn("获取分布式锁最终失败，lockKey: {}, 最大重试次数: {}", lockKey, maxRetryTimes);
        return false;
    }
    
    /**
     * 使用Redisson释放分布式锁
     */
    private void releaseLockWithRedisson(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (Exception e) {
            log.warn("释放分布式锁失败，lockKey: {}", lockKey, e);
        }
    }
}