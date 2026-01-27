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

/**
 * 满减关联表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_full_reduction_product")
@ApiModel(value = "FullReductionProduct对象", description = "满减关联表")
public class FullReductionProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "满减活动ID")
    private Integer reductionId;

    @ApiModelProperty(value = "关联类型：1-品类 2-商品")
    private Integer relationType;

    @ApiModelProperty(value = "品类ID或商品ID")
    private Integer relationId;
}

