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
