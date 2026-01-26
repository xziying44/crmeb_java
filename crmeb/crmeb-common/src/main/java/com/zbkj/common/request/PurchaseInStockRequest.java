package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 采购入库请求对象
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
@ApiModel(value = "PurchaseInStockRequest", description = "采购入库请求对象")
public class PurchaseInStockRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "采购单ID不能为空")
    @ApiModelProperty(value = "采购单ID", required = true)
    private Integer purchaseId;

    @NotEmpty(message = "入库明细不能为空")
    @Valid
    @ApiModelProperty(value = "入库明细列表", required = true)
    private List<InStockItemRequest> items;

    @Data
    @ApiModel(value = "InStockItemRequest", description = "入库明细")
    public static class InStockItemRequest implements Serializable {

        private static final long serialVersionUID = 1L;

        @NotNull(message = "采购明细ID不能为空")
        @ApiModelProperty(value = "采购明细ID", required = true)
        private Integer itemId;

        @NotNull(message = "入库数量不能为空")
        @ApiModelProperty(value = "本次入库数量", required = true)
        private Integer quantity;
    }
}

