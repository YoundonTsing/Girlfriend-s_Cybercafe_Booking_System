# 🎮 Girlfriend's Cybercafe Booking System

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0+-green.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.0+-4FC08D.svg)](https://vuejs.org/)
[![Redis](https://img.shields.io/badge/Redis-7.0+-red.svg)](https://redis.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue.svg)](https://www.mysql.com/)

> 🚀 **A comprehensive, enterprise-grade cybercafe booking system built with modern microservices architecture**

一个为网吧/网咖量身定制的现代化预订管理系统，采用微服务架构，支持高并发座位预订、实时库存管理和完整的支付流程。

## 📋 目录

- [✨ 项目特色](#-项目特色)
- [🏗️ 系统架构](#️-系统架构)
- [🛠️ 技术栈](#️-技术栈)
- [🚀 快速开始](#-快速开始)
- [📁 项目结构](#-项目结构)
- [🔧 配置指南](#-配置指南)
- [📊 性能监控](#-性能监控)
- [🐛 问题排查](#-问题排查)
- [🤝 贡献指南](#-贡献指南)
- [📄 许可证](#-许可证)

## ✨ 项目特色

### 🎯 核心功能
- **🪑 智能座位管理**: 实时座位状态同步，支持座位预订和释放
- **📦 高并发库存控制**: Redis + Lua脚本实现原子化库存操作
- **💳 完整支付流程**: 集成多种支付方式，支持订单状态追踪
- **👥 用户权限管理**: 基于JWT的用户认证和权限控制
- **📱 多端适配**: Web端 + 微信小程序双端支持
- **🔄 实时通信**: WebSocket实时推送座位状态变更

### 🏆 技术亮点
- **微服务架构**: 服务解耦，独立部署，易于扩展
- **分布式锁**: Redisson实现的分布式锁机制
- **读写分离**: 数据库读写分离，提升查询性能
- **连接池优化**: HikariCP连接池，支持高并发访问
- **缓存策略**: 多级缓存设计，Redis + 本地缓存
- **监控体系**: 完整的性能监控和日志追踪

## 🏗️ 系统架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Web Frontend  │    │  WeChat Mini    │    │   Admin Panel   │
│    (Vue.js)     │    │   Program       │    │    (Vue.js)     │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
                    ┌─────────────┴─────────────┐
                    │     API Gateway           │
                    │   (Spring Cloud Gateway)  │
                    └─────────────┬─────────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          │                       │                       │
    ┌─────┴─────┐         ┌─────┴─────┐         ┌─────┴─────┐
    │   User    │         │   Order   │         │   Show    │
    │  Service  │         │  Service  │         │  Service  │
    └─────┬─────┘         └─────┬─────┘         └─────┬─────┘
          │                     │                     │
          └─────────────────────┼─────────────────────┘
                                │
                    ┌─────────────┴─────────────┐
                    │     Data Layer            │
                    │  MySQL + Redis + MQ       │
                    └───────────────────────────┘
```

## 🛠️ 技术栈

### 后端技术
- **框架**: Spring Boot 3.0+, Spring Cloud Gateway
- **数据库**: MySQL 8.0+ (主从复制)
- **缓存**: Redis 7.0+ (Redisson客户端)
- **消息队列**: RocketMQ
- **连接池**: HikariCP
- **认证**: JWT + Spring Security
- **文档**: Swagger/OpenAPI 3.0

### 前端技术
- **Web端**: Vue.js 3.0+, TypeScript, Vite
- **小程序**: uni-app框架
- **UI组件**: Element Plus / uni-ui
- **状态管理**: Pinia
- **HTTP客户端**: Axios

### 运维技术
- **容器化**: Docker, Docker Compose
- **监控**: Prometheus + Grafana
- **日志**: ELK Stack
- **CI/CD**: GitHub Actions

## 🚀 快速开始

### 环境要求
- Java 17+
- Node.js 16+
- MySQL 8.0+
- Redis 7.0+
- Maven 3.8+

### 1. 克隆项目
```bash
git clone https://github.com/YoundonTsing/Girlfriend-s_Cybercafe_Booking_System.git
cd Girlfriend-s_Cybercafe_Booking_System
```

### 2. 数据库初始化
```bash
# 执行数据库初始化脚本
mysql -u root -p < sql/init_all_databases.sql
```

### 3. 配置环境变量
```bash
# 复制环境配置文件
cp env.example .env

# 编辑配置文件，设置数据库和Redis连接信息
vim .env
```

### 4. 启动后端服务
```bash
# 启动网关服务
cd ticket-gateway && mvn spring-boot:run

# 启动用户服务
cd ticket-user && mvn spring-boot:run

# 启动订单服务
cd ticket-order && mvn spring-boot:run

# 启动演出服务
cd ticket-show && mvn spring-boot:run
```

### 5. 启动前端应用
```bash
# Web端
cd ticket-frontend
npm install
npm run dev

# 微信小程序
cd ticket-frontend-wx
npm install
npm run dev:mp-weixin
```

### 6. 访问应用
- Web端: http://localhost:3000
- API文档: http://localhost:8080/swagger-ui.html
- 管理后台: http://localhost:3001

## 📁 项目结构

```
Girlfriend-s_Cybercafe_Booking_System/
├── 📁 ticket-gateway/          # API网关服务
├── 📁 ticket-user/             # 用户管理服务
├── 📁 ticket-order/            # 订单管理服务
├── 📁 ticket-show/             # 演出/座位管理服务
├── 📁 ticket-common/           # 公共组件库
├── 📁 ticket-frontend/         # Web前端应用
├── 📁 ticket-frontend-wx/      # 微信小程序
├── 📁 sql/                     # 数据库脚本
├── 📁 docs/                    # 项目文档
├── 📁 test/                    # 测试脚本
├── 📁 docker/                  # Docker配置
├── 📁 bug_management/          # Bug管理文档
├── 📄 pom.xml                  # Maven父项目配置
├── 📄 README.md               # 项目说明文档
└── 📄 .gitignore              # Git忽略配置
```

## 🔧 配置指南

### 数据库配置
```yaml
# application.yml
spring:
  datasource:
    write:
      url: jdbc:mysql://localhost:3306/ticket_db
      username: ${DB_USERNAME:root}
      password: ${DB_PASSWORD:password}
    read:
      url: jdbc:mysql://localhost:3307/ticket_db
      username: ${DB_USERNAME:root}
      password: ${DB_PASSWORD:password}
```

### Redis配置
```yaml
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    database: 0
```

### 详细配置说明
请参考各服务目录下的 `CONFIGURATION_GUIDE.md` 文件。

## 📊 性能监控

### 关键指标监控
- **QPS**: 每秒查询数
- **响应时间**: P95, P99响应时间
- **错误率**: 4xx, 5xx错误统计
- **连接池**: 活跃连接数监控
- **缓存命中率**: Redis缓存效率

### 监控面板
- Grafana Dashboard: http://localhost:3000/grafana
- 应用监控: http://localhost:8080/actuator

## 🐛 问题排查

### 常见问题

#### 1. 订单创建失败
```bash
# 检查Redis连接
redis-cli ping

# 查看订单服务日志
tail -f logs/ticket-order.log
```

#### 2. 座位锁定异常
```bash
# 检查分布式锁状态
redis-cli keys "seat:lock:*"

# 清理过期锁
python test/redis_stability_test.py
```

#### 3. 数据库连接池耗尽
```bash
# 监控连接池状态
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

### 详细排查指南
参考 `bug_management/` 目录下的具体问题排查文档。

## 🧪 测试

### 运行单元测试
```bash
mvn test
```

### 运行集成测试
```bash
# 并发测试
python test/concurrent_booking_test_1000.py

# 性能测试
python test/performance/load_test.py
```

### 压力测试
```bash
# 使用Apache Bench
ab -n 1000 -c 100 http://localhost:8080/api/orders

# 使用JMeter
cd test/jmeter && jmeter -n -t booking_test.jmx
```

## 🚀 部署

### Docker部署
```bash
# 构建镜像
docker-compose build

# 启动服务
docker-compose up -d
```

### 生产环境部署
详细部署文档请参考 `docs/deployment.md`

## 🤝 贡献指南

我们欢迎所有形式的贡献！请遵循以下步骤：

1. **Fork** 本仓库
2. **创建** 特性分支 (`git checkout -b feature/AmazingFeature`)
3. **提交** 更改 (`git commit -m 'Add some AmazingFeature'`)
4. **推送** 到分支 (`git push origin feature/AmazingFeature`)
5. **创建** Pull Request

### 代码规范
- 遵循阿里巴巴Java开发手册
- 使用ESLint进行前端代码检查
- 提交前运行测试用例

### 提交信息规范
```
type(scope): description

feat: 新功能
fix: 修复bug
docs: 文档更新
style: 代码格式调整
refactor: 代码重构
test: 测试相关
chore: 构建过程或辅助工具的变动
```

## 📈 路线图

- [ ] **v2.0**: 支持多网吧连锁管理
- [ ] **v2.1**: 集成AI智能推荐系统
- [ ] **v2.2**: 支持VR设备预订
- [ ] **v3.0**: 云原生架构升级

## 👥 团队

- **项目负责人**: YoundonTsing
- **架构师**: YoundonTsing
- **前端开发**: YoundonTsing
- **后端开发**: YoundonTsing

## 📞 联系我们

- **Email**: 1939194239@qq.com
- **GitHub**: [@YoundonTsing](https://github.com/YoundonTsing)
- **Issues**: [GitHub Issues](https://github.com/YoundonTsing/Girlfriend-s_Cybercafe_Booking_System/issues)

## 📄 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。

---

<div align="center">

**⭐ 如果这个项目对您有帮助，请给我们一个Star！⭐**

Made with ❤️ for girlfriend's cybercafe business

</div>