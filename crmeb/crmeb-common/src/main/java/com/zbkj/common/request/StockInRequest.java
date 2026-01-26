package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 手动入库请求对象
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
@ApiModel(value = "StockInRequest", description = "手动入库请求对象")
public class StockInRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "商品ID不能为空")
    @ApiModelProperty(value = "商品ID", required = true)
    private Integer productId;

    @ApiModelProperty(value = "规格ID（0=无规格）")
    private Integer attrValueId = 0;

    @NotNull(message = "入库类型不能为空")
    @ApiModelProperty(value = "入库类型：2-退货入库 4-其他入库", required = true)
    private Integer type;

    @NotNull(message = "入库数量不能为空")
    @Min(value = 1, message = "入库数量必须大于0")
    @ApiModelProperty(value = "入库数量", required = true)
    private Integer quantity;

    @ApiModelProperty(value = "备注")
    private String remark;
}

