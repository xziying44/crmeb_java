[根目录](../CLAUDE.md) > **app**

# app 模块 (移动端商城)

## 模块职责

移动端商城前端应用，基于 uni-app 框架构建，支持编译到 H5、微信小程序、APP 等多端。提供商品浏览、购物车、下单、支付、用户中心等核心电商功能。

## 入口与启动

### 主入口

```
App.vue        # 根组件
main.js        # 入口文件
pages.json     # 页面配置
```

### 启动方式

使用 **HBuilderX** 打开项目:

1. 打开 HBuilderX
2. 导入 `app` 目录
3. 选择运行目标:
   - 运行到浏览器 (H5)
   - 运行到微信开发者工具 (小程序)
   - 运行到手机或模拟器 (APP)

### 编译命令

```bash
cd app
npm install

# 如使用 CLI 方式
# npm run dev:h5      # H5 开发
# npm run dev:mp-weixin  # 微信小程序开发
```

## 页面结构

### 主页面 (tabBar)

| 页面 | 路径 | 功能 |
|-----|------|------|
| 首页 | `pages/index/index` | 商城首页、Banner、商品推荐 |
| 分类 | `pages/goods_cate/goods_cate` | 商品分类导航 |
| 购物车 | `pages/order_addcart/order_addcart` | 购物车管理 |
| 我的 | `pages/user/index` | 用户中心 |

### 分包结构

| 分包 | 路径 | 功能 |
|-----|------|------|
| `users` | `pages/users/` | 用户相关页面 (登录/地址/订单) |
| `activity` | `pages/activity/` | 营销活动 (砍价/拼团/秒杀) |
| `goods` | `pages/goods/` | 商品相关 (详情/搜索/评价) |
| `order` | `pages/order/` | 订单相关 (确认/支付/详情) |
| `news` | `pages/news/` | 资讯文章 |
| `infos` | `pages/infos/` | 个人信息 |
| `promoter` | `pages/promoter/` | 分销推广 |

## 关键依赖与配置

### 核心配置文件

| 文件 | 用途 |
|-----|------|
| `pages.json` | 页面路由、tabBar、全局样式 |
| `manifest.json` | 应用配置 (AppID、权限等) |
| `App.vue` | 全局样式、生命周期 |

### 主要依赖

```json
{
  "mp-html": "^2.5.0"  // 富文本解析
}
```

## 项目结构

```
app/
├── components/                # 公共组件 (~60个)
│   ├── homeIndex/            # 首页组件
│   │   ├── swiperBg.vue      # 轮播图
│   │   ├── menus.vue         # 导航菜单
│   │   ├── goodList.vue      # 商品列表
│   │   ├── seckill.vue       # 秒杀组件
│   │   ├── bargain.vue       # 砍价组件
│   │   └── group.vue         # 拼团组件
│   ├── goodList/             # 商品列表组件
│   ├── productWindow/        # 商品弹窗 (SKU选择)
│   ├── payment/              # 支付组件
│   ├── couponWindow/         # 优惠券弹窗
│   └── ...
├── pages/                     # 主包页面
│   ├── index/                # 首页
│   ├── goods_cate/           # 分类
│   ├── order_addcart/        # 购物车
│   └── user/                 # 用户中心
├── pages/users/              # 分包 - 用户
│   ├── login/                # 登录
│   ├── wechat_login/         # 微信登录
│   ├── order_list/           # 订单列表
│   ├── user_address_list/    # 地址列表
│   ├── user_coupon/          # 我的优惠券
│   └── ...
├── pages/activity/           # 分包 - 活动
│   ├── goods_bargain/        # 砍价列表
│   ├── goods_combination/    # 拼团列表
│   ├── goods_seckill/        # 秒杀列表
│   └── ...
├── pages/goods/              # 分包 - 商品
│   ├── goods_details/        # 商品详情
│   ├── goods_search/         # 商品搜索
│   └── goods_list/           # 商品列表
├── pages/order/              # 分包 - 订单
│   ├── order_confirm/        # 订单确认
│   ├── order_details/        # 订单详情
│   └── order_pay_status/     # 支付结果
├── pages/promoter/           # 分包 - 分销
├── api/                      # API 接口封装
├── libs/                     # 工具库
├── static/                   # 静态资源
├── uni_modules/              # uni-app 插件
├── App.vue                   # 根组件
├── main.js                   # 入口文件
├── pages.json                # 页面配置
└── manifest.json             # 应用配置
```

## 核心组件

### 首页组件 (homeIndex)

```
components/homeIndex/
├── swiperBg.vue      # 轮播图
├── menus.vue         # 导航菜单
├── goodList.vue      # 商品列表
├── cateNav.vue       # 分类导航
├── seckill.vue       # 秒杀倒计时
├── bargain.vue       # 砍价入口
├── group.vue         # 拼团入口
├── coupon.vue        # 优惠券
├── news.vue          # 资讯
└── ...
```

### 商品选择器 (productWindow)

商品 SKU 选择弹窗:

```vue
<product-window
  :product-id="productId"
  @close="onClose"
  @confirm="onConfirm"
/>
```

### 支付组件 (payment)

统一支付入口:

```vue
<payment
  :order-id="orderId"
  :pay-price="payPrice"
  @success="onPaySuccess"
/>
```

## API 封装

位于 `api/` 目录:

```javascript
// 示例: api/product.js
export function getProductList(params) {
  return request({
    url: '/front/product/list',
    method: 'get',
    params
  })
}
```

## 多端适配

### 条件编译

```vue
<!-- #ifdef H5 -->
<h5-component />
<!-- #endif -->

<!-- #ifdef MP-WEIXIN -->
<weixin-component />
<!-- #endif -->

<!-- #ifdef APP-PLUS -->
<app-component />
<!-- #endif -->
```

### 平台差异

| 功能 | H5 | 微信小程序 | APP |
|-----|---|---|---|
| 微信支付 | 需公众号授权 | 原生支持 | 需配置 |
| 分享 | 自定义页面 | 原生分享 | 原生分享 |
| 扫码 | 调用摄像头 | 原生扫码 | 原生扫码 |

## 登录方式

1. **微信授权登录** (小程序/公众号)
2. **手机号验证码登录**
3. **账号密码登录**
4. **APP 一键登录** (需配置)

## 测试与质量

### 测试建议

1. 多端真机测试
2. 微信开发者工具预览
3. 支付流程完整测试

### 注意事项

- 小程序包体积限制 2M，主包精简，使用分包
- 图片使用懒加载
- 接口请求做好异常处理

## 常见问题 (FAQ)

### Q: 如何修改 API 地址？

修改 `libs/request.js` 或配置文件中的 `baseURL`。

### Q: 如何添加新页面？

1. 在对应目录创建页面文件
2. 在 `pages.json` 中注册路由
3. 如是分包，配置在 `subPackages`

### Q: 微信小程序如何配置？

1. 在 `manifest.json` 中配置 AppID
2. 后端配置小程序密钥
3. 配置服务器域名

### Q: 如何自定义首页？

使用后台「装修」功能，可视化配置首页组件和数据。

## 相关文件清单

```
app/
├── components/               # 公共组件 (~60个)
│   ├── homeIndex/           # 首页组件
│   ├── goodList/
│   ├── productWindow/
│   └── ...
├── pages/                    # 主包页面 (4个)
├── pages/users/              # 用户分包 (~20个)
├── pages/activity/           # 活动分包 (~10个)
├── pages/goods/              # 商品分包 (~8个)
├── pages/order/              # 订单分包 (~4个)
├── api/                      # API 封装
├── libs/                     # 工具库
├── static/                   # 静态资源
├── App.vue
├── main.js
├── pages.json                # 页面路由配置
└── manifest.json             # 应用配置
```

## 变更记录 (Changelog)

| 日期 | 变更内容 |
|-----|---------|
| 2026-01-18 | AI 架构师首次生成模块文档 |
