package com.zbkj.front.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.zbkj.common.model.salesman.SalesmanInfo;
import com.zbkj.common.model.user.User;
import com.zbkj.common.response.SalesmanResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.SalesmanInfoService;
import com.zbkj.service.service.SalesmanService;
import com.zbkj.service.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 业务员绑定控制器 - 商城端
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
@RequestMapping("api/front/salesman")
@Api(tags = "业务员绑定")
public class SalesmanBindController {

    @Autowired
    private SalesmanService salesmanService;

    @Autowired
    private SalesmanInfoService salesmanInfoService;

    @Autowired
    private UserService userService;

    @ApiOperation(value = "检查邀请码是否有效")
    @GetMapping("/checkCode")
    public CommonResult<Boolean> checkCode(@RequestParam String code) {
        return CommonResult.success(salesmanService.checkCode(code));
    }

    @ApiOperation(value = "绑定业务员")
    @PostMapping("/bind")
    public CommonResult<String> bind(@RequestParam String code) {
        if (StrUtil.isBlank(code)) {
            return CommonResult.failed("邀请码不能为空");
        }
        Integer uid = userService.getUserIdException();
        User user = userService.getById(uid);
        if (ObjectUtil.isNull(user)) {
            return CommonResult.failed("用户不存在");
        }
        if (ObjectUtil.isNotNull(user.getSalesmanId()) && user.getSalesmanId() > 0) {
            return CommonResult.failed("已绑定业务员，无法重复绑定");
        }

        SalesmanInfo info = salesmanInfoService.getByCode(code);
        if (ObjectUtil.isNull(info) || !Boolean.TRUE.equals(info.getBindable())) {
            return CommonResult.failed("邀请码无效");
        }

        user.setSalesmanId(info.getAdminId());
        user.setSalesmanBindTime(new Date());
        if (userService.updateById(user)) {
            return CommonResult.success("绑定成功");
        }
        return CommonResult.failed("绑定失败");
    }

    @ApiOperation(value = "获取当前绑定的业务员信息")
    @GetMapping("/myBind")
    public CommonResult<SalesmanResponse> myBind() {
        Integer uid = userService.getUserIdException();
        User user = userService.getById(uid);
        if (ObjectUtil.isNull(user) || ObjectUtil.isNull(user.getSalesmanId()) || user.getSalesmanId() <= 0) {
            return CommonResult.success((SalesmanResponse) null);
        }
        return CommonResult.success(salesmanService.getDetail(user.getSalesmanId()));
    }
}
