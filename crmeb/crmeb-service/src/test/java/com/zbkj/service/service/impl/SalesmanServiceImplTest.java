package com.zbkj.service.service.impl;

import com.zbkj.common.model.salesman.SalesmanInfo;
import com.zbkj.service.service.SalesmanInfoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * SalesmanServiceImpl 单元测试（聚焦纯逻辑方法）
 */
@ExtendWith(MockitoExtension.class)
class SalesmanServiceImplTest {

    @Mock
    private SalesmanInfoService salesmanInfoService;

    @InjectMocks
    private SalesmanServiceImpl salesmanService;

    @Test
    void checkCode_邀请码不存在时返回false() {
        when(salesmanInfoService.getByCode("ABC123")).thenReturn(null);
        assertFalse(salesmanService.checkCode("ABC123"));
    }

    @Test
    void checkCode_可绑定时返回true() {
        SalesmanInfo info = new SalesmanInfo();
        info.setBindable(true);
        when(salesmanInfoService.getByCode("ABC123")).thenReturn(info);

        assertTrue(salesmanService.checkCode("ABC123"));
    }

    @Test
    void checkCode_不可绑定或为空时返回false() {
        SalesmanInfo info = new SalesmanInfo();
        info.setBindable(false);
        when(salesmanInfoService.getByCode("ABC123")).thenReturn(info);
        assertFalse(salesmanService.checkCode("ABC123"));

        SalesmanInfo infoNull = new SalesmanInfo();
        infoNull.setBindable(null);
        when(salesmanInfoService.getByCode("DEF456")).thenReturn(infoNull);
        assertFalse(salesmanService.checkCode("DEF456"));
    }
}

