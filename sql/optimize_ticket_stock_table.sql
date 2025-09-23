-- 优化 t_ticket_stock 表，添加唯一约束防止重复记录
-- 执行前请确保已清理重复数据

USE ticket_show_db;

-- 1. 添加 ticket_id 唯一约束
-- 注意：执行前必须确保表中没有重复的 ticket_id 记录
ALTER TABLE t_ticket_stock 
ADD CONSTRAINT uk_ticket_stock_ticket_id UNIQUE (ticket_id);

-- 2. 查看约束是否添加成功
SHOW INDEX FROM t_ticket_stock;

-- 3. 验证约束效果（可选）
-- 以下语句应该会失败，因为违反唯一约束
-- INSERT INTO t_ticket_stock (ticket_id, total_stock, locked_stock, sold_stock, version, create_time, update_time) 
-- VALUES (2, 100, 0, 0, 0, NOW(), NOW());

-- 4. 查看表结构确认
DESC t_ticket_stock;

SELECT 
    TABLE_NAME,
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE
FROM 
    INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
WHERE 
    TABLE_SCHEMA = 'ticket_show_db' 
    AND TABLE_NAME = 't_ticket_stock'
    AND CONSTRAINT_TYPE = 'UNIQUE';