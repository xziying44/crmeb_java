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
 * @description 库存列表
 */
export function stockListApi(params) {
  return request({
    url: '/admin/stock/list',
    method: 'get',
    params,
  });
}

/**
 * @description 库存流水列表
 */
export function stockLogListApi(params) {
  return request({
    url: '/admin/stock/log/list',
    method: 'get',
    params,
  });
}

/**
 * @description 手动入库
 */
export function stockInApi(data) {
  return request({
    url: '/admin/stock/in',
    method: 'post',
    data,
  });
}

/**
 * @description 手动出库
 */
export function stockOutApi(data) {
  return request({
    url: '/admin/stock/out',
    method: 'post',
    data,
  });
}

/**
 * @description 设置预警库存
 */
export function stockWarningUpdateApi(params) {
  return request({
    url: '/admin/stock/warning/update',
    method: 'post',
    params,
  });
}

/**
 * @description 初始化库存
 */
export function stockInitApi() {
  return request({
    url: '/admin/stock/init',
    method: 'post',
  });
}

/**
 * @description 供应商列表
 */
export function supplierListApi(params) {
  return request({
    url: '/admin/stock/supplier/list',
    method: 'get',
    params,
  });
}

/**
 * @description 启用供应商（下拉用）
 */
export function supplierEnabledListApi() {
  return request({
    url: '/admin/stock/supplier/enabledList',
    method: 'get',
  });
}

/**
 * @description 添加供应商
 */
export function supplierAddApi(data) {
  return request({
    url: '/admin/stock/supplier/add',
    method: 'post',
    data,
  });
}

/**
 * @description 编辑供应商
 */
export function supplierUpdateApi(data) {
  return request({
    url: '/admin/stock/supplier/update',
    method: 'post',
    data,
  });
}

/**
 * @description 删除供应商
 */
export function supplierDeleteApi(id) {
  return request({
    url: `/admin/stock/supplier/delete/${id}`,
    method: 'post',
  });
}

/**
 * @description 更新供应商状态
 */
export function supplierUpdateStatusApi(id, status) {
  return request({
    url: '/admin/stock/supplier/updateStatus',
    method: 'post',
    params: { id, status },
  });
}

/**
 * @description 采购单列表
 */
export function purchaseListApi(params) {
  return request({
    url: '/admin/stock/purchase/list',
    method: 'get',
    params,
  });
}

/**
 * @description 采购单详情
 */
export function purchaseDetailApi(id) {
  return request({
    url: `/admin/stock/purchase/detail/${id}`,
    method: 'get',
  });
}

/**
 * @description 创建采购单
 */
export function purchaseAddApi(data) {
  return request({
    url: '/admin/stock/purchase/add',
    method: 'post',
    data,
  });
}

/**
 * @description 取消采购单
 */
export function purchaseCancelApi(id) {
  return request({
    url: `/admin/stock/purchase/cancel/${id}`,
    method: 'post',
  });
}

/**
 * @description 采购入库
 */
export function purchaseInStockApi(data) {
  return request({
    url: '/admin/stock/purchase/inStock',
    method: 'post',
    data,
  });
}

/**
 * @description 盘点单列表
 */
export function stockCheckListApi(params) {
  return request({
    url: '/admin/stock/check/list',
    method: 'get',
    params,
  });
}

/**
 * @description 盘点单详情
 */
export function stockCheckDetailApi(id) {
  return request({
    url: `/admin/stock/check/detail/${id}`,
    method: 'get',
  });
}

/**
 * @description 创建盘点单
 */
export function stockCheckCreateApi(data) {
  return request({
    url: '/admin/stock/check/create',
    method: 'post',
    data,
  });
}

/**
 * @description 更新盘点数量
 */
export function stockCheckUpdateItemApi(data) {
  return request({
    url: '/admin/stock/check/updateItem',
    method: 'post',
    data,
  });
}

/**
 * @description 确认盘点完成
 */
export function stockCheckConfirmApi(id) {
  return request({
    url: `/admin/stock/check/confirm/${id}`,
    method: 'post',
  });
}

/**
 * @description 取消盘点单
 */
export function stockCheckCancelApi(id) {
  return request({
    url: `/admin/stock/check/cancel/${id}`,
    method: 'post',
  });
}

/**
 * @description 报表：变动汇总
 */
export function stockReportSummaryApi(params) {
  return request({
    url: '/admin/stock/report/summary',
    method: 'get',
    params,
  });
}

/**
 * @description 报表：采购统计
 */
export function stockReportPurchaseApi(params) {
  return request({
    url: '/admin/stock/report/purchase',
    method: 'get',
    params,
  });
}

/**
 * @description 报表：盘点差异
 */
export function stockReportCheckDiffApi(params) {
  return request({
    url: '/admin/stock/report/checkDiff',
    method: 'get',
    params,
  });
}

