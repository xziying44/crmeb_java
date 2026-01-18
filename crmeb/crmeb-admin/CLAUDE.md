[根目录](../../CLAUDE.md) > [crmeb](../) > **crmeb-admin**

# crmeb-admin 模块

## 模块职责

管理端 API 服务模块，为 PC 管理后台提供 RESTful API 接口。包含用户认证、权限控制、定时任务调度等功能。

## 入口与启动

### 主入口

```
src/main/java/com/zbkj/admin/CrmebAdminApplication.java
```

### 启动命令

```bash
cd crmeb/crmeb-admin
mvn spring-boot:run
```

### 默认配置

- **端口**: 8080
- **上下文路径**: `/`
- **Swagger 文档**: `http://localhost:8080/doc.html`

## 对外接口

### API 前缀

所有管理端 API 以 `/api/admin/` 为前缀。

### 核心 Controller

| Controller | 路径前缀 | 职责 |
|-----------|---------|------|
| `AdminLoginController` | `/admin` | 管理员登录/登出 |
| `StoreProductController` | `/admin/store/product` | 商品管理 |
| `StoreOrderController` | `/admin/store/order` | 订单管理 |
| `UserController` | `/admin/user` | 用户管理 |
| `StoreCouponController` | `/admin/marketing/coupon` | 优惠券管理 |
| `SystemAdminController` | `/admin/system/admin` | 管理员管理 |
| `SystemRoleController` | `/admin/system/role` | 角色权限管理 |
| `ScheduleJobController` | `/admin/system/schedule-job` | 定时任务管理 |

### 公共接口 (无需登录)

| 路径 | 说明 |
|-----|------|
| `/api/public/**` | 公共资源接口 |
| `/api/admin/pagediy/info` | 页面 DIY 信息 |
| `/swagger-ui/**` | Swagger 文档 |
| `/druid/**` | Druid 监控 |

## 关键依赖与配置

### 模块依赖

```xml
<dependency>
    <groupId>com.zbkj</groupId>
    <artifactId>crmeb-service</artifactId>
</dependency>
```

### 核心配置文件

| 文件 | 用途 |
|-----|------|
| `application.yml` | 主配置（端口、数据源、Redis） |
| `application-dev.yml` | 开发环境配置 |
| `application-prod.yml` | 生产环境配置 |
| `logback-spring.xml` | 日志配置 |

### 关键配置项

```yaml
# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/single_open
    username: xxx
    password: xxx

# Redis 配置
spring:
  redis:
    host: 127.0.0.1
    port: 6379
    password: xxx
    database: 6

# Quartz 定时任务
spring:
  quartz:
    job-store-type: JDBC

# 安全路径白名单
crmeb:
  ignored:
    - api/public/**
    - swagger-ui/**
```

## 数据模型

### 模块特有模型

| 模型类 | 表名 | 说明 |
|-------|------|------|
| `ScheduleJob` | `schedule_job` | 定时任务配置 |
| `ScheduleJobLog` | `schedule_job_log` | 定时任务执行日志 |

## 安全认证

### Spring Security 配置

- 配置类: `config/WebSecurityConfig.java`
- JWT 过滤器: `filter/JwtAuthenticationTokenFilter.java`
- Token 组件: `filter/TokenComponent.java`

### 认证流程

1. 用户通过 `/admin/login` 登录
2. 服务端返回 JWT Token
3. 前端在请求头添加 `Authorization: Bearer {token}`
4. `JwtAuthenticationTokenFilter` 验证 Token
5. 通过后访问业务接口

## 定时任务

### Quartz 配置

- 配置类: `config/SchedulerConfig.java`
- 任务执行: `quartz/QuartzJob.java`
- 任务管理: `quartz/ScheduleManager.java`

### 内置任务

| 任务 | 说明 |
|-----|------|
| 订单超时取消 | 自动取消超时未支付订单 |
| 拼团状态更新 | 更新拼团活动状态 |
| 秒杀商品上下架 | 自动处理秒杀商品状态 |

## 测试与质量

### 测试状态

当前模块未发现独立测试文件，建议添加:
- Controller 集成测试
- 定时任务单元测试

### 代码规范

- 使用 `@Api` 和 `@ApiOperation` 注解生成 Swagger 文档
- Controller 方法返回 `CommonResult<T>` 统一响应格式
- 异常统一由 `GlobalExceptionHandler` 处理

## 常见问题 (FAQ)

### Q: 如何添加新的管理端 API？

1. 在 `controller/` 下创建 Controller 类
2. 添加 `@RestController` 和 `@RequestMapping` 注解
3. 注入对应的 Service
4. 添加 Swagger 注解

### Q: 如何配置接口白名单？

在 `application.yml` 的 `crmeb.ignored` 下添加路径模式。

### Q: 如何添加定时任务？

1. 在管理后台「维护 > 定时任务」中添加
2. 或直接操作 `schedule_job` 表

## 相关文件清单

```
crmeb-admin/
├── src/main/java/com/zbkj/admin/
│   ├── CrmebAdminApplication.java    # 主入口
│   ├── config/                       # 配置类
│   │   ├── WebSecurityConfig.java    # 安全配置
│   │   ├── SwaggerConfig.java        # Swagger配置
│   │   └── SchedulerConfig.java      # Quartz配置
│   ├── controller/                   # 控制器 (~70个)
│   ├── filter/                       # 过滤器
│   │   └── JwtAuthenticationTokenFilter.java
│   ├── quartz/                       # 定时任务
│   └── service/                      # 模块特有服务
├── src/main/resources/
│   ├── application.yml               # 主配置
│   ├── application-*.yml             # 环境配置
│   ├── logback-spring.xml            # 日志配置
│   └── mapper/                       # MyBatis映射文件
└── pom.xml
```

## 变更记录 (Changelog)

| 日期 | 变更内容 |
|-----|---------|
| 2026-01-18 | AI 架构师首次生成模块文档 |
