[根目录](../../CLAUDE.md) > [crmeb](../) > **crmeb-front**

# crmeb-front 模块

## 模块职责

移动端/H5 API 服务模块，为 uni-app 移动端应用提供 RESTful API 接口。主要处理用户端的商品浏览、购物车、订单、支付等业务。

## 入口与启动

### 主入口

```
src/main/java/com/zbkj/front/CrmebFrontApplication.java
```

### 启动命令

```bash
cd crmeb/crmeb-front
mvn spring-boot:run
```

### 默认配置

- **端口**: 8081 (通常配置)
- **上下文路径**: `/`
- **Swagger 文档**: `http://localhost:8081/doc.html`

## 对外接口

### API 前缀

所有移动端 API 以 `/api/front/` 为前缀。

### 核心 Controller

| Controller | 路径前缀 | 职责 |
|-----------|---------|------|
| `LoginController` | `/front` | 用户登录/注册 |
| `IndexController` | `/front/index` | 首页数据 |
| `ProductController` | `/front/product` | 商品列表/详情 |
| `CartController` | `/front/cart` | 购物车管理 |
| `StoreOrderController` | `/front/order` | 订单管理 |
| `PayController` | `/front/pay` | 支付接口 |
| `UserController` | `/front/user` | 用户中心 |
| `CouponController` | `/front/coupon` | 优惠券 |
| `BargainController` | `/front/bargain` | 砍价活动 |
| `CombinationController` | `/front/combination` | 拼团活动 |
| `SecKillController` | `/front/seckill` | 秒杀活动 |

### 公共接口

| 路径 | 说明 |
|-----|------|
| `/api/public/**` | 公共资源接口 |
| `/front/index/**` | 首页数据 (部分无需登录) |
| `/front/product/**` | 商品信息 (部分无需登录) |

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
| `application.yml` | 主配置 |
| `application-dev.yml` | 开发环境配置 |
| `application-prod.yml` | 生产环境配置 |
| `logback-spring.xml` | 日志配置 |

### IP 地址库

模块包含 `ip2region.xdb` 文件，用于根据 IP 地址获取地理位置信息。

## 安全认证

### Token 拦截器

- 配置类: `config/WebConfig.java`
- 拦截器: `interceptor/FrontTokenInterceptor.java`

### 认证流程

1. 用户通过微信授权或手机号登录
2. 服务端返回 Token
3. 前端在请求头添加 `Authori-zation: {token}`
4. `FrontTokenInterceptor` 验证 Token
5. 通过后访问业务接口

### 安全配置

- `CloseSecurityConfig.java` - 关闭 Spring Security 默认配置，使用自定义 Token 拦截

## 核心服务

### 模块特有 Service

| Service | 职责 |
|---------|------|
| `IndexService` | 首页数据聚合 |
| `LoginService` | 登录/注册逻辑 |
| `ProductService` | 商品前台展示逻辑 |
| `UserCenterService` | 用户中心数据 |

### 登录方式

1. **微信公众号授权登录**
2. **微信小程序登录**
3. **手机号验证码登录**
4. **账号密码登录**

## 数据模型

本模块不包含独立数据模型，使用 `crmeb-common` 中的公共模型。

## 测试与质量

### 测试状态

当前模块未发现独立测试文件，建议添加:
- Controller 集成测试
- 登录流程测试
- 支付回调测试

### 代码规范

- 使用 `@Api` 和 `@ApiOperation` 注解生成 Swagger 文档
- Controller 方法返回 `CommonResult<T>` 统一响应格式

## 常见问题 (FAQ)

### Q: 如何添加新的前端 API？

1. 在 `controller/` 下创建 Controller 类
2. 添加 `@RestController` 和 `@RequestMapping` 注解
3. 业务逻辑放在 `crmeb-service` 中

### Q: 如何配置不需要登录的接口？

在 `WebConfig.java` 的 `addInterceptors` 方法中配置 `excludePathPatterns`。

### Q: 微信支付回调地址是什么？

支付回调通过 `crmeb-admin` 的 `CallbackController` 处理。

## 相关文件清单

```
crmeb-front/
├── src/main/java/com/zbkj/front/
│   ├── CrmebFrontApplication.java    # 主入口
│   ├── config/                       # 配置类
│   │   ├── WebConfig.java            # Web配置
│   │   ├── CloseSecurityConfig.java  # 安全配置
│   │   └── SwaggerConfig.java        # Swagger配置
│   ├── controller/                   # 控制器 (~25个)
│   │   ├── LoginController.java      # 登录
│   │   ├── ProductController.java    # 商品
│   │   ├── CartController.java       # 购物车
│   │   ├── StoreOrderController.java # 订单
│   │   └── PayController.java        # 支付
│   ├── interceptor/                  # 拦截器
│   │   └── FrontTokenInterceptor.java
│   ├── pub/                          # 公共接口
│   └── service/                      # 模块特有服务
│       ├── IndexService.java
│       ├── LoginService.java
│       └── impl/
├── src/main/resources/
│   ├── application.yml               # 主配置
│   ├── application-*.yml             # 环境配置
│   ├── logback-spring.xml            # 日志配置
│   └── ip2region/ip2region.xdb       # IP地址库
└── pom.xml
```

## 变更记录 (Changelog)

| 日期 | 变更内容 |
|-----|---------|
| 2026-01-18 | AI 架构师首次生成模块文档 |
