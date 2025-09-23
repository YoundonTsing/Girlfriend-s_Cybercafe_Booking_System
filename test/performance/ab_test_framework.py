#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
A/B测试框架
用于对比RocketMQ优化前后的性能差异
支持流量分割、性能对比、效果分析
"""

import requests
import time
import threading
import json
import statistics
import random
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timedelta
import matplotlib.pyplot as plt
import pandas as pd
import os
from dataclasses import dataclass
from typing import Dict, List, Any

@dataclass
class TestConfig:
    """测试配置"""
    name: str
    base_url: str
    concurrent_users: int
    test_duration: int
    traffic_ratio: float  # 流量分配比例
    enable_async: bool    # 是否启用异步处理

class ABTestFramework:
    def __init__(self):
        self.test_configs = {
            'baseline': TestConfig(
                name='基线版本',
                base_url='http://localhost:8080',
                concurrent_users=20,
                test_duration=300,  # 5分钟
                traffic_ratio=0.5,
                enable_async=False
            ),
            'optimized': TestConfig(
                name='RocketMQ优化版本',
                base_url='http://localhost:8080',
                concurrent_users=20,
                test_duration=300,  # 5分钟
                traffic_ratio=0.5,
                enable_async=True
            )
        }
        
        self.results = {
            'baseline': [],
            'optimized': []
        }
        
        self.test_users = []
        self.metrics = {
            'baseline': {},
            'optimized': {}
        }
    
    def setup_test_environment(self):
        """设置测试环境"""
        print("设置A/B测试环境...")
        
        # 创建测试用户
        for i in range(100):
            user_data = {
                "username": f"abtest_user_{i}_{int(time.time())}",
                "password": "123456",
                "email": f"abtest_{i}@example.com",
                "phone": f"1380000{i:04d}"
            }
            
            try:
                response = requests.post(f"{self.test_configs['baseline'].base_url}/user/register", 
                                       json=user_data, timeout=10)
                if response.status_code == 200:
                    self.test_users.append(user_data)
            except Exception as e:
                print(f"创建测试用户失败: {e}")
        
        print(f"成功创建 {len(self.test_users)} 个测试用户")
    
    def execute_operation(self, config: TestConfig, operation_type: str, user_index: int) -> Dict[str, Any]:
        """执行单个操作"""
        start_time = time.time()
        
        try:
            if operation_type == 'user_register':
                user_data = {
                    "username": f"test_{config.name}_{int(time.time() * 1000000)}",
                    "password": "123456",
                    "email": f"test_{int(time.time())}@example.com",
                    "phone": f"139{random.randint(10000000, 99999999)}"
                }
                response = requests.post(f"{config.base_url}/user/register", json=user_data, timeout=10)
                
            elif operation_type == 'user_login':
                if self.test_users:
                    user = self.test_users[user_index % len(self.test_users)]
                    login_data = {
                        "username": user["username"],
                        "password": user["password"]
                    }
                    response = requests.post(f"{config.base_url}/user/login", json=login_data, timeout=10)
                else:
                    raise Exception("No test users available")
                    
            elif operation_type == 'stock_query':
                show_id = random.randint(1, 10)
                response = requests.get(f"{config.base_url}/show/{show_id}/stock", timeout=10)
                
            elif operation_type == 'order_create':
                order_data = {
                    "showId": random.randint(1, 5),
                    "seatIds": [random.randint(1, 100)],
                    "totalAmount": random.uniform(50, 500)
                }
                
                headers = {}
                # 如果启用异步处理，添加特殊标头
                if config.enable_async:
                    headers['X-Async-Processing'] = 'true'
                
                response = requests.post(f"{config.base_url}/order/create", 
                                       json=order_data, headers=headers, timeout=15)
            else:
                raise Exception(f"Unknown operation type: {operation_type}")
            
            end_time = time.time()
            
            return {
                'config_name': config.name,
                'operation': operation_type,
                'response_time': (end_time - start_time) * 1000,
                'status_code': response.status_code,
                'success': response.status_code == 200,
                'timestamp': datetime.now().isoformat(),
                'async_enabled': config.enable_async
            }
            
        except Exception as e:
            end_time = time.time()
            return {
                'config_name': config.name,
                'operation': operation_type,
                'response_time': (end_time - start_time) * 1000,
                'status_code': 0,
                'success': False,
                'error': str(e),
                'timestamp': datetime.now().isoformat(),
                'async_enabled': config.enable_async
            }
    
    def run_traffic_split_test(self, operation_type: str, total_requests: int = 1000):
        """运行流量分割测试"""
        print(f"\n运行流量分割测试: {operation_type}")
        print(f"总请求数: {total_requests}")
        
        def worker(request_index):
            # 根据流量比例决定使用哪个配置
            if random.random() < self.test_configs['baseline'].traffic_ratio:
                config = self.test_configs['baseline']
                result_key = 'baseline'
            else:
                config = self.test_configs['optimized']
                result_key = 'optimized'
            
            result = self.execute_operation(config, operation_type, request_index)
            self.results[result_key].append(result)
            
            return result
        
        # 并发执行测试
        max_workers = max(self.test_configs['baseline'].concurrent_users,
                         self.test_configs['optimized'].concurrent_users)
        
        with ThreadPoolExecutor(max_workers=max_workers) as executor:
            futures = [executor.submit(worker, i) for i in range(total_requests)]
            
            completed = 0
            for future in as_completed(futures):
                future.result()
                completed += 1
                if completed % 100 == 0:
                    print(f"已完成 {completed}/{total_requests} 请求")
    
    def run_sustained_load_test(self, duration_minutes: int = 10):
        """运行持续负载测试"""
        print(f"\n运行持续负载测试: {duration_minutes} 分钟")
        
        end_time = time.time() + (duration_minutes * 60)
        
        def sustained_worker(config_name):
            config = self.test_configs[config_name]
            request_count = 0
            
            while time.time() < end_time:
                # 随机选择操作类型
                operation_types = ['user_login', 'stock_query', 'order_create', 'user_register']
                weights = [0.3, 0.4, 0.2, 0.1]  # 操作权重
                operation_type = random.choices(operation_types, weights=weights)[0]
                
                result = self.execute_operation(config, operation_type, request_count)
                self.results[config_name].append(result)
                
                request_count += 1
                
                # 控制请求频率
                time.sleep(random.uniform(0.1, 0.5))
        
        # 为每个配置启动工作线程
        threads = []
        for config_name in self.test_configs.keys():
            config = self.test_configs[config_name]
            for _ in range(config.concurrent_users):
                thread = threading.Thread(target=sustained_worker, args=(config_name,))
                threads.append(thread)
                thread.start()
        
        # 等待所有线程完成
        for thread in threads:
            thread.join()
    
    def analyze_ab_results(self) -> Dict[str, Any]:
        """分析A/B测试结果"""
        print("\n" + "="*60)
        print("A/B测试结果分析")
        print("="*60)
        
        analysis = {}
        
        for config_name, results in self.results.items():
            if not results:
                continue
            
            print(f"\n{config_name.upper()} 版本分析:")
            print("-" * 40)
            
            # 按操作类型分组分析
            ops_by_type = {}
            for result in results:
                op_type = result['operation']
                if op_type not in ops_by_type:
                    ops_by_type[op_type] = []
                ops_by_type[op_type].append(result)
            
            config_analysis = {}
            
            for op_type, ops in ops_by_type.items():
                response_times = [op['response_time'] for op in ops]
                success_count = sum(1 for op in ops if op['success'])
                total_count = len(ops)
                
                if response_times:
                    metrics = {
                        'total_requests': total_count,
                        'success_count': success_count,
                        'success_rate': (success_count / total_count) * 100,
                        'avg_response_time': statistics.mean(response_times),
                        'median_response_time': statistics.median(response_times),
                        'p95_response_time': sorted(response_times)[int(len(response_times) * 0.95)],
                        'p99_response_time': sorted(response_times)[int(len(response_times) * 0.99)],
                        'max_response_time': max(response_times),
                        'min_response_time': min(response_times),
                        'std_response_time': statistics.stdev(response_times) if len(response_times) > 1 else 0
                    }
                    
                    config_analysis[op_type] = metrics
                    
                    print(f"  {op_type}:")
                    print(f"    总请求数: {metrics['total_requests']}")
                    print(f"    成功率: {metrics['success_rate']:.2f}%")
                    print(f"    平均响应时间: {metrics['avg_response_time']:.2f}ms")
                    print(f"    中位数响应时间: {metrics['median_response_time']:.2f}ms")
                    print(f"    95%响应时间: {metrics['p95_response_time']:.2f}ms")
                    print(f"    99%响应时间: {metrics['p99_response_time']:.2f}ms")
                    print(f"    响应时间标准差: {metrics['std_response_time']:.2f}ms")
            
            analysis[config_name] = config_analysis
        
        # 对比分析
        if 'baseline' in analysis and 'optimized' in analysis:
            print(f"\n性能对比分析:")
            print("-" * 40)
            
            comparison = {}
            
            for op_type in set(analysis['baseline'].keys()) & set(analysis['optimized'].keys()):
                baseline_metrics = analysis['baseline'][op_type]
                optimized_metrics = analysis['optimized'][op_type]
                
                # 计算改进百分比
                response_time_improvement = (
                    (baseline_metrics['avg_response_time'] - optimized_metrics['avg_response_time']) /
                    baseline_metrics['avg_response_time'] * 100
                )
                
                success_rate_improvement = (
                    optimized_metrics['success_rate'] - baseline_metrics['success_rate']
                )
                
                p95_improvement = (
                    (baseline_metrics['p95_response_time'] - optimized_metrics['p95_response_time']) /
                    baseline_metrics['p95_response_time'] * 100
                )
                
                comparison[op_type] = {
                    'response_time_improvement': response_time_improvement,
                    'success_rate_improvement': success_rate_improvement,
                    'p95_improvement': p95_improvement
                }
                
                print(f"  {op_type}:")
                print(f"    平均响应时间改进: {response_time_improvement:+.2f}%")
                print(f"    成功率改进: {success_rate_improvement:+.2f}%")
                print(f"    95%响应时间改进: {p95_improvement:+.2f}%")
                
                # 判断改进效果
                if response_time_improvement > 10:
                    print(f"    ✅ 响应时间显著改进")
                elif response_time_improvement > 0:
                    print(f"    ✅ 响应时间轻微改进")
                else:
                    print(f"    ❌ 响应时间未改进")
            
            analysis['comparison'] = comparison
        
        return analysis
    
    def generate_ab_report(self, analysis: Dict[str, Any]):
        """生成A/B测试报告"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        report_dir = f"test/performance/reports/ab_test_{timestamp}"
        os.makedirs(report_dir, exist_ok=True)
        
        # 保存原始数据
        with open(f"{report_dir}/ab_test_results.json", 'w', encoding='utf-8') as f:
            json.dump(self.results, f, ensure_ascii=False, indent=2)
        
        # 保存分析结果
        with open(f"{report_dir}/ab_analysis.json", 'w', encoding='utf-8') as f:
            json.dump(analysis, f, ensure_ascii=False, indent=2)
        
        # 生成性能对比图表
        if 'comparison' in analysis:
            self.generate_comparison_charts(analysis, report_dir)
        
        # 生成结论报告
        conclusion = self.generate_conclusion(analysis)
        with open(f"{report_dir}/conclusion.json", 'w', encoding='utf-8') as f:
            json.dump(conclusion, f, ensure_ascii=False, indent=2)
        
        print(f"\nA/B测试报告已生成: {report_dir}")
        return report_dir
    
    def generate_comparison_charts(self, analysis: Dict[str, Any], report_dir: str):
        """生成对比图表"""
        if 'comparison' not in analysis:
            return
        
        comparison = analysis['comparison']
        operations = list(comparison.keys())
        
        # 响应时间改进图
        response_improvements = [comparison[op]['response_time_improvement'] for op in operations]
        
        plt.figure(figsize=(12, 6))
        bars = plt.bar(operations, response_improvements, 
                      color=['green' if x > 0 else 'red' for x in response_improvements])
        plt.title('RocketMQ优化 - 响应时间改进对比')
        plt.ylabel('改进百分比 (%)')
        plt.xlabel('操作类型')
        plt.axhline(y=0, color='black', linestyle='-', alpha=0.3)
        
        # 添加数值标签
        for bar, value in zip(bars, response_improvements):
            plt.text(bar.get_x() + bar.get_width()/2, bar.get_height() + (1 if value > 0 else -3),
                    f'{value:.1f}%', ha='center', va='bottom' if value > 0 else 'top')
        
        plt.tight_layout()
        plt.savefig(f"{report_dir}/response_time_improvement.png", dpi=300, bbox_inches='tight')
        plt.close()
        
        # 95%响应时间改进图
        p95_improvements = [comparison[op]['p95_improvement'] for op in operations]
        
        plt.figure(figsize=(12, 6))
        bars = plt.bar(operations, p95_improvements,
                      color=['green' if x > 0 else 'red' for x in p95_improvements])
        plt.title('RocketMQ优化 - 95%响应时间改进对比')
        plt.ylabel('改进百分比 (%)')
        plt.xlabel('操作类型')
        plt.axhline(y=0, color='black', linestyle='-', alpha=0.3)
        
        # 添加数值标签
        for bar, value in zip(bars, p95_improvements):
            plt.text(bar.get_x() + bar.get_width()/2, bar.get_height() + (1 if value > 0 else -3),
                    f'{value:.1f}%', ha='center', va='bottom' if value > 0 else 'top')
        
        plt.tight_layout()
        plt.savefig(f"{report_dir}/p95_improvement.png", dpi=300, bbox_inches='tight')
        plt.close()
    
    def generate_conclusion(self, analysis: Dict[str, Any]) -> Dict[str, Any]:
        """生成测试结论"""
        conclusion = {
            'test_timestamp': datetime.now().isoformat(),
            'overall_assessment': '',
            'key_improvements': [],
            'performance_gains': {},
            'recommendations': []
        }
        
        if 'comparison' in analysis:
            comparison = analysis['comparison']
            
            # 计算总体改进
            total_response_improvement = statistics.mean(
                [comp['response_time_improvement'] for comp in comparison.values()]
            )
            
            total_p95_improvement = statistics.mean(
                [comp['p95_improvement'] for comp in comparison.values()]
            )
            
            conclusion['performance_gains'] = {
                'avg_response_time_improvement': total_response_improvement,
                'avg_p95_improvement': total_p95_improvement
            }
            
            # 评估总体效果
            if total_response_improvement > 15:
                conclusion['overall_assessment'] = 'RocketMQ优化效果显著，建议全面推广'
            elif total_response_improvement > 5:
                conclusion['overall_assessment'] = 'RocketMQ优化效果良好，建议逐步推广'
            elif total_response_improvement > 0:
                conclusion['overall_assessment'] = 'RocketMQ优化有轻微改进，需要进一步调优'
            else:
                conclusion['overall_assessment'] = 'RocketMQ优化效果不明显，需要重新评估方案'
            
            # 识别关键改进点
            for op_type, comp in comparison.items():
                if comp['response_time_improvement'] > 10:
                    conclusion['key_improvements'].append(f"{op_type}: 响应时间改进{comp['response_time_improvement']:.1f}%")
            
            # 生成建议
            if total_response_improvement > 10:
                conclusion['recommendations'].extend([
                    '建议在生产环境中部署RocketMQ优化方案',
                    '继续监控性能指标，确保优化效果持续',
                    '考虑扩展异步处理到更多业务场景'
                ])
            else:
                conclusion['recommendations'].extend([
                    '需要进一步分析性能瓶颈',
                    '考虑调整RocketMQ配置参数',
                    '评估是否需要其他优化手段'
                ])
        
        return conclusion
    
    def run_full_ab_test(self):
        """运行完整的A/B测试"""
        print("开始A/B测试...")
        start_time = time.time()
        
        # 设置测试环境
        self.setup_test_environment()
        
        # 执行流量分割测试
        test_operations = ['user_register', 'user_login', 'stock_query', 'order_create']
        for operation in test_operations:
            self.run_traffic_split_test(operation, total_requests=200)
        
        # 执行持续负载测试
        self.run_sustained_load_test(duration_minutes=5)
        
        # 分析结果
        analysis = self.analyze_ab_results()
        
        # 生成报告
        report_dir = self.generate_ab_report(analysis)
        
        end_time = time.time()
        print(f"\nA/B测试完成，耗时: {end_time - start_time:.2f}秒")
        
        return report_dir

if __name__ == "__main__":
    ab_test = ABTestFramework()
    report_dir = ab_test.run_full_ab_test()
    print(f"\nA/B测试报告保存在: {report_dir}")