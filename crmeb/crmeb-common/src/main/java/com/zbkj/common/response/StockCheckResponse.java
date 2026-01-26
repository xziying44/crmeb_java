package com.zbkj.common.response;

import com.zbkj.common.model.stock.StockCheckItem;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 盘点单响应对象
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
@ApiModel(value = "StockCheckResponse", description = "盘点单响应对象")
public class StockCheckResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "盘点单ID")
    private Integer id;

    @ApiModelProperty(value = "盘点单号")
    private String checkNo;

    @ApiModelProperty(value = "状态：0-盘点中 1-已完成 2-已取消")
    private Integer status;

    @ApiModelProperty(value = "状态名称")
    private String statusName;

    @ApiModelProperty(value = "盘盈总数")
    private Integer totalProfit;

    @ApiModelProperty(value = "盘亏总数")
    private Integer totalLoss;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "创建人")
    private String operatorName;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "完成时间")
    private Date finishTime;

    @ApiModelProperty(value = "盘点明细")
    private List<StockCheckItem> items;
}

