package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 活动横幅请求对象
 */
@Data
@ApiModel(value = "ActivityBannerRequest对象", description = "活动横幅请求")
public class ActivityBannerRequest {

    @ApiModelProperty(value = "主键（编辑时必填）")
    private Integer id;

    @NotEmpty(message = "横幅名称不能为空")
    @ApiModelProperty(value = "横幅名称", required = true)
    private String name;

    @NotEmpty(message = "横幅图片不能为空")
    @ApiModelProperty(value = "横幅图片地址", required = true)
    private String image;

    @NotNull(message = "活动类型不能为空")
    @ApiModelProperty(value = "活动类型：1=秒杀 2=砍价 3=拼团 4=买赠 5=满减", required = true)
    private Integer activityType;

    @ApiModelProperty(value = "排序值（越小越靠前）")
    private Integer sort;
}
