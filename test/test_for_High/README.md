# 订单创建功能综合测试套件

## 📋 项目概述

本测试套件专门用于测试票务系统订单创建功能的完整流程，涵盖JWT认证、正常流程、异常场景、并发测试等多个维度，确保订单创建的10个核心步骤都能正常工作。

## 🎯 测试目标

### 核心测试步骤
1. **分布式锁获取** - 防止重复提交
2. **参数校验** - 确保数据合法性
3. **票价信息获取** - RPC调用验证
4. **订单号生成** - 唯一性验证
5. **Redis预减库存** - 高并发库存控制
6. **演出信息获取** - 数据完整性
7. **订单实体构建** - 业务逻辑验证
8. **订单持久化** - 数据库操作
9. **分布式锁释放** - 资源清理
10. **订单号返回** - 响应完整性

### 测试维度
- ✅ **JWT认证测试** - 用户登录和token验证
- ✅ **正常流程测试** - 标准订单创建流程
- ⚠️ **异常场景测试** - 各种错误情况处理
- 🚀 **并发测试** - 分布式锁和库存超卖验证
- 📊 **性能测试** - 响应时间和吞吐量

## 🛠️ 环境要求

### Python环境
- Python 3.7+
- pip包管理器

### 系统依赖
```bash
pip install -r requirements.txt
```

### 服务环境
- 票务系统后端服务运行在 `http://localhost:8000`
- 测试用户：`admin/123456`

## 🚀 快速开始

### 1. 安装依赖
```bash
cd D:\Tickets\test\test_for_High
pip install -r requirements.txt
```

### 2. 配置测试环境
编辑 `test_config.json` 文件，修改服务器地址和测试参数：
```json
{
  "server_config": {
    "base_url": "http://localhost:8000"
  },
  "test_credentials": {
    "admin_user": {
      "username": "admin",
      "password": "123456"
    }
  }
}
```

### 3. 运行测试

#### 运行所有测试
```bash
python run_tests.py --all
```

#### 运行特定测试
```bash
# JWT认证测试
python run_tests.py --auth

# 正常流程测试
python run_tests.py --normal

# 异常场景测试
python run_tests.py --exception

# 并发测试
python run_tests.py --concurrent
```

#### 使用自定义配置
```bash
python run_tests.py --all --config my_config.json
```

### 4. 直接运行测试程序
```bash
python order_creation_comprehensive_test.py
```

## 📁 文件结构

```
D:\Tickets\test\test_for_High\
├── order_creation_comprehensive_test.py  # 主测试程序
├── run_tests.py                         # 测试启动脚本
├── test_config.json                     # 测试配置文件
├── requirements.txt                     # Python依赖
├── README.md                           # 项目说明
├── reports/                            # 测试报告目录
└── logs/                              # 测试日志目录
```

## 🧪 测试用例详解

### JWT认证测试
- **test_01_jwt_authentication**: 用户登录获取JWT token
- **test_02_jwt_validation**: JWT token有效性验证

### 正常流程测试
- **test_03_order_creation_normal_flow**: 完整订单创建流程
- **test_04_order_creation_steps_validation**: 10个核心步骤详细验证

### 异常场景测试
- **无效演出ID测试**: 验证系统对无效演出ID的处理
- **无效票档ID测试**: 验证系统对无效票档ID的处理
- **无效数量测试**: 验证数量参数校验
- **库存不足测试**: 验证库存不足时的处理逻辑
- **未授权访问测试**: 验证JWT认证机制
- **缺少参数测试**: 验证必要参数校验

### 并发测试
- **分布式锁测试**: 验证同用户并发请求的锁机制
- **并发订单创建**: 多用户并发抢票场景
- **库存超卖验证**: 确保高并发下不会超卖

## 📊 测试报告

### 报告格式
测试完成后会生成以下格式的报告：
- **JSON格式**: 详细的测试数据和结果
- **日志文件**: 完整的测试执行日志

### 报告内容
- 测试总结（通过率、执行时间等）
- 分类测试结果（认证、正常流程、异常、并发）
- 详细测试结果（每个测试用例的执行情况）
- 性能指标（响应时间、并发性能等）

### 报告位置
- JSON报告: `D:\Tickets\test\test_for_High\order_creation_test_report_YYYYMMDD_HHMMSS.json`
- 日志文件: `D:\Tickets\test\test_for_High\order_creation_test.log`

## ⚙️ 配置说明

### 服务器配置
```json
"server_config": {
  "base_url": "http://localhost:8000",
  "timeout": 10
}
```

### 并发测试配置
```json
"concurrent_config": {
  "concurrent_users": 5,
  "concurrent_requests": 20,
  "request_interval": 0.1
}
```

### 性能阈值配置
```json
"performance_config": {
  "max_response_time": 5.0,
  "min_success_rate": 80.0
}
```

## 🔧 故障排除

### 常见问题

1. **连接超时**
   - 检查服务器是否启动
   - 确认服务器地址配置正确
   - 检查网络连接

2. **认证失败**
   - 确认用户名密码正确
   - 检查用户服务是否正常
   - 验证JWT配置

3. **测试失败**
   - 查看详细日志文件
   - 检查测试数据是否有效
   - 确认业务逻辑是否正确

### 调试模式
```bash
python run_tests.py --all --verbose
```

## 📈 性能基准

### 预期性能指标
- **响应时间**: < 5秒
- **成功率**: > 80%
- **并发处理**: 支持5个并发用户
- **吞吐量**: 20个请求/分钟

### 性能优化建议
1. 调整并发参数
2. 优化数据库连接
3. 增加Redis缓存
4. 使用连接池

## 🤝 贡献指南

### 添加新测试用例
1. 在 `OrderCreationComprehensiveTest` 类中添加新方法
2. 遵循命名规范：`test_XX_description`
3. 使用 `log_test_result` 记录结果
4. 更新测试配置文件

### 修改测试配置
1. 编辑 `test_config.json`
2. 添加新的测试参数
3. 更新文档说明

## 📞 技术支持

如有问题或建议，请联系开发团队或查看项目文档。

---

**版本**: 1.0.0  
**更新时间**: 2024年12月  
**维护团队**: 票务系统开发组