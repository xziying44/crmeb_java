package com.zbkj.common.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 业务员响应对象
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
@ApiModel(value = "SalesmanResponse对象", description = "业务员响应对象")
public class SalesmanResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "业务员ID（admin_id）")
    private Integer id;

    @ApiModelProperty(value = "账号")
    private String account;

    @ApiModelProperty(value = "姓名")
    private String realName;

    @ApiModelProperty(value = "手机号")
    private String phone;

    @ApiModelProperty(value = "邀请码")
    private String salesmanCode;

    @ApiModelProperty(value = "小程序码图片地址")
    private String salesmanQrcode;

    @ApiModelProperty(value = "是否可被绑定")
    private Boolean bindable;

    @ApiModelProperty(value = "状态：0-禁用，1-正常")
    private Boolean status;

    @ApiModelProperty(value = "客户数量")
    private Integer customerCount;

    @ApiModelProperty(value = "本月新增客户数")
    private Integer monthNewCustomerCount;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;
}

