# 业务员模块阶段一代码修复计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 修复代码审查发现的 5 个 Important 问题，确保代码质量达到合并标准。

**基于:** `docs/plans/2026-01-21-salesman-phase1-implementation.md` 代码审查报告

---

## 问题清单

| # | 问题 | 严重级别 | 位置 |
|---|------|---------|------|
| 1 | N+1 查询问题（客户统计） | P1 | SalesmanServiceImpl:440-462 |
| 2 | 使用 Random 而非 SecureRandom | P1 | SalesmanInfoServiceImpl:87-93 |
| 3 | 消费统计返回硬编码值 | P2 | SalesmanServiceImpl:334-336 |
| 4 | allList 接口缺少权限控制 | P0 | SalesmanController:149-153 |
| 5 | 邀请码无效时中断登录流程 | P0 | LoginServiceImpl:166-168 |

---

## Task 1: 修复邀请码无效中断登录问题（P0）

**Files:**
- Modify: `crmeb/crmeb-front/src/main/java/com/zbkj/front/service/impl/LoginServiceImpl.java`

### Step 1.1: 修改 bindSalesmanIfNeeded 方法

将抛出异常改为静默跳过并记录日志：

**修改前（约第 166-168 行）:**
```java
if (ObjectUtil.isNull(info) || !Boolean.TRUE.equals(info.getBindable())) {
    throw new CrmebException("邀请码无效");
}
```

**修改后:**
```java
if (ObjectUtil.isNull(info) || !Boolean.TRUE.equals(info.getBindable())) {
    // 邀请码无效时静默跳过，不影响登录流程，仅记录日志
    logger.warn("业务员邀请码无效或不可绑定，跳过绑定: code={}, uid={}", salesmanCode, user.getUid());
    return;
}
```

### Step 1.2: 验证

- 使用无效邀请码进行登录测试
- 预期：登录成功，控制台输出 WARN 日志

---

## Task 2: 修复 allList 接口权限控制（P0）

**Files:**
- Modify: `crmeb/crmeb-admin/src/main/java/com/zbkj/admin/controller/SalesmanController.java`

### Step 2.1: 添加 @PreAuthorize 注解

**修改前（约第 149-153 行）:**
```java
@ApiOperation(value = "获取所有业务员列表（下拉选择用）")
@GetMapping("/allList")
public CommonResult<List<SalesmanResponse>> allList() {
    return CommonResult.success(salesmanService.getAllList());
}
```

**修改后:**
```java
@PreAuthorize("hasAuthority('admin:salesman:list')")
@ApiOperation(value = "获取所有业务员列表（下拉选择用）")
@GetMapping("/allList")
public CommonResult<List<SalesmanResponse>> allList() {
    return CommonResult.success(salesmanService.getAllList());
}
```

### Step 2.2: 验证

- 使用无 `admin:salesman:list` 权限的账号调用接口
- 预期：返回 403 Forbidden

---

## Task 3: 修复 N+1 查询问题（P1）

**Files:**
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/UserService.java`
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/UserServiceImpl.java`
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/SalesmanServiceImpl.java`

### Step 3.1: 在 UserService 接口添加批量统计方法

```java
/**
 * 批量统计业务员客户数量
 * @param salesmanIds 业务员ID列表
 * @return Map<salesmanId, customerCount>
 */
Map<Integer, Integer> countBySalesmanIds(List<Integer> salesmanIds);

/**
 * 批量统计业务员本月新增客户数量
 * @param salesmanIds 业务员ID列表
 * @return Map<salesmanId, monthNewCount>
 */
Map<Integer, Integer> countMonthNewBySalesmanIds(List<Integer> salesmanIds);
```

### Step 3.2: 在 UserServiceImpl 实现批量统计方法

```java
@Override
public Map<Integer, Integer> countBySalesmanIds(List<Integer> salesmanIds) {
    if (CollUtil.isEmpty(salesmanIds)) {
        return new HashMap<>();
    }
    // 使用 MyBatis-Plus 的 selectMaps 进行 GROUP BY 查询
    QueryWrapper<User> wrapper = new QueryWrapper<>();
    wrapper.select("salesman_id", "COUNT(*) as count")
           .in("salesman_id", salesmanIds)
           .groupBy("salesman_id");
    List<Map<String, Object>> results = baseMapper.selectMaps(wrapper);

    Map<Integer, Integer> resultMap = new HashMap<>();
    for (Map<String, Object> row : results) {
        Integer salesmanId = (Integer) row.get("salesman_id");
        Long count = (Long) row.get("count");
        resultMap.put(salesmanId, count.intValue());
    }
    return resultMap;
}

@Override
public Map<Integer, Integer> countMonthNewBySalesmanIds(List<Integer> salesmanIds) {
    if (CollUtil.isEmpty(salesmanIds)) {
        return new HashMap<>();
    }
    // 本月开始时间
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.DAY_OF_MONTH, 1);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    Date monthStart = cal.getTime();

    QueryWrapper<User> wrapper = new QueryWrapper<>();
    wrapper.select("salesman_id", "COUNT(*) as count")
           .in("salesman_id", salesmanIds)
           .ge("salesman_bind_time", monthStart)
           .groupBy("salesman_id");
    List<Map<String, Object>> results = baseMapper.selectMaps(wrapper);

    Map<Integer, Integer> resultMap = new HashMap<>();
    for (Map<String, Object> row : results) {
        Integer salesmanId = (Integer) row.get("salesman_id");
        Long count = (Long) row.get("count");
        resultMap.put(salesmanId, count.intValue());
    }
    return resultMap;
}
```

### Step 3.3: 修改 SalesmanServiceImpl 使用批量查询

**修改 getCustomerCountMap 方法（约第 440-448 行）:**

```java
/**
 * 获取客户数量Map（批量查询优化）
 */
private Map<Integer, Integer> getCustomerCountMap(List<Integer> salesmanIds) {
    if (salesmanIds.isEmpty()) {
        return new HashMap<>();
    }
    // 使用批量 GROUP BY 查询替代 N 次单独查询
    return userService.countBySalesmanIds(salesmanIds);
}
```

**修改 getMonthNewCustomerCountMap 方法（约第 454-462 行）:**

```java
/**
 * 获取本月新增客户数量Map（批量查询优化）
 */
private Map<Integer, Integer> getMonthNewCustomerCountMap(List<Integer> salesmanIds) {
    if (salesmanIds.isEmpty()) {
        return new HashMap<>();
    }
    // 使用批量 GROUP BY 查询替代 N 次单独查询
    return userService.countMonthNewBySalesmanIds(salesmanIds);
}
```

### Step 3.4: 验证

- 业务员列表接口加载 10 条数据
- 预期：SQL 查询数从 21 条降为 3-4 条

---

## Task 4: 修复 Random 改用 SecureRandom（P1）

**Files:**
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/SalesmanInfoServiceImpl.java`

### Step 4.1: 添加 SecureRandom 常量并修改生成方法

**添加导入:**
```java
import java.security.SecureRandom;
```

**添加类常量:**
```java
// 使用 SecureRandom 替代 Random，提高邀请码安全性
private static final SecureRandom SECURE_RANDOM = new SecureRandom();
```

**修改 generateRandomCode 方法（约第 87-93 行）:**

```java
/**
 * 生成随机邀请码（使用安全随机数）
 */
private String generateRandomCode() {
    StringBuilder sb = new StringBuilder(CODE_LENGTH);
    for (int i = 0; i < CODE_LENGTH; i++) {
        sb.append(CODE_CHARS.charAt(SECURE_RANDOM.nextInt(CODE_CHARS.length())));
    }
    return sb.toString();
}
```

### Step 4.2: 验证

- 创建新业务员，检查邀请码生成
- 代码审查确认使用 SecureRandom

---

## Task 5: 实现消费统计查询（P2）

**Files:**
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/StoreOrderService.java`
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/StoreOrderServiceImpl.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/vo/CustomerConsumeStatsVo.java`
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/SalesmanServiceImpl.java`

### Step 5.1: 创建消费统计 VO

**Files:**
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/vo/CustomerConsumeStatsVo.java`

```java
package com.zbkj.common.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 客户消费统计VO
 */
@Data
@ApiModel(value = "CustomerConsumeStatsVo对象", description = "客户消费统计VO")
public class CustomerConsumeStatsVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "用户ID")
    private Integer uid;

    @ApiModelProperty(value = "消费总额")
    private BigDecimal totalAmount;

    @ApiModelProperty(value = "订单数量")
    private Integer orderCount;

    @ApiModelProperty(value = "最近下单时间")
    private Date lastOrderTime;
}
```

### Step 5.2: 在 StoreOrderService 接口添加统计方法

```java
/**
 * 批量获取用户消费统计
 * @param uids 用户ID列表
 * @return Map<uid, CustomerConsumeStatsVo>
 */
Map<Integer, CustomerConsumeStatsVo> getConsumeStatsByUids(List<Integer> uids);
```

### Step 5.3: 在 StoreOrderServiceImpl 实现统计方法

```java
@Override
public Map<Integer, CustomerConsumeStatsVo> getConsumeStatsByUids(List<Integer> uids) {
    if (CollUtil.isEmpty(uids)) {
        return new HashMap<>();
    }

    // 查询已完成订单的消费统计（订单状态：已完成）
    QueryWrapper<StoreOrder> wrapper = new QueryWrapper<>();
    wrapper.select("uid",
                   "SUM(pay_price) as total_amount",
                   "COUNT(*) as order_count",
                   "MAX(create_time) as last_order_time")
           .in("uid", uids)
           .eq("paid", 1)  // 已支付
           .in("status", Arrays.asList(2, 3))  // 已收货或已完成
           .eq("is_del", 0)
           .groupBy("uid");

    List<Map<String, Object>> results = baseMapper.selectMaps(wrapper);

    Map<Integer, CustomerConsumeStatsVo> resultMap = new HashMap<>();
    for (Map<String, Object> row : results) {
        CustomerConsumeStatsVo stats = new CustomerConsumeStatsVo();
        stats.setUid(((Number) row.get("uid")).intValue());
        stats.setTotalAmount((BigDecimal) row.get("total_amount"));
        stats.setOrderCount(((Number) row.get("order_count")).intValue());
        stats.setLastOrderTime((Date) row.get("last_order_time"));
        resultMap.put(stats.getUid(), stats);
    }
    return resultMap;
}
```

### Step 5.4: 修改 SalesmanServiceImpl 的 getBindList 方法

**修改约第 334-336 行的 TODO 部分:**

```java
// 获取用户消费统计（批量查询）
List<Integer> userIds = userList.stream().map(User::getUid).collect(Collectors.toList());
Map<Integer, CustomerConsumeStatsVo> consumeStatsMap = storeOrderService.getConsumeStatsByUids(userIds);

// 组装响应
List<CustomerBindRecordVo> voList = userList.stream().map(user -> {
    CustomerBindRecordVo vo = new CustomerBindRecordVo();
    vo.setUid(user.getUid());
    vo.setNickname(user.getNickname());
    vo.setPhone(user.getPhone());
    vo.setAvatar(user.getAvatar());
    vo.setSalesmanId(user.getSalesmanId());
    vo.setBindTime(user.getSalesmanBindTime());

    SystemAdmin admin = adminMap.get(user.getSalesmanId());
    if (admin != null) {
        vo.setSalesmanName(admin.getRealName());
    }

    // 填充消费统计
    CustomerConsumeStatsVo stats = consumeStatsMap.get(user.getUid());
    if (stats != null) {
        vo.setTotalAmount(stats.getTotalAmount());
        vo.setOrderCount(stats.getOrderCount());
        vo.setLastOrderTime(stats.getLastOrderTime());
    } else {
        vo.setTotalAmount(BigDecimal.ZERO);
        vo.setOrderCount(0);
    }

    return vo;
}).collect(Collectors.toList());
```

### Step 5.5: 添加 StoreOrderService 依赖注入

在 SalesmanServiceImpl 中添加：

```java
@Autowired
private StoreOrderService storeOrderService;
```

### Step 5.6: 验证

- 查看客户绑定记录列表
- 预期：显示真实的消费金额和订单数量

---

## Task 6: 编译验证

### Step 6.1: 编译全部模块

Run: `cd crmeb && mvn clean compile -DskipTests`

Expected: BUILD SUCCESS

### Step 6.2: 启动服务测试

Run: `cd crmeb/crmeb-admin && mvn spring-boot:run`

Expected: 服务正常启动，无报错

---

## 验证清单

| Fix | 验证方式 | 预期结果 | 状态 |
|-----|---------|---------|------|
| #1 邀请码无效不中断登录 | 使用无效邀请码登录 | 登录成功，日志记录 WARN | ⬜ |
| #2 allList 权限控制 | 无权限用户调用接口 | 返回 403 Forbidden | ⬜ |
| #3 N+1 查询优化 | 列表接口 SQL 日志 | 查询数从 N+1 降为 2-3 条 | ⬜ |
| #4 SecureRandom | 代码审查 | 使用安全随机数生成器 | ⬜ |
| #5 消费统计 | 绑定记录列表页面 | 显示真实消费金额和订单数 | ⬜ |

---

## Commit 信息

完成所有修复后，建议的提交信息：

```
fix(salesman): 修复代码审查发现的问题

- fix: 邀请码无效时不中断登录流程，改为静默跳过并记录日志
- fix: allList 接口添加权限控制 @PreAuthorize
- perf: 优化 N+1 查询问题，使用批量 GROUP BY 查询
- security: 邀请码生成改用 SecureRandom 替代 Random
- feat: 实现客户消费统计查询功能
```

---

*本计划基于代码审查报告生成，修复范围：5 个 Important 问题*
