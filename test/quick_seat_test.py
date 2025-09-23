#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
快速座位锁定测试脚本
"""

import requests
import json

def quick_test():
    base_url = "http://localhost:8002"
    headers = {
        'Content-Type': 'application/json',
        'X-User-Id': '2',
        'Authorization': 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjIsInVzZXJuYW1lIjoiYWRtaW4iLCJpYXQiOjE3NTc3ODQyNjMsImV4cCI6MTc1Nzg3MDY2M30.HihZIQ0LtxXSnJdecEaTQklFzOChxSVdrUJN4PhPC9E'
    }
    
    print("=== 快速座位锁定测试 ===")
    
    # 1. 获取座位布局
    print("\n1. 获取座位布局...")
    try:
        url = f"{base_url}/api/seat/layout/17"
        params = {"showId": 1, "sessionId": 10}
        response = requests.get(url, params=params, headers=headers, timeout=10)
        
        if response.status_code == 200:
            result = response.json()
            print(f"✅ 获取座位布局成功")
            
            if 'data' in result and 'seats' in result['data']:
                seats = result['data']['seats']
                print(f"总座位数: {len(seats)}")
                
                # 找到第一个可用座位
                available_seat = None
                for seat in seats:
                    if seat.get('lockStatus') == 0:
                        available_seat = seat
                        break
                
                if available_seat:
                    seat_id = available_seat['id']
                    print(f"找到可用座位: ID={seat_id}, 状态={seat.get('lockStatus')}")
                    
                    # 2. 尝试锁定
                    print(f"\n2. 尝试锁定座位 {seat_id}...")
                    lock_url = f"{base_url}/api/seat/lock"
                    lock_data = {"seatIds": [seat_id]}
                    
                    lock_response = requests.post(lock_url, json=lock_data, headers=headers, timeout=10)
                    print(f"锁定响应状态码: {lock_response.status_code}")
                    print(f"锁定响应内容: {lock_response.text}")
                    
                    if lock_response.status_code == 200:
                        lock_result = lock_response.json()
                        if lock_result.get('success'):
                            print(f"✅ 座位 {seat_id} 锁定成功")
                            
                            # 3. 尝试解锁
                            print(f"\n3. 尝试解锁座位 {seat_id}...")
                            unlock_url = f"{base_url}/api/seat/release"
                            unlock_data = {"seatIds": [seat_id]}
                            
                            unlock_response = requests.post(unlock_url, json=unlock_data, headers=headers, timeout=10)
                            print(f"解锁响应状态码: {unlock_response.status_code}")
                            print(f"解锁响应内容: {unlock_response.text}")
                            
                            if unlock_response.status_code == 200:
                                unlock_result = unlock_response.json()
                                if unlock_result.get('success'):
                                    print(f"✅ 座位 {seat_id} 解锁成功")
                                else:
                                    print(f"❌ 座位 {seat_id} 解锁失败: {unlock_result.get('message')}")
                            else:
                                print(f"❌ 座位 {seat_id} 解锁请求失败")
                        else:
                            print(f"❌ 座位 {seat_id} 锁定失败: {lock_result.get('message')}")
                    else:
                        print(f"❌ 座位 {seat_id} 锁定请求失败")
                else:
                    print("❌ 没有找到可用座位")
            else:
                print("❌ 座位布局数据格式异常")
        else:
            print(f"❌ 获取座位布局失败: {response.text}")
            
    except Exception as e:
        print(f"❌ 测试异常: {e}")

if __name__ == "__main__":
    quick_test()