package com.zbkj.admin.controller;

import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.CustomerTransferRequest;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.SalesmanAddRequest;
import com.zbkj.common.request.SalesmanSearchRequest;
import com.zbkj.common.request.SalesmanUpdateRequest;
import com.zbkj.common.response.SalesmanResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.vo.CustomerBindRecordVo;
import com.zbkj.common.model.salesman.SalesmanInfo;
import com.zbkj.service.service.SalesmanInfoService;
import com.zbkj.service.service.SalesmanService;
import com.zbkj.service.service.WechatQrcodeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 业务员管理控制器 - Admin后台
 * +----------------------------------------------------------------------
 * | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 * +----------------------------------------------------------------------
 * | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
 * +----------------------------------------------------------------------
 * | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 * +----------------------------------------------------------------------
 * | Author: CRMEB Team <admin@crmeb.com>
 * +----------------------------------------------------------------------
 */
@Slf4j
@RestController
@RequestMapping("api/admin/salesman")
@Api(tags = "业务员管理")
public class SalesmanController {

    @Autowired
    private SalesmanService salesmanService;

    @Autowired
    private WechatQrcodeService wechatQrcodeService;

    @Autowired
    private SalesmanInfoService salesmanInfoService;

    @PreAuthorize("hasAuthority('admin:salesman:list')")
    @ApiOperation(value = "业务员分页列表")
    @GetMapping("/list")
    public CommonResult<CommonPage<SalesmanResponse>> list(@Validated SalesmanSearchRequest request,
                                                          @Validated PageParamRequest pageRequest) {
        return CommonResult.success(salesmanService.getList(request, pageRequest));
    }

    @PreAuthorize("hasAuthority('admin:salesman:info')")
    @ApiOperation(value = "业务员详情")
    @GetMapping("/info/{id}")
    public CommonResult<SalesmanResponse> info(@PathVariable Integer id) {
        return CommonResult.success(salesmanService.getDetail(id));
    }

    @PreAuthorize("hasAuthority('admin:salesman:save')")
    @ApiOperation(value = "创建业务员")
    @PostMapping("/save")
    public CommonResult<String> save(@RequestBody @Validated SalesmanAddRequest request) {
        if (salesmanService.create(request)) {
            return CommonResult.success("创建成功");
        }
        return CommonResult.failed("创建失败");
    }

    @PreAuthorize("hasAuthority('admin:salesman:update')")
    @ApiOperation(value = "更新业务员")
    @PostMapping("/update")
    public CommonResult<String> update(@RequestBody @Validated SalesmanUpdateRequest request) {
        if (salesmanService.update(request)) {
            return CommonResult.success("更新成功");
        }
        return CommonResult.failed("更新失败");
    }

    @PreAuthorize("hasAuthority('admin:salesman:delete')")
    @ApiOperation(value = "删除业务员")
    @PostMapping("/delete/{id}")
    public CommonResult<String> delete(@PathVariable Integer id) {
        if (salesmanService.delete(id)) {
            return CommonResult.success("删除成功");
        }
        return CommonResult.failed("删除失败");
    }

    @PreAuthorize("hasAuthority('admin:salesman:update')")
    @ApiOperation(value = "更新业务员状态")
    @PostMapping("/updateStatus/{id}")
    public CommonResult<String> updateStatus(@PathVariable Integer id, @RequestParam Boolean status) {
        if (salesmanService.updateStatus(id, status)) {
            return CommonResult.success("修改成功");
        }
        return CommonResult.failed("修改失败");
    }

    @PreAuthorize("hasAuthority('admin:salesman:update')")
    @ApiOperation(value = "重新生成邀请码")
    @PostMapping("/regenerateCode/{id}")
    public CommonResult<String> regenerateCode(@PathVariable Integer id) {
        String newCode = salesmanService.regenerateCode(id);
        return CommonResult.success(newCode);
    }

    @PreAuthorize("hasAuthority('admin:salesman:bindList')")
    @ApiOperation(value = "客户绑定记录列表")
    @GetMapping("/bindList")
    public CommonResult<CommonPage<CustomerBindRecordVo>> bindList(@RequestParam(required = false) Integer salesmanId,
                                                                   @RequestParam(required = false) String keywords,
                                                                   @Validated PageParamRequest pageRequest) {
        return CommonResult.success(salesmanService.getBindList(salesmanId, keywords, pageRequest));
    }

    @PreAuthorize("hasAuthority('admin:salesman:transfer')")
    @ApiOperation(value = "转移客户")
    @PostMapping("/transferCustomer")
    public CommonResult<String> transferCustomer(@RequestBody @Validated CustomerTransferRequest request) {
        if (salesmanService.transferCustomer(request)) {
            return CommonResult.success("转移成功");
        }
        return CommonResult.failed("转移失败");
    }

    @PreAuthorize("hasAuthority('admin:salesman:statistics')")
    @ApiOperation(value = "业绩统计汇总")
    @GetMapping("/statistics")
    public CommonResult<Object> statistics() {
        return CommonResult.success(salesmanService.getStatistics());
    }

    @PreAuthorize("hasAuthority('admin:salesman:statistics')")
    @ApiOperation(value = "业务员业绩排行榜")
    @GetMapping("/ranking")
    public CommonResult<List<SalesmanResponse>> ranking(@RequestParam(defaultValue = "10") Integer limit) {
        return CommonResult.success(salesmanService.getRanking(limit));
    }

    @ApiOperation(value = "获取所有业务员列表（下拉选择用）")
    @GetMapping("/allList")
    public CommonResult<List<SalesmanResponse>> allList() {
        return CommonResult.success(salesmanService.getAllList());
    }

    @PreAuthorize("hasAuthority('admin:salesman:update')")
    @ApiOperation(value = "生成/刷新小程序码")
    @PostMapping("/generateQrcode/{id}")
    public CommonResult<String> generateQrcode(@PathVariable Integer id) {
        SalesmanInfo info = salesmanInfoService.getByAdminId(id);
        if (info == null) {
            return CommonResult.failed("业务员信息不存在");
        }

        String qrcodeUrl = wechatQrcodeService.generateSalesmanQrcode(info.getSalesmanCode());

        // 更新数据库
        info.setSalesmanQrcode(qrcodeUrl);
        info.setQrcodeScene("s_" + info.getSalesmanCode());
        salesmanInfoService.updateById(info);

        return CommonResult.success(qrcodeUrl);
    }
}
