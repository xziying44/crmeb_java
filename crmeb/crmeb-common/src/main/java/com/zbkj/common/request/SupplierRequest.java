package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 供应商请求对象
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
@ApiModel(value = "SupplierRequest", description = "供应商请求对象")
public class SupplierRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "供应商ID（编辑时必填）")
    private Integer id;

    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 100, message = "供应商名称不能超过100个字符")
    @ApiModelProperty(value = "供应商名称", required = true)
    private String name;

    @Size(max = 50, message = "联系人不能超过50个字符")
    @ApiModelProperty(value = "联系人")
    private String contact;

    @Size(max = 20, message = "联系电话不能超过20个字符")
    @ApiModelProperty(value = "联系电话")
    private String phone;

    @Size(max = 255, message = "地址不能超过255个字符")
    @ApiModelProperty(value = "地址")
    private String address;

    @Size(max = 500, message = "备注不能超过500个字符")
    @ApiModelProperty(value = "备注")
    private String remark;
}

