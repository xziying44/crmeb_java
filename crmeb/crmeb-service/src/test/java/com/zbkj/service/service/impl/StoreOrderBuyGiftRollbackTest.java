package com.zbkj.service.service.impl;

import com.zbkj.common.constants.Constants;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.model.order.StoreOrderInfo;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.product.StoreProductAttrValue;
import com.zbkj.service.service.StoreOrderInfoService;
import com.zbkj.service.service.StoreProductAttrValueService;
import com.zbkj.service.service.StoreProductService;
import com.zbkj.service.service.promotion.BuyGiftRecordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 买赠 M5 回滚相关：
 *  - M5.2：取消/退款回滚库存时，赠品可能无规格(attrValueId=0)或规格已删除，
 *          不能直接对 null 的规格对象取值，否则 NPE 致整单回滚失败、取消/退款受阻。
 *  - M5.3：取消/超时/退款时应删除该订单的买赠参与记录，避免已撤销订单仍占用用户参与次数。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StoreOrderBuyGiftRollbackTest {

    @Mock
    private StoreOrderInfoService storeOrderInfoService;
    @Mock
    private StoreProductService storeProductService;
    @Mock
    private StoreProductAttrValueService attrValueService;
    @Mock
    private BuyGiftRecordService buyGiftRecordService;

    @InjectMocks
    private StoreOrderTaskServiceImpl taskService;

    private StoreOrderInfo info(int productId, int attrValueId, int payNum, boolean isGift) {
        StoreOrderInfo i = new StoreOrderInfo();
        i.setProductId(productId);
        i.setAttrValueId(attrValueId);
        i.setPayNum(payNum);
        i.setIsGift(isGift);
        return i;
    }

    private StoreProduct product(int id) {
        StoreProduct p = new StoreProduct();
        p.setId(id);
        p.setVersion(0);
        return p;
    }

    private StoreProductAttrValue attrValue(int id) {
        StoreProductAttrValue a = new StoreProductAttrValue();
        a.setId(id);
        a.setVersion(0);
        return a;
    }

    private boolean invokeRollbackStock(StoreOrder order) throws Exception {
        Method m = StoreOrderTaskServiceImpl.class.getDeclaredMethod("rollbackStock", StoreOrder.class);
        m.setAccessible(true);
        return (boolean) m.invoke(taskService, order);
    }

    // ---------- M5.2 ----------

    @Test
    void rollbackStock_赠品无规格时跳过规格库存回滚不抛NPE() throws Exception {
        StoreOrder order = new StoreOrder();
        order.setOrderId("ORD1");
        order.setSeckillId(0);
        order.setBargainId(0);
        order.setCombinationId(0);

        StoreOrderInfo normal = info(1, 5, 2, false);   // 普通商品，有规格
        StoreOrderInfo gift = info(9, 0, 1, true);      // 赠品，无规格(attrValueId=0)

        when(storeOrderInfoService.getListByOrderNo("ORD1")).thenReturn(Arrays.asList(normal, gift));
        when(storeProductService.getById(1)).thenReturn(product(1));
        when(storeProductService.getById(9)).thenReturn(product(9));
        when(attrValueService.getById(5)).thenReturn(attrValue(5));
        when(attrValueService.getById(0)).thenReturn(null); // 无规格：查不到
        when(storeProductService.operationStock(anyInt(), anyInt(), anyString(), anyInt())).thenReturn(true);
        when(attrValueService.operationStock(anyInt(), anyInt(), anyString(), anyInt(), anyInt())).thenReturn(true);

        boolean ok = invokeRollbackStock(order);

        assertTrue(ok, "赠品无规格(attrValueId=0)时回滚库存不应失败");
        // 两个商品的主商品库存都应回滚
        verify(storeProductService).operationStock(eq(1), eq(2), eq("add"), anyInt());
        verify(storeProductService).operationStock(eq(9), eq(1), eq("add"), anyInt());
        // 普通商品的规格库存回滚一次；赠品无规格不应去查/回滚规格库存
        verify(attrValueService).operationStock(eq(5), eq(2), eq("add"), eq(Constants.PRODUCT_TYPE_NORMAL), anyInt());
        verify(attrValueService, never()).getById(0);
    }

    // ---------- M5.3 ----------

    @Test
    void rollbackBuyGiftRecord_删除该订单的买赠参与记录() {
        StoreOrder order = new StoreOrder();
        order.setOrderId("ORD1");

        taskService.rollbackBuyGiftRecord(order);

        verify(buyGiftRecordService).deleteByOrderId("ORD1");
    }

    @Test
    void rollbackBuyGiftRecord_订单号为空时不调用删除() {
        StoreOrder order = new StoreOrder();
        order.setOrderId(null);

        taskService.rollbackBuyGiftRecord(order);

        verify(buyGiftRecordService, never()).deleteByOrderId(any());
    }
}
