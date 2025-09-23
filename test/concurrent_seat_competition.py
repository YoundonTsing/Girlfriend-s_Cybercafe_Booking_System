#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
并发座位竞争测试
测试多个人同时抢一个座位的并发能力
"""

import requests
import json
import threading
import time
import random
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime

class SeatCompetitionTester:
    def __init__(self, base_url="http://localhost:8002"):
        self.base_url = base_url
        self.results = []
        self.lock = threading.Lock()
        
    def get_headers(self, user_id):
        """获取用户请求头"""
        return {
            'Content-Type': 'application/json',
            'X-User-Id': str(user_id),
            'Authorization': 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjIsInVzZXJuYW1lIjoiYWRtaW4iLCJpYXQiOjE3NTc3ODQyNjMsImV4cCI6MTc1Nzg3MDY2M30.HihZIQ0LtxXSnJdecEaTQklFzOChxSVdrUJN4PhPC9E'
        }
    
    def get_available_seat(self, user_id):
        """获取可用座位"""
        try:
            url = f"{self.base_url}/api/seat/layout/17"
            params = {"showId": 1, "sessionId": 10}
            headers = self.get_headers(user_id)
            
            response = requests.get(url, params=params, headers=headers, timeout=10)
            if response.status_code == 200:
                result = response.json()
                seats = result.get('data', {}).get('seats', [])
                
                # 找到第一个可用座位
                for seat in seats:
                    if seat.get('lockStatus') == 0 and seat.get('status') == 1:
                        return seat['id']
            return None
        except Exception as e:
            print(f"用户 {user_id} 获取座位失败: {e}")
            return None
    
    def try_lock_seat(self, user_id, seat_id, test_id):
        """尝试锁定座位"""
        start_time = time.time()
        result = {
            'user_id': user_id,
            'seat_id': seat_id,
            'test_id': test_id,
            'start_time': start_time,
            'success': False,
            'error': None,
            'response_time': 0,
            'response_code': 0,
            'response_message': ''
        }
        
        try:
            url = f"{self.base_url}/api/seat/lock"
            headers = self.get_headers(user_id)
            data = {"seatIds": [seat_id]}
            
            # 添加随机延迟，模拟真实用户行为
            time.sleep(random.uniform(0.01, 0.1))
            
            response = requests.post(url, json=data, headers=headers, timeout=10)
            end_time = time.time()
            
            result['response_time'] = end_time - start_time
            result['response_code'] = response.status_code
            
            if response.status_code == 200:
                response_data = response.json()
                result['response_message'] = response_data.get('message', '')
                result['success'] = response_data.get('success', False)
            else:
                result['error'] = f"HTTP {response.status_code}: {response.text}"
                
        except Exception as e:
            end_time = time.time()
            result['response_time'] = end_time - start_time
            result['error'] = str(e)
        
        with self.lock:
            self.results.append(result)
        
        return result
    
    def test_single_seat_competition(self, seat_id, num_users=10, test_name="单座位竞争测试"):
        """测试单个座位的竞争"""
        print(f"\n=== {test_name} ===")
        print(f"座位ID: {seat_id}, 竞争用户数: {num_users}")
        
        self.results = []
        start_time = time.time()
        
        # 创建线程池
        with ThreadPoolExecutor(max_workers=num_users) as executor:
            # 提交所有任务
            futures = []
            for i in range(num_users):
                user_id = i + 1
                future = executor.submit(self.try_lock_seat, user_id, seat_id, i)
                futures.append(future)
            
            # 等待所有任务完成
            for future in as_completed(futures):
                try:
                    result = future.result()
                except Exception as e:
                    print(f"任务执行异常: {e}")
        
        end_time = time.time()
        total_time = end_time - start_time
        
        # 分析结果
        self.analyze_results(test_name, total_time)
    
    def test_multiple_seats_competition(self, num_seats=3, users_per_seat=5):
        """测试多个座位的竞争"""
        print(f"\n=== 多座位竞争测试 ===")
        print(f"座位数: {num_seats}, 每座位用户数: {users_per_seat}")
        
        # 获取可用座位
        available_seats = []
        for i in range(num_seats * 2):  # 获取更多座位以防不够
            seat_id = self.get_available_seat(1)
            if seat_id and seat_id not in available_seats:
                available_seats.append(seat_id)
                if len(available_seats) >= num_seats:
                    break
        
        if len(available_seats) < num_seats:
            print(f"❌ 可用座位不足，需要 {num_seats} 个，只找到 {len(available_seats)} 个")
            return
        
        selected_seats = available_seats[:num_seats]
        print(f"选择的座位: {selected_seats}")
        
        self.results = []
        start_time = time.time()
        
        # 创建线程池
        with ThreadPoolExecutor(max_workers=num_seats * users_per_seat) as executor:
            futures = []
            user_id = 1
            
            for seat_id in selected_seats:
                for i in range(users_per_seat):
                    future = executor.submit(self.try_lock_seat, user_id, seat_id, i)
                    futures.append(future)
                    user_id += 1
            
            # 等待所有任务完成
            for future in as_completed(futures):
                try:
                    result = future.result()
                except Exception as e:
                    print(f"任务执行异常: {e}")
        
        end_time = time.time()
        total_time = end_time - start_time
        
        # 分析结果
        self.analyze_results("多座位竞争测试", total_time)
    
    def analyze_results(self, test_name, total_time):
        """分析测试结果"""
        print(f"\n--- {test_name} 结果分析 ---")
        print(f"总耗时: {total_time:.3f}秒")
        print(f"总请求数: {len(self.results)}")
        
        # 统计成功和失败
        success_count = sum(1 for r in self.results if r['success'])
        failure_count = len(self.results) - success_count
        
        print(f"成功锁定: {success_count}")
        print(f"锁定失败: {failure_count}")
        print(f"成功率: {success_count/len(self.results)*100:.1f}%")
        
        # 统计响应时间
        response_times = [r['response_time'] for r in self.results]
        if response_times:
            avg_time = sum(response_times) / len(response_times)
            max_time = max(response_times)
            min_time = min(response_times)
            print(f"平均响应时间: {avg_time:.3f}秒")
            print(f"最大响应时间: {max_time:.3f}秒")
            print(f"最小响应时间: {min_time:.3f}秒")
        
        # 按座位分组统计
        seat_stats = {}
        for result in self.results:
            seat_id = result['seat_id']
            if seat_id not in seat_stats:
                seat_stats[seat_id] = {'total': 0, 'success': 0}
            seat_stats[seat_id]['total'] += 1
            if result['success']:
                seat_stats[seat_id]['success'] += 1
        
        print(f"\n各座位竞争结果:")
        for seat_id, stats in seat_stats.items():
            success_rate = stats['success'] / stats['total'] * 100
            print(f"  座位 {seat_id}: {stats['success']}/{stats['total']} 成功 ({success_rate:.1f}%)")
        
        # 显示详细结果
        print(f"\n详细结果:")
        for result in self.results:
            status = "✅成功" if result['success'] else "❌失败"
            print(f"  用户{result['user_id']} -> 座位{result['seat_id']}: {status} "
                  f"({result['response_time']:.3f}s) {result['response_message']}")
    
    def test_high_concurrency(self, seat_id, num_users=50):
        """高并发测试"""
        print(f"\n=== 高并发测试 ===")
        print(f"座位ID: {seat_id}, 并发用户数: {num_users}")
        
        self.results = []
        start_time = time.time()
        
        # 创建线程池
        with ThreadPoolExecutor(max_workers=min(num_users, 100)) as executor:
            futures = []
            for i in range(num_users):
                user_id = i + 1
                future = executor.submit(self.try_lock_seat, user_id, seat_id, i)
                futures.append(future)
            
            # 等待所有任务完成
            for future in as_completed(futures):
                try:
                    result = future.result()
                except Exception as e:
                    print(f"任务执行异常: {e}")
        
        end_time = time.time()
        total_time = end_time - start_time
        
        # 分析结果
        self.analyze_results("高并发测试", total_time)

def main():
    tester = SeatCompetitionTester()
    
    print("=== 座位并发竞争测试开始 ===")
    
    # 1. 获取一个可用座位
    print("\n1. 获取可用座位...")
    seat_id = tester.get_available_seat(1)
    if not seat_id:
        print("❌ 没有找到可用座位")
        return
    
    print(f"✅ 找到可用座位: {seat_id}")
    
    # 2. 单座位竞争测试（10个用户）
    tester.test_single_seat_competition(seat_id, 10, "单座位竞争测试(10用户)")
    
    # 3. 单座位竞争测试（20个用户）
    tester.test_single_seat_competition(seat_id, 20, "单座位竞争测试(20用户)")
    
    # 4. 多座位竞争测试
    tester.test_multiple_seats_competition(3, 5)
    
    # 5. 高并发测试
    tester.test_high_concurrency(seat_id, 50)
    
    print(f"\n=== 座位并发竞争测试完成 ===")

if __name__ == "__main__":
    main()