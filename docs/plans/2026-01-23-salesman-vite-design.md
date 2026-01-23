# 业务员端 Vite 迁移设计方案

> 生成时间: 2026-01-23
> 项目版本: CRMEB Java v1.4
> 设计目的: 将业务员端从 uni-app 迁移至 Vite + Vue 3，支持响应式布局

---

## 一、设计决策汇总

| 决策项 | 选定方案 |
|-------|---------|
| 框架版本 | Vue 3.4+ |
| 构建工具 | Vite 5.0+ |
| UI 组件库 | Vant 4.8+ |
| CSS 框架 | Tailwind CSS 3.4+ |
| 状态管理 | Pinia 2.1+ |
| 路由模式 | Hash 模式 |
| 开发语言 | TypeScript 5.3+ |
| 移动适配 | postcss-px-to-viewport（375px 基准） |
| API 接入 | 反向代理（开发 Vite proxy / 生产 Nginx） |

---

## 二、技术栈详情

### 2.1 核心依赖

| 依赖 | 版本 | 用途 |
|-----|------|-----|
| vue | ^3.4 | 核心框架 |
| vite | ^5.0 | 构建工具 |
| vant | ^4.8 | 移动端 UI 组件库 |
| tailwindcss | ^3.4 | 原子化 CSS 框架 |
| pinia | ^2.1 | 状态管理 |
| pinia-plugin-persistedstate | ^3.2 | 状态持久化 |
| vue-router | ^4.2 | 路由管理 |
| axios | ^1.6 | HTTP 客户端 |
| typescript | ^5.3 | 类型支持 |
| postcss-px-to-viewport-8-plugin | ^1.2 | 移动端适配 |

### 2.2 开发依赖

| 依赖 | 用途 |
|-----|-----|
| @vitejs/plugin-vue | Vue 3 Vite 插件 |
| autoprefixer | CSS 前缀自动补全 |
| postcss | CSS 后处理器 |
| @types/node | Node.js 类型定义 |

---

## 三、项目结构

```
salesman/
├── src/
│   ├── api/                     # API 接口封装
│   │   ├── auth.ts              # 认证相关
│   │   ├── dashboard.ts         # 数据看板
│   │   ├── customer.ts          # 客户管理
│   │   └── promote.ts           # 推广相关
│   ├── assets/                  # 静态资源
│   │   └── images/
│   ├── components/              # 公共组件
│   │   ├── AppNavBar.vue        # 顶部导航栏
│   │   ├── AppTabBar.vue        # 底部 TabBar
│   │   ├── AppEmpty.vue         # 空状态组件
│   │   ├── AppLoading.vue       # 加载骨架屏
│   │   ├── StatCard.vue         # 统计卡片
│   │   ├── CustomerCard.vue     # 客户卡片
│   │   └── QrcodeCard.vue       # 二维码卡片
│   ├── composables/             # 组合式函数
│   │   ├── useAuth.ts           # 认证状态
│   │   ├── useLoading.ts        # 加载状态
│   │   ├── usePagination.ts     # 分页逻辑
│   │   └── useCountDown.ts      # 倒计时
│   ├── layouts/                 # 布局组件
│   │   ├── DefaultLayout.vue    # 带 TabBar 主布局
│   │   ├── BlankLayout.vue      # 空白布局（登录页）
│   │   └── PageLayout.vue       # 二级页面布局
│   ├── router/                  # 路由配置
│   │   └── index.ts
│   ├── stores/                  # Pinia 状态
│   │   ├── index.ts
│   │   ├── user.ts              # 用户状态
│   │   └── customer.ts          # 客户状态
│   ├── styles/                  # 全局样式
│   │   └── index.css            # Tailwind 入口
│   ├── types/                   # TypeScript 类型
│   │   ├── api.d.ts             # API 响应类型
│   │   ├── customer.d.ts        # 客户相关类型
│   │   └── user.d.ts            # 用户相关类型
│   ├── utils/                   # 工具函数
│   │   ├── request.ts           # Axios 封装
│   │   ├── storage.ts           # 本地存储
│   │   └── format.ts            # 格式化工具
│   ├── views/                   # 页面组件
│   │   ├── login/
│   │   │   └── index.vue
│   │   ├── home/
│   │   │   └── index.vue
│   │   ├── customer/
│   │   │   ├── list.vue
│   │   │   ├── detail.vue
│   │   │   └── add.vue
│   │   ├── promote/
│   │   │   └── index.vue
│   │   └── profile/
│   │       ├── index.vue
│   │       └── password.vue
│   ├── App.vue                  # 根组件
│   └── main.ts                  # 入口文件
├── public/                      # 公共静态资源
├── index.html                   # HTML 模板
├── vite.config.ts               # Vite 配置
├── tailwind.config.js           # Tailwind 配置
├── postcss.config.js            # PostCSS 配置
├── tsconfig.json                # TypeScript 配置
├── .env                         # 环境变量
└── package.json
```

---

## 四、页面与路由设计

### 4.1 页面清单

| 路由路径 | 页面文件 | 功能 | 布局 |
|---------|---------|------|-----|
| `/login` | `views/login/index.vue` | 业务员登录 | BlankLayout |
| `/` | `views/home/index.vue` | 首页（数据看板） | DefaultLayout |
| `/customer` | `views/customer/list.vue` | 客户列表 | DefaultLayout |
| `/customer/:id` | `views/customer/detail.vue` | 客户详情 | PageLayout |
| `/customer/add` | `views/customer/add.vue` | 添加客户 | PageLayout |
| `/promote` | `views/promote/index.vue` | 推广（邀请码/二维码） | DefaultLayout |
| `/profile` | `views/profile/index.vue` | 个人中心 | DefaultLayout |
| `/profile/password` | `views/profile/password.vue` | 修改密码 | PageLayout |

### 4.2 TabBar 配置

底部导航栏包含 4 个入口：

| 图标 | 文字 | 路由 |
|-----|------|-----|
| home-o | 首页 | `/` |
| friends-o | 客户 | `/customer` |
| qr-o | 推广 | `/promote` |
| user-o | 我的 | `/profile` |

### 4.3 路由守卫

```typescript
// 白名单路由（无需登录）
const whiteList = ['/login']

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (userStore.token) {
    // 已登录
    if (to.path === '/login') {
      next('/')
    } else {
      next()
    }
  } else {
    // 未登录
    if (whiteList.includes(to.path)) {
      next()
    } else {
      next('/login')
    }
  }
})
```

---

## 五、响应式布局设计

### 5.1 适配策略

采用**移动优先 + 响应式增强**策略：

| 断点 | 宽度范围 | 目标设备 | 布局特点 |
|-----|---------|---------|---------|
| 默认 | < 640px | 手机 | 单列布局，全宽组件 |
| `sm` | ≥ 640px | 大屏手机/小平板 | 卡片稍有间距 |
| `md` | ≥ 768px | 平板 | 双列网格可选 |
| `lg` | ≥ 1024px | 桌面 | 居中容器，最大宽度 480px |

### 5.2 PostCSS 配置

```javascript
// postcss.config.js
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
    'postcss-px-to-viewport-8-plugin': {
      viewportWidth: 375,        // 设计稿宽度
      unitPrecision: 5,          // 转换精度
      viewportUnit: 'vw',        // 目标单位
      selectorBlackList: [],     // 忽略的选择器
      minPixelValue: 1,          // 最小转换值
      mediaQuery: false,         // 不转换媒体查询中的 px
      exclude: [/node_modules/]  // 排除第三方库
    }
  }
}
```

### 5.3 桌面端居中布局

```vue
<!-- layouts/DefaultLayout.vue -->
<template>
  <div class="min-h-screen bg-gray-100 lg:flex lg:justify-center lg:items-start lg:py-4">
    <div class="w-full lg:max-w-[480px] lg:min-h-screen lg:shadow-xl lg:rounded-lg bg-white">
      <router-view />
      <AppTabBar />
    </div>
  </div>
</template>
```

---

## 六、状态管理设计

### 6.1 用户状态 (stores/user.ts)

```typescript
interface UserInfo {
  id: number
  realName: string
  phone: string
  account: string
  salesmanCode: string
  salesmanQrcode: string | null
}

interface UserState {
  token: string | null
  userInfo: UserInfo | null
}

// Actions
interface UserActions {
  login(params: LoginParams): Promise<void>
  logout(): void
  getUserInfo(): Promise<void>
  updatePassword(params: UpdatePasswordParams): Promise<void>
}
```

### 6.2 客户状态 (stores/customer.ts)

```typescript
interface Customer {
  uid: number
  nickname: string
  phone: string
  avatar: string
  bindTime: string
  totalAmount: number
  orderCount: number
}

interface CustomerState {
  list: Customer[]
  total: number
  loading: boolean
  currentDetail: Customer | null
}

// Actions
interface CustomerActions {
  fetchList(params: ListParams): Promise<void>
  fetchDetail(uid: number): Promise<void>
  refreshList(): Promise<void>
}
```

### 6.3 持久化配置

```typescript
// stores/user.ts
export const useUserStore = defineStore('user', {
  state: () => ({ ... }),
  actions: { ... },
  persist: {
    key: 'salesman-user',
    storage: localStorage,
    paths: ['token']  // 仅持久化 token
  }
})
```

---

## 七、API 封装设计

### 7.1 请求配置

```typescript
// utils/request.ts
import axios from 'axios'
import { useUserStore } from '@/stores/user'
import { showToast } from 'vant'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',  // 反向代理统一处理
  timeout: 15000
})

// 请求拦截器
request.interceptors.request.use(config => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = userStore.token
  }
  return config
})

// 响应拦截器
request.interceptors.response.use(
  response => {
    const { code, message, data } = response.data
    if (code === 200) {
      return data
    }
    showToast(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  error => {
    if (error.response?.status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      router.push('/login')
    }
    showToast(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
```

### 7.2 API 模块

```typescript
// api/auth.ts
import request from '@/utils/request'

export const loginApi = (data: LoginParams) =>
  request.post('/admin/salesman/app/login', data)

export const logoutApi = () =>
  request.post('/admin/salesman/app/logout')

export const updatePasswordApi = (data: UpdatePasswordParams) =>
  request.post('/admin/salesman/app/updatePassword', data)
```

```typescript
// api/dashboard.ts
import request from '@/utils/request'

export const getDashboardApi = (dateType: string = 'day') =>
  request.get('/admin/salesman/app/dashboard', { params: { dateType } })

export const getMyCodeApi = () =>
  request.get('/admin/salesman/app/myCode')
```

```typescript
// api/customer.ts
import request from '@/utils/request'

export const getCustomerListApi = (params: ListParams) =>
  request.get('/admin/salesman/app/customer/list', { params })

export const getCustomerDetailApi = (uid: number) =>
  request.get(`/admin/salesman/app/customer/detail/${uid}`)

export const sendCodeApi = (phone: string) =>
  request.post('/admin/salesman/app/customer/sendCode', { phone })

export const bindByCodeApi = (phone: string, code: string) =>
  request.post('/admin/salesman/app/customer/bindByCode', { phone, code })
```

---

## 八、Vite 配置

```typescript
// vite.config.ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: false,
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true
      }
    }
  }
})
```

---

## 九、迁移步骤

| 步骤 | 操作 | 说明 |
|-----|------|-----|
| 1 | 备份归档 | 将 `salesman/` 重命名为 `salesman-uniapp-backup/` |
| 2 | 初始化项目 | 使用 Vite 创建新的 `salesman/` 目录 |
| 3 | 安装依赖 | Vue 3 + Vant 4 + Tailwind + Pinia + TypeScript |
| 4 | 配置构建 | vite.config.ts、tailwind.config.js、postcss.config.js、tsconfig.json |
| 5 | 基础架构 | 路由、布局、请求封装、Store |
| 6 | 页面迁移 | 登录 → 首页 → 客户 → 推广 → 个人中心 |
| 7 | 联调测试 | 与后端 API 联调 |

---

## 十、开发规范

### 10.1 文件命名

- 组件：PascalCase（`CustomerCard.vue`）
- 页面：小写目录 + `index.vue` 或功能名（`views/customer/list.vue`）
- 工具/API：camelCase（`request.ts`、`customer.ts`）
- 类型定义：camelCase + `.d.ts`（`customer.d.ts`）

### 10.2 代码风格

- 缩进：2 空格
- 引号：单引号
- 分号：不使用
- 注释语言：中文

### 10.3 Git 提交格式

```
feat(salesman): 添加客户列表页面
fix(salesman): 修复登录状态丢失问题
style(salesman): 调整首页布局样式
refactor(salesman): 重构请求封装逻辑
```

---

## 十一、环境变量

```bash
# .env
VITE_APP_TITLE=业务员助手
```

---

*本设计方案已通过 brainstorming 流程确认，用于指导业务员端 Vite 迁移实施*
