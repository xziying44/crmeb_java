package com.zbkj.front.controller;

import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.IndexProductResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.front.service.PromotionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 促销活动商品 Controller
 */
@Slf4j
@RestController("PromotionController")
@RequestMapping("api/front/promotion")
@Api(tags = "促销活动商品")
public class PromotionController {

    @Autowired
    private PromotionService promotionService;

    /**
     * 买赠活动关联商品列表
     */
    @ApiOperation(value = "买赠活动商品列表")
    @RequestMapping(value = "/buy-gift/products", method = RequestMethod.GET)
    public CommonResult<CommonPage<IndexProductResponse>> getBuyGiftProducts(@Validated PageParamRequest pageParamRequest) {
        return CommonResult.success(promotionService.getBuyGiftProducts(pageParamRequest));
    }

    /**
     * 满减活动关联商品列表
     */
    @ApiOperation(value = "满减活动商品列表")
    @RequestMapping(value = "/full-reduction/products", method = RequestMethod.GET)
    public CommonResult<CommonPage<IndexProductResponse>> getFullReductionProducts(@Validated PageParamRequest pageParamRequest) {
        return CommonResult.success(promotionService.getFullReductionProducts(pageParamRequest));
    }
}
