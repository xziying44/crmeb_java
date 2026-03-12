package com.zbkj.admin.controller;

import cn.hutool.core.date.DateUtil;
import com.zbkj.common.model.activity.ActivityBanner;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.ActivityBannerRequest;
import com.zbkj.common.request.ActivityBannerSearchRequest;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.ActivityBannerService;
import com.zbkj.service.service.SystemAttachmentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 活动横幅管理 控制器
 */
@Slf4j
@RestController
@RequestMapping("api/admin/activity/banner")
@Api(tags = "活动横幅")
public class ActivityBannerController {

    @Autowired
    private ActivityBannerService activityBannerService;

    @Autowired
    private SystemAttachmentService systemAttachmentService;

    /**
     * 分页列表
     */
    @PreAuthorize("hasAuthority('admin:activity:banner:list')")
    @ApiOperation(value = "分页列表")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public CommonResult<CommonPage<ActivityBanner>> getList(@Validated ActivityBannerSearchRequest request,
                                                            @ModelAttribute PageParamRequest pageParamRequest) {
        CommonPage<ActivityBanner> page = CommonPage.restPage(activityBannerService.getList(request, pageParamRequest));
        return CommonResult.success(page);
    }

    /**
     * 新增
     */
    @PreAuthorize("hasAuthority('admin:activity:banner:save')")
    @ApiOperation(value = "新增")
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public CommonResult<String> save(@RequestBody @Validated ActivityBannerRequest request) {
        ActivityBanner banner = new ActivityBanner();
        BeanUtils.copyProperties(request, banner);
        banner.setImage(systemAttachmentService.clearPrefix(banner.getImage()));
        banner.setStatus(0);
        if (banner.getSort() == null) {
            banner.setSort(0);
        }
        if (activityBannerService.save(banner)) {
            return CommonResult.success();
        }
        return CommonResult.failed();
    }

    /**
     * 修改
     */
    @PreAuthorize("hasAuthority('admin:activity:banner:update')")
    @ApiOperation(value = "修改")
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public CommonResult<String> update(@RequestBody @Validated ActivityBannerRequest request) {
        ActivityBanner banner = new ActivityBanner();
        BeanUtils.copyProperties(request, banner);
        banner.setId(request.getId());
        banner.setImage(systemAttachmentService.clearPrefix(banner.getImage()));
        banner.setUpdateTime(DateUtil.date());
        if (activityBannerService.updateById(banner)) {
            return CommonResult.success();
        }
        return CommonResult.failed();
    }

    /**
     * 删除
     */
    @PreAuthorize("hasAuthority('admin:activity:banner:delete')")
    @ApiOperation(value = "删除")
    @RequestMapping(value = "/delete/{id}", method = RequestMethod.GET)
    public CommonResult<String> delete(@PathVariable Integer id) {
        if (activityBannerService.removeById(id)) {
            return CommonResult.success();
        }
        return CommonResult.failed();
    }

    /**
     * 更新状态（上线/下线）
     */
    @PreAuthorize("hasAuthority('admin:activity:banner:status')")
    @ApiOperation(value = "更新状态")
    @RequestMapping(value = "/status", method = RequestMethod.POST)
    public CommonResult<String> updateStatus(@RequestParam Integer id, @RequestParam Integer status) {
        if (activityBannerService.updateStatus(id, status)) {
            return CommonResult.success();
        }
        return CommonResult.failed();
    }
}
