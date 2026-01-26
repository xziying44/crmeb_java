package com.zbkj.admin.controller;

import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.StockInRequest;
import com.zbkj.common.request.StockOutRequest;
import com.zbkj.common.response.StockLogResponse;
import com.zbkj.common.response.StockResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.stock.StockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 库存管理控制器
 */
@RestController
@RequestMapping("api/admin/stock")
@Api(tags = "进销存-库存管理")
public class StockController {

    @Autowired
    private StockService stockService;

    @PreAuthorize("hasAuthority('admin:stock:list')")
    @ApiOperation(value = "库存列表")
    @GetMapping("/list")
    public CommonResult<CommonPage<StockResponse>> list(@RequestParam(required = false) String keywords,
                                                       @RequestParam(required = false) Boolean warning,
                                                       @Validated PageParamRequest pageParamRequest) {
        return CommonResult.success(stockService.getStockList(keywords, warning, pageParamRequest));
    }

    @PreAuthorize("hasAuthority('admin:stock:log:list')")
    @ApiOperation(value = "库存流水列表")
    @GetMapping("/log/list")
    public CommonResult<CommonPage<StockLogResponse>> logList(@RequestParam(required = false) Integer productId,
                                                             @RequestParam(required = false) Integer type,
                                                             @Validated PageParamRequest pageParamRequest) {
        return CommonResult.success(stockService.getLogList(productId, type, pageParamRequest));
    }

    @PreAuthorize("hasAuthority('admin:stock:in')")
    @ApiOperation(value = "手动入库")
    @PostMapping("/in")
    public CommonResult<String> in(@RequestBody @Validated StockInRequest request) {
        if (stockService.manualIn(request)) {
            return CommonResult.success("入库成功");
        }
        return CommonResult.failed("入库失败");
    }

    @PreAuthorize("hasAuthority('admin:stock:out')")
    @ApiOperation(value = "手动出库")
    @PostMapping("/out")
    public CommonResult<String> out(@RequestBody @Validated StockOutRequest request) {
        if (stockService.manualOut(request)) {
            return CommonResult.success("出库成功");
        }
        return CommonResult.failed("出库失败");
    }

    @PreAuthorize("hasAuthority('admin:stock:warning:update')")
    @ApiOperation(value = "设置预警库存")
    @PostMapping("/warning/update")
    public CommonResult<String> updateWarning(@RequestParam Integer productId,
                                              @RequestParam(required = false, defaultValue = "0") Integer attrValueId,
                                              @RequestParam Integer warningStock) {
        if (stockService.updateWarningStock(productId, attrValueId, warningStock)) {
            return CommonResult.success("设置成功");
        }
        return CommonResult.failed("设置失败");
    }

    @PreAuthorize("hasAuthority('admin:stock:init')")
    @ApiOperation(value = "初始化库存（从商品库存同步到进销存库存）")
    @PostMapping("/init")
    public CommonResult<String> initStock() {
        if (stockService.initStockFromProduct()) {
            return CommonResult.success("初始化成功");
        }
        return CommonResult.failed("初始化失败");
    }
}

