# App 端代金券功能实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 补齐 App 移动端代金券展示、选择和购买功能，实现优惠券与代金券的叠加使用。

**Architecture:** 新增 VoucherController 和 VoucherService 处理代金券购买逻辑，生成虚拟订单复用现有支付流程。前端新增代金券选择弹窗组件、改造结算页双入口、新增购买页面。

**Tech Stack:** SpringBoot 2.2.6 + MyBatis-Plus (后端) / uni-app + Vue 2.x (前端)

---

## Task 1: 新增代金券订单类型常量

**Files:**
- Modify: `crmeb/crmeb-common/src/main/java/com/zbkj/common/constants/Constants.java`

**Step 1: 添加代金券订单类型常量**

在 `Constants.java` 文件末尾添加代金券相关常量：

```java
    // ========== 代金券相关常量 ==========
    /** 订单类型：代金券购买订单 */
    public static final Integer ORDER_TYPE_VOUCHER_BUY = 6;

    /** 优惠券类型：代金券 */
    public static final Integer COUPON_TYPE_VOUCHER = 2;

    /** 优惠券类型：普通优惠券 */
    public static final Integer COUPON_TYPE_COUPON = 1;
```

**Step 2: 验证编译通过**

Run: `cd crmeb && mvn compile -pl crmeb-common -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/constants/Constants.java
git commit -m "$(cat <<'EOF'
添加代金券相关常量

- ORDER_TYPE_VOUCHER_BUY = 6 代金券购买订单类型
- COUPON_TYPE_VOUCHER = 2 代金券类型标识
- COUPON_TYPE_COUPON = 1 普通优惠券类型标识
EOF
)"
```

---

## Task 2: 新增代金券购买请求/响应对象

**Files:**
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/VoucherBuyRequest.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/response/VoucherBuyResponse.java`

**Step 1: 创建购买请求对象**

```java
package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 代金券购买请求对象
 */
@Data
@ApiModel(value = "VoucherBuyRequest", description = "代金券购买请求对象")
public class VoucherBuyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "代金券ID", required = true)
    @NotNull(message = "代金券ID不能为空")
    private Integer couponId;

    @ApiModelProperty(value = "支付方式: weixin-微信支付, yue-余额支付", required = true)
    @NotNull(message = "支付方式不能为空")
    private String payType;
}
```

**Step 2: 创建购买响应对象**

```java
package com.zbkj.common.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 代金券购买响应对象
 */
@Data
@ApiModel(value = "VoucherBuyResponse", description = "代金券购买响应对象")
public class VoucherBuyResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "订单号")
    private String orderNo;

    @ApiModelProperty(value = "支付金额")
    private BigDecimal payPrice;

    @ApiModelProperty(value = "代金券面值")
    private BigDecimal voucherMoney;

    @ApiModelProperty(value = "代金券名称")
    private String voucherName;
}
```

**Step 3: 验证编译通过**

Run: `cd crmeb && mvn compile -pl crmeb-common -q`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/request/VoucherBuyRequest.java
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/response/VoucherBuyResponse.java
git commit -m "$(cat <<'EOF'
添加代金券购买请求和响应对象

- VoucherBuyRequest: 购买请求，包含代金券ID和支付方式
- VoucherBuyResponse: 购买响应，包含订单号和支付信息
EOF
)"
```

---

## Task 3: 新增 VoucherService 接口

**Files:**
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/VoucherService.java`

**Step 1: 创建 Service 接口**

```java
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
```

**Step 2: 验证编译通过**

Run: `cd crmeb && mvn compile -pl crmeb-service -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/VoucherService.java
git commit -m "$(cat <<'EOF'
添加 VoucherService 接口定义

- getBuyList: 获取可购买代金券列表
- buy: 购买代金券生成虚拟订单
- getMine: 获取用户代金券列表
- getOrderVouchers: 获取订单可用代金券
- paySuccessCallback: 支付成功回调处理
EOF
)"
```

---

## Task 4: 实现 VoucherServiceImpl - 获取列表方法

**Files:**
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/VoucherServiceImpl.java`

**Step 1: 创建 ServiceImpl 基础结构和列表方法**

```java
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

    // buy() 和 paySuccessCallback() 将在下一个 Task 中实现
    @Override
    public VoucherBuyResponse buy(VoucherBuyRequest request) {
        // TODO: 在 Task 5 中实现
        throw new CrmebException("功能开发中");
    }

    @Override
    public Boolean paySuccessCallback(String orderNo) {
        // TODO: 在 Task 6 中实现
        throw new CrmebException("功能开发中");
    }
}
```

**Step 2: 验证编译通过**

Run: `cd crmeb && mvn compile -pl crmeb-service -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/VoucherServiceImpl.java
git commit -m "$(cat <<'EOF'
实现 VoucherServiceImpl 列表查询方法

- getBuyList: 获取可购买代金券（canBuy=true, 有库存）
- getMine: 获取用户代金券（按状态筛选）
- getOrderVouchers: 获取订单可用代金券
EOF
)"
```

---

## Task 5: 实现 VoucherServiceImpl - 购买方法

**Files:**
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/VoucherServiceImpl.java`

**Step 1: 实现 buy() 方法**

替换 `buy()` 方法的 TODO 实现：

```java
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
        storeOrder.setFreightFee(BigDecimal.ZERO);
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
        storeOrder.setDeliverySn("");
        storeOrder.setDeliveryType("");
        storeOrder.setDeliveryId("");
        storeOrder.setGainIntegral(0);
        storeOrder.setUseIntegral(0);
        storeOrder.setBackIntegral(0);
        storeOrder.setMark("代金券购买");
        storeOrder.setIsDel(false);
        storeOrder.setUnique(String.valueOf(request.getCouponId())); // 存储代金券ID
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

        if (!result) {
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
```

**Step 2: 验证编译通过**

Run: `cd crmeb && mvn compile -pl crmeb-service -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/VoucherServiceImpl.java
git commit -m "$(cat <<'EOF'
实现代金券购买方法

- 校验代金券有效性（类型、状态、库存）
- 生成虚拟订单（type=6 代金券购买订单）
- 事务处理：扣减库存 + 保存订单
- 返回订单号供前端调用支付接口
EOF
)"
```

---

## Task 6: 实现 VoucherServiceImpl - 支付回调处理

**Files:**
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/VoucherServiceImpl.java`

**Step 1: 实现 paySuccessCallback() 方法**

替换 `paySuccessCallback()` 方法的 TODO 实现：

```java
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

        // 2. 获取代金券ID（存储在 unique 字段）
        Integer couponId;
        try {
            couponId = Integer.parseInt(order.getUnique());
        } catch (NumberFormatException e) {
            logger.error("代金券支付回调：代金券ID解析失败, orderNo={}, unique={}", orderNo, order.getUnique());
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
```

**Step 2: 验证编译通过**

Run: `cd crmeb && mvn compile -pl crmeb-service -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/VoucherServiceImpl.java
git commit -m "$(cat <<'EOF'
实现代金券支付成功回调处理

- 从订单获取代金券ID
- 创建用户代金券记录（type=buy）
- 计算有效期（固定时间或按天）
- 事务保证数据一致性
EOF
)"
```

---

## Task 7: 扩展支付成功处理调用代金券回调

**Files:**
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/OrderPayServiceImpl.java`

**Step 1: 注入 VoucherService**

在 OrderPayServiceImpl 的依赖注入区域添加：

```java
    @Autowired
    private VoucherService voucherService;
```

**Step 2: 在 paySuccess() 方法中添加代金券订单处理**

在 `paySuccess()` 方法的事务处理之后（约 L300-340 区域），添加代金券订单判断：

```java
        // 代金券购买订单特殊处理
        if (Constants.ORDER_TYPE_VOUCHER_BUY.equals(storeOrder.getType())) {
            try {
                voucherService.paySuccessCallback(storeOrder.getOrderId());
                logger.info("代金券购买订单支付成功处理完成, orderNo={}", storeOrder.getOrderId());
            } catch (Exception e) {
                logger.error("代金券购买订单支付成功处理异常, orderNo={}, error={}",
                        storeOrder.getOrderId(), e.getMessage(), e);
            }
            // 代金券订单不需要后续商品订单处理，直接返回
            return true;
        }
```

**Step 3: 验证编译通过**

Run: `cd crmeb && mvn compile -pl crmeb-service -q`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/OrderPayServiceImpl.java
git commit -m "$(cat <<'EOF'
支付成功处理扩展代金券订单回调

- 判断订单类型为代金券购买订单（type=6）
- 调用 voucherService.paySuccessCallback() 发放代金券
- 代金券订单跳过后续商品订单处理逻辑
EOF
)"
```

---

## Task 8: 新增 VoucherController

**Files:**
- Create: `crmeb/crmeb-front/src/main/java/com/zbkj/front/controller/VoucherController.java`

**Step 1: 创建 Controller**

```java
package com.zbkj.front.controller;

import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.VoucherBuyRequest;
import com.zbkj.common.response.CommonResult;
import com.zbkj.common.response.StoreCouponFrontResponse;
import com.zbkj.common.response.StoreCouponUserResponse;
import com.zbkj.common.response.VoucherBuyResponse;
import com.zbkj.service.service.VoucherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 代金券 Controller - 移动端
 */
@Slf4j
@RestController
@RequestMapping("api/front/voucher")
@Api(tags = "代金券")
public class VoucherController {

    @Autowired
    private VoucherService voucherService;

    /**
     * 可购买的代金券列表
     */
    @ApiOperation(value = "可购买的代金券列表")
    @GetMapping("/buyList")
    public CommonResult<List<StoreCouponFrontResponse>> getBuyList() {
        return CommonResult.success(voucherService.getBuyList());
    }

    /**
     * 购买代金券
     */
    @ApiOperation(value = "购买代金券")
    @PostMapping("/buy")
    public CommonResult<VoucherBuyResponse> buy(@RequestBody @Validated VoucherBuyRequest request) {
        return CommonResult.success(voucherService.buy(request));
    }

    /**
     * 我的代金券列表
     */
    @ApiOperation(value = "我的代金券列表")
    @GetMapping("/mine")
    public CommonResult<CommonPage<StoreCouponUserResponse>> getMine(
            @ApiParam(value = "类型：usable-可用, unusable-不可用", required = true)
            @RequestParam String type,
            @Validated PageParamRequest pageParamRequest) {
        return CommonResult.success(voucherService.getMine(type, pageParamRequest));
    }

    /**
     * 订单可用代金券列表
     */
    @ApiOperation(value = "订单可用代金券列表")
    @GetMapping("/order/{preOrderNo}")
    public CommonResult<List<StoreCouponUserResponse>> getOrderVouchers(
            @ApiParam(value = "预下单号", required = true)
            @PathVariable String preOrderNo) {
        return CommonResult.success(voucherService.getOrderVouchers(preOrderNo));
    }
}
```

**Step 2: 验证编译通过**

Run: `cd crmeb && mvn compile -pl crmeb-front -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add crmeb/crmeb-front/src/main/java/com/zbkj/front/controller/VoucherController.java
git commit -m "$(cat <<'EOF'
添加代金券 Controller（移动端）

- GET /buyList: 可购买代金券列表
- POST /buy: 购买代金券
- GET /mine: 我的代金券列表
- GET /order/{preOrderNo}: 订单可用代金券
EOF
)"
```

---

## Task 9: 扩展 StoreCouponUserResponse 添加代金券字段

**Files:**
- Modify: `crmeb/crmeb-common/src/main/java/com/zbkj/common/response/StoreCouponUserResponse.java`

**Step 1: 添加代金券相关字段**

在 StoreCouponUserResponse 类中添加字段：

```java
    @ApiModelProperty(value = "券类型：1-优惠券 2-代金券")
    private Integer couponType;

    @ApiModelProperty(value = "是否可抵扣运费：0-否 1-是")
    private Boolean canDeductFreight;
```

**Step 2: 验证编译通过**

Run: `cd crmeb && mvn compile -pl crmeb-common -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/response/StoreCouponUserResponse.java
git commit -m "$(cat <<'EOF'
扩展 StoreCouponUserResponse 支持代金券字段

- couponType: 区分优惠券(1)和代金券(2)
- canDeductFreight: 是否可抵扣运费
EOF
)"
```

---

## Task 10: App 前端 - 新增代金券 API

**Files:**
- Modify: `app/api/api.js`

**Step 1: 添加代金券 API 接口**

在 `api.js` 文件的优惠券接口区域后添加：

```javascript
// ========== 代金券相关 ==========

/**
 * 可购买的代金券列表
 */
export function getVoucherBuyList() {
  return request.get('voucher/buyList');
}

/**
 * 购买代金券
 * @param {number} couponId 代金券ID
 * @param {string} payType 支付方式: weixin/yue
 */
export function buyVoucher(couponId, payType) {
  return request.post('voucher/buy', { couponId, payType });
}

/**
 * 我的代金券列表
 * @param {string} type 类型: usable/unusable
 * @param {object} data 分页参数
 */
export function getMyVouchers(type, data) {
  return request.get('voucher/mine', { type, ...data });
}

/**
 * 订单可用代金券列表
 * @param {string} preOrderNo 预下单号
 */
export function getOrderVouchers(preOrderNo) {
  return request.get(`voucher/order/${preOrderNo}`);
}
```

**Step 2: Commit**

```bash
git add app/api/api.js
git commit -m "$(cat <<'EOF'
App 前端添加代金券 API 接口

- getVoucherBuyList: 可购买代金券列表
- buyVoucher: 购买代金券
- getMyVouchers: 我的代金券
- getOrderVouchers: 订单可用代金券
EOF
)"
```

---

## Task 11: App 前端 - 新增代金券选择弹窗组件

**Files:**
- Create: `app/components/voucherListWindow/index.vue`

**Step 1: 创建组件目录和文件**

```vue
<template>
  <view class="voucher-list-window" :class="{ on: visible }">
    <!-- 遮罩层 -->
    <view class="mask" @tap="close"></view>

    <!-- 弹窗内容 -->
    <view class="content">
      <!-- 标题栏 -->
      <view class="header">
        <text class="title">选择代金券</text>
        <text class="close-btn" @tap="close">×</text>
      </view>

      <!-- 代金券列表 -->
      <scroll-view scroll-y class="voucher-list" v-if="voucherList.length">
        <view
          class="voucher-item"
          :class="{ selected: selectedId === item.id, disabled: !isUsable(item) }"
          v-for="item in voucherList"
          :key="item.id"
          @tap="selectVoucher(item)"
        >
          <!-- 左侧金额 -->
          <view class="left">
            <view class="money">
              <text class="symbol">¥</text>
              <text class="num">{{ item.money }}</text>
            </view>
            <view class="tag" v-if="item.canDeductFreight">可抵运费</view>
          </view>

          <!-- 右侧信息 -->
          <view class="right">
            <view class="name">{{ item.name }}</view>
            <view class="condition">无门槛</view>
            <view class="date">{{ item.startTime }} - {{ item.endTime }}</view>
          </view>

          <!-- 选中标记 -->
          <view class="check" v-if="selectedId === item.id">
            <text class="iconfont icon-xuanzhong"></text>
          </view>
        </view>
      </scroll-view>

      <!-- 空状态 -->
      <view class="empty" v-else>
        <text>暂无可用代金券</text>
      </view>

      <!-- 不使用代金券 -->
      <view class="no-use" @tap="clearSelection">
        <text>不使用代金券</text>
      </view>

      <!-- 确认按钮 -->
      <view class="confirm-btn" @tap="confirm">确认</view>
    </view>
  </view>
</template>

<script>
import { getOrderVouchers } from '@/api/api.js';

export default {
  name: 'voucherListWindow',
  props: {
    // 是否显示
    visible: {
      type: Boolean,
      default: false
    },
    // 预下单号
    preOrderNo: {
      type: String,
      default: ''
    },
    // 当前选中的代金券ID
    currentVoucherId: {
      type: Number,
      default: 0
    }
  },
  data() {
    return {
      voucherList: [],
      selectedId: 0,
      loading: false
    };
  },
  watch: {
    visible(val) {
      if (val && this.preOrderNo) {
        this.loadVouchers();
      }
    },
    currentVoucherId: {
      immediate: true,
      handler(val) {
        this.selectedId = val || 0;
      }
    }
  },
  methods: {
    // 加载代金券列表
    async loadVouchers() {
      if (this.loading) return;
      this.loading = true;
      try {
        const res = await getOrderVouchers(this.preOrderNo);
        this.voucherList = res.data || [];
      } catch (e) {
        console.error('加载代金券失败', e);
        this.voucherList = [];
      } finally {
        this.loading = false;
      }
    },

    // 判断是否可用
    isUsable(item) {
      return item.status === 0;
    },

    // 选择代金券
    selectVoucher(item) {
      if (!this.isUsable(item)) return;
      this.selectedId = item.id;
    },

    // 清除选择
    clearSelection() {
      this.selectedId = 0;
    },

    // 确认选择
    confirm() {
      const selected = this.voucherList.find(v => v.id === this.selectedId);
      this.$emit('change', {
        voucherId: this.selectedId,
        voucher: selected || null
      });
      this.close();
    },

    // 关闭弹窗
    close() {
      this.$emit('update:visible', false);
      this.$emit('close');
    }
  }
};
</script>

<style lang="scss" scoped>
.voucher-list-window {
  position: fixed;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
  visibility: hidden;

  &.on {
    visibility: visible;

    .mask {
      opacity: 1;
    }

    .content {
      transform: translateY(0);
    }
  }

  .mask {
    position: absolute;
    left: 0;
    top: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    opacity: 0;
    transition: opacity 0.3s;
  }

  .content {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    background: #fff;
    border-radius: 24rpx 24rpx 0 0;
    transform: translateY(100%);
    transition: transform 0.3s;
    max-height: 70vh;
    display: flex;
    flex-direction: column;
  }

  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 30rpx;
    border-bottom: 1px solid #eee;

    .title {
      font-size: 32rpx;
      font-weight: bold;
    }

    .close-btn {
      font-size: 40rpx;
      color: #999;
    }
  }

  .voucher-list {
    flex: 1;
    padding: 20rpx;
    max-height: 50vh;
  }

  .voucher-item {
    display: flex;
    align-items: center;
    padding: 20rpx;
    margin-bottom: 20rpx;
    background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
    border-radius: 16rpx;
    position: relative;

    &.selected {
      box-shadow: 0 0 0 4rpx #52c41a;
    }

    &.disabled {
      opacity: 0.5;
    }

    .left {
      width: 160rpx;
      text-align: center;
      border-right: 1px dashed rgba(255, 255, 255, 0.5);
      padding-right: 20rpx;

      .money {
        color: #fff;

        .symbol {
          font-size: 24rpx;
        }

        .num {
          font-size: 48rpx;
          font-weight: bold;
        }
      }

      .tag {
        font-size: 20rpx;
        color: #fff;
        background: rgba(255, 255, 255, 0.3);
        padding: 4rpx 12rpx;
        border-radius: 20rpx;
        margin-top: 10rpx;
        display: inline-block;
      }
    }

    .right {
      flex: 1;
      padding-left: 20rpx;
      color: #fff;

      .name {
        font-size: 28rpx;
        font-weight: bold;
        margin-bottom: 8rpx;
      }

      .condition {
        font-size: 24rpx;
        opacity: 0.9;
      }

      .date {
        font-size: 22rpx;
        opacity: 0.8;
        margin-top: 8rpx;
      }
    }

    .check {
      position: absolute;
      right: 20rpx;
      top: 50%;
      transform: translateY(-50%);

      .iconfont {
        font-size: 40rpx;
        color: #fff;
      }
    }
  }

  .empty {
    padding: 60rpx;
    text-align: center;
    color: #999;
  }

  .no-use {
    padding: 20rpx 30rpx;
    text-align: center;
    color: #666;
    border-top: 1px solid #eee;
  }

  .confirm-btn {
    margin: 20rpx 30rpx 40rpx;
    background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
    color: #fff;
    text-align: center;
    padding: 24rpx;
    border-radius: 44rpx;
    font-size: 30rpx;
  }
}
</style>
```

**Step 2: Commit**

```bash
git add app/components/voucherListWindow/index.vue
git commit -m "$(cat <<'EOF'
新增代金券选择弹窗组件

- 展示订单可用代金券列表
- 支持单选和不使用选项
- 绿色主题区分优惠券
- 显示可抵运费标签
EOF
)"
```

---

## Task 12: App 前端 - 改造我的优惠券页面

**Files:**
- Modify: `app/pages/users/user_coupon/index.vue`

**Step 1: 添加类型标签展示**

在优惠券列表项模板中添加类型标签：

```vue
<!-- 在 .item 内部添加类型角标 -->
<view class="type-tag" :class="item.couponType === 2 ? 'voucher' : 'coupon'">
  {{ item.couponType === 2 ? '代金券' : '优惠券' }}
</view>
```

**Step 2: 添加样式**

```scss
.type-tag {
  position: absolute;
  top: 0;
  right: 0;
  font-size: 20rpx;
  padding: 4rpx 12rpx;
  border-radius: 0 16rpx 0 16rpx;

  &.coupon {
    background: linear-gradient(135deg, #ff9800 0%, #ffb74d 100%);
    color: #fff;
  }

  &.voucher {
    background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
    color: #fff;
  }
}
```

**Step 3: Commit**

```bash
git add app/pages/users/user_coupon/index.vue
git commit -m "$(cat <<'EOF'
我的优惠券页面添加类型标签

- 优惠券显示橙色角标
- 代金券显示绿色角标
- 混合列表统一展示
EOF
)"
```

---

## Task 13: App 前端 - 改造结算页添加代金券入口

**Files:**
- Modify: `app/pages/order/order_confirm/index.vue`

**Step 1: 引入代金券选择弹窗**

在 script 的 import 区域添加：

```javascript
import voucherListWindow from '@/components/voucherListWindow/index.vue';
```

在 components 中注册：

```javascript
components: {
  // ... 现有组件
  voucherListWindow
}
```

**Step 2: 添加代金券相关数据**

在 data() 中添加：

```javascript
// 代金券相关
voucherVisible: false,
selectedVoucher: null,
voucherId: 0,
voucherFee: 0,
```

**Step 3: 在优惠券入口下方添加代金券入口**

```vue
<!-- 代金券入口 - 在优惠券入口下方添加 -->
<view class='item' @tap='openVoucherWindow'
  v-if="!orderInfoVo.bargainId && !orderInfoVo.combinationId && !orderInfoVo.seckillId && productType==='normal'">
  <view>代金券</view>
  <view class='discount'>
    <text v-if="selectedVoucher">-¥{{ selectedVoucher.money }}</text>
    <text v-else>{{ voucherTitle }}</text>
    <text class='iconfont icon-jiantou'></text>
  </view>
</view>
```

**Step 4: 在价格展示区域添加代金券抵扣行**

```vue
<!-- 代金券抵扣 -->
<view class='item' v-if="voucherFee > 0">
  代金券抵扣：-￥{{ voucherFee }}
</view>
```

**Step 5: 添加代金券选择弹窗组件**

```vue
<!-- 代金券选择弹窗 -->
<voucherListWindow
  :visible.sync="voucherVisible"
  :preOrderNo="preOrderNo"
  :currentVoucherId="voucherId"
  @change="onVoucherChange"
  @close="voucherVisible = false"
/>
```

**Step 6: 添加方法**

```javascript
// 打开代金券选择弹窗
openVoucherWindow() {
  this.voucherVisible = true;
},

// 代金券选择变更
onVoucherChange({ voucherId, voucher }) {
  this.voucherId = voucherId;
  this.selectedVoucher = voucher;
  this.voucherFee = voucher ? voucher.money : 0;
  // 重新计算价格
  this.computedPrice();
},
```

**Step 7: 修改 computedPrice 和提交订单时传递 voucherId**

在调用价格计算和创建订单的请求参数中添加 `voucherId: this.voucherId`

**Step 8: Commit**

```bash
git add app/pages/order/order_confirm/index.vue
git commit -m "$(cat <<'EOF'
结算页添加代金券选择入口

- 优惠券入口下方新增代金券入口
- 集成代金券选择弹窗组件
- 价格区域展示代金券抵扣金额
- 提交订单时传递 voucherId
EOF
)"
```

---

## Task 14: App 前端 - 新增代金券购买页面

**Files:**
- Create: `app/pages/activity/voucher_buy/index.vue`
- Modify: `app/pages.json`

**Step 1: 创建购买页面**

```vue
<template>
  <view class="voucher-buy-page" :data-theme="theme">
    <!-- 导航栏 -->
    <view class="header">
      <text class="title">代金券购买</text>
    </view>

    <!-- 代金券列表 -->
    <view class="voucher-list" v-if="voucherList.length">
      <view class="voucher-card" v-for="item in voucherList" :key="item.id">
        <!-- 左侧面值 -->
        <view class="left">
          <view class="face-value">
            <text class="symbol">¥</text>
            <text class="num">{{ item.money }}</text>
          </view>
          <view class="label">面值</view>
        </view>

        <!-- 中间信息 -->
        <view class="center">
          <view class="name">{{ item.name }}</view>
          <view class="price">售价 ¥{{ item.price }}</view>
          <view class="stock" v-if="item.isLimited">
            {{ item.lastTotal > 0 ? '剩余 ' + item.lastTotal + ' 张' : '已售罄' }}
          </view>
          <view class="tags">
            <text class="tag">无门槛</text>
            <text class="tag" v-if="item.canDeductFreight">可抵运费</text>
          </view>
        </view>

        <!-- 右侧按钮 -->
        <view class="right">
          <view
            class="buy-btn"
            :class="{ disabled: item.isLimited && item.lastTotal <= 0 }"
            @tap="handleBuy(item)"
          >
            {{ item.isLimited && item.lastTotal <= 0 ? '已售罄' : '立即购买' }}
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty" v-else-if="!loading">
      <image src="/static/images/noCart.png" mode="aspectFit"></image>
      <text>暂无可购买的代金券</text>
    </view>

    <!-- 购买确认弹窗 -->
    <uni-popup ref="buyPopup" type="center">
      <view class="buy-confirm-popup">
        <view class="popup-title">确认购买</view>
        <view class="popup-content" v-if="currentVoucher">
          <view>确认花费 <text class="price">¥{{ currentVoucher.price }}</text></view>
          <view>购买面值 <text class="money">¥{{ currentVoucher.money }}</text> 的代金券？</view>
        </view>
        <view class="popup-btns">
          <view class="btn cancel" @tap="cancelBuy">取消</view>
          <view class="btn confirm" @tap="confirmBuy">确认购买</view>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script>
import { getVoucherBuyList, buyVoucher } from '@/api/api.js';
import { mapGetters } from 'vuex';

export default {
  data() {
    return {
      voucherList: [],
      loading: false,
      currentVoucher: null
    };
  },
  computed: {
    ...mapGetters(['theme'])
  },
  onLoad() {
    this.loadList();
  },
  methods: {
    // 加载列表
    async loadList() {
      this.loading = true;
      try {
        const res = await getVoucherBuyList();
        this.voucherList = res.data || [];
      } catch (e) {
        this.$util.Tips({ title: '加载失败' });
      } finally {
        this.loading = false;
      }
    },

    // 点击购买
    handleBuy(item) {
      if (item.isLimited && item.lastTotal <= 0) {
        this.$util.Tips({ title: '该代金券已售罄' });
        return;
      }
      this.currentVoucher = item;
      this.$refs.buyPopup.open();
    },

    // 取消购买
    cancelBuy() {
      this.$refs.buyPopup.close();
      this.currentVoucher = null;
    },

    // 确认购买
    async confirmBuy() {
      if (!this.currentVoucher) return;

      try {
        uni.showLoading({ title: '正在创建订单...' });
        const res = await buyVoucher(this.currentVoucher.id, 'weixin');
        uni.hideLoading();

        this.$refs.buyPopup.close();

        // 跳转支付页
        uni.navigateTo({
          url: `/pages/order/order_payment/index?orderNo=${res.data.orderNo}&payPrice=${res.data.payPrice}`
        });
      } catch (e) {
        uni.hideLoading();
        this.$util.Tips({ title: e.message || '购买失败' });
      }
    }
  }
};
</script>

<style lang="scss" scoped>
.voucher-buy-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 40rpx;
}

.header {
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
  padding: 40rpx 30rpx;

  .title {
    font-size: 36rpx;
    color: #fff;
    font-weight: bold;
  }
}

.voucher-list {
  padding: 20rpx;
}

.voucher-card {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.05);

  .left {
    width: 140rpx;
    text-align: center;
    border-right: 1px dashed #eee;
    padding-right: 20rpx;

    .face-value {
      color: #52c41a;

      .symbol {
        font-size: 24rpx;
      }

      .num {
        font-size: 48rpx;
        font-weight: bold;
      }
    }

    .label {
      font-size: 22rpx;
      color: #999;
      margin-top: 8rpx;
    }
  }

  .center {
    flex: 1;
    padding: 0 20rpx;

    .name {
      font-size: 28rpx;
      font-weight: bold;
      color: #333;
      margin-bottom: 10rpx;
    }

    .price {
      font-size: 30rpx;
      color: #ff4d4f;
      font-weight: bold;
      margin-bottom: 8rpx;
    }

    .stock {
      font-size: 22rpx;
      color: #999;
      margin-bottom: 8rpx;
    }

    .tags {
      display: flex;
      gap: 10rpx;

      .tag {
        font-size: 20rpx;
        color: #52c41a;
        background: rgba(82, 196, 26, 0.1);
        padding: 4rpx 12rpx;
        border-radius: 4rpx;
      }
    }
  }

  .right {
    .buy-btn {
      background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
      color: #fff;
      font-size: 26rpx;
      padding: 16rpx 30rpx;
      border-radius: 30rpx;

      &.disabled {
        background: #ccc;
      }
    }
  }
}

.empty {
  padding: 100rpx 0;
  text-align: center;

  image {
    width: 300rpx;
    height: 300rpx;
  }

  text {
    display: block;
    color: #999;
    font-size: 28rpx;
    margin-top: 20rpx;
  }
}

.buy-confirm-popup {
  background: #fff;
  border-radius: 16rpx;
  padding: 40rpx;
  width: 560rpx;

  .popup-title {
    font-size: 32rpx;
    font-weight: bold;
    text-align: center;
    margin-bottom: 30rpx;
  }

  .popup-content {
    text-align: center;
    font-size: 28rpx;
    color: #666;
    line-height: 1.8;

    .price {
      color: #ff4d4f;
      font-weight: bold;
    }

    .money {
      color: #52c41a;
      font-weight: bold;
    }
  }

  .popup-btns {
    display: flex;
    gap: 20rpx;
    margin-top: 40rpx;

    .btn {
      flex: 1;
      text-align: center;
      padding: 20rpx;
      border-radius: 40rpx;
      font-size: 28rpx;

      &.cancel {
        background: #f5f5f5;
        color: #666;
      }

      &.confirm {
        background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
        color: #fff;
      }
    }
  }
}
</style>
```

**Step 2: 添加路由配置**

在 `app/pages.json` 的 subPackages 中找到 activity 分包，添加：

```json
{
  "path": "voucher_buy/index",
  "style": {
    "navigationBarTitleText": "代金券购买",
    "navigationStyle": "custom"
  }
}
```

**Step 3: Commit**

```bash
git add app/pages/activity/voucher_buy/index.vue
git add app/pages.json
git commit -m "$(cat <<'EOF'
新增代金券购买页面

- 展示可购买代金券列表
- 显示面值、售价、库存
- 购买确认弹窗
- 跳转支付页完成支付
- 配置路由入口
EOF
)"
```

---

## Task 15: 后端整体编译验证

**Files:** 全部后端模块

**Step 1: 全量编译**

Run: `cd crmeb && mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

**Step 2: 如有编译错误，修复后重新编译**

**Step 3: Commit 修复（如有）**

---

## Task 16: 最终集成提交

**Step 1: 查看所有改动**

Run: `git status`

**Step 2: 确认无遗漏文件**

**Step 3: 创建功能分支标记（可选）**

```bash
git tag -a v1.4.1-voucher-app -m "App 端代金券功能完整实现"
```

---

## 实现清单汇总

| Task | 模块 | 文件 | 状态 |
|------|------|------|------|
| 1 | 常量 | Constants.java | 新增常量 |
| 2 | 请求/响应 | VoucherBuyRequest/Response.java | 新增 |
| 3 | Service 接口 | VoucherService.java | 新增 |
| 4-6 | Service 实现 | VoucherServiceImpl.java | 新增 |
| 7 | 支付回调 | OrderPayServiceImpl.java | 修改 |
| 8 | Controller | VoucherController.java | 新增 |
| 9 | Response 扩展 | StoreCouponUserResponse.java | 修改 |
| 10 | App API | api.js | 新增接口 |
| 11 | App 组件 | voucherListWindow/index.vue | 新增 |
| 12 | App 页面 | user_coupon/index.vue | 修改 |
| 13 | App 页面 | order_confirm/index.vue | 修改 |
| 14 | App 页面 | voucher_buy/index.vue | 新增 |

---

*Plan generated by superpowers:writing-plans skill*
