-- 演出服务数据库初始化脚本
-- 创建演出相关的独立数据库

-- 创建演出数据库
CREATE DATABASE IF NOT EXISTS ticket_show_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ticket_show_db;

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
    name VARCHAR(100) NOT NULL COMMENT '演出名称',
    type INTEGER NOT NULL COMMENT '演出类型：1-演唱会，2-话剧，3-音乐会，4-展览，5-体育赛事',
    poster_url VARCHAR(255) COMMENT '演出海报图片URL',
    description TEXT COMMENT '演出详情描述',
    venue VARCHAR(100) COMMENT '演出地点',
    city VARCHAR(50) COMMENT '演出城市',
    start_time DATETIME COMMENT '演出开始时间',
    end_time DATETIME COMMENT '演出结束时间',
    min_price DECIMAL(10,2) COMMENT '最低票价',
    max_price DECIMAL(10,2) COMMENT '最高票价',
    status INTEGER NOT NULL DEFAULT 0 COMMENT '演出状态：0-未开售，1-售票中，2-已售罄，3-已结束',
    is_hot INTEGER NOT NULL DEFAULT 0 COMMENT '是否热门：0-否，1-是',
    is_recommend INTEGER NOT NULL DEFAULT 0 COMMENT '是否推荐：0-否，1-是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted INTEGER NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除'
) COMMENT '演出表';

-- 演出场次表
CREATE TABLE t_show_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    show_id BIGINT NOT NULL COMMENT '关联的演出ID',
    name VARCHAR(100) COMMENT '场次名称',
    start_time DATETIME COMMENT '场次开始时间',
    end_time DATETIME COMMENT '场次结束时间',
    status INTEGER NOT NULL DEFAULT 0 COMMENT '场次状态：0-未开售，1-售票中，2-已售罄，3-已结束',
    total_seats INTEGER COMMENT '总座位数',
    sold_seats INTEGER DEFAULT 0 COMMENT '已售座位数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted INTEGER NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    FOREIGN KEY (show_id) REFERENCES t_show(id)
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
-- 创建索引
-- ========================================

-- 演出表索引
CREATE INDEX idx_show_type ON t_show(type);
CREATE INDEX idx_show_status ON t_show(status);
CREATE INDEX idx_show_name ON t_show(name);
CREATE INDEX idx_show_city ON t_show(city);
CREATE INDEX idx_show_start_time ON t_show(start_time);
CREATE INDEX idx_show_is_hot ON t_show(is_hot);
CREATE INDEX idx_show_is_recommend ON t_show(is_recommend);
CREATE INDEX idx_show_create_time ON t_show(create_time);

-- 场次表索引
CREATE INDEX idx_session_show ON t_show_session(show_id);
CREATE INDEX idx_session_start_time ON t_show_session(start_time);
CREATE INDEX idx_session_status ON t_show_session(status);

-- 场馆表索引
CREATE INDEX idx_venue_city ON t_venue(city);
CREATE INDEX idx_venue_name ON t_venue(name);

-- 座位区域表索引
CREATE INDEX idx_seat_area_venue ON t_seat_area(venue_id);
CREATE INDEX idx_seat_area_price ON t_seat_area(price);

-- 座位表索引
CREATE INDEX idx_seat_area ON t_seat(area_id);
CREATE INDEX idx_seat_row_num ON t_seat(row_num);
CREATE INDEX idx_seat_status ON t_seat(status);

-- 类别表索引
CREATE INDEX idx_category_parent ON t_category(parent_id);
CREATE INDEX idx_category_level ON t_category(level);

-- ========================================
-- 插入初始数据
-- ========================================

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

-- 插入场馆数据
INSERT INTO t_venue (name, province, city, district, address, description, contact_phone, traffic_info) VALUES
('北京国家体育馆', '北京市', '北京市', '朝阳区', '奥林匹克公园15号', '2008年奥运会主场馆，可容纳8万人', '010-12345678', '地铁8号线奥体中心站'),
('上海大剧院', '上海市', '上海市', '黄浦区', '人民大道300号', '上海标志性文化建筑', '021-12345678', '地铁1、2、8号线人民广场站'),
('广州体育馆', '广东省', '广州市', '天河区', '天河路299号', '广州市大型综合体育场馆', '020-12345678', '地铁1号线体育中心站'),
('深圳音乐厅', '广东省', '深圳市', '福田区', '福中一路2016号', '深圳市标志性音乐场所', '0755-12345678', '地铁2、3号线福田站');

-- 插入座位区域数据
-- 北京国家体育馆座位区域
INSERT INTO t_seat_area (venue_id, name, price, capacity) VALUES
(1, 'VIP区', 1280.00, 500),
(1, '一等座', 880.00, 2000),
(1, '二等座', 580.00, 5000),
(1, '三等座', 280.00, 10000);

-- 上海大剧院座位区域
INSERT INTO t_seat_area (venue_id, name, price, capacity) VALUES
(2, '池座', 680.00, 800),
(2, '一楼包厢', 980.00, 200),
(2, '二楼', 480.00, 600),
(2, '三楼', 280.00, 400);

-- 广州体育馆座位区域
INSERT INTO t_seat_area (venue_id, name, price, capacity) VALUES
(3, 'VIP区', 888.00, 300),
(3, '内场', 588.00, 1500),
(3, '看台A区', 388.00, 3000),
(3, '看台B区', 188.00, 5000);

-- 深圳音乐厅座位区域
INSERT INTO t_seat_area (venue_id, name, price, capacity) VALUES
(4, '一楼正厅', 580.00, 600),
(4, '一楼包厢', 780.00, 100),
(4, '二楼正厅', 380.00, 400),
(4, '二楼包厢', 580.00, 80);

-- 插入演出数据
INSERT INTO t_show (name, type, poster_url, description, venue, city, start_time, end_time, min_price, max_price, status, is_hot, is_recommend) VALUES
('2024新年音乐会', 3, '/images/show1.jpg', '维也纳爱乐乐团2024新年音乐会，经典曲目演奏', '上海大剧院', '上海市', '2024-12-31 19:30:00', '2024-12-31 21:30:00', 280.00, 680.00, 1, 1, 1),
('《雷雨》话剧', 2, '/images/show2.jpg', '曹禺经典话剧作品，著名演员主演', '上海大剧院', '上海市', '2024-12-20 19:30:00', '2024-12-22 21:00:00', 280.00, 680.00, 1, 0, 1),
('天鹅湖芭蕾舞', 3, '/images/show3.jpg', '俄罗斯国家芭蕾舞团经典演出', '上海大剧院', '上海市', '2024-12-25 19:30:00', '2024-12-26 21:50:00', 280.00, 780.00, 1, 1, 0),
('NBA中国赛', 5, '/images/show4.jpg', '洛杉矶湖人队 VS 金州勇士队', '北京国家体育馆', '北京市', '2024-12-28 19:30:00', '2024-12-28 22:30:00', 280.00, 1280.00, 1, 1, 1),
('毕加索画展', 4, '/images/show5.jpg', '毕加索作品回顾展，珍贵原作展出', '上海大剧院', '上海市', '2024-12-01 09:00:00', '2025-02-28 18:00:00', 50.00, 150.00, 1, 0, 0);

-- 插入演出场次数据
INSERT INTO t_show_session (show_id, name, start_time, end_time, status, total_seats, sold_seats) VALUES
-- 新年音乐会场次
(1, '除夕夜场', '2024-12-31 19:30:00', '2024-12-31 21:30:00', 1, 2000, 1200),
(1, '元旦日场', '2025-01-01 15:00:00', '2025-01-01 17:00:00', 1, 2000, 800),
-- 雷雨话剧场次
(2, '首演场', '2024-12-20 19:30:00', '2024-12-20 22:00:00', 1, 2000, 1500),
(2, '第二场', '2024-12-21 19:30:00', '2024-12-21 22:00:00', 1, 2000, 1800),
(2, '周末日场', '2024-12-22 14:30:00', '2024-12-22 17:00:00', 1, 2000, 1000),
-- 天鹅湖芭蕾舞场次
(3, '圣诞夜场', '2024-12-25 19:30:00', '2024-12-25 21:50:00', 1, 2000, 1600),
(3, '深圳专场', '2024-12-26 19:30:00', '2024-12-26 21:50:00', 1, 1180, 900),
-- NBA中国赛场次
(4, '湖人VS勇士', '2024-12-28 19:30:00', '2024-12-28 22:30:00', 1, 17500, 15000),
-- 毕加索画展（展览类，时间跨度较长）
(5, '毕加索回顾展', '2024-12-01 09:00:00', '2025-02-28 18:00:00', 1, 500, 200);

-- 插入一些示例座位数据
-- 为上海大剧院池座区域插入部分座位
INSERT INTO t_seat (area_id, row_num, seat_num, status) VALUES
-- A排座位
(5, 'A', '1', 1), (5, 'A', '2', 1), (5, 'A', '3', 1), (5, 'A', '4', 1), (5, 'A', '5', 1),
(5, 'A', '6', 1), (5, 'A', '7', 1), (5, 'A', '8', 1), (5, 'A', '9', 1), (5, 'A', '10', 1),
-- B排座位
(5, 'B', '1', 1), (5, 'B', '2', 1), (5, 'B', '3', 1), (5, 'B', '4', 1), (5, 'B', '5', 1),
(5, 'B', '6', 1), (5, 'B', '7', 1), (5, 'B', '8', 1), (5, 'B', '9', 1), (5, 'B', '10', 1),
-- C排座位
(5, 'C', '1', 1), (5, 'C', '2', 1), (5, 'C', '3', 1), (5, 'C', '4', 1), (5, 'C', '5', 1),
(5, 'C', '6', 1), (5, 'C', '7', 1), (5, 'C', '8', 1), (5, 'C', '9', 1), (5, 'C', '10', 1);

COMMIT;

-- 演出数据库初始化完成
SELECT '演出数据库初始化完成！' AS message;