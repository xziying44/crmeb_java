package com.zbkj.service.service.impl;

import cn.hutool.core.util.StrUtil;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.vo.FileResultVo;
import com.zbkj.service.service.UploadService;
import com.zbkj.service.service.WechatNewService;
import com.zbkj.service.service.WechatQrcodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

/**
 * 微信小程序码服务实现类
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
@Slf4j
@Service
public class WechatQrcodeServiceImpl implements WechatQrcodeService {

    @Autowired
    private WechatNewService wechatNewService;

    @Autowired
    private UploadService uploadService;

    @Override
    public String generateSalesmanQrcode(String salesmanCode) {
        if (StrUtil.isBlank(salesmanCode)) {
            throw new CrmebException("邀请码不能为空");
        }

        try {
            // scene参数格式：s_邀请码（s_前缀用于标识业务员绑定场景）
            String scene = "s_" + salesmanCode.trim().toUpperCase();
            // 小程序页面路径（绑定页面）
            String page = "pages/salesman/bind";

            // 生成 base64 小程序码
            String base64Image = wechatNewService.createQrCode(page, scene);
            byte[] qrcodeData = decodeBase64Image(base64Image);
            if (qrcodeData == null || qrcodeData.length == 0) {
                throw new CrmebException("生成小程序码失败");
            }

            // 复用系统上传逻辑（支持本地/七牛/OSS/COS/京东云等配置）
            String fileName = "salesman_qrcode_" + salesmanCode.trim().toUpperCase() + ".png";
            MultipartFile multipartFile = new ByteArrayMultipartFile(qrcodeData, fileName, "image/png");
            FileResultVo uploaded = uploadService.imageUpload(multipartFile, "wechat", 8);

            return uploaded.getUrl();
        } catch (CrmebException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成业务员小程序码失败: {}", e.getMessage(), e);
            throw new CrmebException("生成小程序码失败");
        }
    }

    /**
     * 将 data:image/png;base64,... 转为字节数组
     */
    private byte[] decodeBase64Image(String base64Image) {
        if (StrUtil.isBlank(base64Image)) {
            return new byte[0];
        }
        String data = base64Image.trim();
        int commaIndex = data.indexOf(',');
        String base64 = commaIndex >= 0 ? data.substring(commaIndex + 1) : data;
        return Base64.getDecoder().decode(base64);
    }

    /**
     * 用于将内存字节数组适配为 MultipartFile，复用系统上传链路。
     */
    private static class ByteArrayMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String originalFilename;
        private final String contentType;

        private ByteArrayMultipartFile(byte[] content, String originalFilename, String contentType) {
            this.content = content;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
        }

        @Override
        public String getName() {
            return originalFilename;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize() {
            return content == null ? 0 : content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            org.springframework.util.FileCopyUtils.copy(content, dest);
        }
    }
}

