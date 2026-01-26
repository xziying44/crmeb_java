package com.zbkj.admin.controller;

import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.StockCheckCreateRequest;
import com.zbkj.common.request.StockCheckUpdateItemRequest;
import com.zbkj.common.response.StockCheckResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.stock.StockCheckService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 库存盘点控制器
 */
@RestController
@RequestMapping("api/admin/stock/check")
@Api(tags = "进销存-库存盘点")
public class StockCheckController {

    @Autowired
    private StockCheckService stockCheckService;

    @PreAuthorize("hasAuthority('admin:stock:check:list')")
    @ApiOperation(value = "盘点单列表")
    @GetMapping("/list")
    public CommonResult<CommonPage<StockCheckResponse>> list(@RequestParam(required = false) String checkNo,
                                                            @RequestParam(required = false) Integer status,
                                                            @Validated PageParamRequest pageParamRequest) {
        return CommonResult.success(stockCheckService.getList(checkNo, status, pageParamRequest));
    }

    @PreAuthorize("hasAuthority('admin:stock:check:detail')")
    @ApiOperation(value = "盘点单详情")
    @GetMapping("/detail/{id}")
    public CommonResult<StockCheckResponse> detail(@PathVariable Integer id) {
        return CommonResult.success(stockCheckService.getDetail(id));
    }

    @PreAuthorize("hasAuthority('admin:stock:check:create')")
    @ApiOperation(value = "创建盘点单")
    @PostMapping("/create")
    public CommonResult<String> create(@RequestBody StockCheckCreateRequest request) {
        if (stockCheckService.create(request)) {
            return CommonResult.success("创建成功");
        }
        return CommonResult.failed("创建失败");
    }

    @PreAuthorize("hasAuthority('admin:stock:check:updateItem')")
    @ApiOperation(value = "更新盘点数量")
    @PostMapping("/updateItem")
    public CommonResult<String> updateItem(@RequestBody @Validated StockCheckUpdateItemRequest request) {
        if (stockCheckService.updateItem(request)) {
            return CommonResult.success("更新成功");
        }
        return CommonResult.failed("更新失败");
    }

    @PreAuthorize("hasAuthority('admin:stock:check:confirm')")
    @ApiOperation(value = "确认盘点完成")
    @PostMapping("/confirm/{id}")
    public CommonResult<String> confirm(@PathVariable Integer id) {
        if (stockCheckService.confirm(id)) {
            return CommonResult.success("确认成功");
        }
        return CommonResult.failed("确认失败");
    }

    @PreAuthorize("hasAuthority('admin:stock:check:cancel')")
    @ApiOperation(value = "取消盘点单")
    @PostMapping("/cancel/{id}")
    public CommonResult<String> cancel(@PathVariable Integer id) {
        if (stockCheckService.cancel(id)) {
            return CommonResult.success("取消成功");
        }
        return CommonResult.failed("取消失败");
    }
}

