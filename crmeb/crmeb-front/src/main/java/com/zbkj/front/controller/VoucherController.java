package com.zbkj.front.controller;

import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.VoucherBuyRequest;
import com.zbkj.common.response.StoreCouponFrontResponse;
import com.zbkj.common.response.StoreCouponUserResponse;
import com.zbkj.common.response.VoucherBuyResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.VoucherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 代金券 Controller - 移动端
 */
@Slf4j
@RestController
@RequestMapping("api/front/voucher")
@Api(tags = "代金券")
public class VoucherController {

    @Autowired
    private VoucherService voucherService;

    /**
     * 可购买的代金券列表
     */
    @ApiOperation(value = "可购买的代金券列表")
    @GetMapping("/buyList")
    public CommonResult<List<StoreCouponFrontResponse>> getBuyList() {
        return CommonResult.success(voucherService.getBuyList());
    }

    /**
     * 购买代金券
     */
    @ApiOperation(value = "购买代金券")
    @PostMapping("/buy")
    public CommonResult<VoucherBuyResponse> buy(@RequestBody @Validated VoucherBuyRequest request) {
        return CommonResult.success(voucherService.buy(request));
    }

    /**
     * 我的代金券列表
     */
    @ApiOperation(value = "我的代金券列表")
    @GetMapping("/mine")
    public CommonResult<CommonPage<StoreCouponUserResponse>> getMine(
            @ApiParam(value = "类型：usable-可用, unusable-不可用", required = true)
            @RequestParam String type,
            @Validated PageParamRequest pageParamRequest) {
        return CommonResult.success(voucherService.getMine(type, pageParamRequest));
    }

    /**
     * 订单可用代金券列表
     */
    @ApiOperation(value = "订单可用代金券列表")
    @GetMapping("/order/{preOrderNo}")
    public CommonResult<List<StoreCouponUserResponse>> getOrderVouchers(
            @ApiParam(value = "预下单号", required = true)
            @PathVariable String preOrderNo) {
        return CommonResult.success(voucherService.getOrderVouchers(preOrderNo));
    }
}
