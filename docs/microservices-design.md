# 抢票系统微服务设计文档

## 1. 系统架构概览

本系统采用前后端分离的架构，前端选用Vue3技术栈，后端采用微服务架构。

### 1.1 前端架构

- **框架**: Vue 3 + TypeScript
- **UI 库**: Element Plus
- **构建工具**: Vite
- **状态管理**: Pinia
- **路由**: Vue Router
- **代码规范**: ESLint + Prettier

### 1.2 后端架构

后端微服务主要包含以下服务模块：

- 网关服务 (Gateway Service)
- 用户服务 (User Service)
- 演出服务 (Show Service)
- 票务服务 (Ticket Service)
- 订单服务 (Order Service)
- 支付服务 (Payment Service)
- 通知服务 (Notification Service)

## 2. 各微服务模块详细设计

### 2.1 网关服务 (Gateway Service)

#### 功能职责
- API路由转发
- 请求过滤与限流
- 用户认证与授权
- 跨域处理
- 请求日志记录

#### 主要接口
- `/api/**` - 所有API的统一入口
- `/api/auth/**` - 认证相关接口
- `/api/user/**` - 用户服务接口
- `/api/show/**` - 演出服务接口
- `/api/ticket/**` - 票务服务接口
- `/api/order/**` - 订单服务接口
- `/api/payment/**` - 支付服务接口

### 2.2 用户服务 (User Service)

#### 功能职责
- 用户注册与登录
- 用户信息管理
- 用户权限管理
- 用户地址管理

#### 数据模型

**用户表 (t_user)**
```sql
CREATE TABLE t_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    phone VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(100) UNIQUE,
    avatar VARCHAR(255),
    gender TINYINT,
    birthday DATE,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    last_login_time DATETIME,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
```

**用户地址表 (t_user_address)**
```sql
CREATE TABLE t_user_address (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    receiver_name VARCHAR(50) NOT NULL,
    receiver_phone VARCHAR(20) NOT NULL,
    province VARCHAR(20) NOT NULL,
    city VARCHAR(20) NOT NULL,
    district VARCHAR(20) NOT NULL,
    detail_address VARCHAR(200) NOT NULL,
    is_default TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES t_user(id)
);
```

**用户角色表 (t_user_role)**
```sql
CREATE TABLE t_user_role (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES t_user(id),
    FOREIGN KEY (role_id) REFERENCES t_role(id)
);
```

**角色表 (t_role)**
```sql
CREATE TABLE t_role (
    id BIGINT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200),
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
```

#### 主要接口

**用户管理接口**
- `POST /api/user/register` - 用户注册
- `POST /api/user/login` - 用户登录
- `GET /api/user/info` - 获取用户信息
- `PUT /api/user/info` - 更新用户信息
- `PUT /api/user/password` - 修改密码
- `POST /api/user/logout` - 用户登出

**用户地址接口**
- `POST /api/user/address` - 添加地址
- `GET /api/user/address/list` - 获取地址列表
- `GET /api/user/address/{id}` - 获取地址详情
- `PUT /api/user/address/{id}` - 更新地址
- `DELETE /api/user/address/{id}` - 删除地址
- `PUT /api/user/address/default/{id}` - 设置默认地址

### 2.3 演出服务 (Show Service)

#### 功能职责
- 演出信息管理
- 演出场次管理
- 场馆信息管理
- 座位信息管理
- 演出搜索功能

#### 数据模型

**演出表 (t_show)**
```sql
CREATE TABLE t_show (
    id BIGINT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    category_id BIGINT NOT NULL,
    description TEXT,
    cover_img VARCHAR(255),
    detail_imgs TEXT,
    duration INT COMMENT '演出时长(分钟)',
    notice TEXT COMMENT '观演须知',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-下架，1-上架',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    FOREIGN KEY (category_id) REFERENCES t_category(id)
);
```

**演出类别表 (t_category)**
```sql
CREATE TABLE t_category (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    parent_id BIGINT DEFAULT 0,
    level TINYINT NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
```

**场馆表 (t_venue)**
```sql
CREATE TABLE t_venue (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    province VARCHAR(20) NOT NULL,
    city VARCHAR(20) NOT NULL,
    district VARCHAR(20) NOT NULL,
    address VARCHAR(200) NOT NULL,
    description TEXT,
    contact_phone VARCHAR(20),
    traffic_info TEXT COMMENT '交通信息',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
```

**演出场次表 (t_show_session)**
```sql
CREATE TABLE t_show_session (
    id BIGINT PRIMARY KEY,
    show_id BIGINT NOT NULL,
    venue_id BIGINT NOT NULL,
    session_time DATETIME NOT NULL COMMENT '演出时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-已取消，1-未开售，2-售卖中，3-已售罄，4-已结束',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    FOREIGN KEY (show_id) REFERENCES t_show(id),
    FOREIGN KEY (venue_id) REFERENCES t_venue(id)
);
```

**座位区域表 (t_seat_area)**
```sql
CREATE TABLE t_seat_area (
    id BIGINT PRIMARY KEY,
    venue_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    capacity INT NOT NULL COMMENT '座位容量',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    FOREIGN KEY (venue_id) REFERENCES t_venue(id)
);
```

**座位表 (t_seat)**
```sql
CREATE TABLE t_seat (
    id BIGINT PRIMARY KEY,
    area_id BIGINT NOT NULL,
    row_num VARCHAR(10) NOT NULL COMMENT '排号',
    seat_num VARCHAR(10) NOT NULL COMMENT '座位号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-不可用，1-可用',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    FOREIGN KEY (area_id) REFERENCES t_seat_area(id)
);
```

#### 主要接口

**演出管理接口**
- `GET /api/show/list` - 获取演出列表
- `GET /api/show/{id}` - 获取演出详情
- `GET /api/show/category/list` - 获取演出类别列表
- `GET /api/show/search` - 搜索演出

**场次管理接口**
- `GET /api/show/session/list/{showId}` - 获取演出场次列表
- `GET /api/show/session/{id}` - 获取场次详情

**场馆管理接口**
- `GET /api/show/venue/{id}` - 获取场馆详情
- `GET /api/show/venue/list` - 获取场馆列表

**座位管理接口**
- `GET /api/show/seat/area/list/{venueId}` - 获取场馆座位区域列表
- `GET /api/show/seat/list/{areaId}` - 获取区域座位列表
- `GET /api/show/seat/available/{sessionId}` - 获取场次可用座位

### 2.4 票务服务 (Ticket Service)

#### 功能职责
- 票务库存管理
- 高并发抢票核心逻辑
- 票务锁定机制
- 票务状态管理

#### 数据模型

**票种表 (t_ticket_type)**
```sql
CREATE TABLE t_ticket_type (
    id BIGINT PRIMARY KEY,
    show_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    area_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL COMMENT '总库存',
    remaining INT NOT NULL COMMENT '剩余库存',
    limit_per_user INT NOT NULL DEFAULT 4 COMMENT '每人限购数量',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-停售，1-正常',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    FOREIGN KEY (show_id) REFERENCES t_show(id),
    FOREIGN KEY (session_id) REFERENCES t_show_session(id),
    FOREIGN KEY (area_id) REFERENCES t_seat_area(id)
);
```

**票务库存表 (t_ticket_stock)**
```sql
CREATE TABLE t_ticket_stock (
    id BIGINT PRIMARY KEY,
    ticket_type_id BIGINT NOT NULL,
    total_stock INT NOT NULL,
    locked_stock INT NOT NULL DEFAULT 0,
    sold_stock INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    FOREIGN KEY (ticket_type_id) REFERENCES t_ticket_type(id)
);
```

**票务锁定记录表 (t_ticket_lock)**
```sql
CREATE TABLE t_ticket_lock (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ticket_type_id BIGINT NOT NULL,
    seat_id BIGINT,
    quantity INT NOT NULL,
    lock_time DATETIME NOT NULL,
    expire_time DATETIME NOT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-已释放，1-锁定中，2-已转订单',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES t_user(id),
    FOREIGN KEY (ticket_type_id) REFERENCES t_ticket_type(id),
    FOREIGN KEY (seat_id) REFERENCES t_seat(id)
);
```

#### 主要接口

**票务管理接口**
- `GET /api/ticket/type/list/{sessionId}` - 获取场次票种列表
- `GET /api/ticket/stock/{ticketTypeId}` - 获取票种库存信息
- `POST /api/ticket/lock` - 锁定票务
- `POST /api/ticket/unlock` - 释放票务锁定
- `POST /api/ticket/confirm` - 确认票务购买
- `GET /api/ticket/user/list` - 获取用户票务列表

### 2.5 订单服务 (Order Service)

#### 功能职责
- 订单创建与管理
- 订单状态流转
- 订单超时处理
- 订单支付状态同步

#### 数据模型

**订单表 (t_order)**
```sql
CREATE TABLE t_order (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    pay_amount DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待支付，1-已支付，2-已取消，3-已完成，4-已退款',
    pay_type TINYINT COMMENT '支付方式：1-支付宝，2-微信，3-银联',
    pay_time DATETIME COMMENT '支付时间',
    expire_time DATETIME NOT NULL COMMENT '订单过期时间',
    remark VARCHAR(200),
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES t_user(id)
);
```

**订单明细表 (t_order_item)**
```sql
CREATE TABLE t_order_item (
    id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    show_id BIGINT NOT NULL,
    show_title VARCHAR(100) NOT NULL,
    session_id BIGINT NOT NULL,
    session_time DATETIME NOT NULL,
    venue_id BIGINT NOT NULL,
    venue_name VARCHAR(100) NOT NULL,
    ticket_type_id BIGINT NOT NULL,
    ticket_type_name VARCHAR(50) NOT NULL,
    seat_id BIGINT,
    seat_info VARCHAR(50) COMMENT '座位信息',
    price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    FOREIGN KEY (order_id) REFERENCES t_order(id)
);
```

**订单状态流转表 (t_order_status_log)**
```sql
CREATE TABLE t_order_status_log (
    id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    previous_status TINYINT NOT NULL,
    current_status TINYINT NOT NULL,
    operator VARCHAR(50) COMMENT '操作人',
    remark VARCHAR(200),
    create_time DATETIME NOT NULL,
    FOREIGN KEY (order_id) REFERENCES t_order(id)
);
```

#### 主要接口

**订单管理接口**
- `POST /api/order/create` - 创建订单
- `GET /api/order/detail/{orderNo}` - 获取订单详情
- `GET /api/order/list` - 获取用户订单列表
- `POST /api/order/cancel/{orderNo}` - 取消订单
- `GET /api/order/status/{orderNo}` - 查询订单状态

### 2.6 支付服务 (Payment Service)

#### 功能职责
- 支付渠道集成
- 支付流程处理
- 支付结果通知
- 退款处理

#### 数据模型

**支付记录表 (t_payment)**
```sql
CREATE TABLE t_payment (
    id BIGINT PRIMARY KEY,
    payment_no VARCHAR(64) NOT NULL UNIQUE,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    pay_amount DECIMAL(10,2) NOT NULL,
    pay_type TINYINT NOT NULL COMMENT '支付方式：1-支付宝，2-微信，3-银联',
    pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待支付，1-支付中，2-支付成功，3-支付失败',
    trade_no VARCHAR(64) COMMENT '第三方支付流水号',
    callback_time DATETIME COMMENT '回调时间',
    callback_content TEXT COMMENT '回调内容',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES t_user(id)
);
```

**退款记录表 (t_refund)**
```sql
CREATE TABLE t_refund (
    id BIGINT PRIMARY KEY,
    refund_no VARCHAR(64) NOT NULL UNIQUE,
    payment_no VARCHAR(64) NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    refund_amount DECIMAL(10,2) NOT NULL,
    refund_reason VARCHAR(200),
    refund_status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-申请中，1-退款中，2-退款成功，3-退款失败',
    trade_no VARCHAR(64) COMMENT '第三方退款流水号',
    callback_time DATETIME COMMENT '回调时间',
    callback_content TEXT COMMENT '回调内容',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES t_user(id)
);
```

#### 主要接口

**支付管理接口**
- `POST /api/payment/create` - 创建支付
- `GET /api/payment/status/{paymentNo}` - 查询支付状态
- `POST /api/payment/notify/{payType}` - 支付回调通知
- `POST /api/payment/refund` - 申请退款
- `GET /api/payment/refund/status/{refundNo}` - 查询退款状态

### 2.7 通知服务 (Notification Service)

#### 功能职责
- 短信通知
- 邮件通知
- 站内消息
- 消息模板管理

#### 数据模型

**消息记录表 (t_message)**
```sql
CREATE TABLE t_message (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type TINYINT NOT NULL COMMENT '消息类型：1-系统消息，2-订单消息，3-活动消息',
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
    read_time DATETIME COMMENT '阅读时间',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES t_user(id)
);
```

**消息模板表 (t_message_template)**
```sql
CREATE TABLE t_message_template (
    id BIGINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '模板编码',
    type TINYINT NOT NULL COMMENT '模板类型：1-短信，2-邮件，3-站内信',
    title VARCHAR(100) COMMENT '标题模板',
    content TEXT NOT NULL COMMENT '内容模板',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
```

**发送记录表 (t_send_record)**
```sql
CREATE TABLE t_send_record (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    template_id BIGINT NOT NULL,
    type TINYINT NOT NULL COMMENT '发送类型：1-短信，2-邮件，3-站内信',
    receiver VARCHAR(100) NOT NULL COMMENT '接收者(手机号/邮箱/用户ID)',
    content TEXT NOT NULL COMMENT '发送内容',
    send_status TINYINT NOT NULL COMMENT '发送状态：0-发送中，1-发送成功，2-发送失败',
    send_time DATETIME NOT NULL COMMENT '发送时间',
    error_msg VARCHAR(200) COMMENT '错误信息',
    create_time DATETIME NOT NULL,
    FOREIGN KEY (template_id) REFERENCES t_message_template(id)
);
```

#### 主要接口

**消息管理接口**
- `POST /api/notification/send/sms` - 发送短信
- `POST /api/notification/send/email` - 发送邮件
- `POST /api/notification/send/message` - 发送站内信
- `GET /api/notification/message/list` - 获取用户消息列表
- `PUT /api/notification/message/read/{id}` - 标记消息为已读
- `PUT /api/notification/message/read/all` - 标记所有消息为已读

## 3. 微服务间通信

### 3.1 同步通信
- 使用Feign进行服务间的REST调用
- 使用Sentinel进行服务熔断和降级

### 3.2 异步通信
- 使用RocketMQ进行消息队列通信
- 主要消息主题：
  - `topic-order-created` - 订单创建消息
  - `topic-order-paid` - 订单支付消息
  - `topic-order-canceled` - 订单取消消息
  - `topic-ticket-locked` - 票务锁定消息
  - `topic-ticket-released` - 票务释放消息

### 3.3 分布式事务
- 使用Seata进行分布式事务管理
- 主要事务场景：
  - 创建订单 + 锁定票务
  - 支付订单 + 确认票务
  - 取消订单 + 释放票务

## 4. 高并发抢票设计

### 4.1 抢票流程
1. 用户选择演出和场次
2. 进入抢票队列（Redis队列）
3. 检查用户资格（是否黑名单、购买限制等）
4. 检查票务库存（Redis缓存）
5. 锁定票务（Redis分布式锁）
6. 创建订单（异步）
7. 等待支付

### 4.2 技术实现
- 使用Redis实现分布式锁和库存控制
- 使用RocketMQ实现请求削峰和异步处理
- 使用Redis实现排队机制
- 前端限流 + 网关限流 + 服务限流

## 5. 安全设计

### 5.1 认证与授权
- 基于JWT的用户认证
- 基于RBAC的权限控制
- Gateway统一认证授权

### 5.2 数据安全
- 敏感数据加密存储
- 传输数据HTTPS加密
- 防SQL注入、XSS攻击

### 5.3 接口安全
- 接口幂等性设计
- 接口频率限制
- 数据验证与过滤