#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
230人并发选座测试程序
针对ID为2的4小时时段票型进行并发测试
"""

import asyncio
import aiohttp
import time
import random
import json
from datetime import datetime
from typing import Dict, List, Any
import statistics

class ConcurrentBookingTest:
    def __init__(self):
        self.base_url = "http://localhost:8082"
        self.ticket_id = 2  # 4小时时段票型
        self.concurrent_users = 230
        self.max_quantity_per_user = 3
        self.results = []
        self.start_time = None
        self.end_time = None
        
    async def check_service_availability(self, session: aiohttp.ClientSession) -> bool:
        """检查服务可用性"""
        try:
            print("检查服务可用性...")
            test_url = f"{self.base_url}/api/ticket/lock?ticketId=999&quantity=1"
            async with session.put(test_url, timeout=10) as response:
                print(f"服务可用 (状态码: {response.status})")
                return True
        except Exception as e:
            print(f"服务不可用: {e}")
            return False
    
    async def get_current_stock(self, session: aiohttp.ClientSession) -> Dict[str, Any]:
        """获取当前库存信息"""
        try:
            stock_url = f"{self.base_url}/api/ticket/stock/{self.ticket_id}"
            async with session.get(stock_url, timeout=10) as response:
                if response.status == 200:
                    return await response.json()
                return None
        except Exception as e:
            print(f"获取库存信息失败: {e}")
            return None
    
    async def simulate_user_booking(self, session: aiohttp.ClientSession, user_id: int) -> Dict[str, Any]:
        """模拟单个用户的预定流程"""
        result = {
            'user_id': user_id,
            'success': False,
            'actions': [],
            'total_time': 0,
            'tickets_booked': 0
        }
        
        start_time = time.time()
        
        try:
            # 随机选择预定数量 (1-3张)
            quantity = random.randint(1, self.max_quantity_per_user)
            
            # 步骤1: 锁定库存
            lock_url = f"{self.base_url}/api/ticket/lock?ticketId={self.ticket_id}&quantity={quantity}"
            lock_start = time.time()
            
            async with session.put(lock_url, timeout=30) as lock_response:
                lock_end = time.time()
                lock_time = (lock_end - lock_start) * 1000  # 转换为毫秒
                
                action_result = {
                    'action': 'Lock',
                    'quantity': quantity,
                    'status_code': lock_response.status,
                    'response_time': lock_time,
                    'success': lock_response.status == 200
                }
                result['actions'].append(action_result)
                
                if lock_response.status == 200:
                    # 模拟用户思考时间 (0.5-2秒)
                    await asyncio.sleep(random.uniform(0.5, 2.0))
                    
                    # 85%概率确认，15%概率取消
                    should_confirm = random.random() <= 0.85
                    
                    if should_confirm:
                        # 确认预定（扣减库存）
                        deduct_url = f"{self.base_url}/api/ticket/deduct?ticketId={self.ticket_id}&quantity={quantity}"
                        deduct_start = time.time()
                        
                        async with session.put(deduct_url, timeout=30) as deduct_response:
                            deduct_end = time.time()
                            deduct_time = (deduct_end - deduct_start) * 1000
                            
                            deduct_action = {
                                'action': 'Deduct',
                                'quantity': quantity,
                                'status_code': deduct_response.status,
                                'response_time': deduct_time,
                                'success': deduct_response.status == 200
                            }
                            result['actions'].append(deduct_action)
                            
                            if deduct_response.status == 200:
                                result['success'] = True
                                result['tickets_booked'] = quantity
                    else:
                        # 取消预定
                        unlock_url = f"{self.base_url}/api/ticket/unlock?ticketId={self.ticket_id}&quantity={quantity}"
                        unlock_start = time.time()
                        
                        async with session.put(unlock_url, timeout=30) as unlock_response:
                            unlock_end = time.time()
                            unlock_time = (unlock_end - unlock_start) * 1000
                            
                            unlock_action = {
                                'action': 'Unlock',
                                'quantity': quantity,
                                'status_code': unlock_response.status,
                                'response_time': unlock_time,
                                'success': unlock_response.status == 200
                            }
                            result['actions'].append(unlock_action)
        
        except Exception as e:
            error_action = {
                'action': 'Error',
                'error': str(e),
                'success': False
            }
            result['actions'].append(error_action)
        
        end_time = time.time()
        result['total_time'] = (end_time - start_time) * 1000  # 转换为毫秒
        
        return result
    
    async def run_concurrent_test(self):
        """运行并发测试"""
        print("=== 230人并发选座测试 - 4小时时段票型 ===")
        print(f"测试参数:")
        print(f"  并发用户数: {self.concurrent_users}")
        print(f"  目标服务: {self.base_url}")
        print(f"  票务ID: {self.ticket_id} (4小时时段票型)")
        print(f"  每用户最大预定量: {self.max_quantity_per_user}")
        print()
        
        # 创建HTTP会话
        connector = aiohttp.TCPConnector(limit=300, limit_per_host=300)
        timeout = aiohttp.ClientTimeout(total=60)
        
        async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
            # 检查服务可用性
            if not await self.check_service_availability(session):
                print("服务不可用，测试终止")
                return
            
            # 获取测试前库存
            print("获取测试前库存信息...")
            initial_stock = await self.get_current_stock(session)
            if initial_stock:
                print(f"测试前库存: 总库存={initial_stock.get('totalStock', 'N/A')}, "
                      f"可用库存={initial_stock.get('availableStock', 'N/A')}, "
                      f"锁定库存={initial_stock.get('lockedStock', 'N/A')}")
            
            self.start_time = datetime.now()
            
            # 创建并发任务
            print(f"启动 {self.concurrent_users} 个并发用户...")
            tasks = []
            
            # 分批启动用户，避免系统过载
            batch_size = 50
            batches = (self.concurrent_users + batch_size - 1) // batch_size
            
            for batch in range(batches):
                start_index = batch * batch_size
                end_index = min((batch + 1) * batch_size, self.concurrent_users)
                
                print(f"启动第 {batch + 1} 批用户 (用户 {start_index + 1} - {end_index})...")
                
                batch_tasks = []
                for i in range(start_index, end_index):
                    task = asyncio.create_task(self.simulate_user_booking(session, i + 1))
                    batch_tasks.append(task)
                    tasks.append(task)
                
                # 批次间稍作延迟
                if batch < batches - 1:
                    await asyncio.sleep(0.2)
            
            print("等待所有用户完成预定...")
            
            # 等待所有任务完成并显示进度
            completed = 0
            while completed < len(tasks):
                done_tasks = [task for task in tasks if task.done()]
                new_completed = len(done_tasks)
                
                if new_completed > completed:
                    completed = new_completed
                    print(f"已完成: {completed} / {len(tasks)}")
                
                await asyncio.sleep(0.5)
            
            # 收集结果
            self.results = []
            for task in tasks:
                try:
                    result = await task
                    if result:
                        self.results.append(result)
                except Exception as e:
                    print(f"任务执行错误: {e}")
            
            self.end_time = datetime.now()
            
            # 获取测试后库存
            print("获取测试后库存信息...")
            final_stock = await self.get_current_stock(session)
            
            # 生成测试报告
            self.generate_test_report(initial_stock, final_stock)
    
    def generate_test_report(self, initial_stock: Dict[str, Any], final_stock: Dict[str, Any]):
        """生成测试报告"""
        duration = (self.end_time - self.start_time).total_seconds()
        
        # 统计结果
        success_count = sum(1 for result in self.results if result['success'])
        failure_count = len(self.results) - success_count
        total_tickets_booked = sum(result['tickets_booked'] for result in self.results)
        
        print("\n=== 测试报告 ===")
        print(f"测试时间: {self.start_time.strftime('%Y-%m-%d %H:%M:%S')} - {self.end_time.strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"测试持续时间: {duration:.2f} 秒")
        
        print("\n并发用户统计:")
        print(f"  总用户数: {self.concurrent_users}")
        print(f"  成功用户数: {success_count}")
        print(f"  失败用户数: {failure_count}")
        print(f"  成功率: {(success_count / self.concurrent_users * 100):.2f}%")
        
        print("\n票务预定统计:")
        print(f"  成功预定票数: {total_tickets_booked}")
        
        if initial_stock and final_stock:
            stock_change = initial_stock.get('availableStock', 0) - final_stock.get('availableStock', 0)
            print("\n库存变化:")
            print(f"  测试前可用库存: {initial_stock.get('availableStock', 'N/A')}")
            print(f"  测试后可用库存: {final_stock.get('availableStock', 'N/A')}")
            print(f"  库存减少: {stock_change}")
            print(f"  测试后锁定库存: {final_stock.get('lockedStock', 'N/A')}")
        
        # 响应时间统计
        lock_times = []
        deduct_times = []
        
        for result in self.results:
            for action in result['actions']:
                if action['action'] == 'Lock' and action['success']:
                    lock_times.append(action['response_time'])
                elif action['action'] == 'Deduct' and action['success']:
                    deduct_times.append(action['response_time'])
        
        if lock_times:
            print("\n锁定操作响应时间:")
            print(f"  平均响应时间: {statistics.mean(lock_times):.2f} ms")
            print(f"  最小响应时间: {min(lock_times):.2f} ms")
            print(f"  最大响应时间: {max(lock_times):.2f} ms")
            print(f"  中位数响应时间: {statistics.median(lock_times):.2f} ms")
        
        if deduct_times:
            print("\n扣减操作响应时间:")
            print(f"  平均响应时间: {statistics.mean(deduct_times):.2f} ms")
            print(f"  最小响应时间: {min(deduct_times):.2f} ms")
            print(f"  最大响应时间: {max(deduct_times):.2f} ms")
            print(f"  中位数响应时间: {statistics.median(deduct_times):.2f} ms")
        
        # 保存详细结果到文件
        self.save_detailed_results()
        
        print("\n=== 测试完成 ===")
    
    def save_detailed_results(self):
        """保存详细测试结果到JSON文件"""
        try:
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            filename = f"concurrent_test_results_{timestamp}.json"
            
            detailed_results = {
                'test_info': {
                    'start_time': self.start_time.isoformat(),
                    'end_time': self.end_time.isoformat(),
                    'concurrent_users': self.concurrent_users,
                    'ticket_id': self.ticket_id,
                    'max_quantity_per_user': self.max_quantity_per_user
                },
                'results': self.results
            }
            
            with open(filename, 'w', encoding='utf-8') as f:
                json.dump(detailed_results, f, ensure_ascii=False, indent=2)
            
            print(f"\n详细测试结果已保存到: {filename}")
        except Exception as e:
            print(f"保存测试结果失败: {e}")

async def main():
    """主函数"""
    test = ConcurrentBookingTest()
    await test.run_concurrent_test()

if __name__ == "__main__":
    # 运行测试
    asyncio.run(main())