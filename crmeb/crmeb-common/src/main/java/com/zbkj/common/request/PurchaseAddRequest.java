package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 创建采购单请求对象
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
@ApiModel(value = "PurchaseAddRequest", description = "创建采购单请求对象")
public class PurchaseAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "供应商ID不能为空")
    @ApiModelProperty(value = "供应商ID", required = true)
    private Integer supplierId;

    @ApiModelProperty(value = "备注")
    private String remark;

    @NotEmpty(message = "采购商品不能为空")
    @Valid
    @ApiModelProperty(value = "采购商品列表", required = true)
    private List<PurchaseItemRequest> items;

    @Data
    @ApiModel(value = "PurchaseItemRequest", description = "采购商品明细")
    public static class PurchaseItemRequest implements Serializable {

        private static final long serialVersionUID = 1L;

        @NotNull(message = "商品ID不能为空")
        @ApiModelProperty(value = "商品ID", required = true)
        private Integer productId;

        @ApiModelProperty(value = "规格ID（0=无规格）")
        private Integer attrValueId = 0;

        @NotNull(message = "采购数量不能为空")
        @ApiModelProperty(value = "采购数量", required = true)
        private Integer quantity;

        @NotNull(message = "采购单价不能为空")
        @ApiModelProperty(value = "采购单价", required = true)
        private BigDecimal price;
    }
}

