-- 支付服务数据库初始化脚本
-- 创建支付相关的独立数据库

-- 创建支付数据库
CREATE DATABASE IF NOT EXISTS ticket_payment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ticket_payment_db;

-- ========================================
-- 支付服务相关表
-- ========================================

-- 支付记录表
CREATE TABLE t_payment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_no VARCHAR(64) NOT NULL UNIQUE COMMENT '支付流水号',
    order_id BIGINT NOT NULL COMMENT '订单ID（来自订单服务）',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID（来自用户服务）',
    amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    pay_type TINYINT NOT NULL COMMENT '支付方式：1-支付宝，2-微信，3-银联',
    pay_channel VARCHAR(20) NOT NULL COMMENT '支付渠道',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待支付，1-支付成功，2-支付失败，3-已取消',
    third_party_trade_no VARCHAR(100) COMMENT '第三方交易号',
    pay_time DATETIME COMMENT '支付时间',
    notify_time DATETIME COMMENT '通知时间',
    callback_content TEXT COMMENT '回调内容',
    remark VARCHAR(200) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除'
) COMMENT '支付记录表';

-- 退款记录表
CREATE TABLE t_refund (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    refund_no VARCHAR(64) NOT NULL UNIQUE COMMENT '退款流水号',
    payment_id BIGINT NOT NULL COMMENT '支付记录ID',
    payment_no VARCHAR(64) NOT NULL COMMENT '支付流水号',
    order_id BIGINT NOT NULL COMMENT '订单ID（来自订单服务）',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID（来自用户服务）',
    refund_amount DECIMAL(10,2) NOT NULL COMMENT '退款金额',
    refund_reason VARCHAR(200) COMMENT '退款原因',
    refund_type TINYINT NOT NULL COMMENT '退款类型：1-用户申请，2-系统自动，3-客服处理',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-退款中，1-退款成功，2-退款失败',
    third_party_refund_no VARCHAR(100) COMMENT '第三方退款号',
    refund_time DATETIME COMMENT '退款时间',
    notify_time DATETIME COMMENT '通知时间',
    callback_content TEXT COMMENT '回调内容',
    remark VARCHAR(200) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    FOREIGN KEY (payment_id) REFERENCES t_payment(id)
) COMMENT '退款记录表';

-- ========================================
-- 创建索引
-- ========================================

-- 支付记录表索引
CREATE INDEX idx_payment_order ON t_payment(order_id);
CREATE INDEX idx_payment_order_no ON t_payment(order_no);
CREATE INDEX idx_payment_user ON t_payment(user_id);
CREATE INDEX idx_payment_status ON t_payment(status);
CREATE INDEX idx_payment_type ON t_payment(pay_type);
CREATE INDEX idx_payment_channel ON t_payment(pay_channel);
CREATE INDEX idx_payment_time ON t_payment(pay_time);
CREATE INDEX idx_payment_create_time ON t_payment(create_time);
CREATE INDEX idx_payment_third_party ON t_payment(third_party_trade_no);

-- 退款记录表索引
CREATE INDEX idx_refund_payment ON t_refund(payment_id);
CREATE INDEX idx_refund_payment_no ON t_refund(payment_no);
CREATE INDEX idx_refund_order ON t_refund(order_id);
CREATE INDEX idx_refund_order_no ON t_refund(order_no);
CREATE INDEX idx_refund_user ON t_refund(user_id);
CREATE INDEX idx_refund_status ON t_refund(status);
CREATE INDEX idx_refund_type ON t_refund(refund_type);
CREATE INDEX idx_refund_time ON t_refund(refund_time);
CREATE INDEX idx_refund_create_time ON t_refund(create_time);
CREATE INDEX idx_refund_third_party ON t_refund(third_party_refund_no);

-- ========================================
-- 插入测试数据（可选）
-- ========================================

-- 插入示例支付记录
INSERT INTO t_payment (payment_no, order_id, order_no, user_id, amount, pay_type, pay_channel, status, third_party_trade_no, pay_time, notify_time, remark) VALUES
('PAY202412010001', 2, 'ORD202412010002', 3, 760.00, 1, 'alipay', 1, '2024120122001234567890123456', '2024-12-01 10:15:30', '2024-12-01 10:15:35', '雷雨话剧支付'),
('PAY202412010002', 3, 'ORD202412010003', 4, 2560.00, 2, 'wechat', 1, '4200001234202412011234567890', '2024-12-01 14:20:15', '2024-12-01 14:20:20', 'NBA中国赛支付'),
('PAY202412010003', 1, 'ORD202412010001', 2, 1360.00, 1, 'alipay', 0, NULL, NULL, NULL, '新年音乐会支付（待支付）');

-- 插入示例退款记录
INSERT INTO t_refund (refund_no, payment_id, payment_no, order_id, order_no, user_id, refund_amount, refund_reason, refund_type, status, third_party_refund_no, refund_time, notify_time, remark) VALUES
('REF202412010001', 1, 'PAY202412010001', 2, 'ORD202412010002', 3, 380.00, '用户申请部分退款', 1, 1, 'RF2024120110001234567890', '2024-12-01 16:30:00', '2024-12-01 16:30:05', '部分退款成功');

COMMIT;

-- 支付数据库初始化完成
SELECT '支付数据库初始化完成！' AS message;