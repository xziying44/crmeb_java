package com.zbkj.service.service.promotion;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.promotion.BuyGift;
import com.zbkj.common.request.BuyGiftRequest;
import com.zbkj.common.request.BuyGiftSearchRequest;
import com.zbkj.common.response.BuyGiftResponse;

public interface BuyGiftService extends IService<BuyGift> {

    PageInfo<BuyGiftResponse> getList(BuyGiftSearchRequest request);

    BuyGiftResponse getDetail(Integer id);

    Boolean save(BuyGiftRequest request);

    Boolean delete(Integer id);

    Boolean updateStatus(Integer id, Boolean status);

    BuyGift getAvailableByProductId(Integer productId, Integer uid);

    Boolean checkUserLimit(Integer giftId, Integer uid);
}

