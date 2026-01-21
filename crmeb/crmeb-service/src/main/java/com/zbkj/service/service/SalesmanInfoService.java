package com.zbkj.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.salesman.SalesmanInfo;

/**
 * 业务员扩展信息服务接口
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
public interface SalesmanInfoService extends IService<SalesmanInfo> {

    /**
     * 根据管理员ID获取业务员信息
     */
    SalesmanInfo getByAdminId(Integer adminId);

    /**
     * 根据邀请码获取业务员信息
     */
    SalesmanInfo getByCode(String salesmanCode);

    /**
     * 生成唯一邀请码
     */
    String generateUniqueCode();

    /**
     * 检查邀请码是否存在
     */
    boolean isCodeExists(String code);
}

