package com.zbkj.common.constants;

/**
 * 库存相关常量
 * +----------------------------------------------------------------------
 * | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 * +----------------------------------------------------------------------
 * | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
 * +----------------------------------------------------------------------
 * | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 * +----------------------------------------------------------------------
 * | Author: CRMEB Team <admin@crmeb.com>
 * +----------------------------------------------------------------------
 */
public class StockConstants {

    // ========== 库存流水类型 ==========
    /** 采购入库 */
    public static final int LOG_TYPE_PURCHASE_IN = 1;
    /** 退货入库 */
    public static final int LOG_TYPE_RETURN_IN = 2;
    /** 盘盈入库 */
    public static final int LOG_TYPE_CHECK_PROFIT_IN = 3;
    /** 其他入库 */
    public static final int LOG_TYPE_OTHER_IN = 4;
    /** 销售出库 */
    public static final int LOG_TYPE_SALES_OUT = 5;
    /** 报损出库 */
    public static final int LOG_TYPE_DAMAGE_OUT = 6;
    /** 盘亏出库 */
    public static final int LOG_TYPE_CHECK_LOSS_OUT = 7;
    /** 其他出库 */
    public static final int LOG_TYPE_OTHER_OUT = 8;

    // ========== 采购单状态 ==========
    /** 待入库 */
    public static final int PURCHASE_STATUS_PENDING = 0;
    /** 部分入库 */
    public static final int PURCHASE_STATUS_PARTIAL = 1;
    /** 已入库 */
    public static final int PURCHASE_STATUS_COMPLETED = 2;
    /** 已取消 */
    public static final int PURCHASE_STATUS_CANCELLED = 3;

    // ========== 盘点单状态 ==========
    /** 盘点中 */
    public static final int CHECK_STATUS_CHECKING = 0;
    /** 已完成 */
    public static final int CHECK_STATUS_COMPLETED = 1;
    /** 已取消 */
    public static final int CHECK_STATUS_CANCELLED = 2;

    // ========== 关联单据类型 ==========
    /** 采购单 */
    public static final String RELATION_TYPE_PURCHASE = "purchase";
    /** 订单 */
    public static final String RELATION_TYPE_ORDER = "order";
    /** 盘点单 */
    public static final String RELATION_TYPE_CHECK = "check";
    /** 手动操作 */
    public static final String RELATION_TYPE_MANUAL = "manual";

    // ========== 单号前缀 ==========
    /** 采购单前缀 */
    public static final String PURCHASE_NO_PREFIX = "PO";
    /** 盘点单前缀 */
    public static final String CHECK_NO_PREFIX = "SC";
}

