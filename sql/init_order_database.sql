-- 订单服务数据库初始化脚本
-- 创建订单相关的独立数据库

-- 创建订单数据库
CREATE DATABASE IF NOT EXISTS ticket_order_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ticket_order_db;

-- ========================================
-- 订单服务相关表
-- ========================================

-- 订单表
CREATE TABLE t_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID（来自用户服务）',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    pay_amount DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '优惠金额',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待支付，1-已支付，2-已取消，3-已完成，4-已退款',
    pay_type TINYINT COMMENT '支付方式：1-支付宝，2-微信，3-银联',
    pay_time DATETIME COMMENT '支付时间',
    expire_time DATETIME NOT NULL COMMENT '订单过期时间',
    remark VARCHAR(200) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除'
) COMMENT '订单表';

-- 订单明细表
CREATE TABLE t_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    show_id BIGINT NOT NULL COMMENT '演出ID（来自演出服务）',
    show_title VARCHAR(100) NOT NULL COMMENT '演出标题',
    session_id BIGINT NOT NULL COMMENT '场次ID（来自演出服务）',
    session_time DATETIME NOT NULL COMMENT '演出时间',
    venue_id BIGINT NOT NULL COMMENT '场馆ID（来自演出服务）',
    venue_name VARCHAR(100) NOT NULL COMMENT '场馆名称',
    ticket_type_id BIGINT NOT NULL COMMENT '票种ID（来自票务服务）',
    ticket_type_name VARCHAR(50) NOT NULL COMMENT '票种名称',
    seat_id BIGINT COMMENT '座位ID（来自演出服务）',
    seat_info VARCHAR(50) COMMENT '座位信息',
    price DECIMAL(10,2) NOT NULL COMMENT '单价',
    quantity INT NOT NULL COMMENT '数量',
    subtotal DECIMAL(10,2) NOT NULL COMMENT '小计',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (order_id) REFERENCES t_order(id)
) COMMENT '订单明细表';

-- 订单状态流转表
CREATE TABLE t_order_status_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    previous_status TINYINT NOT NULL COMMENT '之前状态',
    current_status TINYINT NOT NULL COMMENT '当前状态',
    operator VARCHAR(50) COMMENT '操作人',
    remark VARCHAR(200) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (order_id) REFERENCES t_order(id)
) COMMENT '订单状态流转表';

-- ========================================
-- 创建索引
-- ========================================

-- 订单表索引
CREATE INDEX idx_order_user ON t_order(user_id);
CREATE INDEX idx_order_status ON t_order(status);
CREATE INDEX idx_order_create_time ON t_order(create_time);
CREATE INDEX idx_order_expire_time ON t_order(expire_time);
CREATE INDEX idx_order_pay_time ON t_order(pay_time);
CREATE INDEX idx_order_no ON t_order(order_no);

-- 订单明细表索引
CREATE INDEX idx_order_item_order ON t_order_item(order_id);
CREATE INDEX idx_order_item_order_no ON t_order_item(order_no);
CREATE INDEX idx_order_item_show ON t_order_item(show_id);
CREATE INDEX idx_order_item_session ON t_order_item(session_id);
CREATE INDEX idx_order_item_ticket_type ON t_order_item(ticket_type_id);
CREATE INDEX idx_order_item_venue ON t_order_item(venue_id);

-- 订单状态流转表索引
CREATE INDEX idx_order_status_log_order ON t_order_status_log(order_id);
CREATE INDEX idx_order_status_log_order_no ON t_order_status_log(order_no);
CREATE INDEX idx_order_status_log_status ON t_order_status_log(current_status);
CREATE INDEX idx_order_status_log_time ON t_order_status_log(create_time);

-- ========================================
-- 插入测试数据（可选）
-- ========================================

-- 插入示例订单数据
INSERT INTO t_order (order_no, user_id, total_amount, pay_amount, discount_amount, status, expire_time, remark) VALUES
('ORD202412010001', 2, 1360.00, 1360.00, 0.00, 0, DATE_ADD(NOW(), INTERVAL 30 MINUTE), '新年音乐会订单'),
('ORD202412010002', 3, 760.00, 760.00, 0.00, 1, DATE_ADD(NOW(), INTERVAL 30 MINUTE), '雷雨话剧订单'),
('ORD202412010003', 4, 2560.00, 2560.00, 0.00, 1, DATE_ADD(NOW(), INTERVAL 30 MINUTE), 'NBA中国赛订单');

-- 插入订单明细数据
INSERT INTO t_order_item (order_id, order_no, show_id, show_title, session_id, session_time, venue_id, venue_name, ticket_type_id, ticket_type_name, seat_id, seat_info, price, quantity, subtotal) VALUES
-- 订单1明细
(1, 'ORD202412010001', 1, '2024新年音乐会', 1, '2024-12-31 19:30:00', 2, '上海大剧院', 1, '池座', 1, 'A排1座', 680.00, 2, 1360.00),
-- 订单2明细
(2, 'ORD202412010002', 2, '《雷雨》话剧', 3, '2024-12-20 19:30:00', 2, '上海大剧院', 5, '池座', 5, 'B排3座', 380.00, 2, 760.00),
-- 订单3明细
(3, 'ORD202412010003', 4, 'NBA中国赛', 8, '2024-12-28 19:30:00', 1, '北京国家体育馆', 13, 'VIP区', NULL, 'VIP区', 1280.00, 2, 2560.00);

-- 插入订单状态流转记录
INSERT INTO t_order_status_log (order_id, order_no, previous_status, current_status, operator, remark) VALUES
(1, 'ORD202412010001', -1, 0, 'system', '订单创建'),
(2, 'ORD202412010002', -1, 0, 'system', '订单创建'),
(2, 'ORD202412010002', 0, 1, 'system', '支付成功'),
(3, 'ORD202412010003', -1, 0, 'system', '订单创建'),
(3, 'ORD202412010003', 0, 1, 'system', '支付成功');

COMMIT;

-- 订单数据库初始化完成
SELECT '订单数据库初始化完成！' AS message;