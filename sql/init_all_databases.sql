-- 票务系统微服务分库初始化脚本
-- 按照微服务架构进行数据库分离
-- 执行顺序：先执行此脚本，再根据需要执行各个服务的独立脚本

-- ========================================
-- 创建所有微服务数据库
-- ========================================

-- 用户服务数据库
CREATE DATABASE IF NOT EXISTS ticket_user_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 演出服务数据库
CREATE DATABASE IF NOT EXISTS ticket_show_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 票务服务数据库
CREATE DATABASE IF NOT EXISTS ticket_ticket_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 订单服务数据库
CREATE DATABASE IF NOT EXISTS ticket_order_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 支付服务数据库
CREATE DATABASE IF NOT EXISTS ticket_payment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 通知服务数据库
CREATE DATABASE IF NOT EXISTS ticket_message_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ========================================
-- 数据库创建完成提示
-- ========================================

SELECT '所有微服务数据库创建完成！' AS message;
SELECT 'ticket_user_db - 用户服务数据库' AS database_info
UNION ALL
SELECT 'ticket_show_db - 演出服务数据库'
UNION ALL
SELECT 'ticket_ticket_db - 票务服务数据库'
UNION ALL
SELECT 'ticket_order_db - 订单服务数据库'
UNION ALL
SELECT 'ticket_payment_db - 支付服务数据库'
UNION ALL
SELECT 'ticket_message_db - 通知服务数据库';

-- ========================================
-- 执行说明
-- ========================================

/*
微服务分库策略执行说明：

1. 首先执行此脚本创建所有数据库：
   mysql -u root -p < init_all_databases.sql

2. 然后按需执行各服务的初始化脚本：
   mysql -u root -p < init_user_database.sql      # 用户服务
   mysql -u root -p < init_show_database.sql      # 演出服务
   mysql -u root -p < init_ticket_database.sql    # 票务服务
   mysql -u root -p < init_order_database.sql     # 订单服务
   mysql -u root -p < init_payment_database.sql   # 支付服务
   mysql -u root -p < init_message_database.sql   # 通知服务

3. 或者一次性执行所有脚本：
   for file in init_*.sql; do mysql -u root -p < "$file"; done

数据库分离优势：
- 数据隔离：各服务数据独立，避免相互影响
- 性能优化：减少单库压力，提高并发处理能力
- 扩展性：支持独立扩容和优化
- 安全性：可为不同服务设置不同的访问权限
- 维护性：便于独立备份、恢复和维护

注意事项：
- 跨服务数据访问需通过API调用，不能直接跨库查询
- 分布式事务需要特殊处理（如使用Seata等框架）
- 需要修改各服务的数据库连接配置
*/