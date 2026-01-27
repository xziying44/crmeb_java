package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 买赠活动搜索请求对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "BuyGiftSearchRequest", description = "买赠活动搜索请求对象")
public class BuyGiftSearchRequest extends PageParamRequest {

    @ApiModelProperty(value = "活动名称（模糊搜索）")
    private String name;

    @ApiModelProperty(value = "状态：0-关闭 1-开启")
    private Integer status;

    @ApiModelProperty(value = "类型：1-同商品买N送M 2-跨商品买A送B")
    private Integer giftType;
}

