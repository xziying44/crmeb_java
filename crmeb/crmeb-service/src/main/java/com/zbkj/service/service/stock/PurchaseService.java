package com.zbkj.service.service.stock;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.stock.Purchase;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.PurchaseAddRequest;
import com.zbkj.common.request.PurchaseInStockRequest;
import com.zbkj.common.response.PurchaseResponse;

/**
 * 采购管理服务
 */
public interface PurchaseService extends IService<Purchase> {

    /**
     * 采购单分页列表
     */
    CommonPage<PurchaseResponse> getList(String purchaseNo, Integer supplierId, Integer status, PageParamRequest pageParamRequest);

    /**
     * 采购单详情
     */
    PurchaseResponse getDetail(Integer id);

    /**
     * 创建采购单
     */
    Boolean create(PurchaseAddRequest request);

    /**
     * 取消采购单
     */
    Boolean cancel(Integer id);

    /**
     * 采购入库（支持分批）
     */
    Boolean inStock(PurchaseInStockRequest request);
}

