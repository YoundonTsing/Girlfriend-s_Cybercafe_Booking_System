# 票务系统数据库初始化

本目录包含票务系统的数据库初始化脚本，采用微服务分库架构，为每个微服务创建独立的数据库。

## 微服务分库架构

### 数据库分离策略
- **用户库** (`ticket_user_db`): 用户、角色、地址等用户相关数据
- **演出库** (`ticket_show_db`): 演出、场馆、座位等演出相关数据  
- **票务库** (`ticket_ticket_db`): 票种、库存、锁定等票务相关数据
- **订单库** (`ticket_order_db`): 订单、订单项、状态流转等订单相关数据
- **支付库** (`ticket_payment_db`): 支付、退款等支付相关数据
- **通知库** (`ticket_message_db`): 消息模板、消息记录、发送记录等通知相关数据

## 文件说明

### 1. init_all_databases.sql
- **用途**: 创建所有微服务数据库
- **内容**: 一次性创建所有6个微服务数据库
- **执行**: 最先执行，为后续表结构创建做准备

### 2. 各服务独立初始化脚本
- **init_user_database.sql**: 用户服务数据库和表结构
- **init_show_database.sql**: 演出服务数据库和表结构
- **init_ticket_database.sql**: 票务服务数据库和表结构
- **init_order_database.sql**: 订单服务数据库和表结构
- **init_payment_database.sql**: 支付服务数据库和表结构
- **init_message_database.sql**: 通知服务数据库和表结构

### 3. insert_test_data.sql (已废弃)
- **说明**: 原单库架构的测试数据脚本，新架构中测试数据已集成到各服务脚本中

### 4. README.md
- **用途**: 使用说明文档

## 数据库表结构概览

### 用户服务数据库 (ticket_user_db)
- `t_role` - 角色表
- `t_user` - 用户表
- `t_user_address` - 用户地址表
- `t_user_role` - 用户角色关联表

### 演出服务数据库 (ticket_show_db)
- `t_category` - 演出类别表
- `t_venue` - 场馆表
- `t_show` - 演出表
- `t_show_session` - 演出场次表
- `t_seat_area` - 座位区域表
- `t_seat` - 座位表

### 票务服务数据库 (ticket_ticket_db)
- `t_ticket_type` - 票种表
- `t_ticket_stock` - 票务库存表
- `t_ticket_lock` - 票务锁定记录表

### 订单服务数据库 (ticket_order_db)
- `t_order` - 订单表
- `t_order_item` - 订单明细表
- `t_order_status_log` - 订单状态流转表

### 支付服务数据库 (ticket_payment_db)
- `t_payment` - 支付记录表
- `t_refund` - 退款记录表

### 通知服务数据库 (ticket_message_db)
- `t_message_template` - 消息模板表
- `t_message` - 消息记录表
- `t_send_record` - 发送记录表

## 使用方法

### 前提条件
1. 确保 MySQL 服务已启动
2. 确保有创建数据库的权限
3. 建议使用 MySQL 5.7 或更高版本

### 执行步骤

#### 方法一：使用 MySQL 命令行
```bash
# 1. 连接到 MySQL
mysql -u root -p

# 2. 创建所有数据库
source D:\Tickets\sql\init_all_databases.sql

# 3. 初始化各服务数据库（按顺序执行）
source D:\Tickets\sql\init_user_database.sql
source D:\Tickets\sql\init_show_database.sql
source D:\Tickets\sql\init_ticket_database.sql
source D:\Tickets\sql\init_order_database.sql
source D:\Tickets\sql\init_payment_database.sql
source D:\Tickets\sql\init_message_database.sql
```

#### 方法二：使用 MySQL Workbench
1. 打开 MySQL Workbench
2. 连接到数据库服务器
3. 按顺序执行以下脚本：
   - 打开并执行 `init_all_databases.sql`
   - 打开并执行 `init_user_database.sql`
   - 打开并执行 `init_show_database.sql`
   - 打开并执行 `init_ticket_database.sql`
   - 打开并执行 `init_order_database.sql`
   - 打开并执行 `init_payment_database.sql`
   - 打开并执行 `init_message_database.sql`

#### 方法三：使用其他 MySQL 客户端工具
- Navicat
- phpMyAdmin
- DBeaver
- 等其他工具

### 验证安装

执行以下 SQL 语句验证数据库是否创建成功：

```sql
-- 检查所有微服务数据库
SHOW DATABASES LIKE 'ticket_%_db';

-- 检查各数据库的表数量
SELECT 
    table_schema as '数据库',
    COUNT(*) as '表数量'
FROM information_schema.tables 
WHERE table_schema LIKE 'ticket_%_db'
GROUP BY table_schema;

-- 检查测试数据
USE ticket_user_db;
SELECT COUNT(*) as user_count FROM t_user;

USE ticket_show_db;
SELECT COUNT(*) as show_count FROM t_show;
SELECT COUNT(*) as venue_count FROM t_venue;

USE ticket_ticket_db;
SELECT COUNT(*) as ticket_type_count FROM t_ticket_type;
```

## 测试账号信息

各服务数据库初始化脚本中包含以下测试账号：

| 用户名 | 密码 | 角色 | 手机号 |
|--------|------|------|--------|
| admin | 123456 | 管理员 | 13800138000 |
| testuser1 | 123456 | 普通用户 | 13800138001 |
| testuser2 | 123456 | 普通用户 | 13800138002 |
| testuser3 | 123456 | 普通用户 | 13800138003 |

## 注意事项

1. **数据库配置**: 确保各微服务的 `application.yml` 中的数据库配置与实际环境一致
2. **字符集**: 数据库使用 `utf8mb4` 字符集，支持完整的 Unicode 字符
3. **索引优化**: 脚本已包含常用查询的索引，可根据实际使用情况调整
4. **数据安全**: 生产环境请修改默认密码和敏感配置
5. **备份**: 建议在生产环境执行前先备份现有数据

## 故障排除

### 常见问题

1. **权限不足**
   ```
   ERROR 1044 (42000): Access denied for user 'xxx'@'localhost' to database 'ticket_system'
   ```
   解决方案：确保用户有创建数据库和表的权限

2. **字符集问题**
   ```
   ERROR 1273 (HY000): Unknown collation: 'utf8mb4_unicode_ci'
   ```
   解决方案：升级 MySQL 版本或使用 `utf8_general_ci`

3. **外键约束错误**
   ```
   ERROR 1215 (HY000): Cannot add foreign key constraint
   ```
   解决方案：检查引用表是否存在，数据类型是否匹配

### 重新初始化

如需重新初始化数据库：

```sql
-- 删除所有微服务数据库（谨慎操作）
DROP DATABASE IF EXISTS ticket_user_db;
DROP DATABASE IF EXISTS ticket_show_db;
DROP DATABASE IF EXISTS ticket_ticket_db;
DROP DATABASE IF EXISTS ticket_order_db;
DROP DATABASE IF EXISTS ticket_payment_db;
DROP DATABASE IF EXISTS ticket_message_db;

-- 重新执行初始化脚本
source D:\Tickets\sql\init_all_databases.sql;
source D:\Tickets\sql\init_user_database.sql;
source D:\Tickets\sql\init_show_database.sql;
source D:\Tickets\sql\init_ticket_database.sql;
source D:\Tickets\sql\init_order_database.sql;
source D:\Tickets\sql\init_payment_database.sql;
source D:\Tickets\sql\init_message_database.sql;
```

## 联系支持

如遇到问题，请检查：
1. MySQL 服务状态
2. 用户权限配置
3. 网络连接
4. 脚本文件路径

---

**重要提醒**: 请在执行脚本前仔细阅读内容，确保理解每个操作的影响。生产环境使用前请充分测试。