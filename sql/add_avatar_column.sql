-- 为t_user表添加缺失的avatar字段
-- 这个脚本修复了User实体类和数据库表结构不匹配的问题

USE ticket_user_db;

-- 检查avatar字段是否存在，如果不存在则添加
SET @sql = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
         WHERE TABLE_SCHEMA = 'ticket_user_db' 
         AND TABLE_NAME = 't_user' 
         AND COLUMN_NAME = 'avatar') = 0,
        'ALTER TABLE t_user ADD COLUMN avatar VARCHAR(255) COMMENT ''头像'' AFTER email;',
        'SELECT ''avatar字段已存在'' AS message;'
    )
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 验证结果
SELECT '修复完成！avatar字段已添加到t_user表' AS result;

-- 显示当前表结构
DESCRIBE t_user;