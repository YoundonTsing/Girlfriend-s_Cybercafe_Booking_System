-- 通知服务数据库初始化脚本
-- 创建通知相关的独立数据库

-- 创建通知数据库
CREATE DATABASE IF NOT EXISTS ticket_message_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ticket_message_db;

-- ========================================
-- 通知服务相关表
-- ========================================

-- 消息模板表
CREATE TABLE t_message_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_code VARCHAR(50) NOT NULL UNIQUE COMMENT '模板编码',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    template_type TINYINT NOT NULL COMMENT '模板类型：1-短信，2-邮件，3-站内信，4-推送',
    title VARCHAR(200) COMMENT '消息标题（邮件、站内信、推送使用）',
    content TEXT NOT NULL COMMENT '消息内容模板',
    variables VARCHAR(500) COMMENT '模板变量说明（JSON格式）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    remark VARCHAR(200) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除'
) COMMENT '消息模板表';

-- 消息记录表
CREATE TABLE t_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id VARCHAR(64) NOT NULL UNIQUE COMMENT '消息ID',
    template_id BIGINT NOT NULL COMMENT '模板ID',
    template_code VARCHAR(50) NOT NULL COMMENT '模板编码',
    user_id BIGINT NOT NULL COMMENT '接收用户ID（来自用户服务）',
    message_type TINYINT NOT NULL COMMENT '消息类型：1-短信，2-邮件，3-站内信，4-推送',
    title VARCHAR(200) COMMENT '消息标题',
    content TEXT NOT NULL COMMENT '消息内容',
    receiver VARCHAR(100) NOT NULL COMMENT '接收方（手机号、邮箱等）',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待发送，1-发送成功，2-发送失败',
    send_time DATETIME COMMENT '发送时间',
    read_time DATETIME COMMENT '阅读时间（站内信使用）',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    error_msg VARCHAR(500) COMMENT '错误信息',
    business_type VARCHAR(50) COMMENT '业务类型（订单、支付、退款等）',
    business_id VARCHAR(100) COMMENT '业务ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    FOREIGN KEY (template_id) REFERENCES t_message_template(id)
) COMMENT '消息记录表';

-- 发送记录表
CREATE TABLE t_send_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id BIGINT NOT NULL COMMENT '消息ID',
    send_channel VARCHAR(50) NOT NULL COMMENT '发送渠道（阿里云、腾讯云等）',
    channel_message_id VARCHAR(100) COMMENT '渠道消息ID',
    send_status TINYINT NOT NULL COMMENT '发送状态：0-发送中，1-发送成功，2-发送失败',
    send_time DATETIME NOT NULL COMMENT '发送时间',
    response_code VARCHAR(20) COMMENT '响应码',
    response_msg VARCHAR(500) COMMENT '响应消息',
    cost_time INT COMMENT '耗时（毫秒）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (message_id) REFERENCES t_message(id)
) COMMENT '发送记录表';

-- ========================================
-- 创建索引
-- ========================================

-- 消息模板表索引
CREATE INDEX idx_template_code ON t_message_template(template_code);
CREATE INDEX idx_template_type ON t_message_template(template_type);
CREATE INDEX idx_template_status ON t_message_template(status);

-- 消息记录表索引
CREATE INDEX idx_message_template ON t_message(template_id);
CREATE INDEX idx_message_template_code ON t_message(template_code);
CREATE INDEX idx_message_user ON t_message(user_id);
CREATE INDEX idx_message_type ON t_message(message_type);
CREATE INDEX idx_message_status ON t_message(status);
CREATE INDEX idx_message_receiver ON t_message(receiver);
CREATE INDEX idx_message_send_time ON t_message(send_time);
CREATE INDEX idx_message_create_time ON t_message(create_time);
CREATE INDEX idx_message_business ON t_message(business_type, business_id);

-- 发送记录表索引
CREATE INDEX idx_send_record_message ON t_send_record(message_id);
CREATE INDEX idx_send_record_channel ON t_send_record(send_channel);
CREATE INDEX idx_send_record_status ON t_send_record(send_status);
CREATE INDEX idx_send_record_time ON t_send_record(send_time);
CREATE INDEX idx_send_record_channel_id ON t_send_record(channel_message_id);

-- ========================================
-- 插入初始数据
-- ========================================

-- 插入消息模板数据
INSERT INTO t_message_template (template_code, template_name, template_type, title, content, variables, remark) VALUES
-- 短信模板
('SMS_ORDER_CREATE', '订单创建通知', 1, NULL, '您的订单${orderNo}已创建成功，请在${expireTime}前完成支付。', '{"orderNo":"订单号","expireTime":"过期时间"}', '订单创建短信通知'),
('SMS_PAYMENT_SUCCESS', '支付成功通知', 1, NULL, '您的订单${orderNo}支付成功，金额${amount}元，感谢您的购买！', '{"orderNo":"订单号","amount":"支付金额"}', '支付成功短信通知'),
('SMS_REFUND_SUCCESS', '退款成功通知', 1, NULL, '您的订单${orderNo}退款成功，退款金额${refundAmount}元，预计3-5个工作日到账。', '{"orderNo":"订单号","refundAmount":"退款金额"}', '退款成功短信通知'),

-- 邮件模板
('EMAIL_ORDER_CREATE', '订单创建确认', 2, '【票务系统】订单创建成功', '尊敬的用户，您好！\n\n您的订单${orderNo}已创建成功：\n演出：${showTitle}\n时间：${showTime}\n场馆：${venueName}\n金额：${amount}元\n\n请在${expireTime}前完成支付，逾期订单将自动取消。\n\n感谢您的使用！', '{"orderNo":"订单号","showTitle":"演出标题","showTime":"演出时间","venueName":"场馆名称","amount":"订单金额","expireTime":"过期时间"}', '订单创建邮件通知'),
('EMAIL_PAYMENT_SUCCESS', '支付成功确认', 2, '【票务系统】支付成功', '尊敬的用户，您好！\n\n您的订单${orderNo}支付成功：\n支付金额：${amount}元\n支付时间：${payTime}\n\n您可以在个人中心查看电子票，演出当天请携带有效证件入场。\n\n感谢您的购买！', '{"orderNo":"订单号","amount":"支付金额","payTime":"支付时间"}', '支付成功邮件通知'),

-- 站内信模板
('SITE_ORDER_CREATE', '订单创建通知', 3, '订单创建成功', '您的订单${orderNo}已创建成功，请及时完成支付。', '{"orderNo":"订单号"}', '订单创建站内信'),
('SITE_PAYMENT_SUCCESS', '支付成功通知', 3, '支付成功', '恭喜您！订单${orderNo}支付成功，金额${amount}元。', '{"orderNo":"订单号","amount":"支付金额"}', '支付成功站内信'),
('SITE_SHOW_REMINDER', '演出提醒', 3, '演出提醒', '您购买的${showTitle}将在${showTime}开始，请提前到达${venueName}。', '{"showTitle":"演出标题","showTime":"演出时间","venueName":"场馆名称"}', '演出提醒站内信'),

-- 推送模板
('PUSH_ORDER_CREATE', '订单创建推送', 4, '订单创建成功', '您的订单已创建，请及时支付', '{"orderNo":"订单号"}', '订单创建推送通知'),
('PUSH_PAYMENT_SUCCESS', '支付成功推送', 4, '支付成功', '订单支付成功，感谢购买！', '{"orderNo":"订单号"}', '支付成功推送通知');

-- 插入示例消息记录
INSERT INTO t_message (message_id, template_id, template_code, user_id, message_type, title, content, receiver, status, send_time, business_type, business_id) VALUES
('MSG202412010001', 2, 'SMS_PAYMENT_SUCCESS', 3, 1, NULL, '您的订单ORD202412010002支付成功，金额760.00元，感谢您的购买！', '13800138002', 1, '2024-12-01 10:15:40', 'ORDER', 'ORD202412010002'),
('MSG202412010002', 5, 'EMAIL_PAYMENT_SUCCESS', 3, 2, '【票务系统】支付成功', '尊敬的用户，您好！\n\n您的订单ORD202412010002支付成功：\n支付金额：760.00元\n支付时间：2024-12-01 10:15:30\n\n您可以在个人中心查看电子票，演出当天请携带有效证件入场。\n\n感谢您的购买！', 'user2@example.com', 1, '2024-12-01 10:15:45', 'ORDER', 'ORD202412010002'),
('MSG202412010003', 8, 'SITE_PAYMENT_SUCCESS', 3, 3, '支付成功', '恭喜您！订单ORD202412010002支付成功，金额760.00元。', '3', 0, NULL, 'ORDER', 'ORD202412010002');

-- 插入发送记录
INSERT INTO t_send_record (message_id, send_channel, channel_message_id, send_status, send_time, response_code, response_msg, cost_time) VALUES
(1, 'aliyun_sms', 'SMS_123456789', 1, '2024-12-01 10:15:40', '200', '发送成功', 150),
(2, 'smtp_email', 'EMAIL_987654321', 1, '2024-12-01 10:15:45', '200', '发送成功', 300);

COMMIT;

-- 通知数据库初始化完成
SELECT '通知数据库初始化完成！' AS message;