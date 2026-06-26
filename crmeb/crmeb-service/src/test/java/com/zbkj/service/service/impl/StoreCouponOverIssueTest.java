package com.zbkj.service.service.impl;

import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.coupon.StoreCoupon;
import com.zbkj.common.model.coupon.StoreCouponUser;
import com.zbkj.common.request.StoreCouponUserRequest;
import com.zbkj.common.request.UserCouponReceiveRequest;
import com.zbkj.service.dao.StoreCouponUserDao;
import com.zbkj.service.service.StoreCouponService;
import com.zbkj.service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * M3：限量优惠券发放/领取的并发超发防护。
 * 缺陷：发券采用"先 save 券、再 deduction 且忽略返回"或非原子 updateById 盲写 last_total，
 * 并发下多个请求都能通过校验并各自发券，导致发券数超过限量 last_total。
 * 正确：限量时先原子 deduction()（SQL: last_total = last_total - n WHERE last_total - n >= 0），
 * 扣减成功才发券；失败抛异常回滚（用户/管理员主动领取/发放场景）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StoreCouponOverIssueTest {

    @Mock
    private StoreCouponUserDao dao;
    @Mock
    private StoreCouponService storeCouponService;
    @Mock
    private UserService userService;
    @Mock
    private TransactionTemplate transactionTemplate;

    @Spy
    @InjectMocks
    private StoreCouponUserServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // 让 transactionTemplate.execute 真正执行回调，以便覆盖事务体内的扣减/发券逻辑
        when(transactionTemplate.execute(any())).thenAnswer(inv ->
                ((TransactionCallback<Boolean>) inv.getArgument(0)).doInTransaction(null));
        // 屏蔽真实持久化，便于断言"是否发券"
        doReturn(true).when(service).save(any(StoreCouponUser.class));
        doReturn(true).when(service).saveBatch(anyList());
        // filterReceiveUserInUid 查"是否已领过" -> 返回空表示未领过
        when(dao.selectList(any())).thenReturn(Collections.emptyList());
    }

    private StoreCoupon limitedCoupon(int id, int lastTotal) {
        Calendar c = Calendar.getInstance();
        Date start = c.getTime();
        c.add(Calendar.DATE, 30);
        Date end = c.getTime();
        StoreCoupon coupon = new StoreCoupon();
        coupon.setId(id);
        coupon.setName("限量券");
        coupon.setMoney(new BigDecimal("10"));
        coupon.setMinPrice(new BigDecimal("100"));
        coupon.setIsLimited(true);
        coupon.setLastTotal(lastTotal);
        coupon.setIsFixedTime(true);     // 固定时间，跳过按 day 计算
        coupon.setUseStartTime(start);
        coupon.setUseEndTime(end);
        coupon.setUseType(1);            // 跳过 primaryKey 分支
        return coupon;
    }

    // ---------- 用户自助领券（高并发路径） ----------

    @Test
    void receiveCoupon_限量券原子扣减失败时抛异常且不发券() {
        when(storeCouponService.getInfoException(7)).thenReturn(limitedCoupon(7, 1));
        when(userService.getUserIdException()).thenReturn(100);
        // 并发下被他人抢先领走：原子扣减返回 false
        when(storeCouponService.deduction(7, 1, true)).thenReturn(false);

        UserCouponReceiveRequest req = new UserCouponReceiveRequest();
        req.setCouponId(7);

        CrmebException ex = assertThrows(CrmebException.class, () -> service.receiveCoupon(req));
        assertTrue(ex.getMessage() != null && (ex.getMessage().contains("数量") || ex.getMessage().contains("领取")),
                "应提示剩余数量不足，实际=" + ex.getMessage());
        verify(service, never()).save(any(StoreCouponUser.class)); // 扣减失败绝不能发券
    }

    @Test
    void receiveCoupon_扣减成功时先扣后发() {
        when(storeCouponService.getInfoException(7)).thenReturn(limitedCoupon(7, 1));
        when(userService.getUserIdException()).thenReturn(100);
        when(storeCouponService.deduction(7, 1, true)).thenReturn(true);

        UserCouponReceiveRequest req = new UserCouponReceiveRequest();
        req.setCouponId(7);

        Boolean ok = service.receiveCoupon(req);
        assertTrue(ok);
        // 必须"先原子扣减、后发券"
        InOrder order = inOrder(storeCouponService, service);
        order.verify(storeCouponService).deduction(7, 1, true);
        order.verify(service).save(any(StoreCouponUser.class));
    }

    // ---------- 后台批量发券 ----------

    @Test
    void receive_批量发券限量扣减失败时抛异常且不发券() {
        when(storeCouponService.getInfoException(7)).thenReturn(limitedCoupon(7, 5));
        // 3 个用户
        when(storeCouponService.deduction(eq(7), eq(3), eq(true))).thenReturn(false);

        StoreCouponUserRequest req = new StoreCouponUserRequest();
        req.setCouponId(7);
        req.setUid("1,2,3");

        CrmebException ex = assertThrows(CrmebException.class, () -> service.receive(req));
        assertTrue(ex.getMessage() != null && (ex.getMessage().contains("数量") || ex.getMessage().contains("领取")),
                "应提示剩余数量不足，实际=" + ex.getMessage());
        verify(service, never()).saveBatch(anyList());
    }

    @Test
    void receive_批量发券扣减成功时先扣后发() {
        when(storeCouponService.getInfoException(7)).thenReturn(limitedCoupon(7, 5));
        when(storeCouponService.deduction(eq(7), eq(3), eq(true))).thenReturn(true);

        StoreCouponUserRequest req = new StoreCouponUserRequest();
        req.setCouponId(7);
        req.setUid("1,2,3");

        Boolean ok = service.receive(req);
        assertTrue(ok);
        InOrder order = inOrder(storeCouponService, service);
        order.verify(storeCouponService).deduction(7, 3, true);
        order.verify(service).saveBatch(anyList());
    }

    // ---------- 自动赠送（注册赠券/满额赠券）：限量售罄跳过、不阻断主流程 ----------

    private StoreCouponUser cu(int couponId) {
        StoreCouponUser u = new StoreCouponUser();
        u.setCouponId(couponId);
        return u;
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void grantCouponsSkipExhausted_限量券售罄则跳过_仅发成功的券且不抛异常() {
        when(storeCouponService.deduction(7, 1, true)).thenReturn(false);  // 限量券已售罄
        when(storeCouponService.deduction(8, 1, false)).thenReturn(true);  // 非限量
        when(storeCouponService.deduction(9, 1, true)).thenReturn(true);   // 限量券有货

        List<StoreCouponUser> users = Arrays.asList(cu(7), cu(8), cu(9));
        Map<Integer, Boolean> limited = new HashMap<>();
        limited.put(7, true);
        limited.put(8, false);
        limited.put(9, true);

        int granted = service.grantCouponsSkipExhausted(users, limited);

        assertEquals(2, granted, "应只发放扣减成功的券(8,9)，售罄的限量券7跳过");
        // 先扣减后发券，且 saveBatch 只含扣减成功的券；不抛异常即证明不会阻断注册/支付
        ArgumentCaptor<List> cap = ArgumentCaptor.forClass(List.class);
        verify(service).saveBatch(cap.capture());
        assertEquals(2, cap.getValue().size(), "saveBatch 只应包含发放成功的券");
        InOrder order = inOrder(storeCouponService, service);
        order.verify(storeCouponService).deduction(9, 1, true);
        order.verify(service).saveBatch(anyList());
    }
}
