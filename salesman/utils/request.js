/**
 * 业务员端请求封装（阶段一：最小可用）
 *
 * 说明：
 * - 业务员端接口前缀：/api/admin/salesman/app
 * - token 使用 uni.setStorageSync('salesman_token', token) 保存
 */

const BASE_URL = 'http://127.0.0.1:20510';
const TOKEN_KEY = 'salesman_token';

function request(method, url, data = {}, options = {}) {
  const { noAuth = false } = options;
  const headers = {
    'content-type': 'application/json',
  };
  const token = uni.getStorageSync(TOKEN_KEY);
  if (!noAuth && token) {
    headers['Authori-zation'] = token;
  }
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${BASE_URL}/api/admin/salesman/app${url}`,
      method,
      header: headers,
      data,
      success: (res) => {
        const body = res.data || {};
        if (body.code === 200) return resolve(body.data);
        return reject(body.message || '请求失败');
      },
      fail: () => reject('网络异常'),
    });
  });
}

export default {
  get(url, params, options) {
    return request('GET', url, params, options);
  },
  post(url, data, options) {
    return request('POST', url, data, options);
  },
};

