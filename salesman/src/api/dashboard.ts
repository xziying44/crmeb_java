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
