#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Redis Lua脚本预减库存功能测试
测试stock_prededuct.lua脚本的各种场景
"""

import redis
import requests
import json
import time
from datetime import datetime

class LuaPredeductTester:
    def __init__(self):
        # Redis连接
        self.redis_client = redis.Redis(host='localhost', port=6379, db=0, decode_responses=True)
        
        # 微服务API基础URL
        self.show_service_url = "http://localhost:8002"
        
        # 测试数据
        self.test_ticket_id = 1
        self.stock_key = f"stock:ticket:{self.test_ticket_id}"
        
        # 测试结果
        self.test_results = []
        
    def log_test_result(self, test_name, success, details):
        """记录测试结果"""
        result = {
            "test_name": test_name,
            "success": success,
            "details": details,
            "timestamp": datetime.now().isoformat()
        }
        self.test_results.append(result)
        status = "✅ 成功" if success else "❌ 失败"
        print(f"[{status}] {test_name}: {details}")
        
    def clear_redis_data(self):
        """清理Redis测试数据"""
        try:
            # 删除库存key
            self.redis_client.delete(self.stock_key)
            # 删除调试信息
            self.redis_client.delete('debug:stock_operations')
            print(f"已清理Redis数据: {self.stock_key}")
        except Exception as e:
            print(f"清理Redis数据失败: {e}")
            
    def init_stock_via_api(self, stock_amount=100):
        """通过API初始化库存"""
        try:
            url = f"{self.show_service_url}/api/ticket/stock/init"
            data = {
                "ticketId": self.test_ticket_id,
                "availableStock": stock_amount,
                "forceUpdate": True
            }
            response = requests.post(url, json=data)
            
            if response.status_code == 200:
                result = response.json()
                success = result.get('success', False)
                self.log_test_result("API初始化库存", success, f"初始库存: {stock_amount}")
                return success
            else:
                self.log_test_result("API初始化库存", False, f"HTTP错误: {response.status_code}")
                return False
        except Exception as e:
            self.log_test_result("API初始化库存", False, f"异常: {str(e)}")
            return False
            
    def test_prededuct_via_api(self, quantity):
        """通过API测试预减库存"""
        try:
            url = f"{self.show_service_url}/api/ticket/redis/prededuct"
            data = {
                "ticketId": self.test_ticket_id,
                "quantity": quantity
            }
            response = requests.post(url, json=data)
            
            if response.status_code == 200:
                result = response.json()
                success = result.get('success', False)
                data_result = result.get('data', -1)
                
                # 解析预减结果
                if data_result == 1:
                    status = "成功"
                elif data_result == 0:
                    status = "库存不足"
                elif data_result == -1:
                    status = "库存不存在"
                else:
                    status = f"未知结果: {data_result}"
                    
                self.log_test_result(f"API预减库存({quantity})", success, f"结果: {status}")
                return data_result
            else:
                self.log_test_result(f"API预减库存({quantity})", False, f"HTTP错误: {response.status_code}")
                return -1
        except Exception as e:
            self.log_test_result(f"API预减库存({quantity})", False, f"异常: {str(e)}")
            return -1
            
    def check_redis_stock(self):
        """检查Redis中的库存"""
        try:
            stock = self.redis_client.get(self.stock_key)
            if stock is not None:
                stock_value = int(stock)
                self.log_test_result("检查Redis库存", True, f"当前库存: {stock_value}")
                return stock_value
            else:
                self.log_test_result("检查Redis库存", False, "库存不存在")
                return None
        except Exception as e:
            self.log_test_result("检查Redis库存", False, f"异常: {str(e)}")
            return None
            
    def check_debug_info(self):
        """检查Lua脚本的调试信息"""
        try:
            debug_info = self.redis_client.hgetall('debug:stock_operations')
            if debug_info:
                print("\n=== Lua脚本调试信息 ===")
                for key, value in debug_info.items():
                    print(f"{key}: {value}")
                print("========================\n")
                self.log_test_result("检查调试信息", True, f"找到{len(debug_info)}条调试记录")
            else:
                self.log_test_result("检查调试信息", False, "无调试信息")
        except Exception as e:
            self.log_test_result("检查调试信息", False, f"异常: {str(e)}")
            
    def run_comprehensive_test(self):
        """运行综合测试"""
        print("\n=== Redis Lua脚本预减库存功能测试 ===")
        print(f"测试时间: {datetime.now()}")
        print(f"测试票档ID: {self.test_ticket_id}")
        print("="*50)
        
        # 1. 清理环境
        print("\n1. 清理测试环境")
        self.clear_redis_data()
        
        # 2. 初始化库存
        print("\n2. 初始化库存测试")
        if not self.init_stock_via_api(100):
            print("初始化失败，终止测试")
            return
            
        # 3. 检查初始库存
        print("\n3. 检查初始库存")
        initial_stock = self.check_redis_stock()
        if initial_stock != 100:
            print(f"初始库存不正确，期望100，实际{initial_stock}")
            
        # 4. 正常预减测试
        print("\n4. 正常预减库存测试")
        result1 = self.test_prededuct_via_api(10)
        if result1 == 1:
            stock_after_deduct = self.check_redis_stock()
            if stock_after_deduct == 90:
                self.log_test_result("预减后库存检查", True, "库存正确减少")
            else:
                self.log_test_result("预减后库存检查", False, f"期望90，实际{stock_after_deduct}")
                
        # 5. 库存不足测试
        print("\n5. 库存不足测试")
        result2 = self.test_prededuct_via_api(100)  # 尝试扣减100，但只有90
        if result2 == 0:
            self.log_test_result("库存不足处理", True, "正确返回库存不足")
        else:
            self.log_test_result("库存不足处理", False, f"期望返回0，实际{result2}")
            
        # 6. 边界值测试
        print("\n6. 边界值测试")
        result3 = self.test_prededuct_via_api(90)  # 扣减剩余所有库存
        if result3 == 1:
            final_stock = self.check_redis_stock()
            if final_stock == 0:
                self.log_test_result("边界值测试", True, "成功扣减至0")
            else:
                self.log_test_result("边界值测试", False, f"期望0，实际{final_stock}")
                
        # 7. 零库存测试
        print("\n7. 零库存测试")
        result4 = self.test_prededuct_via_api(1)  # 尝试从0库存扣减
        if result4 == 0:
            self.log_test_result("零库存测试", True, "正确处理零库存")
        else:
            self.log_test_result("零库存测试", False, f"期望返回0，实际{result4}")
            
        # 8. 检查调试信息
        print("\n8. 检查Lua脚本调试信息")
        self.check_debug_info()
        
        # 9. 生成测试报告
        self.generate_report()
        
    def generate_report(self):
        """生成测试报告"""
        print("\n=== 测试报告 ===")
        total_tests = len(self.test_results)
        successful_tests = sum(1 for result in self.test_results if result['success'])
        failed_tests = total_tests - successful_tests
        
        print(f"总测试数: {total_tests}")
        print(f"成功: {successful_tests}")
        print(f"失败: {failed_tests}")
        print(f"成功率: {(successful_tests/total_tests*100):.1f}%")
        
        # 保存详细报告
        report_file = f"lua_prededuct_test_report_{int(time.time())}.json"
        with open(report_file, 'w', encoding='utf-8') as f:
            json.dump({
                "summary": {
                    "total_tests": total_tests,
                    "successful_tests": successful_tests,
                    "failed_tests": failed_tests,
                    "success_rate": successful_tests/total_tests*100
                },
                "test_results": self.test_results
            }, f, ensure_ascii=False, indent=2)
            
        print(f"\n详细报告已保存到: {report_file}")
        
if __name__ == "__main__":
    tester = LuaPredeductTester()
    tester.run_comprehensive_test()