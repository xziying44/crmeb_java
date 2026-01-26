package com.zbkj.service.service.stock;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.stock.Stock;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.StockInRequest;
import com.zbkj.common.request.StockOutRequest;
import com.zbkj.common.response.StockLogResponse;
import com.zbkj.common.response.StockResponse;

/**
 * 库存核心服务（统一入口）
 */
public interface StockService extends IService<Stock> {

    /**
     * 库存分页列表
     * @param keywords 关键字（商品名称）
     * @param warning 是否只看预警
     */
    CommonPage<StockResponse> getStockList(String keywords, Boolean warning, PageParamRequest pageParamRequest);

    /**
     * 库存流水分页列表
     */
    CommonPage<StockLogResponse> getLogList(Integer productId, Integer type, PageParamRequest pageParamRequest);

    /**
     * 手动入库
     */
    Boolean manualIn(StockInRequest request);

    /**
     * 手动出库
     */
    Boolean manualOut(StockOutRequest request);

    /**
     * 通用入库（带关联单据信息）
     */
    Boolean stockIn(Integer productId, Integer attrValueId, Integer quantity, Integer type,
                    String relationType, Long relationId, String remark);

    /**
     * 通用出库（带关联单据信息）
     */
    Boolean stockOut(Integer productId, Integer attrValueId, Integer quantity, Integer type,
                     String relationType, Long relationId, String remark);

    /**
     * 设置预警库存
     */
    Boolean updateWarningStock(Integer productId, Integer attrValueId, Integer warningStock);

    /**
     * 销售出库（订单发货联动）
     * @param orderId 订单主键ID（eb_store_order.id）
     */
    Boolean salesOut(Integer orderId);

    /**
     * 初始化库存数据：从商品/规格库存同步到 eb_stock（一次性脚本）
     */
    Boolean initStockFromProduct();
}
