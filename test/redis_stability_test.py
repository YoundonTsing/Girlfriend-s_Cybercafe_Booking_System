#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Redis连接稳定性测试程序
测试连接池配置和重试机制的效果
"""

import redis
import time
import threading
import concurrent.futures
import statistics
from datetime import datetime
import json

class RedisStabilityTester:
    def __init__(self, host='localhost', port=6379, db=0, password=None):
        self.host = host
        self.port = port
        self.db = db
        self.password = password
        
        # 连接池配置 - 对应Java配置的参数
        self.pool = redis.ConnectionPool(
            host=host,
            port=port,
            db=db,
            password=password,
            max_connections=64,  # 对应connectionPoolSize
            retry_on_timeout=True,
            socket_connect_timeout=5,
            socket_timeout=5
        )
        
        self.redis_client = redis.Redis(connection_pool=self.pool)
        self.test_results = {
            'connection_pool_tests': [],
            'retry_mechanism_tests': [],
            'concurrent_access_tests': [],
            'performance_metrics': {}
        }
    
    def test_connection_pool_efficiency(self, num_operations=1000):
        """测试连接池效率"""
        print(f"\n=== 连接池效率测试 (操作数: {num_operations}) ===")
        
        start_time = time.time()
        success_count = 0
        error_count = 0
        response_times = []
        
        for i in range(num_operations):
            try:
                op_start = time.time()
                
                # 执行Redis操作
                key = f"test_pool_{i}"
                self.redis_client.set(key, f"value_{i}", ex=60)
                result = self.redis_client.get(key)
                
                op_end = time.time()
                response_times.append((op_end - op_start) * 1000)  # 转换为毫秒
                success_count += 1
                
                if i % 100 == 0:
                    print(f"已完成 {i}/{num_operations} 操作")
                    
            except Exception as e:
                error_count += 1
                print(f"操作 {i} 失败: {e}")
        
        end_time = time.time()
        total_time = end_time - start_time
        
        # 计算统计数据
        avg_response_time = statistics.mean(response_times) if response_times else 0
        p95_response_time = statistics.quantiles(response_times, n=20)[18] if len(response_times) > 20 else 0
        
        pool_stats = {
            'total_operations': num_operations,
            'success_count': success_count,
            'error_count': error_count,
            'success_rate': (success_count / num_operations) * 100,
            'total_time_seconds': total_time,
            'operations_per_second': num_operations / total_time,
            'avg_response_time_ms': avg_response_time,
            'p95_response_time_ms': p95_response_time,
            'pool_max_connections': 64
        }
        
        self.test_results['connection_pool_tests'].append(pool_stats)
        
        print(f"成功率: {pool_stats['success_rate']:.2f}%")
        print(f"平均响应时间: {avg_response_time:.2f}ms")
        print(f"P95响应时间: {p95_response_time:.2f}ms")
        print(f"每秒操作数: {pool_stats['operations_per_second']:.2f}")
        
        return pool_stats
    
    def test_concurrent_access(self, num_threads=20, operations_per_thread=50):
        """测试并发访问能力"""
        print(f"\n=== 并发访问测试 (线程数: {num_threads}, 每线程操作数: {operations_per_thread}) ===")
        
        def worker_thread(thread_id):
            thread_results = {'success': 0, 'error': 0, 'response_times': []}
            
            for i in range(operations_per_thread):
                try:
                    start_time = time.time()
                    
                    key = f"concurrent_test_{thread_id}_{i}"
                    self.redis_client.set(key, f"thread_{thread_id}_value_{i}", ex=60)
                    result = self.redis_client.get(key)
                    
                    end_time = time.time()
                    thread_results['response_times'].append((end_time - start_time) * 1000)
                    thread_results['success'] += 1
                    
                except Exception as e:
                    thread_results['error'] += 1
                    print(f"线程 {thread_id} 操作 {i} 失败: {e}")
            
            return thread_results
        
        start_time = time.time()
        
        with concurrent.futures.ThreadPoolExecutor(max_workers=num_threads) as executor:
            futures = [executor.submit(worker_thread, i) for i in range(num_threads)]
            thread_results = [future.result() for future in concurrent.futures.as_completed(futures)]
        
        end_time = time.time()
        
        # 汇总结果
        total_success = sum(r['success'] for r in thread_results)
        total_error = sum(r['error'] for r in thread_results)
        total_operations = num_threads * operations_per_thread
        all_response_times = []
        for r in thread_results:
            all_response_times.extend(r['response_times'])
        
        concurrent_stats = {
            'num_threads': num_threads,
            'operations_per_thread': operations_per_thread,
            'total_operations': total_operations,
            'total_success': total_success,
            'total_error': total_error,
            'success_rate': (total_success / total_operations) * 100,
            'total_time_seconds': end_time - start_time,
            'concurrent_ops_per_second': total_operations / (end_time - start_time),
            'avg_response_time_ms': statistics.mean(all_response_times) if all_response_times else 0,
            'p95_response_time_ms': statistics.quantiles(all_response_times, n=20)[18] if len(all_response_times) > 20 else 0
        }
        
        self.test_results['concurrent_access_tests'].append(concurrent_stats)
        
        print(f"并发成功率: {concurrent_stats['success_rate']:.2f}%")
        print(f"并发每秒操作数: {concurrent_stats['concurrent_ops_per_second']:.2f}")
        print(f"并发平均响应时间: {concurrent_stats['avg_response_time_ms']:.2f}ms")
        
        return concurrent_stats
    
    def test_retry_mechanism(self, num_tests=10):
        """测试重试机制（模拟网络异常）"""
        print(f"\n=== 重试机制测试 (测试次数: {num_tests}) ===")
        
        retry_results = []
        
        for i in range(num_tests):
            try:
                # 使用短超时来模拟网络问题
                test_client = redis.Redis(
                    host=self.host,
                    port=self.port,
                    db=self.db,
                    password=self.password,
                    socket_timeout=0.1,  # 极短超时
                    retry_on_timeout=True
                )
                
                start_time = time.time()
                key = f"retry_test_{i}"
                
                try:
                    test_client.set(key, f"retry_value_{i}")
                    result = test_client.get(key)
                    success = True
                except Exception as e:
                    success = False
                    print(f"重试测试 {i} 失败: {e}")
                
                end_time = time.time()
                
                retry_results.append({
                    'test_id': i,
                    'success': success,
                    'response_time_ms': (end_time - start_time) * 1000
                })
                
            except Exception as e:
                print(f"重试测试 {i} 配置失败: {e}")
                retry_results.append({
                    'test_id': i,
                    'success': False,
                    'response_time_ms': 0
                })
        
        success_count = sum(1 for r in retry_results if r['success'])
        retry_stats = {
            'total_tests': num_tests,
            'success_count': success_count,
            'success_rate': (success_count / num_tests) * 100,
            'avg_response_time_ms': statistics.mean([r['response_time_ms'] for r in retry_results if r['success']])
        }
        
        self.test_results['retry_mechanism_tests'].append(retry_stats)
        
        print(f"重试成功率: {retry_stats['success_rate']:.2f}%")
        print(f"重试平均响应时间: {retry_stats['avg_response_time_ms']:.2f}ms")
        
        return retry_stats
    
    def run_comprehensive_test(self):
        """运行综合测试"""
        print("\n" + "="*60)
        print("Redis连接稳定性综合测试开始")
        print(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print("="*60)
        
        try:
            # 测试基本连接
            print("\n>>> 测试Redis连接...")
            self.redis_client.ping()
            print("✓ Redis连接正常")
            
            # 1. 连接池效率测试
            pool_stats = self.test_connection_pool_efficiency(1000)
            
            # 2. 并发访问测试
            concurrent_stats = self.test_concurrent_access(20, 50)
            
            # 3. 重试机制测试
            retry_stats = self.test_retry_mechanism(10)
            
            # 生成测试报告
            self.generate_test_report()
            
        except Exception as e:
            print(f"测试过程中发生错误: {e}")
            return False
        
        return True
    
    def generate_test_report(self):
        """生成测试报告"""
        print("\n" + "="*60)
        print("测试报告")
        print("="*60)
        
        # 连接池测试结果
        if self.test_results['connection_pool_tests']:
            pool_test = self.test_results['connection_pool_tests'][0]
            print(f"\n📊 连接池性能:")
            print(f"  - 成功率: {pool_test['success_rate']:.2f}%")
            print(f"  - 每秒操作数: {pool_test['operations_per_second']:.2f}")
            print(f"  - 平均响应时间: {pool_test['avg_response_time_ms']:.2f}ms")
            print(f"  - P95响应时间: {pool_test['p95_response_time_ms']:.2f}ms")
        
        # 并发测试结果
        if self.test_results['concurrent_access_tests']:
            concurrent_test = self.test_results['concurrent_access_tests'][0]
            print(f"\n🚀 并发性能:")
            print(f"  - 并发成功率: {concurrent_test['success_rate']:.2f}%")
            print(f"  - 并发每秒操作数: {concurrent_test['concurrent_ops_per_second']:.2f}")
            print(f"  - 并发平均响应时间: {concurrent_test['avg_response_time_ms']:.2f}ms")
        
        # 重试机制结果
        if self.test_results['retry_mechanism_tests']:
            retry_test = self.test_results['retry_mechanism_tests'][0]
            print(f"\n🔄 重试机制:")
            print(f"  - 重试成功率: {retry_test['success_rate']:.2f}%")
            print(f"  - 重试平均响应时间: {retry_test['avg_response_time_ms']:.2f}ms")
        
        # 保存详细报告到文件
        report_file = f"redis_stability_test_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        with open(report_file, 'w', encoding='utf-8') as f:
            json.dump(self.test_results, f, indent=2, ensure_ascii=False)
        
        print(f"\n📄 详细报告已保存到: {report_file}")
        
        # 给出优化建议
        self.provide_optimization_suggestions()
    
    def provide_optimization_suggestions(self):
        """提供优化建议"""
        print("\n" + "="*60)
        print("优化建议")
        print("="*60)
        
        suggestions = []
        
        # 基于测试结果给出建议
        if self.test_results['connection_pool_tests']:
            pool_test = self.test_results['connection_pool_tests'][0]
            if pool_test['success_rate'] < 99:
                suggestions.append("🔧 连接池成功率偏低，建议增加连接池大小或检查网络稳定性")
            if pool_test['avg_response_time_ms'] > 10:
                suggestions.append("⚡ 平均响应时间较高，建议优化网络配置或增加最小空闲连接数")
        
        if self.test_results['concurrent_access_tests']:
            concurrent_test = self.test_results['concurrent_access_tests'][0]
            if concurrent_test['success_rate'] < 95:
                suggestions.append("🚀 并发成功率偏低，建议增加连接池大小至128或优化超时配置")
        
        if self.test_results['retry_mechanism_tests']:
            retry_test = self.test_results['retry_mechanism_tests'][0]
            if retry_test['success_rate'] < 80:
                suggestions.append("🔄 重试机制效果不佳，建议调整重试次数至5次或增加重试间隔")
        
        if not suggestions:
            suggestions.append("✅ 当前配置表现良好，建议继续监控生产环境性能")
        
        for i, suggestion in enumerate(suggestions, 1):
            print(f"{i}. {suggestion}")
        
        print("\n💡 下一步建议:")
        print("1. 在生产环境中部署配置")
        print("2. 设置监控告警阈值")
        print("3. 定期运行此测试脚本")
        print("4. 根据实际负载调整参数")

def main():
    """主函数"""
    print("Redis连接稳定性测试程序")
    print("测试Java Redisson配置的连接池和重试机制效果")
    
    # 配置Redis连接参数（根据实际环境调整）
    tester = RedisStabilityTester(
        host='localhost',
        port=6379,
        db=0,
        password=None  # 如果有密码请设置
    )
    
    # 运行综合测试
    success = tester.run_comprehensive_test()
    
    if success:
        print("\n✅ 测试完成！")
    else:
        print("\n❌ 测试失败！")
        return 1
    
    return 0

if __name__ == "__main__":
    exit(main())