import request from '@/utils/request.js';

/**
 * 登录（复用管理员登录请求体）
 */
export function loginApi(data) {
  return request.post('/login', data, { noAuth: true });
}

/**
 * 首页数据看板
 */
export function dashboardApi(params) {
  return request.get('/dashboard', params);
}

/**
 * 获取我的邀请码和二维码
 */
export function myCodeApi() {
  return request.get('/myCode', {});
}

/**
 * 我的客户列表
 */
export function customerListApi(params) {
  return request.get('/customer/list', params);
}

