package com.zbkj.front.controller;

import com.zbkj.common.model.activity.ActivityBanner;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.ActivityBannerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 活动横幅 移动端控制器
 */
@Slf4j
@RestController
@RequestMapping("api/front/activity/banner")
@Api(tags = "活动横幅")
public class ActivityBannerController {

    @Autowired
    private ActivityBannerService activityBannerService;

    /**
     * 获取所有上线横幅
     */
    @ApiOperation(value = "获取上线横幅列表")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public CommonResult<List<ActivityBanner>> getOnlineList() {
        return CommonResult.success(activityBannerService.getOnlineList());
    }
}
