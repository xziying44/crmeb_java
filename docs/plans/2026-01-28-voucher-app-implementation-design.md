# App 端代金券功能实现设计方案

> 生成时间: 2026-01-28
> 项目版本: CRMEB Java v1.4
> 设计目的: 补齐 App 移动端代金券展示、选择和购买功能

---

## 一、背景与问题

### 1.1 现状分析

代金券功能后端已 100% 实现，但 App 移动端完全未适配：

| 层级 | 状态 | 说明 |
|------|------|------|
| 数据模型 | ✅ 已完成 | `couponType`、`canBuy`、`price`、`canDeductFreight` 字段已定义 |
| 计算逻辑 | ✅ 已完成 | 代金券抵扣 + 优惠券叠加逻辑已实现 |
| 后台管理 | ✅ 已完成 | 代金券 CRUD、发放功能完整 |
| App 前端 | ❌ 未实现 | 用户无法查看、购买、使用代金券 |

### 1.2 用户痛点

- 用户优惠券列表无法区分代金券和优惠券
- 结算页无代金券选择入口，无法叠加使用
- 无代金券购买渠道

---

## 二、设计决策

| 功能点 | 设计选择 | 说明 |
|--------|----------|------|
| 我的优惠券展示 | 混合列表 + 视觉标签区分 | 优惠券和代金券在同一列表，通过角标/颜色区分 |
| 结算页选择 | 双入口分离 | 保留现有"优惠券"入口，新增独立"代金券"入口 |
| 代金券购买支付 | 生成虚拟订单 | 复用现有订单支付流程，支持所有支付方式 |
| 购买入口 | 运营位引导 | 通过首页广告位/活动弹窗配置入口，灵活可控 |
| 购买限制 | 复用限量逻辑 | 使用 `isLimited` + `total` 控制总量，先到先得 |

---

## 三、功能范围

```
App 代金券完整功能
├── 1. 我的优惠券页面改造
│   └── 混合列表 + 视觉标签区分优惠券/代金券
├── 2. 结算页双入口
│   ├── 优惠券选择（现有）
│   └── 代金券选择（新增）
└── 3. 代金券购买
    ├── 独立购买页面（运营位入口）
    ├── 生成虚拟订单 → 复用支付流程
    └── 限量控制（isLimited + total）
```

---

## 四、数据流设计

### 4.1 代金券购买流程

```
用户点击运营位 → 代金券购买页 → 选择代金券 → 生成虚拟订单
    → 调用支付（微信/余额）→ 支付成功回调 → 自动发放代金券到用户账户
```

### 4.2 代金券使用流程

```
用户下单 → 结算页选择代金券（独立入口）→ 计算抵扣金额
    → 优惠券 + 代金券可叠加 → 提交订单 → 支付
```

---

## 五、接口设计

### 5.1 新增后端接口

| 接口 | 方法 | 说明 |
|-----|------|------|
| `/api/front/voucher/buyList` | GET | 可购买的代金券列表 |
| `/api/front/voucher/buy` | POST | 购买代金券（生成虚拟订单） |
| `/api/front/voucher/mine` | GET | 我的代金券列表 |
| `/api/front/voucher/order/{preOrderNo}` | GET | 订单可用代金券列表 |

### 5.2 Controller 定义

**文件**: `crmeb-front/src/main/java/com/zbkj/front/controller/VoucherController.java`

```java
@Api(tags = "代金券")
@RestController
@RequestMapping("api/front/voucher")
public class VoucherController {

    // 可购买的代金券列表（canBuy=true, couponType=2, status=1）
    @GetMapping("/buyList")
    public CommonResult<List<StoreCouponFrontResponse>> getBuyList();

    // 购买代金券 → 生成虚拟订单
    @PostMapping("/buy")
    public CommonResult<VoucherBuyResponse> buy(@RequestBody VoucherBuyRequest request);

    // 我的代金券列表（couponType=2 的 StoreCouponUser）
    @GetMapping("/mine")
    public CommonResult<List<StoreCouponUserResponse>> getMine(
        @RequestParam String type  // usable / unusable
    );

    // 订单可用代金券
    @GetMapping("/order/{preOrderNo}")
    public CommonResult<List<StoreCouponUserResponse>> getOrderVouchers(
        @PathVariable String preOrderNo
    );
}
```

### 5.3 购买逻辑核心实现

**VoucherService.buy() 实现要点**：

```
1. 校验代金券有效性
   ├── couponType = 2（代金券）
   ├── canBuy = true（可购买）
   ├── status = true（已开启）
   └── lastTotal > 0（有库存，若 isLimited=true）

2. 生成虚拟订单
   ├── 订单类型标记为"代金券购买"（type=6）
   ├── 订单金额 = coupon.price
   ├── 关联代金券ID
   └── 复用 StoreOrder 表

3. 返回订单号 → 前端调用现有支付接口

4. 支付成功回调中判断订单类型
   ├── 若为代金券购买订单（type=6）
   └── 调用 StoreCouponUserService.receiveCoupon() 发放代金券
```

### 5.4 订单类型扩展

**StoreOrder.type 字段扩展**：
- 0: 普通订单（现有）
- 1: 秒杀订单（现有）
- 2: 砍价订单（现有）
- 3: 拼团订单（现有）
- **6: 代金券购买订单（新增）**

---

## 六、前端设计

### 6.1 我的优惠券页面改造

**文件**: `app/pages/users/user_coupon/index.vue`

**改造要点**：
- 调用接口同时获取优惠券和代金券
- 列表项增加类型标签区分

```vue
<!-- 列表项模板增加类型标签 -->
<view class="coupon-item">
  <!-- 新增：类型角标 -->
  <view class="type-tag" :class="item.couponType === 2 ? 'voucher' : 'coupon'">
    {{ item.couponType === 2 ? '代金券' : '优惠券' }}
  </view>

  <!-- 现有内容保持不变 -->
  <view class="money">￥{{ item.money }}</view>
  <view class="condition">{{ item.couponType === 2 ? '无门槛' : '满' + item.minPrice + '可用' }}</view>
</view>
```

**样式区分**：
- 优惠券：橙色系（现有风格）
- 代金券：绿色系 + "代金券"角标

### 6.2 结算页双入口

**文件**: `app/pages/order/order_confirm/index.vue`

**改造要点**：
- 在现有"优惠券"入口下方新增"代金券"入口
- 两个入口独立选择，可同时使用

```vue
<!-- 现有优惠券入口 -->
<view class='item' @tap='couponTap'>
  <view>优惠券</view>
  <view class='discount'>{{ couponTitle }}<text class='iconfont icon-jiantou'></text></view>
</view>

<!-- 新增：代金券入口 -->
<view class='item' @tap='voucherTap'>
  <view>代金券</view>
  <view class='discount'>{{ voucherTitle }}<text class='iconfont icon-jiantou'></text></view>
</view>
```

**价格展示**：
```
商品总价                    ¥200.00
满减活动                    -¥30.00
优惠券                      -¥20.00
代金券                      -¥15.00  ← 新增行
运费                        ¥10.00
─────────────────────────────────────
实付金额                    ¥145.00
```

### 6.3 代金券选择弹窗

**新建文件**: `app/components/voucherListWindow/index.vue`

**功能**：
- 展示用户可用的代金券列表
- 单选，选中后返回结算页
- 显示面值、有效期、是否可抵扣运费

### 6.4 代金券购买页

**新建文件**: `app/pages/activity/voucher_buy/index.vue`

**页面结构**：
```
┌─────────────────────────────────────────┐
│  ← 代金券购买                            │  导航栏
├─────────────────────────────────────────┤
│                                         │
│  ┌─────────────────────────────────┐   │
│  │  ¥50        售价 ¥39            │   │  代金券卡片
│  │  面值       限量 100张           │   │
│  │  无门槛 · 可抵扣运费             │   │
│  │                      [立即购买]  │   │
│  └─────────────────────────────────┘   │
│                                         │
│  ┌─────────────────────────────────┐   │
│  │  ¥30        售价 ¥25            │   │
│  │  面值       剩余 56张            │   │
│  │  无门槛                          │   │
│  │                      [立即购买]  │   │
│  └─────────────────────────────────┘   │
│                                         │
│  ┌─────────────────────────────────┐   │
│  │  ¥20        售价 ¥15            │   │  库存为0时
│  │  面值       已售罄               │   │
│  │  无门槛                          │   │
│  │                      [已售罄]    │   │  按钮置灰
│  └─────────────────────────────────┘   │
│                                         │
└─────────────────────────────────────────┘
```

**购买流程交互**：
```
1. 点击"立即购买"
       ↓
2. 弹出确认弹窗
   "确认花费 ¥39 购买面值 ¥50 的代金券？"
   [取消] [确认购买]
       ↓
3. 调用 /api/front/voucher/buy
       ↓
4. 返回虚拟订单号 → 跳转支付页（复用现有收银台）
       ↓
5. 支付成功 → 提示"购买成功，代金券已到账"
       ↓
6. 可选：跳转"我的优惠券"查看
```

### 6.5 API 封装

**文件**: `app/api/api.js`

```javascript
// ========== 代金券相关 ==========

// 可购买的代金券列表
export function getVoucherBuyList() {
  return request.get('voucher/buyList');
}

// 购买代金券（生成虚拟订单）
export function buyVoucher(couponId) {
  return request.post('voucher/buy', { couponId });
}

// 我的代金券列表
export function getMyVouchers(type) {
  return request.get('voucher/mine', { type }); // usable / unusable
}

// 订单可用代金券列表（结算页使用）
export function getOrderVouchers(preOrderNo) {
  return request.get(`voucher/order/${preOrderNo}`);
}
```

### 6.6 路由配置

**文件**: `app/pages.json`

```json
{
  "path": "pages/activity/voucher_buy/index",
  "style": { "navigationBarTitleText": "代金券购买" }
}
```

---

## 七、实现清单

| 序号 | 模块 | 文件 | 改动类型 |
|-----|------|------|----------|
| **后端** |
| 1 | 代金券 Controller | `crmeb-front/.../VoucherController.java` | 新增 |
| 2 | 代金券 Service | `crmeb-service/.../VoucherService.java` | 新增 |
| 3 | 代金券 ServiceImpl | `crmeb-service/.../VoucherServiceImpl.java` | 新增 |
| 4 | 支付回调扩展 | `crmeb-service/.../OrderPayServiceImpl.java` | 修改 |
| 5 | 订单类型常量 | `crmeb-common/.../Constants.java` | 新增常量 |
| **App 前端** |
| 6 | API 封装 | `app/api/api.js` | 新增接口 |
| 7 | 我的优惠券 | `app/pages/users/user_coupon/index.vue` | 修改 |
| 8 | 结算页 | `app/pages/order/order_confirm/index.vue` | 修改 |
| 9 | 代金券选择弹窗 | `app/components/voucherListWindow/index.vue` | 新增 |
| 10 | 代金券购买页 | `app/pages/activity/voucher_buy/index.vue` | 新增 |
| 11 | 路由配置 | `app/pages.json` | 修改 |

---

## 八、不涉及的改动

- ❌ 后台管理端（已完整实现）
- ❌ 订单价格计算逻辑（已支持代金券抵扣）
- ❌ 数据库表结构（字段已完备）

---

## 九、测试用例要点

- [ ] 我的优惠券页面正确展示优惠券和代金券，标签区分清晰
- [ ] 结算页显示双入口，优惠券和代金券可独立选择
- [ ] 优惠券和代金券可叠加使用，金额计算正确
- [ ] 代金券购买页正确展示可购买列表和库存
- [ ] 购买代金券生成虚拟订单，支付流程正常
- [ ] 支付成功后代金券自动发放到用户账户
- [ ] 库存为0时显示"已售罄"，按钮置灰不可点击
- [ ] 代金券可抵扣运费功能正常（canDeductFreight=true时）

---

*本设计方案基于需求讨论生成，用于指导后续开发实施*
