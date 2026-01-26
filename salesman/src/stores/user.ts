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
    pick: ['token']
  }
})
