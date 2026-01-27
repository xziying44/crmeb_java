package com.zbkj.service.service.promotion.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.model.promotion.BuyGiftProduct;
import com.zbkj.service.dao.promotion.BuyGiftProductDao;
import com.zbkj.service.service.promotion.BuyGiftProductService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 买赠商品关联 Service 实现类
 */
@Service
public class BuyGiftProductServiceImpl extends ServiceImpl<BuyGiftProductDao, BuyGiftProduct>
        implements BuyGiftProductService {

    @Override
    public List<BuyGiftProduct> getByGiftId(Integer giftId) {
        LambdaQueryWrapper<BuyGiftProduct> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(BuyGiftProduct::getGiftId, giftId);
        return list(wrapper);
    }

    @Override
    public Boolean deleteByGiftId(Integer giftId) {
        LambdaQueryWrapper<BuyGiftProduct> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(BuyGiftProduct::getGiftId, giftId);
        return remove(wrapper);
    }

    @Override
    public List<Integer> getGiftIdsByProductId(Integer productId) {
        LambdaQueryWrapper<BuyGiftProduct> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(BuyGiftProduct::getProductId, productId);
        wrapper.eq(BuyGiftProduct::getProductType, 1); // 购买商品
        return list(wrapper).stream()
                .map(BuyGiftProduct::getGiftId)
                .distinct()
                .collect(Collectors.toList());
    }
}

