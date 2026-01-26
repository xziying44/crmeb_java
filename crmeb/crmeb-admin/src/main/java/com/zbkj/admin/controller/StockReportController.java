package com.zbkj.admin.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.stock.StockReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 库存报表控制器
 */
@RestController
@RequestMapping("api/admin/stock/report")
@Api(tags = "进销存-库存报表")
public class StockReportController {

    @Autowired
    private StockReportService stockReportService;

    @PreAuthorize("hasAuthority('admin:stock:report:summary')")
    @ApiOperation(value = "库存变动汇总")
    @GetMapping("/summary")
    public CommonResult<List<Map<String, Object>>> summary(@RequestParam(required = false) String startTime,
                                                          @RequestParam(required = false) String endTime) {
        return CommonResult.success(stockReportService.summary(parseDate(startTime), parseDate(endTime)));
    }

    @PreAuthorize("hasAuthority('admin:stock:report:purchase')")
    @ApiOperation(value = "采购统计")
    @GetMapping("/purchase")
    public CommonResult<List<Map<String, Object>>> purchase(@RequestParam(required = false) String startTime,
                                                           @RequestParam(required = false) String endTime) {
        return CommonResult.success(stockReportService.purchase(parseDate(startTime), parseDate(endTime)));
    }

    @PreAuthorize("hasAuthority('admin:stock:report:checkDiff')")
    @ApiOperation(value = "盘点差异报表")
    @GetMapping("/checkDiff")
    public CommonResult<List<Map<String, Object>>> checkDiff(@RequestParam(required = false) String startTime,
                                                            @RequestParam(required = false) String endTime) {
        return CommonResult.success(stockReportService.checkDiff(parseDate(startTime), parseDate(endTime)));
    }

    private Date parseDate(String time) {
        if (StrUtil.isBlank(time)) {
            return null;
        }
        // 兼容 yyyy-MM-dd 与 yyyy-MM-dd HH:mm:ss
        return DateUtil.parse(time);
    }
}

