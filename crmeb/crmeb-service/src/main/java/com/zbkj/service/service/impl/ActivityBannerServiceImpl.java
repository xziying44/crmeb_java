package com.zbkj.service.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.URLUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.activity.ActivityBanner;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.ActivityBannerSearchRequest;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.service.dao.ActivityBannerDao;
import com.zbkj.service.service.ActivityBannerService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 活动横幅 Service 实现
 */
@Service
public class ActivityBannerServiceImpl extends ServiceImpl<ActivityBannerDao, ActivityBanner> implements ActivityBannerService {

    @Resource
    private ActivityBannerDao dao;

    @Override
    public PageInfo<ActivityBanner> getList(ActivityBannerSearchRequest request, PageParamRequest pageParamRequest) {
        Page<ActivityBanner> page = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<ActivityBanner> wrapper = Wrappers.lambdaQuery();
        if (ObjectUtil.isNotEmpty(request.getStatus())) {
            wrapper.eq(ActivityBanner::getStatus, request.getStatus());
        }
        if (ObjectUtil.isNotEmpty(request.getName())) {
            wrapper.like(ActivityBanner::getName, URLUtil.decode(request.getName()));
        }
        wrapper.orderByAsc(ActivityBanner::getSort);
        wrapper.orderByDesc(ActivityBanner::getCreateTime);
        List<ActivityBanner> list = dao.selectList(wrapper);
        return CommonPage.copyPageInfo(page, list);
    }

    @Override
    public boolean updateStatus(Integer id, Integer status) {
        ActivityBanner banner = new ActivityBanner();
        banner.setId(id);
        banner.setStatus(status);
        banner.setUpdateTime(DateUtil.date());
        return updateById(banner);
    }

    @Override
    public List<ActivityBanner> getOnlineList() {
        LambdaQueryWrapper<ActivityBanner> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ActivityBanner::getStatus, 1);
        wrapper.orderByAsc(ActivityBanner::getSort);
        wrapper.orderByDesc(ActivityBanner::getCreateTime);
        return dao.selectList(wrapper);
    }
}
