package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 手动出库请求对象
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
@ApiModel(value = "StockOutRequest", description = "手动出库请求对象")
public class StockOutRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "商品ID不能为空")
    @ApiModelProperty(value = "商品ID", required = true)
    private Integer productId;

    @ApiModelProperty(value = "规格ID（0=无规格）")
    private Integer attrValueId = 0;

    @NotNull(message = "出库类型不能为空")
    @ApiModelProperty(value = "出库类型：6-报损出库 8-其他出库", required = true)
    private Integer type;

    @NotNull(message = "出库数量不能为空")
    @Min(value = 1, message = "出库数量必须大于0")
    @ApiModelProperty(value = "出库数量", required = true)
    private Integer quantity;

    @ApiModelProperty(value = "备注")
    private String remark;
}

