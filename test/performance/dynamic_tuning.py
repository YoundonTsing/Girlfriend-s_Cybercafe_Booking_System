#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
动态调优机制
根据A/B测试结果和实时性能监控数据自动调整RocketMQ配置
实现持续优化策略
"""

import json
import os
import time
import requests
import statistics
from datetime import datetime, timedelta
from typing import Dict, List, Any, Optional
from dataclasses import dataclass
import threading
import logging

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('test/performance/logs/dynamic_tuning.log', encoding='utf-8'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

@dataclass
class PerformanceMetrics:
    """性能指标"""
    avg_response_time: float
    p95_response_time: float
    p99_response_time: float
    success_rate: float
    throughput: float
    error_rate: float
    timestamp: str

@dataclass
class TuningRule:
    """调优规则"""
    name: str
    condition: str  # 触发条件
    action: str     # 调优动作
    parameter: str  # 调整参数
    adjustment: Any # 调整值
    priority: int   # 优先级

class DynamicTuningEngine:
    def __init__(self):
        self.current_config = self.load_current_config()
        self.performance_history = []
        self.tuning_rules = self.initialize_tuning_rules()
        self.monitoring_active = False
        self.tuning_active = False
        
        # 性能阈值
        self.thresholds = {
            'response_time_warning': 500,    # ms
            'response_time_critical': 1000,  # ms
            'success_rate_warning': 95,      # %
            'success_rate_critical': 90,     # %
            'throughput_min': 100,           # requests/min
            'error_rate_max': 5              # %
        }
        
        # 调优历史
        self.tuning_history = []
        
        # 创建必要目录
        os.makedirs('test/performance/logs', exist_ok=True)
        os.makedirs('test/performance/configs', exist_ok=True)
    
    def load_current_config(self) -> Dict[str, Any]:
        """加载当前RocketMQ配置"""
        default_config = {
            'rocketmq': {
                'name_server': 'localhost:9876',
                'producer': {
                    'group': 'ticket_producer_group',
                    'send_msg_timeout': 3000,
                    'compress_msg_body_over_howmuch': 4096,
                    'max_message_size': 4194304,
                    'retry_times_when_send_failed': 2,
                    'retry_times_when_send_async_failed': 2,
                    'retry_another_broker_when_not_store_ok': False
                },
                'consumer': {
                    'consume_thread_min': 5,
                    'consume_thread_max': 20,
                    'consume_message_batch_max_size': 1,
                    'pull_batch_size': 32,
                    'pull_interval': 0,
                    'consume_timeout': 15
                }
            },
            'application': {
                'async_processing': {
                    'enabled': True,
                    'thread_pool_size': 10,
                    'queue_capacity': 1000,
                    'timeout': 30000
                }
            }
        }
        
        # 尝试从配置文件加载
        config_file = 'test/performance/configs/current_config.json'
        if os.path.exists(config_file):
            try:
                with open(config_file, 'r', encoding='utf-8') as f:
                    return json.load(f)
            except Exception as e:
                logger.warning(f"加载配置文件失败，使用默认配置: {e}")
        
        return default_config
    
    def save_current_config(self):
        """保存当前配置"""
        config_file = 'test/performance/configs/current_config.json'
        try:
            with open(config_file, 'w', encoding='utf-8') as f:
                json.dump(self.current_config, f, ensure_ascii=False, indent=2)
            logger.info("配置已保存")
        except Exception as e:
            logger.error(f"保存配置失败: {e}")
    
    def initialize_tuning_rules(self) -> List[TuningRule]:
        """初始化调优规则"""
        rules = [
            # 响应时间优化规则
            TuningRule(
                name="减少发送超时时间",
                condition="avg_response_time > 800",
                action="decrease",
                parameter="rocketmq.producer.send_msg_timeout",
                adjustment=500,
                priority=1
            ),
            TuningRule(
                name="增加消费者线程",
                condition="avg_response_time > 600 and throughput < 200",
                action="increase",
                parameter="rocketmq.consumer.consume_thread_max",
                adjustment=5,
                priority=2
            ),
            TuningRule(
                name="调整批量大小",
                condition="p95_response_time > 1000",
                action="decrease",
                parameter="rocketmq.consumer.consume_message_batch_max_size",
                adjustment=1,
                priority=3
            ),
            
            # 吞吐量优化规则
            TuningRule(
                name="增加拉取批量大小",
                condition="throughput < 150 and success_rate > 95",
                action="increase",
                parameter="rocketmq.consumer.pull_batch_size",
                adjustment=16,
                priority=2
            ),
            TuningRule(
                name="减少拉取间隔",
                condition="throughput < 100",
                action="set",
                parameter="rocketmq.consumer.pull_interval",
                adjustment=0,
                priority=3
            ),
            
            # 成功率优化规则
            TuningRule(
                name="增加重试次数",
                condition="success_rate < 95",
                action="increase",
                parameter="rocketmq.producer.retry_times_when_send_failed",
                adjustment=1,
                priority=1
            ),
            TuningRule(
                name="启用跨Broker重试",
                condition="success_rate < 90",
                action="set",
                parameter="rocketmq.producer.retry_another_broker_when_not_store_ok",
                adjustment=True,
                priority=1
            ),
            
            # 应用层优化规则
            TuningRule(
                name="增加线程池大小",
                condition="avg_response_time > 500 and throughput > 200",
                action="increase",
                parameter="application.async_processing.thread_pool_size",
                adjustment=5,
                priority=2
            ),
            TuningRule(
                name="增加队列容量",
                condition="error_rate > 3",
                action="increase",
                parameter="application.async_processing.queue_capacity",
                adjustment=500,
                priority=3
            )
        ]
        
        return rules
    
    def collect_performance_metrics(self) -> Optional[PerformanceMetrics]:
        """收集性能指标"""
        try:
            # 从应用监控端点获取指标
            response = requests.get('http://localhost:8080/actuator/metrics', timeout=5)
            if response.status_code != 200:
                logger.warning("无法获取应用指标")
                return None
            
            # 模拟性能指标收集（实际应用中应从真实监控系统获取）
            # 这里使用简单的健康检查和响应时间测试
            start_time = time.time()
            health_response = requests.get('http://localhost:8080/actuator/health', timeout=10)
            response_time = (time.time() - start_time) * 1000
            
            # 执行简单的性能测试
            test_results = self.run_quick_performance_test()
            
            metrics = PerformanceMetrics(
                avg_response_time=test_results.get('avg_response_time', response_time),
                p95_response_time=test_results.get('p95_response_time', response_time * 1.5),
                p99_response_time=test_results.get('p99_response_time', response_time * 2),
                success_rate=test_results.get('success_rate', 100 if health_response.status_code == 200 else 0),
                throughput=test_results.get('throughput', 0),
                error_rate=test_results.get('error_rate', 0),
                timestamp=datetime.now().isoformat()
            )
            
            return metrics
            
        except Exception as e:
            logger.error(f"收集性能指标失败: {e}")
            return None
    
    def run_quick_performance_test(self) -> Dict[str, float]:
        """运行快速性能测试"""
        try:
            response_times = []
            success_count = 0
            total_requests = 20
            
            start_time = time.time()
            
            for i in range(total_requests):
                try:
                    req_start = time.time()
                    response = requests.get('http://localhost:8080/actuator/health', timeout=5)
                    req_time = (time.time() - req_start) * 1000
                    
                    response_times.append(req_time)
                    if response.status_code == 200:
                        success_count += 1
                        
                except Exception:
                    response_times.append(5000)  # 超时视为5秒
            
            total_time = time.time() - start_time
            throughput = (total_requests / total_time) * 60  # requests per minute
            
            if response_times:
                return {
                    'avg_response_time': statistics.mean(response_times),
                    'p95_response_time': sorted(response_times)[int(len(response_times) * 0.95)],
                    'p99_response_time': sorted(response_times)[int(len(response_times) * 0.99)],
                    'success_rate': (success_count / total_requests) * 100,
                    'throughput': throughput,
                    'error_rate': ((total_requests - success_count) / total_requests) * 100
                }
            
        except Exception as e:
            logger.error(f"快速性能测试失败: {e}")
        
        return {
            'avg_response_time': 0,
            'p95_response_time': 0,
            'p99_response_time': 0,
            'success_rate': 0,
            'throughput': 0,
            'error_rate': 100
        }
    
    def evaluate_tuning_rules(self, metrics: PerformanceMetrics) -> List[TuningRule]:
        """评估调优规则"""
        applicable_rules = []
        
        # 创建评估上下文
        context = {
            'avg_response_time': metrics.avg_response_time,
            'p95_response_time': metrics.p95_response_time,
            'p99_response_time': metrics.p99_response_time,
            'success_rate': metrics.success_rate,
            'throughput': metrics.throughput,
            'error_rate': metrics.error_rate
        }
        
        for rule in self.tuning_rules:
            try:
                # 评估条件
                if eval(rule.condition, {"__builtins__": {}}, context):
                    applicable_rules.append(rule)
                    logger.info(f"规则触发: {rule.name} - {rule.condition}")
            except Exception as e:
                logger.error(f"评估规则失败 {rule.name}: {e}")
        
        # 按优先级排序
        applicable_rules.sort(key=lambda x: x.priority)
        
        return applicable_rules
    
    def apply_tuning_rule(self, rule: TuningRule) -> bool:
        """应用调优规则"""
        try:
            # 解析参数路径
            param_path = rule.parameter.split('.')
            
            # 获取当前值
            current_value = self.current_config
            for key in param_path[:-1]:
                current_value = current_value.get(key, {})
            
            old_value = current_value.get(param_path[-1])
            
            # 应用调整
            if rule.action == 'increase':
                if isinstance(old_value, (int, float)):
                    new_value = old_value + rule.adjustment
                else:
                    logger.warning(f"无法对非数值类型执行增加操作: {rule.parameter}")
                    return False
            elif rule.action == 'decrease':
                if isinstance(old_value, (int, float)):
                    new_value = max(0, old_value - rule.adjustment)
                else:
                    logger.warning(f"无法对非数值类型执行减少操作: {rule.parameter}")
                    return False
            elif rule.action == 'set':
                new_value = rule.adjustment
            else:
                logger.error(f"未知的调优动作: {rule.action}")
                return False
            
            # 更新配置
            current_value[param_path[-1]] = new_value
            
            # 记录调优历史
            tuning_record = {
                'timestamp': datetime.now().isoformat(),
                'rule_name': rule.name,
                'parameter': rule.parameter,
                'old_value': old_value,
                'new_value': new_value,
                'action': rule.action
            }
            
            self.tuning_history.append(tuning_record)
            
            logger.info(f"应用调优规则: {rule.name}")
            logger.info(f"参数 {rule.parameter}: {old_value} -> {new_value}")
            
            return True
            
        except Exception as e:
            logger.error(f"应用调优规则失败 {rule.name}: {e}")
            return False
    
    def update_application_config(self) -> bool:
        """更新应用配置"""
        try:
            # 生成新的application.yml配置
            config_content = self.generate_application_config()
            
            # 备份当前配置
            backup_file = f"test/performance/configs/application_backup_{int(time.time())}.yml"
            original_file = "ticket-order/src/main/resources/application.yml"
            
            if os.path.exists(original_file):
                import shutil
                shutil.copy2(original_file, backup_file)
                logger.info(f"配置已备份到: {backup_file}")
            
            # 写入新配置
            with open(original_file, 'w', encoding='utf-8') as f:
                f.write(config_content)
            
            logger.info("应用配置已更新")
            return True
            
        except Exception as e:
            logger.error(f"更新应用配置失败: {e}")
            return False
    
    def generate_application_config(self) -> str:
        """生成application.yml配置内容"""
        rocketmq_config = self.current_config['rocketmq']
        app_config = self.current_config['application']
        
        config_template = f"""
server:
  port: 8080

spring:
  application:
    name: ticket-order
  cloud:
    nacos:
      config:
        import:
          - dataId: ticket-order.yml
            group: DEFAULT_GROUP
            refresh: true
      discovery:
        server-addr: localhost:8848
        namespace: public
        group: DEFAULT_GROUP
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/ticket_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

knife4j:
  enable: true
  openapi:
    title: 票务系统订单服务API
    version: 1.0
    group:
      default:
        group-name: 订单管理
        api-rule: package
        api-rule-resources:
          - com.ticketsystem.order.controller

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true

# RocketMQ配置 - 动态调优
rocketmq:
  name-server: {rocketmq_config['name_server']}
  producer:
    group: {rocketmq_config['producer']['group']}
    send-msg-timeout: {rocketmq_config['producer']['send_msg_timeout']}
    compress-msg-body-over-howmuch: {rocketmq_config['producer']['compress_msg_body_over_howmuch']}
    max-message-size: {rocketmq_config['producer']['max_message_size']}
    retry-times-when-send-failed: {rocketmq_config['producer']['retry_times_when_send_failed']}
    retry-times-when-send-async-failed: {rocketmq_config['producer']['retry_times_when_send_async_failed']}
    retry-another-broker-when-not-store-ok: {str(rocketmq_config['producer']['retry_another_broker_when_not_store_ok']).lower()}
  consumer:
    consume-thread-min: {rocketmq_config['consumer']['consume_thread_min']}
    consume-thread-max: {rocketmq_config['consumer']['consume_thread_max']}
    consume-message-batch-max-size: {rocketmq_config['consumer']['consume_message_batch_max_size']}
    pull-batch-size: {rocketmq_config['consumer']['pull_batch_size']}
    pull-interval: {rocketmq_config['consumer']['pull_interval']}
    consume-timeout: {rocketmq_config['consumer']['consume_timeout']}

# 应用异步处理配置
async-processing:
  enabled: {str(app_config['async_processing']['enabled']).lower()}
  thread-pool-size: {app_config['async_processing']['thread_pool_size']}
  queue-capacity: {app_config['async_processing']['queue_capacity']}
  timeout: {app_config['async_processing']['timeout']}

# 定时任务配置
scheduling:
  enabled: true
  pool:
    size: 5
  thread-name-prefix: "ticket-task-"

# 日志配置
logging:
  level:
    com.ticketsystem.order: INFO
    org.apache.rocketmq: WARN
  pattern:
    console: "%d{{yyyy-MM-dd HH:mm:ss}} [%thread] %-5level %logger{{36}} - %msg%n"
    file: "%d{{yyyy-MM-dd HH:mm:ss}} [%thread] %-5level %logger{{36}} - %msg%n"
  file:
    name: logs/ticket-order.log
    max-size: 100MB
    max-history: 30
"""
        
        return config_template
    
    def start_monitoring(self, interval_seconds: int = 60):
        """启动性能监控"""
        self.monitoring_active = True
        
        def monitoring_loop():
            logger.info("开始性能监控...")
            
            while self.monitoring_active:
                try:
                    # 收集性能指标
                    metrics = self.collect_performance_metrics()
                    if metrics:
                        self.performance_history.append(metrics)
                        
                        # 保持历史记录在合理范围内
                        if len(self.performance_history) > 100:
                            self.performance_history = self.performance_history[-100:]
                        
                        logger.info(f"性能指标: 响应时间={metrics.avg_response_time:.2f}ms, "
                                  f"成功率={metrics.success_rate:.2f}%, "
                                  f"吞吐量={metrics.throughput:.2f}req/min")
                        
                        # 如果启用了调优，执行调优逻辑
                        if self.tuning_active:
                            self.execute_tuning(metrics)
                    
                    time.sleep(interval_seconds)
                    
                except Exception as e:
                    logger.error(f"监控循环异常: {e}")
                    time.sleep(interval_seconds)
        
        monitoring_thread = threading.Thread(target=monitoring_loop, daemon=True)
        monitoring_thread.start()
        
        logger.info(f"性能监控已启动，监控间隔: {interval_seconds}秒")
    
    def execute_tuning(self, metrics: PerformanceMetrics):
        """执行调优逻辑"""
        try:
            # 评估是否需要调优
            if not self.should_tune(metrics):
                return
            
            # 获取适用的调优规则
            applicable_rules = self.evaluate_tuning_rules(metrics)
            
            if not applicable_rules:
                return
            
            logger.info(f"发现 {len(applicable_rules)} 个适用的调优规则")
            
            # 应用调优规则（限制每次调优的规则数量）
            applied_count = 0
            max_rules_per_cycle = 3
            
            for rule in applicable_rules[:max_rules_per_cycle]:
                if self.apply_tuning_rule(rule):
                    applied_count += 1
            
            if applied_count > 0:
                # 保存配置
                self.save_current_config()
                
                # 更新应用配置
                if self.update_application_config():
                    logger.info(f"已应用 {applied_count} 个调优规则，配置已更新")
                    logger.info("建议重启应用以使配置生效")
                else:
                    logger.error("更新应用配置失败")
            
        except Exception as e:
            logger.error(f"执行调优失败: {e}")
    
    def should_tune(self, metrics: PerformanceMetrics) -> bool:
        """判断是否需要调优"""
        # 检查是否超过阈值
        if (metrics.avg_response_time > self.thresholds['response_time_warning'] or
            metrics.success_rate < self.thresholds['success_rate_warning'] or
            metrics.throughput < self.thresholds['throughput_min'] or
            metrics.error_rate > self.thresholds['error_rate_max']):
            return True
        
        # 检查性能趋势（如果有足够的历史数据）
        if len(self.performance_history) >= 5:
            recent_metrics = self.performance_history[-5:]
            
            # 计算趋势
            response_times = [m.avg_response_time for m in recent_metrics]
            success_rates = [m.success_rate for m in recent_metrics]
            
            # 如果响应时间持续上升或成功率持续下降，触发调优
            if (response_times[-1] > response_times[0] * 1.2 or
                success_rates[-1] < success_rates[0] * 0.95):
                logger.info("检测到性能趋势恶化，触发调优")
                return True
        
        return False
    
    def stop_monitoring(self):
        """停止监控"""
        self.monitoring_active = False
        logger.info("性能监控已停止")
    
    def enable_tuning(self):
        """启用自动调优"""
        self.tuning_active = True
        logger.info("自动调优已启用")
    
    def disable_tuning(self):
        """禁用自动调优"""
        self.tuning_active = False
        logger.info("自动调优已禁用")
    
    def get_tuning_report(self) -> Dict[str, Any]:
        """获取调优报告"""
        return {
            'current_config': self.current_config,
            'performance_history': self.performance_history[-10:],  # 最近10次记录
            'tuning_history': self.tuning_history,
            'thresholds': self.thresholds,
            'monitoring_active': self.monitoring_active,
            'tuning_active': self.tuning_active
        }
    
    def save_tuning_report(self):
        """保存调优报告"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        report_file = f"test/performance/reports/tuning_report_{timestamp}.json"
        
        try:
            os.makedirs(os.path.dirname(report_file), exist_ok=True)
            
            report = self.get_tuning_report()
            with open(report_file, 'w', encoding='utf-8') as f:
                json.dump(report, f, ensure_ascii=False, indent=2)
            
            logger.info(f"调优报告已保存: {report_file}")
            return report_file
            
        except Exception as e:
            logger.error(f"保存调优报告失败: {e}")
            return None

def main():
    """主函数"""
    tuning_engine = DynamicTuningEngine()
    
    print("动态调优引擎")
    print("=" * 40)
    print("1. 启动监控")
    print("2. 启用自动调优")
    print("3. 查看当前状态")
    print("4. 生成调优报告")
    print("5. 退出")
    
    while True:
        try:
            choice = input("\n请选择操作 (1-5): ").strip()
            
            if choice == '1':
                interval = int(input("监控间隔(秒，默认60): ") or "60")
                tuning_engine.start_monitoring(interval)
                print("监控已启动")
                
            elif choice == '2':
                tuning_engine.enable_tuning()
                print("自动调优已启用")
                
            elif choice == '3':
                report = tuning_engine.get_tuning_report()
                print(f"监控状态: {'运行中' if report['monitoring_active'] else '已停止'}")
                print(f"调优状态: {'启用' if report['tuning_active'] else '禁用'}")
                print(f"性能记录数: {len(report['performance_history'])}")
                print(f"调优记录数: {len(report['tuning_history'])}")
                
            elif choice == '4':
                report_file = tuning_engine.save_tuning_report()
                if report_file:
                    print(f"调优报告已生成: {report_file}")
                    
            elif choice == '5':
                tuning_engine.stop_monitoring()
                print("再见！")
                break
                
            else:
                print("无效选择，请重试")
                
        except KeyboardInterrupt:
            tuning_engine.stop_monitoring()
            print("\n程序已退出")
            break
        except Exception as e:
            print(f"操作失败: {e}")

if __name__ == "__main__":
    main()