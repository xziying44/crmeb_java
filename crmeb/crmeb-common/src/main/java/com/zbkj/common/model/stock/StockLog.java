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
import java.util.Date;

/**
 * 库存流水表
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
@TableName("eb_stock_log")
@ApiModel(value = "StockLog对象", description = "库存流水表")
public class StockLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "商品ID")
    private Integer productId;

    @ApiModelProperty(value = "规格ID")
    private Integer attrValueId;

    @ApiModelProperty(value = "类型：1-采购入库 2-退货入库 3-盘盈入库 4-其他入库 5-销售出库 6-报损出库 7-盘亏出库 8-其他出库")
    private Integer type;

    @ApiModelProperty(value = "数量（正=入库，负=出库）")
    private Integer quantity;

    @ApiModelProperty(value = "变动前库存")
    private Integer beforeStock;

    @ApiModelProperty(value = "变动后库存")
    private Integer afterStock;

    @ApiModelProperty(value = "关联单据ID")
    private Long relationId;

    @ApiModelProperty(value = "关联单据类型：purchase/order/check")
    private String relationType;

    @ApiModelProperty(value = "操作人ID")
    private Integer operatorId;

    @ApiModelProperty(value = "操作人姓名")
    private String operatorName;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;
}

