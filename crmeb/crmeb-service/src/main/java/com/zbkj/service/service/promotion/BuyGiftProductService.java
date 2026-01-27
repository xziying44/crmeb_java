package com.zbkj.service.service.promotion;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.promotion.BuyGiftProduct;

import java.util.List;

public interface BuyGiftProductService extends IService<BuyGiftProduct> {

    List<BuyGiftProduct> getByGiftId(Integer giftId);

    Boolean deleteByGiftId(Integer giftId);

    List<Integer> getGiftIdsByProductId(Integer productId);
}

