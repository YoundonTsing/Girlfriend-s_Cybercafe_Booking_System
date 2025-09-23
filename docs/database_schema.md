# 票务系统数据库表结构文档

## 概述

本文档详细描述了票务系统的数据库设计，该系统采用微服务架构，包含四个核心数据库：

1.  **`ticket_user_db`**：管理用户和角色信息。
2.  **`ticket_show_db`**：管理演出、场馆、票务等核心业务信息。
3.  **`ticket_order_db`**：管理用户订单和支付信息。
4.  **`ticket_ticket_db`**：(在此项目中已合并到 `ticket_show_db` 中，相关表为 `t_ticket`, `t_ticket_stock`, `t_ticket_lock`)

---

## 1. 用户数据库 (`ticket_user_db`)

### 1.1 `t_user` (用户表)

**表说明**: 存储系统的用户信息，包括登录凭证、个人信息和状态。

**表结构**:
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外信息 | 说明 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | `bigint` | NO | PRI | NULL | auto_increment | 用户ID (主键) |
| `username` | `varchar(50)` | NO | UNI | NULL | | 用户名 (唯一) |
| `password` | `varchar(100)` | NO | | NULL | | 密码 (加密存储) |
| `nickname` | `varchar(50)` | YES | | NULL | | 昵称 |
| `phone` | `varchar(20)` | NO | UNI | NULL | | 手机号 (唯一) |
| `email` | `varchar(100)` | YES | UNI | NULL | | 邮箱 (唯一) |
| `avatar` | `varchar(255)` | YES | | NULL | | 头像URL |
| `gender` | `tinyint` | YES | | NULL | | 性别: 0-未知, 1-男, 2-女 |
| `birthday` | `date` | YES | | NULL | | 生日 |
| `status` | `tinyint` | NO | | 1 | | 状态: 0-禁用, 1-正常 |
| `create_time` | `datetime` | NO | | CURRENT_TIMESTAMP | | 创建时间 |
| `update_time` | `datetime` | NO | | CURRENT_TIMESTAMP | on update | 更新时间 |
| `last_login_time`| `datetime` | YES | | NULL | | 最后登录时间 |
| `is_deleted` | `tinyint` | NO | | 0 | | 逻辑删除: 0-未删, 1-已删 |

**示例数据**:
```sql
+----+----------+---------------+-------------+--------+
| id | username | nickname      | phone       | status |
+----+----------+---------------+-------------+--------+
|  1 | testuser | Test User     | 13800138000 |      1 |
|  2 | admin    | Administrator | 13800138001 |      1 |
+----+----------+---------------+-------------+--------+
```

---

### 1.2 `t_role` (角色表)

**表说明**: 定义了系统中的用户角色，用于权限管理。

**表结构**:
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外信息 | 说明 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | `bigint` | NO | PRI | NULL | auto_increment | 角色ID (主键) |
| `role_name` | `varchar(50)` | NO | UNI | NULL | | 角色名称 (唯一) |
| `role_code` | `varchar(50)` | NO | UNI | NULL | | 角色编码 (唯一) |
| `description` | `varchar(200)`| YES | | NULL | | 角色描述 |
| `create_time` | `datetime` | NO | | CURRENT_TIMESTAMP | | 创建时间 |
| `update_time` | `datetime` | NO | | CURRENT_TIMESTAMP | on update | 更新时间 |
| `is_deleted` | `tinyint` | NO | | 0 | | 逻辑删除 |

---

## 2. 演出与票务数据库 (`ticket_show_db`)

### 2.1 `t_show` (演出/机位表)

**表说明**: 核心业务表，存储演出或网咖机位的详细信息。

**表结构**:
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外信息 | 说明 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | `bigint` | NO | PRI | NULL | auto_increment | 演出ID (主键) |
| `name` | `varchar(100)`| NO | MUL | NULL | | 演出/机位名称 |
| `type` | `int` | NO | MUL | NULL | | 类型: 1-新客, 2-中级... |
| `poster_url` | `varchar(255)`| YES | | NULL | | 海报URL |
| `description` | `text` | YES | | NULL | | 详细描述 |
| `venue` | `varchar(100)`| YES | | NULL | | 地点/场馆 |
| `city` | `varchar(50)` | YES | MUL | NULL | | 城市 |
| `start_time` | `datetime` | YES | MUL | NULL | | 开始时间 |
| `end_time` | `datetime` | YES | | NULL | | 结束时间 |
| `min_price` | `decimal(10,2)`| YES | | NULL | | 最低价格 |
| `max_price` | `decimal(10,2)`| YES | | NULL | | 最高价格 |
| `status` | `int` | NO | MUL | 0 | | 状态: 0-未开售, 1-售票中... |
| `is_hot` | `int` | NO | MUL | 0 | | 是否热门 |
| `is_recommend`| `int` | NO | MUL | 0 | | 是否推荐 |
| `create_time` | `datetime` | NO | MUL | CURRENT_TIMESTAMP | | 创建时间 |
| `update_time` | `datetime` | NO | | CURRENT_TIMESTAMP | on update| 更新时间 |
| `is_deleted` | `int` | NO | | 0 | | 逻辑删除 |

**示例数据**:
```sql
+----+--------------+------+--------+-----------+-----------+
| id | name         | type | status | min_price | max_price |
+----+--------------+------+--------+-----------+-----------+
|  1 | 新客电竞机位 |    1 |      1 |     50.00 |    680.00 |
|  2 | 中级电竞机位 |    2 |      1 |     80.00 |  10000.00 |
|  3 | 高级电竞机位 |    3 |      1 |    100.00 |  10000.00 |
|  4 | 包厢电竞机位 |    4 |      1 |    250.00 |  10000.00 |
|  5 | SVIP电竞机位 |    5 |      1 |    360.00 |  10000.00 |
+----+--------------+------+--------+-----------+-----------+
```

---

### 2.2 `t_ticket` (票档表)

**表说明**: 定义了不同演出场次下的票种、价格和库存。

**表结构**:
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外信息 | 说明 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | `bigint` | NO | PRI | NULL | auto_increment | 票档ID (主键) |
| `show_id` | `bigint` | NO | MUL | NULL | | 关联演出ID |
| `session_id` | `bigint` | NO | MUL | NULL | | 关联场次ID |
| `name` | `varchar(50)` | NO | | NULL | | 票档名称 (如: VIP区) |
| `price` | `decimal(10,2)`| NO | MUL | NULL | | 价格 |
| `total_count` | `int` | NO | | NULL | | 总票数 |
| `remain_count`| `int` | NO | | NULL | | 剩余票数 |
| `limit_count`| `int` | NO | | 4 | | 每单限购数 |
| `status` | `tinyint` | NO | MUL | 1 | | 状态: 1-售票中 |
| `create_time` | `datetime` | NO | | CURRENT_TIMESTAMP | | 创建时间 |
| `update_time` | `datetime` | NO | | CURRENT_TIMESTAMP | on update | 更新时间 |
| `is_deleted` | `tinyint` | NO | | 0 | | 逻辑删除 |

---

### 2.3 其他 `ticket_show_db` 表结构

-   **`t_category`**: 演出类别表 (如: 音乐会, 话剧)。
-   **`t_venue`**: 场馆信息表。
-   **`t_show_session`**: 演出场次表 (如: 晚场, 下午场)。
-   **`t_seat_area`**: 座位区域表 (如: A区, B区)。
-   **`t_seat`**: 具体座位表。
-   **`t_ticket_stock`**: 票档库存表，用于高并发下的库存管理。
-   **`t_ticket_lock`**: 票档锁定记录表，用于处理下单时临时锁票。

---

## 3. 订单数据库 (`ticket_order_db`)

### 3.1 `t_order` (订单表)

**表说明**: 存储用户的订单主体信息。

**表结构**:
| 字段名 | 类型 | 是否为空 | 键 | 默认值 | 额外信息 | 说明 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | `bigint` | NO | PRI | NULL | auto_increment | 订单ID (主键) |
| `order_no` | `varchar(64)` | NO | UNI | NULL | | 订单号 (唯一) |
| `user_id` | `bigint` | NO | MUL | NULL | | 用户ID |
| `total_amount` | `decimal(10,2)`| NO | | NULL | | 订单总金额 |
| `pay_amount` | `decimal(10,2)`| NO | | NULL | | 实际支付金额 |
| `discount_amount`| `decimal(10,2)`| NO | | 0.00 | | 优惠金额 |
| `status` | `tinyint` | NO | MUL | 0 | | 状态: 0-待支付, 1-已支付... |
| `pay_type` | `tinyint` | YES | | NULL | | 支付方式: 1-支付宝, 2-微信 |
| `pay_time` | `datetime` | YES | MUL | NULL | | 支付时间 |
| `expire_time` | `datetime` | NO | MUL | NULL | | 订单过期时间 |
| `remark` | `varchar(200)`| YES | | NULL | | 备注 |
| `booking_date` | `datetime` | YES | MUL | NULL | | 预约时间 |
| `contact_phone`| `varchar(20)` | YES | MUL | NULL | | 联系电话 |
| `create_time` | `datetime` | NO | MUL | CURRENT_TIMESTAMP | | 创建时间 |
| `update_time` | `datetime` | NO | | CURRENT_TIMESTAMP | on update | 更新时间 |
| `is_deleted` | `tinyint` | NO | | 0 | | 逻辑删除 |

---

### 3.2 其他 `ticket_order_db` 表结构

-   **`t_order_item`**: 订单明细表，记录订单中包含的具体票品信息。
-   **`t_order_status_log`**: 订单状态流转日志表，记录订单状态的每次变更。

---

## 总结与分析

### 1. **设计合理性**
- **微服务架构**: 数据库按业务领域（用户、演出、订单）进行拆分，职责清晰，便于独立扩展和维护，符合现代应用设计趋势。
- **业务完整性**: 表结构覆盖了从用户注册、浏览演出、选座购票到订单支付的完整业务流程。
- **高性能设计**:
    - 使用**索引** (`MUL`) 优化了常用查询字段。
    - 引入了**票务库存** (`t_ticket_stock`) 和**锁** (`t_ticket_lock`) 机制，这是处理高并发抢票场景的关键设计。
    - 使用**逻辑删除** (`is_deleted`) 替代物理删除，保证了数据的可追溯性。

### 2. **与业务的映射**
- **网咖场景适配**: `t_show` 表通过 `type` 字段成功映射为不同等级的“电竞机位”，`name` 字段存储机位名称，`min_price`/`max_price` 对应小时费率，设计灵活，能够很好地支持当前业务。
- **数据一致性**: 订单表 (`t_order`) 和订单详情 (`t_order_item`) 的分离，以及通过外键和订单号 (`order_no`) 进行关联，确保了数据结构规范化和一致性。

### 3. **可扩展性**
- **功能扩展**: 当前设计可以方便地增加如优惠券、积分、会员等级等新功能，只需在相应数据库中增加新表或在现有表中扩展字段。
- **权限管理**: `t_role` 表的存在为未来实现复杂的后台权限管理系统（如场馆管理员、票务管理员）打下了基础。

**结论**: **该数据库设计结构清晰、功能完备、具备良好的性能和扩展性，完全能够支撑当前及未来一段时间的业务需求。**