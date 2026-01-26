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
