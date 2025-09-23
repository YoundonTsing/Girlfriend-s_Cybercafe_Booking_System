#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
票务系统性能压力测试脚本
用于收集基线性能数据，识别系统瓶颈
"""

import asyncio
import aiohttp
import time
import json
import random
from datetime import datetime
from concurrent.futures import ThreadPoolExecutor
import statistics

class PerformanceTestSuite:
    def __init__(self, base_url="http://localhost:8000"):
        self.base_url = base_url
        self.results = {
            'user_operations': [],
            'order_operations': [],
            'stock_operations': [],
            'payment_operations': []
        }
        
    async def test_user_registration_login(self, session, user_id):
        """测试用户注册和登录性能"""
        start_time = time.time()
        
        # 注册用户
        timestamp = int(time.time())
        random_suffix = random.randint(1000, 9999)
        register_data = {
            "username": f"testuser_{timestamp}_{random_suffix}",
            "password": "123456",
            "nickname": f"测试用户_{random_suffix}",
            "phone": f"138{random.randint(10000000, 99999999)}",
            "email": f"test_{timestamp}_{random_suffix}@example.com"
        }
        
        try:
            async with session.post(f"{self.base_url}/api/user/register", 
                                   json=register_data) as resp:
                register_result = await resp.json()
                
            # 登录用户
            login_data = {
                "username": register_data["username"],
                "password": "123456"
            }
            
            async with session.post(f"{self.base_url}/api/user/login", 
                                   json=login_data) as resp:
                login_result = await resp.json()
                
            end_time = time.time()
            response_time = (end_time - start_time) * 1000  # ms
            
            self.results['user_operations'].append({
                'operation': 'register_login',
                'response_time': response_time,
                'success': resp.status == 200,
                'timestamp': datetime.now().isoformat()
            })
            
            return login_result.get('data', {}).get('token')
            
        except Exception as e:
            print(f"用户操作失败: {e}")
            return None
    
    async def test_order_creation(self, session, token, user_id):
        """测试订单创建性能"""
        start_time = time.time()
        
        headers = {'Authorization': f'Bearer {token}'} if token else {}
        order_data = {
            "userId": user_id,
            "showId": random.randint(1, 10),
            "sessionId": 1,
            "ticketId": random.randint(1, 5),
            "quantity": random.randint(1, 4),
            "contactPhone": "13800138000",
            "remark": "性能测试订单"
        }
        
        try:
            async with session.post(f"{self.base_url}/api/order/create", 
                                   json=order_data, headers=headers) as resp:
                result = await resp.json()
                
            end_time = time.time()
            response_time = (end_time - start_time) * 1000  # ms
            
            success = resp.status == 200 and result.get('code') == 200
            
            self.results['order_operations'].append({
                'operation': 'create_order',
                'response_time': response_time,
                'success': success,
                'timestamp': datetime.now().isoformat()
            })
            
            if not success:
                print(f"订单创建失败 - 用户ID: {user_id}, HTTP状态: {resp.status}, 响应: {result}")
            
            return result.get('data') if result.get('code') == 200 else None
            
        except Exception as e:
            print(f"订单创建异常 - 用户ID: {user_id}, 错误: {e}")
            return None
    
    async def test_stock_operations(self, session, ticket_type_id, token=None):
        """测试库存查询和锁定性能"""
        
        headers = {'Authorization': f'Bearer {token}'} if token else {}
        
        try:
            # 查询库存
            start_time = time.time()
            async with session.get(f"{self.base_url}/api/ticket/stock/{ticket_type_id}", headers=headers) as resp:
                stock_result = await resp.json()
                end_time = time.time()
                
                success = resp.status == 200 and stock_result.get('code') == 200
                
                self.results['stock_operations'].append({
                    'operation': 'query_stock',
                    'response_time': (end_time - start_time) * 1000,
                    'success': success,
                    'timestamp': datetime.now().isoformat()
                })
                
                if not success:
                    print(f"库存查询失败: {resp.status}, {stock_result}")
            
            # Redis预减库存
            start_time = time.time()
            quantity = random.randint(1, 3)
            async with session.post(f"{self.base_url}/api/ticket/redis/prededuct?ticketId={ticket_type_id}&quantity={quantity}", headers=headers) as resp:
                prededuct_result = await resp.json()
                end_time = time.time()
                
                success = resp.status == 200 and prededuct_result.get('code') == 200
                
                self.results['stock_operations'].append({
                    'operation': 'prededuct_stock',
                    'response_time': (end_time - start_time) * 1000,
                    'success': success,
                    'timestamp': datetime.now().isoformat()
                })
                
                if not success:
                    print(f"预减库存失败: {resp.status}, {prededuct_result}")
            
        except Exception as e:
            print(f"库存操作失败: {e}")
    
    async def test_payment_processing(self, session, token, order_id):
        """测试支付处理性能"""
        if not order_id:
            return
            
        start_time = time.time()
        
        headers = {'Authorization': f'Bearer {token}'} if token else {}
        payment_data = {
            "orderId": order_id,
            "paymentMethod": "ALIPAY",
            "amount": random.uniform(100, 500)
        }
        
        try:
            async with session.post(f"{self.base_url}/api/order/pay", 
                                   json=payment_data, headers=headers) as resp:
                result = await resp.json()
                
            end_time = time.time()
            response_time = (end_time - start_time) * 1000  # ms
            
            self.results['payment_operations'].append({
                'operation': 'process_payment',
                'response_time': response_time,
                'success': resp.status == 200,
                'timestamp': datetime.now().isoformat()
            })
            
        except Exception as e:
            print(f"支付处理失败: {e}")
    
    async def run_concurrent_test(self, concurrent_users=50, test_duration=300):
        """运行并发测试"""
        print(f"开始压力测试: {concurrent_users}个并发用户, 持续{test_duration}秒")
        
        connector = aiohttp.TCPConnector(limit=100, limit_per_host=50)
        timeout = aiohttp.ClientTimeout(total=30)
        
        async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
            tasks = []
            
            for user_id in range(concurrent_users):
                task = asyncio.create_task(self.simulate_user_journey(session, user_id))
                tasks.append(task)
            
            # 等待所有任务完成或超时
            try:
                await asyncio.wait_for(asyncio.gather(*tasks, return_exceptions=True), 
                                     timeout=test_duration)
            except asyncio.TimeoutError:
                print("测试超时，正在收集结果...")
    
    async def simulate_user_journey(self, session, user_id):
        """模拟完整的用户购票流程"""
        try:
            # 生成唯一的用户ID，避免分布式锁冲突
            unique_user_id = int(time.time() * 1000000) + user_id + random.randint(1000, 9999)
            
            # 1. 用户注册登录
            token = await self.test_user_registration_login(session, unique_user_id)
            
            # 2. 创建订单
            order_id = await self.test_order_creation(session, token, unique_user_id)
            
            # 3. 库存操作
            await self.test_stock_operations(session, random.randint(1, 5), token)
            
            # 4. 支付处理
            await self.test_payment_processing(session, token, order_id)
            
            # 模拟用户思考时间
            await asyncio.sleep(random.uniform(0.5, 2.0))
            
        except Exception as e:
            print(f"用户{user_id}流程执行失败: {e}")
    
    def generate_performance_report(self):
        """生成性能测试报告"""
        report = {
            'test_summary': {
                'timestamp': datetime.now().isoformat(),
                'total_operations': sum(len(ops) for ops in self.results.values())
            },
            'performance_metrics': {}
        }
        
        for operation_type, operations in self.results.items():
            if not operations:
                continue
                
            response_times = [op['response_time'] for op in operations]
            success_count = sum(1 for op in operations if op['success'])
            
            metrics = {
                'total_requests': len(operations),
                'successful_requests': success_count,
                'success_rate': (success_count / len(operations)) * 100,
                'avg_response_time': statistics.mean(response_times),
                'median_response_time': statistics.median(response_times),
                'p95_response_time': self.percentile(response_times, 95),
                'p99_response_time': self.percentile(response_times, 99),
                'min_response_time': min(response_times),
                'max_response_time': max(response_times)
            }
            
            report['performance_metrics'][operation_type] = metrics
        
        return report
    
    def percentile(self, data, percentile):
        """计算百分位数"""
        if not data:
            return 0
        sorted_data = sorted(data)
        index = (percentile / 100) * (len(sorted_data) - 1)
        if index.is_integer():
            return sorted_data[int(index)]
        else:
            lower = sorted_data[int(index)]
            upper = sorted_data[int(index) + 1]
            return lower + (upper - lower) * (index - int(index))
    
    def save_report(self, report, filename=None):
        """保存测试报告"""
        if not filename:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            filename = f"performance_report_{timestamp}.json"
        
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(report, f, indent=2, ensure_ascii=False)
        
        print(f"性能测试报告已保存到: {filename}")
        return filename

async def main():
    """主函数"""
    # 测试配置 - 使用轻量级配置验证修复效果
    test_configs = [
        {'concurrent_users': 5, 'duration': 30},   # 轻负载测试
    ]
    
    for i, config in enumerate(test_configs, 1):
        print(f"\n=== 执行测试场景 {i}/{len(test_configs)} ===")
        
        test_suite = PerformanceTestSuite()
        await test_suite.run_concurrent_test(
            concurrent_users=config['concurrent_users'],
            test_duration=config['duration']
        )
        
        # 生成报告
        report = test_suite.generate_performance_report()
        filename = f"performance_report_scenario_{i}.json"
        test_suite.save_report(report, filename)
        
        # 打印关键指标
        print(f"\n场景 {i} 测试结果:")
        for op_type, metrics in report['performance_metrics'].items():
            print(f"{op_type}:")
            print(f"  成功率: {metrics['success_rate']:.2f}%")
            print(f"  平均响应时间: {metrics['avg_response_time']:.2f}ms")
            print(f"  P95响应时间: {metrics['p95_response_time']:.2f}ms")
        
        # 测试间隔
        if i < len(test_configs):
            print("等待30秒后开始下一个测试场景...")
            await asyncio.sleep(30)

if __name__ == "__main__":
    asyncio.run(main())