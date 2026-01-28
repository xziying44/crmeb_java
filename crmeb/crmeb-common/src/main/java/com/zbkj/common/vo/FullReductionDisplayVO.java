package com.zbkj.common.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 满减活动展示信息 VO
 * 用于商品详情页展示满减活动阶梯
 */
@Data
@ApiModel(value = "FullReductionDisplayVO", description = "满减活动展示信息")
public class FullReductionDisplayVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "活动ID")
    private Integer id;

    @ApiModelProperty(value = "活动名称")
    private String name;

    @ApiModelProperty(value = "阶梯列表，按满足金额升序排列")
    private List<LevelItem> levels;

    /**
     * 满减阶梯项
     */
    @Data
    public static class LevelItem implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "满足金额")
        private BigDecimal fullAmount;

        @ApiModelProperty(value = "减免金额")
        private BigDecimal reduceAmount;
    }
}
