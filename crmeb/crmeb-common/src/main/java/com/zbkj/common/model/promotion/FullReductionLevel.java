package com.zbkj.common.model.promotion;

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
 * 满减阶梯表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_full_reduction_level")
@ApiModel(value = "FullReductionLevel对象", description = "满减阶梯表")
public class FullReductionLevel implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "满减活动ID")
    private Integer reductionId;

    @ApiModelProperty(value = "满足金额")
    private BigDecimal fullAmount;

    @ApiModelProperty(value = "减免金额")
    private BigDecimal reduceAmount;
}

