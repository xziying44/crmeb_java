package com.zbkj.common.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 库存响应对象
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
@ApiModel(value = "StockResponse", description = "库存响应对象")
public class StockResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "库存ID")
    private Integer id;

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

    @ApiModelProperty(value = "当前库存")
    private Integer stock;

    @ApiModelProperty(value = "预警库存")
    private Integer warningStock;

    @ApiModelProperty(value = "是否预警")
    private Boolean isWarning;

    @ApiModelProperty(value = "更新时间")
    private Date updateTime;
}

