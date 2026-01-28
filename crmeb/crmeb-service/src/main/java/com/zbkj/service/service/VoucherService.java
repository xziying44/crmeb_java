package com.zbkj.service.service;

import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.VoucherBuyRequest;
import com.zbkj.common.response.StoreCouponFrontResponse;
import com.zbkj.common.response.StoreCouponUserResponse;
import com.zbkj.common.response.VoucherBuyResponse;

import java.util.List;

/**
 * 代金券 Service 接口
 */
public interface VoucherService {

    /**
     * 获取可购买的代金券列表
     * @return 代金券列表
     */
    List<StoreCouponFrontResponse> getBuyList();

    /**
     * 购买代金券
     * @param request 购买请求
     * @return 购买结果（订单信息）
     */
    VoucherBuyResponse buy(VoucherBuyRequest request);

    /**
     * 获取我的代金券列表
     * @param type 类型：usable-可用, unusable-不可用
     * @param pageParamRequest 分页参数
     * @return 代金券列表
     */
    CommonPage<StoreCouponUserResponse> getMine(String type, PageParamRequest pageParamRequest);

    /**
     * 获取订单可用的代金券列表
     * @param preOrderNo 预下单号
     * @return 可用代金券列表
     */
    List<StoreCouponUserResponse> getOrderVouchers(String preOrderNo);

    /**
     * 代金券购买订单支付成功回调处理
     * @param orderNo 订单号
     * @return 是否处理成功
     */
    Boolean paySuccessCallback(String orderNo);
}
