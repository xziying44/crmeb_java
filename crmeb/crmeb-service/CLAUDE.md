[根目录](../../CLAUDE.md) > [crmeb](../) > **crmeb-service**

# crmeb-service 模块

## 模块职责

核心业务服务层模块，封装所有业务逻辑，被 `crmeb-admin` 和 `crmeb-front` 两个 API 模块依赖。包含商品、订单、用户、营销、支付、微信等核心业务。

## 入口与启动

本模块为业务库，不独立运行，被其他模块引用。

### Maven 依赖

```xml
<dependency>
    <groupId>com.zbkj</groupId>
    <artifactId>crmeb-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## 对外接口

### Service 接口分类

| 业务域 | 接口数量 | 主要接口 |
|-------|---------|---------|
| 商品 | ~15 | `StoreProductService`, `StoreProductAttrService` |
| 订单 | ~10 | `StoreOrderService`, `OrderService`, `OrderPayService` |
| 用户 | ~15 | `UserService`, `UserBillService`, `UserAddressService` |
| 营销 | ~10 | `StoreCouponService`, `StoreBargainService`, `StoreCombinationService` |
| 支付 | ~5 | `WeChatPayService`, `RechargePayService` |
| 系统 | ~15 | `SystemConfigService`, `SystemAdminService` |
| 微信 | ~10 | `WechatPublicService`, `WechatNewService` |

## 关键依赖与配置

### 模块依赖

```xml
<dependency>
    <groupId>com.zbkj</groupId>
    <artifactId>crmeb-common</artifactId>
</dependency>
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
</dependency>
<dependency>
    <groupId>com.jayway.jsonpath</groupId>
    <artifactId>json-path</artifactId>
</dependency>
```

### 包结构

```
com.zbkj.service/
├── dao/                    # 数据访问层 (MyBatis Mapper)
├── service/                # 服务接口
├── service/impl/           # 服务实现
├── delete/                 # 待废弃工具类
└── exception/              # 异常处理
```

## 数据模型

### DAO 层

本模块的 DAO 继承 MyBatis-Plus 的 `BaseMapper<T>`，主要操作 `crmeb-common` 中定义的实体类。

### 核心 DAO 列表

| DAO | 实体 | 职责 |
|-----|------|------|
| `StoreProductDao` | `StoreProduct` | 商品数据访问 |
| `StoreOrderDao` | `StoreOrder` | 订单数据访问 |
| `UserDao` | `User` | 用户数据访问 |
| `StoreCouponDao` | `StoreCoupon` | 优惠券数据访问 |
| `SystemConfigDao` | `SystemConfig` | 系统配置数据访问 |

### MyBatis Mapper XML

位于 `src/main/resources/mapper/` 目录下，按业务模块组织:

```
mapper/
├── store/           # 商品相关
├── user/            # 用户相关
├── system/          # 系统相关
├── marketing/       # 营销相关
├── finance/         # 财务相关
└── wechat/          # 微信相关
```

## 核心业务逻辑

### 商品服务

```java
StoreProductService
├── getList()           # 商品列表
├── getDetail()         # 商品详情
├── save()              # 新增商品
├── update()            # 更新商品
├── updateStock()       # 更新库存
└── operateStock()      # 库存操作 (扣减/回滚)
```

### 订单服务

```java
OrderService
├── createOrder()       # 创建订单
├── payOrder()          # 订单支付
├── cancelOrder()       # 取消订单
├── refundOrder()       # 退款
└── deliverOrder()      # 发货

StoreOrderService
├── getList()           # 订单列表
├── getDetail()         # 订单详情
├── getStatusNum()      # 各状态数量
└── export()            # 订单导出
```

### 用户服务

```java
UserService
├── getInfo()           # 用户信息
├── register()          # 用户注册
├── updateUser()        # 更新用户
├── spread()            # 绑定推广关系
└── operationNowMoney() # 余额操作
```

### 支付服务

```java
WeChatPayService
├── jsApiPay()          # JSAPI 支付
├── h5Pay()             # H5 支付
├── appPay()            # APP 支付
└── refund()            # 退款

OrderPayService
├── payment()           # 统一支付入口
├── paySuccess()        # 支付成功回调
└── payFail()           # 支付失败处理
```

### 营销服务

```java
StoreCouponService      # 优惠券管理
StoreBargainService     # 砍价活动
StoreCombinationService # 拼团活动
StoreSeckillService     # 秒杀活动
RetailShopService       # 分销
```

## 测试与质量

### 测试状态

当前模块未发现独立测试文件。

### 建议测试覆盖

1. **订单创建流程** - 库存扣减、优惠计算、订单生成
2. **支付回调处理** - 订单状态更新、通知发送
3. **营销活动规则** - 秒杀库存、拼团人数、砍价进度

### 代码规范

- Service 接口定义在 `service/` 包
- Service 实现在 `service/impl/` 包，使用 `@Service` 注解
- 使用 `@Transactional` 管理事务
- 使用 `@Slf4j` 记录日志

## 常见问题 (FAQ)

### Q: 如何添加新的业务 Service？

1. 在 `service/` 包下创建接口
2. 在 `service/impl/` 包下创建实现类
3. 添加 `@Service` 注解
4. 需要时添加 `@Transactional` 注解

### Q: 如何新增数据库操作？

1. 在 `crmeb-common` 中定义实体类
2. 在 `dao/` 包下创建 DAO 接口，继承 `BaseMapper<T>`
3. 如需自定义 SQL，在 `resources/mapper/` 下创建 XML 文件

### Q: 事务如何管理？

使用 Spring 的 `@Transactional` 注解:
```java
@Transactional(rollbackFor = Exception.class)
public void createOrder() {
    // 涉及多表操作的业务逻辑
}
```

## 相关文件清单

```
crmeb-service/
├── src/main/java/com/zbkj/service/
│   ├── dao/                          # DAO层 (~70个)
│   │   ├── StoreProductDao.java
│   │   ├── StoreOrderDao.java
│   │   └── UserDao.java
│   ├── service/                      # 服务接口 (~80个)
│   │   ├── StoreProductService.java
│   │   ├── OrderService.java
│   │   └── UserService.java
│   ├── service/impl/                 # 服务实现 (~80个)
│   │   ├── StoreProductServiceImpl.java
│   │   ├── OrderServiceImpl.java
│   │   └── UserServiceImpl.java
│   └── exception/
│       └── GlobalExceptionHandler.java
├── src/main/resources/
│   └── mapper/                       # MyBatis XML (~80个)
│       ├── store/
│       ├── user/
│       ├── system/
│       └── ...
└── pom.xml
```

## 变更记录 (Changelog)

| 日期 | 变更内容 |
|-----|---------|
| 2026-01-18 | AI 架构师首次生成模块文档 |
