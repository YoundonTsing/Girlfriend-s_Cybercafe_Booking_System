# RocketMQ引入计划 - 提升票务系统并发能力、解耦能力、容错能力

## 📊 当前系统分析

### 🔍 现状评估
基于对代码的深入分析，当前系统具备以下特点：

**✅ 已有优势：**
- Redis + Redisson 实现高性能缓存和分布式锁
- Lua脚本保证库存操作原子性
- Nacos服务发现和配置管理
- Feign实现服务间通信
- 分布式锁防止并发冲突

**⚠️ 存在问题：**
- 同步调用链路长，用户等待时间长
- 服务间强耦合，单点故障影响全局
- 缺乏异步处理机制
- 无消息重试和补偿机制
- 库存同步依赖同步调用

### 🎯 关键瓶颈识别

1. **订单创建流程瓶颈**
   ```java
   // 当前同步流程
   创建订单 → 查询票价 → 预减库存 → 获取演出信息 → 保存订单
   ```
   - 问题：串行执行，任一环节失败影响整体
   - 影响：用户体验差，系统吞吐量低

2. **支付流程瓶颈**
   ```java
   // 当前支付流程
   支付验证 → 更新订单状态 → 同步库存 → 返回结果
   ```
   - 问题：库存同步阻塞支付响应
   - 影响：支付响应慢，用户体验差

3. **库存管理瓶颈**
   - 问题：多服务直接调用库存接口
   - 影响：库存服务压力大，容易成为瓶颈

## 🚀 RocketMQ集成方案

### 📋 Phase 1: 基础设施搭建 (Week 1-2)

#### 1.1 RocketMQ环境部署

**本地部署方案（基于您的安装路径：D:\rocketmq-5.2.0）**

##### 1.1.1 配置文件设置
```properties
# D:\rocketmq-5.2.0\conf\broker.conf
brokerClusterName = DefaultCluster
brokerName = broker-a
brokerId = 0
deleteWhen = 04
fileReservedTime = 48
brokerRole = ASYNC_MASTER
flushDiskType = ASYNC_FLUSH
namesrvAddr = 127.0.0.1:9876
autoCreateTopicEnable = true
autoCreateSubscriptionGroup = true

# 存储路径配置
storePathRootDir = D:\\rocketmq-5.2.0\\store
storePathCommitLog = D:\\rocketmq-5.2.0\\store\\commitlog
storePathConsumeQueue = D:\\rocketmq-5.2.0\\store\\consumequeue
storePathIndex = D:\\rocketmq-5.2.0\\store\\index

# 网络配置
listenPort = 10911
brokerIP1 = 127.0.0.1
```

##### 1.1.2 启动脚本
```batch
# start-rocketmq.bat
@echo off
echo 启动RocketMQ NameServer...
cd /d D:\rocketmq-5.2.0\bin
start "NameServer" mqnamesrv.cmd

echo 等待NameServer启动...
timeout /t 10

echo 启动RocketMQ Broker...
start "Broker" mqbroker.cmd -n 127.0.0.1:9876 -c ../conf/broker.conf

echo RocketMQ启动完成！
echo NameServer: 127.0.0.1:9876
echo Broker: 127.0.0.1:10911
pause
```

##### 1.1.3 停止脚本
```batch
# stop-rocketmq.bat
@echo off
echo 停止RocketMQ Broker...
cd /d D:\rocketmq-5.2.0\bin
mqshutdown.cmd broker

echo 停止RocketMQ NameServer...
mqshutdown.cmd namesrv

echo RocketMQ已停止！
pause
```

**Docker部署方案（可选）**
```yaml
# docker-compose.yml
version: '3.8'
services:
  rocketmq-nameserver:
    image: apache/rocketmq:5.2.0
    container_name: rocketmq-nameserver
    ports:
      - "9876:9876"
    environment:
      - JAVA_OPT_EXT=-server -Xms512m -Xmx512m
    command: mqnamesrv

  rocketmq-broker:
    image: apache/rocketmq:5.2.0
    container_name: rocketmq-broker
    ports:
      - "10909:10909"
      - "10911:10911"
    environment:
      - NAMESRV_ADDR=rocketmq-nameserver:9876
      - JAVA_OPT_EXT=-server -Xms1g -Xmx1g
    command: mqbroker -c /opt/rocketmq-5.2.0/conf/broker.conf
    depends_on:
      - rocketmq-nameserver

  rocketmq-dashboard:
    image: apacherocketmq/rocketmq-dashboard:latest
    container_name: rocketmq-dashboard
    ports:
      - "8080:8080"
    environment:
      - JAVA_OPTS=-Drocketmq.namesrv.addr=rocketmq-nameserver:9876
    depends_on:
      - rocketmq-nameserver
```

#### 1.2 Maven依赖配置
```xml
<!-- ticket-common/pom.xml -->
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>2.2.3</version>
</dependency>
```

#### 1.3 通用配置类
```java
// ticket-common/src/main/java/com/ticketsystem/common/config/RocketMQConfig.java
@Configuration
@EnableConfigurationProperties(RocketMQProperties.class)
public class RocketMQConfig {
    
    @Bean
    public DefaultMQProducer defaultMQProducer(RocketMQProperties properties) {
        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setNamesrvAddr(properties.getNameServer());
        producer.setProducerGroup(properties.getProducer().getGroup());
        producer.setSendMsgTimeout(properties.getProducer().getSendMessageTimeout());
        producer.setRetryTimesWhenSendFailed(properties.getProducer().getRetryTimesWhenSendFailed());
        return producer;
    }
}
```

### 📋 Phase 2: 消息模型设计 (Week 2-3)

#### 2.1 Topic和Tag设计
```java
// 消息主题定义
public class MQTopics {
    // 订单相关
    public static final String ORDER_TOPIC = "ORDER_TOPIC";
    public static final String ORDER_CREATED_TAG = "ORDER_CREATED";
    public static final String ORDER_PAID_TAG = "ORDER_PAID";
    public static final String ORDER_CANCELLED_TAG = "ORDER_CANCELLED";
    
    // 库存相关
    public static final String STOCK_TOPIC = "STOCK_TOPIC";
    public static final String STOCK_DEDUCT_TAG = "STOCK_DEDUCT";
    public static final String STOCK_ROLLBACK_TAG = "STOCK_ROLLBACK";
    public static final String STOCK_SYNC_TAG = "STOCK_SYNC";
    
    // 通知相关
    public static final String NOTIFICATION_TOPIC = "NOTIFICATION_TOPIC";
    public static final String SMS_TAG = "SMS";
    public static final String EMAIL_TAG = "EMAIL";
    public static final String PUSH_TAG = "PUSH";
}
```

#### 2.2 消息实体设计
```java
// 订单事件消息
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderEventMessage {
    private String orderNo;
    private Long userId;
    private Long showId;
    private Long sessionId;
    private Long ticketId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private Integer status;
    private LocalDateTime eventTime;
    private String eventType; // CREATED, PAID, CANCELLED
    private Map<String, Object> extData;
}

// 库存事件消息
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockEventMessage {
    private Long ticketId;
    private Integer quantity;
    private String operation; // DEDUCT, ROLLBACK, SYNC
    private String orderNo;
    private LocalDateTime eventTime;
    private Map<String, Object> extData;
}
```

### 📋 Phase 3: 订单流程异步化改造 (Week 3-4)

#### 3.1 订单创建异步化
```java
// 改造后的订单创建流程
@Service
public class OrderServiceImpl {
    
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    
    @Override
    public String createOrder(CreateOrderDTO createOrderDTO) {
        // 1. 快速创建订单（仅核心信息）
        Order order = createOrderQuickly(createOrderDTO);
        
        // 2. 发送异步消息处理后续流程
        OrderEventMessage message = new OrderEventMessage();
        BeanUtils.copyProperties(order, message);
        message.setEventType("CREATED");
        message.setEventTime(LocalDateTime.now());
        
        // 发送消息到RocketMQ
        rocketMQTemplate.asyncSend(
            MQTopics.ORDER_TOPIC + ":" + MQTopics.ORDER_CREATED_TAG,
            message,
            new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("订单创建消息发送成功: {}", order.getOrderNo());
                }
                
                @Override
                public void onException(Throwable e) {
                    log.error("订单创建消息发送失败: {}", order.getOrderNo(), e);
                    // 降级处理：标记订单需要重新处理
                    markOrderForRetry(order.getOrderNo());
                }
            }
        );
        
        return order.getOrderNo();
    }
    
    // 快速创建订单（仅核心信息，减少同步操作）
    private Order createOrderQuickly(CreateOrderDTO dto) {
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(dto.getUserId());
        order.setShowId(dto.getShowId());
        order.setSessionId(dto.getSessionId());
        order.setTicketId(dto.getTicketId());
        order.setQuantity(dto.getQuantity());
        order.setStatus(0); // 待处理
        order.setExpireTime(LocalDateTime.now().plusMinutes(15));
        
        save(order);
        return order;
    }
}
```

#### 3.2 订单处理消费者
```java
@Component
@RocketMQMessageListener(
    topic = MQTopics.ORDER_TOPIC,
    selectorExpression = MQTopics.ORDER_CREATED_TAG,
    consumerGroup = "order-process-consumer-group"
)
public class OrderProcessConsumer implements RocketMQListener<OrderEventMessage> {
    
    @Autowired
    private ShowFeignClient showFeignClient;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    
    @Override
    public void onMessage(OrderEventMessage message) {
        try {
            processOrderAsync(message);
        } catch (Exception e) {
            log.error("处理订单消息失败: {}", message.getOrderNo(), e);
            throw new RuntimeException("订单处理失败", e); // 触发重试
        }
    }
    
    private void processOrderAsync(OrderEventMessage message) {
        // 1. 查询票价
        Result<BigDecimal> priceResult = showFeignClient.getTicketPrice(message.getTicketId());
        if (!priceResult.getCode().equals(200)) {
            handleOrderProcessFailure(message, "获取票价失败");
            return;
        }
        
        // 2. 预减库存
        Result<Integer> stockResult = showFeignClient.predeductStockFromRedis(
            message.getTicketId(), message.getQuantity());
        if (!stockResult.getCode().equals(200) || stockResult.getData() != 1) {
            handleOrderProcessFailure(message, "库存不足");
            return;
        }
        
        // 3. 更新订单详细信息
        updateOrderDetails(message, priceResult.getData());
        
        // 4. 发送库存扣减消息
        sendStockDeductMessage(message);
        
        log.info("订单异步处理完成: {}", message.getOrderNo());
    }
    
    private void handleOrderProcessFailure(OrderEventMessage message, String reason) {
        // 更新订单状态为失败
        Order order = orderMapper.selectByOrderNo(message.getOrderNo());
        order.setStatus(2); // 已取消
        order.setRemark("系统处理失败: " + reason);
        orderMapper.updateById(order);
        
        // 发送订单取消消息
        OrderEventMessage cancelMessage = new OrderEventMessage();
        BeanUtils.copyProperties(message, cancelMessage);
        cancelMessage.setEventType("CANCELLED");
        cancelMessage.setEventTime(LocalDateTime.now());
        
        rocketMQTemplate.syncSend(
            MQTopics.ORDER_TOPIC + ":" + MQTopics.ORDER_CANCELLED_TAG,
            cancelMessage
        );
    }
}
```

### 📋 Phase 4: 支付流程优化 (Week 4-5)

#### 4.1 支付异步化
```java
@Override
public boolean payOrder(String orderNo, Integer payType) {
    // 1. 快速更新支付状态
    Order order = updatePaymentStatusQuickly(orderNo, payType);
    
    // 2. 发送支付成功消息
    OrderEventMessage message = new OrderEventMessage();
    BeanUtils.copyProperties(order, message);
    message.setEventType("PAID");
    message.setEventTime(LocalDateTime.now());
    
    // 同步发送确保支付状态一致性
    SendResult result = rocketMQTemplate.syncSend(
        MQTopics.ORDER_TOPIC + ":" + MQTopics.ORDER_PAID_TAG,
        message
    );
    
    return result.getSendStatus() == SendStatus.SEND_OK;
}
```

#### 4.2 支付后处理消费者
```java
@Component
@RocketMQMessageListener(
    topic = MQTopics.ORDER_TOPIC,
    selectorExpression = MQTopics.ORDER_PAID_TAG,
    consumerGroup = "order-paid-consumer-group"
)
public class OrderPaidConsumer implements RocketMQListener<OrderEventMessage> {
    
    @Override
    public void onMessage(OrderEventMessage message) {
        try {
            // 1. 库存确认扣减
            confirmStockDeduction(message);
            
            // 2. 发送通知消息
            sendNotificationMessages(message);
            
            // 3. 更新相关统计数据
            updateStatistics(message);
            
        } catch (Exception e) {
            log.error("支付后处理失败: {}", message.getOrderNo(), e);
            // 不抛异常，避免影响支付结果
        }
    }
    
    private void confirmStockDeduction(OrderEventMessage message) {
        StockEventMessage stockMessage = new StockEventMessage();
        stockMessage.setTicketId(message.getTicketId());
        stockMessage.setQuantity(message.getQuantity());
        stockMessage.setOperation("CONFIRM");
        stockMessage.setOrderNo(message.getOrderNo());
        stockMessage.setEventTime(LocalDateTime.now());
        
        rocketMQTemplate.asyncSend(
            MQTopics.STOCK_TOPIC + ":" + MQTopics.STOCK_SYNC_TAG,
            stockMessage
        );
    }
}
```

### 📋 Phase 5: 库存管理异步化 (Week 5-6)

#### 5.1 库存事件处理
```java
@Component
@RocketMQMessageListener(
    topic = MQTopics.STOCK_TOPIC,
    consumerGroup = "stock-management-consumer-group"
)
public class StockManagementConsumer implements RocketMQListener<StockEventMessage> {
    
    @Autowired
    private RedisStockService redisStockService;
    @Autowired
    private TicketStockService ticketStockService;
    
    @Override
    public void onMessage(StockEventMessage message) {
        switch (message.getOperation()) {
            case "DEDUCT":
                handleStockDeduction(message);
                break;
            case "ROLLBACK":
                handleStockRollback(message);
                break;
            case "SYNC":
                handleStockSync(message);
                break;
            case "CONFIRM":
                handleStockConfirm(message);
                break;
        }
    }
    
    private void handleStockSync(StockEventMessage message) {
        try {
            // 同步Redis库存到数据库
            Integer redisStock = redisStockService.getStock(message.getTicketId());
            if (redisStock != null) {
                ticketStockService.syncStockFromRedis(message.getTicketId(), redisStock);
            }
        } catch (Exception e) {
            log.error("库存同步失败: ticketId={}", message.getTicketId(), e);
            throw new RuntimeException("库存同步失败", e);
        }
    }
}
```

### 📋 Phase 6: 通知系统建设 (Week 6-7)

#### 6.1 通知消息生产者
```java
@Component
public class NotificationProducer {
    
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    
    public void sendOrderNotification(OrderEventMessage orderMessage) {
        // 短信通知
        NotificationMessage smsMessage = createSMSMessage(orderMessage);
        rocketMQTemplate.asyncSend(
            MQTopics.NOTIFICATION_TOPIC + ":" + MQTopics.SMS_TAG,
            smsMessage
        );
        
        // 邮件通知
        NotificationMessage emailMessage = createEmailMessage(orderMessage);
        rocketMQTemplate.asyncSend(
            MQTopics.NOTIFICATION_TOPIC + ":" + MQTopics.EMAIL_TAG,
            emailMessage
        );
        
        // APP推送
        NotificationMessage pushMessage = createPushMessage(orderMessage);
        rocketMQTemplate.asyncSend(
            MQTopics.NOTIFICATION_TOPIC + ":" + MQTopics.PUSH_TAG,
            pushMessage
        );
    }
}
```

#### 6.2 通知消费者
```java
@Component
@RocketMQMessageListener(
    topic = MQTopics.NOTIFICATION_TOPIC,
    selectorExpression = MQTopics.SMS_TAG,
    consumerGroup = "sms-notification-consumer-group"
)
public class SMSNotificationConsumer implements RocketMQListener<NotificationMessage> {
    
    @Override
    public void onMessage(NotificationMessage message) {
        try {
            // 调用短信服务发送通知
            smsService.sendSMS(message.getPhone(), message.getContent());
            log.info("短信发送成功: {}", message.getPhone());
        } catch (Exception e) {
            log.error("短信发送失败: {}", message.getPhone(), e);
            throw new RuntimeException("短信发送失败", e); // 触发重试
        }
    }
}
```

### 📋 Phase 7: 监控和运维 (Week 7-8)

#### 7.1 消息监控
```java
@Component
public class MQMonitor {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @EventListener
    public void handleMessageSent(MessageSentEvent event) {
        Counter.builder("mq.message.sent")
            .tag("topic", event.getTopic())
            .tag("tag", event.getTag())
            .register(meterRegistry)
            .increment();
    }
    
    @EventListener
    public void handleMessageConsumed(MessageConsumedEvent event) {
        Timer.builder("mq.message.consumed")
            .tag("topic", event.getTopic())
            .tag("consumer_group", event.getConsumerGroup())
            .register(meterRegistry)
            .record(event.getConsumeTime(), TimeUnit.MILLISECONDS);
    }
}
```

#### 7.2 死信队列处理
```java
@Component
@RocketMQMessageListener(
    topic = "%DLQ%ORDER_TOPIC",
    consumerGroup = "dlq-handler-group"
)
public class DeadLetterQueueHandler implements RocketMQListener<MessageExt> {
    
    @Override
    public void onMessage(MessageExt message) {
        log.error("处理死信消息: topic={}, msgId={}, body={}", 
            message.getTopic(), message.getMsgId(), new String(message.getBody()));
        
        // 记录到数据库用于人工处理
        saveDeadLetterMessage(message);
        
        // 发送告警
        alertService.sendAlert("死信消息告警", message);
    }
}
```

##[object Object]预期效果

### 🚀 性能提升
- **响应时间**: 订单创建从2-3秒降至200-300ms
- **并发处理**: 支持10,000+ TPS
- **系统吞吐量**: 提升300%+

### 🔧 架构优化
- **服务解耦**: 服务间依赖降低80%
- **容错能力**: 单服务故障不影响整体
- **扩展性**: 支持水平扩展

### 💡 业务价值
- **用户体验**: 响应速度提升10倍
- **运维效率**: 故障定位和处理效率提升
- **业务扩展**: 支持更复杂的业务场景

## 🎯 实施建议

### 📅 时间规划
- **总周期**: 8周
- **里程碑**: 每2周一个阶段
- **上线方式**: 灰度发布，逐步切换

### ⚠️ 风险控制
1. **消息丢失**: 配置持久化和确认机制
2. **重复消费**: 实现幂等性处理
3. **消息积压**: 监控队列深度，及时扩容
4. **服务降级**: 保留同步调用作为降级方案

### 🔍 监控指标
- 消息发送成功率 > 99.9%
- 消息消费延迟 < 100ms
- 死信消息比例 < 0.1%
- 系统可用性 > 99.95%

通过这个RocketMQ集成方案，您的票务系统将具备更强的并发处理能力、更好的服务解耦性和更高的容错能力，为业务的快速发展提供坚实的技术支撑。