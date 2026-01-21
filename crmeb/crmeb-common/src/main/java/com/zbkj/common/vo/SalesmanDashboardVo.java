package com.zbkj.common.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 业务员数据看板VO
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
@ApiModel(value = "SalesmanDashboardVo对象", description = "业务员数据看板VO")
public class SalesmanDashboardVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "客户总数")
    private Integer totalCustomerCount;

    @ApiModelProperty(value = "本月新增客户数")
    private Integer monthNewCustomerCount;

    @ApiModelProperty(value = "客户订单总额")
    private BigDecimal totalOrderAmount;

    @ApiModelProperty(value = "本月订单金额")
    private BigDecimal monthOrderAmount;

    @ApiModelProperty(value = "销售趋势数据")
    private List<TrendDataVo> trendData;

    @ApiModelProperty(value = "客户消费排行榜")
    private List<CustomerRankingVo> customerRanking;

    @Data
    public static class TrendDataVo implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "日期")
        private String date;

        @ApiModelProperty(value = "订单数量")
        private Integer orderCount;

        @ApiModelProperty(value = "订单金额")
        private BigDecimal orderAmount;
    }

    @Data
    public static class CustomerRankingVo implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "用户ID")
        private Integer uid;

        @ApiModelProperty(value = "用户昵称")
        private String nickname;

        @ApiModelProperty(value = "手机号")
        private String phone;

        @ApiModelProperty(value = "消费总额")
        private BigDecimal totalAmount;

        @ApiModelProperty(value = "订单数量")
        private Integer orderCount;
    }
}

