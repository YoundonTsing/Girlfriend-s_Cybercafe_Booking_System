-- 修复t_user表缺失avatar字段的问题
-- 使用这个SQL脚本来解决 "Unknown column 'avatar' in 'field list'" 错误

-- 切换到用户数据库
USE ticket_user_db;

-- 方法1: 直接添加avatar字段（如果字段不存在）
ALTER TABLE t_user ADD COLUMN IF NOT EXISTS avatar VARCHAR(255) COMMENT '头像URL' AFTER email;

-- 如果上面的语句不支持 IF NOT EXISTS，使用下面的方法：
-- 先检查字段是否存在，如果不存在则添加
-- ALTER TABLE t_user ADD COLUMN avatar VARCHAR(255) COMMENT '头像URL' AFTER email;

-- 验证修复结果
DESCRIBE t_user;

-- 查看修复后的表结构
SHOW CREATE TABLE t_user;

SELECT '✓ avatar字段修复完成！现在可以重新启动ticket-user服务了。' AS status;