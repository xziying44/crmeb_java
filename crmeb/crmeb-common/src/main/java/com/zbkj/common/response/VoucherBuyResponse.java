package com.zbkj.common.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 代金券购买响应对象
 */
@Data
@ApiModel(value = "VoucherBuyResponse", description = "代金券购买响应对象")
public class VoucherBuyResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "订单号")
    private String orderNo;

    @ApiModelProperty(value = "支付金额")
    private BigDecimal payPrice;

    @ApiModelProperty(value = "代金券面值")
    private BigDecimal voucherMoney;

    @ApiModelProperty(value = "代金券名称")
    private String voucherName;
}
