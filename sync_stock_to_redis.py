#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
库存数据同步脚本
从MySQL数据库同步库存数据到Redis
"""

import mysql.connector
import redis
import logging
import sys

# 配置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# 数据库配置
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '123456',
    'database': 'ticket_show_db',
    'charset': 'utf8mb4'
}

# Redis配置
REDIS_CONFIG = {
    'host': 'localhost',
    'port': 6379,
    'db': 0,
    'decode_responses': True
}

# 库存key前缀
STOCK_KEY_PREFIX = "stock:ticket:"

def connect_mysql():
    """连接MySQL数据库"""
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        logger.info("MySQL连接成功")
        return conn
    except Exception as e:
        logger.error(f"MySQL连接失败: {e}")
        return None

def connect_redis():
    """连接Redis"""
    try:
        r = redis.Redis(**REDIS_CONFIG)
        r.ping()
        logger.info("Redis连接成功")
        return r
    except Exception as e:
        logger.error(f"Redis连接失败: {e}")
        return None

def get_ticket_stocks(mysql_conn):
    """从数据库获取所有票档库存信息"""
    try:
        cursor = mysql_conn.cursor(dictionary=True)
        query = """
        SELECT id, name, price, total_count, remain_count, status
        FROM t_ticket 
        WHERE is_deleted = 0 AND status = 1
        ORDER BY id
        """
        cursor.execute(query)
        tickets = cursor.fetchall()
        cursor.close()
        logger.info(f"从数据库获取到 {len(tickets)} 个票档")
        return tickets
    except Exception as e:
        logger.error(f"获取票档库存信息失败: {e}")
        return []

def sync_stock_to_redis(redis_conn, tickets):
    """同步库存到Redis"""
    success_count = 0
    error_count = 0
    
    for ticket in tickets:
        try:
            ticket_id = ticket['id']
            remain_count = ticket['remain_count']
            stock_key = f"{STOCK_KEY_PREFIX}{ticket_id}"
            
            # 设置库存值
            redis_conn.set(stock_key, str(remain_count))
            
            # 设置过期时间（24小时）
            redis_conn.expire(stock_key, 86400)
            
            logger.info(f"同步成功 - 票档ID: {ticket_id}, 库存: {remain_count}, Key: {stock_key}")
            success_count += 1
            
        except Exception as e:
            logger.error(f"同步失败 - 票档ID: {ticket['id']}, 错误: {e}")
            error_count += 1
    
    logger.info(f"库存同步完成 - 成功: {success_count}, 失败: {error_count}")
    return success_count, error_count

def verify_sync_result(redis_conn, tickets):
    """验证同步结果"""
    logger.info("开始验证同步结果...")
    
    for ticket in tickets:
        ticket_id = ticket['id']
        expected_stock = ticket['remain_count']
        stock_key = f"{STOCK_KEY_PREFIX}{ticket_id}"
        
        try:
            redis_stock = redis_conn.get(stock_key)
            if redis_stock is None:
                logger.error(f"验证失败 - 票档ID: {ticket_id}, Redis中不存在")
            elif int(redis_stock) != expected_stock:
                logger.error(f"验证失败 - 票档ID: {ticket_id}, 期望: {expected_stock}, 实际: {redis_stock}")
            else:
                logger.info(f"验证成功 - 票档ID: {ticket_id}, 库存: {redis_stock}")
        except Exception as e:
            logger.error(f"验证异常 - 票档ID: {ticket_id}, 错误: {e}")

def main():
    """主函数"""
    logger.info("开始库存数据同步...")
    
    # 连接数据库
    mysql_conn = connect_mysql()
    if not mysql_conn:
        logger.error("无法连接MySQL，退出程序")
        sys.exit(1)
    
    # 连接Redis
    redis_conn = connect_redis()
    if not redis_conn:
        logger.error("无法连接Redis，退出程序")
        mysql_conn.close()
        sys.exit(1)
    
    try:
        # 获取票档库存信息
        tickets = get_ticket_stocks(mysql_conn)
        if not tickets:
            logger.warning("没有找到有效的票档数据")
            return
        
        # 同步到Redis
        success_count, error_count = sync_stock_to_redis(redis_conn, tickets)
        
        # 验证同步结果
        if success_count > 0:
            verify_sync_result(redis_conn, tickets)
        
        logger.info(f"库存同步任务完成 - 总数: {len(tickets)}, 成功: {success_count}, 失败: {error_count}")
        
    except Exception as e:
        logger.error(f"同步过程中发生异常: {e}")
    finally:
        # 关闭连接
        mysql_conn.close()
        redis_conn.close()
        logger.info("数据库连接已关闭")

if __name__ == "__main__":
    main()