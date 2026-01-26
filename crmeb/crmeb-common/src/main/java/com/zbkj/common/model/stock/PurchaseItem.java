package com.zbkj.common.model.stock;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 采购单明细表
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
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_purchase_item")
@ApiModel(value = "PurchaseItem对象", description = "采购单明细表")
public class PurchaseItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "采购单ID")
    private Integer purchaseId;

    @ApiModelProperty(value = "商品ID")
    private Integer productId;

    @ApiModelProperty(value = "规格ID")
    private Integer attrValueId;

    @ApiModelProperty(value = "商品名称")
    private String productName;

    @ApiModelProperty(value = "规格名称")
    private String skuName;

    @ApiModelProperty(value = "采购数量")
    private Integer quantity;

    @ApiModelProperty(value = "已入库数量")
    private Integer inQuantity;

    @ApiModelProperty(value = "采购单价")
    private BigDecimal price;

    @ApiModelProperty(value = "小计金额")
    private BigDecimal amount;
}

