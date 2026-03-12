// +----------------------------------------------------------------------
// | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
// +----------------------------------------------------------------------
// | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
// +----------------------------------------------------------------------
// | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
// +----------------------------------------------------------------------
// | Author: CRMEB Team <admin@crmeb.com>
// +----------------------------------------------------------------------

import request from '@/utils/request'

/**
 * 活动横幅列表
 */
export function activityBannerListApi(params) {
  return request({
    url: '/admin/activity/banner/list',
    method: 'get',
    params,
  })
}

/**
 * 新增活动横幅
 */
export function activityBannerSaveApi(data) {
  return request({
    url: '/admin/activity/banner/save',
    method: 'post',
    data,
  })
}

/**
 * 编辑活动横幅
 */
export function activityBannerUpdateApi(data) {
  return request({
    url: '/admin/activity/banner/update',
    method: 'post',
    data,
  })
}

/**
 * 删除活动横幅
 */
export function activityBannerDeleteApi(id) {
  return request({
    url: `/admin/activity/banner/delete/${id}`,
    method: 'get',
  })
}

/**
 * 更新活动横幅状态
 */
export function activityBannerStatusApi(params) {
  return request({
    url: '/admin/activity/banner/status',
    method: 'post',
    params,
  })
}
