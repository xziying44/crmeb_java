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

const stockRouter = {
  path: '/stock',
  component: Layout,
  redirect: '/stock/list',
  name: 'Stock',
  meta: {
    title: '库存',
    icon: 's-goods',
  },
  children: [
    {
      path: 'list',
      component: () => import('@/views/stock/list/index'),
      name: 'StockList',
      meta: { title: '库存列表', icon: '' },
    },
    {
      path: 'log',
      component: () => import('@/views/stock/log/index'),
      name: 'StockLog',
      meta: { title: '库存流水', icon: '' },
    },
    {
      path: 'supplier',
      component: () => import('@/views/stock/supplier/index'),
      name: 'StockSupplier',
      meta: { title: '供应商管理', icon: '' },
    },
    {
      path: 'purchase',
      component: () => import('@/views/stock/purchase/index'),
      name: 'StockPurchase',
      meta: { title: '采购单管理', icon: '' },
    },
    {
      path: 'in',
      component: () => import('@/views/stock/in/index'),
      name: 'StockIn',
      meta: { title: '手动入库', icon: '' },
    },
    {
      path: 'out',
      component: () => import('@/views/stock/out/index'),
      name: 'StockOut',
      meta: { title: '手动出库', icon: '' },
    },
    {
      path: 'check',
      component: () => import('@/views/stock/check/index'),
      name: 'StockCheck',
      meta: { title: '库存盘点', icon: '' },
    },
    {
      path: 'report/summary',
      component: () => import('@/views/stock/report/summary'),
      name: 'StockReportSummary',
      meta: { title: '变动汇总', icon: '' },
    },
    {
      path: 'report/purchase',
      component: () => import('@/views/stock/report/purchase'),
      name: 'StockReportPurchase',
      meta: { title: '采购统计', icon: '' },
    },
    {
      path: 'report/checkDiff',
      component: () => import('@/views/stock/report/checkDiff'),
      name: 'StockReportCheckDiff',
      meta: { title: '盘点差异', icon: '' },
    },
  ],
};

export default stockRouter;

