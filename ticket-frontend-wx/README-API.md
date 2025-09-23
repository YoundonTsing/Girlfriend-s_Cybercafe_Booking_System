# 微信小程序前端 API 配置说明

## 概述

本项目已根据后端微服务设计文档完成了 API 接口的配置，包括用户、演出、订单、票务、支付和通知等模块的完整接口定义。

## 项目结构

```
src/
├── api/                    # API 接口定义
│   ├── index.ts           # 统一导出
│   ├── user.ts            # 用户相关接口
│   ├── show.ts            # 演出相关接口
│   ├── order.ts           # 订单相关接口
│   ├── ticket.ts          # 票务相关接口
│   ├── payment.ts         # 支付相关接口
│   └── notification.ts    # 通知相关接口
├── config/
│   └── api.ts             # API 配置文件
├── types/
│   └── index.ts           # TypeScript 类型定义
└── utils/
    └── request.ts         # HTTP 请求封装
```

## 环境配置

### 开发环境 (.env.development)
```env
VITE_API_BASE_URL=http://localhost:8000/api
VITE_APP_TITLE=抢票系统
NODE_ENV=development
VITE_DEBUG=true
VITE_WECHAT_APPID=your_wechat_appid_dev
```

### 生产环境 (.env.production)
```env
VITE_API_BASE_URL=https://api.ticketsystem.com/api
VITE_APP_TITLE=抢票系统
NODE_ENV=production
VITE_DEBUG=false
VITE_WECHAT_APPID=your_wechat_appid_prod
```

## API 模块说明

### 1. 用户模块 (user.ts)
- 用户登录/注册
- 微信登录
- 用户信息管理
- 密码修改
- 用户统计

### 2. 演出模块 (show.ts)
- 演出列表/详情
- 演出场次管理
- 场馆信息
- 座位区域和座位信息
- 演出搜索和推荐

### 3. 订单模块 (order.ts)
- 订单创建/查询
- 订单状态管理
- 订单支付/取消
- 订单确认和退款
- 用户订单统计

### 4. 票务模块 (ticket.ts)
- 座位锁定/释放
- 票务库存和价格
- 用户票务管理
- 票务验证
- 电子票和二维码

### 5. 支付模块 (payment.ts)
- 支付创建/查询
- 支付回调处理
- 退款申请/查询
- 支付方式管理
- 微信支付/支付宝集成

### 6. 通知模块 (notification.ts)
- 短信/邮件发送
- 站内消息管理
- 消息设置
- 推送订阅

## 使用示例

### 基础用法
```typescript
import { userApi, showApi, orderApi } from '@/api'

// 用户登录
const loginResult = await userApi.login({
  username: 'user@example.com',
  password: 'password123'
})

// 获取演出列表
const shows = await showApi.getShowList({
  page: 1,
  limit: 10,
  keyword: '演唱会'
})

// 创建订单
const order = await orderApi.createOrder({
  sessionId: 1,
  tickets: [{
    ticketTypeId: 1,
    seatIds: [1, 2],
    quantity: 2
  }],
  contactInfo: {
    name: '张三',
    phone: '13800138000'
  }
})
```

### 错误处理
```typescript
try {
  const result = await userApi.login(loginData)
  // 处理成功结果
} catch (error) {
  // 处理错误
  console.error('登录失败:', error.message)
}
```

## 类型定义

所有 API 接口都有完整的 TypeScript 类型定义，包括：
- 请求参数类型
- 响应数据类型
- 业务实体类型
- 枚举类型

## 配置说明

### API 配置 (config/api.ts)
- `API_CONFIG`: 基础配置（URL、超时、重试等）
- `API_ENDPOINTS`: 所有接口端点定义
- `HTTP_STATUS`: HTTP 状态码
- `BUSINESS_CODE`: 业务状态码

### 请求封装 (utils/request.ts)
- 统一的请求/响应处理
- 自动添加认证头
- 错误处理和重试机制
- 请求/响应拦截器

## 注意事项

1. **环境变量**: 确保在不同环境下设置正确的 `VITE_API_BASE_URL`
2. **认证**: 所有需要认证的接口会自动添加 Authorization 头
3. **错误处理**: 统一的错误处理机制，包括网络错误和业务错误
4. **类型安全**: 使用 TypeScript 确保类型安全
5. **微信小程序**: 适配微信小程序的网络请求限制

## 后端服务依赖

确保以下后端服务正常运行：
- API 网关: `http://localhost:8000`
- 用户服务
- 演出服务
- 订单服务
- 票务服务
- 支付服务
- 通知服务

## 开发建议

1. 在开发过程中，可以通过修改 `.env.development` 文件来切换不同的后端环境
2. 使用 TypeScript 的类型提示来确保 API 调用的正确性
3. 在生产环境部署前，确保更新 `.env.production` 中的 API 地址
4. 建议在调用 API 前进行参数验证，避免无效请求