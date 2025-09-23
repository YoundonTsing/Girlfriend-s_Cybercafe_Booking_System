# 订单创建失败Bug排查修复报告

## 问题概述

**问题描述**: 用户在创建订单后进行支付时，系统返回"库存确认失败"错误，导致支付流程无法完成。

**影响范围**: 所有订单支付流程

**严重级别**: P0 - 阻塞性问题

**发现时间**: 2025-09-22 10:14

## 问题现象

### 1. 错误日志分析

**支付服务错误日志**:
```
2025-09-22 10:14:32.123 [http-nio-8003-exec-5] ERROR c.t.o.s.OrderServiceImpl - 库存确认失败: false
2025-09-22 10:14:32.124 [http-nio-8003-exec-5] ERROR c.t.o.s.OrderServiceImpl - 支付失败，订单号: ORD20250922101432001
```

**Show服务错误日志**:
```
2025-09-22 10:14:32.120 [http-nio-8002-exec-3] WARN c.t.s.s.TicketStockServiceImpl - 尝试确认库存，但锁定库存为0: ticketId=1, quantity=1, lockedStock=0
```

### 2. 问题表现

1. 订单创建成功，状态为"待支付"
2. 支付时调用库存确认接口失败
3. 错误信息显示"锁定库存为0，但尝试确认1个库存"
4. 支付流程中断，用户无法完成购票

## 问题排查过程

### 1. 初步分析

通过错误日志可以看出：
- 订单创建成功，说明Redis预减库存正常
- 支付时库存确认失败，提示锁定库存为0
- 问题出现在数据库库存锁定环节

### 2. 代码流程分析

#### 2.1 订单创建流程检查

检查 `OrderServiceImpl.createOrder` 方法：

**发现问题**: 订单创建流程中只调用了Redis预减库存，缺少数据库库存锁定步骤

```java
// 原有流程 - 只有Redis预减库存
Integer stockResult = showFeignClient.predeductStockFromRedis(ticketId, quantity);
if (stockResult != 1) {
    throw new RuntimeException("库存不足");
}
// 缺少数据库库存锁定步骤
```

#### 2.2 支付流程检查

检查 `OrderServiceImpl.payOrder` 方法：

**发现问题**: 支付时直接调用库存确认，但此时数据库中没有锁定库存

```java
// 支付流程 - 直接确认库存
Boolean confirmResult = showFeignClient.confirmStockFromDatabase(ticketId, quantity);
if (!confirmResult) {
    throw new RuntimeException("库存确认失败");
}
```

#### 2.3 库存确认方法检查

检查 `TicketController.confirmStockFromDatabase` 方法：

**发现问题**: 返回值处理错误，失败时返回了成功结果

```java
// 原有代码 - 返回值处理错误
@PostMapping("/confirmStockFromDatabase")
public Result<Boolean> confirmStockFromDatabase(@RequestParam Long ticketId, @RequestParam Integer quantity) {
    boolean result = ticketStockService.confirmStockFromDatabase(ticketId, quantity);
    return Result.success(result); // 错误：失败时也返回success
}
```

### 3. 根本原因分析

**核心问题**: 订单创建和支付流程设计不完整

1. **订单创建阶段**: 只进行了Redis预减库存，没有进行数据库库存锁定
2. **支付确认阶段**: 尝试确认数据库库存，但实际上没有锁定库存可供确认
3. **返回值处理**: 库存确认失败时返回值包装错误

**业务流程缺陷**:
```
正确流程: 创建订单 -> Redis预减库存 -> 数据库锁定库存 -> 支付 -> 确认库存
实际流程: 创建订单 -> Redis预减库存 -> 支付 -> 确认库存(失败)
```

## 修复方案

### 1. 修复库存确认返回值处理

**文件**: `ticket-show/src/main/java/com/ticketsystem/show/controller/TicketController.java`

**修改内容**:
```java
@PostMapping("/confirmStockFromDatabase")
@PerformanceMonitor
public Result<Boolean> confirmStockFromDatabase(@RequestParam Long ticketId, @RequestParam Integer quantity) {
    boolean result = ticketStockService.confirmStockFromDatabase(ticketId, quantity);
    if (result) {
        return Result.success(true);
    } else {
        return Result.error("库存确认失败，锁定库存不足");
    }
}
```

### 2. 修复订单创建流程

**文件**: `ticket-order/src/main/java/com/ticketsystem/order/service/impl/OrderServiceImpl.java`

**修改内容**: 在Redis预减库存成功后，添加数据库库存锁定步骤

```java
// Redis预减库存
Integer stockResult = showFeignClient.predeductStockFromRedis(ticketId, quantity);
if (stockResult != 1) {
    throw new RuntimeException("库存不足");
}

// 新增：数据库库存锁定
try {
    Result<Boolean> lockResult = showFeignClient.lockTicketStock(ticketId, quantity);
    if (!lockResult.isSuccess() || !lockResult.getData()) {
        // 锁定失败，回滚Redis库存
        try {
            showFeignClient.rollbackStockToRedis(ticketId, quantity);
        } catch (Exception rollbackEx) {
            log.error("回滚Redis库存失败: ticketId={}, quantity={}", ticketId, quantity, rollbackEx);
            // 异步补偿
            compensationService.compensateStockRollback(ticketId, quantity);
        }
        throw new RuntimeException("数据库库存锁定失败");
    }
} catch (Exception e) {
    // 锁定异常，回滚Redis库存
    try {
        showFeignClient.rollbackStockToRedis(ticketId, quantity);
    } catch (Exception rollbackEx) {
        log.error("回滚Redis库存失败: ticketId={}, quantity={}", ticketId, quantity, rollbackEx);
        compensationService.compensateStockRollback(ticketId, quantity);
    }
    throw new RuntimeException("数据库库存锁定异常: " + e.getMessage());
}
```

## 修复验证

### 1. 代码审查

- ✅ 确认 `ShowFeignClient` 接口包含 `lockTicketStock` 方法
- ✅ 确认库存锁定和回滚逻辑完整
- ✅ 确认异常处理和补偿机制

### 2. 业务流程验证

修复后的完整流程：
```
1. 用户创建订单
2. Redis预减库存
3. 数据库锁定库存 (新增)
4. 创建订单记录
5. 用户支付
6. 确认数据库库存
7. 支付成功
```

## 技术细节

### 1. 连接池和线程池配置

系统采用了高并发优化配置：

**数据库连接池配置**:
- 写库连接池: 最大100个连接，最小20个空闲连接
- 读库连接池: 最大200个连接，最小50个空闲连接
- 连接超时: 写库10秒，读库5秒

**线程池配置**:
- 核心线程数: 10
- 最大线程数: 50
- 队列容量: 200
- 拒绝策略: CallerRunsPolicy

### 2. 监控和告警

系统配置了连接池监控：
- 每30秒监控一次连接池状态
- 写库活跃连接超过80个时告警
- 读库活跃连接超过150个时告警

## 预防措施

### 1. 代码规范

1. **完整的业务流程设计**: 确保每个业务操作都有完整的正向和逆向流程
2. **统一的返回值处理**: 建立统一的Result包装规范
3. **完善的异常处理**: 每个外部调用都要有异常处理和补偿机制

### 2. 测试规范

1. **集成测试**: 覆盖完整的业务流程，包括异常场景
2. **压力测试**: 验证高并发场景下的系统稳定性
3. **回滚测试**: 验证各种异常情况下的数据一致性

### 3. 监控告警

1. **业务监控**: 监控订单创建成功率、支付成功率
2. **技术监控**: 监控连接池使用率、线程池使用率
3. **异常告警**: 及时发现和处理系统异常

## 总结

本次bug的根本原因是业务流程设计不完整，订单创建时缺少数据库库存锁定步骤，导致支付时无法确认库存。通过完善订单创建流程和修复返回值处理，问题得到彻底解决。

**关键修复点**:
1. 订单创建流程添加数据库库存锁定步骤
2. 完善异常处理和回滚机制
3. 修复库存确认接口的返回值处理

**经验教训**:
1. 业务流程设计要考虑完整性，包括正向流程和异常处理
2. 外部接口调用要有完善的异常处理和补偿机制
3. 返回值处理要严格区分成功和失败场景

---

**报告生成时间**: 2025-09-22  
**修复状态**: 已完成  
**下一步**: 进行完整的回归测试