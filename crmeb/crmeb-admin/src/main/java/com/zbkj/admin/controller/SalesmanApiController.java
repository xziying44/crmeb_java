package com.zbkj.admin.controller;

import com.zbkj.admin.service.AdminLoginService;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.SystemAdminLoginRequest;
import com.zbkj.common.response.SalesmanResponse;
import com.zbkj.common.response.SystemLoginResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.utils.CrmebUtil;
import com.zbkj.common.vo.CustomerBindRecordVo;
import com.zbkj.common.vo.SalesmanDashboardVo;
import com.zbkj.service.service.SalesmanService;
import com.zbkj.service.service.SystemAdminService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 业务员端专用接口
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
@RequestMapping("api/admin/salesman/app")
@Api(tags = "业务员端接口")
public class SalesmanApiController {

    @Autowired
    private AdminLoginService adminLoginService;

    @Autowired
    private SalesmanService salesmanService;

    @Autowired
    private SystemAdminService systemAdminService;

    @ApiOperation(value = "业务员登录")
    @PostMapping("/login")
    public CommonResult<SystemLoginResponse> login(@RequestBody @Validated SystemAdminLoginRequest request, HttpServletRequest httpServletRequest) {
        // 阶段一：先复用管理端登录逻辑，业务员账号本质为管理员账号（通过角色区分）
        String ip = CrmebUtil.getClientIp(httpServletRequest);
        return CommonResult.success(adminLoginService.login(request, ip));
    }

    @ApiOperation(value = "首页数据看板")
    @GetMapping("/dashboard")
    public CommonResult<SalesmanDashboardVo> dashboard(@RequestParam(defaultValue = "day") String dateType) {
        // 从token中获取当前登录的业务员ID
        Integer adminId = systemAdminService.getLoginAdminId();
        return CommonResult.success(salesmanService.getDashboard(adminId, dateType));
    }

    @ApiOperation(value = "获取我的邀请码和二维码")
    @GetMapping("/myCode")
    public CommonResult<SalesmanResponse> myCode() {
        Integer adminId = systemAdminService.getLoginAdminId();
        return CommonResult.success(salesmanService.getMyCode(adminId));
    }

    @ApiOperation(value = "我的客户列表")
    @GetMapping("/customer/list")
    public CommonResult<CommonPage<CustomerBindRecordVo>> customerList(@RequestParam(required = false) String keywords,
                                                                      @Validated PageParamRequest pageRequest) {
        Integer adminId = systemAdminService.getLoginAdminId();
        return CommonResult.success(salesmanService.getBindList(adminId, keywords, pageRequest));
    }

    @ApiOperation(value = "客户详情")
    @GetMapping("/customer/detail/{uid}")
    public CommonResult<CustomerBindRecordVo> customerDetail(@PathVariable Integer uid) {
        // TODO: 实现客户详情
        return CommonResult.success(new CustomerBindRecordVo());
    }

    @ApiOperation(value = "添加客户-发送验证码")
    @PostMapping("/customer/sendCode")
    public CommonResult<String> sendCode(@RequestParam String phone) {
        // TODO: 实现发送验证码
        return CommonResult.success("验证码已发送");
    }

    @ApiOperation(value = "添加客户-验证码确认绑定")
    @PostMapping("/customer/bindByCode")
    public CommonResult<String> bindByCode(@RequestParam String phone, @RequestParam String code) {
        // TODO: 实现验证码绑定
        return CommonResult.success("绑定成功");
    }

    @ApiOperation(value = "销售趋势数据")
    @GetMapping("/statistics/trend")
    public CommonResult<Object> trend(@RequestParam(defaultValue = "day") String dateType) {
        Integer adminId = systemAdminService.getLoginAdminId();
        SalesmanDashboardVo dashboard = salesmanService.getDashboard(adminId, dateType);
        return CommonResult.success(dashboard.getTrendData());
    }
}
