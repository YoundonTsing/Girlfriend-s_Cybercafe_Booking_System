#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
检查Lua脚本修复是否生效的测试脚本
"""

import requests
import json
import time

def test_seat_lock_fix():
    """测试座位锁定修复是否生效"""
    base_url = "http://localhost:8002"
    headers = {
        'Content-Type': 'application/json',
        'X-User-Id': '2',
        'Authorization': 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjIsInVzZXJuYW1lIjoiYWRtaW4iLCJpYXQiOjE3NTc3ODQyNjMsImV4cCI6MTc1Nzg3MDY2M30.HihZIQ0LtxXSnJdecEaTQklFzOChxSVdrUJN4PhPC9E'
    }
    
    print("=== 检查Lua脚本修复是否生效 ===")
    
    # 测试多个座位ID
    test_seat_ids = [34, 35, 36, 37, 38]
    
    for seat_id in test_seat_ids:
        try:
            print(f"\n测试座位 {seat_id} 锁定...")
            
            url = f"{base_url}/api/seat/lock"
            data = {"seatIds": [seat_id]}
            
            response = requests.post(url, json=data, headers=headers, timeout=10)
            
            print(f"响应状态码: {response.status_code}")
            
            if response.status_code == 200:
                result = response.json()
                print(f"✅ 座位 {seat_id} 锁定成功: {result}")
                
                # 解锁座位
                time.sleep(1)
                unlock_data = {"seatIds": [seat_id]}
                unlock_response = requests.post(f"{base_url}/api/seat/release", json=unlock_data, headers=headers, timeout=10)
                
                if unlock_response.status_code == 200:
                    print(f"✅ 座位 {seat_id} 解锁成功")
                else:
                    print(f"⚠️ 座位 {seat_id} 解锁失败: {unlock_response.text}")
                    
            else:
                print(f"❌ 座位 {seat_id} 锁定失败: {response.text}")
                
        except Exception as e:
            print(f"❌ 座位 {seat_id} 测试异常: {e}")
    
    print("\n=== 测试完成 ===")

if __name__ == "__main__":
    test_seat_lock_fix()