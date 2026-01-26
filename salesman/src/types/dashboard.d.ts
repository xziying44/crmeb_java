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
