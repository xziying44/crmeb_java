package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.stock.Stock;
import com.zbkj.common.response.StockResponse;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 库存 Mapper 接口
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
public interface StockDao extends BaseMapper<Stock> {

    /**
     * 安全扣减库存（防止超卖）
     * @param productId 商品ID
     * @param attrValueId 规格ID
     * @param quantity 扣减数量（正数）
     * @return 影响行数
     */
    @Update("UPDATE eb_stock SET stock = stock - #{quantity}, update_time = NOW() " +
            "WHERE product_id = #{productId} AND attr_value_id = #{attrValueId} AND stock >= #{quantity}")
    int deductStock(@Param("productId") Integer productId,
                    @Param("attrValueId") Integer attrValueId,
                    @Param("quantity") Integer quantity);

    /**
     * 增加库存
     * @param productId 商品ID
     * @param attrValueId 规格ID
     * @param quantity 增加数量（正数）
     * @return 影响行数
     */
    @Update("UPDATE eb_stock SET stock = stock + #{quantity}, update_time = NOW() " +
            "WHERE product_id = #{productId} AND attr_value_id = #{attrValueId}")
    int addStock(@Param("productId") Integer productId,
                 @Param("attrValueId") Integer attrValueId,
                 @Param("quantity") Integer quantity);

    /**
     * 库存分页列表（包含商品/规格信息）
     */
    List<StockResponse> selectStockList(@Param("keywords") String keywords,
                                        @Param("warning") Boolean warning);

    /**
     * 按商品汇总库存（所有规格求和）
     */
    Integer sumStockByProductId(@Param("productId") Integer productId);

    /**
     * 同步库存值（插入或更新）
     */
    @Insert("INSERT INTO eb_stock (product_id, attr_value_id, stock, warning_stock, create_time, update_time) " +
            "VALUES (#{productId}, #{attrValueId}, #{stock}, 0, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE stock = VALUES(stock), update_time = NOW()")
    int upsertStock(@Param("productId") Integer productId,
                    @Param("attrValueId") Integer attrValueId,
                    @Param("stock") Integer stock);
}
