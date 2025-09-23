#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
检查数据库表结构
"""

import pymysql
import json

def check_table_structure():
    print("=== 检查数据库表结构 ===")
    
    # 数据库连接配置
    config = {
        'host': 'localhost',
        'port': 3306,
        'user': 'root',
        'password': '123456',
        'database': 'ticket_show_db',  # 使用正确的数据库名
        'charset': 'utf8mb4'
    }
    
    try:
        # 连接数据库
        connection = pymysql.connect(**config)
        cursor = connection.cursor()
        
        print("✅ 数据库连接成功")
        
        # 1. 检查t_seat表结构
        print(f"\n1. 检查t_seat表结构...")
        cursor.execute("DESCRIBE t_seat")
        seat_columns = cursor.fetchall()
        
        print(f"t_seat表字段:")
        print(f"{'字段名':<20} {'类型':<20} {'允许NULL':<10} {'键':<10} {'默认值':<15} {'额外':<20}")
        print("-" * 100)
        
        for column in seat_columns:
            field, type_info, null, key, default, extra = column
            print(f"{field:<20} {type_info:<20} {null:<10} {key:<10} {str(default):<15} {extra:<20}")
        
        # 2. 检查t_ticket表结构
        print(f"\n2. 检查t_ticket表结构...")
        try:
            cursor.execute("DESCRIBE t_ticket")
            ticket_columns = cursor.fetchall()
            
            print(f"t_ticket表字段:")
            print(f"{'字段名':<20} {'类型':<20} {'允许NULL':<10} {'键':<10} {'默认值':<15} {'额外':<20}")
            print("-" * 100)
            
            for column in ticket_columns:
                field, type_info, null, key, default, extra = column
                print(f"{field:<20} {type_info:<20} {null:<10} {key:<10} {str(default):<15} {extra:<20}")
        except Exception as e:
            print(f"❌ 查询t_ticket表失败: {e}")
        
        # 3. 检查座位数据
        print(f"\n3. 检查座位数据...")
        cursor.execute("SELECT COUNT(*) FROM t_seat")
        total_seats = cursor.fetchone()[0]
        print(f"总座位数: {total_seats}")
        
        # 检查座位状态分布
        cursor.execute("""
        SELECT status, COUNT(*) as count
        FROM t_seat 
        GROUP BY status
        """)
        status_stats = cursor.fetchall()
        
        print(f"座位状态分布:")
        for status, count in status_stats:
            status_name = "维护中" if status == 0 else "可用" if status == 1 else f"未知({status})"
            print(f"  {status_name}: {count} 个")
        
        # 检查锁定状态分布
        cursor.execute("""
        SELECT lock_status, COUNT(*) as count
        FROM t_seat 
        GROUP BY lock_status
        """)
        lock_status_stats = cursor.fetchall()
        
        print(f"锁定状态分布:")
        for lock_status, count in lock_status_stats:
            lock_status_name = "空闲" if lock_status == 0 else "已锁定" if lock_status == 1 else "已占用" if lock_status == 2 else f"未知({lock_status})"
            print(f"  {lock_status_name}: {count} 个")
        
        # 检查删除状态分布
        cursor.execute("""
        SELECT is_deleted, COUNT(*) as count
        FROM t_seat 
        GROUP BY is_deleted
        """)
        deleted_stats = cursor.fetchall()
        
        print(f"删除状态分布:")
        for is_deleted, count in deleted_stats:
            deleted_name = "未删除" if is_deleted == 0 else "已删除"
            print(f"  {deleted_name}: {count} 个")
        
        # 4. 检查锁定条件
        print(f"\n4. 检查锁定条件...")
        
        # 检查可锁定座位
        cursor.execute("""
        SELECT COUNT(*) as count
        FROM t_seat 
        WHERE lock_status = 0 
        AND status = 1 
        AND is_deleted = 0
        """)
        lockable_count = cursor.fetchone()[0]
        print(f"可锁定座位数: {lockable_count}")
        
        # 检查不可锁定的原因
        cursor.execute("""
        SELECT 
            SUM(CASE WHEN lock_status != 0 THEN 1 ELSE 0 END) as locked,
            SUM(CASE WHEN status != 1 THEN 1 ELSE 0 END) as not_available,
            SUM(CASE WHEN is_deleted != 0 THEN 1 ELSE 0 END) as deleted
        FROM t_seat
        """)
        reasons = cursor.fetchone()
        locked, not_available, deleted = reasons
        
        print(f"不可锁定原因:")
        print(f"  已锁定: {locked} 个")
        print(f"  不可用: {not_available} 个")
        print(f"  已删除: {deleted} 个")
        
        # 5. 检查特定座位
        print(f"\n5. 检查特定座位 (ID 31-35)...")
        for seat_id in range(31, 36):
            cursor.execute("""
            SELECT id, status, lock_status, lock_user_id, lock_time, lock_expire_time, is_deleted
            FROM t_seat 
            WHERE id = %s
            """, (seat_id,))
            seat = cursor.fetchone()
            
            if seat:
                seat_id, status, lock_status, lock_user_id, lock_time, lock_expire_time, is_deleted = seat
                can_lock = (lock_status == 0 and status == 1 and is_deleted == 0)
                print(f"  座位 {seat_id}: 状态={status}, 锁定={lock_status}, 用户={lock_user_id}, 可锁定={can_lock}")
            else:
                print(f"  座位 {seat_id}: 不存在")
        
        # 6. 检查是否有过期锁定
        print(f"\n6. 检查过期锁定...")
        cursor.execute("""
        SELECT COUNT(*) as count
        FROM t_seat 
        WHERE lock_status = 1 
        AND lock_expire_time < NOW()
        """)
        expired_count = cursor.fetchone()[0]
        print(f"过期锁定座位数: {expired_count}")
        
        if expired_count > 0:
            print("⚠️ 发现过期锁定座位，建议清理")
            
            # 显示过期锁定的座位
            cursor.execute("""
            SELECT id, lock_user_id, lock_time, lock_expire_time
            FROM t_seat 
            WHERE lock_status = 1 
            AND lock_expire_time < NOW()
            LIMIT 5
            """)
            expired_seats = cursor.fetchall()
            
            print(f"过期锁定座位示例:")
            for seat in expired_seats:
                seat_id, lock_user_id, lock_time, lock_expire_time = seat
                print(f"  座位 {seat_id}: 用户={lock_user_id}, 锁定时间={lock_time}, 过期时间={lock_expire_time}")
        
        # 7. 检查表索引
        print(f"\n7. 检查表索引...")
        cursor.execute("SHOW INDEX FROM t_seat")
        indexes = cursor.fetchall()
        
        print(f"t_seat表索引:")
        for index in indexes:
            print(f"  {index[2]}: {index[4]} ({index[10]})")
        
    except Exception as e:
        print(f"❌ 数据库查询异常: {e}")
    finally:
        if 'connection' in locals():
            connection.close()

if __name__ == "__main__":
    check_table_structure()