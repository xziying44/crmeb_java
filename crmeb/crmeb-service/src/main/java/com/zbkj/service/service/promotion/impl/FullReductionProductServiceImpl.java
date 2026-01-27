package com.zbkj.service.service.promotion.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.model.promotion.FullReductionProduct;
import com.zbkj.service.dao.promotion.FullReductionProductDao;
import com.zbkj.service.service.promotion.FullReductionProductService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 满减关联 Service 实现类
 */
@Service
public class FullReductionProductServiceImpl extends ServiceImpl<FullReductionProductDao, FullReductionProduct>
        implements FullReductionProductService {

    @Override
    public List<FullReductionProduct> getByReductionId(Integer reductionId) {
        LambdaQueryWrapper<FullReductionProduct> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FullReductionProduct::getReductionId, reductionId);
        return list(wrapper);
    }

    @Override
    public Boolean deleteByReductionId(Integer reductionId) {
        LambdaQueryWrapper<FullReductionProduct> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FullReductionProduct::getReductionId, reductionId);
        return remove(wrapper);
    }

    @Override
    public List<Integer> getReductionIdsByProductId(Integer productId) {
        LambdaQueryWrapper<FullReductionProduct> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FullReductionProduct::getRelationType, 2); // 商品
        wrapper.eq(FullReductionProduct::getRelationId, productId);
        return list(wrapper).stream()
                .map(FullReductionProduct::getReductionId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<Integer> getReductionIdsByCategoryId(Integer categoryId) {
        LambdaQueryWrapper<FullReductionProduct> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FullReductionProduct::getRelationType, 1); // 品类
        wrapper.eq(FullReductionProduct::getRelationId, categoryId);
        return list(wrapper).stream()
                .map(FullReductionProduct::getReductionId)
                .distinct()
                .collect(Collectors.toList());
    }
}

