package com.zbkj.service.service.impl;

import com.zbkj.common.vo.OrderInfoDetailVo;
import com.zbkj.service.service.StoreProductService;
import com.zbkj.service.service.promotion.FullReductionProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * M6：指定商品/品类满减必须按"参与活动的商品子总额"计算阈值与减免，
 * 不能用整单金额凑单触发减免（否则未参与商品也会被算进满减门槛，造成超额折扣）。
 * <p>
 * 覆盖私有方法 {@code scopedReductionSubtotal}：仅累加命中该满减活动（商品级或品类级）的明细。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderFullReductionScopeTest {

    @Mock
    private FullReductionProductService fullReductionProductService;
    @Mock
    private StoreProductService storeProductService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private OrderInfoDetailVo detail(int productId, String vipPrice, int payNum) {
        OrderInfoDetailVo d = new OrderInfoDetailVo();
        d.setProductId(productId);
        d.setVipPrice(new BigDecimal(vipPrice));
        d.setPayNum(payNum);
        return d;
    }

    private BigDecimal invokeSubtotal(Integer reductionId, List<OrderInfoDetailVo> list) throws Exception {
        Method m = OrderServiceImpl.class.getDeclaredMethod(
                "scopedReductionSubtotal", Integer.class, List.class);
        m.setAccessible(true);
        return (BigDecimal) m.invoke(orderService, reductionId, list);
    }

    @Test
    void scopedReductionSubtotal_仅累加参与活动的商品_商品级与品类级命中() throws Exception {
        Integer reductionId = 50;

        // 商品1：商品级命中该满减活动
        when(fullReductionProductService.getReductionIdsByProductId(1)).thenReturn(Collections.singletonList(50));
        // 商品2：商品级、品类级均不命中
        when(fullReductionProductService.getReductionIdsByProductId(2)).thenReturn(new ArrayList<>());
        // 商品3：商品级不命中，但其品类命中
        when(fullReductionProductService.getReductionIdsByProductId(3)).thenReturn(new ArrayList<>());

        when(storeProductService.getProductAllCategoryIdByProductIds(Collections.singletonList(1)))
                .thenReturn(Collections.singletonList(7));
        when(storeProductService.getProductAllCategoryIdByProductIds(Collections.singletonList(2)))
                .thenReturn(Collections.singletonList(8));
        when(storeProductService.getProductAllCategoryIdByProductIds(Collections.singletonList(3)))
                .thenReturn(Collections.singletonList(9));

        when(fullReductionProductService.getReductionIdsByCategoryId(7)).thenReturn(new ArrayList<>());
        when(fullReductionProductService.getReductionIdsByCategoryId(8)).thenReturn(new ArrayList<>());
        when(fullReductionProductService.getReductionIdsByCategoryId(9)).thenReturn(Collections.singletonList(50));

        List<OrderInfoDetailVo> list = Arrays.asList(
                detail(1, "100", 2),  // 参与（商品级） → 200
                detail(2, "50", 1),   // 不参与 → 0
                detail(3, "30", 3));  // 参与（品类级） → 90

        BigDecimal subtotal = invokeSubtotal(reductionId, list);
        assertEquals(0, subtotal.compareTo(new BigDecimal("290")),
                "应仅累加参与活动的商品（200+90=290），未参与的商品2(50)必须排除，实际=" + subtotal);
    }

    @Test
    void scopedReductionSubtotal_全部不参与时子总额为零() throws Exception {
        Integer reductionId = 50;

        when(fullReductionProductService.getReductionIdsByProductId(2)).thenReturn(new ArrayList<>());
        when(storeProductService.getProductAllCategoryIdByProductIds(Collections.singletonList(2)))
                .thenReturn(Collections.singletonList(8));
        when(fullReductionProductService.getReductionIdsByCategoryId(8)).thenReturn(new ArrayList<>());

        List<OrderInfoDetailVo> list = Collections.singletonList(detail(2, "50", 1));

        BigDecimal subtotal = invokeSubtotal(reductionId, list);
        assertEquals(0, subtotal.compareTo(BigDecimal.ZERO),
                "无任何商品参与活动时子总额应为 0，实际=" + subtotal);
    }
}
