package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 代金券购买请求对象
 */
@Data
@ApiModel(value = "VoucherBuyRequest", description = "代金券购买请求对象")
public class VoucherBuyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "代金券ID", required = true)
    @NotNull(message = "代金券ID不能为空")
    private Integer couponId;

    @ApiModelProperty(value = "支付方式: weixin-微信支付, yue-余额支付", required = true)
    @NotNull(message = "支付方式不能为空")
    private String payType;
}
