package com.zbkj.service.service.impl;

import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.coupon.StoreCoupon;
import com.zbkj.common.model.coupon.StoreCouponUser;
import com.zbkj.common.model.user.User;
import com.zbkj.common.request.OrderComputedPriceRequest;
import com.zbkj.common.vo.MyRecord;
import com.zbkj.common.vo.OrderInfoDetailVo;
import com.zbkj.common.vo.OrderInfoVo;
import com.zbkj.service.service.StoreCouponService;
import com.zbkj.service.service.StoreCouponUserService;
import com.zbkj.service.service.StoreProductService;
import com.zbkj.service.service.promotion.PromotionCalculateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * 代金券相关金额计算的单元测试。
 * 覆盖：
 *  - H2：代金券抵扣运费时的"商品应付 / 剩余运费"拆分，避免运费被重复计入。
 *  - H3：把代金券（couponType=2）当作普通优惠券使用应被拒绝，防止同券双通道重复抵扣。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderPriceVoucherTest {

    @Mock
    private StoreCouponUserService storeCouponUserService;
    @Mock
    private StoreCouponService storeCouponService;
    @Mock
    private StoreProductService storeProductService;
    @Mock
    private PromotionCalculateService promotionCalculateService;

    @InjectMocks
    private OrderServiceImpl orderService;

    // ---------- H2：代金券抵运费拆分（纯函数） ----------

    @Test
    void splitVoucherDeduction_代金券超出商品应付时余额抵运费而非重复计入运费() {
        // 商品应付 10，代金券可抵 12（含运费），运费 5
        BigDecimal[] r = OrderServiceImpl.splitVoucherDeduction(
                new BigDecimal("10"), new BigDecimal("12"), new BigDecimal("5"));
        // 期望：商品应付 0，剩余运费 3（代金券 12 = 商品 10 + 运费 2）
        assertEquals(0, r[0].compareTo(BigDecimal.ZERO), "商品应付应为 0，实际=" + r[0]);
        assertEquals(0, r[1].compareTo(new BigDecimal("3")), "剩余运费应为 3，实际=" + r[1]);
    }

    @Test
    void splitVoucherDeduction_代金券不超过商品应付时运费不受影响() {
        BigDecimal[] r = OrderServiceImpl.splitVoucherDeduction(
                new BigDecimal("10"), new BigDecimal("4"), new BigDecimal("5"));
        assertEquals(0, r[0].compareTo(new BigDecimal("6")), "商品应付应为 6，实际=" + r[0]);
        assertEquals(0, r[1].compareTo(new BigDecimal("5")), "运费应保持 5，实际=" + r[1]);
    }

    // ---------- H3：代金券不能当作优惠券使用 ----------

    @Test
    void computedPrice_代金券被当作优惠券时应抛异常() throws Exception {
        User user = new User();
        user.setUid(100);
        user.setIntegral(0);

        OrderInfoDetailVo detail = new OrderInfoDetailVo();
        detail.setProductId(1);
        detail.setPayNum(1);
        detail.setVipPrice(new BigDecimal("100"));

        OrderInfoVo orderInfoVo = new OrderInfoVo();
        orderInfoVo.setProTotalFee(new BigDecimal("100"));
        orderInfoVo.setOrderDetailList(Collections.singletonList(detail));
        // seckillId/bargainId/combinationId/isVideo 已有默认值 0/0/0/false

        OrderComputedPriceRequest request = new OrderComputedPriceRequest();
        request.setShippingType(2);   // 门店自提：免运费，无需地址，规避私有运费计算
        request.setCouponId(5);       // 用一张"代金券"的领取记录冒充优惠券
        request.setVoucherId(0);
        request.setUseIntegral(false);

        // 满减无活动
        MyRecord fr = new MyRecord().set("reduction", (Object) null).set("reduceAmount", BigDecimal.ZERO);
        when(promotionCalculateService.calculateFullReduction(anyList(), anyList(), any())).thenReturn(fr);
        when(promotionCalculateService.isAllowCoupon(any())).thenReturn(Boolean.TRUE);
        when(storeProductService.getProductAllCategoryIdByProductIds(anyList())).thenReturn(Collections.emptyList());

        // couponId=5 的领取记录，其对应的券定义 couponId=77 是一张代金券（couponType=2）
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, -1);
        Date start = c.getTime();
        c.add(Calendar.DATE, 30);
        Date end = c.getTime();
        StoreCouponUser cu = new StoreCouponUser();
        cu.setId(5);
        cu.setCouponId(77);
        cu.setUid(100);
        cu.setStatus(0);
        cu.setMoney(new BigDecimal("20"));
        cu.setMinPrice(BigDecimal.ZERO);
        cu.setUseType(1);
        cu.setStartTime(start);
        cu.setEndTime(end);
        when(storeCouponUserService.getById(5)).thenReturn(cu);

        StoreCoupon voucherDef = new StoreCoupon();
        voucherDef.setCouponType(2); // 2 = 代金券
        when(storeCouponService.getById(77)).thenReturn(voucherDef);

        Method m = OrderServiceImpl.class.getDeclaredMethod(
                "computedPrice", OrderComputedPriceRequest.class, OrderInfoVo.class, User.class);
        m.setAccessible(true);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> m.invoke(orderService, request, orderInfoVo, user));
        assertTrue(ex.getCause() instanceof CrmebException,
                "应抛出 CrmebException，实际=" + ex.getCause());
        assertTrue(ex.getCause().getMessage() != null && ex.getCause().getMessage().contains("代金券"),
                "异常信息应说明代金券不能作为优惠券，实际=" + ex.getCause().getMessage());
    }
}
