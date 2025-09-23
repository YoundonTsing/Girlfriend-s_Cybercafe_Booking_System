#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
简化版Redis连接测试脚本
快速验证Redis连接和基本功能
"""

import redis
import time

def test_redis_connection():
    """测试Redis基本连接"""
    try:
        # 连接Redis
        r = redis.Redis(host='localhost', port=6379, db=0, decode_responses=True)
        
        # 测试连接
        print("🔍 测试Redis连接...")
        r.ping()
        print("✅ Redis连接成功")
        
        # 测试基本操作
        print("\n🔧 测试基本Redis操作...")
        
        # 设置和获取
        test_key = "test:redisson:migration"
        test_value = "success"
        r.set(test_key, test_value)
        retrieved_value = r.get(test_key)
        
        if retrieved_value == test_value:
            print("✅ Redis SET/GET操作正常")
        else:
            print("❌ Redis SET/GET操作异常")
            
        # 测试过期时间
        r.setex("test:expire", 2, "will_expire")
        print("✅ Redis SETEX操作正常")
        
        # 测试列表操作
        list_key = "test:list"
        r.delete(list_key)  # 清理
        r.lpush(list_key, "item1", "item2", "item3")
        list_length = r.llen(list_key)
        
        if list_length == 3:
            print("✅ Redis LIST操作正常")
        else:
            print("❌ Redis LIST操作异常")
            
        # 测试哈希操作
        hash_key = "test:hash"
        r.delete(hash_key)  # 清理
        r.hset(hash_key, mapping={"field1": "value1", "field2": "value2"})
        hash_length = r.hlen(hash_key)
        
        if hash_length == 2:
            print("✅ Redis HASH操作正常")
        else:
            print("❌ Redis HASH操作异常")
            
        # 清理测试数据
        r.delete(test_key, list_key, hash_key)
        print("\n🧹 测试数据清理完成")
        
        return True
        
    except redis.ConnectionError as e:
        print(f"❌ Redis连接失败: {e}")
        return False
    except Exception as e:
        print(f"❌ Redis测试异常: {e}")
        return False

def test_stock_keys():
    """检查库存相关的Redis键"""
    try:
        r = redis.Redis(host='localhost', port=6379, db=0, decode_responses=True)
        
        print("\n📦 检查库存相关Redis键...")
        
        # 查找库存相关的键
        stock_keys = r.keys("ticket:stock:*")
        if stock_keys:
            print(f"✅ 找到 {len(stock_keys)} 个库存键:")
            for key in stock_keys[:5]:  # 只显示前5个
                value = r.get(key)
                print(f"   {key}: {value}")
        else:
            print("ℹ️  未找到库存键（可能需要先同步库存）")
            
        # 查找锁相关的键
        lock_keys = r.keys("*lock*")
        if lock_keys:
            print(f"\n🔒 找到 {len(lock_keys)} 个锁相关键:")
            for key in lock_keys[:5]:  # 只显示前5个
                ttl = r.ttl(key)
                print(f"   {key}: TTL={ttl}s")
        else:
            print("\nℹ️  未找到锁相关键")
            
        # 查找限流相关的键
        rate_limit_keys = r.keys("rate_limit:*")
        if rate_limit_keys:
            print(f"\n⏱️  找到 {len(rate_limit_keys)} 个限流键:")
            for key in rate_limit_keys[:5]:  # 只显示前5个
                value = r.get(key)
                ttl = r.ttl(key)
                print(f"   {key}: {value}, TTL={ttl}s")
        else:
            print("\nℹ️  未找到限流键")
            
        return True
        
    except Exception as e:
        print(f"❌ 检查Redis键异常: {e}")
        return False

def main():
    print("🚀 开始简化版Redis测试...\n")
    
    # 基本连接测试
    connection_ok = test_redis_connection()
    
    if connection_ok:
        # 检查业务相关键
        test_stock_keys()
        
        print("\n🎉 Redis基本功能测试完成！")
        print("\n💡 提示:")
        print("   - 如果要测试完整功能，请先启动ticket-show和ticket-order服务")
        print("   - 然后运行 redis_redisson_test.py 进行完整测试")
    else:
        print("\n❌ Redis连接失败，请检查Redis服务是否启动")
        print("\n🔧 解决方案:")
        print("   1. 确保Redis服务正在运行")
        print("   2. 检查Redis配置（host: localhost, port: 6379）")
        print("   3. 检查防火墙设置")

if __name__ == "__main__":
    main()