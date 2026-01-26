package com.zbkj.common.response;

import com.zbkj.common.model.stock.PurchaseItem;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 采购单响应对象
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
@ApiModel(value = "PurchaseResponse", description = "采购单响应对象")
public class PurchaseResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "采购单ID")
    private Integer id;

    @ApiModelProperty(value = "采购单号")
    private String purchaseNo;

    @ApiModelProperty(value = "供应商ID")
    private Integer supplierId;

    @ApiModelProperty(value = "供应商名称")
    private String supplierName;

    @ApiModelProperty(value = "商品总数量")
    private Integer totalQuantity;

    @ApiModelProperty(value = "采购总金额")
    private BigDecimal totalAmount;

    @ApiModelProperty(value = "状态：0-待入库 1-部分入库 2-已入库 3-已取消")
    private Integer status;

    @ApiModelProperty(value = "状态名称")
    private String statusName;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "创建人")
    private String operatorName;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "采购明细")
    private List<PurchaseItem> items;
}

