-- ========================================
-- 微服务分库架构：票务系统转网咖预约系统数据转换脚本
-- 适配实际的分库表结构
-- ========================================

-- ========================================
-- 1. 演出服务数据库 (ticket_show_db) 转换
-- ========================================

USE ticket_show_db;

-- 修改演出类别为机位类型
UPDATE t_category SET name = '新客电竞机位' WHERE id = 1; -- 原:音乐会
UPDATE t_category SET name = '中级电竞机位' WHERE id = 2; -- 原:话剧  
UPDATE t_category SET name = '高级电竞机位' WHERE id = 3; -- 原:舞蹈
UPDATE t_category SET name = '包厢电竞机位' WHERE id = 4; -- 原:体育赛事
UPDATE t_category SET name = 'SVIP电竞机位' WHERE id = 5; -- 原:展览

-- 删除二级类别（网咖不需要细分）
DELETE FROM t_category WHERE level = 2;

-- 修改演出数据为机位数据 (注意：字段名是name不是title，poster_url不是cover_img)
-- 新客电竞机位01号 (原:2024新年音乐会)
UPDATE t_show SET 
    name = '新客电竞机位01号',
    type = 1, -- 重新定义type：1-新客，2-中级，3-高级，4-包厢，5-SVIP
    poster_url = '/images/seat_newbie.jpg',
    description = '适合新手玩家，配置：Intel i5-8400/GTX1060 6G/16G内存/256G SSD，舒适环境，网速稳定',
    venue = '自由点网咖A区',
    city = '哈尔滨市',
    start_time = CURDATE() + INTERVAL 10 HOUR, -- 今天上午10点开始营业
    end_time = CURDATE() + INTERVAL 1 DAY, -- 明天结束
    min_price = 30.00, -- 2小时最低价
    max_price = 100.00, -- 通宵最高价
    status = 1, -- 营业中
    is_hot = 1,
    is_recommend = 1
WHERE id = 1;

-- 中级电竞机位01号 (原:《雷雨》话剧)
UPDATE t_show SET 
    name = '中级电竞机位01号',
    type = 2,
    poster_url = '/images/seat_intermediate.jpg',
    description = '适合中级玩家，配置：Intel i7-9700/RTX2070 8G/32G内存/512G SSD，游戏体验佳，支持高画质',
    venue = '自由点网咖B区',
    city = '哈尔滨市',
    start_time = CURDATE() + INTERVAL 10 HOUR,
    end_time = CURDATE() + INTERVAL 1 DAY,
    min_price = 50.00,
    max_price = 150.00,
    status = 1,
    is_hot = 0,
    is_recommend = 1
WHERE id = 2;

-- 高级电竞机位01号 (原:天鹅湖芭蕾舞)
UPDATE t_show SET 
    name = '高级电竞机位01号',
    type = 3,
    poster_url = '/images/seat_advanced.jpg',
    description = '高端配置机位，配置：Intel i7-11700K/RTX3070 8G/32G内存/1TB SSD，专业电竞配置',
    venue = '自由点网咖B区',
    city = '哈尔滨市',
    start_time = CURDATE() + INTERVAL 10 HOUR,
    end_time = CURDATE() + INTERVAL 1 DAY,
    min_price = 70.00,
    max_price = 200.00,
    status = 1,
    is_hot = 1,
    is_recommend = 0
WHERE id = 3;

-- 包厢电竞机位01号 (原:NBA中国赛)
UPDATE t_show SET 
    name = '包厢电竞机位01号',
    type = 4,
    poster_url = '/images/seat_vip_room.jpg',
    description = '私密包厢环境，配置：Intel i9-12900K/RTX3080 10G/64G内存/1TB SSD，独立空间，可多人开黑',
    venue = '自由点网咖C区',
    city = '哈尔滨市',
    start_time = CURDATE() + INTERVAL 10 HOUR,
    end_time = CURDATE() + INTERVAL 1 DAY,
    min_price = 100.00,
    max_price = 300.00,
    status = 1,
    is_hot = 1,
    is_recommend = 1
WHERE id = 4;

-- SVIP电竞机位01号 (原:毕加索画展)
UPDATE t_show SET 
    name = 'SVIP电竞机位01号',
    type = 5,
    poster_url = '/images/seat_svip.jpg',
    description = '顶级配置机位，配置：Intel i9-13900K/RTX4090 24G/128G内存/2TB SSD，至尊体验，专属服务',
    venue = '自由点网咖SVIP区',
    city = '哈尔滨市',
    start_time = CURDATE() + INTERVAL 10 HOUR,
    end_time = CURDATE() + INTERVAL 1 DAY,
    min_price = 100.00,
    max_price = 300.00,
    status = 1,
    is_hot = 0,
    is_recommend = 0
WHERE id = 5;

-- 修改场馆数据为网咖区域
-- 自由点网咖A区 (原:北京国家体育馆)
UPDATE t_venue SET 
    name = '自由点网咖A区',
    province = '黑龙江省',
    city = '哈尔滨市', 
    district = '南岗区',
    address = '学府路288号',
    description = '新客和中级机位区域，环境舒适明亮，适合日常娱乐和休闲游戏',
    contact_phone = '0451-88888888',
    traffic_info = '地铁1号线学府路站A出口步行5分钟'
WHERE id = 1;

-- 自由点网咖B区 (原:上海大剧院)
UPDATE t_venue SET 
    name = '自由点网咖B区',
    province = '黑龙江省',
    city = '哈尔滨市',
    district = '南岗区', 
    address = '学府路298号',
    description = '高级机位区域，配置较高，环境优雅，适合专业游戏和电竞比赛',
    contact_phone = '0451-88888889',
    traffic_info = '地铁1号线学府路站B出口步行3分钟'
WHERE id = 2;

-- 自由点网咖C区 (原:广州体育馆)
UPDATE t_venue SET 
    name = '自由点网咖C区',
    province = '黑龙江省',
    city = '哈尔滨市',
    district = '南岗区',
    address = '学府路308号', 
    description = '包厢区域，私密独立空间，适合团队开黑和朋友聚会',
    contact_phone = '0451-88888890',
    traffic_info = '地铁1号线学府路站C出口直达'
WHERE id = 3;

-- 自由点网咖SVIP区 (原:深圳音乐厅)
UPDATE t_venue SET 
    name = '自由点网咖SVIP区',
    province = '黑龙江省',
    city = '哈尔滨市',
    district = '南岗区',
    address = '学府路318号',
    description = 'SVIP专区，顶级配置和专属服务，豪华装修，专业电竞椅和外设',
    contact_phone = '0451-88888891',
    traffic_info = '地铁1号线学府路站VIP专用通道'
WHERE id = 4;

-- 修改座位区域为时段价格
-- 自由点网咖A区 (venue_id=1) 的时段
UPDATE t_seat_area SET name = '2小时时段', price = 30.00 WHERE venue_id = 1 AND name = 'VIP区';
UPDATE t_seat_area SET name = '4小时时段', price = 50.00 WHERE venue_id = 1 AND name = '一等座';
UPDATE t_seat_area SET name = '6小时时段', price = 70.00 WHERE venue_id = 1 AND name = '二等座';
UPDATE t_seat_area SET name = '通宵时段', price = 100.00 WHERE venue_id = 1 AND name = '三等座';

-- 自由点网咖B区 (venue_id=2) 的时段
UPDATE t_seat_area SET name = '2小时时段', price = 50.00 WHERE venue_id = 2 AND name = '池座';
UPDATE t_seat_area SET name = '4小时时段', price = 80.00 WHERE venue_id = 2 AND name = '一楼包厢';
UPDATE t_seat_area SET name = '6小时时段', price = 110.00 WHERE venue_id = 2 AND name = '二楼';
UPDATE t_seat_area SET name = '通宵时段', price = 150.00 WHERE venue_id = 2 AND name = '三楼';

-- 自由点网咖C区 (venue_id=3) 的时段
UPDATE t_seat_area SET name = '2小时时段', price = 70.00 WHERE venue_id = 3 AND name = 'VIP区';
UPDATE t_seat_area SET name = '4小时时段', price = 110.00 WHERE venue_id = 3 AND name = '内场';
UPDATE t_seat_area SET name = '6小时时段', price = 150.00 WHERE venue_id = 3 AND name = '看台A区';
UPDATE t_seat_area SET name = '通宵时段', price = 200.00 WHERE venue_id = 3 AND name = '看台B区';

-- 自由点网咖SVIP区 (venue_id=4) 的时段
UPDATE t_seat_area SET name = '2小时时段', price = 100.00 WHERE venue_id = 4 AND name = '一楼正厅';
UPDATE t_seat_area SET name = '4小时时段', price = 160.00 WHERE venue_id = 4 AND name = '一楼包厢';
UPDATE t_seat_area SET name = '6小时时段', price = 220.00 WHERE venue_id = 4 AND name = '二楼正厅';
UPDATE t_seat_area SET name = '通宵时段', price = 300.00 WHERE venue_id = 4 AND name = '二楼包厅';

-- 更新演出场次为预约时段
-- 场次1：今天上午10点
UPDATE t_show_session SET 
    name = '上午时段',
    start_time = CURDATE() + INTERVAL 10 HOUR,
    end_time = CURDATE() + INTERVAL 12 HOUR,
    status = 1
WHERE id = 1;

-- 场次2：今天下午2点
UPDATE t_show_session SET 
    name = '下午时段',
    start_time = CURDATE() + INTERVAL 14 HOUR,
    end_time = CURDATE() + INTERVAL 16 HOUR,
    status = 1
WHERE id = 2;

-- 场次3：今天下午6点
UPDATE t_show_session SET 
    name = '傍晚时段',
    start_time = CURDATE() + INTERVAL 18 HOUR,
    end_time = CURDATE() + INTERVAL 20 HOUR,
    status = 1
WHERE id = 3;

-- 场次4：今天晚上10点
UPDATE t_show_session SET 
    name = '夜间时段',
    start_time = CURDATE() + INTERVAL 22 HOUR,
    end_time = CURDATE() + INTERVAL 1 DAY,
    status = 1
WHERE id = 4;

-- 场次5：明天上午10点
UPDATE t_show_session SET 
    name = '明日上午时段',
    start_time = CURDATE() + INTERVAL 1 DAY + INTERVAL 10 HOUR,
    end_time = CURDATE() + INTERVAL 1 DAY + INTERVAL 12 HOUR,
    status = 1
WHERE id = 5;

COMMIT;

-- ========================================
-- 2. 票务服务数据库 (ticket_ticket_db) 转换
-- ========================================

USE ticket_ticket_db;

-- 修改票档名称为时段（注意：这里表名是t_ticket，不是t_ticket_type）
-- 更新ticket表中的name字段
UPDATE t_ticket SET name = '2小时时段' WHERE name = '池座';
UPDATE t_ticket SET name = '4小时时段' WHERE name = '一楼包厢';
UPDATE t_ticket SET name = '6小时时段' WHERE name = '二楼';
UPDATE t_ticket SET name = '通宵时段' WHERE name = '三楼';

-- 更新VIP相关的票档
UPDATE t_ticket SET name = '2小时时段' WHERE name = 'VIP区';
UPDATE t_ticket SET name = '4小时时段' WHERE name = '一等座';
UPDATE t_ticket SET name = '6小时时段' WHERE name = '二等座';
UPDATE t_ticket SET name = '通宵时段' WHERE name = '三等座';

-- 调整价格为网咖时段价格
UPDATE t_ticket SET price = 30.00 WHERE name = '2小时时段' AND show_id = 1;
UPDATE t_ticket SET price = 50.00 WHERE name = '4小时时段' AND show_id = 1;
UPDATE t_ticket SET price = 70.00 WHERE name = '6小时时段' AND show_id = 1;
UPDATE t_ticket SET price = 100.00 WHERE name = '通宵时段' AND show_id = 1;

UPDATE t_ticket SET price = 50.00 WHERE name = '2小时时段' AND show_id = 2;
UPDATE t_ticket SET price = 80.00 WHERE name = '4小时时段' AND show_id = 2;
UPDATE t_ticket SET price = 110.00 WHERE name = '6小时时段' AND show_id = 2;
UPDATE t_ticket SET price = 150.00 WHERE name = '通宵时段' AND show_id = 2;

-- 更新限购数量为合理的预约限制
UPDATE t_ticket SET limit_count = 2 WHERE name IN ('2小时时段', '4小时时段');
UPDATE t_ticket SET limit_count = 1 WHERE name IN ('6小时时段', '通宵时段');

COMMIT;

-- ========================================
-- 3. 订单服务数据库 (ticket_order_db) 转换
-- ========================================

USE ticket_order_db;

-- 更新订单明细中的演出标题
UPDATE t_order_item SET show_title = '新客电竞机位01号' WHERE show_title = '2024新年音乐会';
UPDATE t_order_item SET show_title = '中级电竞机位01号' WHERE show_title = '《雷雨》话剧';
UPDATE t_order_item SET show_title = '包厢电竞机位01号' WHERE show_title = 'NBA中国赛';

-- 更新票种名称
UPDATE t_order_item SET ticket_type_name = '2小时时段' WHERE ticket_type_name = '池座';
UPDATE t_order_item SET ticket_type_name = '2小时时段' WHERE ticket_type_name = 'VIP区';

-- 更新场馆名称
UPDATE t_order_item SET venue_name = '自由点网咖A区' WHERE venue_name = '北京国家体育馆';
UPDATE t_order_item SET venue_name = '自由点网咖B区' WHERE venue_name = '上海大剧院';

-- 更新订单备注
UPDATE t_order SET remark = '新客机位预约订单' WHERE remark = '新年音乐会订单';
UPDATE t_order SET remark = '中级机位预约订单' WHERE remark = '雷雨话剧订单';
UPDATE t_order SET remark = '包厢机位预约订单' WHERE remark = 'NBA中国赛订单';

COMMIT;

-- ========================================
-- 4. 用户服务数据库 (ticket_user_db) 保持不变
-- ========================================
-- 用户数据无需修改，角色和权限体系保持不变

-- ========================================
-- 验证转换结果
-- ========================================

-- 检查演出服务数据库
USE ticket_show_db;
SELECT '=== 演出服务数据库验证 ===' AS verification;
SELECT id, name FROM t_category WHERE level = 1 ORDER BY id;
SELECT id, name, type, venue, city, min_price, max_price FROM t_show ORDER BY id;
SELECT id, name, start_time, end_time FROM t_show_session ORDER BY id LIMIT 5;
SELECT v.name AS venue_name, sa.name AS time_slot, sa.price 
FROM t_seat_area sa 
JOIN t_venue v ON sa.venue_id = v.id 
ORDER BY v.id, sa.price LIMIT 10;

-- 检查票务服务数据库
USE ticket_ticket_db;
SELECT '=== 票务服务数据库验证 ===' AS verification;
SELECT id, show_id, session_id, name, price, total_count FROM t_ticket ORDER BY id LIMIT 10;

-- 检查订单服务数据库
USE ticket_order_db;
SELECT '=== 订单服务数据库验证 ===' AS verification;
SELECT id, order_no, show_title, venue_name, ticket_type_name FROM t_order_item ORDER BY id LIMIT 5;

-- ========================================
-- 转换完成提示
-- ========================================
SELECT '微服务分库数据转换完成！' AS message;
SELECT '票务系统已成功转为网咖预约系统' AS result;
SELECT '机位类型：新客/中级/高级/包厢/SVIP电竞机位' AS seat_types;
SELECT '时段选择：2小时/4小时/6小时/通宵时段' AS time_slots;
SELECT '已适配微服务架构的分库结构' AS architecture;