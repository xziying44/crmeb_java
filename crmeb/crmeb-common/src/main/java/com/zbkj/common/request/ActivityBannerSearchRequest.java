package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 活动横幅搜索请求对象
 */
@Data
@ApiModel(value = "ActivityBannerSearchRequest对象", description = "活动横幅搜索请求")
public class ActivityBannerSearchRequest {

    @ApiModelProperty(value = "状态：0=下线 1=上线")
    private Integer status;

    @ApiModelProperty(value = "横幅名称（模糊查询）")
    private String name;
}
