#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import mysql.connector

try:
    # 连接数据库
    conn = mysql.connector.connect(
        host='localhost',
        user='root',
        password='123456',
        database='ticket_show'
    )
    
    cursor = conn.cursor()
    
    # 获取所有表名
    cursor.execute("SHOW TABLES")
    tables = cursor.fetchall()
    
    print(f"数据库 'ticket_show' 中的表结构:\n")
    
    for (table_name,) in tables:
        print(f"--- 表: {table_name} ---")
        
        # 获取表结构
        cursor.execute(f"SHOW CREATE TABLE {table_name}")
        create_table_statement = cursor.fetchone()
        
        print(create_table_statement[1])
        print("\n" + "-" * 50 + "\n")

except mysql.connector.Error as e:
    print(f"数据库连接错误: {e}")
except Exception as e:
    print(f"执行错误: {e}")
finally:
    if 'conn' in locals() and conn.is_connected():
        cursor.close()
        conn.close()
        print("数据库连接已关闭")

