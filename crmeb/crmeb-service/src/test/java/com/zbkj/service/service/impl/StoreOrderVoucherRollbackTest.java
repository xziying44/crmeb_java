package com.zbkj.service.service.impl;

import com.zbkj.common.constants.CouponConstants;
import com.zbkj.common.model.coupon.StoreCouponUser;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.service.service.StoreCouponUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * H4：取消/超时/退款订单时应回滚（退还）代金券。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StoreOrderVoucherRollbackTest {

    @Mock
    private StoreCouponUserService couponUserService;

    @InjectMocks
    private StoreOrderTaskServiceImpl taskService;

    @Test
    void rollbackVoucher_有代金券时恢复为可用并持久化() {
        StoreOrder order = new StoreOrder();
        order.setVoucherId(5);
        StoreCouponUser voucher = new StoreCouponUser();
        voucher.setId(5);
        voucher.setStatus(1); // 已使用
        when(couponUserService.getById(5)).thenReturn(voucher);

        taskService.rollbackVoucher(order);

        ArgumentCaptor<StoreCouponUser> captor = ArgumentCaptor.forClass(StoreCouponUser.class);
        verify(couponUserService).updateById(captor.capture());
        assertEquals(CouponConstants.STORE_COUPON_USER_STATUS_USABLE, captor.getValue().getStatus(),
                "代金券状态应被恢复为可用");
    }

    @Test
    void rollbackVoucher_无代金券时不做任何操作() {
        StoreOrder order = new StoreOrder();
        order.setVoucherId(0);

        taskService.rollbackVoucher(order);

        verify(couponUserService, never()).updateById(any());
    }
}
