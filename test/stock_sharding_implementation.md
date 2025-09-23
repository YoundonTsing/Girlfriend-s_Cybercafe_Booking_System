# 库存分片策略实施方案

## 🎯 分片策略概述

### 实现难度评估: ⭐⭐⭐⭐⭐ (困难)

**核心挑战:**
- 需要重构现有库存架构
- 分片间数据一致性保证
- 热点数据识别和动态调整
- 分片路由算法设计
- 跨分片事务处理

## 📋 渐进式实施路径

### 阶段一: 静态分片基础架构 (2-3天)

#### 1.1 创建分片配置

```java
package com.ticketsystem.show.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "stock.sharding")
public class StockShardingConfig {
    
    /**
     * 是否启用分片
     */
    private boolean enabled = false;
    
    /**
     * 默认分片数量
     */
    private int defaultShardCount = 4;
    
    /**
     * 热点票档分片数量
     */
    private int hotTicketShardCount = 8;
    
    /**
     * 热点票档ID列表
     */
    private List<Long> hotTicketIds;
    
    /**
     * 分片键前缀
     */
    private String shardKeyPrefix = "stock:shard";
    
    /**
     * 聚合键前缀
     */
    private String aggregateKeyPrefix = "stock:aggregate";
}
```

#### 1.2 分片路由器

```java
package com.ticketsystem.show.sharding;

import com.ticketsystem.show.config.StockShardingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockShardRouter {
    
    private final StockShardingConfig config;
    
    /**
     * 获取票档的分片数量
     */
    public int getShardCount(Long ticketId) {
        if (!config.isEnabled()) {
            return 1; // 未启用分片时返回1
        }
        
        // 检查是否为热点票档
        if (config.getHotTicketIds() != null && config.getHotTicketIds().contains(ticketId)) {
            return config.getHotTicketShardCount();
        }
        
        return config.getDefaultShardCount();
    }
    
    /**
     * 获取所有分片键
     */
    public List<String> getAllShardKeys(Long ticketId) {
        int shardCount = getShardCount(ticketId);
        List<String> shardKeys = new ArrayList<>();
        
        for (int i = 0; i < shardCount; i++) {
            shardKeys.add(getShardKey(ticketId, i));
        }
        
        return shardKeys;
    }
    
    /**
     * 获取特定分片键
     */
    public String getShardKey(Long ticketId, int shardIndex) {
        return String.format("%s:%d:%d", config.getShardKeyPrefix(), ticketId, shardIndex);
    }
    
    /**
     * 根据负载选择最优分片
     */
    public String selectOptimalShard(Long ticketId) {
        int shardCount = getShardCount(ticketId);
        
        if (shardCount == 1) {
            return getShardKey(ticketId, 0);
        }
        
        // 简单的随机选择策略（后续可优化为基于负载的选择）
        int selectedShard = ThreadLocalRandom.current().nextInt(shardCount);
        return getShardKey(ticketId, selectedShard);
    }
    
    /**
     * 获取聚合键
     */
    public String getAggregateKey(Long ticketId) {
        return String.format("%s:%d", config.getAggregateKeyPrefix(), ticketId);
    }
    
    /**
     * 计算分片索引
     */
    public int calculateShardIndex(Long ticketId, String userId) {
        int shardCount = getShardCount(ticketId);
        if (shardCount == 1) {
            return 0;
        }
        
        // 基于用户ID的哈希分片
        return Math.abs(userId.hashCode()) % shardCount;
    }
}
```

#### 1.3 分片库存服务

```java
package com.ticketsystem.show.service.impl;

import com.ticketsystem.show.config.StockShardingConfig;
import com.ticketsystem.show.sharding.StockShardRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShardedStockService {
    
    private final RedissonClient redissonClient;
    private final StockShardRouter shardRouter;
    private final StockShardingConfig config;
    
    private String shardInitScript;
    private String shardPredeductScript;
    private String shardRollbackScript;
    private String shardAggregateScript;
    
    @PostConstruct
    public void loadLuaScripts() {
        // 分片初始化脚本
        shardInitScript = """
            local shardKeys = KEYS
            local totalStock = tonumber(ARGV[1])
            local shardCount = #shardKeys
            local stockPerShard = math.floor(totalStock / shardCount)
            local remainder = totalStock % shardCount
            
            for i = 1, shardCount do
                local shardStock = stockPerShard
                if i <= remainder then
                    shardStock = shardStock + 1
                end
                redis.call('HSET', shardKeys[i], 'available', shardStock, 'total', shardStock)
                redis.call('EXPIRE', shardKeys[i], 86400)
            end
            
            return totalStock
        """;
        
        // 分片预减脚本
        shardPredeductScript = """
            local shardKey = KEYS[1]
            local quantity = tonumber(ARGV[1])
            
            local available = tonumber(redis.call('HGET', shardKey, 'available') or 0)
            
            if available >= quantity then
                redis.call('HINCRBY', shardKey, 'available', -quantity)
                return 1
            else
                return 0
            end
        """;
        
        // 分片回滚脚本
        shardRollbackScript = """
            local shardKey = KEYS[1]
            local quantity = tonumber(ARGV[1])
            
            redis.call('HINCRBY', shardKey, 'available', quantity)
            return 1
        """;
        
        // 分片聚合脚本
        shardAggregateScript = """
            local shardKeys = KEYS
            local totalAvailable = 0
            local totalStock = 0
            
            for i = 1, #shardKeys do
                local available = tonumber(redis.call('HGET', shardKeys[i], 'available') or 0)
                local total = tonumber(redis.call('HGET', shardKeys[i], 'total') or 0)
                totalAvailable = totalAvailable + available
                totalStock = totalStock + total
            end
            
            return {totalAvailable, totalStock}
        """;
    }
    
    /**
     * 初始化分片库存
     */
    public boolean initializeShardedStock(Long ticketId, Integer totalStock) {
        if (!config.isEnabled()) {
            return false; // 分片未启用
        }
        
        try {
            List<String> shardKeys = shardRouter.getAllShardKeys(ticketId);
            
            Long result = redissonClient.getScript().eval(
                RScript.Mode.READ_WRITE,
                shardInitScript,
                RScript.ReturnType.INTEGER,
                shardKeys,
                totalStock.toString()
            );
            
            log.info("初始化分片库存成功 - 票档ID: {}, 总库存: {}, 分片数: {}", 
                    ticketId, totalStock, shardKeys.size());
            
            return result != null && result.intValue() == totalStock;
            
        } catch (Exception e) {
            log.error("初始化分片库存失败 - 票档ID: {}", ticketId, e);
            return false;
        }
    }
    
    /**
     * 分片预减库存
     */
    public boolean predeductFromShard(Long ticketId, Integer quantity, String userId) {
        if (!config.isEnabled()) {
            return false;
        }
        
        try {
            // 选择分片策略：优先尝试用户关联的分片
            int userShardIndex = shardRouter.calculateShardIndex(ticketId, userId);
            String userShardKey = shardRouter.getShardKey(ticketId, userShardIndex);
            
            // 尝试从用户分片预减
            Long result = redissonClient.getScript().eval(
                RScript.Mode.READ_WRITE,
                shardPredeductScript,
                RScript.ReturnType.INTEGER,
                Arrays.asList(userShardKey),
                quantity.toString()
            );
            
            if (result != null && result.intValue() == 1) {
                log.debug("从用户分片预减成功 - 票档ID: {}, 数量: {}, 分片: {}", 
                        ticketId, quantity, userShardIndex);
                return true;
            }
            
            // 用户分片库存不足，尝试其他分片
            return tryOtherShards(ticketId, quantity, userShardIndex);
            
        } catch (Exception e) {
            log.error("分片预减库存失败 - 票档ID: {}, 数量: {}", ticketId, quantity, e);
            return false;
        }
    }
    
    /**
     * 尝试其他分片
     */
    private boolean tryOtherShards(Long ticketId, Integer quantity, int excludeShardIndex) {
        List<String> allShardKeys = shardRouter.getAllShardKeys(ticketId);
        
        for (int i = 0; i < allShardKeys.size(); i++) {
            if (i == excludeShardIndex) {
                continue; // 跳过已尝试的分片
            }
            
            String shardKey = allShardKeys.get(i);
            
            try {
                Long result = redissonClient.getScript().eval(
                    RScript.Mode.READ_WRITE,
                    shardPredeductScript,
                    RScript.ReturnType.INTEGER,
                    Arrays.asList(shardKey),
                    quantity.toString()
                );
                
                if (result != null && result.intValue() == 1) {
                    log.debug("从备选分片预减成功 - 票档ID: {}, 数量: {}, 分片: {}", 
                            ticketId, quantity, i);
                    return true;
                }
                
            } catch (Exception e) {
                log.warn("尝试分片 {} 预减失败", i, e);
            }
        }
        
        log.warn("所有分片库存不足 - 票档ID: {}, 请求数量: {}", ticketId, quantity);
        return false;
    }
    
    /**
     * 回滚分片库存
     */
    public boolean rollbackToShard(Long ticketId, Integer quantity, String userId) {
        if (!config.isEnabled()) {
            return false;
        }
        
        try {
            // 回滚到用户关联的分片
            int userShardIndex = shardRouter.calculateShardIndex(ticketId, userId);
            String userShardKey = shardRouter.getShardKey(ticketId, userShardIndex);
            
            Long result = redissonClient.getScript().eval(
                RScript.Mode.READ_WRITE,
                shardRollbackScript,
                RScript.ReturnType.INTEGER,
                Arrays.asList(userShardKey),
                quantity.toString()
            );
            
            log.debug("分片库存回滚成功 - 票档ID: {}, 数量: {}, 分片: {}", 
                    ticketId, quantity, userShardIndex);
            
            return result != null && result.intValue() == 1;
            
        } catch (Exception e) {
            log.error("分片库存回滚失败 - 票档ID: {}, 数量: {}", ticketId, quantity, e);
            return false;
        }
    }
    
    /**
     * 聚合分片库存
     */
    public int[] aggregateShardedStock(Long ticketId) {
        if (!config.isEnabled()) {
            return new int[]{0, 0};
        }
        
        try {
            List<String> shardKeys = shardRouter.getAllShardKeys(ticketId);
            
            List<Long> result = redissonClient.getScript().eval(
                RScript.Mode.READ_ONLY,
                shardAggregateScript,
                RScript.ReturnType.MULTI,
                shardKeys
            );
            
            if (result != null && result.size() >= 2) {
                int availableStock = result.get(0).intValue();
                int totalStock = result.get(1).intValue();
                
                log.debug("聚合分片库存 - 票档ID: {}, 可用: {}, 总计: {}", 
                        ticketId, availableStock, totalStock);
                
                return new int[]{availableStock, totalStock};
            }
            
        } catch (Exception e) {
            log.error("聚合分片库存失败 - 票档ID: {}", ticketId, e);
        }
        
        return new int[]{0, 0};
    }
    
    /**
     * 异步聚合所有分片（用于缓存更新）
     */
    public CompletableFuture<Void> asyncAggregateAndCache(Long ticketId) {
        return CompletableFuture.runAsync(() -> {
            try {
                int[] stockInfo = aggregateShardedStock(ticketId);
                String aggregateKey = shardRouter.getAggregateKey(ticketId);
                
                // 缓存聚合结果
                redissonClient.getBucket(aggregateKey)
                        .set(String.format("%d:%d", stockInfo[0], stockInfo[1]), 30, TimeUnit.SECONDS);
                
            } catch (Exception e) {
                log.error("异步聚合缓存失败 - 票档ID: {}", ticketId, e);
            }
        });
    }
}
```

### 阶段二: 集成现有服务 (1-2天)

#### 2.1 修改TicketStockServiceImpl

```java
// 在TicketStockServiceImpl中添加分片支持

@Autowired
private ShardedStockService shardedStockService;

@Autowired
private StockShardingConfig shardingConfig;

// 修改initializeStock方法
@Override
public boolean initializeStock(Long ticketId, Integer totalStock) {
    try {
        // 优先尝试分片初始化
        if (shardingConfig.isEnabled()) {
            boolean shardResult = shardedStockService.initializeShardedStock(ticketId, totalStock);
            if (shardResult) {
                log.info("使用分片模式初始化库存 - 票档ID: {}", ticketId);
                return true;
            }
        }
        
        // 降级到原有逻辑
        return redisStockService.initializeStock(ticketId, totalStock);
        
    } catch (Exception e) {
        log.error("初始化库存失败 - 票档ID: {}", ticketId, e);
        return false;
    }
}

// 修改predeductStockFromRedis方法
private boolean predeductStockFromRedis(Long ticketId, Integer quantity, String userId) {
    try {
        // 优先尝试分片预减
        if (shardingConfig.isEnabled()) {
            boolean shardResult = shardedStockService.predeductFromShard(ticketId, quantity, userId);
            if (shardResult) {
                // 异步更新聚合缓存
                shardedStockService.asyncAggregateAndCache(ticketId);
                return true;
            }
        }
        
        // 降级到原有逻辑
        return redisStockService.predeductStock(ticketId, quantity);
        
    } catch (Exception e) {
        log.error("Redis预减库存失败 - 票档ID: {}, 数量: {}", ticketId, quantity, e);
        return false;
    }
}
```

### 阶段三: 配置和测试 (1天)

#### 3.1 配置文件

```yaml
# application.yml
stock:
  sharding:
    enabled: true
    default-shard-count: 4
    hot-ticket-shard-count: 8
    hot-ticket-ids:
      - 1
      - 2
      - 3
    shard-key-prefix: "stock:shard"
    aggregate-key-prefix: "stock:aggregate"
```

#### 3.2 分片测试脚本

```python
# test/stock_sharding_test.py
import redis
import time
import threading
from concurrent.futures import ThreadPoolExecutor

class StockShardingTest:
    def __init__(self):
        self.redis_client = redis.Redis(host='localhost', port=6379, db=0)
        
    def test_shard_initialization(self):
        """测试分片初始化"""
        print("\n=== 测试分片初始化 ===")
        
        ticket_id = 1
        total_stock = 1000
        shard_count = 4
        
        # 清理旧数据
        for i in range(shard_count):
            shard_key = f"stock:shard:{ticket_id}:{i}"
            self.redis_client.delete(shard_key)
        
        # 模拟分片初始化
        stock_per_shard = total_stock // shard_count
        remainder = total_stock % shard_count
        
        for i in range(shard_count):
            shard_stock = stock_per_shard + (1 if i < remainder else 0)
            shard_key = f"stock:shard:{ticket_id}:{i}"
            
            self.redis_client.hset(shard_key, mapping={
                'available': shard_stock,
                'total': shard_stock
            })
            self.redis_client.expire(shard_key, 86400)
            
            print(f"分片 {i}: {shard_stock} 库存")
        
        # 验证总库存
        total_available = sum(
            int(self.redis_client.hget(f"stock:shard:{ticket_id}:{i}", 'available') or 0)
            for i in range(shard_count)
        )
        
        print(f"总库存验证: {total_available}/{total_stock}")
        assert total_available == total_stock, "分片初始化失败"
        print("✅ 分片初始化测试通过")
    
    def test_concurrent_shard_prededuct(self):
        """测试并发分片预减"""
        print("\n=== 测试并发分片预减 ===")
        
        ticket_id = 1
        concurrent_users = 50
        quantity_per_user = 5
        
        success_count = 0
        failure_count = 0
        lock = threading.Lock()
        
        def prededuct_worker(user_id):
            nonlocal success_count, failure_count
            
            # 选择用户对应的分片
            shard_index = hash(str(user_id)) % 4
            shard_key = f"stock:shard:{ticket_id}:{shard_index}"
            
            # 模拟预减操作
            lua_script = """
                local available = tonumber(redis.call('HGET', KEYS[1], 'available') or 0)
                local quantity = tonumber(ARGV[1])
                
                if available >= quantity then
                    redis.call('HINCRBY', KEYS[1], 'available', -quantity)
                    return 1
                else
                    return 0
                end
            """
            
            try:
                result = self.redis_client.eval(lua_script, 1, shard_key, quantity_per_user)
                
                with lock:
                    if result == 1:
                        success_count += 1
                        print(f"用户 {user_id} 预减成功 (分片 {shard_index})")
                    else:
                        failure_count += 1
                        print(f"用户 {user_id} 预减失败 (分片 {shard_index})")
                        
            except Exception as e:
                with lock:
                    failure_count += 1
                    print(f"用户 {user_id} 预减异常: {e}")
        
        # 并发执行
        with ThreadPoolExecutor(max_workers=20) as executor:
            futures = [executor.submit(prededuct_worker, i) for i in range(concurrent_users)]
            for future in futures:
                future.result()
        
        print(f"\n并发测试结果:")
        print(f"成功: {success_count}")
        print(f"失败: {failure_count}")
        print(f"总计: {success_count + failure_count}")
        
        # 验证剩余库存
        remaining_stock = sum(
            int(self.redis_client.hget(f"stock:shard:{ticket_id}:{i}", 'available') or 0)
            for i in range(4)
        )
        
        expected_remaining = 1000 - (success_count * quantity_per_user)
        print(f"剩余库存: {remaining_stock}")
        print(f"预期剩余: {expected_remaining}")
        
        assert remaining_stock == expected_remaining, "库存计算错误"
        print("✅ 并发分片预减测试通过")
    
    def test_shard_aggregation(self):
        """测试分片聚合"""
        print("\n=== 测试分片聚合 ===")
        
        ticket_id = 1
        shard_count = 4
        
        # 聚合脚本
        lua_script = """
            local shardKeys = KEYS
            local totalAvailable = 0
            local totalStock = 0
            
            for i = 1, #shardKeys do
                local available = tonumber(redis.call('HGET', shardKeys[i], 'available') or 0)
                local total = tonumber(redis.call('HGET', shardKeys[i], 'total') or 0)
                totalAvailable = totalAvailable + available
                totalStock = totalStock + total
            end
            
            return {totalAvailable, totalStock}
        """
        
        shard_keys = [f"stock:shard:{ticket_id}:{i}" for i in range(shard_count)]
        result = self.redis_client.eval(lua_script, len(shard_keys), *shard_keys)
        
        total_available, total_stock = result
        print(f"聚合结果: 可用库存 {total_available}, 总库存 {total_stock}")
        
        # 验证单个分片
        for i in range(shard_count):
            shard_key = f"stock:shard:{ticket_id}:{i}"
            available = int(self.redis_client.hget(shard_key, 'available') or 0)
            total = int(self.redis_client.hget(shard_key, 'total') or 0)
            print(f"分片 {i}: 可用 {available}, 总计 {total}")
        
        print("✅ 分片聚合测试通过")
    
    def run_all_tests(self):
        """运行所有测试"""
        print("开始库存分片测试...")
        
        try:
            self.test_shard_initialization()
            self.test_concurrent_shard_prededuct()
            self.test_shard_aggregation()
            
            print("\n🎉 所有分片测试通过！")
            
        except Exception as e:
            print(f"\n❌ 测试失败: {e}")
            raise

if __name__ == "__main__":
    test = StockShardingTest()
    test.run_all_tests()
```

## 🚀 快速启用方案

### 最小化实施 (4小时)

如果你想快速看到分片效果，可以采用以下简化方案：

#### 1. 简化配置 (30分钟)

```yaml
# 只启用2个分片进行测试
stock:
  sharding:
    enabled: true
    default-shard-count: 2
    hot-ticket-ids: [1, 2]
```

#### 2. 简化路由器 (1小时)

```java
@Component
public class SimpleShardRouter {
    
    public String getShardKey(Long ticketId, String userId) {
        int shard = Math.abs(userId.hashCode()) % 2;
        return String.format("stock:shard:%d:%d", ticketId, shard);
    }
    
    public List<String> getAllShardKeys(Long ticketId) {
        return Arrays.asList(
            String.format("stock:shard:%d:0", ticketId),
            String.format("stock:shard:%d:1", ticketId)
        );
    }
}
```

#### 3. 简化预减逻辑 (2小时)

```java
public boolean simpleShardPrededuct(Long ticketId, Integer quantity, String userId) {
    String shardKey = simpleShardRouter.getShardKey(ticketId, userId);
    
    // 直接使用现有的预减脚本
    return redisStockService.predeductFromKey(shardKey, quantity);
}
```

#### 4. 快速测试 (30分钟)

```bash
# 初始化分片
curl -X POST "http://localhost:8081/api/stock/init-shard/1?totalStock=100"

# 测试分片预减
curl -X POST "http://localhost:8081/api/stock/shard-prededuct/1?quantity=5&userId=user123"

# 查看分片状态
curl "http://localhost:8081/api/stock/shard-status/1"
```

## 📊 预期收益

### 性能提升
- **并发处理能力**: 提升 2-4倍
- **热点缓解**: 减少 60-80% 的Redis键冲突
- **响应时间**: 降低 30-50%

### 可扩展性
- **水平扩展**: 支持动态增加分片
- **负载均衡**: 自动分散热点访问
- **故障隔离**: 单分片故障不影响整体服务

## ⚠️ 注意事项

1. **数据一致性**: 分片间可能存在短暂不一致
2. **复杂度增加**: 需要额外的监控和运维
3. **存储开销**: 分片元数据会增加存储成本
4. **迁移风险**: 需要制定详细的迁移计划

## 🎯 建议实施顺序

1. **第一周**: 实施监控告警（立即见效）
2. **第二周**: 开发分片基础架构
3. **第三周**: 小规模测试分片功能
4. **第四周**: 逐步迁移热点票档
5. **第五周**: 全面启用分片策略

通过这种渐进式的实施方式，可以最大化降低风险，同时确保系统的稳定性和性能提升。