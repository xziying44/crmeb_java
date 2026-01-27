package com.zbkj.admin.controller;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.request.BuyGiftRequest;
import com.zbkj.common.request.BuyGiftSearchRequest;
import com.zbkj.common.response.BuyGiftResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.promotion.BuyGiftService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 买赠活动管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("api/admin/promotion/buy-gift")
@Api(tags = "买赠活动管理")
public class BuyGiftController {

    @Autowired
    private BuyGiftService buyGiftService;

    @PreAuthorize("hasAuthority('admin:promotion:buy-gift:list')")
    @ApiOperation(value = "买赠活动列表")
    @GetMapping("/list")
    public CommonResult<PageInfo<BuyGiftResponse>> getList(@Validated BuyGiftSearchRequest request) {
        return CommonResult.success(buyGiftService.getList(request));
    }

    @PreAuthorize("hasAuthority('admin:promotion:buy-gift:info')")
    @ApiOperation(value = "买赠活动详情")
    @GetMapping("/detail/{id}")
    public CommonResult<BuyGiftResponse> getDetail(@PathVariable Integer id) {
        return CommonResult.success(buyGiftService.getDetail(id));
    }

    @PreAuthorize("hasAuthority('admin:promotion:buy-gift:save')")
    @ApiOperation(value = "新增/编辑买赠活动")
    @PostMapping("/save")
    public CommonResult<Boolean> save(@RequestBody @Validated BuyGiftRequest request) {
        return CommonResult.success(buyGiftService.save(request));
    }

    @PreAuthorize("hasAuthority('admin:promotion:buy-gift:delete')")
    @ApiOperation(value = "删除买赠活动")
    @PostMapping("/delete/{id}")
    public CommonResult<Boolean> delete(@PathVariable Integer id) {
        return CommonResult.success(buyGiftService.delete(id));
    }

    @PreAuthorize("hasAuthority('admin:promotion:buy-gift:status')")
    @ApiOperation(value = "更新买赠活动状态")
    @PostMapping("/updateStatus")
    public CommonResult<Boolean> updateStatus(@RequestParam Integer id, @RequestParam Boolean status) {
        return CommonResult.success(buyGiftService.updateStatus(id, status));
    }
}

