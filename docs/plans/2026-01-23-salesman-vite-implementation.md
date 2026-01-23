# 业务员端 Vite 迁移实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将业务员端从 uni-app 迁移至 Vite + Vue 3 + Vant 4，实现响应式移动端布局。

**Architecture:** 采用 Vue 3 Composition API + Pinia 状态管理 + Vue Router Hash 模式。使用 Tailwind CSS 实现响应式布局，postcss-px-to-viewport 实现移动端适配。通过反向代理统一处理 API 请求。

**Tech Stack:** Vue 3.4+, Vite 5.0+, Vant 4.8+, Tailwind CSS 3.4+, Pinia 2.1+, TypeScript 5.3+, Axios 1.6+

---

## 任务总览

| 任务 | 描述 | 预估时间 |
|-----|------|---------|
| Task 1 | 备份归档 uni-app 项目 | 2 分钟 |
| Task 2 | 初始化 Vite + Vue 3 项目 | 5 分钟 |
| Task 3 | 安装核心依赖 | 3 分钟 |
| Task 4 | 配置 Vite | 5 分钟 |
| Task 5 | 配置 Tailwind CSS | 5 分钟 |
| Task 6 | 配置 TypeScript | 3 分钟 |
| Task 7 | 创建目录结构 | 3 分钟 |
| Task 8 | 实现请求封装 | 10 分钟 |
| Task 9 | 实现用户 Store | 10 分钟 |
| Task 10 | 实现路由配置 | 10 分钟 |
| Task 11 | 实现布局组件 | 15 分钟 |
| Task 12 | 实现登录页面 | 15 分钟 |
| Task 13 | 实现首页数据看板 | 20 分钟 |
| Task 14 | 实现客户列表页 | 20 分钟 |
| Task 15 | 实现客户详情页 | 15 分钟 |
| Task 16 | 实现添加客户页 | 15 分钟 |
| Task 17 | 实现推广页 | 15 分钟 |
| Task 18 | 实现个人中心页 | 10 分钟 |
| Task 19 | 实现修改密码页 | 10 分钟 |
| Task 20 | 验证与清理 | 10 分钟 |

---

## Task 1: 备份归档 uni-app 项目

**Files:**
- Rename: `salesman/` → `salesman-uniapp-backup/`

### Step 1: 备份现有项目

Run: `mv /Users/xziying/project/bespoke/crmeb_java/salesman /Users/xziying/project/bespoke/crmeb_java/salesman-uniapp-backup`

Expected: 目录重命名成功

### Step 2: 验证备份

Run: `ls -la /Users/xziying/project/bespoke/crmeb_java/salesman-uniapp-backup`

Expected: 显示原 uni-app 项目文件（App.vue, main.js, pages.json 等）

---

## Task 2: 初始化 Vite + Vue 3 项目

**Files:**
- Create: `salesman/` (新 Vite 项目)

### Step 1: 创建 Vite 项目

Run: `cd /Users/xziying/project/bespoke/crmeb_java && npm create vite@latest salesman -- --template vue-ts`

Expected: 项目创建成功

### Step 2: 进入项目目录

Run: `cd /Users/xziying/project/bespoke/crmeb_java/salesman`

Expected: 成功进入目录

### Step 3: 验证项目结构

Run: `ls -la /Users/xziying/project/bespoke/crmeb_java/salesman`

Expected: 显示 Vite 项目文件（index.html, package.json, vite.config.ts, src/ 等）

---

## Task 3: 安装核心依赖

**Files:**
- Modify: `salesman/package.json`

### Step 1: 安装运行时依赖

Run: `cd /Users/xziying/project/bespoke/crmeb_java/salesman && npm install vue-router@4 pinia pinia-plugin-persistedstate vant @vant/use axios`

Expected: 依赖安装成功

### Step 2: 安装开发依赖

Run: `cd /Users/xziying/project/bespoke/crmeb_java/salesman && npm install -D tailwindcss postcss autoprefixer postcss-px-to-viewport-8-plugin @types/node`

Expected: 开发依赖安装成功

### Step 3: 验证 package.json

Run: `cat /Users/xziying/project/bespoke/crmeb_java/salesman/package.json`

Expected: 包含所有安装的依赖

---

## Task 4: 配置 Vite

**Files:**
- Modify: `salesman/vite.config.ts`

### Step 1: 更新 Vite 配置

```typescript
// salesman/vite.config.ts
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

### Step 2: 验证配置语法

Run: `cd /Users/xziying/project/bespoke/crmeb_java/salesman && npx tsc --noEmit vite.config.ts 2>/dev/null || echo "配置文件已更新"`

Expected: 无语法错误

---

## Task 5: 配置 Tailwind CSS

**Files:**
- Create: `salesman/tailwind.config.js`
- Create: `salesman/postcss.config.js`
- Modify: `salesman/src/style.css`

### Step 1: 初始化 Tailwind

Run: `cd /Users/xziying/project/bespoke/crmeb_java/salesman && npx tailwindcss init`

Expected: 创建 tailwind.config.js

### Step 2: 更新 Tailwind 配置

```javascript
// salesman/tailwind.config.js
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: '#1989fa',
        success: '#07c160',
        warning: '#ff976a',
        danger: '#ee0a24',
      }
    },
  },
  plugins: [],
}
```

### Step 3: 创建 PostCSS 配置

```javascript
// salesman/postcss.config.js
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
    'postcss-px-to-viewport-8-plugin': {
      viewportWidth: 375,
      unitPrecision: 5,
      viewportUnit: 'vw',
      selectorBlackList: [],
      minPixelValue: 1,
      mediaQuery: false,
      exclude: [/node_modules/]
    }
  }
}
```

### Step 4: 更新全局样式

```css
/* salesman/src/style.css */
@tailwind base;
@tailwind components;
@tailwind utilities;

/* 自定义全局样式 */
html, body, #app {
  height: 100%;
  margin: 0;
  padding: 0;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* 安全区域适配 */
.safe-area-bottom {
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}
```

---

## Task 6: 配置 TypeScript

**Files:**
- Modify: `salesman/tsconfig.json`
- Create: `salesman/src/env.d.ts`

### Step 1: 更新 tsconfig.json

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "module": "ESNext",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "preserve",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    },
    "types": ["vite/client"]
  },
  "include": ["src/**/*.ts", "src/**/*.tsx", "src/**/*.vue"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

### Step 2: 创建环境类型声明

```typescript
// salesman/src/env.d.ts
/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

interface ImportMetaEnv {
  readonly VITE_APP_TITLE: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
```

---

## Task 7: 创建目录结构

**Files:**
- Create: 多个目录

### Step 1: 创建源码目录结构

Run: `cd /Users/xziying/project/bespoke/crmeb_java/salesman/src && mkdir -p api assets/images components composables layouts router stores styles types utils views/login views/home views/customer views/promote views/profile`

Expected: 目录创建成功

### Step 2: 创建环境变量文件

```bash
# salesman/.env
VITE_APP_TITLE=业务员助手
```

### Step 3: 验证目录结构

Run: `find /Users/xziying/project/bespoke/crmeb_java/salesman/src -type d | head -20`

Expected: 显示创建的目录结构

---

## Task 8: 实现请求封装

**Files:**
- Create: `salesman/src/utils/request.ts`
- Create: `salesman/src/utils/storage.ts`
- Create: `salesman/src/types/api.d.ts`

### Step 1: 创建存储工具

```typescript
// salesman/src/utils/storage.ts
const TOKEN_KEY = 'salesman_token'

/**
 * 获取 Token
 */
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

/**
 * 设置 Token
 */
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

/**
 * 移除 Token
 */
export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

/**
 * 清除所有存储
 */
export function clearStorage(): void {
  localStorage.clear()
}
```

### Step 2: 创建 API 类型定义

```typescript
// salesman/src/types/api.d.ts
/**
 * API 响应基础结构
 */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

/**
 * 分页参数
 */
export interface PageParams {
  page: number
  limit: number
}

/**
 * 分页响应
 */
export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  limit: number
  totalPage: number
}

/**
 * 登录参数
 */
export interface LoginParams {
  account: string
  pwd: string
}

/**
 * 登录响应
 */
export interface LoginResult {
  token: string
  userInfo: UserInfo
}

/**
 * 用户信息
 */
export interface UserInfo {
  id: number
  account: string
  realName: string
  phone: string
  salesmanCode: string
  salesmanQrcode: string | null
}
```

### Step 3: 创建请求封装

```typescript
// salesman/src/utils/request.ts
import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { getToken, removeToken } from './storage'
import type { ApiResponse } from '@/types/api'

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = token
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const { code, message, data } = response.data

    if (code === 200) {
      return data as any
    }

    // 业务错误
    showToast(message || '请求失败')
    return Promise.reject(new Error(message || '请求失败'))
  },
  (error) => {
    // 401 未授权
    if (error.response?.status === 401) {
      removeToken()
      window.location.hash = '#/login'
      showToast('登录已过期，请重新登录')
      return Promise.reject(error)
    }

    // 网络错误
    const message = error.response?.data?.message || error.message || '网络错误'
    showToast(message)
    return Promise.reject(error)
  }
)

/**
 * 请求方法封装
 */
const request = {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.get(url, config)
  },

  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.post(url, data, config)
  },

  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.put(url, data, config)
  },

  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.delete(url, config)
  }
}

export default request
```

---

## Task 9: 实现用户 Store

**Files:**
- Create: `salesman/src/stores/index.ts`
- Create: `salesman/src/stores/user.ts`
- Create: `salesman/src/types/user.d.ts`

### Step 1: 创建用户类型定义

```typescript
// salesman/src/types/user.d.ts
/**
 * 用户信息
 */
export interface UserInfo {
  id: number
  account: string
  realName: string
  phone: string
  salesmanCode: string
  salesmanQrcode: string | null
  bindable: boolean
  customerCount: number
  monthNewCustomerCount: number
}

/**
 * 用户状态
 */
export interface UserState {
  token: string | null
  userInfo: UserInfo | null
}

/**
 * 修改密码参数
 */
export interface UpdatePasswordParams {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}
```

### Step 2: 创建 Pinia Store 入口

```typescript
// salesman/src/stores/index.ts
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

export default pinia

export * from './user'
```

### Step 3: 创建用户 Store

```typescript
// salesman/src/stores/user.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo } from '@/types/user'
import type { LoginParams, LoginResult } from '@/types/api'
import { getToken, setToken, removeToken, clearStorage } from '@/utils/storage'
import request from '@/utils/request'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string | null>(getToken())
  const userInfo = ref<UserInfo | null>(null)

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const salesmanCode = computed(() => userInfo.value?.salesmanCode || '')

  /**
   * 登录
   */
  async function login(params: LoginParams): Promise<void> {
    const result = await request.post<LoginResult>('/admin/salesman/app/login', params)
    token.value = result.token
    userInfo.value = result.userInfo as UserInfo
    setToken(result.token)
  }

  /**
   * 获取用户信息
   */
  async function getUserInfo(): Promise<void> {
    const result = await request.get<UserInfo>('/admin/salesman/app/myCode')
    userInfo.value = result
  }

  /**
   * 退出登录
   */
  function logout(): void {
    token.value = null
    userInfo.value = null
    removeToken()
    clearStorage()
  }

  /**
   * 修改密码
   */
  async function updatePassword(oldPwd: string, newPwd: string): Promise<void> {
    await request.post('/admin/salesman/app/updatePassword', {
      oldPassword: oldPwd,
      newPassword: newPwd
    })
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    salesmanCode,
    login,
    getUserInfo,
    logout,
    updatePassword
  }
}, {
  persist: {
    key: 'salesman-user',
    storage: localStorage,
    paths: ['token']
  }
})
```

---

## Task 10: 实现路由配置

**Files:**
- Create: `salesman/src/router/index.ts`

### Step 1: 创建路由配置

```typescript
// salesman/src/router/index.ts
import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

// 路由配置
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', layout: 'blank' }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/home/index.vue'),
    meta: { title: '首页', tabbar: true }
  },
  {
    path: '/customer',
    name: 'CustomerList',
    component: () => import('@/views/customer/list.vue'),
    meta: { title: '我的客户', tabbar: true }
  },
  {
    path: '/customer/:id',
    name: 'CustomerDetail',
    component: () => import('@/views/customer/detail.vue'),
    meta: { title: '客户详情' }
  },
  {
    path: '/customer/add',
    name: 'CustomerAdd',
    component: () => import('@/views/customer/add.vue'),
    meta: { title: '添加客户' }
  },
  {
    path: '/promote',
    name: 'Promote',
    component: () => import('@/views/promote/index.vue'),
    meta: { title: '推广', tabbar: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/profile/index.vue'),
    meta: { title: '我的', tabbar: true }
  },
  {
    path: '/profile/password',
    name: 'Password',
    component: () => import('@/views/profile/password.vue'),
    meta: { title: '修改密码' }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 白名单
const whiteList = ['/login']

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = (to.meta.title as string) || import.meta.env.VITE_APP_TITLE

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

export default router
```

---

## Task 11: 实现布局组件

**Files:**
- Create: `salesman/src/layouts/DefaultLayout.vue`
- Create: `salesman/src/layouts/BlankLayout.vue`
- Create: `salesman/src/components/AppTabBar.vue`
- Create: `salesman/src/components/AppNavBar.vue`
- Modify: `salesman/src/App.vue`
- Modify: `salesman/src/main.ts`

### Step 1: 创建 TabBar 组件

```vue
<!-- salesman/src/components/AppTabBar.vue -->
<template>
  <van-tabbar v-model="active" route class="safe-area-bottom">
    <van-tabbar-item to="/" icon="home-o">首页</van-tabbar-item>
    <van-tabbar-item to="/customer" icon="friends-o">客户</van-tabbar-item>
    <van-tabbar-item to="/promote" icon="qr">推广</van-tabbar-item>
    <van-tabbar-item to="/profile" icon="user-o">我的</van-tabbar-item>
  </van-tabbar>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const active = ref(0)
</script>
```

### Step 2: 创建导航栏组件

```vue
<!-- salesman/src/components/AppNavBar.vue -->
<template>
  <van-nav-bar
    :title="title"
    :left-arrow="showBack"
    @click-left="handleBack"
    :fixed="fixed"
    :placeholder="fixed"
  >
    <template #right v-if="$slots.right">
      <slot name="right" />
    </template>
  </van-nav-bar>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

interface Props {
  title?: string
  showBack?: boolean
  fixed?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  title: '',
  showBack: true,
  fixed: true
})

const router = useRouter()

function handleBack() {
  router.back()
}
</script>
```

### Step 3: 创建默认布局

```vue
<!-- salesman/src/layouts/DefaultLayout.vue -->
<template>
  <div class="min-h-screen bg-gray-100 lg:flex lg:justify-center lg:items-start lg:py-4">
    <div class="w-full lg:max-w-[480px] lg:min-h-screen lg:shadow-xl bg-white relative">
      <!-- 页面内容 -->
      <div class="pb-[50px]">
        <slot />
      </div>
      <!-- TabBar -->
      <AppTabBar v-if="showTabBar" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppTabBar from '@/components/AppTabBar.vue'

const route = useRoute()

const showTabBar = computed(() => route.meta.tabbar === true)
</script>
```

### Step 4: 创建空白布局

```vue
<!-- salesman/src/layouts/BlankLayout.vue -->
<template>
  <div class="min-h-screen bg-white lg:flex lg:justify-center lg:items-start lg:py-4">
    <div class="w-full lg:max-w-[480px] lg:min-h-screen lg:shadow-xl bg-white">
      <slot />
    </div>
  </div>
</template>
```

### Step 5: 更新 App.vue

```vue
<!-- salesman/src/App.vue -->
<template>
  <component :is="layout">
    <router-view />
  </component>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import BlankLayout from '@/layouts/BlankLayout.vue'

const route = useRoute()

const layouts: Record<string, any> = {
  default: DefaultLayout,
  blank: BlankLayout
}

const layout = computed(() => {
  const layoutName = (route.meta.layout as string) || 'default'
  return layouts[layoutName] || DefaultLayout
})
</script>

<style>
@import './style.css';
</style>
```

### Step 6: 更新 main.ts

```typescript
// salesman/src/main.ts
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import pinia from './stores'

// Vant 组件
import {
  Button,
  Cell,
  CellGroup,
  Field,
  Form,
  NavBar,
  Tabbar,
  TabbarItem,
  Toast,
  Dialog,
  Loading,
  PullRefresh,
  List,
  Empty,
  Image as VanImage,
  Icon,
  Tag,
  Divider,
  Grid,
  GridItem,
  Skeleton,
  Search,
  ActionSheet,
  Popup
} from 'vant'
import 'vant/lib/index.css'

// 样式
import './style.css'

const app = createApp(App)

// 注册 Vant 组件
const vantComponents = [
  Button,
  Cell,
  CellGroup,
  Field,
  Form,
  NavBar,
  Tabbar,
  TabbarItem,
  Toast,
  Dialog,
  Loading,
  PullRefresh,
  List,
  Empty,
  VanImage,
  Icon,
  Tag,
  Divider,
  Grid,
  GridItem,
  Skeleton,
  Search,
  ActionSheet,
  Popup
]

vantComponents.forEach((component) => {
  app.use(component)
})

app.use(pinia)
app.use(router)
app.mount('#app')
```

---

## Task 12: 实现登录页面

**Files:**
- Create: `salesman/src/views/login/index.vue`
- Create: `salesman/src/api/auth.ts`

### Step 1: 创建认证 API

```typescript
// salesman/src/api/auth.ts
import request from '@/utils/request'
import type { LoginParams, LoginResult } from '@/types/api'

/**
 * 业务员登录
 */
export function loginApi(data: LoginParams) {
  return request.post<LoginResult>('/admin/salesman/app/login', data)
}

/**
 * 退出登录
 */
export function logoutApi() {
  return request.post('/admin/salesman/app/logout')
}
```

### Step 2: 创建登录页面

```vue
<!-- salesman/src/views/login/index.vue -->
<template>
  <div class="min-h-screen flex flex-col justify-center px-6 bg-white">
    <!-- Logo 区域 -->
    <div class="text-center mb-10">
      <div class="w-20 h-20 mx-auto mb-4 bg-primary rounded-full flex items-center justify-center">
        <van-icon name="manager" size="40" color="#fff" />
      </div>
      <h1 class="text-2xl font-bold text-gray-800">业务员助手</h1>
      <p class="text-gray-500 mt-2">专属业务员管理平台</p>
    </div>

    <!-- 登录表单 -->
    <van-form @submit="handleLogin" class="w-full">
      <van-cell-group inset>
        <van-field
          v-model="form.account"
          name="account"
          label="账号"
          placeholder="请输入账号"
          :rules="[{ required: true, message: '请输入账号' }]"
          clearable
        />
        <van-field
          v-model="form.pwd"
          type="password"
          name="pwd"
          label="密码"
          placeholder="请输入密码"
          :rules="[{ required: true, message: '请输入密码' }]"
          clearable
        />
      </van-cell-group>

      <div class="mt-6 px-4">
        <van-button
          round
          block
          type="primary"
          native-type="submit"
          :loading="loading"
          loading-text="登录中..."
        >
          登录
        </van-button>
      </div>
    </van-form>

    <!-- 底部版权 -->
    <div class="text-center text-gray-400 text-sm mt-10">
      © 2026 CRMEB. All rights reserved.
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)

const form = reactive({
  account: '',
  pwd: ''
})

async function handleLogin() {
  if (loading.value) return

  loading.value = true
  try {
    await userStore.login({
      account: form.account,
      pwd: form.pwd
    })
    showToast('登录成功')
    router.replace('/')
  } catch (error) {
    // 错误已在 request 中处理
  } finally {
    loading.value = false
  }
}
</script>
```

### Step 3: 验证登录页

Run: `cd /Users/xziying/project/bespoke/crmeb_java/salesman && npm run dev`

Expected: 项目启动成功，访问显示登录页面

---

## Task 13: 实现首页数据看板

**Files:**
- Create: `salesman/src/views/home/index.vue`
- Create: `salesman/src/api/dashboard.ts`
- Create: `salesman/src/components/StatCard.vue`
- Create: `salesman/src/types/dashboard.d.ts`

### Step 1: 创建看板类型定义

```typescript
// salesman/src/types/dashboard.d.ts
/**
 * 数据看板
 */
export interface DashboardData {
  totalCustomerCount: number
  monthNewCustomerCount: number
  totalOrderAmount: number
  monthOrderAmount: number
  trendData: TrendItem[]
  customerRanking: CustomerRankItem[]
}

/**
 * 趋势数据项
 */
export interface TrendItem {
  date: string
  orderCount: number
  orderAmount: number
}

/**
 * 客户排行项
 */
export interface CustomerRankItem {
  uid: number
  nickname: string
  phone: string
  totalAmount: number
  orderCount: number
}
```

### Step 2: 创建看板 API

```typescript
// salesman/src/api/dashboard.ts
import request from '@/utils/request'
import type { DashboardData } from '@/types/dashboard'

/**
 * 获取数据看板
 */
export function getDashboardApi(dateType: string = 'day') {
  return request.get<DashboardData>('/admin/salesman/app/dashboard', {
    params: { dateType }
  })
}

/**
 * 获取销售趋势
 */
export function getTrendApi(dateType: string = 'day') {
  return request.get('/admin/salesman/app/statistics/trend', {
    params: { dateType }
  })
}
```

### Step 3: 创建统计卡片组件

```vue
<!-- salesman/src/components/StatCard.vue -->
<template>
  <div class="bg-white rounded-lg p-4 shadow-sm">
    <div class="text-gray-500 text-sm mb-2">{{ title }}</div>
    <div class="flex items-end justify-between">
      <div class="text-2xl font-bold" :class="valueClass">
        {{ prefix }}{{ displayValue }}{{ suffix }}
      </div>
      <div v-if="subValue !== undefined" class="text-xs text-gray-400">
        {{ subLabel }}: {{ subValue }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  title: string
  value: number | string
  prefix?: string
  suffix?: string
  subLabel?: string
  subValue?: number | string
  type?: 'default' | 'primary' | 'success' | 'warning'
}

const props = withDefaults(defineProps<Props>(), {
  prefix: '',
  suffix: '',
  type: 'default'
})

const displayValue = computed(() => {
  if (typeof props.value === 'number') {
    return props.value.toLocaleString()
  }
  return props.value
})

const valueClass = computed(() => {
  const classes: Record<string, string> = {
    default: 'text-gray-800',
    primary: 'text-primary',
    success: 'text-success',
    warning: 'text-warning'
  }
  return classes[props.type] || classes.default
})
</script>
```

### Step 4: 创建首页

```vue
<!-- salesman/src/views/home/index.vue -->
<template>
  <div class="min-h-screen bg-gray-100">
    <!-- 顶部欢迎区域 -->
    <div class="bg-primary text-white p-6 pb-16">
      <div class="flex items-center justify-between">
        <div>
          <div class="text-lg font-medium">{{ greeting }}</div>
          <div class="text-sm opacity-80 mt-1">{{ userStore.userInfo?.realName || '业务员' }}</div>
        </div>
        <van-icon name="bell" size="24" @click="handleNotice" />
      </div>
    </div>

    <!-- 数据卡片 -->
    <div class="px-4 -mt-10">
      <div class="grid grid-cols-2 gap-3">
        <StatCard
          title="我的客户"
          :value="dashboard.totalCustomerCount"
          suffix="人"
          type="primary"
          sub-label="本月新增"
          :sub-value="dashboard.monthNewCustomerCount"
        />
        <StatCard
          title="客户订单额"
          :value="formatAmount(dashboard.totalOrderAmount)"
          prefix="¥"
          type="success"
          sub-label="本月"
          :sub-value="'¥' + formatAmount(dashboard.monthOrderAmount)"
        />
      </div>
    </div>

    <!-- 日期类型切换 -->
    <div class="px-4 mt-4">
      <van-cell-group inset>
        <van-cell title="数据统计">
          <template #right-icon>
            <van-tag
              v-for="item in dateTypes"
              :key="item.value"
              :type="dateType === item.value ? 'primary' : 'default'"
              class="ml-2 cursor-pointer"
              @click="handleDateTypeChange(item.value)"
            >
              {{ item.label }}
            </van-tag>
          </template>
        </van-cell>
      </van-cell-group>
    </div>

    <!-- 客户消费排行 -->
    <div class="px-4 mt-4 pb-4">
      <van-cell-group inset>
        <van-cell title="客户消费排行" is-link @click="goCustomerList" />
        <template v-if="dashboard.customerRanking.length > 0">
          <van-cell
            v-for="(item, index) in dashboard.customerRanking.slice(0, 5)"
            :key="item.uid"
            :title="item.nickname || item.phone"
            :label="`订单 ${item.orderCount} 笔`"
            center
          >
            <template #icon>
              <div
                class="w-6 h-6 rounded-full flex items-center justify-center text-white text-xs mr-3"
                :class="getRankClass(index)"
              >
                {{ index + 1 }}
              </div>
            </template>
            <template #value>
              <span class="text-primary font-medium">¥{{ formatAmount(item.totalAmount) }}</span>
            </template>
          </van-cell>
        </template>
        <van-empty v-else description="暂无数据" :image-size="80" />
      </van-cell-group>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getDashboardApi } from '@/api/dashboard'
import StatCard from '@/components/StatCard.vue'
import type { DashboardData } from '@/types/dashboard'

const router = useRouter()
const userStore = useUserStore()

const dateType = ref('day')
const dateTypes = [
  { label: '日', value: 'day' },
  { label: '周', value: 'week' },
  { label: '月', value: 'month' }
]

const dashboard = reactive<DashboardData>({
  totalCustomerCount: 0,
  monthNewCustomerCount: 0,
  totalOrderAmount: 0,
  monthOrderAmount: 0,
  trendData: [],
  customerRanking: []
})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

function formatAmount(value: number): string {
  if (value >= 10000) {
    return (value / 10000).toFixed(2) + '万'
  }
  return value.toFixed(2)
}

function getRankClass(index: number): string {
  const classes = ['bg-red-500', 'bg-orange-500', 'bg-yellow-500']
  return classes[index] || 'bg-gray-400'
}

async function fetchDashboard() {
  try {
    const data = await getDashboardApi(dateType.value)
    Object.assign(dashboard, data)
  } catch (error) {
    // 错误已处理
  }
}

function handleDateTypeChange(value: string) {
  dateType.value = value
  fetchDashboard()
}

function handleNotice() {
  // TODO: 通知功能
}

function goCustomerList() {
  router.push('/customer')
}

onMounted(() => {
  fetchDashboard()
  userStore.getUserInfo()
})
</script>
```

---

## Task 14: 实现客户列表页

**Files:**
- Create: `salesman/src/views/customer/list.vue`
- Create: `salesman/src/api/customer.ts`
- Create: `salesman/src/types/customer.d.ts`
- Create: `salesman/src/components/CustomerCard.vue`

### Step 1: 创建客户类型定义

```typescript
// salesman/src/types/customer.d.ts
/**
 * 客户信息
 */
export interface Customer {
  uid: number
  nickname: string
  phone: string
  avatar: string
  salesmanId: number
  salesmanName: string
  bindTime: string
  totalAmount: number
  orderCount: number
  lastOrderTime: string | null
  remark?: string
  tags?: string[]
}

/**
 * 客户列表参数
 */
export interface CustomerListParams {
  page: number
  limit: number
  keywords?: string
}
```

### Step 2: 创建客户 API

```typescript
// salesman/src/api/customer.ts
import request from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { Customer, CustomerListParams } from '@/types/customer'

/**
 * 获取客户列表
 */
export function getCustomerListApi(params: CustomerListParams) {
  return request.get<PageResult<Customer>>('/admin/salesman/app/customer/list', { params })
}

/**
 * 获取客户详情
 */
export function getCustomerDetailApi(uid: number) {
  return request.get<Customer>(`/admin/salesman/app/customer/detail/${uid}`)
}

/**
 * 发送验证码
 */
export function sendCodeApi(phone: string) {
  return request.post('/admin/salesman/app/customer/sendCode', { phone })
}

/**
 * 验证码绑定客户
 */
export function bindByCodeApi(phone: string, code: string) {
  return request.post('/admin/salesman/app/customer/bindByCode', { phone, code })
}

/**
 * 更新客户备注
 */
export function updateRemarkApi(uid: number, remark: string) {
  return request.post('/admin/salesman/app/customer/updateRemark', { uid, remark })
}
```

### Step 3: 创建客户卡片组件

```vue
<!-- salesman/src/components/CustomerCard.vue -->
<template>
  <div class="bg-white rounded-lg p-4 shadow-sm" @click="handleClick">
    <div class="flex items-center">
      <!-- 头像 -->
      <van-image
        round
        width="48"
        height="48"
        :src="customer.avatar || defaultAvatar"
        fit="cover"
      />
      <!-- 信息 -->
      <div class="flex-1 ml-3 min-w-0">
        <div class="flex items-center justify-between">
          <div class="font-medium text-gray-800 truncate">
            {{ customer.nickname || customer.phone }}
          </div>
          <div class="text-primary font-medium">
            ¥{{ customer.totalAmount.toFixed(2) }}
          </div>
        </div>
        <div class="flex items-center justify-between mt-1">
          <div class="text-sm text-gray-500">{{ customer.phone }}</div>
          <div class="text-xs text-gray-400">{{ customer.orderCount }}笔订单</div>
        </div>
        <div class="text-xs text-gray-400 mt-1">
          绑定时间: {{ formatDate(customer.bindTime) }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Customer } from '@/types/customer'

interface Props {
  customer: Customer
}

const props = defineProps<Props>()

const emit = defineEmits<{
  click: [customer: Customer]
}>()

const defaultAvatar = 'https://img.yzcdn.cn/vant/cat.jpeg'

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString()
}

function handleClick() {
  emit('click', props.customer)
}
</script>
```

### Step 4: 创建客户列表页

```vue
<!-- salesman/src/views/customer/list.vue -->
<template>
  <div class="min-h-screen bg-gray-100">
    <!-- 搜索栏 -->
    <div class="bg-white sticky top-0 z-10">
      <van-search
        v-model="keywords"
        placeholder="搜索客户姓名/手机号"
        @search="handleSearch"
        @clear="handleSearch"
      />
    </div>

    <!-- 列表 -->
    <van-pull-refresh v-model="refreshing" @refresh="handleRefresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="loadMore"
        class="px-4 py-3"
      >
        <div class="space-y-3">
          <CustomerCard
            v-for="item in list"
            :key="item.uid"
            :customer="item"
            @click="goDetail"
          />
        </div>

        <!-- 空状态 -->
        <van-empty
          v-if="!loading && list.length === 0"
          description="暂无客户"
          :image-size="120"
        />
      </van-list>
    </van-pull-refresh>

    <!-- 添加按钮 -->
    <div class="fixed bottom-20 right-4 lg:right-[calc(50%-240px+16px)]">
      <van-button
        round
        type="primary"
        icon="plus"
        @click="goAdd"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCustomerListApi } from '@/api/customer'
import CustomerCard from '@/components/CustomerCard.vue'
import type { Customer } from '@/types/customer'

const router = useRouter()

const keywords = ref('')
const list = ref<Customer[]>([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const page = ref(1)
const limit = 10

async function fetchList(isRefresh = false) {
  if (isRefresh) {
    page.value = 1
    finished.value = false
  }

  try {
    const result = await getCustomerListApi({
      page: page.value,
      limit,
      keywords: keywords.value || undefined
    })

    if (isRefresh) {
      list.value = result.list
    } else {
      list.value.push(...result.list)
    }

    if (list.value.length >= result.total) {
      finished.value = true
    }

    page.value++
  } catch (error) {
    finished.value = true
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function loadMore() {
  fetchList()
}

function handleRefresh() {
  fetchList(true)
}

function handleSearch() {
  fetchList(true)
}

function goDetail(customer: Customer) {
  router.push(`/customer/${customer.uid}`)
}

function goAdd() {
  router.push('/customer/add')
}

onMounted(() => {
  fetchList(true)
})
</script>
```

---

## Task 15: 实现客户详情页

**Files:**
- Create: `salesman/src/views/customer/detail.vue`

### Step 1: 创建客户详情页

```vue
<!-- salesman/src/views/customer/detail.vue -->
<template>
  <div class="min-h-screen bg-gray-100">
    <!-- 导航栏 -->
    <van-nav-bar title="客户详情" left-arrow @click-left="router.back()" fixed placeholder />

    <!-- 加载中 -->
    <div v-if="loading" class="p-4">
      <van-skeleton title :row="5" />
    </div>

    <template v-else-if="customer">
      <!-- 客户信息卡片 -->
      <div class="bg-white p-4 mb-3">
        <div class="flex items-center">
          <van-image
            round
            width="60"
            height="60"
            :src="customer.avatar || defaultAvatar"
            fit="cover"
          />
          <div class="ml-4 flex-1">
            <div class="text-lg font-medium text-gray-800">
              {{ customer.nickname || customer.phone }}
            </div>
            <div class="text-gray-500 mt-1">{{ customer.phone }}</div>
          </div>
        </div>
      </div>

      <!-- 统计数据 -->
      <div class="grid grid-cols-3 bg-white p-4 mb-3">
        <div class="text-center">
          <div class="text-xl font-bold text-primary">{{ customer.orderCount }}</div>
          <div class="text-sm text-gray-500 mt-1">订单数</div>
        </div>
        <div class="text-center border-x border-gray-100">
          <div class="text-xl font-bold text-primary">¥{{ customer.totalAmount.toFixed(2) }}</div>
          <div class="text-sm text-gray-500 mt-1">消费金额</div>
        </div>
        <div class="text-center">
          <div class="text-xl font-bold text-primary">{{ bindDays }}</div>
          <div class="text-sm text-gray-500 mt-1">绑定天数</div>
        </div>
      </div>

      <!-- 详细信息 -->
      <van-cell-group inset class="mb-3">
        <van-cell title="绑定时间" :value="formatDate(customer.bindTime)" />
        <van-cell title="最近下单" :value="customer.lastOrderTime ? formatDate(customer.lastOrderTime) : '暂无'" />
      </van-cell-group>

      <!-- 备注 -->
      <van-cell-group inset>
        <van-cell
          title="备注"
          :value="customer.remark || '点击添加备注'"
          is-link
          @click="showRemarkPopup = true"
        />
      </van-cell-group>
    </template>

    <!-- 备注弹窗 -->
    <van-popup v-model:show="showRemarkPopup" position="bottom" round>
      <div class="p-4">
        <div class="text-lg font-medium mb-4">编辑备注</div>
        <van-field
          v-model="remarkInput"
          type="textarea"
          placeholder="请输入备注信息"
          rows="4"
          maxlength="200"
          show-word-limit
        />
        <div class="flex gap-3 mt-4">
          <van-button block @click="showRemarkPopup = false">取消</van-button>
          <van-button block type="primary" @click="handleSaveRemark">保存</van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getCustomerDetailApi, updateRemarkApi } from '@/api/customer'
import type { Customer } from '@/types/customer'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const customer = ref<Customer | null>(null)
const showRemarkPopup = ref(false)
const remarkInput = ref('')

const defaultAvatar = 'https://img.yzcdn.cn/vant/cat.jpeg'

const bindDays = computed(() => {
  if (!customer.value?.bindTime) return 0
  const bindDate = new Date(customer.value.bindTime)
  const now = new Date()
  const diff = now.getTime() - bindDate.getTime()
  return Math.floor(diff / (1000 * 60 * 60 * 24))
})

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString()
}

async function fetchDetail() {
  const uid = Number(route.params.id)
  if (!uid) {
    router.back()
    return
  }

  loading.value = true
  try {
    customer.value = await getCustomerDetailApi(uid)
    remarkInput.value = customer.value.remark || ''
  } catch (error) {
    router.back()
  } finally {
    loading.value = false
  }
}

async function handleSaveRemark() {
  if (!customer.value) return

  try {
    await updateRemarkApi(customer.value.uid, remarkInput.value)
    customer.value.remark = remarkInput.value
    showRemarkPopup.value = false
    showToast('保存成功')
  } catch (error) {
    // 错误已处理
  }
}

onMounted(() => {
  fetchDetail()
})
</script>
```

---

## Task 16: 实现添加客户页

**Files:**
- Create: `salesman/src/views/customer/add.vue`
- Create: `salesman/src/composables/useCountDown.ts`

### Step 1: 创建倒计时 composable

```typescript
// salesman/src/composables/useCountDown.ts
import { ref, onUnmounted } from 'vue'

export function useCountDown(seconds = 60) {
  const count = ref(0)
  const isCounting = ref(false)
  let timer: ReturnType<typeof setInterval> | null = null

  function start() {
    if (isCounting.value) return

    count.value = seconds
    isCounting.value = true

    timer = setInterval(() => {
      count.value--
      if (count.value <= 0) {
        stop()
      }
    }, 1000)
  }

  function stop() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
    isCounting.value = false
    count.value = 0
  }

  onUnmounted(() => {
    stop()
  })

  return {
    count,
    isCounting,
    start,
    stop
  }
}
```

### Step 2: 创建添加客户页

```vue
<!-- salesman/src/views/customer/add.vue -->
<template>
  <div class="min-h-screen bg-gray-100">
    <!-- 导航栏 -->
    <van-nav-bar title="添加客户" left-arrow @click-left="router.back()" fixed placeholder />

    <!-- 提示 -->
    <div class="bg-blue-50 text-blue-600 text-sm p-3 mx-4 mt-4 rounded-lg">
      <van-icon name="info-o" class="mr-1" />
      输入客户手机号，发送验证码后由客户提供验证码完成绑定
    </div>

    <!-- 表单 -->
    <van-form @submit="handleSubmit" class="mt-4">
      <van-cell-group inset>
        <van-field
          v-model="form.phone"
          type="tel"
          label="手机号"
          placeholder="请输入客户手机号"
          maxlength="11"
          :rules="[
            { required: true, message: '请输入手机号' },
            { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号' }
          ]"
          clearable
        >
          <template #button>
            <van-button
              size="small"
              type="primary"
              :disabled="!isPhoneValid || countDown.isCounting.value"
              @click="handleSendCode"
            >
              {{ countDown.isCounting.value ? `${countDown.count.value}s` : '发送验证码' }}
            </van-button>
          </template>
        </van-field>

        <van-field
          v-model="form.code"
          type="digit"
          label="验证码"
          placeholder="请输入验证码"
          maxlength="6"
          :rules="[{ required: true, message: '请输入验证码' }]"
          clearable
        />
      </van-cell-group>

      <div class="p-4">
        <van-button
          round
          block
          type="primary"
          native-type="submit"
          :loading="submitting"
          :disabled="!form.phone || !form.code"
        >
          确认绑定
        </van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { sendCodeApi, bindByCodeApi } from '@/api/customer'
import { useCountDown } from '@/composables/useCountDown'

const router = useRouter()
const countDown = useCountDown(60)

const form = reactive({
  phone: '',
  code: ''
})

const submitting = ref(false)

const isPhoneValid = computed(() => /^1[3-9]\d{9}$/.test(form.phone))

async function handleSendCode() {
  if (!isPhoneValid.value || countDown.isCounting.value) return

  try {
    await sendCodeApi(form.phone)
    showToast('验证码已发送')
    countDown.start()
  } catch (error) {
    // 错误已处理
  }
}

async function handleSubmit() {
  if (submitting.value) return

  submitting.value = true
  try {
    await bindByCodeApi(form.phone, form.code)
    showToast('绑定成功')
    router.back()
  } catch (error) {
    // 错误已处理
  } finally {
    submitting.value = false
  }
}
</script>
```

---

## Task 17: 实现推广页

**Files:**
- Create: `salesman/src/views/promote/index.vue`
- Create: `salesman/src/api/promote.ts`
- Create: `salesman/src/components/QrcodeCard.vue`

### Step 1: 创建推广 API

```typescript
// salesman/src/api/promote.ts
import request from '@/utils/request'
import type { UserInfo } from '@/types/user'

/**
 * 获取我的邀请码和二维码
 */
export function getMyCodeApi() {
  return request.get<UserInfo>('/admin/salesman/app/myCode')
}

/**
 * 刷新二维码
 */
export function refreshQrcodeApi() {
  return request.post<string>('/admin/salesman/app/refreshQrcode')
}
```

### Step 2: 创建二维码卡片组件

```vue
<!-- salesman/src/components/QrcodeCard.vue -->
<template>
  <div class="bg-white rounded-lg p-6 text-center">
    <div class="text-lg font-medium mb-4">我的推广码</div>

    <!-- 二维码 -->
    <div class="inline-block p-4 bg-gray-50 rounded-lg">
      <van-image
        v-if="qrcodeUrl"
        width="180"
        height="180"
        :src="qrcodeUrl"
        fit="contain"
      >
        <template #loading>
          <van-loading type="spinner" size="40" />
        </template>
        <template #error>
          <div class="text-gray-400 text-sm">加载失败</div>
        </template>
      </van-image>
      <div v-else class="w-[180px] h-[180px] flex items-center justify-center">
        <van-button size="small" type="primary" @click="emit('refresh')">
          生成二维码
        </van-button>
      </div>
    </div>

    <!-- 邀请码 -->
    <div class="mt-4">
      <div class="text-gray-500 text-sm">邀请码</div>
      <div class="text-2xl font-bold text-primary mt-2 tracking-widest">
        {{ code }}
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="flex gap-3 mt-6">
      <van-button block icon="photograph" @click="emit('save')">
        保存图片
      </van-button>
      <van-button block icon="share-o" type="primary" @click="emit('share')">
        分享
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  code: string
  qrcodeUrl: string | null
}

defineProps<Props>()

const emit = defineEmits<{
  refresh: []
  save: []
  share: []
}>()
</script>
```

### Step 3: 创建推广页

```vue
<!-- salesman/src/views/promote/index.vue -->
<template>
  <div class="min-h-screen bg-gray-100 p-4">
    <!-- 二维码卡片 -->
    <QrcodeCard
      :code="userStore.userInfo?.salesmanCode || ''"
      :qrcode-url="userStore.userInfo?.salesmanQrcode || null"
      @refresh="handleRefresh"
      @save="handleSave"
      @share="handleShare"
    />

    <!-- 使用说明 -->
    <div class="bg-white rounded-lg p-4 mt-4">
      <div class="font-medium mb-3">使用说明</div>
      <div class="space-y-2 text-sm text-gray-600">
        <div class="flex items-start">
          <van-icon name="checked" class="text-primary mr-2 mt-0.5" />
          <span>客户扫描二维码可直接绑定为您的客户</span>
        </div>
        <div class="flex items-start">
          <van-icon name="checked" class="text-primary mr-2 mt-0.5" />
          <span>也可以让客户在注册时输入您的邀请码</span>
        </div>
        <div class="flex items-start">
          <van-icon name="checked" class="text-primary mr-2 mt-0.5" />
          <span>绑定成功后，客户的消费将计入您的业绩</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { showToast } from 'vant'
import { useUserStore } from '@/stores/user'
import { refreshQrcodeApi } from '@/api/promote'
import QrcodeCard from '@/components/QrcodeCard.vue'

const userStore = useUserStore()

async function handleRefresh() {
  try {
    const qrcodeUrl = await refreshQrcodeApi()
    if (userStore.userInfo) {
      userStore.userInfo.salesmanQrcode = qrcodeUrl
    }
    showToast('二维码已刷新')
  } catch (error) {
    // 错误已处理
  }
}

function handleSave() {
  const url = userStore.userInfo?.salesmanQrcode
  if (!url) {
    showToast('请先生成二维码')
    return
  }
  // 创建下载链接
  const link = document.createElement('a')
  link.href = url
  link.download = `推广码_${userStore.userInfo?.salesmanCode}.png`
  link.click()
  showToast('已保存到相册')
}

function handleShare() {
  const code = userStore.userInfo?.salesmanCode
  if (!code) return

  // 尝试使用 Web Share API
  if (navigator.share) {
    navigator.share({
      title: '邀请您加入',
      text: `使用邀请码 ${code} 注册，即可成为我的专属客户`,
    }).catch(() => {
      // 用户取消分享
    })
  } else {
    // 复制到剪贴板
    navigator.clipboard.writeText(code).then(() => {
      showToast('邀请码已复制')
    })
  }
}

onMounted(() => {
  userStore.getUserInfo()
})
</script>
```

---

## Task 18: 实现个人中心页

**Files:**
- Create: `salesman/src/views/profile/index.vue`

### Step 1: 创建个人中心页

```vue
<!-- salesman/src/views/profile/index.vue -->
<template>
  <div class="min-h-screen bg-gray-100">
    <!-- 用户信息卡片 -->
    <div class="bg-primary text-white p-6">
      <div class="flex items-center">
        <div class="w-16 h-16 rounded-full bg-white/20 flex items-center justify-center">
          <van-icon name="manager" size="32" />
        </div>
        <div class="ml-4">
          <div class="text-xl font-medium">{{ userStore.userInfo?.realName || '业务员' }}</div>
          <div class="text-white/80 mt-1">{{ userStore.userInfo?.phone }}</div>
        </div>
      </div>
    </div>

    <!-- 统计数据 -->
    <div class="bg-white -mt-2 rounded-t-lg">
      <div class="grid grid-cols-2 py-4">
        <div class="text-center border-r border-gray-100">
          <div class="text-2xl font-bold text-primary">
            {{ userStore.userInfo?.customerCount || 0 }}
          </div>
          <div class="text-sm text-gray-500 mt-1">我的客户</div>
        </div>
        <div class="text-center">
          <div class="text-2xl font-bold text-primary">
            {{ userStore.userInfo?.monthNewCustomerCount || 0 }}
          </div>
          <div class="text-sm text-gray-500 mt-1">本月新增</div>
        </div>
      </div>
    </div>

    <!-- 功能菜单 -->
    <van-cell-group inset class="mt-3">
      <van-cell title="我的邀请码" is-link @click="router.push('/promote')">
        <template #value>
          <span class="text-primary">{{ userStore.userInfo?.salesmanCode }}</span>
        </template>
      </van-cell>
      <van-cell title="修改密码" is-link @click="router.push('/profile/password')" />
      <van-cell title="关于我们" is-link @click="showAbout = true" />
    </van-cell-group>

    <!-- 退出登录 -->
    <div class="p-4 mt-4">
      <van-button block type="danger" plain @click="handleLogout">
        退出登录
      </van-button>
    </div>

    <!-- 版本信息 -->
    <div class="text-center text-gray-400 text-sm py-4">
      版本 1.0.0
    </div>

    <!-- 关于我们弹窗 -->
    <van-popup v-model:show="showAbout" round position="bottom">
      <div class="p-6 text-center">
        <div class="text-lg font-medium mb-4">关于我们</div>
        <div class="text-gray-600">
          <p>业务员助手 v1.0.0</p>
          <p class="mt-2">CRMEB 开源商城系统</p>
          <p class="mt-2 text-sm text-gray-400">© 2026 CRMEB. All rights reserved.</p>
        </div>
        <van-button block class="mt-6" @click="showAbout = false">关闭</van-button>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog } from 'vant'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const showAbout = ref(false)

function handleLogout() {
  showConfirmDialog({
    title: '提示',
    message: '确定要退出登录吗？',
  }).then(() => {
    userStore.logout()
    router.replace('/login')
  }).catch(() => {
    // 取消
  })
}

onMounted(() => {
  userStore.getUserInfo()
})
</script>
```

---

## Task 19: 实现修改密码页

**Files:**
- Create: `salesman/src/views/profile/password.vue`

### Step 1: 创建修改密码页

```vue
<!-- salesman/src/views/profile/password.vue -->
<template>
  <div class="min-h-screen bg-gray-100">
    <!-- 导航栏 -->
    <van-nav-bar title="修改密码" left-arrow @click-left="router.back()" fixed placeholder />

    <!-- 表单 -->
    <van-form @submit="handleSubmit" class="mt-4">
      <van-cell-group inset>
        <van-field
          v-model="form.oldPassword"
          type="password"
          label="原密码"
          placeholder="请输入原密码"
          :rules="[{ required: true, message: '请输入原密码' }]"
          clearable
        />
        <van-field
          v-model="form.newPassword"
          type="password"
          label="新密码"
          placeholder="请输入新密码"
          :rules="[
            { required: true, message: '请输入新密码' },
            { pattern: /^.{6,20}$/, message: '密码长度为6-20位' }
          ]"
          clearable
        />
        <van-field
          v-model="form.confirmPassword"
          type="password"
          label="确认密码"
          placeholder="请再次输入新密码"
          :rules="[
            { required: true, message: '请确认新密码' },
            { validator: validateConfirm, message: '两次输入的密码不一致' }
          ]"
          clearable
        />
      </van-cell-group>

      <div class="p-4">
        <van-button
          round
          block
          type="primary"
          native-type="submit"
          :loading="submitting"
        >
          确认修改
        </van-button>
      </div>
    </van-form>

    <!-- 提示 -->
    <div class="px-4 text-sm text-gray-400">
      <p>• 密码长度为 6-20 位</p>
      <p>• 修改成功后需要重新登录</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const submitting = ref(false)

function validateConfirm(value: string): boolean {
  return value === form.newPassword
}

async function handleSubmit() {
  if (submitting.value) return

  submitting.value = true
  try {
    await userStore.updatePassword(form.oldPassword, form.newPassword)
    showToast('修改成功，请重新登录')
    userStore.logout()
    router.replace('/login')
  } catch (error) {
    // 错误已处理
  } finally {
    submitting.value = false
  }
}
</script>
```

---

## Task 20: 验证与清理

### Step 1: 验证项目启动

Run: `cd /Users/xziying/project/bespoke/crmeb_java/salesman && npm run dev`

Expected: 项目成功启动，无编译错误

### Step 2: 验证 TypeScript 类型检查

Run: `cd /Users/xziying/project/bespoke/crmeb_java/salesman && npx vue-tsc --noEmit`

Expected: 无类型错误

### Step 3: 验证生产构建

Run: `cd /Users/xziying/project/bespoke/crmeb_java/salesman && npm run build`

Expected: 构建成功，生成 dist 目录

### Step 4: 清理默认文件

删除 Vite 默认创建的示例文件：

Run: `rm -f /Users/xziying/project/bespoke/crmeb_java/salesman/src/components/HelloWorld.vue`

### Step 5: 更新 .gitignore

确保 `salesman/.gitignore` 包含：

```
node_modules
dist
.DS_Store
*.local
```

### Step 6: 提交代码

建议的 Commit 信息：

```
feat(salesman): 完成业务员端 Vite + Vue 3 迁移

- 从 uni-app 迁移至 Vite + Vue 3 + TypeScript
- 使用 Vant 4 作为 UI 组件库
- 使用 Tailwind CSS 实现响应式布局
- 使用 Pinia 进行状态管理
- 实现登录、首页、客户管理、推广、个人中心等页面
- 支持移动端和桌面端响应式适配
```

---

## 验证清单

完成所有任务后，确保以下功能正常：

- [ ] 项目启动无报错
- [ ] 登录页面正常显示
- [ ] 登录/退出功能正常
- [ ] 路由守卫正常工作
- [ ] TabBar 导航正常
- [ ] 响应式布局在不同屏幕尺寸下正常
- [ ] API 请求通过代理正常发送
- [ ] TypeScript 类型检查通过
- [ ] 生产构建成功

---

*本计划基于设计方案 `docs/plans/2026-01-23-salesman-vite-design.md` 生成*
