#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
检查数据库座位状态
"""

import requests
import json

def check_database_status():
    base_url = "http://localhost:8002"
    headers = {
        'Content-Type': 'application/json',
        'X-User-Id': '2',
        'Authorization': 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjIsInVzZXJuYW1lIjoiYWRtaW4iLCJpYXQiOjE3NTc3ODQyNjMsImV4cCI6MTc1Nzg3MDY2M30.HihZIQ0LtxXSnJdecEaTQklFzOChxSVdrUJN4PhPC9E'
    }
    
    print("=== 检查数据库座位状态 ===")
    
    # 1. 获取座位布局
    print("\n1. 获取座位布局...")
    url = f"{base_url}/api/seat/layout/17"
    params = {"showId": 1, "sessionId": 10}
    response = requests.get(url, params=params, headers=headers, timeout=10)
    
    if response.status_code != 200:
        print(f"❌ 获取座位布局失败: {response.text}")
        return
    
    result = response.json()
    seats = result['data']['seats']
    
    print(f"总座位数: {len(seats)}")
    
    # 2. 分析座位状态
    print(f"\n2. 分析座位状态...")
    
    status_analysis = {
        'status_0': 0,  # 维护中
        'status_1': 0,  # 可用
        'lockStatus_0': 0,  # 空闲
        'lockStatus_1': 0,  # 已锁定
        'lockStatus_2': 0,  # 已占用
        'isDeleted_0': 0,  # 未删除
        'isDeleted_1': 0,  # 已删除
    }
    
    for seat in seats:
        status = seat.get('status')
        lock_status = seat.get('lockStatus')
        is_deleted = seat.get('isDeleted')
        
        if status == 0:
            status_analysis['status_0'] += 1
        elif status == 1:
            status_analysis['status_1'] += 1
            
        if lock_status == 0:
            status_analysis['lockStatus_0'] += 1
        elif lock_status == 1:
            status_analysis['lockStatus_1'] += 1
        elif lock_status == 2:
            status_analysis['lockStatus_2'] += 1
            
        if is_deleted == 0:
            status_analysis['isDeleted_0'] += 1
        elif is_deleted == 1:
            status_analysis['isDeleted_1'] += 1
    
    print(f"座位状态统计:")
    for key, count in status_analysis.items():
        print(f"  {key}: {count}")
    
    # 3. 检查锁定条件
    print(f"\n3. 检查锁定条件...")
    
    lockable_seats = []
    for seat in seats:
        seat_id = seat.get('id')
        status = seat.get('status')
        lock_status = seat.get('lockStatus')
        is_deleted = seat.get('isDeleted')
        
        # 检查是否符合锁定条件
        can_lock = (lock_status == 0 and status == 1 and is_deleted == 0)
        
        if can_lock:
            lockable_seats.append(seat)
        else:
            print(f"座位 {seat_id} 不可锁定: status={status}, lockStatus={lock_status}, isDeleted={is_deleted}")
    
    print(f"可锁定座位数: {len(lockable_seats)}")
    
    if lockable_seats:
        print(f"前5个可锁定座位:")
        for i, seat in enumerate(lockable_seats[:5]):
            print(f"  座位 {i+1}: ID={seat.get('id')}, 状态={seat.get('status')}, 锁定状态={seat.get('lockStatus')}")
        
        # 4. 尝试锁定第一个可锁定座位
        test_seat = lockable_seats[0]
        seat_id = test_seat['id']
        
        print(f"\n4. 尝试锁定座位 {seat_id}...")
        
        lock_url = f"{base_url}/api/seat/lock"
        lock_data = {"seatIds": [seat_id]}
        
        lock_response = requests.post(lock_url, json=lock_data, headers=headers, timeout=10)
        print(f"锁定响应: {lock_response.status_code}")
        print(f"锁定内容: {lock_response.text}")
        
        if lock_response.status_code == 200:
            lock_result = lock_response.json()
            if lock_result.get('success'):
                print(f"✅ 座位 {seat_id} 锁定成功")
            else:
                print(f"❌ 座位 {seat_id} 锁定失败: {lock_result.get('message')}")
        else:
            print(f"❌ 座位 {seat_id} 锁定请求失败")
    else:
        print("❌ 没有可锁定的座位")

if __name__ == "__main__":
    check_database_status()