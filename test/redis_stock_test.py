#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Redis库存管理功能测试
测试内容：
1. Redis预减逻辑
2. Lua脚本原子性
3. 库存回滚机制
4. 并发场景测试
"""

import redis
import requests
import threading
import time
import json
from concurrent.futures import ThreadPoolExecutor, as_completed

class RedisStockTester:
    def __init__(self):
        # Redis连接
        self.redis_client = redis.Redis(host='localhost', port=6379, db=0, decode_responses=True)
        
        # 微服务API基础URL
        self.show_service_url = "http://localhost:8002"
        self.order_service_url = "http://localhost:8003"
        
        # 测试数据
        self.test_ticket_id = 3
        self.initial_stock = 600
        
    def setup_test_data(self):
        """初始化测试数据"""
        print("=== 初始化测试数据 ===")
        
        # 清理Redis中的库存数据
        stock_key = f"stock:ticket:{self.test_ticket_id}"
        self.redis_client.delete(stock_key)
        
        # 同步库存到Redis
        sync_url = f"{self.show_service_url}/api/ticket/redis/sync"
        response = requests.post(sync_url, params={"ticketId": self.test_ticket_id})
        
        if response.status_code == 200:
            result = response.json()
            if result.get('code') == 200:
                print(f"✓ 库存同步成功，票档ID: {self.test_ticket_id}")
            else:
                print(f"✗ 库存同步失败: {result.get('message')}")
        else:
            print(f"✗ 库存同步请求失败: {response.status_code}")
        
        # 检查Redis中的库存
        current_stock = self.redis_client.get(stock_key)
        print(f"当前Redis库存: {current_stock}")
        
    def test_redis_prededuct_logic(self):
        """测试Redis预减逻辑"""
        print("\n=== 测试Redis预减逻辑 ===")
        
        # 测试正常预减
        prededuct_url = f"{self.show_service_url}/api/ticket/redis/prededuct"
        
        test_cases = [
            {"quantity": 5, "expected_result": 1, "description": "正常预减5张票"},
            {"quantity": 10, "expected_result": 1, "description": "正常预减10张票"},
            {"quantity": 200, "expected_result": 0, "description": "预减超量（库存不足）"},
        ]
        
        for case in test_cases:
            response = requests.post(prededuct_url, params={
                "ticketId": self.test_ticket_id,
                "quantity": case["quantity"]
            })
            
            if response.status_code == 200:
                result = response.json()
                actual_result = result.get('data')
                if actual_result == case["expected_result"]:
                    print(f"✓ {case['description']}: 预期结果 {case['expected_result']}, 实际结果 {actual_result}")
                else:
                    print(f"✗ {case['description']}: 预期结果 {case['expected_result']}, 实际结果 {actual_result}")
            else:
                print(f"✗ {case['description']}: 请求失败 {response.status_code}")
    
    def test_lua_script_atomicity(self):
        """测试Lua脚本原子性（并发测试）"""
        print("\n=== 测试Lua脚本原子性（并发测试） ===")
        
        # 重新初始化库存
        self.setup_test_data()
        
        # 测试预减接口
        prededuct_url = f"{self.show_service_url}/api/ticket/redis/prededuct"
        
        def concurrent_prededuct(thread_id):
            """并发预减函数"""
            try:
                response = requests.post(prededuct_url, params={
                    "ticketId": self.test_ticket_id,
                    "quantity": 1
                })
                
                if response.status_code == 200:
                    result = response.json()
                    return {
                        "thread_id": thread_id,
                        "success": result.get('data') == 1,
                        "result": result.get('data'),
                        "timestamp": time.time()
                    }
                else:
                    return {
                        "thread_id": thread_id,
                        "success": False,
                        "error": f"HTTP {response.status_code}",
                        "timestamp": time.time()
                    }
            except Exception as e:
                return {
                    "thread_id": thread_id,
                    "success": False,
                    "error": str(e),
                    "timestamp": time.time()
                }
        
        # 启动50个并发线程，每个预减1张票
        thread_count = 50
        results = []
        
        with ThreadPoolExecutor(max_workers=thread_count) as executor:
            futures = [executor.submit(concurrent_prededuct, i) for i in range(thread_count)]
            
            for future in as_completed(futures):
                results.append(future.result())
        
        # 分析结果
        successful_prededucts = sum(1 for r in results if r.get('success', False))
        failed_prededucts = thread_count - successful_prededucts
        
        print(f"并发预减结果: 成功 {successful_prededucts} 次, 失败 {failed_prededucts} 次")
        
        # 检查最终库存
        stock_key = f"stock:ticket:{self.test_ticket_id}"
        final_stock = self.redis_client.get(stock_key)
        expected_stock = self.initial_stock - successful_prededucts
        
        if int(final_stock) == expected_stock:
            print(f"✓ 库存一致性检查通过: 最终库存 {final_stock}, 预期库存 {expected_stock}")
        else:
            print(f"✗ 库存一致性检查失败: 最终库存 {final_stock}, 预期库存 {expected_stock}")
    
    def test_rollback_mechanism(self):
        """测试库存回滚机制"""
        print("\n=== 测试库存回滚机制 ===")
        
        # 先预减一些库存
        prededuct_url = f"{self.show_service_url}/api/ticket/redis/prededuct"
        rollback_url = f"{self.show_service_url}/api/ticket/redis/rollback"
        
        # 记录初始库存
        stock_key = f"stock:ticket:{self.test_ticket_id}"
        initial_stock = int(self.redis_client.get(stock_key) or 0)
        
        # 预减20张票
        prededuct_quantity = 20
        response = requests.post(prededuct_url, params={
            "ticketId": self.test_ticket_id,
            "quantity": prededuct_quantity
        })
        
        if response.status_code == 200 and response.json().get('data') == 1:
            print(f"✓ 预减 {prededuct_quantity} 张票成功")
            
            # 检查预减后库存
            after_prededuct_stock = int(self.redis_client.get(stock_key))
            expected_after_prededuct = initial_stock - prededuct_quantity
            
            if after_prededuct_stock == expected_after_prededuct:
                print(f"✓ 预减后库存正确: {after_prededuct_stock}")
            else:
                print(f"✗ 预减后库存错误: 实际 {after_prededuct_stock}, 预期 {expected_after_prededuct}")
            
            # 回滚10张票
            rollback_quantity = 10
            response = requests.post(rollback_url, params={
                "ticketId": self.test_ticket_id,
                "quantity": rollback_quantity
            })
            
            if response.status_code == 200:
                result = response.json()
                if result.get('data') is True:
                    print(f"✓ 回滚 {rollback_quantity} 张票成功")
                    
                    # 检查回滚后库存
                    after_rollback_stock = int(self.redis_client.get(stock_key))
                    expected_after_rollback = after_prededuct_stock + rollback_quantity
                    
                    if after_rollback_stock == expected_after_rollback:
                        print(f"✓ 回滚后库存正确: {after_rollback_stock}")
                    else:
                        print(f"✗ 回滚后库存错误: 实际 {after_rollback_stock}, 预期 {expected_after_rollback}")
                else:
                    print(f"✗ 回滚失败: {result.get('message')}")
            else:
                print(f"✗ 回滚请求失败: {response.status_code}")
        else:
            print(f"✗ 预减失败，无法进行回滚测试")
    
    def test_stock_sharding_strategy(self):
        """检查库存分片策略实现"""
        print("\n=== 检查库存分片策略实现 ===")
        
        # 检查是否有分片相关的Redis键
        shard_patterns = [
            f"stock:ticket:{self.test_ticket_id}:shard:*",
            f"stock:shard:*:ticket:{self.test_ticket_id}",
            f"shard:stock:ticket:{self.test_ticket_id}:*"
        ]
        
        found_shards = False
        for pattern in shard_patterns:
            keys = self.redis_client.keys(pattern)
            if keys:
                print(f"✓ 发现分片键: {keys}")
                found_shards = True
        
        if not found_shards:
            print("⚠ 未发现库存分片策略实现，当前使用单一键存储")
            print(f"当前库存键: stock:ticket:{self.test_ticket_id}")
    
    def test_monitoring_and_alerts(self):
        """检查库存监控和告警功能"""
        print("\n=== 检查库存监控和告警功能 ===")
        
        # 检查是否有监控相关的Redis键或日志
        monitoring_patterns = [
            "monitor:stock:*",
            "alert:stock:*",
            "metrics:stock:*"
        ]
        
        found_monitoring = False
        for pattern in monitoring_patterns:
            keys = self.redis_client.keys(pattern)
            if keys:
                print(f"✓ 发现监控键: {keys}")
                found_monitoring = True
        
        if not found_monitoring:
            print("⚠ 未发现专门的库存监控和告警功能")
            print("建议实现库存监控指标收集和告警机制")
    
    def run_all_tests(self):
        """运行所有测试"""
        print("开始Redis库存管理功能测试...")
        print("=" * 50)
        
        try:
            # 初始化测试数据
            self.setup_test_data()
            
            # 运行各项测试
            self.test_redis_prededuct_logic()
            self.test_lua_script_atomicity()
            self.test_rollback_mechanism()
            self.test_stock_sharding_strategy()
            self.test_monitoring_and_alerts()
            
            print("\n=" * 50)
            print("所有测试完成！")
            
        except Exception as e:
            print(f"\n测试过程中发生错误: {e}")
            import traceback
            traceback.print_exc()

if __name__ == "__main__":
    tester = RedisStockTester()
    tester.run_all_tests()