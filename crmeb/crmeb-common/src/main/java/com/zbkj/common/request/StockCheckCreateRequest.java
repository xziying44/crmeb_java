package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建盘点单请求对象
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
@ApiModel(value = "StockCheckCreateRequest", description = "创建盘点单请求对象")
public class StockCheckCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "盘点商品ID列表（商品主键），为空则盘点全部")
    private List<Integer> productIds;
}
