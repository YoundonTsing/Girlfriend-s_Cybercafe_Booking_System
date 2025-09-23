#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
座位锁定测试脚本
用于验证修复后的座位锁定功能
"""

import requests
import json
import time
import threading
from concurrent.futures import ThreadPoolExecutor, as_completed

class SeatLockTest:
    def __init__(self, base_url="http://localhost:8082"):
        self.base_url = base_url
        self.session = requests.Session()
        self.headers = {
            'Content-Type': 'application/json',
            'X-User-Id': '2',
            'Authorization': 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjIsInVzZXJuYW1lIjoiYWRtaW4iLCJpYXQiOjE3NTc3ODQyNjMsImV4cCI6MTc1Nzg3MDY2M30.HihZIQ0LtxXSnJdecEaTQklFzOChxSVdrUJN4PhPC9E'
        }
    
    def test_seat_lock(self, seat_id):
        """测试单个座位锁定"""
        try:
            url = f"{self.base_url}/api/seat/lock"
            data = {"seatIds": [seat_id]}
            
            print(f"尝试锁定座位 {seat_id}...")
            response = self.session.post(url, json=data, headers=self.headers)
            
            if response.status_code == 200:
                result = response.json()
                print(f"座位 {seat_id} 锁定成功: {result}")
                return True
            else:
                print(f"座位 {seat_id} 锁定失败: {response.status_code} - {response.text}")
                return False
        except Exception as e:
            print(f"座位 {seat_id} 锁定异常: {e}")
            return False
    
    def test_seat_unlock(self, seat_id):
        """测试单个座位解锁"""
        try:
            url = f"{self.base_url}/api/seat/unlock"
            data = {"seatIds": [seat_id]}
            
            print(f"尝试解锁座位 {seat_id}...")
            response = self.session.post(url, json=data, headers=self.headers)
            
            if response.status_code == 200:
                result = response.json()
                print(f"座位 {seat_id} 解锁成功: {result}")
                return True
            else:
                print(f"座位 {seat_id} 解锁失败: {response.status_code} - {response.text}")
                return False
        except Exception as e:
            print(f"座位 {seat_id} 解锁异常: {e}")
            return False
    
    def test_concurrent_lock(self, seat_id, thread_count=5):
        """测试并发锁定同一座位"""
        print(f"\n=== 测试并发锁定座位 {seat_id} ===")
        
        results = []
        
        def worker(thread_id):
            try:
                print(f"线程 {thread_id} 开始锁定座位 {seat_id}")
                success = self.test_seat_lock(seat_id)
                return {"thread_id": thread_id, "success": success}
            except Exception as e:
                print(f"线程 {thread_id} 异常: {e}")
                return {"thread_id": thread_id, "success": False, "error": str(e)}
        
        with ThreadPoolExecutor(max_workers=thread_count) as executor:
            futures = [executor.submit(worker, i) for i in range(thread_count)]
            
            for future in as_completed(futures):
                try:
                    result = future.result()
                    results.append(result)
                except Exception as e:
                    print(f"线程执行异常: {e}")
        
        success_count = sum(1 for r in results if r.get('success', False))
        print(f"并发测试完成，成功锁定: {success_count}/{thread_count}")
        return results
    
    def test_multiple_seats_lock(self, seat_ids):
        """测试锁定多个座位"""
        print(f"\n=== 测试锁定多个座位 {seat_ids} ===")
        
        try:
            url = f"{self.base_url}/api/seat/lock"
            data = {"seatIds": seat_ids}
            
            print(f"尝试锁定座位 {seat_ids}...")
            response = self.session.post(url, json=data, headers=self.headers)
            
            if response.status_code == 200:
                result = response.json()
                print(f"多座位锁定成功: {result}")
                return True
            else:
                print(f"多座位锁定失败: {response.status_code} - {response.text}")
                return False
        except Exception as e:
            print(f"多座位锁定异常: {e}")
            return False
    
    def test_seat_status(self, seat_id):
        """测试座位状态查询"""
        try:
            url = f"{self.base_url}/api/seat/status/{seat_id}"
            
            print(f"查询座位 {seat_id} 状态...")
            response = self.session.get(url, headers=self.headers)
            
            if response.status_code == 200:
                result = response.json()
                print(f"座位 {seat_id} 状态: {result}")
                return result
            else:
                print(f"查询座位状态失败: {response.status_code} - {response.text}")
                return None
        except Exception as e:
            print(f"查询座位状态异常: {e}")
            return None

def main():
    """主测试函数"""
    print("=== 座位锁定功能测试开始 ===")
    
    tester = SeatLockTest()
    
    # 测试座位ID列表
    test_seat_ids = [35, 36, 37, 38, 39]
    
    print("\n1. 测试单个座位锁定...")
    for seat_id in test_seat_ids[:3]:  # 只测试前3个座位
        success = tester.test_seat_lock(seat_id)
        if success:
            time.sleep(1)  # 等待1秒
            tester.test_seat_unlock(seat_id)
    
    print("\n2. 测试多个座位锁定...")
    tester.test_multiple_seats_lock([35, 36])
    
    print("\n3. 测试并发锁定...")
    tester.test_concurrent_lock(35, 3)
    
    print("\n4. 测试座位状态查询...")
    for seat_id in test_seat_ids[:2]:
        tester.test_seat_status(seat_id)
    
    print("\n=== 座位锁定功能测试完成 ===")

if __name__ == "__main__":
    main()