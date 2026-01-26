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
