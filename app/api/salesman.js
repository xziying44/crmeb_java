// +----------------------------------------------------------------------
// | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
// +----------------------------------------------------------------------
// | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
// +----------------------------------------------------------------------
// | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
// +----------------------------------------------------------------------
// | Author: CRMEB Team <admin@crmeb.com>
// +----------------------------------------------------------------------

import request from '@/utils/request.js';

/**
 * @description 校验邀请码是否有效
 */
export function checkSalesmanCode(code) {
  return request.get('salesman/checkCode', { code }, { noAuth: true });
}

/**
 * @description 绑定业务员
 * 说明：后端使用 @RequestParam 接参，需要使用 x-www-form-urlencoded
 */
export function bindSalesman(code) {
  return request.post('salesman/bind', { code }, {}, 1);
}

/**
 * @description 获取我的绑定信息
 */
export function mySalesmanBind() {
  return request.get('salesman/myBind');
}

