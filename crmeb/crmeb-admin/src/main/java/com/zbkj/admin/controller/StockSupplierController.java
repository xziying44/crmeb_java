package com.zbkj.admin.controller;

import com.zbkj.common.model.stock.Supplier;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.SupplierRequest;
import com.zbkj.common.request.SupplierSearchRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.stock.SupplierService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 供应商管理控制器
 */
@RestController
@RequestMapping("api/admin/stock/supplier")
@Api(tags = "进销存-供应商管理")
public class StockSupplierController {

    @Autowired
    private SupplierService supplierService;

    @PreAuthorize("hasAuthority('admin:stock:supplier:list')")
    @ApiOperation(value = "供应商分页列表")
    @GetMapping("/list")
    public CommonResult<CommonPage<Supplier>> list(@Validated SupplierSearchRequest request,
                                                  @Validated PageParamRequest pageParamRequest) {
        return CommonResult.success(supplierService.getList(request, pageParamRequest));
    }

    @PreAuthorize("hasAuthority('admin:stock:supplier:allList')")
    @ApiOperation(value = "启用供应商列表（下拉用）")
    @GetMapping("/enabledList")
    public CommonResult<List<Supplier>> enabledList() {
        return CommonResult.success(supplierService.getEnabledList());
    }

    @PreAuthorize("hasAuthority('admin:stock:supplier:add')")
    @ApiOperation(value = "添加供应商")
    @PostMapping("/add")
    public CommonResult<String> add(@RequestBody @Validated SupplierRequest request) {
        if (supplierService.create(request)) {
            return CommonResult.success("添加成功");
        }
        return CommonResult.failed("添加失败");
    }

    @PreAuthorize("hasAuthority('admin:stock:supplier:update')")
    @ApiOperation(value = "编辑供应商")
    @PostMapping("/update")
    public CommonResult<String> update(@RequestBody @Validated SupplierRequest request) {
        if (supplierService.update(request)) {
            return CommonResult.success("修改成功");
        }
        return CommonResult.failed("修改失败");
    }

    @PreAuthorize("hasAuthority('admin:stock:supplier:delete')")
    @ApiOperation(value = "删除供应商")
    @PostMapping("/delete/{id}")
    public CommonResult<String> delete(@PathVariable Integer id) {
        if (supplierService.delete(id)) {
            return CommonResult.success("删除成功");
        }
        return CommonResult.failed("删除失败");
    }

    @PreAuthorize("hasAuthority('admin:stock:supplier:updateStatus')")
    @ApiOperation(value = "启用/禁用供应商")
    @PostMapping("/updateStatus")
    public CommonResult<String> updateStatus(@RequestParam Integer id, @RequestParam Boolean status) {
        if (supplierService.updateStatus(id, status)) {
            return CommonResult.success("修改成功");
        }
        return CommonResult.failed("修改失败");
    }
}

