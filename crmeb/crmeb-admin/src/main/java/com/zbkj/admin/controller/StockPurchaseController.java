package com.zbkj.admin.controller;

import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.PurchaseAddRequest;
import com.zbkj.common.request.PurchaseInStockRequest;
import com.zbkj.common.response.PurchaseResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.stock.PurchaseService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 采购管理控制器
 */
@RestController
@RequestMapping("api/admin/stock/purchase")
@Api(tags = "进销存-采购管理")
public class StockPurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    @PreAuthorize("hasAuthority('admin:stock:purchase:list')")
    @ApiOperation(value = "采购单列表")
    @GetMapping("/list")
    public CommonResult<CommonPage<PurchaseResponse>> list(@RequestParam(required = false) String purchaseNo,
                                                          @RequestParam(required = false) Integer supplierId,
                                                          @RequestParam(required = false) Integer status,
                                                          @Validated PageParamRequest pageParamRequest) {
        return CommonResult.success(purchaseService.getList(purchaseNo, supplierId, status, pageParamRequest));
    }

    @PreAuthorize("hasAuthority('admin:stock:purchase:detail')")
    @ApiOperation(value = "采购单详情")
    @GetMapping("/detail/{id}")
    public CommonResult<PurchaseResponse> detail(@PathVariable Integer id) {
        return CommonResult.success(purchaseService.getDetail(id));
    }

    @PreAuthorize("hasAuthority('admin:stock:purchase:add')")
    @ApiOperation(value = "创建采购单")
    @PostMapping("/add")
    public CommonResult<String> add(@RequestBody @Validated PurchaseAddRequest request) {
        if (purchaseService.create(request)) {
            return CommonResult.success("创建成功");
        }
        return CommonResult.failed("创建失败");
    }

    @PreAuthorize("hasAuthority('admin:stock:purchase:cancel')")
    @ApiOperation(value = "取消采购单")
    @PostMapping("/cancel/{id}")
    public CommonResult<String> cancel(@PathVariable Integer id) {
        if (purchaseService.cancel(id)) {
            return CommonResult.success("取消成功");
        }
        return CommonResult.failed("取消失败");
    }

    @PreAuthorize("hasAuthority('admin:stock:purchase:inStock')")
    @ApiOperation(value = "采购入库")
    @PostMapping("/inStock")
    public CommonResult<String> inStock(@RequestBody @Validated PurchaseInStockRequest request) {
        if (purchaseService.inStock(request)) {
            return CommonResult.success("入库成功");
        }
        return CommonResult.failed("入库失败");
    }
}

