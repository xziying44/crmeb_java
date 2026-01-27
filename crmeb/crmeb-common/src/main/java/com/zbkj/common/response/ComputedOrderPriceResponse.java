package com.zbkj.common.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 计算订单价格响应对象
 * +----------------------------------------------------------------------
 * | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 * +----------------------------------------------------------------------
 * | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
 * +----------------------------------------------------------------------
 * | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 * +----------------------------------------------------------------------
 * | Author: CRMEB Team <admin@crmeb.com>
 * +----------------------------------------------------------------------
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="ComputedOrderPriceResponse对象", description="计算订单价格响应对象")
public class ComputedOrderPriceResponse implements Serializable {

    private static final long serialVersionUID = 7282892323898493847L;

    @ApiModelProperty(value = "优惠券优惠金额")
    private BigDecimal couponFee;

    @ApiModelProperty(value = "满减活动ID")
    private Integer fullReductionId;

    @ApiModelProperty(value = "满减活动名称")
    private String fullReductionName;

    @ApiModelProperty(value = "满减金额")
    private BigDecimal fullReductionPrice;

    @ApiModelProperty(value = "代金券ID")
    private Integer voucherId;

    @ApiModelProperty(value = "代金券抵扣金额")
    private BigDecimal voucherPrice;

    @ApiModelProperty(value = "是否允许使用优惠券（满减活动配置）")
    private Boolean allowCoupon;

    @ApiModelProperty(value = "买赠赠品列表")
    private List<GiftProductVo> giftProducts;

    @ApiModelProperty(value = "积分抵扣金额")
    private BigDecimal deductionPrice;

    @ApiModelProperty(value = "运费金额")
    private BigDecimal freightFee;

    @ApiModelProperty(value = "实际支付金额")
    private BigDecimal payFee;

    @ApiModelProperty(value = "商品总金额")
    private BigDecimal proTotalFee;

    @ApiModelProperty(value = "剩余积分")
    private Integer surplusIntegral;

    @ApiModelProperty(value = "是否使用积分")
    private Boolean useIntegral;

    @ApiModelProperty(value = "使用的积分")
    private Integer usedIntegral;

    @Data
    public static class GiftProductVo implements Serializable {
        private static final long serialVersionUID = 1L;

        private Integer productId;
        private String productName;
        private String productImage;
        private Integer attrValueId;
        private String skuName;
        private Integer giftNum;
    }
}
