package com.zbkj.service.service.promotion;

import com.zbkj.common.model.promotion.BuyGift;
import com.zbkj.common.model.promotion.FullReduction;
import com.zbkj.common.vo.MyRecord;

import java.math.BigDecimal;
import java.util.List;

/**
 * 促销计算服务
 */
public interface PromotionCalculateService {

    /**
     * 计算满减金额
     * @param productIds 商品ID列表
     * @param categoryIds 品类ID列表
     * @param totalAmount 商品总金额
     * @return 满减结果（reduction: FullReduction, reduceAmount: BigDecimal）
     */
    MyRecord calculateFullReduction(List<Integer> productIds, List<Integer> categoryIds, BigDecimal totalAmount);

    /**
     * 计算代金券抵扣金额
     * @param voucherId 代金券ID（用户持有的代金券记录ID）
     * @param productPayable 商品应付金额（扣除满减和优惠券后）
     * @param freightFee 运费
     * @return 代金券抵扣金额
     */
    BigDecimal calculateVoucherDeduction(Integer voucherId, BigDecimal productPayable, BigDecimal freightFee);

    /**
     * 检查满减活动是否允许使用优惠券
     */
    Boolean isAllowCoupon(Integer fullReductionId);

    /**
     * 获取商品的买赠活动
     */
    BuyGift getProductBuyGift(Integer productId, Integer uid);

    /**
     * 计算买赠赠品
     * @param giftId 买赠活动ID
     * @param productId 购买商品ID
     * @param buyQuantity 购买数量
     * @return 赠品信息列表（productId, attrValueId, giftNum）
     */
    List<MyRecord> calculateGiftProducts(Integer giftId, Integer productId, Integer buyQuantity);
}

