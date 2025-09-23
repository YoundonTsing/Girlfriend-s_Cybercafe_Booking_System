# 彻底修复Lua脚本参数问题

## 问题现状
即使修复了代码，仍然出现 `PooledUnsafeDirectByteBuf` 错误，说明问题可能更深层。

## 可能的原因

### 1. 服务未重启
- 修复的代码没有生效
- 需要重新编译和重启服务

### 2. Redisson版本问题
- 某些Redisson版本对参数类型处理有问题
- 需要检查Redisson版本和配置

### 3. 参数传递方式问题
- 可能需要使用不同的参数传递方式
- 或者需要额外的类型转换

## 彻底修复方案

### 方案1: 强制重启服务
```bash
# 1. 停止所有Java进程
taskkill /f /im java.exe

# 2. 清理编译缓存
cd D:\Tickets\ticket-show
mvn clean

# 3. 重新编译
mvn compile

# 4. 重启服务
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 方案2: 修改参数传递方式
如果问题仍然存在，可能需要使用不同的参数传递方式：

```java
// 当前方式
Arrays.asList(lockValueStr, expireTimeStr, syncExpireStr, timestampStr)

// 替代方式1: 使用Collections.singletonList
Collections.singletonList(lockValueStr)

// 替代方式2: 使用Object数组
new Object[]{lockValueStr, expireTimeStr, syncExpireStr, timestampStr}

// 替代方式3: 使用List.of (Java 9+)
List.of(lockValueStr, expireTimeStr, syncExpireStr, timestampStr)
```

### 方案3: 使用Redisson的evalAsync方法
```java
// 使用异步方式执行Lua脚本
CompletableFuture<Long> future = redissonClient.getScript().evalAsync(
    RScript.Mode.READ_WRITE,
    ENHANCED_LOCK_SCRIPT,
    RScript.ReturnType.INTEGER,
    Arrays.asList(lockKey, syncKey),
    Arrays.asList(lockValueStr, expireTimeStr, syncExpireStr, timestampStr)
);

Long result = future.get(5, TimeUnit.SECONDS);
```

### 方案4: 检查Redisson配置
在 `application.yml` 中添加：
```yaml
spring:
  redis:
    redisson:
      config: |
        singleServerConfig:
          address: "redis://localhost:6379"
          connectionPoolSize: 64
          connectionMinimumIdleSize: 10
          idleConnectionTimeout: 10000
          connectTimeout: 10000
          timeout: 3000
          retryAttempts: 3
          retryInterval: 1500
```

## 测试验证

### 1. 运行测试脚本
```bash
python test/check_lua_fix.py
```

### 2. 检查日志
查看是否还有 `PooledUnsafeDirectByteBuf` 错误

### 3. 验证功能
确认座位锁定功能正常工作

## 应急方案

如果问题仍然存在，可以考虑：

### 1. 临时禁用Lua脚本
使用简单的Redis命令替代Lua脚本：
```java
// 替代Lua脚本的简单实现
public boolean tryRedisLockSimple(Long seatId, String lockValue) {
    String lockKey = SEAT_LOCK_PREFIX + seatId;
    String syncKey = SEAT_SYNC_PREFIX + seatId;
    
    try {
        // 使用简单的Redis命令
        Boolean lockResult = redissonClient.getBucket(lockKey).trySet(lockValue, LOCK_EXPIRE_TIME, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lockResult)) {
            redissonClient.getBucket(syncKey).set(System.currentTimeMillis(), SYNC_EXPIRE_TIME, TimeUnit.SECONDS);
            return true;
        }
        return false;
    } catch (Exception e) {
        log.error("Redis锁定失败", e);
        return false;
    }
}
```

### 2. 降级到数据库锁定
如果Redis问题无法解决，可以临时使用纯数据库锁定：
```java
public boolean lockSeatsDatabaseOnly(List<Long> seatIds, Long userId) {
    try {
        int lockedCount = seatMapper.lockSeats(seatIds, userId);
        return lockedCount == seatIds.size();
    } catch (Exception e) {
        log.error("数据库锁定失败", e);
        return false;
    }
}
```

## 总结

1. **立即执行**: 重启服务并运行测试
2. **如果仍有问题**: 尝试不同的参数传递方式
3. **最后手段**: 使用应急方案确保功能可用

关键是要确保修复生效，然后逐步验证功能正常。