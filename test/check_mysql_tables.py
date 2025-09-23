#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
检查MySQL数据库表结构
"""

import pymysql
import json

def check_mysql_tables():
    # 数据库连接配置
    config = {
        'host': 'localhost',
        'port': 3306,
        'user': 'root',
        'password': '123456',
        'charset': 'utf8mb4'
    }
    
    try:
        # 连接MySQL
        connection = pymysql.connect(**config)
        cursor = connection.cursor()
        
        print("=== MySQL数据库表结构检查 ===")
        
        # 1. 检查ticket_order_db.t_order表
        print("\n1. 检查ticket_order_db.t_order表...")
        cursor.execute("USE ticket_order_db")
        cursor.execute("DESCRIBE t_order")
        order_columns = cursor.fetchall()
        
        print("t_order表结构:")
        print(f"{'字段名':<20} {'类型':<20} {'允许NULL':<10} {'键':<10} {'默认值':<15} {'额外':<20}")
        print("-" * 100)
        for column in order_columns:
            field, type_info, null, key, default, extra = column
            print(f"{field:<20} {type_info:<20} {null:<10} {key:<10} {str(default):<15} {extra:<20}")
        
        # 2. 检查ticket_show_db.t_ticket表
        print(f"\n2. 检查ticket_show_db.t_ticket表...")
        cursor.execute("USE ticket_show_db")
        cursor.execute("DESCRIBE t_ticket")
        ticket_columns = cursor.fetchall()
        
        print("t_ticket表结构:")
        print(f"{'字段名':<20} {'类型':<20} {'允许NULL':<10} {'键':<10} {'默认值':<15} {'额外':<20}")
        print("-" * 100)
        for column in ticket_columns:
            field, type_info, null, key, default, extra = column
            print(f"{field:<20} {type_info:<20} {null:<10} {key:<10} {str(default):<15} {extra:<20}")
        
        # 3. 检查ticket_show_db.t_ticket_stock表
        print(f"\n3. 检查ticket_show_db.t_ticket_stock表...")
        cursor.execute("DESCRIBE t_ticket_stock")
        stock_columns = cursor.fetchall()
        
        print("t_ticket_stock表结构:")
        print(f"{'字段名':<20} {'类型':<20} {'允许NULL':<10} {'键':<10} {'默认值':<15} {'额外':<20}")
        print("-" * 100)
        for column in stock_columns:
            field, type_info, null, key, default, extra = column
            print(f"{field:<20} {type_info:<20} {null:<10} {key:<10} {str(default):<15} {extra:<20}")
        
        # 4. 检查ticket_show_db.t_ticket_lock表
        print(f"\n4. 检查ticket_show_db.t_ticket_lock表...")
        cursor.execute("DESCRIBE t_ticket_lock")
        lock_columns = cursor.fetchall()
        
        print("t_ticket_lock表结构:")
        print(f"{'字段名':<20} {'类型':<20} {'允许NULL':<10} {'键':<10} {'默认值':<15} {'额外':<20}")
        print("-" * 100)
        for column in lock_columns:
            field, type_info, null, key, default, extra = column
            print(f"{field:<20} {type_info:<20} {null:<10} {key:<10} {str(default):<15} {extra:<20}")
        
        # 5. 检查ticket_show_db.t_seat表
        print(f"\n5. 检查ticket_show_db.t_seat表...")
        cursor.execute("DESCRIBE t_seat")
        seat_columns = cursor.fetchall()
        
        print("t_seat表结构:")
        print(f"{'字段名':<20} {'类型':<20} {'允许NULL':<10} {'键':<10} {'默认值':<15} {'额外':<20}")
        print("-" * 100)
        for column in seat_columns:
            field, type_info, null, key, default, extra = column
            print(f"{field:<20} {type_info:<20} {null:<10} {key:<10} {str(default):<15} {extra:<20}")
        
        # 6. 检查ticket_show_db.t_seat_area表
        print(f"\n6. 检查ticket_show_db.t_seat_area表...")
        cursor.execute("DESCRIBE t_seat_area")
        area_columns = cursor.fetchall()
        
        print("t_seat_area表结构:")
        print(f"{'字段名':<20} {'类型':<20} {'允许NULL':<10} {'键':<10} {'默认值':<15} {'额外':<20}")
        print("-" * 100)
        for column in area_columns:
            field, type_info, null, key, default, extra = column
            print(f"{field:<20} {type_info:<20} {null:<10} {key:<10} {str(default):<15} {extra:<20}")
        
        # 7. 检查所有表的数据量
        print(f"\n7. 检查表数据量...")
        
        # ticket_order_db
        cursor.execute("USE ticket_order_db")
        cursor.execute("SELECT COUNT(*) FROM t_order")
        order_count = cursor.fetchone()[0]
        print(f"t_order表数据量: {order_count}")
        
        # ticket_show_db
        cursor.execute("USE ticket_show_db")
        cursor.execute("SELECT COUNT(*) FROM t_ticket")
        ticket_count = cursor.fetchone()[0]
        print(f"t_ticket表数据量: {ticket_count}")
        
        cursor.execute("SELECT COUNT(*) FROM t_ticket_stock")
        stock_count = cursor.fetchone()[0]
        print(f"t_ticket_stock表数据量: {stock_count}")
        
        cursor.execute("SELECT COUNT(*) FROM t_ticket_lock")
        lock_count = cursor.fetchone()[0]
        print(f"t_ticket_lock表数据量: {lock_count}")
        
        cursor.execute("SELECT COUNT(*) FROM t_seat")
        seat_count = cursor.fetchone()[0]
        print(f"t_seat表数据量: {seat_count}")
        
        cursor.execute("SELECT COUNT(*) FROM t_seat_area")
        area_count = cursor.fetchone()[0]
        print(f"t_seat_area表数据量: {area_count}")
        
        # 8. 检查索引
        print(f"\n8. 检查关键表索引...")
        
        # t_order表索引
        cursor.execute("USE ticket_order_db")
        cursor.execute("SHOW INDEX FROM t_order")
        order_indexes = cursor.fetchall()
        print(f"t_order表索引:")
        for index in order_indexes:
            print(f"  {index[2]}: {index[4]} ({index[10]})")
        
        # t_ticket_stock表索引
        cursor.execute("USE ticket_show_db")
        cursor.execute("SHOW INDEX FROM t_ticket_stock")
        stock_indexes = cursor.fetchall()
        print(f"t_ticket_stock表索引:")
        for index in stock_indexes:
            print(f"  {index[2]}: {index[4]} ({index[10]})")
        
        # t_seat表索引
        cursor.execute("SHOW INDEX FROM t_seat")
        seat_indexes = cursor.fetchall()
        print(f"t_seat表索引:")
        for index in seat_indexes:
            print(f"  {index[2]}: {index[4]} ({index[10]})")
        
    except Exception as e:
        print(f"❌ 数据库查询异常: {e}")
    finally:
        if 'connection' in locals():
            connection.close()

if __name__ == "__main__":
    check_mysql_tables()