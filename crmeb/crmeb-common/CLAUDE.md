[根目录](../../CLAUDE.md) > [crmeb](../) > **crmeb-common**

# crmeb-common 模块

## 模块职责

公共基础模块，定义所有实体模型、常量、工具类、异常处理、Redis 配置等基础设施代码。被其他所有后端模块依赖。

## 入口与启动

本模块为公共库，不独立运行，被其他模块引用。

### Maven 依赖

```xml
<dependency>
    <groupId>com.zbkj</groupId>
    <artifactId>crmeb-common</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## 包结构

```
com.zbkj.common/
├── acpect/              # AOP 切面
├── annotation/          # 自定义注解
├── config/              # 配置类 (Redis, CRMEB配置)
├── constants/           # 常量定义
├── enums/               # 枚举类
├── exception/           # 异常定义与处理
├── interceptor/         # 拦截器
├── model/               # 实体模型 (核心)
├── page/                # 分页相关
├── request/             # 请求对象
├── response/            # 响应对象
├── result/              # 统一返回结果
├── token/               # Token 相关
├── utils/               # 工具类
└── vo/                  # 视图对象
```

## 核心组件

### 实体模型 (model)

按业务域组织，对应数据库表:

| 包路径 | 业务域 | 主要实体 |
|-------|--------|---------|
| `model/product/` | 商品 | `StoreProduct`, `StoreProductAttr`, `StoreProductAttrValue` |
| `model/order/` | 订单 | `StoreOrder`, `StoreOrderInfo`, `StoreOrderStatus` |
| `model/user/` | 用户 | `User`, `UserAddress`, `UserBill`, `UserLevel` |
| `model/coupon/` | 优惠券 | `StoreCoupon`, `StoreCouponUser` |
| `model/bargain/` | 砍价 | `StoreBargain`, `StoreBargainUser` |
| `model/combination/` | 拼团 | `StoreCombination`, `StorePink` |
| `model/seckill/` | 秒杀 | `StoreSeckill`, `StoreSeckillManger` |
| `model/system/` | 系统 | `SystemAdmin`, `SystemRole`, `SystemConfig` |
| `model/wechat/` | 微信 | `WechatPayInfo`, `WechatCallback` |
| `model/finance/` | 财务 | `UserRecharge`, `UserExtract` |

### 常量定义 (constants)

| 常量类 | 用途 |
|-------|------|
| `Constants` | 通用常量 |
| `PayConstants` | 支付相关常量 |
| `ProductConstants` | 商品相关常量 |
| `UserConstants` | 用户相关常量 |
| `CouponConstants` | 优惠券相关常量 |
| `WeChatConstants` | 微信相关常量 |
| `RedisConstatns` | Redis 键名常量 |
| `SysConfigConstants` | 系统配置键名 |

### 工具类 (utils)

| 工具类 | 功能 |
|-------|------|
| `CrmebUtil` | 通用工具方法 |
| `DateUtil` | 日期处理 |
| `RedisUtil` | Redis 操作封装 |
| `RequestUtil` | HTTP 请求工具 |
| `ThreadPoolUtil` | 线程池工具 |
| `ValidateFormUtil` | 表单验证 |

### 异常处理 (exception)

```java
CrmebException          # 业务异常基类
ExceptionHandler        # 全局异常处理器
ExceptionCodeEnum       # 异常码枚举
```

### 统一响应 (result)

```java
CommonResult<T>         # 统一响应包装类
├── code               # 响应码
├── message            # 响应消息
└── data               # 响应数据

ResultAdvice            # 响应自动包装
```

## 关键依赖

```xml
<!-- Spring Boot 核心 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- MyBatis Plus -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
</dependency>

<!-- Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Swagger -->
<dependency>
    <groupId>io.springfox</groupId>
    <artifactId>springfox-swagger2</artifactId>
</dependency>

<!-- 云存储 -->
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
</dependency>
<dependency>
    <groupId>com.qcloud</groupId>
    <artifactId>cos_api</artifactId>
</dependency>
<dependency>
    <groupId>com.qiniu</groupId>
    <artifactId>qiniu-java-sdk</artifactId>
</dependency>
```

## 数据模型示例

### 商品实体 (StoreProduct)

```java
@Data
@TableName("eb_store_product")
public class StoreProduct {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String storeName;      // 商品名称
    private String image;          // 商品图片
    private BigDecimal price;      // 销售价格
    private BigDecimal otPrice;    // 原价
    private Integer stock;         // 库存
    private Integer sales;         // 销量
    private Boolean isShow;        // 是否显示
    // ...
}
```

### 订单实体 (StoreOrder)

```java
@Data
@TableName("eb_store_order")
public class StoreOrder {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String orderId;        // 订单号
    private Integer uid;           // 用户ID
    private BigDecimal totalPrice; // 订单总价
    private BigDecimal payPrice;   // 实付金额
    private Integer status;        // 订单状态
    private Integer paid;          // 支付状态
    // ...
}
```

## 配置类

### Redis 配置

```java
// config/RedisConfig.java
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate();
}
```

### CRMEB 配置

```java
// config/CrmebConfig.java
@Configuration
@ConfigurationProperties(prefix = "crmeb")
public class CrmebConfig {
    private String version;
    private String domain;
    private List<String> ignored;
    // ...
}
```

## 测试与质量

### 测试状态

当前模块未发现独立测试文件。

### 建议测试覆盖

1. 工具类单元测试
2. 日期处理边界测试
3. Redis 操作测试

## 常见问题 (FAQ)

### Q: 如何新增实体模型？

1. 在对应业务包 `model/{业务}/` 下创建实体类
2. 添加 `@Data` (Lombok) 和 `@TableName` (MyBatis-Plus) 注解
3. 字段使用 `@TableId`, `@TableField` 等注解

### Q: 如何新增常量？

1. 在 `constants/` 包下找到对应常量类
2. 或创建新的常量类

### Q: 如何使用 Redis？

注入 `RedisUtil` 使用:
```java
@Autowired
private RedisUtil redisUtil;

// 设置值
redisUtil.set("key", value);
// 获取值
Object value = redisUtil.get("key");
```

## 相关文件清单

```
crmeb-common/
├── src/main/java/com/zbkj/common/
│   ├── config/                       # 配置类
│   │   ├── CrmebConfig.java
│   │   └── RedisConfig.java
│   ├── constants/                    # 常量 (~25个)
│   │   ├── Constants.java
│   │   ├── PayConstants.java
│   │   └── ...
│   ├── exception/                    # 异常
│   │   ├── CrmebException.java
│   │   └── ExceptionHandler.java
│   ├── model/                        # 实体模型 (~60个)
│   │   ├── product/
│   │   ├── order/
│   │   ├── user/
│   │   └── ...
│   ├── request/                      # 请求对象
│   ├── response/                     # 响应对象
│   ├── result/                       # 统一结果
│   │   ├── CommonResult.java
│   │   └── ResultAdvice.java
│   ├── utils/                        # 工具类 (~20个)
│   │   ├── CrmebUtil.java
│   │   ├── DateUtil.java
│   │   └── RedisUtil.java
│   └── vo/                           # 视图对象
└── pom.xml
```

## 变更记录 (Changelog)

| 日期 | 变更内容 |
|-----|---------|
| 2026-01-18 | AI 架构师首次生成模块文档 |
