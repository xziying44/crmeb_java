// +----------------------------------------------------------------------
// | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
// +----------------------------------------------------------------------
// | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
// +----------------------------------------------------------------------
// | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
// +----------------------------------------------------------------------
// | Author: CRMEB Team <admin@crmeb.com>
// +----------------------------------------------------------------------

import request from '@/utils/request';

/**
 * @description 业务员列表
 */
export function salesmanListApi(params) {
  return request({
    url: '/admin/salesman/list',
    method: 'get',
    params,
  });
}

/**
 * @description 业务员详情
 */
export function salesmanInfoApi(id) {
  return request({
    url: `/admin/salesman/info/${id}`,
    method: 'get',
  });
}

/**
 * @description 创建业务员
 */
export function salesmanAddApi(data) {
  return request({
    url: '/admin/salesman/save',
    method: 'post',
    data,
  });
}

/**
 * @description 更新业务员
 */
export function salesmanUpdateApi(data) {
  return request({
    url: '/admin/salesman/update',
    method: 'post',
    data,
  });
}

/**
 * @description 删除业务员
 */
export function salesmanDeleteApi(id) {
  return request({
    url: `/admin/salesman/delete/${id}`,
    method: 'post',
  });
}

/**
 * @description 更新状态
 */
export function salesmanUpdateStatusApi(id, status) {
  return request({
    url: `/admin/salesman/updateStatus/${id}`,
    method: 'post',
    params: { status },
  });
}

/**
 * @description 重新生成邀请码
 */
export function salesmanRegenerateCodeApi(id) {
  return request({
    url: `/admin/salesman/regenerateCode/${id}`,
    method: 'post',
  });
}

/**
 * @description 生成/刷新小程序码
 */
export function salesmanGenerateQrcodeApi(id) {
  return request({
    url: `/admin/salesman/generateQrcode/${id}`,
    method: 'post',
  });
}

/**
 * @description 客户绑定记录
 */
export function salesmanBindListApi(params) {
  return request({
    url: '/admin/salesman/bindList',
    method: 'get',
    params,
  });
}

/**
 * @description 转移客户
 */
export function salesmanTransferApi(data) {
  return request({
    url: '/admin/salesman/transferCustomer',
    method: 'post',
    data,
  });
}

/**
 * @description 业绩统计
 */
export function salesmanStatisticsApi() {
  return request({
    url: '/admin/salesman/statistics',
    method: 'get',
  });
}

/**
 * @description 业务员排行榜
 */
export function salesmanRankingApi(limit = 10) {
  return request({
    url: '/admin/salesman/ranking',
    method: 'get',
    params: { limit },
  });
}

/**
 * @description 所有业务员列表（下拉用）
 */
export function salesmanAllListApi() {
  return request({
    url: '/admin/salesman/allList',
    method: 'get',
  });
}

