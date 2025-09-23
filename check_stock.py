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
    
    # 查询 t_ticket 表
    print("查询 t_ticket 表 (前10条记录):")
    cursor.execute("""
        SELECT * FROM t_ticket LIMIT 10
    """)
    
    results = cursor.fetchall()
    
    if results:
        # 获取并打印列名
        column_names = [i[0] for i in cursor.description]
        print("\t".join(column_names))
        print("-" * (len(column_names) * 10))
        
        # 打印每一行数据
        for row in results:
            print("\t".join(map(str, row)))
    else:
        print("t_ticket 表中没有数据或查询出错。")

except mysql.connector.Error as e:
    print(f"数据库连接错误: {e}")
except Exception as e:
    print(f"执行错误: {e}")
finally:
    if 'conn' in locals() and conn.is_connected():
        cursor.close()
        conn.close()
        print("\n数据库连接已关闭")
