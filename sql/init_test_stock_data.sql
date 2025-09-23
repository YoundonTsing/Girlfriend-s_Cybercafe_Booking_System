-- 初始化票档库存测试数据
-- 用于库存监控看板和并发测试

USE ticket_show;

-- 清理现有测试数据（可选）
-- DELETE FROM t_ticket_stock WHERE ticket_id IN (SELECT id FROM t_ticket WHERE name LIKE '测试%');
-- DELETE FROM t_ticket WHERE name LIKE '测试%';

-- 插入测试票档数据
INSERT INTO t_ticket (name, price, show_id, session_id, status, create_time, update_time) VALUES
('测试VIP座位', 299.00, 1, 1, 1, NOW(), NOW()),
('测试普通座位', 199.00, 1, 1, 1, NOW(), NOW()),
('测试学生票', 99.00, 1, 1, 1, NOW(), NOW()),
('测试情侣座', 399.00, 1, 1, 1, NOW(), NOW()),
('测试包厢座位', 599.00, 1, 1, 1, NOW(), NOW());

-- 获取刚插入的票档ID并初始化库存
-- 注意：这里使用变量来获取票档ID，实际执行时可能需要手动替换ID

-- 为测试VIP座位初始化库存（使用INSERT IGNORE防止重复插入）
INSERT IGNORE INTO t_ticket_stock (ticket_id, total_stock, available_stock, locked_stock, sold_stock, version, create_time, update_time)
SELECT id, 100, 100, 0, 0, 0, NOW(), NOW()
FROM t_ticket 
WHERE name = '测试VIP座位' AND show_id = 1 AND session_id = 1;

-- 为测试普通座位初始化库存（使用INSERT IGNORE防止重复插入）
INSERT IGNORE INTO t_ticket_stock (ticket_id, total_stock, available_stock, locked_stock, sold_stock, version, create_time, update_time)
SELECT id, 200, 180, 15, 5, 0, NOW(), NOW()
FROM t_ticket 
WHERE name = '测试普通座位' AND show_id = 1 AND session_id = 1;

-- 为测试学生票初始化库存（使用INSERT IGNORE防止重复插入）
INSERT IGNORE INTO t_ticket_stock (ticket_id, total_stock, available_stock, locked_stock, sold_stock, version, create_time, update_time)
SELECT id, 50, 30, 10, 10, 0, NOW(), NOW()
FROM t_ticket 
WHERE name = '测试学生票' AND show_id = 1 AND session_id = 1;

-- 为测试情侣座初始化库存（使用INSERT IGNORE防止重复插入）
INSERT IGNORE INTO t_ticket_stock (ticket_id, total_stock, available_stock, locked_stock, sold_stock, version, create_time, update_time)
SELECT id, 20, 5, 2, 13, 0, NOW(), NOW()
FROM t_ticket 
WHERE name = '测试情侣座' AND show_id = 1 AND session_id = 1;

-- 为测试包厢座位初始化库存（使用INSERT IGNORE防止重复插入）
INSERT IGNORE INTO t_ticket_stock (ticket_id, total_stock, available_stock, locked_stock, sold_stock, version, create_time, update_time)
SELECT id, 10, 0, 0, 10, 0, NOW(), NOW()
FROM t_ticket 
WHERE name = '测试包厢座位' AND show_id = 1 AND session_id = 1;

-- 查询验证数据
SELECT 
    t.id as ticket_id,
    t.name as ticket_name,
    t.price,
    ts.total_stock,
    ts.available_stock,
    ts.locked_stock,
    ts.sold_stock,
    ts.version
FROM t_ticket t
LEFT JOIN t_ticket_stock ts ON t.id = ts.ticket_id
WHERE t.name LIKE '测试%'
ORDER BY t.price;

-- 显示库存统计信息
SELECT 
    COUNT(*) as total_tickets,
    SUM(ts.total_stock) as total_stock,
    SUM(ts.available_stock) as available_stock,
    SUM(ts.locked_stock) as locked_stock,
    SUM(ts.sold_stock) as sold_stock
FROM t_ticket t
LEFT JOIN t_ticket_stock ts ON t.id = ts.ticket_id
WHERE t.name LIKE '测试%';