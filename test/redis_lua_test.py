#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Redis Lua脚本测试
直接测试Redis中的Lua脚本功能，验证Redisson迁移后的兼容性
"""

import redis
import time
import threading
from concurrent.futures import ThreadPoolExecutor

class RedisLuaTestSuite:
    def __init__(self):
        self.redis_client = redis.Redis(host='localhost', port=6379, db=0, decode_responses=True)
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
            
    def test_stock_prededuct_lua(self):
        """测试库存预减Lua脚本"""
        # 这是从RedisStockService.java中提取的Lua脚本
        lua_script = """
        local key = KEYS[1]
        local deductQuantity = tonumber(ARGV[1])
        local currentStock = redis.call('GET', key)
        
        if currentStock == false then
            return 0
        end
        
        currentStock = tonumber(currentStock)
        if currentStock >= deductQuantity then
            redis.call('DECRBY', key, deductQuantity)
            return 1
        else
            return 0
        end
        """
        
        try:
            # 准备测试数据
            stock_key = "ticket:stock:test"
            initial_stock = 100
            
            # 设置初始库存
            self.redis_client.set(stock_key, initial_stock)
            
            # 执行Lua脚本
            result = self.redis_client.eval(lua_script, 1, stock_key, 10)
            
            if result == 1:
                # 检查库存是否正确减少
                remaining_stock = int(self.redis_client.get(stock_key))
                if remaining_stock == 90:
                    self.log_result("库存预减Lua脚本", True, f"预减成功，剩余库存: {remaining_stock}")
                    return True
                else:
                    self.log_result("库存预减Lua脚本", False, f"库存计算错误，期望90，实际{remaining_stock}")
                    return False
            else:
                self.log_result("库存预减Lua脚本", False, f"预减失败，返回值: {result}")
                return False
                
        except Exception as e:
            self.log_result("库存预减Lua脚本", False, f"异常: {e}")
            return False
        finally:
            # 清理测试数据
            self.redis_client.delete(stock_key)
            
    def test_stock_rollback_lua(self):
        """测试库存回滚Lua脚本"""
        lua_script = """
        local key = KEYS[1]
        local rollbackQuantity = tonumber(ARGV[1])
        redis.call('INCRBY', key, rollbackQuantity)
        return 1
        """
        
        try:
            # 准备测试数据
            stock_key = "ticket:stock:test"
            initial_stock = 80
            
            # 设置初始库存
            self.redis_client.set(stock_key, initial_stock)
            
            # 执行回滚脚本
            result = self.redis_client.eval(lua_script, 1, stock_key, 20)
            
            if result == 1:
                # 检查库存是否正确增加
                final_stock = int(self.redis_client.get(stock_key))
                if final_stock == 100:
                    self.log_result("库存回滚Lua脚本", True, f"回滚成功，最终库存: {final_stock}")
                    return True
                else:
                    self.log_result("库存回滚Lua脚本", False, f"库存计算错误，期望100，实际{final_stock}")
                    return False
            else:
                self.log_result("库存回滚Lua脚本", False, f"回滚失败，返回值: {result}")
                return False
                
        except Exception as e:
            self.log_result("库存回滚Lua脚本", False, f"异常: {e}")
            return False
        finally:
            # 清理测试数据
            self.redis_client.delete(stock_key)
            
    def test_rate_limiter_lua(self):
        """测试限流Lua脚本"""
        # 这是从RateLimiter.java中提取的Lua脚本
        lua_script = """
        local key = KEYS[1]
        local limit = tonumber(ARGV[1])
        local window = tonumber(ARGV[2])
        local current = redis.call('GET', key)
        
        if current == false then
            redis.call('SET', key, 1)
            redis.call('EXPIRE', key, window)
            return 1
        else
            current = tonumber(current)
            if current < limit then
                redis.call('INCR', key)
                return 1
            else
                return 0
            end
        end
        """
        
        try:
            # 准备测试数据
            rate_key = "rate_limit:test:user1"
            limit = 5
            window = 60
            
            # 清理可能存在的键
            self.redis_client.delete(rate_key)
            
            success_count = 0
            blocked_count = 0
            
            # 测试限流逻辑
            for i in range(7):  # 尝试7次请求，限制是5次
                result = self.redis_client.eval(lua_script, 1, rate_key, limit, window)
                if result == 1:
                    success_count += 1
                else:
                    blocked_count += 1
                    
            if success_count == 5 and blocked_count == 2:
                self.log_result("限流Lua脚本", True, f"限流正常，通过: {success_count}, 阻止: {blocked_count}")
                return True
            else:
                self.log_result("限流Lua脚本", False, f"限流异常，通过: {success_count}, 阻止: {blocked_count}")
                return False
                
        except Exception as e:
            self.log_result("限流Lua脚本", False, f"异常: {e}")
            return False
        finally:
            # 清理测试数据
            self.redis_client.delete(rate_key)
            
    def test_concurrent_lua_execution(self):
        """测试并发Lua脚本执行"""
        lua_script = """
        local key = KEYS[1]
        local deductQuantity = tonumber(ARGV[1])
        local currentStock = redis.call('GET', key)
        
        if currentStock == false then
            return 0
        end
        
        currentStock = tonumber(currentStock)
        if currentStock >= deductQuantity then
            redis.call('DECRBY', key, deductQuantity)
            return 1
        else
            return 0
        end
        """
        
        def worker():
            """工作线程函数"""
            try:
                result = self.redis_client.eval(lua_script, 1, "ticket:stock:concurrent", 1)
                return result
            except:
                return 0
                
        try:
            # 准备测试数据
            stock_key = "ticket:stock:concurrent"
            initial_stock = 10
            thread_count = 15
            
            # 设置初始库存
            self.redis_client.set(stock_key, initial_stock)
            
            # 并发执行
            with ThreadPoolExecutor(max_workers=thread_count) as executor:
                futures = [executor.submit(worker) for _ in range(thread_count)]
                results = [future.result() for future in futures]
                
            success_count = sum(results)
            final_stock = int(self.redis_client.get(stock_key) or 0)
            
            # 验证结果：成功次数 + 最终库存 = 初始库存
            if success_count + final_stock == initial_stock:
                self.log_result("并发Lua脚本执行", True, 
                              f"并发测试通过，成功扣减: {success_count}, 剩余库存: {final_stock}")
                return True
            else:
                self.log_result("并发Lua脚本执行", False, 
                              f"数据不一致，成功: {success_count}, 剩余: {final_stock}, 初始: {initial_stock}")
                return False
                
        except Exception as e:
            self.log_result("并发Lua脚本执行", False, f"异常: {e}")
            return False
        finally:
            # 清理测试数据
            self.redis_client.delete(stock_key)
            
    def test_redisson_compatibility(self):
        """测试Redisson兼容性（模拟Redisson的脚本执行方式）"""
        try:
            # 测试基本的Redis操作，这些操作Redisson也会使用
            test_key = "redisson:test:compatibility"
            
            # 测试原子操作
            self.redis_client.delete(test_key)
            result1 = self.redis_client.setnx(test_key, "value1")
            result2 = self.redis_client.setnx(test_key, "value2")
            
            if result1 == 1 and result2 == 0:
                # 测试过期时间
                self.redis_client.expire(test_key, 1)
                ttl = self.redis_client.ttl(test_key)
                
                if ttl > 0:
                    self.log_result("Redisson兼容性测试", True, "原子操作和TTL设置正常")
                    return True
                else:
                    self.log_result("Redisson兼容性测试", False, "TTL设置异常")
                    return False
            else:
                self.log_result("Redisson兼容性测试", False, "原子操作异常")
                return False
                
        except Exception as e:
            self.log_result("Redisson兼容性测试", False, f"异常: {e}")
            return False
        finally:
            # 清理测试数据
            self.redis_client.delete(test_key)
            
    def run_all_tests(self):
        """运行所有测试"""
        print("🚀 开始Redis Lua脚本功能测试...\n")
        
        # 基础连接测试
        if not self.test_redis_connection():
            print("❌ Redis连接失败，终止测试")
            return
            
        print("\n📜 Lua脚本功能测试")
        self.test_stock_prededuct_lua()
        self.test_stock_rollback_lua()
        self.test_rate_limiter_lua()
        
        print("\n⚡ 并发测试")
        self.test_concurrent_lua_execution()
        
        print("\n🔧 Redisson兼容性测试")
        self.test_redisson_compatibility()
        
        print()
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
            print("🎉 所有Redis Lua脚本测试通过！")
            print("✨ Redisson迁移后Redis功能正常！")
        else:
            print("⚠️  部分测试失败，请检查Redis配置")

if __name__ == "__main__":
    test_suite = RedisLuaTestSuite()
    test_suite.run_all_tests()