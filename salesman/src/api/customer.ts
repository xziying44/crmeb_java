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
