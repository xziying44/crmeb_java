package com.zbkj.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.activity.ActivityBanner;
import com.zbkj.common.request.ActivityBannerSearchRequest;
import com.zbkj.common.request.PageParamRequest;

import java.util.List;

/**
 * 活动横幅 Service 接口
 */
public interface ActivityBannerService extends IService<ActivityBanner> {

    /**
     * 分页查询活动横幅
     */
    PageInfo<ActivityBanner> getList(ActivityBannerSearchRequest request, PageParamRequest pageParamRequest);

    /**
     * 更新状态（上线/下线）
     */
    boolean updateStatus(Integer id, Integer status);

    /**
     * 获取所有上线横幅（按 sort 升序），用于移动端展示
     */
    List<ActivityBanner> getOnlineList();
}
