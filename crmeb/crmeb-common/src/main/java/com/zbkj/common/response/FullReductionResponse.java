package com.zbkj.common.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 满减活动响应对象
 */
@Data
@ApiModel(value = "FullReductionResponse", description = "满减活动响应对象")
public class FullReductionResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "活动ID")
    private Integer id;

    @ApiModelProperty(value = "活动名称")
    private String name;

    @ApiModelProperty(value = "范围：1-全场 2-品类 3-指定商品")
    private Integer scopeType;

    @ApiModelProperty(value = "范围类型名称")
    private String scopeTypeName;

    @ApiModelProperty(value = "开始时间")
    private Date startTime;

    @ApiModelProperty(value = "结束时间")
    private Date endTime;

    @ApiModelProperty(value = "是否允许叠加优惠券")
    private Boolean allowCoupon;

    @ApiModelProperty(value = "状态：0-关闭 1-开启")
    private Boolean status;

    @ApiModelProperty(value = "活动状态：0-未开始 1-进行中 2-已结束")
    private Integer activityStatus;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "阶梯满减列表")
    private List<LevelItem> levels;

    @ApiModelProperty(value = "关联的品类/商品ID列表")
    private List<Integer> relationIds;

    /**
     * 阶梯项
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

