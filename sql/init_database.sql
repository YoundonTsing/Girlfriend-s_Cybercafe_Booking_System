-- 抢票系统数据库初始化脚本
-- 基于微服务设计文档创建所有必要的表结构

-- 创建数据库
CREATE DATABASE IF NOT EXISTS ticket_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ticket_system;

-- ========================================
-- 用户服务相关表
-- ========================================

-- 角色表
CREATE TABLE t_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    description VARCHAR(200) COMMENT '角色描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除'
) COMMENT '角色表';

-- 用户表
CREATE TABLE t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    nickname VARCHAR(50) COMMENT '昵称',
    phone VARCHAR(20) NOT NULL UNIQUE COMMENT '手机号',
    email VARCHAR(100) UNIQUE COMMENT '邮箱',
    avatar VARCHAR(255) COMMENT '头像',
    gender TINYINT COMMENT '性别：0-未知，1-男，2-女',
    birthday DATE COMMENT '生日',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_login_time DATETIME COMMENT '最后登录时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除'
) COMMENT '用户表';

-- 用户地址表
CREATE TABLE t_user_address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    receiver_name VARCHAR(50) NOT NULL COMMENT '收件人姓名',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '收件人电话',
    province VARCHAR(20) NOT NULL COMMENT '省份',
    city VARCHAR(20) NOT NULL COMMENT '城市',
    district VARCHAR(20) NOT NULL COMMENT '区县',
    detail_address VARCHAR(200) NOT NULL COMMENT '详细地址',
    is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认地址：0-否，1-是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    FOREIGN KEY (user_id) REFERENCES t_user(id)
) COMMENT '用户地址表';

-- 用户角色关联表
CREATE TABLE t_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES t_user(id),
    FOREIGN KEY (role_id) REFERENCES t_role(id)
) COMMENT '用户角色关联表';

-- ========================================
-- 演出服务相关表
-- ========================================

-- 演出类别表
CREATE TABLE t_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '类别名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父类别ID，0表示顶级类别',
    level TINYINT NOT NULL COMMENT '类别层级',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除'
) COMMENT '演出类别表';

-- 场馆表
CREATE TABLE t_venue (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '场馆名称',
    province VARCHAR(20) NOT NULL COMMENT '省份',
    city VARCHAR(20) NOT NULL COMMENT '城市',
    district VARCHAR(20) NOT NULL COMMENT '区县',
    address VARCHAR(200) NOT NULL COMMENT '详细地址',
    description TEXT COMMENT '场馆描述',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    traffic_info TEXT COMMENT '交通信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除'
) COMMENT '场馆表';

-- 演出表
CREATE TABLE t_show (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL COMMENT '演出标题',
    category_id BIGINT NOT NULL COMMENT '演出类别ID',
    description TEXT COMMENT '演出描述',
    cover_img VARCHAR(255) COMMENT '封面图片',
    detail_imgs TEXT COMMENT '详情图片，JSON格式存储',
    duration INT COMMENT '演出时长(分钟)',
    notice TEXT COMMENT '观演须知',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-下架，1-上架',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    FOREIGN KEY (category_id) REFERENCES t_category(id)
) COMMENT '演出表';

-- 演出场次表
CREATE TABLE t_show_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    show_id BIGINT NOT NULL COMMENT '演出ID',
    venue_id BIGINT NOT NULL COMMENT '场馆ID',
    session_time DATETIME NOT NULL COMMENT '演出时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-已取消，1-未开售，2-售卖中，3-已售罄，4-已结束',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    FOREIGN KEY (show_id) REFERENCES t_show(id),
    FOREIGN KEY (venue_id) REFERENCES t_venue(id)
) COMMENT '演出场次表';

-- 座位区域表
CREATE TABLE t_seat_area (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    venue_id BIGINT NOT NULL COMMENT '场馆ID',
    name VARCHAR(50) NOT NULL COMMENT '区域名称',
    price DECIMAL(10,2) NOT NULL COMMENT '区域价格',
    capacity INT NOT NULL COMMENT '座位容量',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    FOREIGN KEY (venue_id) REFERENCES t_venue(id)
) COMMENT '座位区域表';

-- 座位表
CREATE TABLE t_seat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    area_id BIGINT NOT NULL COMMENT '区域ID',
    row_num VARCHAR(10) NOT NULL COMMENT '排号',
    seat_num VARCHAR(10) NOT NULL COMMENT '座位号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-不可用，1-可用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    FOREIGN KEY (area_id) REFERENCES t_seat_area(id)
) COMMENT '座位表';

-- ========================================
-- 票务服务相关表
-- ========================================

-- 票种表
CREATE TABLE t_ticket_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    show_id BIGINT NOT NULL COMMENT '演出ID',
    session_id BIGINT NOT NULL COMMENT '场次ID',
    area_id BIGINT NOT NULL COMMENT '区域ID',
    name VARCHAR(50) NOT NULL COMMENT '票种名称',
    price DECIMAL(10,2) NOT NULL COMMENT '票价',
    stock INT NOT NULL COMMENT '总库存',
    remaining INT NOT NULL COMMENT '剩余库存',
    limit_per_user INT NOT NULL DEFAULT 4 COMMENT '每人限购数量',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-停售，1-正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    FOREIGN KEY (show_id) REFERENCES t_show(id),
    FOREIGN KEY (session_id) REFERENCES t_show_session(id),
    FOREIGN KEY (area_id) REFERENCES t_seat_area(id)
) COMMENT '票种表';

-- 票务库存表
CREATE TABLE t_ticket_stock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_type_id BIGINT NOT NULL COMMENT '票种ID',
    total_stock INT NOT NULL COMMENT '总库存',
    locked_stock INT NOT NULL DEFAULT 0 COMMENT '锁定库存',
    sold_stock INT NOT NULL DEFAULT 0 COMMENT '已售库存',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (ticket_type_id) REFERENCES t_ticket_type(id)
) COMMENT '票务库存表';

-- 票务锁定记录表
CREATE TABLE t_ticket_lock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    ticket_type_id BIGINT NOT NULL COMMENT '票种ID',
    seat_id BIGINT COMMENT '座位ID',
    quantity INT NOT NULL COMMENT '锁定数量',
    lock_time DATETIME NOT NULL COMMENT '锁定时间',
    expire_time DATETIME NOT NULL COMMENT '过期时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-已释放，1-锁定中，2-已转订单',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES t_user(id),
    FOREIGN KEY (ticket_type_id) REFERENCES t_ticket_type(id),
    FOREIGN KEY (seat_id) REFERENCES t_seat(id)
) COMMENT '票务锁定记录表';

-- ========================================
-- 订单服务相关表
-- ========================================

-- 订单表
CREATE TABLE t_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
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
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    FOREIGN KEY (user_id) REFERENCES t_user(id)
) COMMENT '订单表';

-- 订单明细表
CREATE TABLE t_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    show_id BIGINT NOT NULL COMMENT '演出ID',
    show_title VARCHAR(100) NOT NULL COMMENT '演出标题',
    session_id BIGINT NOT NULL COMMENT '场次ID',
    session_time DATETIME NOT NULL COMMENT '演出时间',
    venue_id BIGINT NOT NULL COMMENT '场馆ID',
    venue_name VARCHAR(100) NOT NULL COMMENT '场馆名称',
    ticket_type_id BIGINT NOT NULL COMMENT '票种ID',
    ticket_type_name VARCHAR(50) NOT NULL COMMENT '票种名称',
    seat_id BIGINT COMMENT '座位ID',
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
-- 支付服务相关表
-- ========================================

-- 支付记录表
CREATE TABLE t_payment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_no VARCHAR(64) NOT NULL UNIQUE COMMENT '支付单号',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    pay_amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    pay_type TINYINT NOT NULL COMMENT '支付方式：1-支付宝，2-微信，3-银联',
    pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待支付，1-支付中，2-支付成功，3-支付失败',
    trade_no VARCHAR(64) COMMENT '第三方支付流水号',
    callback_time DATETIME COMMENT '回调时间',
    callback_content TEXT COMMENT '回调内容',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES t_user(id)
) COMMENT '支付记录表';

-- 退款记录表
CREATE TABLE t_refund (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    refund_no VARCHAR(64) NOT NULL UNIQUE COMMENT '退款单号',
    payment_no VARCHAR(64) NOT NULL COMMENT '支付单号',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    refund_amount DECIMAL(10,2) NOT NULL COMMENT '退款金额',
    refund_reason VARCHAR(200) COMMENT '退款原因',
    refund_status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-申请中，1-退款中，2-退款成功，3-退款失败',
    trade_no VARCHAR(64) COMMENT '第三方退款流水号',
    callback_time DATETIME COMMENT '回调时间',
    callback_content TEXT COMMENT '回调内容',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES t_user(id)
) COMMENT '退款记录表';

-- ========================================
-- 通知服务相关表
-- ========================================

-- 消息模板表
CREATE TABLE t_message_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '模板编码',
    type TINYINT NOT NULL COMMENT '模板类型：1-短信，2-邮件，3-站内信',
    title VARCHAR(100) COMMENT '标题模板',
    content TEXT NOT NULL COMMENT '内容模板',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除'
) COMMENT '消息模板表';

-- 消息记录表
CREATE TABLE t_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type TINYINT NOT NULL COMMENT '消息类型：1-系统消息，2-订单消息，3-活动消息',
    title VARCHAR(100) NOT NULL COMMENT '消息标题',
    content TEXT NOT NULL COMMENT '消息内容',
    is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
    read_time DATETIME COMMENT '阅读时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    FOREIGN KEY (user_id) REFERENCES t_user(id)
) COMMENT '消息记录表';

-- 发送记录表
CREATE TABLE t_send_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT COMMENT '用户ID',
    template_id BIGINT NOT NULL COMMENT '模板ID',
    type TINYINT NOT NULL COMMENT '发送类型：1-短信，2-邮件，3-站内信',
    receiver VARCHAR(100) NOT NULL COMMENT '接收者(手机号/邮箱/用户ID)',
    content TEXT NOT NULL COMMENT '发送内容',
    send_status TINYINT NOT NULL COMMENT '发送状态：0-发送中，1-发送成功，2-发送失败',
    send_time DATETIME NOT NULL COMMENT '发送时间',
    error_msg VARCHAR(200) COMMENT '错误信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (template_id) REFERENCES t_message_template(id)
) COMMENT '发送记录表';

-- ========================================
-- 创建索引
-- ========================================

-- 用户表索引
CREATE INDEX idx_user_phone ON t_user(phone);
CREATE INDEX idx_user_email ON t_user(email);
CREATE INDEX idx_user_status ON t_user(status);

-- 演出表索引
CREATE INDEX idx_show_category ON t_show(category_id);
CREATE INDEX idx_show_status ON t_show(status);
CREATE INDEX idx_show_title ON t_show(title);

-- 场次表索引
CREATE INDEX idx_session_show ON t_show_session(show_id);
CREATE INDEX idx_session_venue ON t_show_session(venue_id);
CREATE INDEX idx_session_time ON t_show_session(session_time);
CREATE INDEX idx_session_status ON t_show_session(status);

-- 票种表索引
CREATE INDEX idx_ticket_type_show ON t_ticket_type(show_id);
CREATE INDEX idx_ticket_type_session ON t_ticket_type(session_id);
CREATE INDEX idx_ticket_type_area ON t_ticket_type(area_id);

-- 订单表索引
CREATE INDEX idx_order_user ON t_order(user_id);
CREATE INDEX idx_order_status ON t_order(status);
CREATE INDEX idx_order_create_time ON t_order(create_time);
CREATE INDEX idx_order_expire_time ON t_order(expire_time);

-- 支付表索引
CREATE INDEX idx_payment_order ON t_payment(order_no);
CREATE INDEX idx_payment_user ON t_payment(user_id);
CREATE INDEX idx_payment_status ON t_payment(pay_status);

-- 消息表索引
CREATE INDEX idx_message_user ON t_message(user_id);
CREATE INDEX idx_message_type ON t_message(type);
CREATE INDEX idx_message_read ON t_message(is_read);

-- ========================================
-- 插入初始数据
-- ========================================

-- 插入默认角色
INSERT INTO t_role (role_name, role_code, description) VALUES
('管理员', 'ADMIN', '系统管理员'),
('普通用户', 'USER', '普通用户');

-- 插入演出类别
INSERT INTO t_category (name, parent_id, level, sort) VALUES
('音乐会', 0, 1, 1),
('话剧', 0, 1, 2),
('舞蹈', 0, 1, 3),
('体育赛事', 0, 1, 4),
('展览', 0, 1, 5),
('古典音乐', 1, 2, 1),
('流行音乐', 1, 2, 2),
('民族音乐', 1, 2, 3),
('原创话剧', 2, 2, 1),
('经典话剧', 2, 2, 2);

-- 插入消息模板
INSERT INTO t_message_template (code, type, title, content) VALUES
('USER_REGISTER', 3, '欢迎注册', '欢迎您注册抢票系统，祝您使用愉快！'),
('ORDER_CREATE', 3, '订单创建成功', '您的订单{orderNo}已创建成功，请在{expireTime}前完成支付。'),
('ORDER_PAID', 3, '支付成功', '您的订单{orderNo}支付成功，感谢您的购买！'),
('ORDER_CANCEL', 3, '订单取消', '您的订单{orderNo}已取消，如有疑问请联系客服。'),
('TICKET_REMIND', 1, '演出提醒', '您购买的{showTitle}将在{sessionTime}开始，请提前到场。');

COMMIT;

-- 数据库初始化完成
SELECT '数据库初始化完成！' AS message;