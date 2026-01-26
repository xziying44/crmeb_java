package com.zbkj.common.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 库存流水响应对象
 */
@Data
@ApiModel(value = "StockLogResponse", description = "库存流水响应对象")
public class StockLogResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "流水ID")
    private Long id;

    @ApiModelProperty(value = "商品ID")
    private Integer productId;

    @ApiModelProperty(value = "商品名称")
    private String productName;

    @ApiModelProperty(value = "商品图片")
    private String productImage;

    @ApiModelProperty(value = "规格ID")
    private Integer attrValueId;

    @ApiModelProperty(value = "规格名称")
    private String skuName;

    @ApiModelProperty(value = "类型")
    private Integer type;

    @ApiModelProperty(value = "类型名称")
    private String typeName;

    @ApiModelProperty(value = "数量（正=入库，负=出库）")
    private Integer quantity;

    @ApiModelProperty(value = "变动前库存")
    private Integer beforeStock;

    @ApiModelProperty(value = "变动后库存")
    private Integer afterStock;

    @ApiModelProperty(value = "关联单据ID")
    private Long relationId;

    @ApiModelProperty(value = "关联单据类型")
    private String relationType;

    @ApiModelProperty(value = "操作人")
    private String operatorName;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;
}

