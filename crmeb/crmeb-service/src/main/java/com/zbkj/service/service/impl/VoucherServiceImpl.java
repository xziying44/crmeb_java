package com.zbkj.service.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.constants.Constants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.coupon.StoreCoupon;
import com.zbkj.common.model.coupon.StoreCouponUser;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.model.user.User;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.VoucherBuyRequest;
import com.zbkj.common.response.StoreCouponFrontResponse;
import com.zbkj.common.response.StoreCouponUserResponse;
import com.zbkj.common.response.VoucherBuyResponse;
import com.zbkj.common.utils.CrmebUtil;
import com.zbkj.service.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 代金券 Service 实现类
 */
@Service
public class VoucherServiceImpl implements VoucherService {

    private static final Logger logger = LoggerFactory.getLogger(VoucherServiceImpl.class);

    @Autowired
    private StoreCouponService storeCouponService;

    @Autowired
    private StoreCouponUserService storeCouponUserService;

    @Autowired
    private StoreOrderService storeOrderService;

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * 获取可购买的代金券列表
     */
    @Override
    public List<StoreCouponFrontResponse> getBuyList() {
        // 查询条件：couponType=2, canBuy=true, status=true, isDel=false, 在有效期内
        LambdaQueryWrapper<StoreCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoreCoupon::getCouponType, Constants.COUPON_TYPE_VOUCHER);
        wrapper.eq(StoreCoupon::getCanBuy, true);
        wrapper.eq(StoreCoupon::getStatus, true);
        wrapper.eq(StoreCoupon::getIsDel, false);
        // 有库存或不限量
        wrapper.and(i -> i.eq(StoreCoupon::getIsLimited, false)
                .or(j -> j.eq(StoreCoupon::getIsLimited, true).gt(StoreCoupon::getLastTotal, 0)));
        wrapper.orderByDesc(StoreCoupon::getSort);
        wrapper.orderByDesc(StoreCoupon::getId);

        List<StoreCoupon> couponList = storeCouponService.list(wrapper);
        if (CollUtil.isEmpty(couponList)) {
            return new ArrayList<>();
        }

        // 转换为前端响应对象
        return couponList.stream().map(coupon -> {
            StoreCouponFrontResponse response = new StoreCouponFrontResponse();
            BeanUtils.copyProperties(coupon, response);
            response.setIsUse(false); // 购买列表不需要判断是否已使用
            return response;
        }).collect(Collectors.toList());
    }

    /**
     * 获取我的代金券列表
     */
    @Override
    public CommonPage<StoreCouponUserResponse> getMine(String type, PageParamRequest pageParamRequest) {
        User user = userService.getInfo();
        if (ObjectUtil.isNull(user)) {
            throw new CrmebException("用户未登录");
        }

        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());

        LambdaQueryWrapper<StoreCouponUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoreCouponUser::getUid, user.getUid());

        // 关联查询代金券类型（couponType=2）
        // 通过 couponId 关联 StoreCoupon 表筛选
        List<Integer> voucherCouponIds = getVoucherCouponIds();
        if (CollUtil.isEmpty(voucherCouponIds)) {
            return CommonPage.restPage(new PageInfo<>(new ArrayList<>()));
        }
        wrapper.in(StoreCouponUser::getCouponId, voucherCouponIds);

        Date now = DateUtil.date();
        if ("usable".equals(type)) {
            // 可用：未使用且未过期
            wrapper.eq(StoreCouponUser::getStatus, 0);
            wrapper.ge(StoreCouponUser::getEndTime, now);
        } else {
            // 不可用：已使用或已过期
            wrapper.and(i -> i.ne(StoreCouponUser::getStatus, 0)
                    .or().lt(StoreCouponUser::getEndTime, now));
        }
        wrapper.orderByDesc(StoreCouponUser::getId);

        List<StoreCouponUser> couponUserList = storeCouponUserService.list(wrapper);
        PageInfo<StoreCouponUser> pageInfo = new PageInfo<>(couponUserList);

        // 转换为响应对象
        List<StoreCouponUserResponse> responseList = couponUserList.stream().map(cu -> {
            StoreCouponUserResponse response = new StoreCouponUserResponse();
            BeanUtils.copyProperties(cu, response);
            // 获取代金券详情
            StoreCoupon coupon = storeCouponService.getById(cu.getCouponId());
            if (coupon != null) {
                response.setCanDeductFreight(coupon.getCanDeductFreight());
                response.setCouponType(coupon.getCouponType());
            }
            return response;
        }).collect(Collectors.toList());

        PageInfo<StoreCouponUserResponse> responsePage = new PageInfo<>();
        BeanUtils.copyProperties(pageInfo, responsePage, "list");
        responsePage.setList(responseList);

        return CommonPage.restPage(responsePage);
    }

    /**
     * 获取所有代金券的 couponId 列表
     */
    private List<Integer> getVoucherCouponIds() {
        LambdaQueryWrapper<StoreCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoreCoupon::getCouponType, Constants.COUPON_TYPE_VOUCHER);
        wrapper.eq(StoreCoupon::getIsDel, false);
        wrapper.select(StoreCoupon::getId);
        List<StoreCoupon> list = storeCouponService.list(wrapper);
        return list.stream().map(StoreCoupon::getId).collect(Collectors.toList());
    }

    /**
     * 获取订单可用的代金券列表
     */
    @Override
    public List<StoreCouponUserResponse> getOrderVouchers(String preOrderNo) {
        User user = userService.getInfo();
        if (ObjectUtil.isNull(user)) {
            throw new CrmebException("用户未登录");
        }

        // 获取用户可用的代金券
        List<Integer> voucherCouponIds = getVoucherCouponIds();
        if (CollUtil.isEmpty(voucherCouponIds)) {
            return new ArrayList<>();
        }

        Date now = DateUtil.date();
        LambdaQueryWrapper<StoreCouponUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoreCouponUser::getUid, user.getUid());
        wrapper.in(StoreCouponUser::getCouponId, voucherCouponIds);
        wrapper.eq(StoreCouponUser::getStatus, 0); // 未使用
        wrapper.ge(StoreCouponUser::getEndTime, now); // 未过期
        wrapper.orderByDesc(StoreCouponUser::getMoney);

        List<StoreCouponUser> couponUserList = storeCouponUserService.list(wrapper);

        return couponUserList.stream().map(cu -> {
            StoreCouponUserResponse response = new StoreCouponUserResponse();
            BeanUtils.copyProperties(cu, response);
            StoreCoupon coupon = storeCouponService.getById(cu.getCouponId());
            if (coupon != null) {
                response.setCanDeductFreight(coupon.getCanDeductFreight());
                response.setCouponType(coupon.getCouponType());
            }
            return response;
        }).collect(Collectors.toList());
    }

    /**
     * 购买代金券
     */
    @Override
    public VoucherBuyResponse buy(VoucherBuyRequest request) {
        User user = userService.getInfo();
        if (ObjectUtil.isNull(user)) {
            throw new CrmebException("用户未登录");
        }

        // 1. 校验代金券有效性
        StoreCoupon coupon = storeCouponService.getById(request.getCouponId());
        if (ObjectUtil.isNull(coupon)) {
            throw new CrmebException("代金券不存在");
        }
        if (!Constants.COUPON_TYPE_VOUCHER.equals(coupon.getCouponType())) {
            throw new CrmebException("该券不是代金券");
        }
        if (!coupon.getCanBuy()) {
            throw new CrmebException("该代金券不支持购买");
        }
        if (!coupon.getStatus()) {
            throw new CrmebException("该代金券已下架");
        }
        if (coupon.getIsDel()) {
            throw new CrmebException("该代金券已删除");
        }
        // 检查库存
        if (coupon.getIsLimited() && coupon.getLastTotal() <= 0) {
            throw new CrmebException("该代金券已售罄");
        }

        // 2. 生成虚拟订单
        String orderNo = CrmebUtil.getOrderNo("voucher");
        BigDecimal payPrice = coupon.getPrice();
        if (payPrice == null || payPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CrmebException("代金券售价配置异常");
        }

        StoreOrder storeOrder = new StoreOrder();
        storeOrder.setOrderId(orderNo);
        storeOrder.setUid(user.getUid());
        storeOrder.setRealName(user.getNickname());
        storeOrder.setUserPhone(user.getPhone());
        storeOrder.setUserAddress("");
        storeOrder.setTotalNum(1);
        storeOrder.setTotalPrice(payPrice);
        storeOrder.setTotalPostage(BigDecimal.ZERO);
        storeOrder.setCouponId(0);
        storeOrder.setCouponPrice(BigDecimal.ZERO);
        storeOrder.setPayPrice(payPrice);
        storeOrder.setPayPostage(BigDecimal.ZERO);
        storeOrder.setDeductionPrice(BigDecimal.ZERO);
        storeOrder.setFreightPrice(BigDecimal.ZERO);
        storeOrder.setPaid(false);
        storeOrder.setPayType(request.getPayType());
        storeOrder.setPayTime(null);
        storeOrder.setStatus(0); // 待支付
        storeOrder.setRefundStatus(0);
        storeOrder.setRefundReasonWapImg("");
        storeOrder.setRefundReasonWapExplain("");
        storeOrder.setRefundReasonTime(null);
        storeOrder.setRefundReasonWap("");
        storeOrder.setRefundReason("");
        storeOrder.setRefundPrice(BigDecimal.ZERO);
        storeOrder.setDeliveryName("");
        storeOrder.setDeliveryType("");
        storeOrder.setDeliveryId("");
        storeOrder.setGainIntegral(0);
        storeOrder.setUseIntegral(0);
        storeOrder.setBackIntegral(0);
        storeOrder.setMark("VOUCHER_BUY:" + request.getCouponId()); // 存储代金券ID
        storeOrder.setIsDel(false);
        storeOrder.setRemark("购买代金券：" + coupon.getName());
        storeOrder.setCost(BigDecimal.ZERO);
        storeOrder.setIsChannel(0);
        storeOrder.setIsRemind(false);
        storeOrder.setShippingType(0); // 无需物流
        storeOrder.setType(Constants.ORDER_TYPE_VOUCHER_BUY); // 代金券购买订单类型
        storeOrder.setVerifyCode("");
        storeOrder.setStoreId(0);
        storeOrder.setClerkId(0);
        storeOrder.setCreateTime(DateUtil.date());
        storeOrder.setUpdateTime(DateUtil.date());

        // 3. 保存订单
        Boolean result = transactionTemplate.execute(e -> {
            // 扣减库存（如果限量）
            if (coupon.getIsLimited()) {
                boolean deductResult = storeCouponService.deduction(coupon.getId(), 1, true);
                if (!deductResult) {
                    throw new CrmebException("代金券库存不足");
                }
            }
            // 保存订单
            storeOrderService.save(storeOrder);
            return Boolean.TRUE;
        });

        if (Boolean.FALSE.equals(result)) {
            throw new CrmebException("订单创建失败");
        }

        // 4. 返回订单信息
        VoucherBuyResponse response = new VoucherBuyResponse();
        response.setOrderNo(orderNo);
        response.setPayPrice(payPrice);
        response.setVoucherMoney(coupon.getMoney());
        response.setVoucherName(coupon.getName());

        logger.info("代金券购买订单创建成功，订单号: {}, 用户: {}, 代金券: {}",
                orderNo, user.getUid(), coupon.getName());

        return response;
    }

    /**
     * 代金券购买订单支付成功回调处理
     */
    @Override
    public Boolean paySuccessCallback(String orderNo) {
        // 1. 查询订单
        StoreOrder order = storeOrderService.getByOderId(orderNo);
        if (ObjectUtil.isNull(order)) {
            logger.error("代金券支付回调：订单不存在, orderNo={}", orderNo);
            return false;
        }
        if (!Constants.ORDER_TYPE_VOUCHER_BUY.equals(order.getType())) {
            logger.warn("代金券支付回调：订单类型不匹配, orderNo={}, type={}", orderNo, order.getType());
            return false;
        }

        // 2. 获取代金券ID（存储在 mark 字段，格式：VOUCHER_BUY:couponId）
        Integer couponId;
        try {
            String mark = order.getMark();
            if (StrUtil.isBlank(mark) || !mark.startsWith("VOUCHER_BUY:")) {
                logger.error("代金券支付回调：mark格式不正确, orderNo={}, mark={}", orderNo, mark);
                return false;
            }
            couponId = Integer.parseInt(mark.substring("VOUCHER_BUY:".length()));
        } catch (NumberFormatException e) {
            logger.error("代金券支付回调：代金券ID解析失败, orderNo={}, mark={}", orderNo, order.getMark());
            return false;
        }

        // 3. 查询代金券
        StoreCoupon coupon = storeCouponService.getById(couponId);
        if (ObjectUtil.isNull(coupon)) {
            logger.error("代金券支付回调：代金券不存在, couponId={}", couponId);
            return false;
        }

        // 4. 发放代金券给用户
        return transactionTemplate.execute(e -> {
            try {
                // 创建用户代金券记录
                StoreCouponUser couponUser = new StoreCouponUser();
                couponUser.setCouponId(couponId);
                couponUser.setUid(order.getUid());
                couponUser.setName(coupon.getName());
                couponUser.setMoney(coupon.getMoney());
                couponUser.setMinPrice(coupon.getMinPrice());
                couponUser.setStatus(0); // 未使用
                couponUser.setType("buy"); // 购买获得

                // 计算有效期
                Date now = DateUtil.date();
                if (coupon.getIsFixedTime()) {
                    // 固定时间范围
                    couponUser.setStartTime(coupon.getUseStartTime());
                    couponUser.setEndTime(coupon.getUseEndTime());
                } else {
                    // 按天计算
                    couponUser.setStartTime(now);
                    couponUser.setEndTime(DateUtil.offsetDay(now, coupon.getDay()));
                }
                couponUser.setCreateTime(now);

                // 保存用户代金券
                storeCouponUserService.save(couponUser);

                logger.info("代金券发放成功，订单号: {}, 用户: {}, 代金券: {}",
                        orderNo, order.getUid(), coupon.getName());

                return Boolean.TRUE;
            } catch (Exception ex) {
                logger.error("代金券发放失败，订单号: {}, 错误: {}", orderNo, ex.getMessage(), ex);
                throw new CrmebException("代金券发放失败");
            }
        });
    }
}
