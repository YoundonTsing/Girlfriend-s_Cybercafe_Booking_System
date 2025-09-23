-- 用户服务数据库初始化脚本
-- 创建用户相关的独立数据库

-- 创建用户数据库
CREATE DATABASE IF NOT EXISTS ticket_user_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ticket_user_db;

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
-- 创建索引
-- ========================================

-- 用户表索引
CREATE INDEX idx_user_phone ON t_user(phone);
CREATE INDEX idx_user_email ON t_user(email);
CREATE INDEX idx_user_status ON t_user(status);
CREATE INDEX idx_user_create_time ON t_user(create_time);

-- 用户地址表索引
CREATE INDEX idx_user_address_user_id ON t_user_address(user_id);
CREATE INDEX idx_user_address_default ON t_user_address(is_default);

-- 用户角色关联表索引
CREATE INDEX idx_user_role_user_id ON t_user_role(user_id);
CREATE INDEX idx_user_role_role_id ON t_user_role(role_id);

-- ========================================
-- 插入初始数据
-- ========================================

-- 插入默认角色
INSERT INTO t_role (role_name, role_code, description) VALUES
('管理员', 'ADMIN', '系统管理员'),
('普通用户', 'USER', '普通用户');

-- 插入测试用户（密码为123456的MD5值）
INSERT INTO t_user (username, password, nickname, phone, email, gender, status) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', '系统管理员', '13800138000', 'admin@example.com', 1, 1),
('testuser1', 'e10adc3949ba59abbe56e057f20f883e', '测试用户1', '13800138001', 'user1@example.com', 1, 1),
('testuser2', 'e10adc3949ba59abbe56e057f20f883e', '测试用户2', '13800138002', 'user2@example.com', 2, 1),
('testuser3', 'e10adc3949ba59abbe56e057f20f883e', '测试用户3', '13800138003', 'user3@example.com', 1, 1);

-- 分配用户角色
INSERT INTO t_user_role (user_id, role_id) VALUES
(1, 1), -- admin为管理员
(2, 2), -- testuser1为普通用户
(3, 2), -- testuser2为普通用户
(4, 2); -- testuser3为普通用户

COMMIT;

-- 用户数据库初始化完成
SELECT '用户数据库初始化完成！' AS message;