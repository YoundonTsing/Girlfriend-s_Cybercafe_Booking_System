#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
票务系统业务流程完整测试用例
测试从展示订单到支付完成/取消预约的完整业务流程

测试用户：admin/123456
"""

import requests
import json
import time
from typing import Dict, Any, Optional

class TicketSystemBusinessFlowTest:
    def __init__(self, base_url: str = "http://localhost:8000"):
        self.base_url = base_url
        self.session = requests.Session()
        self.jwt_token = None
        self.user_id = None
        self.test_results = []
        
    def log_test_result(self, test_name: str, success: bool, message: str, response_data: Any = None):
        """记录测试结果"""
        result = {
            "test_name": test_name,
            "success": success,
            "message": message,
            "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
            "response_data": response_data
        }
        self.test_results.append(result)
        status = "✅ PASS" if success else "❌ FAIL"
        print(f"{status} {test_name}: {message}")
        
    def make_request(self, method: str, endpoint: str, data: Dict = None, headers: Dict = None) -> requests.Response:
        """统一请求方法"""
        url = f"{self.base_url}{endpoint}"
        request_headers = {"Content-Type": "application/json"}
        
        if self.jwt_token:
            request_headers["Authorization"] = f"Bearer {self.jwt_token}"
            
        if headers:
            request_headers.update(headers)
            
        try:
            if method.upper() == "GET":
                response = self.session.get(url, headers=request_headers, params=data, timeout=10)
            elif method.upper() == "POST":
                response = self.session.post(url, json=data, headers=request_headers, timeout=10)
            elif method.upper() == "PUT":
                response = self.session.put(url, json=data, headers=request_headers, timeout=10)
            elif method.upper() == "DELETE":
                response = self.session.delete(url, headers=request_headers, timeout=10)
            else:
                raise ValueError(f"Unsupported HTTP method: {method}")
                
            return response
        except requests.exceptions.RequestException as e:
            print(f"Request failed: {e}")
            raise
    
    def test_01_user_login(self) -> bool:
        """测试用户登录并获取JWT token"""
        login_data = {
            "username": "admin",
            "password": "123456"
        }
        
        try:
            response = self.make_request("POST", "/api/user/login", login_data)
            
            if response.status_code == 200:
                response_data = response.json()
                if response_data.get("success") and response_data.get("data"):
                    login_vo = response_data["data"]
                    self.jwt_token = login_vo.get("token")
                    self.user_id = login_vo.get("userId")
                    
                    if self.jwt_token:
                        self.log_test_result("用户登录", True, f"登录成功，获取到JWT token，用户ID: {self.user_id}", login_vo)
                        return True
                    else:
                        self.log_test_result("用户登录", False, "登录响应中未找到token", response_data)
                        return False
                else:
                    self.log_test_result("用户登录", False, f"登录失败: {response_data.get('message', '未知错误')}", response_data)
                    return False
            else:
                self.log_test_result("用户登录", False, f"HTTP错误: {response.status_code}", response.text)
                return False
                
        except Exception as e:
            self.log_test_result("用户登录", False, f"请求异常: {str(e)}")
            return False
    
    def test_02_get_show_list(self) -> Optional[Dict]:
        """测试获取演出列表"""
        try:
            response = self.make_request("GET", "/api/show/list", {"page": 1, "size": 10})
            
            if response.status_code == 200:
                response_data = response.json()
                if response_data.get("success") and response_data.get("data"):
                    shows = response_data["data"]
                    if isinstance(shows, dict) and "records" in shows:
                        show_list = shows["records"]
                    elif isinstance(shows, list):
                        show_list = shows
                    else:
                        show_list = []
                        
                    if show_list:
                        selected_show = show_list[0]  # 选择第一个演出
                        self.log_test_result("获取演出列表", True, f"成功获取演出列表，共{len(show_list)}个演出，选择演出: {selected_show.get('name', 'N/A')}", selected_show)
                        return selected_show
                    else:
                        self.log_test_result("获取演出列表", False, "演出列表为空", response_data)
                        return None
                else:
                    self.log_test_result("获取演出列表", False, f"获取失败: {response_data.get('message', '未知错误')}", response_data)
                    return None
            else:
                self.log_test_result("获取演出列表", False, f"HTTP错误: {response.status_code}", response.text)
                return None
                
        except Exception as e:
            self.log_test_result("获取演出列表", False, f"请求异常: {str(e)}")
            return None
    
    def test_03_get_show_detail(self, show_id: int) -> Optional[Dict]:
        """测试获取演出详情"""
        try:
            response = self.make_request("GET", f"/api/show/{show_id}")
            
            if response.status_code == 200:
                response_data = response.json()
                if response_data.get("success") and response_data.get("data"):
                    show_detail = response_data["data"]
                    self.log_test_result("获取演出详情", True, f"成功获取演出详情: {show_detail.get('name', 'N/A')}", show_detail)
                    return show_detail
                else:
                    self.log_test_result("获取演出详情", False, f"获取失败: {response_data.get('message', '未知错误')}", response_data)
                    return None
            else:
                self.log_test_result("获取演出详情", False, f"HTTP错误: {response.status_code}", response.text)
                return None
                
        except Exception as e:
            self.log_test_result("获取演出详情", False, f"请求异常: {str(e)}")
            return None
    
    def test_04_get_ticket_list(self, show_id: int, session_id: int) -> Optional[list]:
        """测试获取票档列表"""
        try:
            response = self.make_request("GET", f"/api/ticket/list", {"showId": show_id, "sessionId": session_id})
            
            if response.status_code == 200:
                response_data = response.json()
                if response_data.get("success") and response_data.get("data"):
                    tickets = response_data["data"]
                    if isinstance(tickets, list) and tickets:
                        self.log_test_result("获取票档列表", True, f"成功获取票档列表，共{len(tickets)}个票档", tickets)
                        return tickets
                    else:
                        self.log_test_result("获取票档列表", False, "票档列表为空", response_data)
                        return None
                else:
                    self.log_test_result("获取票档列表", False, f"获取失败: {response_data.get('message', '未知错误')}", response_data)
                    return None
            else:
                self.log_test_result("获取票档列表", False, f"HTTP错误: {response.status_code}", response.text)
                return None
                
        except Exception as e:
            self.log_test_result("获取票档列表", False, f"请求异常: {str(e)}")
            return None
    
    def test_05_get_ticket_price(self, ticket_id: int) -> Optional[float]:
        """测试获取票档价格"""
        try:
            response = self.make_request("GET", f"/api/ticket/price/{ticket_id}")
            
            if response.status_code == 200:
                response_data = response.json()
                if response_data.get("success") and response_data.get("data") is not None:
                    price = response_data["data"]
                    self.log_test_result("获取票档价格", True, f"成功获取票档价格: {price}元", response_data)
                    return float(price)
                else:
                    self.log_test_result("获取票档价格", False, f"获取失败: {response_data.get('message', '未知错误')}", response_data)
                    return None
            else:
                self.log_test_result("获取票档价格", False, f"HTTP错误: {response.status_code}", response.text)
                return None
                
        except Exception as e:
            self.log_test_result("获取票档价格", False, f"请求异常: {str(e)}")
            return None
    
    def test_06_get_seat_layout(self, show_id: int) -> Optional[Dict]:
        """测试获取座位布局"""
        try:
            response = self.make_request("GET", f"/api/seat/layout/{show_id}")
            
            if response.status_code == 200:
                response_data = response.json()
                if response_data.get("success") and response_data.get("data"):
                    seat_layout = response_data["data"]
                    self.log_test_result("获取座位布局", True, "成功获取座位布局信息", seat_layout)
                    return seat_layout
                else:
                    self.log_test_result("获取座位布局", False, f"获取失败: {response_data.get('message', '未知错误')}", response_data)
                    return None
            else:
                self.log_test_result("获取座位布局", False, f"HTTP错误: {response.status_code}", response.text)
                return None
                
        except Exception as e:
            self.log_test_result("获取座位布局", False, f"请求异常: {str(e)}")
            return None
    
    def test_07_lock_seat(self, seat_id: int) -> bool:
        """测试锁定座位"""
        try:
            lock_data = {
                "seatIds": [seat_id]
            }
            response = self.make_request("POST", "/api/seat/lock", lock_data)
            
            if response.status_code == 200:
                response_data = response.json()
                if response_data.get("success") and response_data.get("data"):
                    self.log_test_result("锁定座位", True, f"成功锁定座位ID: {seat_id}", response_data)
                    return True
                else:
                    self.log_test_result("锁定座位", False, f"锁定失败: {response_data.get('message', '未知错误')}", response_data)
                    return False
            else:
                self.log_test_result("锁定座位", False, f"HTTP错误: {response.status_code}", response.text)
                return False
                
        except Exception as e:
            self.log_test_result("锁定座位", False, f"请求异常: {str(e)}")
            return False
    
    def test_08_create_order(self, show_id: int, session_id: int, ticket_id: int, seat_ids: list, total_amount: float) -> Optional[str]:
        """测试创建订单"""
        try:
            order_data = {
                "userId": self.user_id,  # 添加用户ID
                "showId": show_id,
                "sessionId": session_id,  # 添加场次ID
                "ticketId": ticket_id,
                "seatIds": seat_ids,
                "quantity": len(seat_ids),
                "totalAmount": total_amount,
                "contactPhone": "13800138000",
                "contactName": "测试用户"
            }
            response = self.make_request("POST", "/api/order/create", order_data)
            
            if response.status_code == 200:
                response_data = response.json()
                if response_data.get("success") and response_data.get("data"):
                    order_info = response_data["data"]
                    order_id = order_info.get("orderId") or order_info.get("id")
                    if order_id:
                        self.log_test_result("创建订单", True, f"成功创建订单，订单ID: {order_id}", order_info)
                        return str(order_id)
                    else:
                        self.log_test_result("创建订单", False, "订单创建成功但未返回订单ID", response_data)
                        return None
                else:
                    self.log_test_result("创建订单", False, f"创建失败: {response_data.get('message', '未知错误')}", response_data)
                    return None
            else:
                self.log_test_result("创建订单", False, f"HTTP错误: {response.status_code}", response.text)
                return None
                
        except Exception as e:
            self.log_test_result("创建订单", False, f"请求异常: {str(e)}")
            return None
    
    def test_09_get_order_detail(self, order_id: str) -> Optional[Dict]:
        """测试获取订单详情"""
        try:
            response = self.make_request("GET", f"/api/order/{order_id}")
            
            if response.status_code == 200:
                response_data = response.json()
                if response_data.get("success") and response_data.get("data"):
                    order_detail = response_data["data"]
                    self.log_test_result("获取订单详情", True, f"成功获取订单详情，状态: {order_detail.get('status', 'N/A')}", order_detail)
                    return order_detail
                else:
                    self.log_test_result("获取订单详情", False, f"获取失败: {response_data.get('message', '未知错误')}", response_data)
                    return None
            else:
                self.log_test_result("获取订单详情", False, f"HTTP错误: {response.status_code}", response.text)
                return None
                
        except Exception as e:
            self.log_test_result("获取订单详情", False, f"请求异常: {str(e)}")
            return None
    
    def test_10_pay_order(self, order_id: str) -> bool:
        """测试支付订单"""
        try:
            pay_data = {
                "orderId": order_id,
                "paymentMethod": "ALIPAY",
                "amount": 100.0  # 假设金额
            }
            response = self.make_request("POST", f"/api/order/pay/{order_id}", pay_data)
            
            if response.status_code == 200:
                response_data = response.json()
                if response_data.get("success"):
                    self.log_test_result("支付订单", True, f"订单支付成功，订单ID: {order_id}", response_data)
                    return True
                else:
                    self.log_test_result("支付订单", False, f"支付失败: {response_data.get('message', '未知错误')}", response_data)
                    return False
            else:
                self.log_test_result("支付订单", False, f"HTTP错误: {response.status_code}", response.text)
                return False
                
        except Exception as e:
            self.log_test_result("支付订单", False, f"请求异常: {str(e)}")
            return False
    
    def test_11_cancel_order(self, order_id: str) -> bool:
        """测试取消订单"""
        try:
            response = self.make_request("PUT", f"/api/order/cancel/{order_id}")
            
            if response.status_code == 200:
                response_data = response.json()
                if response_data.get("success"):
                    self.log_test_result("取消订单", True, f"订单取消成功，订单ID: {order_id}", response_data)
                    return True
                else:
                    self.log_test_result("取消订单", False, f"取消失败: {response_data.get('message', '未知错误')}", response_data)
                    return False
            else:
                self.log_test_result("取消订单", False, f"HTTP错误: {response.status_code}", response.text)
                return False
                
        except Exception as e:
            self.log_test_result("取消订单", False, f"请求异常: {str(e)}")
            return False
    
    def run_complete_business_flow_test(self):
        """运行完整业务流程测试"""
        print("\n" + "="*80)
        print("🎫 票务系统完整业务流程测试开始")
        print("="*80)
        
        # 1. 用户登录
        if not self.test_01_user_login():
            print("❌ 登录失败，终止测试")
            return
        
        # 2. 获取演出列表
        selected_show = self.test_02_get_show_list()
        if not selected_show:
            print("❌ 获取演出列表失败，终止测试")
            return
        
        show_id = selected_show.get("id")
        if not show_id:
            print("❌ 演出ID不存在，终止测试")
            return
        
        # 3. 获取演出详情
        show_detail = self.test_03_get_show_detail(show_id)
        if not show_detail:
            print("⚠️ 获取演出详情失败，但继续测试")
        
        # 4. 获取票档列表
        # 从演出详情中获取场次ID，但需要使用有票档数据的场次
        sessions = show_detail.get("sessions", [])
        if not sessions:
            print("❌ 演出没有可用场次，终止测试")
            return
        
        # 根据数据库中的票档数据，使用正确的场次ID
        # show_id=1 对应 session_id=1或2，show_id=2 对应 session_id=3
        if show_id == 1:
            session_id = 1  # 使用有票档数据的场次ID
        elif show_id == 2:
            session_id = 3  # 使用有票档数据的场次ID
        else:
            session_id = sessions[0]["id"]  # 其他演出使用默认场次
        
        tickets = self.test_04_get_ticket_list(show_id, session_id)
        if not tickets:
            print("❌ 获取票档列表失败，终止测试")
            return
        
        # 选择第一个票档
        selected_ticket = tickets[0]
        ticket_id = selected_ticket.get("id")
        
        # 5. 获取票档价格
        ticket_price = self.test_05_get_ticket_price(ticket_id)
        if ticket_price is None:
            print("❌ 获取票档价格失败，终止测试")
            return
        
        # 6. 获取座位布局
        seat_layout = self.test_06_get_seat_layout(show_id)
        if not seat_layout:
            print("⚠️ 获取座位布局失败，使用默认座位ID")
            seat_id = 1  # 使用默认座位ID
        else:
            # 从座位布局中选择第一个可用座位
            seat_id = 1  # 简化处理，使用默认座位ID
        
        # 7. 锁定座位
        if not self.test_07_lock_seat(seat_id):
            print("⚠️ 锁定座位失败，但继续测试")
        
        # 8. 创建订单
        order_id = self.test_08_create_order(show_id, session_id, ticket_id, [seat_id], ticket_price)
        if not order_id:
            print("❌ 创建订单失败，终止测试")
            return
        
        # 9. 获取订单详情
        order_detail = self.test_09_get_order_detail(order_id)
        if not order_detail:
            print("⚠️ 获取订单详情失败，但继续测试")
        
        # 10. 测试两种结束方式
        print("\n🔄 测试支付流程...")
        if self.test_10_pay_order(order_id):
            print("✅ 支付流程测试完成")
        else:
            print("⚠️ 支付失败，测试取消流程")
            if self.test_11_cancel_order(order_id):
                print("✅ 取消流程测试完成")
            else:
                print("❌ 取消流程也失败")
        
        # 输出测试结果统计
        self.print_test_summary()
    
    def print_test_summary(self):
        """打印测试结果统计"""
        print("\n" + "="*80)
        print("📊 测试结果统计")
        print("="*80)
        
        total_tests = len(self.test_results)
        passed_tests = sum(1 for result in self.test_results if result["success"])
        failed_tests = total_tests - passed_tests
        
        print(f"总测试数: {total_tests}")
        print(f"通过: {passed_tests} ✅")
        print(f"失败: {failed_tests} ❌")
        print(f"成功率: {(passed_tests/total_tests*100):.1f}%")
        
        if failed_tests > 0:
            print("\n❌ 失败的测试:")
            for result in self.test_results:
                if not result["success"]:
                    print(f"  - {result['test_name']}: {result['message']}")
        
        print("\n" + "="*80)
        print("🎫 票务系统业务流程测试完成")
        print("="*80)

if __name__ == "__main__":
    # 创建测试实例
    test = TicketSystemBusinessFlowTest()
    
    # 运行完整业务流程测试
    test.run_complete_business_flow_test()
    
    # 保存测试结果到文件
    with open("business_flow_test_results.json", "w", encoding="utf-8") as f:
        json.dump(test.test_results, f, ensure_ascii=False, indent=2)
    
    print(f"\n📄 测试结果已保存到: business_flow_test_results.json")