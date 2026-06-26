package com.zbkj.service.service.promotion.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.coupon.StoreCoupon;
import com.zbkj.common.model.coupon.StoreCouponUser;
import com.zbkj.common.model.promotion.BuyGift;
import com.zbkj.common.model.promotion.BuyGiftProduct;
import com.zbkj.common.model.promotion.FullReduction;
import com.zbkj.common.utils.CrmebDateUtil;
import com.zbkj.common.vo.MyRecord;
import com.zbkj.service.service.StoreCouponUserService;
import com.zbkj.service.service.promotion.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 促销计算服务实现
 */
@Service
public class PromotionCalculateServiceImpl implements PromotionCalculateService {

    @Autowired
    private FullReductionService fullReductionService;

    @Autowired
    private BuyGiftService buyGiftService;

    @Autowired
    private BuyGiftProductService buyGiftProductService;

    @Autowired
    private StoreCouponUserService storeCouponUserService;

    @Autowired
    private com.zbkj.service.service.StoreCouponService storeCouponService;

    @Override
    public MyRecord calculateFullReduction(List<Integer> productIds, List<Integer> categoryIds, BigDecimal totalAmount) {
        MyRecord result = new MyRecord();
        result.set("reduction", null);
        result.set("reduceAmount", BigDecimal.ZERO);

        FullReduction reduction = fullReductionService.getAvailableByProductIds(productIds, categoryIds);
        if (ObjectUtil.isNull(reduction)) {
            return result;
        }

        BigDecimal reduceAmount = fullReductionService.calculateReduction(reduction, totalAmount);
        result.set("reduction", reduction);
        result.set("reduceAmount", reduceAmount);
        return result;
    }

    @Override
    public BigDecimal calculateVoucherDeduction(Integer voucherId, BigDecimal productPayable, BigDecimal freightFee) {
        if (voucherId == null || voucherId <= 0) {
            return BigDecimal.ZERO;
        }
        if (productPayable == null) {
            productPayable = BigDecimal.ZERO;
        }
        if (freightFee == null) {
            freightFee = BigDecimal.ZERO;
        }

        StoreCouponUser couponUser = storeCouponUserService.getById(voucherId);
        if (ObjectUtil.isNull(couponUser)) {
            throw new CrmebException("代金券领取记录不存在");
        }
        if (couponUser.getStatus() != null && couponUser.getStatus() != 0) {
            throw new CrmebException("代金券不可用");
        }
        Date now = CrmebDateUtil.nowDateTime();
        if (couponUser.getStartTime() != null && couponUser.getStartTime().compareTo(now) > 0) {
            throw new CrmebException("代金券还未到达使用时间范围之内");
        }
        if (couponUser.getEndTime() != null && now.compareTo(couponUser.getEndTime()) > 0) {
            throw new CrmebException("代金券已经失效");
        }

        StoreCoupon coupon = storeCouponService.getById(couponUser.getCouponId());
        if (ObjectUtil.isNull(coupon) || Boolean.TRUE.equals(coupon.getIsDel())) {
            throw new CrmebException("代金券信息不存在");
        }
        if (coupon.getCouponType() == null || coupon.getCouponType() != 2) {
            throw new CrmebException("所选券不是代金券");
        }
        // 校验最低使用门槛（与优惠券一致），防止低于门槛使用代金券
        BigDecimal minPrice = couponUser.getMinPrice();
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) > 0 && productPayable.compareTo(minPrice) < 0) {
            throw new CrmebException("订单金额未达到代金券使用门槛");
        }

        BigDecimal maxDeduct = productPayable;
        if (Boolean.TRUE.equals(coupon.getCanDeductFreight())) {
            maxDeduct = maxDeduct.add(freightFee);
        }
        if (maxDeduct.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal money = couponUser.getMoney() == null ? BigDecimal.ZERO : couponUser.getMoney();
        BigDecimal deduct = money.min(maxDeduct);
        if (deduct.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return deduct;
    }

    @Override
    public Boolean isAllowCoupon(Integer fullReductionId) {
        if (fullReductionId == null || fullReductionId <= 0) {
            return Boolean.TRUE;
        }
        FullReduction reduction = fullReductionService.getById(fullReductionId);
        if (ObjectUtil.isNull(reduction) || Boolean.TRUE.equals(reduction.getIsDel())) {
            return Boolean.TRUE;
        }
        return ObjectUtil.defaultIfNull(reduction.getAllowCoupon(), Boolean.TRUE);
    }

    @Override
    public BuyGift getProductBuyGift(Integer productId, Integer uid) {
        return buyGiftService.getAvailableByProductId(productId, uid);
    }

    @Override
    public List<MyRecord> calculateGiftProducts(Integer giftId, Integer productId, Integer buyQuantity) {
        if (giftId == null || giftId <= 0 || productId == null || productId <= 0 || buyQuantity == null || buyQuantity <= 0) {
            return new ArrayList<>();
        }
        List<BuyGiftProduct> products = buyGiftProductService.getByGiftId(giftId);
        if (CollUtil.isEmpty(products)) {
            return new ArrayList<>();
        }

        List<BuyGiftProduct> buyList = products.stream()
                .filter(p -> p.getProductType() != null && p.getProductType() == 1)
                .collect(Collectors.toList());
        List<BuyGiftProduct> giftList = products.stream()
                .filter(p -> p.getProductType() != null && p.getProductType() == 2)
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(buyList) || CollUtil.isEmpty(giftList)) {
            return new ArrayList<>();
        }

        BuyGiftProduct buyRule = buyList.stream()
                .filter(p -> p.getProductId() != null && p.getProductId().equals(productId))
                .findFirst()
                .orElse(null);
        if (buyRule == null || buyRule.getBuyNum() == null || buyRule.getBuyNum() <= 0) {
            return new ArrayList<>();
        }

        int times = buyQuantity / buyRule.getBuyNum();
        if (times <= 0) {
            return new ArrayList<>();
        }

        // 聚合：同一赠品（商品+规格）可能来自多条配置时合并数量
        Map<String, Integer> agg = new LinkedHashMap<>();
        Map<String, MyRecord> sample = new LinkedHashMap<>();
        for (BuyGiftProduct gift : giftList) {
            if (gift.getProductId() == null || gift.getGiftNum() == null || gift.getGiftNum() <= 0) {
                continue;
            }
            int giftNum = gift.getGiftNum() * times;
            if (giftNum <= 0) {
                continue;
            }
            int attrValueId = gift.getAttrValueId() == null ? 0 : gift.getAttrValueId();
            String key = gift.getProductId() + ":" + attrValueId;
            agg.put(key, agg.getOrDefault(key, 0) + giftNum);
            if (!sample.containsKey(key)) {
                MyRecord r = new MyRecord();
                r.set("productId", gift.getProductId());
                r.set("attrValueId", attrValueId);
                sample.put(key, r);
            }
        }

        List<MyRecord> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : agg.entrySet()) {
            MyRecord r = sample.get(entry.getKey());
            if (r == null) {
                continue;
            }
            r.set("giftNum", entry.getValue());
            result.add(r);
        }
        return result;
    }
}

