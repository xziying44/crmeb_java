package com.zbkj.service.service.promotion;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.promotion.FullReductionLevel;

import java.util.List;

/**
 * 满减阶梯 Service 接口
 */
public interface FullReductionLevelService extends IService<FullReductionLevel> {

    /**
     * 根据满减活动ID获取阶梯列表
     */
    List<FullReductionLevel> getByReductionId(Integer reductionId);

    /**
     * 删除满减活动的阶梯
     */
    Boolean deleteByReductionId(Integer reductionId);
}

