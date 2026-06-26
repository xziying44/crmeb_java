package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.stock.PurchaseItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 采购单明细 Mapper 接口
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
public interface PurchaseItemDao extends BaseMapper<PurchaseItem> {

    /**
     * 原子增加已入库数量：仅当"已入库 + 本次入库 <= 计划采购数量"时才更新成功。
     * 用于采购入库的并发安全，避免读-改-写导致超收/丢失更新。
     *
     * @return 影响行数（1 = 成功，0 = 会超过计划数量或记录不存在）
     */
    @Update("UPDATE eb_purchase_item SET in_quantity = in_quantity + #{inQty} " +
            "WHERE id = #{id} AND in_quantity + #{inQty} <= quantity")
    int increaseInQuantity(@Param("id") Integer id, @Param("inQty") Integer inQty);
}

