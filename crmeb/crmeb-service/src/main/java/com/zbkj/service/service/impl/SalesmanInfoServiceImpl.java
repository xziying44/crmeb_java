package com.zbkj.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.model.salesman.SalesmanInfo;
import com.zbkj.service.dao.SalesmanInfoDao;
import com.zbkj.service.service.SalesmanInfoService;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * 业务员扩展信息服务实现类
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
@Service
public class SalesmanInfoServiceImpl extends ServiceImpl<SalesmanInfoDao, SalesmanInfo> implements SalesmanInfoService {

    /**
     * 邀请码字符集（排除容易混淆的字符：0,O,1,I,L）
     */
    private static final String CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    /**
     * 邀请码长度（阶段一：6位）
     */
    private static final int CODE_LENGTH = 6;

    @Override
    public SalesmanInfo getByAdminId(Integer adminId) {
        if (adminId == null || adminId <= 0) {
            return null;
        }
        LambdaQueryWrapper<SalesmanInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalesmanInfo::getAdminId, adminId);
        wrapper.last("LIMIT 1");
        return getOne(wrapper);
    }

    @Override
    public SalesmanInfo getByCode(String salesmanCode) {
        if (salesmanCode == null || salesmanCode.trim().isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<SalesmanInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalesmanInfo::getSalesmanCode, salesmanCode.trim().toUpperCase());
        wrapper.last("LIMIT 1");
        return getOne(wrapper);
    }

    @Override
    public String generateUniqueCode() {
        String code;
        int maxAttempts = 100;
        int attempts = 0;
        do {
            code = generateRandomCode();
            attempts++;
            if (attempts > maxAttempts) {
                throw new RuntimeException("无法生成唯一邀请码，请稍后重试");
            }
        } while (isCodeExists(code));
        return code;
    }

    @Override
    public boolean isCodeExists(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<SalesmanInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalesmanInfo::getSalesmanCode, code.trim().toUpperCase());
        return count(wrapper) > 0;
    }

    /**
     * 生成随机邀请码
     */
    private String generateRandomCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}

