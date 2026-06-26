package com.zbkj.service.service.promotion.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.model.promotion.BuyGiftRecord;
import com.zbkj.service.dao.promotion.BuyGiftRecordDao;
import com.zbkj.service.service.promotion.BuyGiftRecordService;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 买赠参与记录 Service 实现类
 */
@Service
public class BuyGiftRecordServiceImpl extends ServiceImpl<BuyGiftRecordDao, BuyGiftRecord>
        implements BuyGiftRecordService {

    @Override
    public Integer countByGiftAndUser(Integer giftId, Integer uid) {
        LambdaQueryWrapper<BuyGiftRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(BuyGiftRecord::getGiftId, giftId);
        wrapper.eq(BuyGiftRecord::getUid, uid);
        return count(wrapper);
    }

    @Override
    public Integer countTodayByGiftAndUser(Integer giftId, Integer uid) {
        LambdaQueryWrapper<BuyGiftRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(BuyGiftRecord::getGiftId, giftId);
        wrapper.eq(BuyGiftRecord::getUid, uid);
        Date todayStart = DateUtil.beginOfDay(new Date());
        Date todayEnd = DateUtil.endOfDay(new Date());
        wrapper.between(BuyGiftRecord::getCreateTime, todayStart, todayEnd);
        return count(wrapper);
    }

    @Override
    public Boolean addRecord(Integer giftId, Integer uid, String orderId) {
        BuyGiftRecord record = new BuyGiftRecord();
        record.setGiftId(giftId);
        record.setUid(uid);
        record.setOrderId(orderId);
        record.setCreateTime(new Date());
        return save(record);
    }

    @Override
    public Boolean deleteByOrderId(String orderId) {
        if (orderId == null || orderId.length() == 0) {
            return false;
        }
        LambdaQueryWrapper<BuyGiftRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(BuyGiftRecord::getOrderId, orderId);
        return remove(wrapper);
    }
}

