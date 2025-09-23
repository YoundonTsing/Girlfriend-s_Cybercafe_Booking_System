-- ========================================
-- 票务系统转网咖预约系统数据转换脚本
-- ========================================

USE ticket_system;

-- ========================================
-- 1. 修改演出类别为机位类型
-- ========================================

UPDATE t_category SET name = '新客电竞机位' WHERE id = 1; -- 原:音乐会
UPDATE t_category SET name = '中级电竞机位' WHERE id = 2; -- 原:话剧  
UPDATE t_category SET name = '高级电竞机位' WHERE id = 3; -- 原:舞蹈
UPDATE t_category SET name = '包厢电竞机位' WHERE id = 4; -- 原:体育赛事
UPDATE t_category SET name = 'SVIP电竞机位' WHERE id = 5; -- 原:展览

-- 删除二级类别（网咖不需要细分）
DELETE FROM t_category WHERE level = 2;

-- ========================================
-- 2. 修改演出数据为机位数据
-- ========================================

-- 新客电竞机位01号 (原:2024新年音乐会)
UPDATE t_show SET 
    title = '新客电竞机位01号',
    description = '适合新手玩家，配置：Intel i5-8400/GTX1060 6G/16G内存/256G SSD，舒适环境，网速稳定',
    cover_img = '/images/seat_newbie.jpg',
    duration = 0, -- 网咖按小时计费，不设固定时长
    notice = '请保持环境卫生，禁止在机位上饮食，游戏时间灵活选择'
WHERE id = 1;

-- 中级电竞机位01号 (原:《雷雨》话剧)
UPDATE t_show SET 
    title = '中级电竞机位01号', 
    description = '适合中级玩家，配置：Intel i7-9700/RTX2070 8G/32G内存/512G SSD，游戏体验佳，支持高画质',
    cover_img = '/images/seat_intermediate.jpg',
    duration = 0,
    notice = '请保持环境卫生，禁止在机位上饮食，支持各类主流游戏'
WHERE id = 2;

-- 高级电竞机位01号 (原:天鹅湖芭蕾舞)
UPDATE t_show SET 
    title = '高级电竞机位01号',
    description = '高端配置机位，配置：Intel i7-11700K/RTX3070 8G/32G内存/1TB SSD，专业电竞配置',
    cover_img = '/images/seat_advanced.jpg',
    duration = 0,
    notice = '专业电竞配置，支持4K游戏，请爱护设备'
WHERE id = 3;

-- 包厢电竞机位01号 (原:NBA中国赛)
UPDATE t_show SET 
    title = '包厢电竞机位01号',
    description = '私密包厢环境，配置：Intel i9-12900K/RTX3080 10G/64G内存/1TB SSD，独立空间，可多人开黑',
    cover_img = '/images/seat_vip_room.jpg',
    duration = 0,
    notice = '包厢内可适量饮食，请保持环境整洁，支持团队开黑'
WHERE id = 4;

-- SVIP电竞机位01号 (原:毕加索画展)
UPDATE t_show SET 
    title = 'SVIP电竞机位01号',
    description = '顶级配置机位，配置：Intel i9-13900K/RTX4090 24G/128G内存/2TB SSD，至尊体验，专属服务',
    cover_img = '/images/seat_svip.jpg',
    duration = 0,
    notice = '提供专属服务和茶水，请提前15分钟到达，享受顶级游戏体验'
WHERE id = 5;

-- ========================================
-- 3. 修改场馆数据为网咖区域
-- ========================================

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

-- ========================================
-- 4. 修改座位区域为时段价格
-- ========================================

-- 更新座位区域名称为时段名称
UPDATE t_seat_area SET name = '2小时时段', price = 30.00 WHERE name = 'VIP区' AND venue_id = 1;
UPDATE t_seat_area SET name = '4小时时段', price = 50.00 WHERE name = '一等座' AND venue_id = 1;
UPDATE t_seat_area SET name = '6小时时段', price = 70.00 WHERE name = '二等座' AND venue_id = 1;
UPDATE t_seat_area SET name = '通宵时段', price = 100.00 WHERE name = '三等座' AND venue_id = 1;

UPDATE t_seat_area SET name = '2小时时段', price = 50.00 WHERE name = '池座' AND venue_id = 2;
UPDATE t_seat_area SET name = '4小时时段', price = 80.00 WHERE name = '一楼包厢' AND venue_id = 2;
UPDATE t_seat_area SET name = '6小时时段', price = 110.00 WHERE name = '二楼' AND venue_id = 2;
UPDATE t_seat_area SET name = '通宵时段', price = 150.00 WHERE name = '三楼' AND venue_id = 2;

UPDATE t_seat_area SET name = '2小时时段', price = 70.00 WHERE name = 'VIP区' AND venue_id = 3;
UPDATE t_seat_area SET name = '4小时时段', price = 110.00 WHERE name = '内场' AND venue_id = 3;
UPDATE t_seat_area SET name = '6小时时段', price = 150.00 WHERE name = '看台A区' AND venue_id = 3;
UPDATE t_seat_area SET name = '通宵时段', price = 200.00 WHERE name = '看台B区' AND venue_id = 3;

UPDATE t_seat_area SET name = '2小时时段', price = 100.00 WHERE name = '一楼正厅' AND venue_id = 4;
UPDATE t_seat_area SET name = '4小时时段', price = 160.00 WHERE name = '一楼包厅' AND venue_id = 4;
UPDATE t_seat_area SET name = '6小时时段', price = 220.00 WHERE name = '二楼正厅' AND venue_id = 4;
UPDATE t_seat_area SET name = '通宵时段', price = 300.00 WHERE name = '二楼包厅' AND venue_id = 4;

-- ========================================
-- 5. 修改票种名称为时段
-- ========================================

UPDATE t_ticket_type SET name = '2小时时段' WHERE name = '池座';
UPDATE t_ticket_type SET name = '4小时时段' WHERE name = '一楼包厅';  
UPDATE t_ticket_type SET name = '6小时时段' WHERE name = '二楼';
UPDATE t_ticket_type SET name = '通宵时段' WHERE name = '三楼';

-- ========================================
-- 6. 更新演出场次为预约时段
-- ========================================

-- 更新场次时间为今天和未来几天的营业时段
UPDATE t_show_session SET session_time = '2025-01-20 10:00:00' WHERE id = 1;
UPDATE t_show_session SET session_time = '2025-01-20 14:00:00' WHERE id = 2;
UPDATE t_show_session SET session_time = '2025-01-20 18:00:00' WHERE id = 3;
UPDATE t_show_session SET session_time = '2025-01-20 22:00:00' WHERE id = 4;
UPDATE t_show_session SET session_time = '2025-01-21 10:00:00' WHERE id = 5;

COMMIT;

-- ========================================
-- 转换完成提示
-- ========================================
SELECT '数据转换完成！票务系统已成功转为网咖预约系统' AS message;
SELECT '机位类型：新客/中级/高级/包厢/SVIP电竞机位' AS seat_types;
SELECT '时段选择：2小时/4小时/6小时/通宵时段' AS time_slots;