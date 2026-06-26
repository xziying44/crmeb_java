package com.zbkj.service.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.service.dao.StoreOrderDao;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M2：核销并发/重复提交防护。
 * <p>
 * 待核销订单的"状态校验"与"写入核销状态"必须是单条原子的条件 UPDATE：
 * 仅当订单仍处于待核销（status=0、已支付、未退款）时才更新。
 * 否则两个并发请求会同时通过 status==0 的检查并各自 updateById，造成重复核销、后续奖励重复发放。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StoreOrderVerificationConcurrencyTest {

    @Mock
    private StoreOrderDao dao;

    @InjectMocks
    private StoreOrderVerificationImpl service;

    @BeforeAll
    static void initMpLambdaCache() {
        // claimVerification 内部用 lambdaUpdate 构造条件，需初始化 MyBatis-Plus 的列元数据缓存
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, StoreOrder.class);
    }

    @Test
    void claimVerification_使用带状态守卫的条件更新而非盲目updateById() {
        when(dao.update(any(StoreOrder.class), any())).thenReturn(1);

        boolean ok = service.claimVerification(100, 9);
        assertTrue(ok, "抢占成功（影响 1 行）时应返回 true");

        // 必须走条件 UPDATE（dao.update(entity, wrapper)），而不是无守卫的 updateById
        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(dao).update(any(StoreOrder.class), captor.capture());
        String sql = captor.getValue().getSqlSegment().toLowerCase();
        assertTrue(sql.contains("status"), "更新条件须守卫订单状态(status)，实际 SQL=" + sql);
        assertTrue(sql.contains("paid"), "更新条件须守卫已支付(paid)，实际 SQL=" + sql);
        assertTrue(sql.contains("refund"), "更新条件须守卫未退款(refund_status)，实际 SQL=" + sql);
    }

    @Test
    void claimVerification_并发抢占失败时返回false() {
        when(dao.update(any(StoreOrder.class), any())).thenReturn(0); // 已被并发核销，条件不再匹配

        boolean ok = service.claimVerification(100, 9);
        assertFalse(ok, "抢占失败（影响 0 行）时应返回 false，调用方据此拒绝重复核销");
    }
}
