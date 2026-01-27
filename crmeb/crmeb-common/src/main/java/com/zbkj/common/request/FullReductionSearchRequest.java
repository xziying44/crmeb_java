package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 满减活动搜索请求对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "FullReductionSearchRequest", description = "满减活动搜索请求对象")
public class FullReductionSearchRequest extends PageParamRequest {

    @ApiModelProperty(value = "活动名称（模糊搜索）")
    private String name;

    @ApiModelProperty(value = "状态：0-关闭 1-开启")
    private Integer status;

    @ApiModelProperty(value = "范围类型：1-全场 2-品类 3-指定商品")
    private Integer scopeType;
}

