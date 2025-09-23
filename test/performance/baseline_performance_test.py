#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
性能基线测试脚本
用于测试当前系统性能，识别瓶颈点，为后续RocketMQ优化提供数据支撑
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

class BaselinePerformanceTest:
    def __init__(self):
        self.base_url = "http://localhost:8000"  # 网关地址
        self.results = {
            'user_operations': [],
            'order_operations': [],
            'stock_operations': [],
            'concurrent_operations': []
        }
        self.test_users = []
        self.test_shows = []
        self.auth_tokens = {}  # 存储用户的JWT token
        
    def setup_test_data(self):
        """准备测试数据"""
        print("准备测试数据...")
        
        # 创建测试用户
        for i in range(50):
            timestamp = int(time.time())
            # 使用更随机的数据避免冲突
            random_suffix = random.randint(10000, 99999)
            # 确保手机号唯一性，使用时间戳和随机数
            phone_suffix = f"{timestamp % 100000000:08d}"  # 取时间戳后8位
            phone_number = f"139{phone_suffix[:8]}"
            user_data = {
                "username": f"testuser_{i}_{timestamp}_{random_suffix}",
                "password": "123456",
                "nickname": f"测试用户{i}_{random_suffix}",
                "phone": phone_number,
                "email": f"testuser_{i}_{timestamp}_{random_suffix}@example.com"
            }
            
            try:
                response = requests.post(f"{self.base_url}/api/user/register", json=user_data, timeout=10)
                print(f"注册用户 {user_data['username']} - 状态码: {response.status_code}")
                if response.status_code == 200:
                    response_data = response.json()
                    print(f"注册响应: {response_data}")
                    if response_data.get('success', True):  # 如果有success字段且为true，或没有success字段默认成功
                        self.test_users.append(user_data)
                        print(f"创建测试用户成功: {user_data['username']}")
                    else:
                        print(f"注册业务逻辑失败: {response_data.get('message', '未知错误')}")
                else:
                    print(f"注册HTTP失败: {response.status_code} - {response.text}")
            except Exception as e:
                print(f"创建用户失败: {e}")
        
        print(f"成功创建 {len(self.test_users)} 个测试用户")
    
    def test_user_registration_performance(self, concurrent_users=10, total_requests=100):
        """测试用户注册性能"""
        print(f"\n测试用户注册性能 - 并发用户: {concurrent_users}, 总请求: {total_requests}")
        
        def register_user(index):
            start_time = time.time()
            user_data = {
                "username": f"perftest_{index}_{int(time.time() * 1000)}",
                "password": "123456",
                "email": f"perftest_{index}@example.com",
                "phone": f"1390000{index:04d}"
            }
            
            try:
                response = requests.post(f"{self.base_url}/api/user/register", json=user_data, timeout=10)
                end_time = time.time()
                
                return {
                    'operation': 'user_register',
                    'response_time': (end_time - start_time) * 1000,
                    'status_code': response.status_code,
                    'success': response.status_code == 200,
                    'timestamp': datetime.now().isoformat()
                }
            except Exception as e:
                end_time = time.time()
                return {
                    'operation': 'user_register',
                    'response_time': (end_time - start_time) * 1000,
                    'status_code': 0,
                    'success': False,
                    'error': str(e),
                    'timestamp': datetime.now().isoformat()
                }
        
        with ThreadPoolExecutor(max_workers=concurrent_users) as executor:
            futures = [executor.submit(register_user, i) for i in range(total_requests)]
            
            for future in as_completed(futures):
                result = future.result()
                self.results['user_operations'].append(result)
    
    def test_user_login_performance(self, concurrent_users=20, total_requests=200):
        """测试用户登录性能"""
        print(f"\n测试用户登录性能 - 并发用户: {concurrent_users}, 总请求: {total_requests}")
        print(f"可用测试用户数量: {len(self.test_users)}")
        
        if not self.test_users:
            print("没有可用的测试用户，跳过登录测试")
            return
            
        # 确保所有测试用户都已注册
        print("确保所有测试用户都已注册...")
        for user in self.test_users:
            try:
                # 尝试注册用户（如果已存在会返回错误，但不影响后续登录）
                register_data = {
                    "username": user["username"],
                    "password": user["password"],
                    "nickname": user["nickname"],
                    "phone": user["phone"],
                    "email": user["email"]
                }
                requests.post(f"{self.base_url}/api/user/register", json=register_data, timeout=5)
            except:
                pass  # 忽略注册错误，继续登录测试
        
        def login_user(index):
            start_time = time.time()
            user = self.test_users[index % len(self.test_users)]
            login_data = {
                "username": user["username"],
                "password": user["password"]
            }
            
            try:
                response = requests.post(f"{self.base_url}/api/user/login", json=login_data, timeout=10)
                end_time = time.time()
                
                # 保存JWT token用于后续认证
                if response.status_code == 200:
                    try:
                        response_data = response.json()
                        # 检查业务逻辑是否成功
                        if response_data.get('success') and response_data.get('data'):
                            token_data = response_data['data']
                            if isinstance(token_data, dict) and 'token' in token_data:
                                self.auth_tokens[user['username']] = token_data['token']
                                print(f"保存token for {user['username']}: {token_data['token'][:20]}...")  # 调试信息
                            elif isinstance(token_data, str):
                                # 如果data直接是token字符串
                                self.auth_tokens[user['username']] = token_data
                                print(f"保存token for {user['username']}: {token_data[:20]}...")  # 调试信息
                        else:
                            print(f"登录业务逻辑失败: {response_data.get('message', '未知错误')}")  # 调试信息
                    except Exception as e:
                        print(f"解析登录响应失败: {e}")  # 调试信息
                
                return {
                    'operation': 'user_login',
                    'response_time': (end_time - start_time) * 1000,
                    'status_code': response.status_code,
                    'success': response.status_code == 200,
                    'timestamp': datetime.now().isoformat()
                }
            except Exception as e:
                end_time = time.time()
                return {
                    'operation': 'user_login',
                    'response_time': (end_time - start_time) * 1000,
                    'status_code': 0,
                    'success': False,
                    'error': str(e),
                    'timestamp': datetime.now().isoformat()
                }
        
        with ThreadPoolExecutor(max_workers=concurrent_users) as executor:
            futures = [executor.submit(login_user, i) for i in range(total_requests)]
            
            for future in as_completed(futures):
                result = future.result()
                self.results['user_operations'].append(result)
    
    def test_order_creation_performance(self, concurrent_users=15, total_requests=150):
        """测试订单创建性能"""
        print(f"\n测试订单创建性能 - 并发用户: {concurrent_users}, 总请求: {total_requests}")
        print(f"当前可用token数量: {len(self.auth_tokens)}")
        
        # 确保有登录用户和token
        if not self.test_users or not self.auth_tokens:
            print("没有可用的认证token，跳过订单创建测试")
            return
        
        def create_order(index):
            start_time = time.time()
            order_data = {
                "showId": 1,
                "seatIds": [1, 2],
                "totalAmount": 200.0
            }
            
            try:
                # 使用已缓存的JWT token
                headers = {}
                if self.auth_tokens:
                    token = list(self.auth_tokens.values())[index % len(self.auth_tokens)]
                    headers['Authorization'] = f'Bearer {token}'
                
                response = requests.post(f"{self.base_url}/api/order/create", json=order_data, headers=headers, timeout=15)
                end_time = time.time()
                
                return {
                    'operation': 'order_create',
                    'response_time': (end_time - start_time) * 1000,
                    'status_code': response.status_code,
                    'success': response.status_code == 200,
                    'timestamp': datetime.now().isoformat()
                }
            except Exception as e:
                end_time = time.time()
                return {
                    'operation': 'order_create',
                    'response_time': (end_time - start_time) * 1000,
                    'status_code': 0,
                    'success': False,
                    'error': str(e),
                    'timestamp': datetime.now().isoformat()
                }
        
        with ThreadPoolExecutor(max_workers=concurrent_users) as executor:
            futures = [executor.submit(create_order, i) for i in range(total_requests)]
            
            for future in as_completed(futures):
                result = future.result()
                self.results['order_operations'].append(result)
    
    def test_stock_query_performance(self, concurrent_users=30, total_requests=300):
        """测试库存查询性能"""
        print(f"\n测试库存查询性能 - 并发用户: {concurrent_users}, 总请求: {total_requests}")
        print(f"当前可用token数量: {len(self.auth_tokens)}")
        
        # 确保有登录用户和token
        if not self.test_users or not self.auth_tokens:
            print("没有可用的认证token，跳过库存查询测试")
            return
        
        def query_stock(index):
            start_time = time.time()
            show_id = (index % 10) + 1  # 查询不同的演出
            
            try:
                # 使用JWT token进行认证
                token = list(self.auth_tokens.values())[index % len(self.auth_tokens)]
                headers = {'Authorization': f'Bearer {token}'}
                response = requests.get(f"{self.base_url}/api/ticket/stock/{show_id}", headers=headers, timeout=10)
                end_time = time.time()
                
                return {
                    'operation': 'stock_query',
                    'response_time': (end_time - start_time) * 1000,
                    'status_code': response.status_code,
                    'success': response.status_code == 200,
                    'timestamp': datetime.now().isoformat()
                }
            except Exception as e:
                end_time = time.time()
                return {
                    'operation': 'stock_query',
                    'response_time': (end_time - start_time) * 1000,
                    'status_code': 0,
                    'success': False,
                    'error': str(e),
                    'timestamp': datetime.now().isoformat()
                }
        
        with ThreadPoolExecutor(max_workers=concurrent_users) as executor:
            futures = [executor.submit(query_stock, i) for i in range(total_requests)]
            
            for future in as_completed(futures):
                result = future.result()
                self.results['stock_operations'].append(result)
    
    def test_concurrent_mixed_operations(self, concurrent_users=25, duration_seconds=60):
        """测试混合并发操作"""
        print(f"\n测试混合并发操作 - 并发用户: {concurrent_users}, 持续时间: {duration_seconds}秒")
        
        def mixed_operations():
            end_time = time.time() + duration_seconds
            operation_count = 0
            
            # 先进行一些登录操作来获取token
            initial_login_count = 0
            while initial_login_count < 10 and self.test_users:
                user = self.test_users[initial_login_count % len(self.test_users)]
                login_data = {
                    "username": user["username"],
                    "password": user["password"]
                }
                try:
                    response = requests.post(f"{self.base_url}/api/user/login", json=login_data, timeout=5)
                    if response.status_code == 200:
                        response_data = response.json()
                        if 'token' in response_data:
                            self.auth_tokens[user['username']] = response_data['token']
                except:
                    pass
                initial_login_count += 1
            
            while time.time() < end_time:
                # 调整操作权重：前40%时间主要做注册和登录，后60%时间包含所有操作
                elapsed_ratio = (time.time() - (end_time - duration_seconds)) / duration_seconds
                
                if elapsed_ratio < 0.4:  # 前40%时间主要做注册登录
                    operation_type = operation_count % 2
                else:  # 后60%时间做所有操作
                    operation_type = operation_count % 4
                
                start_time = time.time()
                
                try:
                    if operation_type == 0:  # 用户注册
                        user_data = {
                            "username": f"mixtest_{int(time.time() * 1000000)}",
                            "password": "123456",
                            "email": f"mixtest_{int(time.time())}@example.com",
                            "phone": f"139{int(time.time()) % 100000000:08d}"
                        }
                        response = requests.post(f"{self.base_url}/api/user/register", json=user_data, timeout=5)
                        op_name = 'mixed_register'
                        
                    elif operation_type == 1:  # 用户登录
                        if self.test_users:
                            user = self.test_users[operation_count % len(self.test_users)]
                            login_data = {
                                "username": user["username"],
                                "password": user["password"]
                            }
                            response = requests.post(f"{self.base_url}/api/user/login", json=login_data, timeout=5)
                            # 保存JWT token
                            if response.status_code == 200:
                                try:
                                    response_data = response.json()
                                    if 'token' in response_data:
                                        self.auth_tokens[user['username']] = response_data['token']
                                except:
                                    pass
                            op_name = 'mixed_login'
                        else:
                            continue
                            
                    elif operation_type == 2:  # 库存查询
                        # 只有在有可用token时才执行库存查询
                        if not self.auth_tokens:
                            continue
                        show_id = (operation_count % 10) + 1
                        # 使用JWT token进行认证
                        token = list(self.auth_tokens.values())[operation_count % len(self.auth_tokens)]
                        headers = {'Authorization': f'Bearer {token}'}
                        response = requests.get(f"{self.base_url}/api/ticket/stock/{show_id}", headers=headers, timeout=5)
                        op_name = 'mixed_stock_query'
                        
                    else:  # 订单创建
                        # 只有在有可用token时才执行订单创建
                        if not self.auth_tokens:
                            continue
                        order_data = {
                            "showId": (operation_count % 5) + 1,
                            "seatIds": [operation_count % 100 + 1],
                            "totalAmount": 100.0
                        }
                        # 使用JWT token进行认证
                        token = list(self.auth_tokens.values())[operation_count % len(self.auth_tokens)]
                        headers = {'Authorization': f'Bearer {token}'}
                        response = requests.post(f"{self.base_url}/api/order/create", json=order_data, headers=headers, timeout=10)
                        op_name = 'mixed_order_create'
                    
                    request_end_time = time.time()
                    
                    result = {
                        'operation': op_name,
                        'response_time': (request_end_time - start_time) * 1000,
                        'status_code': response.status_code,
                        'success': response.status_code == 200,
                        'timestamp': datetime.now().isoformat()
                    }
                    
                    self.results['concurrent_operations'].append(result)
                    
                except Exception as e:
                    request_end_time = time.time()
                    result = {
                        'operation': f'mixed_operation_{operation_type}',
                        'response_time': (request_end_time - start_time) * 1000,
                        'status_code': 0,
                        'success': False,
                        'error': str(e),
                        'timestamp': datetime.now().isoformat()
                    }
                    self.results['concurrent_operations'].append(result)
                
                operation_count += 1
                time.sleep(0.1)  # 短暂间隔
        
        with ThreadPoolExecutor(max_workers=concurrent_users) as executor:
            futures = [executor.submit(mixed_operations) for _ in range(concurrent_users)]
            
            for future in as_completed(futures):
                future.result()
    
    def analyze_results(self):
        """分析测试结果"""
        print("\n" + "="*50)
        print("性能测试结果分析")
        print("="*50)
        
        analysis = {}
        
        for category, operations in self.results.items():
            if not operations:
                continue
                
            print(f"\n{category.upper()} 性能分析:")
            print("-" * 30)
            
            # 按操作类型分组
            ops_by_type = {}
            for op in operations:
                op_type = op['operation']
                if op_type not in ops_by_type:
                    ops_by_type[op_type] = []
                ops_by_type[op_type].append(op)
            
            category_analysis = {}
            
            for op_type, ops in ops_by_type.items():
                response_times = [op['response_time'] for op in ops]
                success_count = sum(1 for op in ops if op['success'])
                total_count = len(ops)
                
                if response_times:
                    avg_response = statistics.mean(response_times)
                    median_response = statistics.median(response_times)
                    p95_response = sorted(response_times)[int(len(response_times) * 0.95)]
                    max_response = max(response_times)
                    min_response = min(response_times)
                    
                    success_rate = (success_count / total_count) * 100
                    
                    op_analysis = {
                        'total_requests': total_count,
                        'success_count': success_count,
                        'success_rate': success_rate,
                        'avg_response_time': avg_response,
                        'median_response_time': median_response,
                        'p95_response_time': p95_response,
                        'max_response_time': max_response,
                        'min_response_time': min_response
                    }
                    
                    category_analysis[op_type] = op_analysis
                    
                    print(f"  {op_type}:")
                    print(f"    总请求数: {total_count}")
                    print(f"    成功请求数: {success_count}")
                    print(f"    成功率: {success_rate:.2f}%")
                    print(f"    平均响应时间: {avg_response:.2f}ms")
                    print(f"    中位数响应时间: {median_response:.2f}ms")
                    print(f"    95%响应时间: {p95_response:.2f}ms")
                    print(f"    最大响应时间: {max_response:.2f}ms")
                    print(f"    最小响应时间: {min_response:.2f}ms")
                    
                    # 识别性能瓶颈
                    if avg_response > 2000:
                        print(f"    ⚠️  性能瓶颈: 平均响应时间过长 ({avg_response:.2f}ms)")
                    if success_rate < 95:
                        print(f"    ⚠️  可靠性问题: 成功率过低 ({success_rate:.2f}%)")
                    if p95_response > 5000:
                        print(f"    ⚠️  性能瓶颈: 95%响应时间过长 ({p95_response:.2f}ms)")
            
            analysis[category] = category_analysis
        
        return analysis
    
    def generate_report(self, analysis):
        """生成性能测试报告"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        report_dir = f"test/performance/reports/baseline_{timestamp}"
        os.makedirs(report_dir, exist_ok=True)
        
        # 保存原始数据
        with open(f"{report_dir}/raw_results.json", 'w', encoding='utf-8') as f:
            json.dump(self.results, f, ensure_ascii=False, indent=2)
        
        # 保存分析结果
        with open(f"{report_dir}/analysis.json", 'w', encoding='utf-8') as f:
            json.dump(analysis, f, ensure_ascii=False, indent=2)
        
        # 生成性能瓶颈报告
        bottlenecks = []
        recommendations = []
        
        for category, ops in analysis.items():
            for op_type, metrics in ops.items():
                if metrics['avg_response_time'] > 2000:
                    bottlenecks.append({
                        'operation': op_type,
                        'issue': '平均响应时间过长',
                        'value': f"{metrics['avg_response_time']:.2f}ms",
                        'severity': 'high'
                    })
                    recommendations.append(f"{op_type}: 考虑引入异步处理优化响应时间")
                
                if metrics['success_rate'] < 95:
                    bottlenecks.append({
                        'operation': op_type,
                        'issue': '成功率过低',
                        'value': f"{metrics['success_rate']:.2f}%",
                        'severity': 'critical'
                    })
                    recommendations.append(f"{op_type}: 需要提高系统稳定性和错误处理")
                
                if metrics['p95_response_time'] > 5000:
                    bottlenecks.append({
                        'operation': op_type,
                        'issue': '95%响应时间过长',
                        'value': f"{metrics['p95_response_time']:.2f}ms",
                        'severity': 'medium'
                    })
                    recommendations.append(f"{op_type}: 考虑使用消息队列进行异步处理")
        
        # 生成优化建议报告
        optimization_report = {
            'test_timestamp': timestamp,
            'bottlenecks': bottlenecks,
            'recommendations': recommendations,
            'rocketmq_optimization_targets': [
                '订单创建流程异步化',
                '库存操作异步化',
                '用户通知异步化',
                '支付流程异步化'
            ]
        }
        
        with open(f"{report_dir}/optimization_recommendations.json", 'w', encoding='utf-8') as f:
            json.dump(optimization_report, f, ensure_ascii=False, indent=2)
        
        print(f"\n性能测试报告已生成: {report_dir}")
        print(f"\n发现 {len(bottlenecks)} 个性能瓶颈")
        print(f"生成 {len(recommendations)} 条优化建议")
        
        return report_dir
    
    def run_baseline_test(self):
        """运行完整的基线性能测试"""
        print("开始基线性能测试...")
        start_time = time.time()
        
        # 准备测试数据
        self.setup_test_data()
        
        # 执行各项性能测试
        self.test_user_registration_performance()
        self.test_user_login_performance()
        self.test_stock_query_performance()
        self.test_order_creation_performance()
        self.test_concurrent_mixed_operations()
        
        # 分析结果
        analysis = self.analyze_results()
        
        # 生成报告
        report_dir = self.generate_report(analysis)
        
        end_time = time.time()
        print(f"\n基线性能测试完成，耗时: {end_time - start_time:.2f}秒")
        
        return report_dir

if __name__ == "__main__":
    test = BaselinePerformanceTest()
    report_dir = test.run_baseline_test()
    print(f"\n测试报告保存在: {report_dir}")
    print("\n基于测试结果，可以针对性地引入RocketMQ进行性能优化")