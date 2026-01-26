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
