package com.zbkj.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.UserCouponReceiveRequest;
import com.zbkj.common.vo.MyRecord;
import com.zbkj.common.request.PageParamRequest;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.coupon.StoreCouponUser;
import com.zbkj.common.request.StoreCouponUserRequest;
import com.zbkj.common.request.StoreCouponUserSearchRequest;
import com.zbkj.common.response.StoreCouponUserOrder;
import com.zbkj.common.response.StoreCouponUserResponse;

import java.util.HashMap;
import java.util.List;

/**
 * StoreCouponUserService 接口
 * +----------------------------------------------------------------------
 * | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 * +----------------------------------------------------------------------
 * | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
 * +----------------------------------------------------------------------
 * | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 * +----------------------------------------------------------------------
 * | Author: CRMEB Team <admin@crmeb.com>
 * +----------------------------------------------------------------------
 */
public interface StoreCouponUserService extends IService<StoreCouponUser> {

    /**
     * 优惠券发放记录
     * @param request 查询参数
     * @param pageParamRequest 分页参数
     * @return PageInfo
     */
    PageInfo<StoreCouponUserResponse> getList(StoreCouponUserSearchRequest request, PageParamRequest pageParamRequest);

    /**
     * PC领取优惠券
     * @param storeCouponUserRequest 优惠券参数
     * @return Boolean
     */
    Boolean receive(StoreCouponUserRequest storeCouponUserRequest);

    /**
     * 自动赠送场景批量发券（注册赠券/满额赠券等）：逐券先原子扣减库存
     * （限量券 deduction(true)，非限量 deduction(false) 仅累计），
     * 限量券扣减失败则跳过该券、不阻断主流程（注册/支付回调等绝不能因赠券售罄而回滚），
     * 仅持久化扣减成功的券。
     *
     * @param couponUsers       待发放的券（已设置好 uid/couponId 等）
     * @param limitedByCouponId couponId -&gt; 是否限量
     * @return 实际发放（持久化）的券数量
     */
    int grantCouponsSkipExhausted(List<StoreCouponUser> couponUsers, java.util.Map<Integer, Boolean> limitedByCouponId);

    HashMap<Integer, StoreCouponUser> getMapByUserId(Integer userId);

    /**
     * 当前订单可用优惠券
     * @param preOrderNo 预下单订单号
     * @return 可用优惠券集合
     */
    List<StoreCouponUserOrder> getListByPreOrderNo(String preOrderNo);

    /**
     * 优惠券过期定时任务
     */
    void overdueTask();

    /**
     * 用户领取优惠券
     */
    Boolean receiveCoupon(UserCouponReceiveRequest request);

    /**
     * 支付成功赠送处理
     * @param couponId 优惠券编号
     * @param uid  用户uid
     * @return MyRecord
     */
    MyRecord paySuccessGiveAway(Integer couponId, Integer uid);

    /**
     * 根据uid获取列表
     * @param uid uid
     * @param pageParamRequest 分页参数
     * @return List<StoreCouponUser>
     */
    List<StoreCouponUser> findListByUid(Integer uid, PageParamRequest pageParamRequest);

    /**
     * 获取可用优惠券数量
     * @param uid 用户uid
     */
    Integer getUseCount(Integer uid);

    /**
     * 我的优惠券列表
     * @param type 类型，usable-可用，unusable-不可用
     * @param pageParamRequest 分页参数
     * @return CommonPage<StoreCouponUserResponse>
     */
    CommonPage<StoreCouponUserResponse> getMyCouponList(String type, PageParamRequest pageParamRequest);
}
