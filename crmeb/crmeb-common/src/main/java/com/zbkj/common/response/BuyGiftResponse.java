package com.zbkj.common.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 买赠活动响应对象
 */
@Data
@ApiModel(value = "BuyGiftResponse", description = "买赠活动响应对象")
public class BuyGiftResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "活动ID")
    private Integer id;

    @ApiModelProperty(value = "活动名称")
    private String name;

    @ApiModelProperty(value = "类型：1-同商品买N送M 2-跨商品买A送B")
    private Integer giftType;

    @ApiModelProperty(value = "类型名称")
    private String giftTypeName;

    @ApiModelProperty(value = "开始时间")
    private Date startTime;

    @ApiModelProperty(value = "结束时间")
    private Date endTime;

    @ApiModelProperty(value = "限制类型：0-不限 1-总次数 2-每日次数")
    private Integer limitType;

    @ApiModelProperty(value = "限制类型名称")
    private String limitTypeName;

    @ApiModelProperty(value = "限制次数")
    private Integer limitNum;

    @ApiModelProperty(value = "状态：0-关闭 1-开启")
    private Boolean status;

    @ApiModelProperty(value = "活动状态：0-未开始 1-进行中 2-已结束")
    private Integer activityStatus;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "购买商品配置列表")
    private List<ProductItem> buyProducts;

    @ApiModelProperty(value = "赠品配置列表")
    private List<ProductItem> giftProducts;

    @Data
    public static class ProductItem implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "商品ID")
        private Integer productId;

        @ApiModelProperty(value = "规格ID（0=不限规格）")
        private Integer attrValueId;

        @ApiModelProperty(value = "购买数量")
        private Integer buyNum;

        @ApiModelProperty(value = "赠送数量")
        private Integer giftNum;
    }
}

