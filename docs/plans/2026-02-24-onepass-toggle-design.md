# 一号通可选开关设计

## 背景

当前系统中一号通（短信、物流查询、电子面单、商家寄件、产品复制）功能没有开关控制。当 accessKey/secretKey 未配置或不正确时，打开订单发货页面会触发自动请求并报错"一号通平台接口接口请求失败:accessKey或者secretKey不正确"，影响正常使用。

## 目标

将一号通作为可选功能，默认关闭。关闭时所有一号通相关请求不发出，不报错。

## 设计方案

### 数据层

在 `eb_system_config` 表新增配置：

| 字段 | 值 |
|-----|-----|
| key | `one_pass_status` |
| value | `"0"` (默认关闭) |
| 含义 | `"0"` = 关闭，`"1"` = 开启 |

在 `OnePassConstants.java` 中添加常量：

```java
public static final String ONE_PASS_STATUS = "one_pass_status";
```

### 后端改动

#### OnePassUtil 统一拦截

新增 `isEnabled()` 方法：

```java
public boolean isEnabled() {
    String status = systemConfigService.getValueByKey(OnePassConstants.ONE_PASS_STATUS);
    return "1".equals(status);
}
```

修改 `getLoginVo()` 前置检查：

```java
public OnePassLoginVo getLoginVo() {
    if (!isEnabled()) {
        throw new CrmebException("一号通功能未启用，请在设置中开启");
    }
    // 原有逻辑不变
}
```

#### 新增查询开关状态 API

在 `OnePassController.java` 中：

```java
@ApiOperation(value = "获取一号通开关状态")
@RequestMapping(value = "/status", method = RequestMethod.GET)
public CommonResult<Boolean> getOnePassStatus() {
    return CommonResult.success(onePassService.isEnabled());
}
```

在 `OnePassService` 接口和 `OnePassServiceImpl` 中新增 `isEnabled()` 方法。

### 前端改动

#### 配置页面 (`sms/smsConfig/config.vue`)

在 FormGenerator 表单上方添加 `<el-switch>` 开关，关闭时下方表单灰显。

#### 订单发货页面 (`order/orderSend.vue`)

`mounted()` 中先查询一号通状态，关闭时跳过商家寄件快递公司列表请求。

#### 其他依赖一号通的入口

先查状态，关闭时显示提示"请先在设置中启用一号通服务"。

### 错误处理

| 场景 | 行为 |
|-----|------|
| 关闭 + 前端主动调用 | 前端先查状态，显示提示，不发请求 |
| 关闭 + 后端兜底 | `getLoginVo()` 抛出友好异常 |
| 开启 + key 错误 | 保持原有行为 |
| 查询状态接口失败 | 前端静默处理，不阻塞页面 |

### 影响范围

- 不影响：订单列表、订单详情、常规发货
- 受控影响：商家寄件、产品复制、短信服务 — 关闭时提示而非报错

### 测试要点

1. 默认关闭状态：打开订单发货页面不报错
2. 开关切换：开启后正常调用一号通 API
3. 前端降级：关闭时点击商家寄件 Tab 显示提示
4. 后端兜底：绕过前端直接调用后端 API 返回友好错误
