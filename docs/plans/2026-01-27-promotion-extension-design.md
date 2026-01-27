# 促销活动扩展设计方案

> 生成时间: 2026-01-27
> 项目版本: CRMEB Java v1.4
> 设计目的: 阶段四 - 促销活动扩展（满减、买赠、代金券）

---

## 一、需求概述

### 1.1 设计决策

| 功能 | 设计选择 | 说明 |
|-----|---------|------|
| **满减活动** | 全场/品类/指定商品 + 阶梯多档位 | 最灵活的范围控制 |
| **买赠活动** | 买N送M + 买A送B | 两种类型都支持 |
| **代金券** | 扩展现有优惠券 | 复用领取/使用逻辑，开发量小 |
| **叠加规则** | 满减可配置 + 优惠券与代金券可叠加 | 灵活且有吸引力 |

### 1.2 功能范围

```
促销活动扩展
├── 满减活动
│   ├── 范围：全场/品类/指定商品
│   ├── 阶梯满减（多档位）
│   └── 可配置是否与优惠券叠加
├── 买赠活动
│   ├── 同商品买N送M（如买10送3）
│   ├── 跨商品买A送B
│   ├── 赠品从商品库存扣减
│   └── 可配置参与限制（不限/总次数/每日）
└── 代金券
    ├── 扩展现有优惠券系统
    ├── 无门槛使用
    ├── 可抵扣运费
    └── 可与优惠券叠加使用
```

---

## 二、数据模型设计

### 2.1 表结构概览

```
┌─────────────────────────────────────────────────────────────┐
│                    促销活动数据模型                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌───────────────────┐       ┌───────────────────┐         │
│  │ eb_full_reduction │       │   eb_buy_gift     │         │
│  │     满减活动主表    │       │   买赠活动主表     │         │
│  └─────────┬─────────┘       └─────────┬─────────┘         │
│            │                           │                    │
│     ┌──────┴──────┐             ┌──────┴──────┐            │
│     ▼             ▼             ▼             ▼            │
│ ┌────────┐  ┌────────────┐ ┌────────────┐ ┌────────────┐  │
│ │ _level │  │  _product  │ │  _product  │ │  _record   │  │
│ │阶梯档位 │  │ 关联商品表  │ │ 关联商品表  │ │ 参与记录表  │  │
│ └────────┘  └────────────┘ └────────────┘ └────────────┘  │
│                                                             │
│  ┌───────────────────┐                                     │
│  │  eb_store_coupon  │  ← 扩展：新增 coupon_type 字段       │
│  │    优惠券/代金券   │                                     │
│  └───────────────────┘                                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 满减活动表

#### eb_full_reduction（满减活动主表）

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | INT | 主键 |
| name | VARCHAR(100) | 活动名称 |
| scope_type | TINYINT | 范围：1-全场 2-品类 3-指定商品 |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| allow_coupon | TINYINT | 是否允许叠加优惠券：0-否 1-是 |
| status | TINYINT | 状态：0-关闭 1-开启 |
| is_del | TINYINT | 是否删除：0-否 1-是 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### eb_full_reduction_level（满减阶梯表）

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | INT | 主键 |
| reduction_id | INT | 满减活动ID |
| full_amount | DECIMAL(10,2) | 满足金额 |
| reduce_amount | DECIMAL(10,2) | 减免金额 |

#### eb_full_reduction_product（满减关联表）

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | INT | 主键 |
| reduction_id | INT | 满减活动ID |
| relation_type | TINYINT | 关联类型：1-品类 2-商品 |
| relation_id | INT | 品类ID 或 商品ID |

### 2.3 买赠活动表

#### eb_buy_gift（买赠活动主表）

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | INT | 主键 |
| name | VARCHAR(100) | 活动名称 |
| gift_type | TINYINT | 类型：1-同商品买N送M 2-跨商品买A送B |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| limit_type | TINYINT | 限制类型：0-不限 1-总次数 2-每日次数 |
| limit_num | INT | 限制次数（limit_type>0 时生效） |
| status | TINYINT | 状态：0-关闭 1-开启 |
| is_del | TINYINT | 是否删除：0-否 1-是 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### eb_buy_gift_product（买赠商品关联表）

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | INT | 主键 |
| gift_id | INT | 买赠活动ID |
| product_id | INT | 商品ID |
| attr_value_id | INT | 规格ID（0=不限规格） |
| product_type | TINYINT | 类型：1-购买商品 2-赠品 |
| buy_num | INT | 购买数量（购买商品时填写） |
| gift_num | INT | 赠送数量（赠品时填写） |

#### eb_buy_gift_record（买赠参与记录）

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | INT | 主键 |
| gift_id | INT | 买赠活动ID |
| uid | INT | 用户ID |
| order_id | VARCHAR(32) | 订单号 |
| create_time | DATETIME | 参与时间 |

### 2.4 代金券扩展

#### eb_store_coupon 新增字段

| 新增字段 | 类型 | 说明 |
|---------|------|------|
| coupon_type | TINYINT | 券类型：1-优惠券（默认） 2-代金券 |
| can_deduct_freight | TINYINT | 是否可抵扣运费：0-否 1-是 |

#### eb_store_order 新增字段

| 新增字段 | 类型 | 说明 |
|---------|------|------|
| voucher_id | INT | 使用的代金券ID（0=未使用） |
| voucher_price | DECIMAL(10,2) | 代金券抵扣金额 |
| full_reduction_id | INT | 满减活动ID（0=未参与） |
| full_reduction_price | DECIMAL(10,2) | 满减金额 |

### 2.5 SQL 建表语句

```sql
-- 满减活动主表
CREATE TABLE eb_full_reduction (
  id INT(11) PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL COMMENT '活动名称',
  scope_type TINYINT(1) NOT NULL DEFAULT 1 COMMENT '范围：1-全场 2-品类 3-指定商品',
  start_time DATETIME NOT NULL COMMENT '开始时间',
  end_time DATETIME NOT NULL COMMENT '结束时间',
  allow_coupon TINYINT(1) DEFAULT 1 COMMENT '是否允许叠加优惠券：0-否 1-是',
  status TINYINT(1) DEFAULT 0 COMMENT '状态：0-关闭 1-开启',
  is_del TINYINT(1) DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='满减活动主表';

-- 满减阶梯表
CREATE TABLE eb_full_reduction_level (
  id INT(11) PRIMARY KEY AUTO_INCREMENT,
  reduction_id INT(11) NOT NULL COMMENT '满减活动ID',
  full_amount DECIMAL(10,2) NOT NULL COMMENT '满足金额',
  reduce_amount DECIMAL(10,2) NOT NULL COMMENT '减免金额',
  KEY idx_reduction_id (reduction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='满减阶梯表';

-- 满减关联表
CREATE TABLE eb_full_reduction_product (
  id INT(11) PRIMARY KEY AUTO_INCREMENT,
  reduction_id INT(11) NOT NULL COMMENT '满减活动ID',
  relation_type TINYINT(1) NOT NULL COMMENT '关联类型：1-品类 2-商品',
  relation_id INT(11) NOT NULL COMMENT '品类ID或商品ID',
  KEY idx_reduction_id (reduction_id),
  KEY idx_relation (relation_type, relation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='满减关联表';

-- 买赠活动主表
CREATE TABLE eb_buy_gift (
  id INT(11) PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL COMMENT '活动名称',
  gift_type TINYINT(1) NOT NULL DEFAULT 1 COMMENT '类型：1-同商品买N送M 2-跨商品买A送B',
  start_time DATETIME NOT NULL COMMENT '开始时间',
  end_time DATETIME NOT NULL COMMENT '结束时间',
  limit_type TINYINT(1) DEFAULT 0 COMMENT '限制类型：0-不限 1-总次数 2-每日次数',
  limit_num INT(11) DEFAULT 0 COMMENT '限制次数',
  status TINYINT(1) DEFAULT 0 COMMENT '状态：0-关闭 1-开启',
  is_del TINYINT(1) DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='买赠活动主表';

-- 买赠商品关联表
CREATE TABLE eb_buy_gift_product (
  id INT(11) PRIMARY KEY AUTO_INCREMENT,
  gift_id INT(11) NOT NULL COMMENT '买赠活动ID',
  product_id INT(11) NOT NULL COMMENT '商品ID',
  attr_value_id INT(11) DEFAULT 0 COMMENT '规格ID（0=不限规格）',
  product_type TINYINT(1) NOT NULL COMMENT '类型：1-购买商品 2-赠品',
  buy_num INT(11) DEFAULT 0 COMMENT '购买数量',
  gift_num INT(11) DEFAULT 0 COMMENT '赠送数量',
  KEY idx_gift_id (gift_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='买赠商品关联表';

-- 买赠参与记录表
CREATE TABLE eb_buy_gift_record (
  id INT(11) PRIMARY KEY AUTO_INCREMENT,
  gift_id INT(11) NOT NULL COMMENT '买赠活动ID',
  uid INT(11) NOT NULL COMMENT '用户ID',
  order_id VARCHAR(32) NOT NULL COMMENT '订单号',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '参与时间',
  KEY idx_gift_uid (gift_id, uid),
  KEY idx_uid_date (uid, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='买赠参与记录表';

-- 扩展优惠券表
ALTER TABLE eb_store_coupon
ADD COLUMN coupon_type TINYINT(1) DEFAULT 1 COMMENT '券类型：1-优惠券 2-代金券' AFTER id,
ADD COLUMN can_deduct_freight TINYINT(1) DEFAULT 0 COMMENT '是否可抵扣运费：0-否 1-是' AFTER min_price;

-- 扩展订单表
ALTER TABLE eb_store_order
ADD COLUMN voucher_id INT(11) DEFAULT 0 COMMENT '代金券ID' AFTER coupon_price,
ADD COLUMN voucher_price DECIMAL(8,2) DEFAULT 0.00 COMMENT '代金券抵扣金额' AFTER voucher_id,
ADD COLUMN full_reduction_id INT(11) DEFAULT 0 COMMENT '满减活动ID' AFTER voucher_price,
ADD COLUMN full_reduction_price DECIMAL(8,2) DEFAULT 0.00 COMMENT '满减金额' AFTER full_reduction_id;
```

---

## 三、业务流程设计

### 3.1 满减活动流程

```
用户下单结算
    │
    ▼
查询用户购物车/订单商品
    │
    ▼
匹配满减活动（按优先级：指定商品 > 品类 > 全场）
    │
    ▼
计算参与活动的商品总金额
    │
    ▼
匹配阶梯档位（取最高满足的档位）
    │
    ▼
返回满减金额，展示给用户
```

**活动匹配优先级：**
1. 指定商品满减（最精确）
2. 品类满减
3. 全场满减（兜底）

### 3.2 买赠活动流程

**同商品买N送M：**
```
用户购买商品A × 10件
    │
    ▼
匹配买赠活动（买10送3）
    │
    ▼
校验用户参与次数限制
    │
    ▼
自动添加赠品：商品A × 3件（价格为0）
    │
    ▼
扣减库存：商品A × 13件
```

**跨商品买A送B：**
```
用户购买商品A × 1件
    │
    ▼
匹配买赠活动（买A送B）
    │
    ▼
校验用户参与次数限制
    │
    ▼
自动添加赠品：商品B × 1件（价格为0）
    │
    ▼
扣减库存：商品A × 1件，商品B × 1件
```

### 3.3 订单价格计算流程

```
商品总价
    │
    ▼
━━━ 满减计算 ━━━
    │ 匹配满减活动
    │ 计算满减金额
    ▼
━━━ 优惠券计算 ━━━
    │ 校验满减活动是否允许叠加
    │ 计算优惠券抵扣（仅抵扣商品）
    ▼
━━━ 代金券计算 ━━━
    │ 优惠券和代金券可同时使用
    │ 代金券可抵扣商品+运费
    ▼
━━━ 积分计算 ━━━
    │ 计算积分抵扣
    ▼
+ 运费
    │
    ▼
= 实付金额
```

**计算示例：**
```
商品总价: 200元
运费: 10元
满减: 满200减30 → -30元
优惠券: 满100减20 → -20元（仅抵扣商品）
代金券: 15元面额 → -15元（可抵扣剩余商品+运费）

计算过程:
① 商品应付 = 200 - 30(满减) - 20(优惠券) = 150元
② 代金券可抵扣 = min(15, 150+10) = 15元
③ 实付 = 150 + 10(运费) - 15(代金券) = 145元
```

### 3.4 代金券抵扣逻辑

```java
// 代金券金额
BigDecimal voucherMoney = coupon.getMoney();
// 商品应付金额（扣除满减和优惠券后）
BigDecimal productPayable = proTotalFee
    .subtract(fullReductionFee)
    .subtract(couponFee);
// 运费
BigDecimal freightFee = orderInfo.getFreightFee();

if (coupon.getCanDeductFreight() == 1) {
    // 代金券可抵扣总金额 = 商品 + 运费
    BigDecimal totalPayable = productPayable.add(freightFee);
    if (voucherMoney.compareTo(totalPayable) >= 0) {
        // 代金券足够抵扣全部
        voucherFee = totalPayable;
    } else {
        voucherFee = voucherMoney;
    }
} else {
    // 不可抵扣运费，只抵扣商品
    voucherFee = voucherMoney.min(productPayable);
}
```

---

## 四、API 接口设计

### 4.1 满减活动接口（Admin 后台）

| 接口 | 方法 | 说明 |
|-----|------|------|
| `/api/admin/promotion/full-reduction/list` | GET | 满减活动列表 |
| `/api/admin/promotion/full-reduction/detail/{id}` | GET | 满减活动详情 |
| `/api/admin/promotion/full-reduction/save` | POST | 新增/编辑满减活动 |
| `/api/admin/promotion/full-reduction/delete/{id}` | POST | 删除满减活动 |
| `/api/admin/promotion/full-reduction/updateStatus` | POST | 开启/关闭活动 |

### 4.2 买赠活动接口（Admin 后台）

| 接口 | 方法 | 说明 |
|-----|------|------|
| `/api/admin/promotion/buy-gift/list` | GET | 买赠活动列表 |
| `/api/admin/promotion/buy-gift/detail/{id}` | GET | 买赠活动详情 |
| `/api/admin/promotion/buy-gift/save` | POST | 新增/编辑买赠活动 |
| `/api/admin/promotion/buy-gift/delete/{id}` | POST | 删除买赠活动 |
| `/api/admin/promotion/buy-gift/updateStatus` | POST | 开启/关闭活动 |

### 4.3 代金券接口（Admin 后台）

| 接口 | 方法 | 说明 |
|-----|------|------|
| `/api/admin/marketing/voucher/list` | GET | 代金券列表 |
| `/api/admin/marketing/voucher/save` | POST | 新增/编辑代金券 |
| `/api/admin/marketing/voucher/delete/{id}` | POST | 删除代金券 |
| `/api/admin/marketing/voucher/send` | POST | 发放代金券给用户 |

### 4.4 移动端接口（Front）

| 接口 | 方法 | 说明 |
|-----|------|------|
| `/api/front/voucher/list` | GET | 可领取的代金券列表 |
| `/api/front/voucher/receive/{id}` | POST | 领取代金券 |
| `/api/front/voucher/mine` | GET | 我的代金券 |
| `/api/front/order/computed` | POST | 订单价格计算（扩展） |

---

## 五、前端设计

### 5.1 Admin 后台菜单结构

```
营销（一级菜单，已有）
├── 优惠券（已有）
│   ├── 优惠券列表
│   └── 代金券列表          → /marketing/voucher    【新增】
├── 促销活动               → 【新增一级菜单】
│   ├── 满减活动            → /promotion/full-reduction
│   └── 买赠活动            → /promotion/buy-gift
├── 秒杀活动（已有）
├── 砍价活动（已有）
└── 拼团活动（已有）
```

### 5.2 核心页面说明

| 页面 | 主要功能 |
|-----|---------|
| **满减活动列表** | 活动列表、搜索、状态筛选、开启/关闭、删除 |
| **满减活动编辑** | 基本信息 + 范围选择（全场/品类/商品）+ 阶梯档位配置 + 叠加设置 |
| **买赠活动列表** | 活动列表、搜索、状态筛选、开启/关闭、删除 |
| **买赠活动编辑** | 基本信息 + 类型选择 + 购买商品/赠品配置 + 限购设置 |
| **代金券列表** | 代金券列表、发放、删除（复用优惠券页面结构） |
| **代金券编辑** | 名称、面额、数量、有效期、是否可抵扣运费 |

### 5.3 移动端页面调整

| 页面 | 调整内容 |
|-----|---------|
| **优惠券中心** | 新增"代金券"Tab，展示可领取的代金券 |
| **我的优惠券** | 新增"代金券"Tab，展示已领取的代金券 |
| **订单结算页** | 优惠券选择区域拆分为"优惠券"+"代金券"两个入口 |
| **商品详情页** | 展示满减活动标签（如"满200减30"） |
| **购物车页面** | 展示买赠活动提示（如"再买2件可获赠1件"） |

### 5.4 结算页交互示例

```
┌─────────────────────────────────────────┐
│ 商品总价                         ¥200.00 │
├─────────────────────────────────────────┤
│ 满减活动    满200减30            -¥30.00 │  ← 自动匹配
├─────────────────────────────────────────┤
│ 优惠券      [去选择 >]                    │
│             已选：满100减20      -¥20.00 │
├─────────────────────────────────────────┤
│ 代金券      [去选择 >]                    │  ← 新增
│             已选：15元代金券     -¥15.00 │
├─────────────────────────────────────────┤
│ 运费                              ¥10.00 │
├─────────────────────────────────────────┤
│ 实付金额                         ¥145.00 │
└─────────────────────────────────────────┘
```

---

## 六、实现规划

### 6.1 开发阶段划分（预计 1-2 周）

| 阶段 | 时间 | 内容 |
|-----|------|------|
| **第1阶段** | Day 1-3 | 满减活动：数据库表、后端CRUD、价格计算集成、前端页面 |
| **第2阶段** | Day 4-6 | 买赠活动：数据库表、后端CRUD、订单赠品逻辑、前端页面 |
| **第3阶段** | Day 7-8 | 代金券：扩展优惠券表、叠加使用逻辑、前端调整 |
| **第4阶段** | Day 9-10 | 移动端适配、联调测试、Bug修复 |

### 6.2 技术实现要点

| 要点 | 说明 |
|-----|------|
| **包结构** | `com.zbkj.service.service.promotion.*` 新建 promotion 子包 |
| **价格计算** | 扩展 `OrderServiceImpl.computedPrice()` 方法，集成满减和代金券 |
| **事务控制** | 买赠活动涉及库存扣减，使用 `@Transactional` 保证原子性 |
| **活动冲突** | 同一商品只能参与一个满减活动（优先级：指定商品 > 品类 > 全场） |
| **缓存策略** | 活动数据缓存到 Redis，减少数据库查询 |

### 6.3 与进销存系统联动

| 场景 | 处理方式 |
|-----|---------|
| 买赠赠品出库 | 调用 `StockService.salesOut()`，流水类型=销售出库 |
| 赠品库存不足 | 下单时校验，不足则提示"赠品库存不足，活动暂停" |
| 退货退赠品 | 赠品随主商品一起退货，库存回滚 |

### 6.4 风险控制

| 风险 | 应对措施 |
|-----|---------|
| 活动叠加计算错误 | 严格按顺序计算：满减 → 优惠券 → 代金券 → 积分 |
| 超卖问题 | 买赠赠品使用乐观锁扣减库存 |
| 活动时间冲突 | 后台创建活动时校验时间范围是否与同商品其他活动重叠 |

### 6.5 测试用例要点

- [ ] 满减：单档满减、阶梯满减、品类满减、指定商品满减
- [ ] 满减：与优惠券叠加（允许/不允许）
- [ ] 买赠：同商品买10送3、跨商品买A送B
- [ ] 买赠：参与次数限制（不限/总次数/每日）
- [ ] 代金券：无门槛使用、抵扣运费
- [ ] 代金券：与优惠券同时使用
- [ ] 综合：满减 + 优惠券 + 代金券 + 积分抵扣

---

## 七、叠加规则汇总

### 7.1 优惠叠加矩阵

| 优惠类型 | 满减活动 | 优惠券 | 代金券 | 积分抵扣 |
|---------|---------|--------|--------|---------|
| **满减活动** | ❌ 互斥 | ⚙️ 可配置 | ✅ 可叠加 | ✅ 可叠加 |
| **优惠券** | ⚙️ 可配置 | ❌ 互斥 | ✅ 可叠加 | ✅ 可叠加 |
| **代金券** | ✅ 可叠加 | ✅ 可叠加 | ❌ 互斥 | ✅ 可叠加 |
| **积分抵扣** | ✅ 可叠加 | ✅ 可叠加 | ✅ 可叠加 | - |

### 7.2 计算顺序

```
1. 会员价（已有）
2. 满减活动（新增）
3. 优惠券（已有）
4. 代金券（新增，可抵扣运费）
5. 积分抵扣（已有）
```

---

*本设计方案基于需求讨论生成，用于指导后续开发实施*
