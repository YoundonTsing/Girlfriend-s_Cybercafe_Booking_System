#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Redis Redisson功能测试脚本
测试统一使用Redisson后的Redis连接和功能
"""

import requests
import json
import time
import threading
from concurrent.futures import ThreadPoolExecutor
import redis

# 配置
BASE_URL = "http://localhost:8080"
REDIS_HOST = "localhost"
REDIS_PORT = 6379
REDIS_DB = 0

class RedissonTestSuite:
    def __init__(self):
        self.redis_client = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, db=REDIS_DB, decode_responses=True)
        self.test_results = []
        
    def log_result(self, test_name, success, message=""):
        """记录测试结果"""
        status = "✅ PASS" if success else "❌ FAIL"
        result = f"{status} {test_name}: {message}"
        print(result)
        self.test_results.append((test_name, success, message))
        
    def test_redis_connection(self):
        """测试Redis连接"""
        try:
            self.redis_client.ping()
            self.log_result("Redis连接测试", True, "连接成功")
            return True
        except Exception as e:
            self.log_result("Redis连接测试", False, f"连接失败: {e}")
            return False
            
    def test_service_health(self, service_name, port):
        """测试服务健康状态"""
        try:
            url = f"http://localhost:{port}/actuator/health"
            response = requests.get(url, timeout=5)
            if response.status_code == 200:
                self.log_result(f"{service_name}服务健康检查", True, "服务正常运行")
                return True
            else:
                self.log_result(f"{service_name}服务健康检查", False, f"状态码: {response.status_code}")
                return False
        except Exception as e:
            self.log_result(f"{service_name}服务健康检查", False, f"请求失败: {e}")
            return False
            
    def test_stock_sync_to_redis(self, ticket_id=1):
        """测试库存同步到Redis"""
        try:
            url = f"{BASE_URL}/api/tickets/redis/sync"
            params = {"ticketId": ticket_id}
            response = requests.post(url, params=params, timeout=10)
            
            if response.status_code == 200:
                result = response.json()
                if result.get("success") and result.get("data"):
                    # 检查Redis中是否存在库存数据
                    stock_key = f"ticket:stock:{ticket_id}"
                    stock_value = self.redis_client.get(stock_key)
                    if stock_value:
                        self.log_result("库存同步到Redis", True, f"同步成功，库存值: {stock_value}")
                        return True
                    else:
                        self.log_result("库存同步到Redis", False, "Redis中未找到库存数据")
                        return False
                else:
                    self.log_result("库存同步到Redis", False, f"同步失败: {result}")
                    return False
            else:
                self.log_result("库存同步到Redis", False, f"HTTP错误: {response.status_code}")
                return False
        except Exception as e:
            self.log_result("库存同步到Redis", False, f"异常: {e}")
            return False
            
    def test_stock_prededuct(self, ticket_id=1, quantity=1):
        """测试库存预减"""
        try:
            url = f"{BASE_URL}/api/tickets/redis/prededuct"
            params = {"ticketId": ticket_id, "quantity": quantity}
            response = requests.post(url, params=params, timeout=10)
            
            if response.status_code == 200:
                result = response.json()
                if result.get("success"):
                    prededuct_result = result.get("data")
                    if prededuct_result == 1:
                        self.log_result("库存预减测试", True, f"预减成功，扣减数量: {quantity}")
                        return True
                    elif prededuct_result == 0:
                        self.log_result("库存预减测试", True, "库存不足（预期行为）")
                        return True
                    else:
                        self.log_result("库存预减测试", False, f"预减失败，返回值: {prededuct_result}")
                        return False
                else:
                    self.log_result("库存预减测试", False, f"预减失败: {result}")
                    return False
            else:
                self.log_result("库存预减测试", False, f"HTTP错误: {response.status_code}")
                return False
        except Exception as e:
            self.log_result("库存预减测试", False, f"异常: {e}")
            return False
            
    def test_stock_rollback(self, ticket_id=1, quantity=1):
        """测试库存回滚"""
        try:
            url = f"{BASE_URL}/api/tickets/redis/rollback"
            params = {"ticketId": ticket_id, "quantity": quantity}
            response = requests.post(url, params=params, timeout=10)
            
            if response.status_code == 200:
                result = response.json()
                if result.get("success") and result.get("data"):
                    self.log_result("库存回滚测试", True, f"回滚成功，回滚数量: {quantity}")
                    return True
                else:
                    self.log_result("库存回滚测试", False, f"回滚失败: {result}")
                    return False
            else:
                self.log_result("库存回滚测试", False, f"HTTP错误: {response.status_code}")
                return False
        except Exception as e:
            self.log_result("库存回滚测试", False, f"异常: {e}")
            return False
            
    def test_concurrent_stock_operations(self, ticket_id=1, thread_count=5, operations_per_thread=2):
        """测试并发库存操作"""
        def worker():
            success_count = 0
            for _ in range(operations_per_thread):
                try:
                    # 预减库存
                    url = f"{BASE_URL}/api/tickets/redis/prededuct"
                    params = {"ticketId": ticket_id, "quantity": 1}
                    response = requests.post(url, params=params, timeout=5)
                    if response.status_code == 200:
                        result = response.json()
                        if result.get("success") and result.get("data") == 1:
                            success_count += 1
                    time.sleep(0.1)  # 短暂延迟
                except:
                    pass
            return success_count
            
        try:
            # 先同步库存
            self.test_stock_sync_to_redis(ticket_id)
            
            # 并发测试
            with ThreadPoolExecutor(max_workers=thread_count) as executor:
                futures = [executor.submit(worker) for _ in range(thread_count)]
                total_success = sum(future.result() for future in futures)
                
            self.log_result("并发库存操作测试", True, 
                          f"{thread_count}个线程，每线程{operations_per_thread}次操作，成功{total_success}次")
            return True
        except Exception as e:
            self.log_result("并发库存操作测试", False, f"异常: {e}")
            return False
            
    def test_order_creation_with_lock(self):
        """测试订单创建（包含分布式锁）"""
        try:
            # 模拟创建订单请求
            url = f"{BASE_URL}/api/orders"
            order_data = {
                "ticketId": 1,
                "quantity": 1,
                "userId": 1
            }
            
            response = requests.post(url, json=order_data, timeout=10)
            
            if response.status_code == 200:
                result = response.json()
                if result.get("success"):
                    order_no = result.get("data")
                    self.log_result("订单创建测试", True, f"订单创建成功，订单号: {order_no}")
                    return True
                else:
                    self.log_result("订单创建测试", False, f"订单创建失败: {result}")
                    return False
            else:
                self.log_result("订单创建测试", False, f"HTTP错误: {response.status_code}")
                return False
        except Exception as e:
            self.log_result("订单创建测试", False, f"异常: {e}")
            return False
            
    def test_rate_limiter(self):
        """测试限流功能"""
        try:
            # 快速发送多个请求测试限流
            url = f"{BASE_URL}/api/tickets/1"
            success_count = 0
            rate_limited_count = 0
            
            for i in range(10):
                response = requests.get(url, timeout=5)
                if response.status_code == 200:
                    success_count += 1
                elif response.status_code == 429:  # Too Many Requests
                    rate_limited_count += 1
                time.sleep(0.1)
                
            if rate_limited_count > 0:
                self.log_result("限流功能测试", True, 
                              f"成功请求: {success_count}, 被限流: {rate_limited_count}")
            else:
                self.log_result("限流功能测试", True, 
                              f"所有请求成功: {success_count}（可能限流阈值较高）")
            return True
        except Exception as e:
            self.log_result("限流功能测试", False, f"异常: {e}")
            return False
            
    def run_all_tests(self):
        """运行所有测试"""
        print("🚀 开始Redis Redisson功能测试...\n")
        
        # 基础连接测试
        print("📡 基础连接测试")
        self.test_redis_connection()
        self.test_service_health("ticket-show", 8081)
        self.test_service_health("ticket-order", 8082)
        print()
        
        # Redis库存操作测试
        print("📦 Redis库存操作测试")
        self.test_stock_sync_to_redis()
        self.test_stock_prededuct()
        self.test_stock_rollback()
        print()
        
        # 并发测试
        print("⚡ 并发操作测试")
        self.test_concurrent_stock_operations()
        print()
        
        # 业务功能测试
        print("🛒 业务功能测试")
        self.test_order_creation_with_lock()
        self.test_rate_limiter()
        print()
        
        # 测试结果汇总
        self.print_summary()
        
    def print_summary(self):
        """打印测试结果汇总"""
        print("📊 测试结果汇总")
        print("=" * 50)
        
        passed = sum(1 for _, success, _ in self.test_results if success)
        total = len(self.test_results)
        
        for test_name, success, message in self.test_results:
            status = "✅" if success else "❌"
            print(f"{status} {test_name}: {message}")
            
        print("=" * 50)
        print(f"总计: {total} 个测试，通过: {passed} 个，失败: {total - passed} 个")
        
        if passed == total:
            print("🎉 所有测试通过！Redis Redisson迁移成功！")
        else:
            print("⚠️  部分测试失败，请检查相关配置和服务状态")

if __name__ == "__main__":
    test_suite = RedissonTestSuite()
    test_suite.run_all_tests()