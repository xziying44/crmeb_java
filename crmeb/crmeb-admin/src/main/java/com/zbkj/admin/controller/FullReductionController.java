package com.zbkj.admin.controller;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.request.FullReductionRequest;
import com.zbkj.common.request.FullReductionSearchRequest;
import com.zbkj.common.response.FullReductionResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.promotion.FullReductionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 满减活动管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("api/admin/promotion/full-reduction")
@Api(tags = "满减活动管理")
public class FullReductionController {

    @Autowired
    private FullReductionService fullReductionService;

    @PreAuthorize("hasAuthority('admin:promotion:full-reduction:list')")
    @ApiOperation(value = "满减活动列表")
    @GetMapping("/list")
    public CommonResult<PageInfo<FullReductionResponse>> getList(@Validated FullReductionSearchRequest request) {
        return CommonResult.success(fullReductionService.getList(request));
    }

    @PreAuthorize("hasAuthority('admin:promotion:full-reduction:info')")
    @ApiOperation(value = "满减活动详情")
    @GetMapping("/detail/{id}")
    public CommonResult<FullReductionResponse> getDetail(@PathVariable Integer id) {
        return CommonResult.success(fullReductionService.getDetail(id));
    }

    @PreAuthorize("hasAuthority('admin:promotion:full-reduction:save')")
    @ApiOperation(value = "新增/编辑满减活动")
    @PostMapping("/save")
    public CommonResult<Boolean> save(@RequestBody @Validated FullReductionRequest request) {
        return CommonResult.success(fullReductionService.save(request));
    }

    @PreAuthorize("hasAuthority('admin:promotion:full-reduction:delete')")
    @ApiOperation(value = "删除满减活动")
    @PostMapping("/delete/{id}")
    public CommonResult<Boolean> delete(@PathVariable Integer id) {
        return CommonResult.success(fullReductionService.delete(id));
    }

    @PreAuthorize("hasAuthority('admin:promotion:full-reduction:status')")
    @ApiOperation(value = "更新满减活动状态")
    @PostMapping("/updateStatus")
    public CommonResult<Boolean> updateStatus(@RequestParam Integer id, @RequestParam Boolean status) {
        return CommonResult.success(fullReductionService.updateStatus(id, status));
    }
}

