# RocketMQ集成方案设计文档

## 1. 概述

基于性能测试结果分析，票务系统需要引入RocketMQ消息队列来实现关键业务流程的异步化处理，以提升系统性能和用户体验。

## 2. 架构设计

### 2.1 整体架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Gateway       │    │   RocketMQ      │    │   Services      │
│                 │    │   NameServer    │    │                 │
│  ┌───────────┐  │    │                 │    │  ┌───────────┐  │
│  │ 路由转发   │  │    │  ┌───────────┐  │    │  │ 订单服务   │  │
│  └───────────┘  │    │  │  Broker   │  │    │  └───────────┘  │
│                 │    │  │  Cluster  │  │    │                 │
│  ┌───────────┐  │    │  └───────────┘  │    │  ┌───────────┐  │
│  │ 负载均衡   │  │◄──►│                 │◄──►│  │ 支付服务   │  │
│  └───────────┘  │    │  ┌───────────┐  │    │  └───────────┘  │
│                 │    │  │ Console   │  │    │                 │
└─────────────────┘    │  └───────────┘  │    │  ┌───────────┐  │
                       └─────────────────┘    │  │ 库存服务   │  │
                                              │  └───────────┘  │
                                              └─────────────────┘
```

### 2.2 消息流转设计

```
用户请求 → 网关 → 业务服务 → 发送消息到MQ → 返回响应
                                    ↓
                            消息消费者处理 → 更新状态 → 通知用户
```

## 3. Topic设计

### 3.1 订单相关Topic

| Topic名称 | 描述 | 生产者 | 消费者 | 消息类型 |
|-----------|------|--------|--------|---------|
| `order-create` | 订单创建 | 订单服务 | 订单处理器 | 异步订单创建 |
| `order-payment` | 订单支付 | 订单服务 | 支付处理器 | 异步支付处理 |
| `order-cancel` | 订单取消 | 订单服务 | 订单处理器 | 异步订单取消 |
| `order-status-update` | 订单状态更新 | 各服务 | 通知服务 | 状态变更通知 |

### 3.2 库存相关Topic

| Topic名称 | 描述 | 生产者 | 消费者 | 消息类型 |
|-----------|------|--------|--------|---------|
| `stock-lock` | 库存锁定 | 演出服务 | 库存处理器 | 异步库存锁定 |
| `stock-deduct` | 库存扣减 | 演出服务 | 库存处理器 | 异步库存扣减 |
| `stock-rollback` | 库存回滚 | 各服务 | 库存处理器 | 异步库存回滚 |
| `stock-sync` | 库存同步 | 演出服务 | 缓存服务 | 缓存同步 |

### 3.3 通知相关Topic

| Topic名称 | 描述 | 生产者 | 消费者 | 消息类型 |
|-----------|------|--------|--------|---------|
| `user-notification` | 用户通知 | 各服务 | 通知服务 | 用户消息推送 |
| `sms-notification` | 短信通知 | 通知服务 | 短信服务 | 短信发送 |
| `email-notification` | 邮件通知 | 通知服务 | 邮件服务 | 邮件发送 |

## 4. 消息设计

### 4.1 订单创建消息

```json
{
  "messageId": "uuid",
  "timestamp": 1640995200000,
  "eventType": "ORDER_CREATE",
  "data": {
    "orderId": "ORD202312250001",
    "userId": 12345,
    "showId": 1001,
    "ticketTypeId": 2001,
    "quantity": 2,
    "totalAmount": 299.00,
    "orderTime": 1640995200000,
    "expireTime": 1640996100000
  },
  "source": "order-service",
  "version": "1.0"
}
```

### 4.2 支付处理消息

```json
{
  "messageId": "uuid",
  "timestamp": 1640995300000,
  "eventType": "PAYMENT_PROCESS",
  "data": {
    "orderId": "ORD202312250001",
    "paymentId": "PAY202312250001",
    "paymentMethod": "ALIPAY",
    "amount": 299.00,
    "paymentTime": 1640995300000,
    "callbackUrl": "https://api.example.com/payment/callback"
  },
  "source": "order-service",
  "version": "1.0"
}
```

### 4.3 库存锁定消息

```json
{
  "messageId": "uuid",
  "timestamp": 1640995200000,
  "eventType": "STOCK_LOCK",
  "data": {
    "lockId": "LOCK202312250001",
    "orderId": "ORD202312250001",
    "ticketTypeId": 2001,
    "quantity": 2,
    "lockTime": 300,
    "expireTime": 1640995500000
  },
  "source": "show-service",
  "version": "1.0"
}
```

## 5. 实施计划

### 5.1 Phase 1: 基础设施搭建 (1-2天)

1. **RocketMQ环境部署**
   - 部署NameServer集群
   - 部署Broker集群
   - 配置Console管理界面
   - 网络和安全配置

2. **基础组件开发**
   - 消息生产者封装
   - 消息消费者封装
   - 消息序列化/反序列化
   - 异常处理和重试机制

### 5.2 Phase 2: 订单异步化 (2-3天)

1. **订单创建异步化**
   - 修改订单创建接口为异步模式
   - 实现订单创建消息生产者
   - 开发订单处理消费者
   - 添加订单状态查询接口

2. **支付处理异步化**
   - 修改支付接口为异步模式
   - 实现支付消息生产者
   - 开发支付处理消费者
   - 集成支付回调机制

### 5.3 Phase 3: 库存异步化 (2-3天)

1. **库存操作异步化**
   - 库存锁定异步化
   - 库存扣减异步化
   - 库存回滚异步化
   - 库存缓存同步

2. **分布式事务处理**
   - 实现Saga模式事务
   - 添加补偿机制
   - 事务状态监控

### 5.4 Phase 4: 通知系统 (1-2天)

1. **用户通知异步化**
   - 订单状态变更通知
   - 支付结果通知
   - 短信/邮件通知

## 6. 配置参数

### 6.1 RocketMQ配置

```yaml
rocketmq:
  name-server: 192.168.1.100:9876;192.168.1.101:9876
  producer:
    group: ticket-producer-group
    send-message-timeout: 3000
    retry-times-when-send-failed: 2
    max-message-size: 4194304
  consumer:
    group: ticket-consumer-group
    consume-thread-min: 5
    consume-thread-max: 20
    consume-message-batch-max-size: 1
```

### 6.2 Topic配置

```bash
# 创建Topic命令
sh mqadmin updateTopic -c DefaultCluster -t order-create -r 8 -w 8
sh mqadmin updateTopic -c DefaultCluster -t order-payment -r 8 -w 8
sh mqadmin updateTopic -c DefaultCluster -t stock-lock -r 8 -w 8
sh mqadmin updateTopic -c DefaultCluster -t stock-deduct -r 8 -w 8
```

## 7. 监控和运维

### 7.1 监控指标

- **消息生产监控**
  - 消息发送成功率
  - 消息发送延迟
  - 消息发送失败率

- **消息消费监控**
  - 消息消费成功率
  - 消息消费延迟
  - 消息积压数量
  - 消费者健康状态

- **系统性能监控**
  - Broker CPU/内存使用率
  - 磁盘使用率
  - 网络IO

### 7.2 告警规则

```yaml
alerts:
  - name: 消息积压告警
    condition: message_backlog > 1000
    severity: warning
    
  - name: 消费失败率告警
    condition: consume_fail_rate > 5%
    severity: critical
    
  - name: Broker不可用告警
    condition: broker_status == down
    severity: critical
```

## 8. 性能预期

### 8.1 优化目标

| 操作类型 | 当前性能 | 目标性能 | 提升幅度 |
|----------|----------|----------|---------|
| 订单创建 | 2000ms | 500ms | 75% |
| 支付处理 | 3000ms | 800ms | 73% |
| 库存锁定 | 1500ms | 400ms | 73% |
| 库存扣减 | 1200ms | 300ms | 75% |

### 8.2 吞吐量目标

- **订单处理**: 从 50 TPS 提升到 200 TPS
- **支付处理**: 从 30 TPS 提升到 150 TPS
- **库存操作**: 从 100 TPS 提升到 500 TPS

## 9. 风险评估

### 9.1 技术风险

- **消息丢失风险**: 通过持久化和确认机制降低
- **消息重复风险**: 实现幂等性处理
- **消息顺序风险**: 使用顺序消息保证关键业务顺序
- **系统复杂度**: 通过完善的监控和文档降低运维复杂度

### 9.2 业务风险

- **数据一致性**: 实现最终一致性和补偿机制
- **用户体验**: 提供实时状态查询和通知
- **系统可用性**: 实现降级和熔断机制

## 10. 回滚方案

### 10.1 快速回滚

1. **配置开关**: 实现异步/同步模式切换开关
2. **流量切换**: 通过网关配置快速切换流量
3. **数据恢复**: 保留原有同步处理逻辑作为备份

### 10.2 渐进式上线

1. **灰度发布**: 先在部分用户群体测试
2. **A/B测试**: 对比异步和同步模式的效果
3. **监控验证**: 实时监控关键指标
4. **逐步扩量**: 根据监控结果逐步扩大异步处理范围

## 11. 总结

通过引入RocketMQ消息队列，实现关键业务流程的异步化处理，预期可以显著提升系统性能和用户体验。整个实施过程需要7-10天，采用渐进式上线策略，确保系统稳定性和业务连续性。