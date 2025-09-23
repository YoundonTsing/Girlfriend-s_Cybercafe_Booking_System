-- 创建票档库存表
CREATE TABLE IF NOT EXISTS `t_ticket_stock` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ticket_id` bigint NOT NULL COMMENT '票档ID',
  `total_stock` int NOT NULL DEFAULT '0' COMMENT '总库存',
  `locked_stock` int NOT NULL DEFAULT '0' COMMENT '锁定库存',
  `sold_stock` int NOT NULL DEFAULT '0' COMMENT '已售库存',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ticket_id` (`ticket_id`),
  KEY `idx_ticket_id` (`ticket_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票档库存表';

-- 为现有票档初始化库存记录
INSERT INTO `t_ticket_stock` (`ticket_id`, `total_stock`, `locked_stock`, `sold_stock`, `version`)
SELECT 
    t.`id` as `ticket_id`,
    COALESCE(t.`total_count`, 0) as `total_stock`,
    0 as `locked_stock`,
    COALESCE(t.`total_count` - t.`remain_count`, 0) as `sold_stock`,
    0 as `version`
FROM `t_ticket` t
WHERE NOT EXISTS (
    SELECT 1 FROM `t_ticket_stock` ts WHERE ts.`ticket_id` = t.`id`
);

-- 验证数据
SELECT 
    ts.ticket_id,
    ts.total_stock,
    ts.locked_stock,
    ts.sold_stock,
    ts.version,
    t.total_count,
    t.remain_count
FROM t_ticket_stock ts
JOIN t_ticket t ON ts.ticket_id = t.id
ORDER BY ts.ticket_id;

-- 显示表结构
DESC t_ticket_stock;