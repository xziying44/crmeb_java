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
