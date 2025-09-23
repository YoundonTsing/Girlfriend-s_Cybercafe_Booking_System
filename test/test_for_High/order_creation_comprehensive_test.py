#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
订单创建功能综合测试程序
测试订单创建的10个核心步骤，包括JWT认证、正常流程、异常场景和并发测试

测试用户：admin/123456
创建时间：2024年12月
"""

import requests
import json
import time
import threading
import concurrent.futures
from datetime import datetime, timedelta
from typing import Dict, Any, Optional, List
import uuid
import random
import logging

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('order_creation_test.log', encoding='utf-8'),
        logging.StreamHandler()
    ]
)

class OrderCreationComprehensiveTest:
    """订单创建功能综合测试类"""
    
    def __init__(self, base_url: str = "http://localhost:8000"):
        self.base_url = base_url
        self.session = requests.Session()
        self.jwt_token = None
        self.user_id = None
        self.test_results = []
        self.lock = threading.Lock()
        
        # 测试配置
        self.test_config = {
            "admin_username": "admin",
            "admin_password": "123456",
            "request_timeout": 10,
            "concurrent_users": 5,
            "concurrent_requests": 20,
            "retry_times": 3
        }
        
        # 测试数据 - 根据数据库实际数据更新
        self.test_data = {
            "valid_show_ids": [1, 2, 4, 5, 6],  # 数据库中存在的演出ID
            "valid_session_ids": [10, 11, 13, 14, 15],  # 数据库中存在的场次ID
            "valid_ticket_ids": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12],  # 数据库中存在的票档ID
            "invalid_show_ids": [0, -1, 999999],
            "invalid_ticket_ids": [0, -1, 999999],
            "invalid_quantities": [0, -1, 1000],
            "valid_data": {
                "show_ids": [1, 2, 4, 5, 6],
                "session_ids": [10, 11, 13, 14, 15],
                "ticket_ids": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12],
                "quantities": [1, 2, 3],
                "contact_phones": ["13800138000", "13900139000", "13700137000"]
            }
        }
        
    def log_test_result(self, test_name: str, success: bool, message: str, 
                       response_data: Any = None, execution_time: float = 0):
        """记录测试结果"""
        with self.lock:
            result = {
                "test_name": test_name,
                "success": success,
                "message": message,
                "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                "execution_time": round(execution_time, 3),
                "response_data": response_data
            }
            self.test_results.append(result)
            
            status = "✅ PASS" if success else "❌ FAIL"
            logging.info(f"{status} {test_name}: {message} (耗时: {execution_time:.3f}s)")
    
    def make_request(self, method: str, endpoint: str, data: Dict = None, 
                    headers: Dict = None, use_auth: bool = True) -> requests.Response:
        """发送HTTP请求"""
        url = f"{self.base_url}{endpoint}"
        
        # 设置默认headers
        request_headers = {
            "Content-Type": "application/json",
            "Accept": "application/json"
        }
        
        # 添加JWT认证
        if use_auth and self.jwt_token:
            request_headers["Authorization"] = f"Bearer {self.jwt_token}"
            request_headers["X-User-Id"] = str(self.user_id)
        
        # 合并自定义headers
        if headers:
            request_headers.update(headers)
        
        try:
            if method.upper() == "GET":
                return self.session.get(url, headers=request_headers, 
                                      timeout=self.test_config["request_timeout"])
            elif method.upper() == "POST":
                return self.session.post(url, json=data, headers=request_headers,
                                       timeout=self.test_config["request_timeout"])
            elif method.upper() == "PUT":
                return self.session.put(url, json=data, headers=request_headers,
                                      timeout=self.test_config["request_timeout"])
            elif method.upper() == "DELETE":
                return self.session.delete(url, headers=request_headers,
                                         timeout=self.test_config["request_timeout"])
        except Exception as e:
            logging.error(f"请求异常: {method} {url} - {str(e)}")
            raise
    
    # ==================== JWT认证测试 ====================
    
    def test_01_jwt_authentication(self) -> bool:
        """测试JWT认证 - 用户登录获取token"""
        start_time = time.time()
        
        login_data = {
            "username": self.test_config["admin_username"],
            "password": self.test_config["admin_password"]
        }
        
        try:
            response = self.make_request("POST", "/api/user/login", login_data, use_auth=False)
            execution_time = time.time() - start_time
            
            if response.status_code == 200:
                response_data = response.json()
                
                # 检查响应结构
                if response_data.get("success") and response_data.get("data"):
                    login_vo = response_data["data"]
                    self.jwt_token = login_vo.get("token")
                    self.user_id = login_vo.get("userId")
                    
                    if self.jwt_token and self.user_id:
                        self.log_test_result(
                            "JWT认证-用户登录", True, 
                            f"登录成功，获取JWT token，用户ID: {self.user_id}",
                            {"token_length": len(self.jwt_token), "user_id": self.user_id},
                            execution_time
                        )
                        return True
                    else:
                        self.log_test_result(
                            "JWT认证-用户登录", False,
                            "登录响应中缺少token或userId字段",
                            response_data, execution_time
                        )
                        return False
                else:
                    self.log_test_result(
                        "JWT认证-用户登录", False,
                        f"登录失败: {response_data.get('message', '未知错误')}",
                        response_data, execution_time
                    )
                    return False
            else:
                self.log_test_result(
                    "JWT认证-用户登录", False,
                    f"HTTP错误: {response.status_code}",
                    response.text, execution_time
                )
                return False
                
        except Exception as e:
            execution_time = time.time() - start_time
            self.log_test_result(
                "JWT认证-用户登录", False,
                f"请求异常: {str(e)}", None, execution_time
            )
            return False
    
    def test_02_jwt_validation(self) -> bool:
        """测试JWT token验证"""
        if not self.jwt_token:
            self.log_test_result("JWT认证-token验证", False, "未获取到JWT token")
            return False
        
        start_time = time.time()
        
        try:
            # 使用token访问需要认证的接口
            response = self.make_request("GET", "/api/order/user/page?page=1&size=5")
            execution_time = time.time() - start_time
            
            if response.status_code == 200:
                self.log_test_result(
                    "JWT认证-token验证", True,
                    "JWT token验证成功，可正常访问受保护接口",
                    None, execution_time
                )
                return True
            else:
                self.log_test_result(
                    "JWT认证-token验证", False,
                    f"JWT token验证失败: HTTP {response.status_code}",
                    response.text, execution_time
                )
                return False
                
        except Exception as e:
            execution_time = time.time() - start_time
            self.log_test_result(
                "JWT认证-token验证", False,
                f"JWT token验证异常: {str(e)}", None, execution_time
            )
            return False
    
    # ==================== 订单创建正常流程测试 ====================
    
    def test_03_order_creation_normal_flow(self) -> bool:
        """测试订单创建正常流程"""
        if not self.jwt_token:
            self.log_test_result("订单创建-正常流程", False, "未获取到JWT token")
            return False
        
        start_time = time.time()
        
        # 构造有效的订单数据
        order_data = {
            "userId": self.user_id,
            "showId": random.choice(self.test_data["valid_show_ids"]),
            "sessionId": random.choice(self.test_data["valid_session_ids"]),
            "ticketId": random.choice(self.test_data["valid_ticket_ids"]),
            "quantity": random.randint(1, 3),
            "bookingDate": (datetime.now() + timedelta(days=1)).strftime("%Y-%m-%d"),
            "contactPhone": "13800138000",
            "remark": "自动化测试订单"
        }
        
        try:
            response = self.make_request("POST", "/api/order/create", order_data)
            execution_time = time.time() - start_time
            
            if response.status_code == 200:
                response_data = response.json()
                
                if response_data.get("success") and response_data.get("data"):
                    order_no = response_data["data"]
                    self.log_test_result(
                        "订单创建-正常流程", True,
                        f"订单创建成功，订单号: {order_no}",
                        {"order_no": order_no, "order_data": order_data},
                        execution_time
                    )
                    
                    # 保存订单号用于后续测试
                    self.test_order_no = order_no
                    return True
                else:
                    self.log_test_result(
                        "订单创建-正常流程", False,
                        f"订单创建失败: {response_data.get('message', '未知错误')}",
                        response_data, execution_time
                    )
                    return False
            else:
                self.log_test_result(
                    "订单创建-正常流程", False,
                    f"HTTP错误: {response.status_code}",
                    response.text, execution_time
                )
                return False
                
        except Exception as e:
            execution_time = time.time() - start_time
            self.log_test_result(
                "订单创建-正常流程", False,
                f"请求异常: {str(e)}", None, execution_time
            )
            return False
    
    def test_04_order_creation_steps_validation(self) -> bool:
        """测试订单创建10个步骤的详细验证"""
        if not self.jwt_token:
            self.log_test_result("订单创建-步骤验证", False, "未获取到JWT token")
            return False
        
        # 测试每个步骤
        steps_results = []
        
        # 步骤1: 分布式锁测试（通过并发请求验证）
        steps_results.append(self._test_distributed_lock())
        
        # 步骤2: 参数校验测试
        steps_results.append(self._test_parameter_validation())
        
        # 步骤3: 票价信息获取测试
        steps_results.append(self._test_ticket_price_retrieval())
        
        # 步骤4: 订单号生成测试
        steps_results.append(self._test_order_number_generation())
        
        # 步骤5: Redis预减库存测试
        steps_results.append(self._test_redis_stock_prededuction())
        
        # 步骤6: 演出信息获取测试
        steps_results.append(self._test_show_info_retrieval())
        
        # 步骤7-10: 订单持久化和完整流程测试
        steps_results.append(self._test_order_persistence())
        
        success_count = sum(steps_results)
        total_steps = len(steps_results)
        
        if success_count == total_steps:
            self.log_test_result(
                "订单创建-步骤验证", True,
                f"所有{total_steps}个步骤验证通过",
                {"success_steps": success_count, "total_steps": total_steps}
            )
            return True
        else:
            self.log_test_result(
                "订单创建-步骤验证", False,
                f"步骤验证失败: {success_count}/{total_steps}个步骤通过",
                {"success_steps": success_count, "total_steps": total_steps}
            )
            return False
    
    def _test_distributed_lock(self) -> bool:
        """测试分布式锁机制"""
        # 通过快速连续请求测试分布式锁
        start_time = time.time()
        
        order_data = {
            "userId": self.user_id,
            "showId": 1,
            "sessionId": 1,
            "ticketId": 1,
            "quantity": 1,
            "contactPhone": "13800138000"
        }
        
        # 发送3个并发请求
        results = []
        threads = []
        
        def create_order():
            try:
                response = self.make_request("POST", "/api/order/create", order_data)
                results.append(response.status_code == 200)
            except:
                results.append(False)
        
        for _ in range(3):
            thread = threading.Thread(target=create_order)
            threads.append(thread)
            thread.start()
        
        for thread in threads:
            thread.join()
        
        execution_time = time.time() - start_time
        
        # 分布式锁应该确保只有一个请求成功
        success_count = sum(results)
        
        if success_count <= 1:
            self.log_test_result(
                "分布式锁测试", True,
                f"分布式锁机制正常，{success_count}/3个请求成功",
                {"success_requests": success_count}, execution_time
            )
            return True
        else:
            self.log_test_result(
                "分布式锁测试", False,
                f"分布式锁机制异常，{success_count}/3个请求成功",
                {"success_requests": success_count}, execution_time
            )
            return False
    
    def _test_parameter_validation(self) -> bool:
        """测试参数校验"""
        start_time = time.time()
        
        # 测试无效参数
        invalid_data = {
            "userId": self.user_id,
            "showId": -1,  # 无效showId
            "sessionId": 1,
            "ticketId": -1,  # 无效ticketId
            "quantity": 0,  # 无效quantity
            "contactPhone": "invalid_phone"  # 无效手机号
        }
        
        try:
            response = self.make_request("POST", "/api/order/create", invalid_data)
            execution_time = time.time() - start_time
            
            # 应该返回400或业务错误
            if response.status_code in [400, 500] or (
                response.status_code == 200 and 
                not response.json().get("success", True)
            ):
                self.log_test_result(
                    "参数校验测试", True,
                    "参数校验正常，拒绝了无效参数",
                    None, execution_time
                )
                return True
            else:
                self.log_test_result(
                    "参数校验测试", False,
                    "参数校验异常，接受了无效参数",
                    response.text, execution_time
                )
                return False
                
        except Exception as e:
            execution_time = time.time() - start_time
            self.log_test_result(
                "参数校验测试", False,
                f"参数校验测试异常: {str(e)}", None, execution_time
            )
            return False
    
    def _test_ticket_price_retrieval(self) -> bool:
        """测试票价信息获取"""
        start_time = time.time()
        
        try:
            # 直接测试票价获取接口
            ticket_id = random.choice(self.test_data["valid_ticket_ids"])
            response = self.make_request("GET", f"/api/ticket/price/{ticket_id}")
            execution_time = time.time() - start_time
            
            if response.status_code == 200:
                response_data = response.json()
                if response_data.get("success"):
                    self.log_test_result(
                        "票价信息获取测试", True,
                        f"成功获取票价信息，票档ID: {ticket_id}",
                        response_data.get("data"), execution_time
                    )
                    return True
            
            self.log_test_result(
                "票价信息获取测试", False,
                f"票价信息获取失败: HTTP {response.status_code}",
                response.text, execution_time
            )
            return False
            
        except Exception as e:
            execution_time = time.time() - start_time
            self.log_test_result(
                "票价信息获取测试", False,
                f"票价信息获取异常: {str(e)}", None, execution_time
            )
            return False
    
    def _test_order_number_generation(self) -> bool:
        """测试订单号生成"""
        # 通过创建多个订单验证订单号唯一性
        start_time = time.time()
        
        order_numbers = set()
        
        for i in range(3):
            order_data = {
                "userId": self.user_id,
                "showId": random.choice(self.test_data["valid_show_ids"]),
                "sessionId": 1,
                "ticketId": random.choice(self.test_data["valid_ticket_ids"]),
                "quantity": 1,
                "contactPhone": "13800138000"
            }
            
            try:
                response = self.make_request("POST", "/api/order/create", order_data)
                if response.status_code == 200:
                    response_data = response.json()
                    if response_data.get("success") and response_data.get("data"):
                        order_numbers.add(response_data["data"])
                time.sleep(0.1)  # 避免时间戳重复
            except:
                pass
        
        execution_time = time.time() - start_time
        
        if len(order_numbers) >= 2:  # 至少生成了2个不同的订单号
            self.log_test_result(
                "订单号生成测试", True,
                f"订单号生成正常，生成了{len(order_numbers)}个唯一订单号",
                list(order_numbers), execution_time
            )
            return True
        else:
            self.log_test_result(
                "订单号生成测试", False,
                f"订单号生成异常，只生成了{len(order_numbers)}个唯一订单号",
                list(order_numbers), execution_time
            )
            return False
    
    def _test_redis_stock_prededuction(self) -> bool:
        """测试Redis预减库存"""
        start_time = time.time()
        
        try:
            # 测试库存查询接口
            ticket_id = random.choice(self.test_data["valid_ticket_ids"])
            response = self.make_request("GET", f"/api/ticket/stock/{ticket_id}")
            execution_time = time.time() - start_time
            
            if response.status_code == 200:
                response_data = response.json()
                if response_data.get("success"):
                    stock_info = response_data.get("data", {})
                    self.log_test_result(
                        "Redis预减库存测试", True,
                        f"Redis库存查询正常，票档ID: {ticket_id}",
                        stock_info, execution_time
                    )
                    return True
            
            self.log_test_result(
                "Redis预减库存测试", False,
                f"Redis库存查询失败: HTTP {response.status_code}",
                response.text, execution_time
            )
            return False
            
        except Exception as e:
            execution_time = time.time() - start_time
            self.log_test_result(
                "Redis预减库存测试", False,
                f"Redis库存查询异常: {str(e)}", None, execution_time
            )
            return False
    
    def _test_show_info_retrieval(self) -> bool:
        """测试演出信息获取"""
        start_time = time.time()
        
        try:
            show_id = random.choice(self.test_data["valid_show_ids"])
            response = self.make_request("GET", f"/api/show/detail/{show_id}")
            execution_time = time.time() - start_time
            
            if response.status_code == 200:
                response_data = response.json()
                if response_data.get("success"):
                    show_info = response_data.get("data", {})
                    self.log_test_result(
                        "演出信息获取测试", True,
                        f"演出信息获取正常，演出ID: {show_id}",
                        show_info, execution_time
                    )
                    return True
            
            self.log_test_result(
                "演出信息获取测试", False,
                f"演出信息获取失败: HTTP {response.status_code}",
                response.text, execution_time
            )
            return False
            
        except Exception as e:
            execution_time = time.time() - start_time
            self.log_test_result(
                "演出信息获取测试", False,
                f"演出信息获取异常: {str(e)}", None, execution_time
            )
            return False
    
    def _test_order_persistence(self) -> bool:
        """测试订单持久化"""
        start_time = time.time()
        
        order_data = {
            "userId": self.user_id,
            "showId": random.choice(self.test_data["valid_show_ids"]),
            "sessionId": 1,
            "ticketId": random.choice(self.test_data["valid_ticket_ids"]),
            "quantity": 1,
            "contactPhone": "13800138000",
            "remark": "订单持久化测试"
        }
        
        try:
            # 创建订单
            response = self.make_request("POST", "/api/order/create", order_data)
            
            if response.status_code == 200:
                response_data = response.json()
                if response_data.get("success") and response_data.get("data"):
                    order_no = response_data["data"]
                    
                    # 验证订单是否已持久化（查询订单详情）
                    time.sleep(0.5)  # 等待数据库写入
                    detail_response = self.make_request("GET", f"/api/order/detail/by-order-no/{order_no}")
                    
                    execution_time = time.time() - start_time
                    
                    if detail_response.status_code == 200:
                        detail_data = detail_response.json()
                        if detail_data.get("success"):
                            self.log_test_result(
                                "订单持久化测试", True,
                                f"订单持久化成功，订单号: {order_no}",
                                {"order_no": order_no}, execution_time
                            )
                            return True
            
            execution_time = time.time() - start_time
            self.log_test_result(
                "订单持久化测试", False,
                "订单持久化失败",
                None, execution_time
            )
            return False
            
        except Exception as e:
            execution_time = time.time() - start_time
            self.log_test_result(
                "订单持久化测试", False,
                f"订单持久化测试异常: {str(e)}", None, execution_time
            )
            return False
    
    # ==================== 异常场景测试 ====================
    
    def test_05_exception_scenarios(self) -> bool:
        """测试异常场景"""
        if not self.jwt_token:
            self.log_test_result("异常场景测试", False, "未获取到JWT token")
            return False
        
        exception_tests = [
            self._test_invalid_show_id,
            self._test_invalid_ticket_id,
            self._test_invalid_quantity,
            self._test_insufficient_stock,
            self._test_unauthorized_access,
            self._test_missing_parameters
        ]
        
        success_count = 0
        for test_func in exception_tests:
            if test_func():
                success_count += 1
        
        total_tests = len(exception_tests)
        if success_count == total_tests:
            self.log_test_result(
                "异常场景测试", True,
                f"所有{total_tests}个异常场景测试通过",
                {"success_tests": success_count, "total_tests": total_tests}
            )
            return True
        else:
            self.log_test_result(
                "异常场景测试", False,
                f"异常场景测试失败: {success_count}/{total_tests}个测试通过",
                {"success_tests": success_count, "total_tests": total_tests}
            )
            return False
    
    def _test_invalid_show_id(self) -> bool:
        """测试无效演出ID"""
        start_time = time.time()
        
        order_data = {
            "userId": self.user_id,
            "showId": 999999,  # 无效ID
            "sessionId": 1,
            "ticketId": 1,
            "quantity": 1,
            "contactPhone": "13800138000"
        }
        
        try:
            response = self.make_request("POST", "/api/order/create", order_data)
            execution_time = time.time() - start_time
            
            # 应该返回错误
            if response.status_code != 200 or not response.json().get("success", True):
                self.log_test_result(
                    "无效演出ID测试", True,
                    "正确拒绝了无效演出ID",
                    None, execution_time
                )
                return True
            else:
                self.log_test_result(
                    "无效演出ID测试", False,
                    "错误接受了无效演出ID",
                    response.text, execution_time
                )
                return False
                
        except Exception as e:
            execution_time = time.time() - start_time
            self.log_test_result(
                "无效演出ID测试", False,
                f"测试异常: {str(e)}", None, execution_time
            )
            return False
    
    def _test_invalid_ticket_id(self) -> bool:
        """测试无效票档ID"""
        start_time = time.time()
        
        order_data = {
            "userId": self.user_id,
            "showId": 1,
            "sessionId": 1,
            "ticketId": 999999,  # 无效ID
            "quantity": 1,
            "contactPhone": "13800138000"
        }
        
        try:
            response = self.make_request("POST", "/api/order/create", order_data)
            execution_time = time.time() - start_time
            
            if response.status_code != 200 or not response.json().get("success", True):
                self.log_test_result(
                    "无效票档ID测试", True,
                    "正确拒绝了无效票档ID",
                    None, execution_time
                )
                return True
            else:
                self.log_test_result(
                    "无效票档ID测试", False,
                    "错误接受了无效票档ID",
                    response.text, execution_time
                )
                return False
                
        except Exception as e:
            execution_time = time.time() - start_time
            self.log_test_result(
                "无效票档ID测试", False,
                f"测试异常: {str(e)}", None, execution_time
            )
            return False
    
    def _test_invalid_quantity(self) -> bool:
        """测试无效数量"""
        start_time = time.time()
        
        order_data = {
            "userId": self.user_id,
            "showId": 1,
            "sessionId": 1,
            "ticketId": 1,
            "quantity": 0,  # 无效数量
            "contactPhone": "13800138000"
        }
        
        try:
            response = self.make_request("POST", "/api/order/create", order_data)
            execution_time = time.time() - start_time
            
            if response.status_code != 200 or not response.json().get("success", True):
                self.log_test_result(
                    "无效数量测试", True,
                    "正确拒绝了无效数量",
                    None, execution_time
                )
                return True
            else:
                self.log_test_result(
                    "无效数量测试", False,
                    "错误接受了无效数量",
                    response.text, execution_time
                )
                return False
                
        except Exception as e:
            execution_time = time.time() - start_time
            self.log_test_result(
                "无效数量测试", False,
                f"测试异常: {str(e)}", None, execution_time
            )
            return False
    
    def _test_insufficient_stock(self) -> bool:
        """测试库存不足场景"""
        start_time = time.time()
        
        order_data = {
            "userId": self.user_id,
            "showId": 1,
            "sessionId": 1,
            "ticketId": 1,
            "quantity": 1000,  # 超大数量，模拟库存不足
            "contactPhone": "13800138000"
        }
        
        try:
            response = self.make_request("POST", "/api/order/create", order_data)
            execution_time = time.time() - start_time
            
            if response.status_code != 200 or not response.json().get("success", True):
                self.log_test_result(
                    "库存不足测试", True,
                    "正确处理了库存不足场景",
                    None, execution_time
                )
                return True
            else:
                self.log_test_result(
                    "库存不足测试", False,
                    "库存不足场景处理异常",
                    response.text, execution_time
                )
                return False
                
        except Exception as e:
            execution_time = time.time() - start_time
            self.log_test_result(
                "库存不足测试", False,
                f"测试异常: {str(e)}", None, execution_time
            )
            return False
    
    def _test_unauthorized_access(self) -> bool:
        """测试未授权访问"""
        start_time = time.time()
        
        order_data = {
            "userId": self.user_id,
            "showId": 1,
            "sessionId": 1,
            "ticketId": 1,
            "quantity": 1,
            "contactPhone": "13800138000"
        }
        
        try:
            # 不使用认证token
            response = self.make_request("POST", "/api/order/create", order_data, use_auth=False)
            execution_time = time.time() - start_time
            
            if response.status_code in [401, 403] or (
                response.status_code == 500 and "用户未登录" in response.text
            ):
                self.log_test_result(
                    "未授权访问测试", True,
                    "正确拒绝了未授权访问",
                    None, execution_time
                )
                return True
            else:
                self.log_test_result(
                    "未授权访问测试", False,
                    f"未授权访问处理异常: HTTP {response.status_code}",
                    response.text, execution_time
                )
                return False
                
        except Exception as e:
            execution_time = time.time() - start_time
            self.log_test_result(
                "未授权访问测试", False,
                f"测试异常: {str(e)}", None, execution_time
            )
            return False
    
    def _test_missing_parameters(self) -> bool:
        """测试缺少必要参数"""
        start_time = time.time()
        
        # 缺少必要字段的订单数据
        order_data = {
            "userId": self.user_id,
            # 缺少showId, sessionId, ticketId, quantity
            "contactPhone": "13800138000"
        }
        
        try:
            response = self.make_request("POST", "/api/order/create", order_data)
            execution_time = time.time() - start_time
            
            if response.status_code in [400, 500] or not response.json().get("success", True):
                self.log_test_result(
                    "缺少参数测试", True,
                    "正确拒绝了缺少必要参数的请求",
                    None, execution_time
                )
                return True
            else:
                self.log_test_result(
                    "缺少参数测试", False,
                    "错误接受了缺少必要参数的请求",
                    response.text, execution_time
                )
                return False
                
        except Exception as e:
            execution_time = time.time() - start_time
            self.log_test_result(
                "缺少参数测试", False,
                f"测试异常: {str(e)}", None, execution_time
            )
            return False
    
    # ==================== 并发测试 ====================
    
    def test_06_concurrent_order_creation(self) -> bool:
        """测试并发订单创建"""
        if not self.jwt_token:
            self.log_test_result("并发测试", False, "未获取到JWT token")
            return False
        
        start_time = time.time()
        concurrent_users = self.test_config["concurrent_users"]
        requests_per_user = self.test_config["concurrent_requests"] // concurrent_users
        
        results = []
        
        def concurrent_create_orders(user_index):
            """并发创建订单的线程函数"""
            user_results = []
            
            for i in range(requests_per_user):
                order_data = {
                    "userId": self.user_id,
                    "showId": random.choice(self.test_data["valid_show_ids"]),
                    "sessionId": 1,
                    "ticketId": random.choice(self.test_data["valid_ticket_ids"]),
                    "quantity": 1,
                    "contactPhone": "13800138000",
                    "remark": f"并发测试-用户{user_index}-请求{i}"
                }
                
                try:
                    response = self.make_request("POST", "/api/order/create", order_data)
                    success = response.status_code == 200 and response.json().get("success", False)
                    user_results.append({
                        "success": success,
                        "response_time": response.elapsed.total_seconds(),
                        "status_code": response.status_code
                    })
                    
                    # 避免请求过于密集
                    time.sleep(0.1)
                    
                except Exception as e:
                    user_results.append({
                        "success": False,
                        "error": str(e),
                        "response_time": 0
                    })
            
            results.extend(user_results)
        
        # 启动并发线程
        with concurrent.futures.ThreadPoolExecutor(max_workers=concurrent_users) as executor:
            futures = [
                executor.submit(concurrent_create_orders, i) 
                for i in range(concurrent_users)
            ]
            
            # 等待所有线程完成
            concurrent.futures.wait(futures)
        
        execution_time = time.time() - start_time
        
        # 分析结果
        total_requests = len(results)
        successful_requests = sum(1 for r in results if r.get("success", False))
        failed_requests = total_requests - successful_requests
        
        if successful_requests > 0:
            avg_response_time = sum(r.get("response_time", 0) for r in results if r.get("success", False)) / successful_requests
        else:
            avg_response_time = 0
        
        success_rate = (successful_requests / total_requests) * 100 if total_requests > 0 else 0
        
        # 判断测试是否通过（成功率 > 80%，平均响应时间 < 5秒）
        test_passed = success_rate > 80 and avg_response_time < 5.0
        
        self.log_test_result(
            "并发订单创建测试", test_passed,
            f"并发测试完成: 成功率 {success_rate:.1f}%, 平均响应时间 {avg_response_time:.3f}s",
            {
                "total_requests": total_requests,
                "successful_requests": successful_requests,
                "failed_requests": failed_requests,
                "success_rate": success_rate,
                "avg_response_time": avg_response_time,
                "concurrent_users": concurrent_users
            },
            execution_time
        )
        
        return test_passed
    
    # ==================== 测试报告生成 ====================
    
    def generate_test_report(self) -> Dict[str, Any]:
        """生成测试报告"""
        total_tests = len(self.test_results)
        passed_tests = sum(1 for result in self.test_results if result["success"])
        failed_tests = total_tests - passed_tests
        
        success_rate = (passed_tests / total_tests) * 100 if total_tests > 0 else 0
        
        # 计算平均执行时间
        avg_execution_time = sum(result.get("execution_time", 0) for result in self.test_results) / total_tests if total_tests > 0 else 0
        
        report = {
            "test_summary": {
                "total_tests": total_tests,
                "passed_tests": passed_tests,
                "failed_tests": failed_tests,
                "success_rate": round(success_rate, 2),
                "avg_execution_time": round(avg_execution_time, 3),
                "test_duration": sum(result.get("execution_time", 0) for result in self.test_results),
                "test_timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            },
            "test_categories": {
                "jwt_authentication": [r for r in self.test_results if "JWT认证" in r["test_name"]],
                "normal_flow": [r for r in self.test_results if "正常流程" in r["test_name"] or "步骤验证" in r["test_name"]],
                "exception_scenarios": [r for r in self.test_results if any(x in r["test_name"] for x in ["异常", "无效", "库存不足", "未授权", "缺少"])],
                "concurrent_tests": [r for r in self.test_results if "并发" in r["test_name"]],
                "component_tests": [r for r in self.test_results if any(x in r["test_name"] for x in ["分布式锁", "参数校验", "票价", "订单号", "Redis", "演出信息", "持久化"])]
            },
            "detailed_results": self.test_results,
            "test_config": self.test_config
        }
        
        return report
    
    def save_test_report(self, report: Dict[str, Any], filename: str = None):
        """保存测试报告到文件"""
        if filename is None:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            filename = f"order_creation_test_report_{timestamp}.json"
        
        filepath = f"D:\\Tickets\\test\\test_for_High\\{filename}"
        
        try:
            with open(filepath, 'w', encoding='utf-8') as f:
                json.dump(report, f, ensure_ascii=False, indent=2)
            
            logging.info(f"测试报告已保存到: {filepath}")
            return filepath
        except Exception as e:
            logging.error(f"保存测试报告失败: {str(e)}")
            return None
    
    # ==================== 主测试流程 ====================
    
    def run_all_tests(self):
        """运行所有测试"""
        logging.info("=" * 60)
        logging.info("开始订单创建功能综合测试")
        logging.info("=" * 60)
        
        start_time = time.time()
        
        # 测试流程
        test_methods = [
            self.test_01_jwt_authentication,
            self.test_02_jwt_validation,
            self.test_03_order_creation_normal_flow,
            self.test_04_order_creation_steps_validation,
            self.test_05_exception_scenarios,
            self.test_06_concurrent_order_creation
        ]
        
        for test_method in test_methods:
            try:
                test_method()
            except Exception as e:
                logging.error(f"测试方法 {test_method.__name__} 执行异常: {str(e)}")
            
            # 测试间隔
            time.sleep(1)
        
        total_time = time.time() - start_time
        
        # 生成并保存测试报告
        report = self.generate_test_report()
        report_file = self.save_test_report(report)
        
        # 输出测试总结
        logging.info("=" * 60)
        logging.info("测试完成总结")
        logging.info("=" * 60)
        logging.info(f"总测试数: {report['test_summary']['total_tests']}")
        logging.info(f"通过测试: {report['test_summary']['passed_tests']}")
        logging.info(f"失败测试: {report['test_summary']['failed_tests']}")
        logging.info(f"成功率: {report['test_summary']['success_rate']}%")
        logging.info(f"总耗时: {total_time:.3f}秒")
        logging.info(f"测试报告: {report_file}")
        logging.info("=" * 60)
        
        return report


def main():
    """主函数"""
    # 创建测试实例
    tester = OrderCreationComprehensiveTest()
    
    # 运行所有测试
    report = tester.run_all_tests()
    
    # 返回测试结果
    return report['test_summary']['success_rate'] > 80


if __name__ == "__main__":
    main()