package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 买赠活动请求对象
 */
@Data
@ApiModel(value = "BuyGiftRequest", description = "买赠活动请求对象")
public class BuyGiftRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "活动ID（编辑时必填）")
    private Integer id;

    @ApiModelProperty(value = "活动名称", required = true)
    @NotBlank(message = "活动名称不能为空")
    private String name;

    @ApiModelProperty(value = "类型：1-同商品买N送M 2-跨商品买A送B", required = true)
    @NotNull(message = "请选择买赠类型")
    private Integer giftType;

    @ApiModelProperty(value = "开始时间", required = true)
    @NotBlank(message = "请选择开始时间")
    private String startTime;

    @ApiModelProperty(value = "结束时间", required = true)
    @NotBlank(message = "请选择结束时间")
    private String endTime;

    @ApiModelProperty(value = "限制类型：0-不限 1-总次数 2-每日次数", required = true)
    @NotNull(message = "请选择限制类型")
    private Integer limitType = 0;

    @ApiModelProperty(value = "限制次数（limitType=1或2时必填）")
    private Integer limitNum = 0;

    @ApiModelProperty(value = "购买商品配置列表", required = true)
    @NotNull(message = "请配置购买商品")
    @Valid
    private List<ProductItem> buyProducts;

    @ApiModelProperty(value = "赠品配置列表", required = true)
    @NotNull(message = "请配置赠品")
    @Valid
    private List<ProductItem> giftProducts;

    @Data
    public static class ProductItem implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "商品ID", required = true)
        @NotNull(message = "商品ID不能为空")
        private Integer productId;

        @ApiModelProperty(value = "规格ID（0=不限规格）")
        private Integer attrValueId = 0;

        @ApiModelProperty(value = "购买数量（购买商品时填写）")
        private Integer buyNum = 0;

        @ApiModelProperty(value = "赠送数量（赠品时填写）")
        private Integer giftNum = 0;
    }
}

