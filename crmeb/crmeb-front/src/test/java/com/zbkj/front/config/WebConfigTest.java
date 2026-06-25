package com.zbkj.front.config;

import org.junit.Test;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class WebConfigTest {

    @Test
    public void addInterceptors_首页首屏匿名接口应加入白名单() throws Exception {
        WebConfig webConfig = new WebConfig();
        InterceptorRegistry registry = new InterceptorRegistry();

        webConfig.addInterceptors(registry);

        List<InterceptorRegistration> registrations = getFieldValue(registry, "registrations");
        InterceptorRegistration registration = registrations.get(0);
        List<String> excludePatterns = getFieldValue(registration, "excludePatterns");

        assertTrue(excludePatterns.contains("/api/front/activity/banner/list"));
        assertTrue(excludePatterns.contains("/api/front/category/listByIds"));
    }

    @SuppressWarnings("unchecked")
    private <T> T getFieldValue(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(target);
    }
}
