package com.zbkj.common.request;

import com.zbkj.common.constants.RegularConstants;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 业务员更新请求对象
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
@ApiModel(value = "SalesmanUpdateRequest对象", description = "业务员更新请求对象")
public class SalesmanUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "业务员ID（admin_id）", required = true)
    @NotNull(message = "ID不能为空")
    private Integer id;

    @ApiModelProperty(value = "业务员姓名")
    @Length(max = 16, message = "姓名长度不能超过16个字符")
    private String realName;

    @ApiModelProperty(value = "手机号码")
    @Pattern(regexp = RegularConstants.PHONE_TWO, message = "请填写正确的手机号")
    private String phone;

    @ApiModelProperty(value = "密码（不修改则不传）")
    @Length(max = 32, message = "密码长度不能超过32个字符")
    private String pwd;

    @ApiModelProperty(value = "是否可被绑定")
    private Boolean bindable;
}

