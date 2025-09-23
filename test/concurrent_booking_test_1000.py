import asyncio
import aiohttp
import time
import json
import random
import statistics
from datetime import datetime

class ConcurrentBookingTest:
    def __init__(self, concurrent_users=1000, ticket_id=6, base_url="http://localhost:8000"):
        self.concurrent_users = concurrent_users
        self.ticket_id = ticket_id
        self.base_url = base_url
        self.max_quantity_per_user = 3
        self.results = []
        self.batch_size = 20  # 每批启动的用户数
        self.auth_token = None  # 认证token
        self.session = None  # 共享session
        
    async def login_and_get_token(self):
        """登录获取认证token"""
        login_data = {
            "username": "admin",
            "password": "123456"
        }
        try:
            async with self.session.post(f"{self.base_url}/api/user/login", json=login_data) as response:
                if response.status == 200:
                    result = await response.json()
                    if result.get('code') == 200:
                        self.auth_token = result['data']['token']
                        print("登录成功，获取到认证token")
                        return True
                    else:
                        print(f"登录失败: {result.get('message', '未知错误')}")
                        return False
                else:
                    print(f"登录请求失败，状态码: {response.status}")
                    return False
        except Exception as e:
            print(f"登录异常: {e}")
            return False

    async def check_service_availability(self):
        """检查服务是否可用"""
        try:
            self.session = aiohttp.ClientSession()
            # 先登录获取token
            if not await self.login_and_get_token():
                return False
                
            headers = {"Authorization": f"Bearer {self.auth_token}"}
            async with self.session.get(f"{self.base_url}/api/ticket/stock/{self.ticket_id}", headers=headers, timeout=10) as response:
                if response.status == 200:
                    data = await response.json()
                    print(f"服务可用，票ID {self.ticket_id} 当前库存: {data.get('availableStock', 'N/A')}")
                    return True
                else:
                    print(f"服务检查失败，状态码: {response.status}")
                    return False
        except Exception as e:
            print(f"服务检查异常: {e}")
            return False
    
    async def get_stock_info(self):
        """获取库存信息"""
        try:
            headers = {"Authorization": f"Bearer {self.auth_token}"}
            async with self.session.get(f"{self.base_url}/api/ticket/stock/{self.ticket_id}", headers=headers, timeout=10) as response:
                if response.status == 200:
                    return await response.json()
                else:
                    return None
        except Exception as e:
            print(f"获取库存信息失败: {e}")
            return None
    
    async def simulate_user_booking(self, user_id):
        """模拟单个用户的预定过程"""
        start_time = time.time()
        result = {
            'user_id': user_id,
            'success': False,
            'actions': [],
            'total_time': 0,
            'tickets_booked': 0
        }
        
        try:
            # 随机选择预定数量 (1-3张)
            quantity = random.randint(1, self.max_quantity_per_user)
            
            headers = {"Authorization": f"Bearer {self.auth_token}"}
            async with aiohttp.ClientSession() as session:
                # 步骤1: 锁定库存
                lock_url = f"{self.base_url}/api/ticket/lock?ticketId={self.ticket_id}&quantity={quantity}"
                lock_start = time.time()
                
                async with session.put(lock_url, headers=headers, timeout=60) as lock_response:
                    lock_end = time.time()
                    lock_time = (lock_end - lock_start) * 1000
                    
                    lock_action = {
                        'action': 'Lock',
                        'quantity': quantity,
                        'status_code': lock_response.status,
                        'response_time': lock_time,
                        'success': lock_response.status == 200
                    }
                    result['actions'].append(lock_action)
                    
                    if lock_response.status == 200:
                        # 模拟用户思考时间 (0.5-2秒)
                        await asyncio.sleep(random.uniform(0.5, 2.0))
                        
                        # 随机决定是否确认预定 (85%概率确认)
                        should_confirm = random.random() < 0.85
                        
                        if should_confirm:
                            # 确认预定（扣减库存）
                            deduct_url = f"{self.base_url}/api/ticket/deduct?ticketId={self.ticket_id}&quantity={quantity}"
                            deduct_start = time.time()
                            
                            async with session.put(deduct_url, headers=headers, timeout=60) as deduct_response:
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
                                    # 扣减失败，释放锁定的库存
                                    unlock_url = f"{self.base_url}/api/ticket/unlock?ticketId={self.ticket_id}&quantity={quantity}"
                                    unlock_start = time.time()
                                    
                                    async with session.put(unlock_url, headers=headers, timeout=60) as unlock_response:
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
                        else:
                            # 用户取消预定，释放锁定的库存
                            unlock_url = f"{self.base_url}/api/ticket/unlock?ticketId={self.ticket_id}&quantity={quantity}"
                            unlock_start = time.time()
                            
                            async with session.put(unlock_url, headers=headers, timeout=30) as unlock_response:
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
            result['error'] = str(e)
            print(f"用户 {user_id} 预定过程异常: {e}")
        
        end_time = time.time()
        result['total_time'] = (end_time - start_time) * 1000
        return result
    
    async def run_test(self):
        """运行并发测试"""
        print(f"开始 {self.concurrent_users} 人并发抢座测试...")
        print(f"目标票ID: {self.ticket_id}")
        print(f"每批启动用户数: {self.batch_size}")
        
        # 检查服务可用性
        if not await self.check_service_availability():
            print("服务不可用，测试终止")
            return
        
        # 获取测试前库存信息
        initial_stock = await self.get_stock_info()
        
        start_time = datetime.now()
        
        # 分批启动用户
        tasks = []
        batch_count = (self.concurrent_users + self.batch_size - 1) // self.batch_size
        
        for batch in range(batch_count):
            batch_start = batch * self.batch_size + 1
            batch_end = min((batch + 1) * self.batch_size, self.concurrent_users)
            
            print(f"启动第 {batch + 1} 批用户 (用户 {batch_start} - {batch_end})...")
            
            # 为当前批次创建任务
            batch_tasks = []
            for user_id in range(batch_start, batch_end + 1):
                task = asyncio.create_task(self.simulate_user_booking(user_id))
                batch_tasks.append(task)
                tasks.append(task)
            
            # 批次间稍微延迟，避免瞬间压力过大
            if batch < batch_count - 1:
                await asyncio.sleep(0.1)
        
        print("等待所有用户完成预定...")
        
        # 等待所有任务完成，并显示进度
        completed = 0
        while completed < len(tasks):
            await asyncio.sleep(1)
            new_completed = sum(1 for task in tasks if task.done())
            if new_completed > completed:
                completed = new_completed
                print(f"已完成: {completed} / {self.concurrent_users}")
        
        # 收集所有结果
        self.results = await asyncio.gather(*tasks)
        
        end_time = datetime.now()
        
        # 获取测试后库存信息
        print("获取测试后库存信息...")
        final_stock = await self.get_stock_info()
        
        # 关闭session
        if self.session:
            await self.session.close()
        
        # 生成测试报告
        await self.generate_report(start_time, end_time, initial_stock, final_stock)
    
    async def generate_report(self, start_time, end_time, initial_stock, final_stock):
        """生成测试报告"""
        duration = (end_time - start_time).total_seconds()
        
        # 统计成功和失败的用户
        successful_users = [r for r in self.results if r['success']]
        failed_users = [r for r in self.results if not r['success']]
        
        # 统计预定的票数
        total_tickets_booked = sum(r['tickets_booked'] for r in self.results)
        
        print("\n=== 测试报告 ===")
        print(f"测试时间: {start_time.strftime('%Y-%m-%d %H:%M:%S')} - {end_time.strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"测试持续时间: {duration:.2f} 秒")
        
        print(f"\n并发用户统计:")
        print(f"  总用户数: {self.concurrent_users}")
        print(f"  成功用户数: {len(successful_users)}")
        print(f"  失败用户数: {len(failed_users)}")
        print(f"  成功率: {len(successful_users)/self.concurrent_users*100:.2f}%")
        
        print(f"\n票务预定统计:")
        print(f"  成功预定票数: {total_tickets_booked}")
        
        print(f"\n库存变化:")
        if initial_stock:
            print(f"  测试前可用库存: {initial_stock.get('availableStock', 'N/A')}")
        else:
            print(f"  测试前可用库存: N/A")
        
        if final_stock:
            print(f"  测试后可用库存: {final_stock.get('availableStock', 'N/A')}")
        else:
            print(f"  测试后可用库存: N/A")
        
        if initial_stock and final_stock:
            initial_available = initial_stock.get('availableStock', 0)
            final_available = final_stock.get('availableStock', 0)
            stock_change = initial_available - final_available
            print(f"  库存减少: {stock_change}")
        else:
            print(f"  库存减少: 0")
        
        if final_stock:
            print(f"  测试后锁定库存: {final_stock.get('lockedStock', 'N/A')}")
        else:
            print(f"  测试后锁定库存: N/A")
        
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
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"concurrent_test_results_1000_{timestamp}.json"
        
        detailed_results = {
            'test_info': {
                'start_time': start_time.isoformat(),
                'end_time': end_time.isoformat(),
                'concurrent_users': self.concurrent_users,
                'ticket_id': self.ticket_id,
                'max_quantity_per_user': self.max_quantity_per_user
            },
            'results': self.results
        }
        
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(detailed_results, f, ensure_ascii=False, indent=2)
        
        print(f"\n详细测试结果已保存到: {filename}")
        print("\n=== 测试完成 ===")

async def main():
    # 创建1000人并发测试实例，测试票ID为6
    test = ConcurrentBookingTest(concurrent_users=1000, ticket_id=6)
    await test.run_test()

if __name__ == "__main__":
    asyncio.run(main())