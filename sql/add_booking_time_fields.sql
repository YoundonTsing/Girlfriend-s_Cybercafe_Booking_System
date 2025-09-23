-- 为订单表添加预约结束时间和时长字段
USE ticket_order_db;

-- 添加预约结束时间字段
ALTER TABLE t_order ADD COLUMN booking_end_time DATETIME COMMENT '预约结束时间（网咖场景）' AFTER booking_date;

-- 添加预约时长字段（小时）
ALTER TABLE t_order ADD COLUMN booking_duration INT COMMENT '预约时长（小时）（网咖场景）' AFTER booking_end_time;

SELECT '订单表字段添加完成！' AS message;