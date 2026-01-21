package com.zbkj.common.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 客户消费统计VO
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
@ApiModel(value = "CustomerConsumeStatsVo对象", description = "客户消费统计VO")
public class CustomerConsumeStatsVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "用户ID")
    private Integer uid;

    @ApiModelProperty(value = "消费总额")
    private BigDecimal totalAmount;

    @ApiModelProperty(value = "订单数量")
    private Integer orderCount;

    @ApiModelProperty(value = "最近下单时间")
    private Date lastOrderTime;
}
