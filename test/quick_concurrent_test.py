#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
快速并发座位竞争测试
"""

import requests
import json
import threading
import time
import random
from concurrent.futures import ThreadPoolExecutor

def test_concurrent_seat_lock():
    base_url = "http://localhost:8002"
    
    def get_headers(user_id):
        return {
            'Content-Type': 'application/json',
            'X-User-Id': str(user_id),
            'Authorization': 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjIsInVzZXJuYW1lIjoiYWRtaW4iLCJpYXQiOjE3NTc3ODQyNjMsImV4cCI6MTc1Nzg3MDY2M30.HihZIQ0LtxXSnJdecEaTQklFzOChxSVdrUJN4PhPC9E'
        }
    
    def try_lock_seat(user_id, seat_id, results):
        """尝试锁定座位"""
        start_time = time.time()
        
        try:
            url = f"{base_url}/api/seat/lock"
            headers = get_headers(user_id)
            data = {"seatIds": [seat_id]}
            
            # 添加随机延迟
            time.sleep(random.uniform(0.01, 0.05))
            
            response = requests.post(url, json=data, headers=headers, timeout=10)
            end_time = time.time()
            
            result = {
                'user_id': user_id,
                'seat_id': seat_id,
                'success': False,
                'response_time': end_time - start_time,
                'response_code': response.status_code,
                'message': ''
            }
            
            if response.status_code == 200:
                response_data = response.json()
                result['success'] = response_data.get('success', False)
                result['message'] = response_data.get('message', '')
            else:
                result['message'] = f"HTTP {response.status_code}"
                
        except Exception as e:
            end_time = time.time()
            result = {
                'user_id': user_id,
                'seat_id': seat_id,
                'success': False,
                'response_time': end_time - start_time,
                'response_code': 0,
                'message': str(e)
            }
        
        results.append(result)
        return result
    
    # 获取可用座位
    print("=== 快速并发座位竞争测试 ===")
    print("\n1. 获取可用座位...")
    
    url = f"{base_url}/api/seat/layout/17"
    params = {"showId": 1, "sessionId": 10}
    headers = get_headers(1)
    
    response = requests.get(url, params=params, headers=headers, timeout=10)
    if response.status_code != 200:
        print(f"❌ 获取座位布局失败: {response.text}")
        return
    
    result = response.json()
    seats = result.get('data', {}).get('seats', [])
    
    # 找到第一个可用座位
    available_seat = None
    for seat in seats:
        if seat.get('lockStatus') == 0 and seat.get('status') == 1:
            available_seat = seat
            break
    
    if not available_seat:
        print("❌ 没有找到可用座位")
        return
    
    seat_id = available_seat['id']
    print(f"✅ 找到可用座位: ID={seat_id}")
    
    # 并发测试
    print(f"\n2. 开始并发测试...")
    
    # 测试不同并发数
    test_cases = [
        (5, "5个用户竞争"),
        (10, "10个用户竞争"),
        (20, "20个用户竞争"),
        (50, "50个用户竞争")
    ]
    
    for num_users, test_name in test_cases:
        print(f"\n--- {test_name} ---")
        
        results = []
        start_time = time.time()
        
        # 创建线程池
        with ThreadPoolExecutor(max_workers=min(num_users, 50)) as executor:
            futures = []
            for i in range(num_users):
                user_id = i + 1
                future = executor.submit(try_lock_seat, user_id, seat_id, results)
                futures.append(future)
            
            # 等待所有任务完成
            for future in futures:
                try:
                    future.result()
                except Exception as e:
                    print(f"任务执行异常: {e}")
        
        end_time = time.time()
        total_time = end_time - start_time
        
        # 分析结果
        success_count = sum(1 for r in results if r['success'])
        failure_count = len(results) - success_count
        
        print(f"总耗时: {total_time:.3f}秒")
        print(f"总请求数: {len(results)}")
        print(f"成功锁定: {success_count}")
        print(f"锁定失败: {failure_count}")
        print(f"成功率: {success_count/len(results)*100:.1f}%")
        
        # 响应时间统计
        response_times = [r['response_time'] for r in results]
        if response_times:
            avg_time = sum(response_times) / len(response_times)
            max_time = max(response_times)
            min_time = min(response_times)
            print(f"平均响应时间: {avg_time:.3f}秒")
            print(f"最大响应时间: {max_time:.3f}秒")
            print(f"最小响应时间: {min_time:.3f}秒")
        
        # 显示前5个结果
        print(f"前5个结果:")
        for i, result in enumerate(results[:5]):
            status = "✅成功" if result['success'] else "❌失败"
            print(f"  用户{result['user_id']}: {status} ({result['response_time']:.3f}s) {result['message']}")
        
        # 等待一下再进行下一个测试
        time.sleep(2)
    
    print(f"\n=== 并发测试完成 ===")

if __name__ == "__main__":
    test_concurrent_seat_lock()