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
router.beforeEach((to, _from, next) => {
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
