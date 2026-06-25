package com.zbkj.front.service;

import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.IndexProductResponse;

/**
 * 促销活动商品 Service 接口
 */
public interface PromotionService {

    /**
     * 获取买赠活动关联的购买商品列表
     * @param pageParamRequest 分页参数
     * @return 商品分页列表
     */
    CommonPage<IndexProductResponse> getBuyGiftProducts(PageParamRequest pageParamRequest);

    /**
     * 获取满减活动关联的商品列表
     * @param pageParamRequest 分页参数
     * @return 商品分页列表
     */
    CommonPage<IndexProductResponse> getFullReductionProducts(PageParamRequest pageParamRequest);
}
