package com.zbkj.service.service.promotion;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.promotion.BuyGiftRecord;

public interface BuyGiftRecordService extends IService<BuyGiftRecord> {

    Integer countByGiftAndUser(Integer giftId, Integer uid);

    Integer countTodayByGiftAndUser(Integer giftId, Integer uid);

    Boolean addRecord(Integer giftId, Integer uid, String orderId);
}

