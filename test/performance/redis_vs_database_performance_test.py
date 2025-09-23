#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Redis vs 数据库性能对比测试
验证统一技术栈后的性能提升效果
"""

import requests
import time
import threading
import json
import statistics
import random
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
import matplotlib.pyplot as plt
import pandas as pd
import os

class RedisVsDatabasePerformanceTest:
    def __init__(self):
        self.base_url = "http://localhost:8000"
        self.results = {
            'redis_based_orders': [],
            'database_based_orders': [],
            'concurrent_comparison': []
        }
        self.test_users = []
        self.auth_tokens = {}
        
    def setup_test_data(self):
        """准备测试数据"""
        print("准备测试数据...")
        
        # 创建测试用户
        for i in range(100):
            user_data = {
                "username": f"perftest_{i}_{int(time.time() * 1000)}",
                "password": "123456",
                "email": f"perftest_{i}@example.com",
                "phone": f"1390000{i:04d}"
            }
            
            try:
                response = requests.post(f"{self.base_url}/api/user/register", json=user_data, timeout=10)
                if response.status_code == 200:
                    response_data = response.json()
                    if response_data.get('success', True):
                        self.test_users.append(user_data)
                        
                        # 登录获取token
                        login_data = {
                            "username": user_data["username"],
                            "password": user_data["password"]
                        }
                        login_response = requests.post(f"{self.base_url}/api/user/login", json=login_data, timeout=10)
                        if login_response.status_code == 200:
                            login_data = login_response.json()
                            if login_data.get('success', True):
                                self.auth_tokens[user_data["username"]] = login_data.get('data', {}).get('token', '')
                
            except Exception as e:
                print(f"创建用户失败: {e}")
        
        print(f"成功创建 {len(self.test_users)} 个测试用户")
    
    def test_redis_based_order_performance(self, concurrent_users=50, total_requests=1000):
        """测试基于Redis的订单服务性能"""
        print(f"\n测试Redis订单服务性能 - 并发用户: {concurrent_users}, 总请求: {total_requests}")
        
        def create_redis_order(user_index):
            start_time = time.time()
            user = self.test_users[user_index % len(self.test_users)]
            token = self.auth_tokens.get(user["username"], "")
            
            if not token:
                return {
                    'operation': 'redis_order_create',
                    'response_time': 0,
                    'status_code': 0,
                    'success': False,
                    'error': 'no_token',
                    'timestamp': datetime.now().isoformat()
                }
            
            headers = {"Authorization": f"Bearer {token}"}
            order_data = {
                "userId": user_index + 1,
                "ticketId": 1,
                "quantity": 1,
                "showId": 1,
                "sessionId": 1,
                "price": 100.0
            }
            
            try:
                # 调用Redis订单服务
                response = requests.post(
                    f"{self.base_url}/api/order/redis/create", 
                    json=order_data, 
                    headers=headers, 
                    timeout=5
                )
                end_time = time.time()
                
                return {
                    'operation': 'redis_order_create',
                    'response_time': (end_time - start_time) * 1000,
                    'status_code': response.status_code,
                    'success': response.status_code == 200,
                    'timestamp': datetime.now().isoformat()
                }
            except Exception as e:
                end_time = time.time()
                return {
                    'operation': 'redis_order_create',
                    'response_time': (end_time - start_time) * 1000,
                    'status_code': 0,
                    'success': False,
                    'error': str(e),
                    'timestamp': datetime.now().isoformat()
                }
        
        # 执行并发测试
        with ThreadPoolExecutor(max_workers=concurrent_users) as executor:
            futures = [executor.submit(create_redis_order, i) for i in range(total_requests)]
            
            for future in as_completed(futures):
                result = future.result()
                self.results['redis_based_orders'].append(result)
        
        # 分析结果
        self.analyze_redis_results()
    
    def test_database_based_order_performance(self, concurrent_users=50, total_requests=1000):
        """测试基于数据库的订单服务性能"""
        print(f"\n测试数据库订单服务性能 - 并发用户: {concurrent_users}, 总请求: {total_requests}")
        
        def create_database_order(user_index):
            start_time = time.time()
            user = self.test_users[user_index % len(self.test_users)]
            token = self.auth_tokens.get(user["username"], "")
            
            if not token:
                return {
                    'operation': 'database_order_create',
                    'response_time': 0,
                    'status_code': 0,
                    'success': False,
                    'error': 'no_token',
                    'timestamp': datetime.now().isoformat()
                }
            
            headers = {"Authorization": f"Bearer {token}"}
            order_data = {
                "userId": user_index + 1,
                "ticketId": 1,
                "quantity": 1,
                "showId": 1,
                "sessionId": 1,
                "price": 100.0
            }
            
            try:
                # 调用数据库订单服务
                response = requests.post(
                    f"{self.base_url}/api/order/database/create", 
                    json=order_data, 
                    headers=headers, 
                    timeout=5
                )
                end_time = time.time()
                
                return {
                    'operation': 'database_order_create',
                    'response_time': (end_time - start_time) * 1000,
                    'status_code': response.status_code,
                    'success': response.status_code == 200,
                    'timestamp': datetime.now().isoformat()
                }
            except Exception as e:
                end_time = time.time()
                return {
                    'operation': 'database_order_create',
                    'response_time': (end_time - start_time) * 1000,
                    'status_code': 0,
                    'success': False,
                    'error': str(e),
                    'timestamp': datetime.now().isoformat()
                }
        
        # 执行并发测试
        with ThreadPoolExecutor(max_workers=concurrent_users) as executor:
            futures = [executor.submit(create_database_order, i) for i in range(total_requests)]
            
            for future in as_completed(futures):
                result = future.result()
                self.results['database_based_orders'].append(result)
        
        # 分析结果
        self.analyze_database_results()
    
    def test_concurrent_performance_comparison(self, concurrent_users_list=[10, 20, 50, 100, 200]):
        """并发性能对比测试"""
        print(f"\n并发性能对比测试 - 并发用户数: {concurrent_users_list}")
        
        for concurrent_users in concurrent_users_list:
            print(f"\n测试并发用户数: {concurrent_users}")
            
            # 测试Redis性能
            redis_start_time = time.time()
            self.test_redis_based_order_performance(concurrent_users, concurrent_users * 10)
            redis_end_time = time.time()
            redis_duration = redis_end_time - redis_start_time
            redis_qps = (concurrent_users * 10) / redis_duration
            
            # 测试数据库性能
            database_start_time = time.time()
            self.test_database_based_order_performance(concurrent_users, concurrent_users * 10)
            database_end_time = time.time()
            database_duration = database_end_time - database_start_time
            database_qps = (concurrent_users * 10) / database_duration
            
            # 记录对比结果
            comparison_result = {
                'concurrent_users': concurrent_users,
                'redis_qps': redis_qps,
                'database_qps': database_qps,
                'redis_duration': redis_duration,
                'database_duration': database_duration,
                'performance_improvement': (redis_qps - database_qps) / database_qps * 100,
                'timestamp': datetime.now().isoformat()
            }
            self.results['concurrent_comparison'].append(comparison_result)
            
            print(f"Redis QPS: {redis_qps:.2f}, 数据库 QPS: {database_qps:.2f}, 性能提升: {comparison_result['performance_improvement']:.2f}%")
    
    def analyze_redis_results(self):
        """分析Redis结果"""
        if not self.results['redis_based_orders']:
            return
        
        response_times = [r['response_time'] for r in self.results['redis_based_orders'] if r['response_time'] > 0]
        success_count = sum(1 for r in self.results['redis_based_orders'] if r['success'])
        total_count = len(self.results['redis_based_orders'])
        
        if response_times:
            print(f"Redis订单服务性能分析:")
            print(f"  总请求数: {total_count}")
            print(f"  成功请求数: {success_count}")
            print(f"  成功率: {success_count/total_count*100:.2f}%")
            print(f"  平均响应时间: {statistics.mean(response_times):.2f}ms")
            print(f"  中位数响应时间: {statistics.median(response_times):.2f}ms")
            print(f"  95%分位数响应时间: {sorted(response_times)[int(len(response_times)*0.95)]:.2f}ms")
            print(f"  最大响应时间: {max(response_times):.2f}ms")
            print(f"  最小响应时间: {min(response_times):.2f}ms")
    
    def analyze_database_results(self):
        """分析数据库结果"""
        if not self.results['database_based_orders']:
            return
        
        response_times = [r['response_time'] for r in self.results['database_based_orders'] if r['response_time'] > 0]
        success_count = sum(1 for r in self.results['database_based_orders'] if r['success'])
        total_count = len(self.results['database_based_orders'])
        
        if response_times:
            print(f"数据库订单服务性能分析:")
            print(f"  总请求数: {total_count}")
            print(f"  成功请求数: {success_count}")
            print(f"  成功率: {success_count/total_count*100:.2f}%")
            print(f"  平均响应时间: {statistics.mean(response_times):.2f}ms")
            print(f"  中位数响应时间: {statistics.median(response_times):.2f}ms")
            print(f"  95%分位数响应时间: {sorted(response_times)[int(len(response_times)*0.95)]:.2f}ms")
            print(f"  最大响应时间: {max(response_times):.2f}ms")
            print(f"  最小响应时间: {min(response_times):.2f}ms")
    
    def generate_performance_report(self):
        """生成性能对比报告"""
        print("\n" + "="*60)
        print("性能对比报告")
        print("="*60)
        
        # 计算总体性能指标
        redis_response_times = [r['response_time'] for r in self.results['redis_based_orders'] if r['response_time'] > 0]
        database_response_times = [r['response_time'] for r in self.results['database_based_orders'] if r['response_time'] > 0]
        
        if redis_response_times and database_response_times:
            redis_avg = statistics.mean(redis_response_times)
            database_avg = statistics.mean(database_response_times)
            improvement = (database_avg - redis_avg) / database_avg * 100
            
            print(f"响应时间对比:")
            print(f"  Redis平均响应时间: {redis_avg:.2f}ms")
            print(f"  数据库平均响应时间: {database_avg:.2f}ms")
            print(f"  性能提升: {improvement:.2f}%")
        
        # 并发性能对比
        if self.results['concurrent_comparison']:
            print(f"\n并发性能对比:")
            for result in self.results['concurrent_comparison']:
                print(f"  并发用户数: {result['concurrent_users']}")
                print(f"    Redis QPS: {result['redis_qps']:.2f}")
                print(f"    数据库 QPS: {result['database_qps']:.2f}")
                print(f"    性能提升: {result['performance_improvement']:.2f}%")
        
        # 保存结果到文件
        self.save_results_to_file()
    
    def save_results_to_file(self):
        """保存测试结果到文件"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"performance_comparison_{timestamp}.json"
        
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(self.results, f, ensure_ascii=False, indent=2)
        
        print(f"\n测试结果已保存到: {filename}")
    
    def run_full_test(self):
        """运行完整测试"""
        print("开始Redis vs 数据库性能对比测试")
        print("="*60)
        
        # 准备测试数据
        self.setup_test_data()
        
        # 基础性能测试
        self.test_redis_based_order_performance(50, 500)
        self.test_database_based_order_performance(50, 500)
        
        # 并发性能对比测试
        self.test_concurrent_performance_comparison([10, 20, 50, 100])
        
        # 生成报告
        self.generate_performance_report()

if __name__ == "__main__":
    test = RedisVsDatabasePerformanceTest()
    test.run_full_test()