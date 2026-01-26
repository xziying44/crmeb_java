// +----------------------------------------------------------------------
// | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
// +----------------------------------------------------------------------
// | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
// +----------------------------------------------------------------------
// | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
// +----------------------------------------------------------------------
// | Author: CRMEB Team <admin@crmeb.com>
// +----------------------------------------------------------------------

import Layout from '@/layout';

const salesmanRouter = {
  path: '/salesman',
  component: Layout,
  redirect: '/salesman/list',
  name: 'Salesman',
  meta: {
    title: '业务',
    icon: 'user-solid',
  },
  children: [
    {
      path: 'list',
      component: () => import('@/views/salesman/list/index'),
      name: 'SalesmanList',
      meta: { title: '业务员列表', icon: '' },
    },
    {
      path: 'bindList',
      component: () => import('@/views/salesman/bindList/index'),
      name: 'SalesmanBindList',
      meta: { title: '客户绑定记录', icon: '' },
    },
    {
      path: 'statistics',
      component: () => import('@/views/salesman/statistics/index'),
      name: 'SalesmanStatistics',
      meta: { title: '业绩统计', icon: '' },
    },
  ],
};

export default salesmanRouter;

