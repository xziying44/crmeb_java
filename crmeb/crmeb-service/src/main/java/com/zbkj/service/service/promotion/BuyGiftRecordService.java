package com.zbkj.service.service.promotion;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.promotion.BuyGiftRecord;

public interface BuyGiftRecordService extends IService<BuyGiftRecord> {

    Integer countByGiftAndUser(Integer giftId, Integer uid);

    Integer countTodayByGiftAndUser(Integer giftId, Integer uid);

    Boolean addRecord(Integer giftId, Integer uid, String orderId);

    /**
     * 删除某订单的买赠参与记录（订单取消/超时/退款时调用，避免占用用户参与次数）
     */
    Boolean deleteByOrderId(String orderId);
}

