package com.zbkj.service.service.stock;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.stock.StockCheck;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.StockCheckCreateRequest;
import com.zbkj.common.request.StockCheckUpdateItemRequest;
import com.zbkj.common.response.StockCheckResponse;

/**
 * 库存盘点服务
 */
public interface StockCheckService extends IService<StockCheck> {

    /**
     * 盘点单分页列表
     */
    CommonPage<StockCheckResponse> getList(String checkNo, Integer status, PageParamRequest pageParamRequest);

    /**
     * 盘点单详情
     */
    StockCheckResponse getDetail(Integer id);

    /**
     * 创建盘点单
     */
    Boolean create(StockCheckCreateRequest request);

    /**
     * 更新盘点明细实际库存
     */
    Boolean updateItem(StockCheckUpdateItemRequest request);

    /**
     * 确认盘点完成（自动调整库存并写入流水）
     */
    Boolean confirm(Integer id);

    /**
     * 取消盘点单
     */
    Boolean cancel(Integer id);
}

