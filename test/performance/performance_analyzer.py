#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
性能测试结果分析器
用于分析压力测试结果，识别性能瓶颈并生成优化建议
"""

import json
import os
import glob
import matplotlib.pyplot as plt
import pandas as pd
from datetime import datetime
import numpy as np
from typing import Dict, List, Any

class PerformanceAnalyzer:
    def __init__(self, reports_dir="."):
        self.reports_dir = reports_dir
        self.analysis_results = {}
        
    def load_test_reports(self) -> List[Dict]:
        """加载所有性能测试报告"""
        reports = []
        pattern = os.path.join(self.reports_dir, "performance_report_*.json")
        
        for file_path in glob.glob(pattern):
            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    report = json.load(f)
                    report['file_path'] = file_path
                    reports.append(report)
                    print(f"已加载报告: {file_path}")
            except Exception as e:
                print(f"加载报告失败 {file_path}: {e}")
        
        return reports
    
    def analyze_performance_trends(self, reports: List[Dict]) -> Dict:
        """分析性能趋势"""
        trends = {
            'response_time_trends': {},
            'success_rate_trends': {},
            'throughput_trends': {}
        }
        
        for report in reports:
            scenario = self.extract_scenario_info(report['file_path'])
            
            for op_type, metrics in report['performance_metrics'].items():
                if op_type not in trends['response_time_trends']:
                    trends['response_time_trends'][op_type] = []
                    trends['success_rate_trends'][op_type] = []
                    trends['throughput_trends'][op_type] = []
                
                trends['response_time_trends'][op_type].append({
                    'scenario': scenario,
                    'avg_response_time': metrics['avg_response_time'],
                    'p95_response_time': metrics['p95_response_time'],
                    'p99_response_time': metrics['p99_response_time']
                })
                
                trends['success_rate_trends'][op_type].append({
                    'scenario': scenario,
                    'success_rate': metrics['success_rate']
                })
                
                # 计算吞吐量 (请求数/秒，假设测试时长)
                throughput = metrics['total_requests'] / 120  # 假设平均测试时长2分钟
                trends['throughput_trends'][op_type].append({
                    'scenario': scenario,
                    'throughput': throughput
                })
        
        return trends
    
    def identify_bottlenecks(self, reports: List[Dict]) -> Dict:
        """识别性能瓶颈"""
        bottlenecks = {
            'critical_issues': [],
            'warning_issues': [],
            'recommendations': []
        }
        
        # 定义性能阈值
        thresholds = {
            'critical_response_time': 3000,  # 3秒
            'warning_response_time': 1000,   # 1秒
            'critical_success_rate': 95,     # 95%
            'warning_success_rate': 98       # 98%
        }
        
        for report in reports:
            scenario = self.extract_scenario_info(report['file_path'])
            
            for op_type, metrics in report['performance_metrics'].items():
                # 检查响应时间
                if metrics['p95_response_time'] > thresholds['critical_response_time']:
                    bottlenecks['critical_issues'].append({
                        'type': 'high_response_time',
                        'operation': op_type,
                        'scenario': scenario,
                        'value': metrics['p95_response_time'],
                        'threshold': thresholds['critical_response_time'],
                        'description': f"{op_type}在{scenario}场景下P95响应时间过高: {metrics['p95_response_time']:.2f}ms"
                    })
                elif metrics['p95_response_time'] > thresholds['warning_response_time']:
                    bottlenecks['warning_issues'].append({
                        'type': 'moderate_response_time',
                        'operation': op_type,
                        'scenario': scenario,
                        'value': metrics['p95_response_time'],
                        'threshold': thresholds['warning_response_time'],
                        'description': f"{op_type}在{scenario}场景下P95响应时间较高: {metrics['p95_response_time']:.2f}ms"
                    })
                
                # 检查成功率
                if metrics['success_rate'] < thresholds['critical_success_rate']:
                    bottlenecks['critical_issues'].append({
                        'type': 'low_success_rate',
                        'operation': op_type,
                        'scenario': scenario,
                        'value': metrics['success_rate'],
                        'threshold': thresholds['critical_success_rate'],
                        'description': f"{op_type}在{scenario}场景下成功率过低: {metrics['success_rate']:.2f}%"
                    })
                elif metrics['success_rate'] < thresholds['warning_success_rate']:
                    bottlenecks['warning_issues'].append({
                        'type': 'moderate_success_rate',
                        'operation': op_type,
                        'scenario': scenario,
                        'value': metrics['success_rate'],
                        'threshold': thresholds['warning_success_rate'],
                        'description': f"{op_type}在{scenario}场景下成功率较低: {metrics['success_rate']:.2f}%"
                    })
        
        # 生成优化建议
        bottlenecks['recommendations'] = self.generate_recommendations(bottlenecks)
        
        return bottlenecks
    
    def generate_recommendations(self, bottlenecks: Dict) -> List[Dict]:
        """生成优化建议"""
        recommendations = []
        
        # 基于瓶颈类型生成建议
        operation_issues = {}
        for issue in bottlenecks['critical_issues'] + bottlenecks['warning_issues']:
            op = issue['operation']
            if op not in operation_issues:
                operation_issues[op] = []
            operation_issues[op].append(issue)
        
        for operation, issues in operation_issues.items():
            if operation == 'order_operations':
                recommendations.append({
                    'priority': 'high',
                    'category': 'async_processing',
                    'title': '订单处理异步化',
                    'description': '将订单创建和支付处理改为异步模式，使用消息队列解耦',
                    'implementation': [
                        '引入RocketMQ消息队列',
                        '设计订单创建Topic和支付处理Topic',
                        '实现异步订单状态更新机制',
                        '添加订单状态查询接口'
                    ],
                    'expected_improvement': '响应时间减少60-80%，吞吐量提升3-5倍'
                })
            
            elif operation == 'stock_operations':
                recommendations.append({
                    'priority': 'high',
                    'category': 'caching_optimization',
                    'title': '库存查询缓存优化',
                    'description': '优化库存查询和锁定机制，减少数据库压力',
                    'implementation': [
                        '实现Redis库存缓存预热',
                        '优化库存锁定算法，减少锁竞争',
                        '使用分布式锁替代数据库锁',
                        '实现库存变更的异步同步机制'
                    ],
                    'expected_improvement': '库存查询响应时间减少50-70%'
                })
            
            elif operation == 'user_operations':
                recommendations.append({
                    'priority': 'medium',
                    'category': 'authentication_optimization',
                    'title': '用户认证优化',
                    'description': '优化用户注册登录流程，提升用户体验',
                    'implementation': [
                        '实现JWT Token缓存',
                        '优化密码加密算法',
                        '添加用户信息缓存',
                        '实现单点登录(SSO)'
                    ],
                    'expected_improvement': '登录响应时间减少30-50%'
                })
            
            elif operation == 'payment_operations':
                recommendations.append({
                    'priority': 'high',
                    'category': 'payment_async',
                    'title': '支付处理异步化',
                    'description': '将支付处理改为异步模式，提升用户体验',
                    'implementation': [
                        '设计支付异步处理Topic',
                        '实现支付状态回调机制',
                        '添加支付重试和补偿机制',
                        '优化支付网关集成'
                    ],
                    'expected_improvement': '支付响应时间减少70-90%'
                })
        
        # 通用优化建议
        recommendations.extend([
            {
                'priority': 'medium',
                'category': 'database_optimization',
                'title': '数据库性能优化',
                'description': '优化数据库查询和连接池配置',
                'implementation': [
                    '添加必要的数据库索引',
                    '优化SQL查询语句',
                    '调整数据库连接池参数',
                    '实现读写分离'
                ],
                'expected_improvement': '数据库查询性能提升20-40%'
            },
            {
                'priority': 'medium',
                'category': 'jvm_optimization',
                'title': 'JVM参数调优',
                'description': '优化JVM内存和垃圾回收配置',
                'implementation': [
                    '调整堆内存大小',
                    '优化垃圾回收器选择',
                    '配置JVM监控参数',
                    '实现应用预热机制'
                ],
                'expected_improvement': '整体性能提升10-20%'
            }
        ])
        
        return recommendations
    
    def extract_scenario_info(self, file_path: str) -> str:
        """从文件路径提取场景信息"""
        filename = os.path.basename(file_path)
        if 'scenario_1' in filename:
            return '轻负载(10用户)'
        elif 'scenario_2' in filename:
            return '中等负载(50用户)'
        elif 'scenario_3' in filename:
            return '高负载(100用户)'
        else:
            return '未知场景'
    
    def generate_charts(self, trends: Dict, output_dir="charts"):
        """生成性能图表"""
        os.makedirs(output_dir, exist_ok=True)
        
        # 设置中文字体
        plt.rcParams['font.sans-serif'] = ['SimHei', 'Arial Unicode MS']
        plt.rcParams['axes.unicode_minus'] = False
        
        # 响应时间趋势图
        for op_type, data in trends['response_time_trends'].items():
            if not data:
                continue
                
            scenarios = [item['scenario'] for item in data]
            avg_times = [item['avg_response_time'] for item in data]
            p95_times = [item['p95_response_time'] for item in data]
            p99_times = [item['p99_response_time'] for item in data]
            
            plt.figure(figsize=(12, 6))
            x = range(len(scenarios))
            
            plt.plot(x, avg_times, 'o-', label='平均响应时间', linewidth=2)
            plt.plot(x, p95_times, 's-', label='P95响应时间', linewidth=2)
            plt.plot(x, p99_times, '^-', label='P99响应时间', linewidth=2)
            
            plt.xlabel('测试场景')
            plt.ylabel('响应时间 (ms)')
            plt.title(f'{op_type} - 响应时间趋势')
            plt.xticks(x, scenarios, rotation=45)
            plt.legend()
            plt.grid(True, alpha=0.3)
            plt.tight_layout()
            
            chart_path = os.path.join(output_dir, f'{op_type}_response_time.png')
            plt.savefig(chart_path, dpi=300, bbox_inches='tight')
            plt.close()
            
            print(f"已生成图表: {chart_path}")
        
        # 成功率趋势图
        plt.figure(figsize=(12, 6))
        for op_type, data in trends['success_rate_trends'].items():
            if not data:
                continue
            scenarios = [item['scenario'] for item in data]
            success_rates = [item['success_rate'] for item in data]
            plt.plot(range(len(scenarios)), success_rates, 'o-', label=op_type, linewidth=2)
        
        plt.xlabel('测试场景')
        plt.ylabel('成功率 (%)')
        plt.title('各操作成功率趋势')
        plt.xticks(range(len(scenarios)), scenarios, rotation=45)
        plt.legend()
        plt.grid(True, alpha=0.3)
        plt.ylim(0, 105)
        plt.tight_layout()
        
        chart_path = os.path.join(output_dir, 'success_rate_trends.png')
        plt.savefig(chart_path, dpi=300, bbox_inches='tight')
        plt.close()
        
        print(f"已生成图表: {chart_path}")
    
    def generate_analysis_report(self, reports: List[Dict]) -> Dict:
        """生成完整的分析报告"""
        print("开始分析性能测试结果...")
        
        # 分析性能趋势
        trends = self.analyze_performance_trends(reports)
        
        # 识别瓶颈
        bottlenecks = self.identify_bottlenecks(reports)
        
        # 生成图表
        self.generate_charts(trends)
        
        # 汇总分析结果
        analysis_report = {
            'analysis_timestamp': datetime.now().isoformat(),
            'test_reports_analyzed': len(reports),
            'performance_trends': trends,
            'bottleneck_analysis': bottlenecks,
            'summary': self.generate_executive_summary(bottlenecks),
            'next_steps': self.generate_next_steps(bottlenecks)
        }
        
        return analysis_report
    
    def generate_executive_summary(self, bottlenecks: Dict) -> Dict:
        """生成执行摘要"""
        return {
            'critical_issues_count': len(bottlenecks['critical_issues']),
            'warning_issues_count': len(bottlenecks['warning_issues']),
            'top_priority_recommendations': [
                rec for rec in bottlenecks['recommendations'] 
                if rec['priority'] == 'high'
            ][:3],
            'overall_assessment': self.assess_overall_performance(bottlenecks)
        }
    
    def assess_overall_performance(self, bottlenecks: Dict) -> str:
        """评估整体性能"""
        critical_count = len(bottlenecks['critical_issues'])
        warning_count = len(bottlenecks['warning_issues'])
        
        if critical_count > 5:
            return "严重 - 系统存在多个关键性能问题，需要立即优化"
        elif critical_count > 2:
            return "中等 - 系统存在一些性能问题，建议尽快优化"
        elif warning_count > 3:
            return "良好 - 系统整体性能可接受，有优化空间"
        else:
            return "优秀 - 系统性能表现良好"
    
    def generate_next_steps(self, bottlenecks: Dict) -> List[str]:
        """生成后续步骤建议"""
        steps = [
            "1. 优先处理高优先级的性能瓶颈",
            "2. 实施RocketMQ异步化方案",
            "3. 建立A/B测试框架验证优化效果",
            "4. 建立持续性能监控机制"
        ]
        
        if len(bottlenecks['critical_issues']) > 0:
            steps.insert(1, "1.1. 立即处理关键性能问题")
        
        return steps
    
    def save_analysis_report(self, report: Dict, filename: str = None):
        """保存分析报告"""
        if not filename:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            filename = f"performance_analysis_{timestamp}.json"
        
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(report, f, indent=2, ensure_ascii=False)
        
        print(f"性能分析报告已保存到: {filename}")
        return filename

def main():
    """主函数"""
    analyzer = PerformanceAnalyzer()
    
    # 加载测试报告
    reports = analyzer.load_test_reports()
    
    if not reports:
        print("未找到性能测试报告文件")
        return
    
    # 生成分析报告
    analysis_report = analyzer.generate_analysis_report(reports)
    
    # 保存分析报告
    report_file = analyzer.save_analysis_report(analysis_report)
    
    # 打印关键发现
    print("\n=== 性能分析关键发现 ===")
    print(f"关键问题数量: {analysis_report['summary']['critical_issues_count']}")
    print(f"警告问题数量: {analysis_report['summary']['warning_issues_count']}")
    print(f"整体评估: {analysis_report['summary']['overall_assessment']}")
    
    print("\n=== 高优先级优化建议 ===")
    for i, rec in enumerate(analysis_report['summary']['top_priority_recommendations'], 1):
        print(f"{i}. {rec['title']} - {rec['description']}")
    
    print(f"\n详细分析报告已保存到: {report_file}")
    print("图表已保存到 charts/ 目录")

if __name__ == "__main__":
    main()