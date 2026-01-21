package com.zbkj.service.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SalesmanInfoServiceImpl 单元测试
 */
class SalesmanInfoServiceImplTest {

    @Test
    void generateUniqueCode_应生成6位且符合字符集() {
        SalesmanInfoServiceImpl service = Mockito.spy(new SalesmanInfoServiceImpl());
        Mockito.doReturn(false).when(service).isCodeExists(Mockito.anyString());

        String code = service.generateUniqueCode();

        assertNotNull(code);
        assertTrue(code.matches("^[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{6}$"),
                "邀请码应为6位且仅包含排除易混淆字符后的大写字母与数字");
    }

    @Test
    void generateUniqueCode_当始终冲突时应抛出异常() {
        SalesmanInfoServiceImpl service = Mockito.spy(new SalesmanInfoServiceImpl());
        Mockito.doReturn(true).when(service).isCodeExists(Mockito.anyString());

        RuntimeException ex = assertThrows(RuntimeException.class, service::generateUniqueCode);
        assertTrue(ex.getMessage().contains("无法生成唯一邀请码"));
    }
}

