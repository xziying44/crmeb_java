# 阶段一：业务员模块完整设计方案

> 生成时间: 2026-01-21
> 项目版本: CRMEB Java v1.4
> 设计目的: 阶段一开发实施指南

---

## 一、设计决策汇总

| 决策项 | 选定方案 |
|-------|---------|
| 业务员端架构 | 全新独立 uni-app 项目（salesman 目录） |
| 开发范围 | 完整版（Admin 后台 + 业务员端 + 商城端绑定） |
| 登录认证 | 独立登录入口 `/api/admin/salesman/login`，共用 JWT 认证体系 |
| 邀请码规则 | 6-8 位字母数字混合（如 `AB12CD`） |
| 二维码方案 | 微信小程序码（wxacode.getUnlimited），带 scene 参数 |
| 绑定更换策略 | 禁止用户自行更换，仅允许管理员后台转移 |
| 添加客户逻辑 | 智能处理：已注册则绑定，未注册则创建并绑定 |
| 角色权限 | 固定权限，所有业务员共用一个预设角色 |

---

## 二、整体架构

### 2.1 项目结构

```
agents-2988c19abe/
├── crmeb/                    # 后端（现有）
│   ├── crmeb-admin/          # 管理端 API - 新增业务员相关接口
│   ├── crmeb-front/          # 移动端 API - 修改注册绑定逻辑
│   ├── crmeb-service/        # 业务逻辑 - 新增业务员服务
│   └── crmeb-common/         # 公共模块 - 新增实体/请求/响应类
├── admin/                    # 管理后台前端（现有）- 新增业务员管理页面
├── app/                      # 商城小程序（现有）- 修改注册/绑定流程
└── salesman/                 # 业务员端（新建）- uni-app 独立项目
```

### 2.2 技术选型

| 模块 | 技术 | 说明 |
|-----|------|-----|
| 业务员端 | uni-app + Vue 2 | 与现有 app 保持一致，支持小程序/H5 |
| 后端接口 | Spring Boot | 复用现有架构，新增 Controller |
| 认证方式 | JWT | 独立登录入口，共用认证体系 |
| 小程序码 | 微信 wxacode API | 生成带参数的小程序码 |

---

## 三、数据库设计

### 3.1 eb_system_role 表新增字段

```sql
ALTER TABLE eb_system_role ADD COLUMN
  is_salesman_role TINYINT(1) DEFAULT 0 COMMENT '是否业务员角色 0-否 1-是';
```

### 3.2 eb_salesman_info 新增表

```sql
CREATE TABLE eb_salesman_info (
  id              INT(11) PRIMARY KEY AUTO_INCREMENT,
  admin_id        INT(11) NOT NULL COMMENT '关联的管理员ID',
  salesman_code   VARCHAR(8) NOT NULL COMMENT '业务员邀请码（6-8位字母数字）',
  salesman_qrcode VARCHAR(255) NULL COMMENT '小程序码图片地址',
  qrcode_scene    VARCHAR(32) NULL COMMENT '小程序码scene参数',
  bindable        TINYINT(1) DEFAULT 1 COMMENT '是否可被绑定 0-否 1-是',
  create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_admin_id (admin_id),
  UNIQUE KEY uk_salesman_code (salesman_code)
) COMMENT '业务员扩展信息表';
```

### 3.3 eb_user 表新增字段

```sql
ALTER TABLE eb_user ADD COLUMN (
  salesman_id        INT(11) DEFAULT 0 COMMENT '绑定的业务员ID（admin_id）',
  salesman_bind_time DATETIME NULL COMMENT '绑定业务员时间'
);
CREATE INDEX idx_salesman_id ON eb_user(salesman_id);
```

### 3.4 设计说明

| 设计点 | 说明 |
|-------|------|
| 角色驱动 | 通过 `eb_system_role.is_salesman_role` 判断是否业务员角色 |
| 信息分离 | 业务员专属信息独立存表，保持 `eb_system_admin` 表干净 |
| 一对一关系 | `eb_salesman_info.admin_id` 唯一，确保一个管理员只有一条业务员信息 |
| 邀请码唯一 | 用于用户注册时输入绑定 |
| 二维码存储 | 生成后存储图片地址，避免重复生成 |

---

## 四、Admin 后台业务员管理功能

### 4.1 菜单结构

```
系统设置（或新增一级菜单「业务员管理」）
└── 业务员管理
    ├── 业务员列表        # 增删改查、启用/禁用
    ├── 客户绑定记录      # 查看绑定关系、转移客户
    └── 业绩统计          # 各业务员数据汇总、排行榜
```

### 4.2 后端接口设计

| 接口 | 方法 | 功能 |
|-----|------|-----|
| `/api/admin/salesman/list` | GET | 业务员分页列表（支持搜索） |
| `/api/admin/salesman/info/{id}` | GET | 业务员详情（含邀请码、二维码） |
| `/api/admin/salesman/save` | POST | 创建业务员（自动生成邀请码） |
| `/api/admin/salesman/update` | POST | 编辑业务员信息 |
| `/api/admin/salesman/updateStatus/{id}` | POST | 启用/禁用业务员 |
| `/api/admin/salesman/delete/{id}` | POST | 删除业务员 |
| `/api/admin/salesman/regenerateCode/{id}` | POST | 重新生成邀请码 |
| `/api/admin/salesman/generateQrcode/{id}` | POST | 生成/刷新小程序码 |
| `/api/admin/salesman/bindList` | GET | 客户绑定记录列表 |
| `/api/admin/salesman/transferCustomer` | POST | 转移客户到其他业务员 |
| `/api/admin/salesman/statistics` | GET | 业绩统计汇总 |
| `/api/admin/salesman/ranking` | GET | 业务员业绩排行榜 |

### 4.3 创建业务员流程

创建业务员时自动执行：
1. 创建 `eb_system_admin` 记录（分配预设业务员角色）
2. 生成唯一邀请码（6-8 位字母数字）
3. 创建 `eb_salesman_info` 记录

---

## 五、业务员端功能设计

### 5.1 页面结构

```
salesman/                          # uni-app 项目
├── pages/
│   ├── login/index.vue           # 登录页
│   ├── index/index.vue           # 首页（数据看板）
│   ├── customer/
│   │   ├── list.vue              # 客户列表
│   │   ├── detail.vue            # 客户详情
│   │   ├── add.vue               # 添加客户
│   │   └── orders.vue            # 客户订单记录
│   ├── promote/
│   │   └── index.vue             # 推广码（邀请码+二维码）
│   └── user/
│       ├── index.vue             # 个人中心
│       └── password.vue          # 修改密码
└── api/
    └── salesman.js               # 接口封装
```

### 5.2 后端接口设计（业务员端专用）

| 接口 | 方法 | 功能 |
|-----|------|-----|
| `/api/admin/salesman/login` | POST | 业务员登录（限业务员角色） |
| `/api/admin/salesman/dashboard` | GET | 首页数据看板 |
| `/api/admin/salesman/myCode` | GET | 获取我的邀请码和二维码 |
| `/api/admin/salesman/customer/list` | GET | 我的客户列表（分页/搜索） |
| `/api/admin/salesman/customer/detail/{uid}` | GET | 客户详情 |
| `/api/admin/salesman/customer/sendCode` | POST | 添加客户-发送验证码 |
| `/api/admin/salesman/customer/bindByCode` | POST | 添加客户-验证码确认绑定 |
| `/api/admin/salesman/customer/updateRemark` | POST | 编辑客户备注/标签 |
| `/api/admin/salesman/customer/orders/{uid}` | GET | 客户订单记录 |
| `/api/admin/salesman/statistics/trend` | GET | 销售趋势数据（日/周/月） |

### 5.3 首页数据看板

展示内容：
- 我的客户数（总数 / 本月新增）
- 客户订单额（总额 / 本月）
- 销售趋势图（可切换日/周/月）
- 客户消费排行榜（Top 10）

---

## 六、商城端绑定功能

### 6.1 绑定场景与处理逻辑

| 场景 | 触发方式 | 处理逻辑 |
|-----|---------|---------|
| 注册时绑定 | 注册页输入邀请码 | 校验邀请码有效性 → 注册成功后自动绑定 |
| 扫码绑定（未登录） | 扫小程序码 | 解析 scene 参数 → 跳转注册页（自动填充邀请码） |
| 扫码绑定（已登录未绑定） | 扫小程序码 | 解析 scene 参数 → 跳转绑定确认页 → 确认后绑定 |
| 扫码绑定（已绑定） | 扫小程序码 | 提示"您已绑定业务员 XXX，如需更换请联系客服" |

### 6.2 后端接口修改/新增

| 接口 | 修改内容 |
|-----|---------|
| `/api/front/user/register` | 新增 `salesmanCode` 可选参数，注册时绑定 |
| `/api/front/user/bindSalesman` | 新增接口，已登录用户绑定业务员 |
| `/api/front/user/getSalesmanInfo` | 新增接口，查询当前绑定的业务员信息 |
| `/api/front/user/checkSalesmanCode` | 新增接口，校验邀请码是否有效 |

### 6.3 小程序端处理逻辑

```javascript
// app.vue 或 页面 onLoad 中处理 scene 参数
onLoad(options) {
  if (options.scene) {
    const scene = decodeURIComponent(options.scene);
    // scene 格式: s_ABC123 (s_ 前缀 + 邀请码)
    if (scene.startsWith('s_')) {
      const salesmanCode = scene.substring(2);
      // 存储到本地，注册/绑定时使用
      uni.setStorageSync('pendingSalesmanCode', salesmanCode);
      // 根据登录状态跳转
      this.handleSalesmanBind(salesmanCode);
    }
  }
}
```

---

## 七、开发任务拆分

### 7.1 完整任务清单

| 序号 | 任务 | 预估工时 | 依赖 |
|-----|------|---------|-----|
| **数据库** |
| 1 | 数据库表结构变更（role字段、salesman_info表、user字段） | 0.5天 | - |
| **后端开发** |
| 2 | 业务员实体类、Request/Response/VO 类 | 0.5天 | 1 |
| 3 | SalesmanService 业务逻辑（邀请码生成、统计计算） | 1天 | 2 |
| 4 | Admin 后台业务员管理接口（CRUD、转移客户） | 1.5天 | 3 |
| 5 | 业务员端接口（登录、客户管理、数据看板） | 2天 | 3 |
| 6 | 微信小程序码生成服务 | 1天 | 3 |
| 7 | 商城端注册/绑定接口修改 | 1天 | 3 |
| **Admin 前端** |
| 8 | 业务员列表页面（表格、搜索、操作按钮） | 1.5天 | 4 |
| 9 | 业务员新增/编辑弹窗 | 0.5天 | 4 |
| 10 | 客户绑定记录页面、转移功能 | 1天 | 4 |
| 11 | 业绩统计页面（图表、排行榜） | 1天 | 4 |
| **业务员端前端** |
| 12 | 项目初始化、登录页、请求封装 | 1天 | 5 |
| 13 | 首页数据看板（统计卡片、趋势图） | 1.5天 | 5 |
| 14 | 客户列表、详情、订单记录页 | 2天 | 5 |
| 15 | 添加客户页面（发送验证码、确认绑定） | 1天 | 5 |
| 16 | 推广码页面（邀请码展示、二维码保存） | 0.5天 | 6 |
| 17 | 个人中心、修改密码 | 0.5天 | 5 |
| **商城端前端** |
| 18 | 注册页增加邀请码输入框 | 0.5天 | 7 |
| 19 | 扫码绑定逻辑、绑定确认页 | 1天 | 7 |
| **测试联调** |
| 20 | 功能测试、联调修复 | 2天 | 全部 |

**总计：约 20 天（4 周）**

### 7.2 建议开发顺序

| 周次 | 内容 |
|-----|------|
| 第 1 周 | 数据库 + 后端核心服务 + Admin 接口 |
| 第 2 周 | Admin 前端页面 + 业务员端接口 |
| 第 3 周 | 业务员端前端 + 商城端修改 |
| 第 4 周 | 联调测试 + Bug 修复 |

---

## 八、关键业务流程

### 8.1 业务员创建流程

```
管理员操作 (Admin后台)
    │
    ▼
┌─────────────────────────────┐
│ 1. 进入「业务员管理」菜单     │
│ 2. 点击「添加业务员」         │
│ 3. 填写：姓名/手机号/密码     │
│ 4. 系统自动：                │
│    - 创建 admin 账号         │
│    - 分配业务员角色           │
│    - 生成唯一邀请码           │
└─────────────────────────────┘
```

### 8.2 散户绑定业务员流程

**方式A：注册时输入邀请码**
```
用户 (小程序)                         系统
    │                                  │
    ├─ 打开注册页面 ──────────────────►│
    │                                  │
    ├─ 输入手机号+验证码+邀请码 ──────►│
    │                                  │
    │◄─────────────────── 校验邀请码 ──┤
    │                                  │
    │◄─── 注册成功，自动绑定业务员 ────┤
```

**方式B：扫码绑定**
```
用户 (小程序)                         系统
    │                                  │
    ├─ 扫描业务员小程序码 ────────────►│
    │                                  │
    │◄──────────── 跳转小程序绑定页 ───┤
    │                                  │
    ├─ 未登录 → 先注册/登录 ─────────►│
    ├─ 已登录 → 确认绑定 ────────────►│
    │                                  │
    │◄─────────────── 绑定成功提示 ────┤
```

### 8.3 业务员添加客户流程

```
业务员 (业务员端)              系统                    客户
    │                          │                       │
    ├─ 输入客户手机号 ────────►│                       │
    │                          │                       │
    │                          ├─ 发送验证码 ─────────►│
    │                          │                       │
    │◄── 等待客户提供验证码 ───┤                       │
    │                          │                       │
    ├─ 输入验证码 ───────────►│                       │
    │                          │                       │
    │◄─── 创建/绑定成功 ───────┤                       │
```

---

*本设计方案已通过 brainstorming 流程确认，用于指导阶段一开发实施*
