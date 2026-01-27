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
 * 满减活动 API
 */
export function fullReductionListApi(params) {
  return request({
    url: '/admin/promotion/full-reduction/list',
    method: 'get',
    params,
  });
}

export function fullReductionDetailApi(id) {
  return request({
    url: `/admin/promotion/full-reduction/detail/${id}`,
    method: 'get',
  });
}

export function fullReductionSaveApi(data) {
  return request({
    url: '/admin/promotion/full-reduction/save',
    method: 'post',
    data,
  });
}

export function fullReductionDeleteApi(id) {
  return request({
    url: `/admin/promotion/full-reduction/delete/${id}`,
    method: 'post',
  });
}

export function fullReductionUpdateStatusApi(id, status) {
  return request({
    url: '/admin/promotion/full-reduction/updateStatus',
    method: 'post',
    params: { id, status },
  });
}

/**
 * 买赠活动 API
 */
export function buyGiftListApi(params) {
  return request({
    url: '/admin/promotion/buy-gift/list',
    method: 'get',
    params,
  });
}

export function buyGiftDetailApi(id) {
  return request({
    url: `/admin/promotion/buy-gift/detail/${id}`,
    method: 'get',
  });
}

export function buyGiftSaveApi(data) {
  return request({
    url: '/admin/promotion/buy-gift/save',
    method: 'post',
    data,
  });
}

export function buyGiftDeleteApi(id) {
  return request({
    url: `/admin/promotion/buy-gift/delete/${id}`,
    method: 'post',
  });
}

export function buyGiftUpdateStatusApi(id, status) {
  return request({
    url: '/admin/promotion/buy-gift/updateStatus',
    method: 'post',
    params: { id, status },
  });
}

