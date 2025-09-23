-- 在演出数据库中添加票档相关表
USE ticket_show_db;

-- 票档表
CREATE TABLE t_ticket (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    show_id BIGINT NOT NULL COMMENT '演出ID',
    session_id BIGINT NOT NULL COMMENT '场次ID',
    name VARCHAR(50) NOT NULL COMMENT '票档名称',
    price DECIMAL(10,2) NOT NULL COMMENT '票价',
    total_count INT NOT NULL COMMENT '总票数',
    remain_count INT NOT NULL COMMENT '剩余票数',
    limit_count INT NOT NULL DEFAULT 4 COMMENT '限购数量',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '票档状态：0-未开售，1-售票中，2-已售罄',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除'
) COMMENT '票档表';

-- 票档库存表
CREATE TABLE t_ticket_stock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL COMMENT '票档ID',
    total_stock INT NOT NULL COMMENT '总库存',
    locked_stock INT NOT NULL DEFAULT 0 COMMENT '锁定库存',
    sold_stock INT NOT NULL DEFAULT 0 COMMENT '已售库存',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (ticket_id) REFERENCES t_ticket(id)
) COMMENT '票档库存表';

-- 票档锁定记录表
CREATE TABLE t_ticket_lock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID（来自用户服务）',
    ticket_id BIGINT NOT NULL COMMENT '票档ID',
    seat_id BIGINT COMMENT '座位ID（来自演出服务）',
    quantity INT NOT NULL COMMENT '锁定数量',
    lock_time DATETIME NOT NULL COMMENT '锁定时间',
    expire_time DATETIME NOT NULL COMMENT '过期时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-已释放，1-锁定中，2-已转订单',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (ticket_id) REFERENCES t_ticket(id)
) COMMENT '票档锁定记录表';

-- 创建索引
CREATE INDEX idx_ticket_show ON t_ticket(show_id);
CREATE INDEX idx_ticket_session ON t_ticket(session_id);
CREATE INDEX idx_ticket_status ON t_ticket(status);
CREATE INDEX idx_ticket_price ON t_ticket(price);

CREATE INDEX idx_ticket_stock_ticket ON t_ticket_stock(ticket_id);
CREATE INDEX idx_ticket_stock_version ON t_ticket_stock(version);

CREATE INDEX idx_ticket_lock_user ON t_ticket_lock(user_id);
CREATE INDEX idx_ticket_lock_ticket ON t_ticket_lock(ticket_id);
CREATE INDEX idx_ticket_lock_seat ON t_ticket_lock(seat_id);
CREATE INDEX idx_ticket_lock_status ON t_ticket_lock(status);
CREATE INDEX idx_ticket_lock_expire ON t_ticket_lock(expire_time);
CREATE INDEX idx_ticket_lock_time ON t_ticket_lock(lock_time);

-- 插入测试票档数据
-- 新年音乐会票档（12月31日场次）
INSERT INTO t_ticket (show_id, session_id, name, price, total_count, remain_count, limit_count) VALUES
(1, 1, '池座', 680.00, 800, 800, 4),
(1, 1, '一楼包厢', 980.00, 200, 200, 2),
(1, 1, '二楼', 480.00, 600, 600, 4),
(1, 1, '三楼', 280.00, 400, 400, 6);

-- 新年音乐会票档（1月1日场次）
INSERT INTO t_ticket (show_id, session_id, name, price, total_count, remain_count, limit_count) VALUES
(1, 2, '池座', 680.00, 800, 800, 4),
(1, 2, '一楼包厢', 980.00, 200, 200, 2),
(1, 2, '二楼', 480.00, 600, 600, 4),
(1, 2, '三楼', 280.00, 400, 400, 6);

-- 雷雨话剧票档（12月20日场次）
INSERT INTO t_ticket (show_id, session_id, name, price, total_count, remain_count, limit_count) VALUES
(2, 3, '池座', 380.00, 800, 800, 4),
(2, 3, '一楼包厢', 580.00, 200, 200, 2),
(2, 3, '二楼', 280.00, 600, 600, 4),
(2, 3, '三楼', 180.00, 400, 400, 6);

-- NBA中国赛票档
INSERT INTO t_ticket (show_id, session_id, name, price, total_count, remain_count, limit_count) VALUES
(4, 8, 'VIP区', 1280.00, 500, 500, 2),
(4, 8, '一等座', 880.00, 2000, 2000, 4),
(4, 8, '二等座', 580.00, 5000, 5000, 6),
(4, 8, '三等座', 280.00, 10000, 10000, 8);

-- 为每个票档创建对应的库存记录（使用INSERT IGNORE防止重复插入）
INSERT IGNORE INTO t_ticket_stock (ticket_id, total_stock, locked_stock, sold_stock, version)
SELECT id, total_count, 0, 0, 0 FROM t_ticket;

COMMIT;

SELECT '票档表添加完成！' AS message;