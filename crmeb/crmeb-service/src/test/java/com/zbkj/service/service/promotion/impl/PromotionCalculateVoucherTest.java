package com.zbkj.service.service.promotion.impl;

import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.coupon.StoreCoupon;
import com.zbkj.common.model.coupon.StoreCouponUser;
import com.zbkj.service.service.StoreCouponService;
import com.zbkj.service.service.StoreCouponUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * M1：代金券抵扣需校验最低使用门槛（minPrice），与优惠券一致，防止低于门槛使用。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PromotionCalculateVoucherTest {

    @Mock
    private StoreCouponUserService storeCouponUserService;
    @Mock
    private StoreCouponService storeCouponService;

    @InjectMocks
    private PromotionCalculateServiceImpl service;

    private StoreCouponUser voucherUser(BigDecimal money, BigDecimal minPrice) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, -1);
        Date start = c.getTime();
        c.add(Calendar.DATE, 30);
        Date end = c.getTime();
        StoreCouponUser cu = new StoreCouponUser();
        cu.setId(5);
        cu.setCouponId(77);
        cu.setStatus(0);
        cu.setMoney(money);
        cu.setMinPrice(minPrice);
        cu.setStartTime(start);
        cu.setEndTime(end);
        return cu;
    }

    private StoreCoupon voucherDef() {
        StoreCoupon coupon = new StoreCoupon();
        coupon.setCouponType(2);
        coupon.setCanDeductFreight(false);
        coupon.setIsDel(false);
        return coupon;
    }

    @Test
    void calculateVoucherDeduction_未达最低使用门槛时抛异常() {
        when(storeCouponUserService.getById(5)).thenReturn(voucherUser(new BigDecimal("20"), new BigDecimal("100")));
        when(storeCouponService.getById(77)).thenReturn(voucherDef());

        // 订单可抵金额 50 < 门槛 100
        CrmebException ex = assertThrows(CrmebException.class,
                () -> service.calculateVoucherDeduction(5, new BigDecimal("50"), BigDecimal.ZERO));
        assertTrue(ex.getMessage() != null && ex.getMessage().contains("门槛"),
                "应提示未达使用门槛，实际=" + ex.getMessage());
    }

    @Test
    void calculateVoucherDeduction_达到门槛时正常抵扣() {
        when(storeCouponUserService.getById(5)).thenReturn(voucherUser(new BigDecimal("8"), new BigDecimal("5")));
        when(storeCouponService.getById(77)).thenReturn(voucherDef());

        BigDecimal deduct = service.calculateVoucherDeduction(5, new BigDecimal("10"), BigDecimal.ZERO);
        assertEquals(0, deduct.compareTo(new BigDecimal("8")), "应抵扣 8，实际=" + deduct);
    }
}
