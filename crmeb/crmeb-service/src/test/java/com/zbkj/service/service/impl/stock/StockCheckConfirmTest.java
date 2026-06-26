package com.zbkj.service.service.impl.stock;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.zbkj.common.constants.StockConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.stock.StockCheck;
import com.zbkj.common.model.stock.StockCheckItem;
import com.zbkj.service.dao.StockCheckDao;
import com.zbkj.service.dao.StockCheckItemDao;
import com.zbkj.service.service.stock.StockService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 库存盘点确认（confirm）的单元测试。
 * H5：以当前实际库存为基准校正，而非创建时快照。
 * M7：并发/重复确认时原子抢占失败应拒绝，不重复调整库存。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockCheckConfirmTest {

    @Mock
    private StockCheckDao stockCheckDao;
    @Mock
    private StockCheckItemDao stockCheckItemDao;
    @Mock
    private StockService stockService;

    @InjectMocks
    private StockCheckServiceImpl service;

    @BeforeAll
    static void initMpLambdaCache() {
        // confirm() 内部用 LambdaQueryWrapper/lambdaUpdate 构造条件，需初始化 MyBatis-Plus 的列元数据缓存
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, StockCheck.class);
        TableInfoHelper.initTableInfo(assistant, StockCheckItem.class);
    }

    private StockCheck checkingOrder() {
        StockCheck check = new StockCheck();
        check.setId(1);
        check.setCheckNo("CK001");
        check.setStatus(StockConstants.CHECK_STATUS_CHECKING);
        return check;
    }

    private StockCheckItem item(int system, int actual) {
        StockCheckItem item = new StockCheckItem();
        item.setId(11);
        item.setCheckId(1);
        item.setProductId(1);
        item.setAttrValueId(0);
        item.setSystemStock(system);
        item.setActualStock(actual);
        return item;
    }

    @Test
    void confirm_按当前实际库存校正而非创建时快照() {
        when(stockCheckDao.selectById(1)).thenReturn(checkingOrder());
        when(stockCheckItemDao.selectList(any())).thenReturn(Collections.singletonList(item(10, 8)));
        when(stockCheckDao.update(isNull(), any())).thenReturn(1); // 抢占成功
        when(stockService.getCurrentStock(1, 0)).thenReturn(5);    // 盘点期间库存已从 10 降到 5
        when(stockService.stockIn(anyInt(), anyInt(), anyInt(), anyInt(), any(), anyLong(), any())).thenReturn(true);

        service.confirm(1);

        // 应按 实盘8 - 当前5 = +3 入库，而非 实盘8 - 快照10 = -2 出库
        verify(stockService).stockIn(eq(1), eq(0), eq(3), anyInt(), any(), anyLong(), any());
        verify(stockService, never()).stockOut(anyInt(), anyInt(), anyInt(), anyInt(), any(), anyLong(), any());
    }

    @Test
    void confirm_并发或重复确认抢占失败时拒绝并不调整库存() {
        when(stockCheckDao.selectById(1)).thenReturn(checkingOrder());
        when(stockCheckItemDao.selectList(any())).thenReturn(Collections.singletonList(item(10, 8)));
        when(stockCheckDao.update(isNull(), any())).thenReturn(0); // 抢占失败（已被并发确认）

        CrmebException ex = assertThrows(CrmebException.class, () -> service.confirm(1));
        assertTrue(ex.getMessage() != null && ex.getMessage().contains("重复确认"),
                "应提示请勿重复确认，实际=" + ex.getMessage());
        verify(stockService, never()).stockIn(anyInt(), anyInt(), anyInt(), anyInt(), any(), anyLong(), any());
        verify(stockService, never()).stockOut(anyInt(), anyInt(), anyInt(), anyInt(), any(), anyLong(), any());
    }
}
