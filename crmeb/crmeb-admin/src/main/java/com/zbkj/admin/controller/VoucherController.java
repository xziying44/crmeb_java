package com.zbkj.admin.controller;

import com.zbkj.common.model.coupon.StoreCoupon;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.StoreCouponRequest;
import com.zbkj.common.request.StoreCouponSearchRequest;
import com.zbkj.common.response.StoreCouponInfoResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.StoreCouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 代金券管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("api/admin/marketing/voucher")
@Api(tags = "营销 -- 代金券")
public class VoucherController {

    @Autowired
    private StoreCouponService storeCouponService;

    @PreAuthorize("hasAuthority('admin:marketing:voucher:list')")
    @ApiOperation(value = "代金券列表")
    @GetMapping("/list")
    public CommonResult<CommonPage<StoreCoupon>> getList(@Validated StoreCouponSearchRequest request,
                                                         @Validated PageParamRequest pageParamRequest) {
        CommonPage<StoreCoupon> page = CommonPage.restPage(storeCouponService.getVoucherList(request, pageParamRequest));
        return CommonResult.success(page);
    }

    @PreAuthorize("hasAuthority('admin:marketing:voucher:info')")
    @ApiOperation(value = "代金券详情")
    @GetMapping("/info/{id}")
    public CommonResult<StoreCouponInfoResponse> info(@PathVariable Integer id) {
        return CommonResult.success(storeCouponService.info(id));
    }

    @PreAuthorize("hasAuthority('admin:marketing:voucher:save')")
    @ApiOperation(value = "新增/编辑代金券")
    @PostMapping("/save")
    public CommonResult<Boolean> save(@RequestBody @Validated StoreCouponRequest request) {
        // 强制标记为代金券，避免误入优惠券列表
        request.setCouponType(2);
        return CommonResult.success(storeCouponService.create(request));
    }

    @PreAuthorize("hasAuthority('admin:marketing:voucher:delete')")
    @ApiOperation(value = "删除代金券")
    @PostMapping("/delete/{id}")
    public CommonResult<Boolean> delete(@PathVariable Integer id) {
        return CommonResult.success(storeCouponService.delete(id));
    }

    @PreAuthorize("hasAuthority('admin:marketing:voucher:send')")
    @ApiOperation(value = "发放代金券给用户")
    @PostMapping("/send")
    public CommonResult<Boolean> send(@RequestParam Integer couponId, @RequestParam String userIds) {
        return CommonResult.success(storeCouponService.sendCouponToUsers(couponId, userIds));
    }
}

