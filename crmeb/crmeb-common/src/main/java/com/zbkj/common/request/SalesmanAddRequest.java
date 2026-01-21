package com.zbkj.common.request;

import com.zbkj.common.constants.RegularConstants;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 业务员新增请求对象
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
@ApiModel(value = "SalesmanAddRequest对象", description = "业务员新增请求对象")
public class SalesmanAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "业务员账号", required = true)
    @NotBlank(message = "账号不能为空")
    @Length(max = 32, message = "账号长度不能超过32个字符")
    private String account;

    @ApiModelProperty(value = "业务员密码", required = true)
    @NotBlank(message = "密码不能为空")
    @Length(max = 32, message = "密码长度不能超过32个字符")
    private String pwd;

    @ApiModelProperty(value = "业务员姓名", required = true)
    @NotBlank(message = "姓名不能为空")
    @Length(max = 16, message = "姓名长度不能超过16个字符")
    private String realName;

    @ApiModelProperty(value = "手机号码", required = true)
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = RegularConstants.PHONE_TWO, message = "请填写正确的手机号")
    private String phone;

    @ApiModelProperty(value = "状态 1有效0无效", required = true)
    @NotNull(message = "状态不能为空")
    private Boolean status;
}

