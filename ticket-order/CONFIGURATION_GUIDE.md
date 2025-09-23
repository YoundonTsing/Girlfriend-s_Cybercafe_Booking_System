# 订单服务配置指南

## 概述

本文档说明了订单服务的配置项，包括高并发优化配置、Redis配置、数据库配置等。

## 主要配置项

### 1. 数据库配置

#### 写库配置（高并发优化）
```yaml
spring:
  datasource:
    write:
      hikari:
        maximum-pool-size: 100  # 最大连接数
        minimum-idle: 20        # 最小空闲连接
        connection-timeout: 10000  # 连接超时时间
```

#### 读库配置（高并发优化）
```yaml
spring:
  datasource:
    read:
      hikari:
        maximum-pool-size: 200  # 最大连接数
        minimum-idle: 50        # 最小空闲连接
        connection-timeout: 5000   # 连接超时时间
```

### 2. Redis配置

#### Redisson配置（统一技术栈）
```yaml
spring:
  redis:
    redisson:
      config: |
        singleServerConfig:
          connectionPoolSize: 64
          connectionMinimumIdleSize: 10
          threads: 16
          nettyThreads: 32
```

### 3. 应用配置

#### 统一订单服务配置
```yaml
app:
  order:
    unified:
      redis:
        enabled: true
        order-expire-time: 900  # 15分钟
      async:
        enabled: true
        persist-delay: 100  # 100ms后持久化
      performance:
        enable-monitoring: true
        slow-query-threshold: 1000  # 1秒
```

#### 迁移配置
```yaml
app:
  order:
    migration:
      enable-gradual: false  # 是否启用渐进式迁移
      redis-percentage: 0    # Redis方案流量比例
      unified-percentage: 0  # 统一方案流量比例
      database-percentage: 100  # 数据库方案流量比例
```

## 环境变量配置

### 数据库相关
- `DB_WRITE_MAX_POOL_SIZE`: 写库最大连接池大小（默认：100）
- `DB_READ_MAX_POOL_SIZE`: 读库最大连接池大小（默认：200）
- `DB_WRITE_CONNECTION_TIMEOUT`: 写库连接超时（默认：10000ms）
- `DB_READ_CONNECTION_TIMEOUT`: 读库连接超时（默认：5000ms）

### Redis相关
- `REDIS_CONNECTION_POOL_SIZE`: Redis连接池大小（默认：64）
- `REDIS_CONNECTION_MIN_IDLE`: Redis最小空闲连接（默认：10）
- `REDIS_THREADS`: Redis线程数（默认：16）
- `REDIS_NETTY_THREADS`: Redis Netty线程数（默认：32）

### 订单服务相关
- `ORDER_REDIS_ENABLED`: 是否启用Redis订单服务（默认：true）
- `ORDER_ASYNC_ENABLED`: 是否启用异步处理（默认：true）
- `ORDER_MIGRATION_ENABLED`: 是否启用渐进式迁移（默认：false）
- `ORDER_MIGRATION_REDIS_PERCENTAGE`: Redis方案流量比例（默认：0）
- `ORDER_MIGRATION_UNIFIED_PERCENTAGE`: 统一方案流量比例（默认：0）
- `ORDER_MIGRATION_DATABASE_PERCENTAGE`: 数据库方案流量比例（默认：100）

## 性能调优建议

### 1. 数据库连接池调优
- **写库连接池**: 根据并发写入量调整，建议50-100个连接
- **读库连接池**: 根据查询并发量调整，建议100-200个连接
- **连接超时**: 写库10秒，读库5秒

### 2. Redis连接池调优
- **连接池大小**: 根据Redis服务器性能调整，建议32-64个连接
- **线程数**: 根据CPU核心数调整，建议16-32个线程

### 3. 异步处理调优
- **持久化延迟**: 根据业务需求调整，建议100-500ms
- **通知延迟**: 根据用户体验需求调整，建议50-200ms

## 迁移策略

### 阶段1：数据库优化（立即实施）
```bash
# 设置环境变量
export DB_WRITE_MAX_POOL_SIZE=100
export DB_READ_MAX_POOL_SIZE=200
export ORDER_MIGRATION_ENABLED=false
```

### 阶段2：小流量验证（1-2天）
```bash
# 设置环境变量
export ORDER_MIGRATION_ENABLED=true
export ORDER_MIGRATION_REDIS_PERCENTAGE=0
export ORDER_MIGRATION_UNIFIED_PERCENTAGE=20
export ORDER_MIGRATION_DATABASE_PERCENTAGE=80
```

### 阶段3：中等流量验证（3-5天）
```bash
# 设置环境变量
export ORDER_MIGRATION_REDIS_PERCENTAGE=20
export ORDER_MIGRATION_UNIFIED_PERCENTAGE=30
export ORDER_MIGRATION_DATABASE_PERCENTAGE=50
```

### 阶段4：完全切换（1天）
```bash
# 设置环境变量
export ORDER_MIGRATION_REDIS_PERCENTAGE=0
export ORDER_MIGRATION_UNIFIED_PERCENTAGE=100
export ORDER_MIGRATION_DATABASE_PERCENTAGE=0
```

## 监控配置

### 1. 性能监控
- 启用性能监控：`ORDER_PERFORMANCE_MONITORING=true`
- 慢查询阈值：`ORDER_SLOW_QUERY_THRESHOLD=1000`
- 高并发阈值：`ORDER_HIGH_CONCURRENCY_THRESHOLD=100`

### 2. 日志监控
- 日志级别：`LOG_LEVEL=debug`
- 日志文件：`logs/ticket-order.log`
- 日志轮转：100MB，保留30天

## 故障排查

### 1. 数据库连接问题
- 检查连接池配置
- 检查数据库服务器状态
- 查看连接池监控日志

### 2. Redis连接问题
- 检查Redis服务器状态
- 检查网络连接
- 查看Redisson连接日志

### 3. 性能问题
- 查看性能监控指标
- 检查慢查询日志
- 分析连接池使用情况

## 注意事项

1. **配置一致性**: 确保所有环境使用相同的配置结构
2. **向后兼容**: 保持默认配置的向后兼容性
3. **环境隔离**: 不同环境使用不同的配置值
4. **监控告警**: 设置关键指标的监控告警
5. **定期检查**: 定期检查配置的有效性和性能影响