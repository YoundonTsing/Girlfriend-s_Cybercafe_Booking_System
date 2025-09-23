#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
订单创建功能测试启动脚本
提供命令行界面来运行不同类型的测试

使用方法:
python run_tests.py --all                    # 运行所有测试
python run_tests.py --auth                   # 只运行认证测试
python run_tests.py --normal                 # 只运行正常流程测试
python run_tests.py --exception              # 只运行异常场景测试
python run_tests.py --concurrent             # 只运行并发测试
python run_tests.py --config config.json    # 使用指定配置文件
"""

import argparse
import json
import os
import sys
from datetime import datetime
from order_creation_comprehensive_test import OrderCreationComprehensiveTest

class TestRunner:
    """测试运行器"""
    
    def __init__(self, config_file: str = "test_config.json"):
        self.config_file = config_file
        self.config = self.load_config()
        self.tester = None
        
    def load_config(self) -> dict:
        """加载测试配置"""
        try:
            config_path = os.path.join(os.path.dirname(__file__), self.config_file)
            with open(config_path, 'r', encoding='utf-8') as f:
                return json.load(f)
        except FileNotFoundError:
            print(f"❌ 配置文件 {self.config_file} 不存在")
            sys.exit(1)
        except json.JSONDecodeError as e:
            print(f"❌ 配置文件格式错误: {str(e)}")
            sys.exit(1)
    
    def initialize_tester(self):
        """初始化测试器"""
        base_url = self.config["server_config"]["base_url"]
        self.tester = OrderCreationComprehensiveTest(base_url)
        
        # 更新测试配置
        self.tester.test_config.update({
            "admin_username": self.config["test_credentials"]["admin_user"]["username"],
            "admin_password": self.config["test_credentials"]["admin_user"]["password"],
            "request_timeout": self.config["server_config"]["timeout"],
            "concurrent_users": self.config["test_parameters"]["concurrent_config"]["concurrent_users"],
            "concurrent_requests": self.config["test_parameters"]["concurrent_config"]["concurrent_requests"],
            "retry_times": self.config["test_parameters"]["retry_config"]["max_retries"]
        })
        
        # 更新测试数据
        self.tester.test_data.update(self.config["test_data"]["valid_data"])
        
    def run_auth_tests(self):
        """运行认证测试"""
        print("🔐 开始运行JWT认证测试...")
        
        success = True
        success &= self.tester.test_01_jwt_authentication()
        success &= self.tester.test_02_jwt_validation()
        
        return success
    
    def run_normal_flow_tests(self):
        """运行正常流程测试"""
        print("✅ 开始运行正常流程测试...")
        
        # 先运行JWT认证获取token
        if not self.tester.jwt_token:
            print("🔐 先进行JWT认证...")
            auth_success = self.tester.test_01_jwt_authentication()
            if not auth_success:
                print("❌ JWT认证失败，无法继续正常流程测试")
                return False
        
        success = True
        success &= self.tester.test_03_order_creation_normal_flow()
        success &= self.tester.test_04_order_creation_steps_validation()
        
        return success
    
    def run_exception_tests(self):
        """运行异常场景测试"""
        print("⚠️ 开始运行异常场景测试...")
        
        # 先运行JWT认证获取token
        if not self.tester.jwt_token:
            print("🔐 先进行JWT认证...")
            auth_success = self.tester.test_01_jwt_authentication()
            if not auth_success:
                print("❌ JWT认证失败，无法继续异常场景测试")
                return False
        
        return self.tester.test_05_exception_scenarios()
    
    def run_concurrent_tests(self):
        """运行并发测试"""
        print("🚀 开始运行并发测试...")
        
        # 先运行JWT认证获取token
        if not self.tester.jwt_token:
            print("🔐 先进行JWT认证...")
            auth_success = self.tester.test_01_jwt_authentication()
            if not auth_success:
                print("❌ JWT认证失败，无法继续并发测试")
                return False
        
        return self.tester.test_06_concurrent_order_creation()
    
    def run_all_tests(self):
        """运行所有测试"""
        print("🎯 开始运行完整测试套件...")
        
        return self.tester.run_all_tests()
    
    def print_banner(self):
        """打印测试横幅"""
        print("=" * 80)
        print("🎫 订单创建功能测试套件")
        print("=" * 80)
        print(f"📅 测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"🌐 服务地址: {self.config['server_config']['base_url']}")
        print(f"👤 测试用户: {self.config['test_credentials']['admin_user']['username']}")
        print("=" * 80)
    
    def print_summary(self, results: dict):
        """打印测试总结"""
        print("\n" + "=" * 80)
        print("📊 测试结果总结")
        print("=" * 80)
        
        if isinstance(results, dict) and 'test_summary' in results:
            summary = results['test_summary']
            print(f"📈 总测试数: {summary['total_tests']}")
            print(f"✅ 通过测试: {summary['passed_tests']}")
            print(f"❌ 失败测试: {summary['failed_tests']}")
            print(f"📊 成功率: {summary['success_rate']}%")
            print(f"⏱️ 总耗时: {summary['test_duration']:.3f}秒")
            print(f"⚡ 平均响应时间: {summary['avg_execution_time']:.3f}秒")
            
            # 判断测试是否整体通过
            if summary['success_rate'] >= 80:
                print("🎉 测试整体通过!")
            else:
                print("💥 测试整体失败!")
        else:
            print("⚠️ 无法获取测试结果摘要")
        
        print("=" * 80)


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description="订单创建功能测试启动器")
    
    # 测试类型选项
    test_group = parser.add_mutually_exclusive_group(required=True)
    test_group.add_argument("--all", action="store_true", help="运行所有测试")
    test_group.add_argument("--auth", action="store_true", help="只运行JWT认证测试")
    test_group.add_argument("--normal", action="store_true", help="只运行正常流程测试")
    test_group.add_argument("--exception", action="store_true", help="只运行异常场景测试")
    test_group.add_argument("--concurrent", action="store_true", help="只运行并发测试")
    
    # 配置选项
    parser.add_argument("--config", default="test_config.json", help="指定配置文件路径")
    parser.add_argument("--verbose", "-v", action="store_true", help="详细输出")
    
    args = parser.parse_args()
    
    try:
        # 初始化测试运行器
        runner = TestRunner(args.config)
        runner.print_banner()
        runner.initialize_tester()
        
        # 根据参数运行相应测试
        results = None
        
        if args.all:
            results = runner.run_all_tests()
        elif args.auth:
            if not runner.run_auth_tests():
                print("❌ JWT认证测试失败")
                sys.exit(1)
            print("✅ JWT认证测试通过")
        elif args.normal:
            if not runner.run_normal_flow_tests():
                print("❌ 正常流程测试失败")
                sys.exit(1)
            print("✅ 正常流程测试通过")
        elif args.exception:
            if not runner.run_exception_tests():
                print("❌ 异常场景测试失败")
                sys.exit(1)
            print("✅ 异常场景测试通过")
        elif args.concurrent:
            if not runner.run_concurrent_tests():
                print("❌ 并发测试失败")
                sys.exit(1)
            print("✅ 并发测试通过")
        
        # 打印测试总结
        if results:
            runner.print_summary(results)
            
            # 根据成功率决定退出码
            if results.get('test_summary', {}).get('success_rate', 0) >= 80:
                sys.exit(0)
            else:
                sys.exit(1)
        
    except KeyboardInterrupt:
        print("\n⚠️ 测试被用户中断")
        sys.exit(130)
    except Exception as e:
        print(f"💥 测试运行异常: {str(e)}")
        if args.verbose:
            import traceback
            traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()