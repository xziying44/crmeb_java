package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 满减活动请求对象
 */
@Data
@ApiModel(value = "FullReductionRequest", description = "满减活动请求对象")
public class FullReductionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "活动ID（编辑时必填）")
    private Integer id;

    @ApiModelProperty(value = "活动名称", required = true)
    @NotBlank(message = "活动名称不能为空")
    private String name;

    @ApiModelProperty(value = "范围：1-全场 2-品类 3-指定商品", required = true)
    @NotNull(message = "请选择活动范围")
    private Integer scopeType;

    @ApiModelProperty(value = "开始时间", required = true)
    @NotBlank(message = "请选择开始时间")
    private String startTime;

    @ApiModelProperty(value = "结束时间", required = true)
    @NotBlank(message = "请选择结束时间")
    private String endTime;

    @ApiModelProperty(value = "是否允许叠加优惠券：0-否 1-是")
    private Boolean allowCoupon = true;

    @ApiModelProperty(value = "关联的品类/商品ID列表（scopeType=2或3时必填）")
    private List<Integer> relationIds;

    @ApiModelProperty(value = "阶梯满减列表", required = true)
    @NotNull(message = "请设置满减阶梯")
    private List<LevelItem> levels;

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

