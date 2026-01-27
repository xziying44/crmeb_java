package com.zbkj.service.service.promotion.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.model.promotion.FullReductionLevel;
import com.zbkj.service.dao.promotion.FullReductionLevelDao;
import com.zbkj.service.service.promotion.FullReductionLevelService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 满减阶梯 Service 实现类
 */
@Service
public class FullReductionLevelServiceImpl extends ServiceImpl<FullReductionLevelDao, FullReductionLevel>
        implements FullReductionLevelService {

    @Override
    public List<FullReductionLevel> getByReductionId(Integer reductionId) {
        LambdaQueryWrapper<FullReductionLevel> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FullReductionLevel::getReductionId, reductionId);
        wrapper.orderByDesc(FullReductionLevel::getFullAmount);
        return list(wrapper);
    }

    @Override
    public Boolean deleteByReductionId(Integer reductionId) {
        LambdaQueryWrapper<FullReductionLevel> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FullReductionLevel::getReductionId, reductionId);
        return remove(wrapper);
    }
}

