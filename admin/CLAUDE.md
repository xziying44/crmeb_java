[根目录](../CLAUDE.md) > **admin**

# admin 模块 (PC 管理后台)

## 模块职责

PC 端管理后台前端应用，基于 Vue 2 + Element UI 构建，提供商城管理、订单处理、用户管理、营销活动等功能界面。

## 入口与启动

### 主入口

```
src/main.js
```

### 启动命令

```bash
cd admin

# 安装依赖
npm install

# 开发环境启动
npm run dev

# 生产环境构建
npm run build:prod

# 测试环境构建
npm run build:stage
```

### 默认配置

- **开发端口**: 9527 (通常)
- **API 代理**: 配置在 `vue.config.js`

## 对外接口

### 路由模块

| 路由模块 | 路径 | 功能 |
|---------|------|------|
| `storeRouter` | `/store` | 商品管理 |
| `orderRouter` | `/order` | 订单管理 |
| `userRouter` | `/user` | 用户管理 |
| `distributionRouter` | `/distribution` | 分销管理 |
| `marketingRouter` | `/marketing` | 营销管理 |
| `financialRouter` | `/financial` | 财务管理 |
| `contentRouter` | `/content` | 内容管理 |
| `operationRouter` | `/operation` | 设置管理 |
| `appSettingRouter` | `/appSetting` | 应用设置 |
| `maintainRouter` | `/maintain` | 系统维护 |
| `designRouter` | `/design` | 页面装修 |

### API 封装

位于 `src/api/` 目录:

| API 文件 | 功能 |
|---------|------|
| `user.js` | 用户相关接口 |
| `store.js` | 商品相关接口 |
| `order.js` | 订单相关接口 |
| `marketing.js` | 营销相关接口 |
| `financial.js` | 财务相关接口 |
| `systemConfig.js` | 系统配置接口 |
| `wxApi.js` | 微信相关接口 |

## 关键依赖与配置

### 核心依赖

```json
{
  "vue": "2.6.10",
  "vue-router": "3.0.2",
  "vuex": "3.1.0",
  "element-ui": "2.15.6",
  "axios": "^0.24.0",
  "echarts": "4.2.1"
}
```

### 环境配置文件

| 文件 | 用途 |
|-----|------|
| `.env.development` | 开发环境变量 |
| `.env.production` | 生产环境变量 |
| `.env.staging` | 测试环境变量 |

### 代码规范配置

| 文件 | 用途 |
|-----|------|
| `.eslintrc.js` | ESLint 规则 |
| `.prettierrc.js` | Prettier 格式化 |
| `.editorconfig` | 编辑器配置 |

## 项目结构

```
admin/
├── public/                    # 静态资源
│   ├── index.html
│   └── static/tinymce4.7.5/   # 富文本编辑器
├── src/
│   ├── api/                   # API 接口封装 (~30个)
│   ├── assets/                # 静态资源
│   ├── components/            # 公共组件 (~50个)
│   │   ├── FormGenerator/     # 表单生成器
│   │   ├── uploadPicture/     # 图片上传
│   │   ├── Tinymce/           # 富文本编辑器
│   │   └── ...
│   ├── directive/             # 自定义指令
│   ├── filters/               # 过滤器
│   ├── icons/                 # 图标
│   ├── layout/                # 布局组件
│   ├── libs/                  # 工具库
│   ├── router/                # 路由配置
│   │   ├── index.js
│   │   └── modules/           # 路由模块
│   ├── store/                 # Vuex 状态管理
│   ├── theme/                 # 主题样式
│   ├── utils/                 # 工具函数
│   ├── views/                 # 页面组件
│   │   ├── dashboard/         # 控制台
│   │   ├── store/             # 商品管理
│   │   ├── order/             # 订单管理
│   │   ├── user/              # 用户管理
│   │   ├── marketing/         # 营销管理
│   │   ├── design/            # 页面装修
│   │   └── ...
│   ├── App.vue                # 根组件
│   ├── main.js                # 入口文件
│   └── permission.js          # 权限控制
├── .eslintrc.js               # ESLint 配置
├── .prettierrc.js             # Prettier 配置
├── babel.config.js            # Babel 配置
├── vue.config.js              # Vue CLI 配置
└── package.json
```

## 核心组件

### 表单生成器 (FormGenerator)

动态表单生成组件，支持拖拽设计:

```
components/FormGenerator/
├── components/
│   ├── generator/     # 表单配置生成
│   ├── parser/        # 表单解析渲染
│   ├── render/        # 渲染器
│   └── tinymce/       # 富文本集成
└── index/             # 主入口
```

### 图片上传 (uploadPicture)

统一的图片上传组件，支持多种存储方式:

```vue
<upload-picture v-model="form.image" />
```

### 页面装修 (design)

可视化页面 DIY 设计器:

```
views/design/
├── devise/            # 装修设计器
└── components/        # 装修组件
```

## 测试与质量

### 测试命令

```bash
# 运行单元测试
npm run test:unit

# 代码检查
npm run lint

# 格式化代码
npm run prettier
```

### 测试配置

- 测试框架: Jest
- 配置文件: `jest.config.js`

### 代码规范

- 缩进: 2 空格
- 引号: 单引号
- 分号: 不使用
- 组件命名: PascalCase

## 常见问题 (FAQ)

### Q: 如何新增页面？

1. 在 `src/views/{模块}/` 下创建 `.vue` 文件
2. 在 `src/router/modules/{模块}.js` 中添加路由
3. 如需 API 调用，在 `src/api/` 中添加接口

### Q: 如何修改 API 地址？

修改 `.env.{环境}` 文件中的 `VUE_APP_BASE_API` 变量。

### Q: 如何添加菜单权限？

1. 后端在数据库 `system_menu` 表添加菜单
2. 前端路由的 `meta.roles` 配置角色权限
3. 使用 `v-hasPermi` 指令控制按钮权限

### Q: 图片上传配置在哪？

后端 `system_config` 表中配置存储方式（本地/阿里云/腾讯云/七牛云）。

## 相关文件清单

```
admin/src/
├── api/                       # API 接口 (~30个)
├── components/                # 公共组件 (~50个)
├── router/
│   ├── index.js              # 路由主入口
│   └── modules/              # 路由模块 (~12个)
├── store/
│   ├── index.js              # Vuex 主入口
│   └── modules/              # 状态模块
├── views/                     # 页面组件 (~100个)
│   ├── dashboard/
│   ├── store/
│   ├── order/
│   └── ...
├── utils/
│   └── request.js            # Axios 封装
├── main.js                    # 入口
└── permission.js              # 权限
```

## 变更记录 (Changelog)

| 日期 | 变更内容 |
|-----|---------|
| 2026-01-18 | AI 架构师首次生成模块文档 |
