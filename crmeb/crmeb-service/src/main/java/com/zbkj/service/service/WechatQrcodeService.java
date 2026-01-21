package com.zbkj.service.service;

/**
 * 微信小程序码服务接口
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
public interface WechatQrcodeService {

    /**
     * 生成业务员小程序码
     * @param salesmanCode 业务员邀请码
     * @return 小程序码图片路径（如：crmebimage/public/...，最终输出会在响应过滤器中自动加 CDN 前缀）
     */
    String generateSalesmanQrcode(String salesmanCode);
}

