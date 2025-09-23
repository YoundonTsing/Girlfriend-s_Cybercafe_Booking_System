#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Java Lua脚本参数传递测试
用于验证Java代码中Redisson客户端调用Lua脚本时的参数格式问题
"""

import redis
import time

def test_lua_script_params():
    """测试Lua脚本参数传递"""
    # 连接Redis
    r = redis.Redis(host='localhost', port=6379, db=0, decode_responses=True)
    
    print("=== Java Lua脚本参数传递测试 ===")
    
    # 清理测试数据
    test_key = "stock:ticket:test"
    r.delete(test_key)
    
    # 读取Lua脚本
    with open('D:\\Tickets\\ticket-show\\src\\main\\resources\\lua\\stock_init.lua', 'r', encoding='utf-8') as f:
        lua_script = f.read()
    
    print(f"Lua脚本内容长度: {len(lua_script)} 字符")
    print("\n--- 测试1: 模拟Java代码的参数传递方式 ---")
    
    # 模拟Java代码中的参数传递
    # Java代码: Object[] args = {availableStock.toString(), String.valueOf(DEFAULT_EXPIRE_TIME), forceUpdate ? "1" : "0"};
    keys = [test_key]
    args = ["600", "86400", "1"]  # 模拟Java传递的字符串参数
    
    try:
        # 执行Lua脚本
        result = r.eval(lua_script, 1, test_key, "600", "86400", "1")
        print(f"执行结果: {result}")
        print(f"类型: {type(result)}")
        
        # 检查Redis中的值
        stock_value = r.get(test_key)
        print(f"Redis中的库存值: {stock_value}")
        print(f"库存值类型: {type(stock_value)}")
        
    except Exception as e:
        print(f"执行失败: {e}")
    
    print("\n--- 测试2: 验证参数类型转换 ---")
    
    # 清理
    r.delete(test_key)
    
    # 测试不同的参数类型
    test_cases = [
        {"name": "字符串参数", "args": ["600", "86400", "1"]},
        {"name": "整数参数", "args": [600, 86400, 1]},
        {"name": "混合参数", "args": ["600", 86400, "1"]},
    ]
    
    for i, case in enumerate(test_cases):
        print(f"\n测试用例 {i+1}: {case['name']}")
        print(f"参数: {case['args']} (类型: {[type(arg).__name__ for arg in case['args']]})")
        
        try:
            result = r.eval(lua_script, 1, test_key, *case['args'])
            print(f"执行结果: {result}")
            
            stock_value = r.get(test_key)
            print(f"Redis库存值: {stock_value}")
            
            # 清理
            r.delete(test_key)
            
        except Exception as e:
            print(f"执行失败: {e}")
    
    print("\n--- 测试3: 验证Redisson风格的参数传递 ---")
    
    # 模拟Redisson的参数传递方式
    print("模拟Redisson客户端的参数传递...")
    
    # Redisson使用List<Object> keys和Object[] args
    try:
        # 这里模拟Redisson的调用方式
        result = r.eval(lua_script, 1, test_key, "600", "86400", "1")
        print(f"Redisson风格调用结果: {result}")
        
        if result == 1:
            print("✓ 初始化成功")
            stock_value = r.get(test_key)
            print(f"库存值: {stock_value}")
        else:
            print("✗ 初始化失败")
            
    except Exception as e:
        print(f"Redisson风格调用失败: {e}")
    
    print("\n--- 测试4: 验证预减操作 ---")
    
    # 读取预减脚本
    try:
        with open('D:\\Tickets\\ticket-show\\src\\main\\resources\\lua\\stock_prededuct.lua', 'r', encoding='utf-8') as f:
            prededuct_script = f.read()
        
        print("测试预减操作...")
        result = r.eval(prededuct_script, 1, test_key, "1", "86400")
        print(f"预减结果: {result}")
        
        stock_value = r.get(test_key)
        print(f"预减后库存: {stock_value}")
        
    except FileNotFoundError:
        print("预减脚本文件不存在")
    except Exception as e:
        print(f"预减操作失败: {e}")
    
    print("\n=== 测试完成 ===")

if __name__ == "__main__":
    test_lua_script_params()