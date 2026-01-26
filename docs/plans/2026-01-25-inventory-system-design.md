# 进销存系统设计方案

> 生成时间: 2026-01-25
> 项目版本: CRMEB Java v1.4
> 设计目的: 阶段二 - 进销存系统（基础版）

---

## 一、需求概述

### 1.1 设计决策

| 维度 | 选择 | 说明 |
|-----|------|------|
| 复杂度 | 基础版 | 单仓库模式，满足当前需求 |
| 入库 | 采购入库 + 其他入库 | 支持退货入库、调整入库等 |
| 出库 | 销售自动出库 + 手动出库 | 订单发货自动扣减，支持报损、领用等 |
| 盘点 | 需要 | 盘点单 → 盘盈盘亏 → 自动调整 |
| 权限 | 复用现有 RBAC | 不新增角色，通过菜单权限控制 |
| 报表 | 完整报表 | 库存、流水、汇总、采购统计、盘点差异 |
| 库存架构 | 独立库存表模式 | `eb_stock` 为主，同步到 `eb_store_product.stock` |

### 1.2 功能范围

```
进销存系统（基础版）
├── 供应商管理
│   └── 供应商 CRUD、启用/禁用
├── 采购管理
│   ├── 采购单创建/取消
│   └── 采购入库（支持分批）
├── 库存管理
│   ├── 库存列表（预警筛选）
│   ├── 库存流水查询
│   ├── 手动入库（退货/其他）
│   └── 手动出库（报损/其他）
├── 库存盘点
│   ├── 创建盘点单
│   ├── 录入实际库存
│   └── 确认盘点（自动调整）
├── 销售联动
│   └── 订单发货自动扣减库存
└── 库存报表
    ├── 库存变动汇总
    ├── 采购统计
    └── 盘点差异报表
```

---

## 二、数据模型设计

### 2.1 表结构概览

```
┌─────────────────────────────────────────────────────────────┐
│                      进销存数据模型                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐       ┌─────────────┐                     │
│  │ eb_supplier │       │  eb_stock   │                     │
│  │   供应商表   │       │   库存表    │                     │
│  └─────────────┘       └─────────────┘                     │
│         │                    ▲                              │
│         ▼                    │ 更新                         │
│  ┌─────────────┐       ┌─────────────┐                     │
│  │eb_purchase  │──────►│eb_stock_log │                     │
│  │  采购单表   │       │  库存流水表  │                     │
│  └─────────────┘       └─────────────┘                     │
│         │                    ▲                              │
│         ▼                    │                              │
│  ┌──────────────┐      ┌─────────────┐                     │
│  │eb_purchase   │      │eb_stock     │                     │
│  │   _item      │      │  _check     │                     │
│  │ 采购单明细表 │      │  盘点单表   │                     │
│  └──────────────┘      └─────────────┘                     │
│                              │                              │
│                              ▼                              │
│                        ┌──────────────┐                    │
│                        │eb_stock_check│                    │
│                        │    _item     │                    │
│                        │ 盘点单明细表 │                    │
│                        └──────────────┘                    │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 表字段详细设计

#### eb_supplier（供应商表）

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | INT | 主键 |
| name | VARCHAR(100) | 供应商名称 |
| contact | VARCHAR(50) | 联系人 |
| phone | VARCHAR(20) | 联系电话 |
| address | VARCHAR(255) | 地址 |
| remark | VARCHAR(500) | 备注 |
| status | TINYINT | 状态：0-禁用 1-启用 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### eb_stock（库存表）

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | INT | 主键 |
| product_id | INT | 商品ID |
| attr_value_id | INT | 规格ID（0=无规格） |
| stock | INT | 当前库存数量 |
| warning_stock | INT | 预警库存 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

> **索引**：`product_id + attr_value_id` 唯一索引

#### eb_stock_log（库存流水表）

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | BIGINT | 主键 |
| product_id | INT | 商品ID |
| attr_value_id | INT | 规格ID |
| type | TINYINT | 类型（见下方枚举） |
| quantity | INT | 数量（正=入库，负=出库） |
| before_stock | INT | 变动前库存 |
| after_stock | INT | 变动后库存 |
| relation_id | BIGINT | 关联单据ID |
| relation_type | VARCHAR(20) | 关联单据类型 |
| operator_id | INT | 操作人ID |
| operator_name | VARCHAR(50) | 操作人姓名 |
| remark | VARCHAR(255) | 备注 |
| create_time | DATETIME | 创建时间 |

**流水类型枚举**：
- 1-采购入库
- 2-退货入库
- 3-盘盈入库
- 4-其他入库
- 5-销售出库
- 6-报损出库
- 7-盘亏出库
- 8-其他出库

#### eb_purchase（采购单主表）

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | INT | 主键 |
| purchase_no | VARCHAR(32) | 采购单号（唯一） |
| supplier_id | INT | 供应商ID |
| total_quantity | INT | 商品总数量 |
| total_amount | DECIMAL(10,2) | 采购总金额 |
| status | TINYINT | 状态：0-待入库 1-部分入库 2-已入库 3-已取消 |
| remark | VARCHAR(500) | 备注 |
| operator_id | INT | 创建人ID |
| operator_name | VARCHAR(50) | 创建人姓名 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### eb_purchase_item（采购单明细表）

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | INT | 主键 |
| purchase_id | INT | 采购单ID |
| product_id | INT | 商品ID |
| attr_value_id | INT | 规格ID |
| product_name | VARCHAR(200) | 商品名称（冗余） |
| sku_name | VARCHAR(200) | 规格名称（冗余） |
| quantity | INT | 采购数量 |
| in_quantity | INT | 已入库数量 |
| price | DECIMAL(10,2) | 采购单价 |
| amount | DECIMAL(10,2) | 小计金额 |

#### eb_stock_check（盘点单主表）

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | INT | 主键 |
| check_no | VARCHAR(32) | 盘点单号（唯一） |
| status | TINYINT | 状态：0-盘点中 1-已完成 2-已取消 |
| total_profit | INT | 盘盈总数 |
| total_loss | INT | 盘亏总数 |
| remark | VARCHAR(500) | 备注 |
| operator_id | INT | 创建人ID |
| operator_name | VARCHAR(50) | 创建人姓名 |
| create_time | DATETIME | 创建时间 |
| finish_time | DATETIME | 完成时间 |

#### eb_stock_check_item（盘点单明细表）

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | INT | 主键 |
| check_id | INT | 盘点单ID |
| product_id | INT | 商品ID |
| attr_value_id | INT | 规格ID |
| product_name | VARCHAR(200) | 商品名称 |
| sku_name | VARCHAR(200) | 规格名称 |
| system_stock | INT | 系统库存（盘点时） |
| actual_stock | INT | 实际库存（盘点数） |
| diff_quantity | INT | 差异数量（实际-系统） |

### 2.3 SQL 建表语句

```sql
-- 供应商表
CREATE TABLE eb_supplier (
  id INT(11) PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL COMMENT '供应商名称',
  contact VARCHAR(50) NULL COMMENT '联系人',
  phone VARCHAR(20) NULL COMMENT '联系电话',
  address VARCHAR(255) NULL COMMENT '地址',
  remark VARCHAR(500) NULL COMMENT '备注',
  status TINYINT(1) DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商表';

-- 库存表
CREATE TABLE eb_stock (
  id INT(11) PRIMARY KEY AUTO_INCREMENT,
  product_id INT(11) NOT NULL COMMENT '商品ID',
  attr_value_id INT(11) DEFAULT 0 COMMENT '规格ID（0=无规格）',
  stock INT(11) DEFAULT 0 COMMENT '当前库存数量',
  warning_stock INT(11) DEFAULT 0 COMMENT '预警库存',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_product_attr (product_id, attr_value_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

-- 库存流水表
CREATE TABLE eb_stock_log (
  id BIGINT(20) PRIMARY KEY AUTO_INCREMENT,
  product_id INT(11) NOT NULL COMMENT '商品ID',
  attr_value_id INT(11) DEFAULT 0 COMMENT '规格ID',
  type TINYINT(2) NOT NULL COMMENT '类型：1-采购入库 2-退货入库 3-盘盈入库 4-其他入库 5-销售出库 6-报损出库 7-盘亏出库 8-其他出库',
  quantity INT(11) NOT NULL COMMENT '数量（正=入库，负=出库）',
  before_stock INT(11) NOT NULL COMMENT '变动前库存',
  after_stock INT(11) NOT NULL COMMENT '变动后库存',
  relation_id BIGINT(20) NULL COMMENT '关联单据ID',
  relation_type VARCHAR(20) NULL COMMENT '关联单据类型',
  operator_id INT(11) NULL COMMENT '操作人ID',
  operator_name VARCHAR(50) NULL COMMENT '操作人姓名',
  remark VARCHAR(255) NULL COMMENT '备注',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_product (product_id, attr_value_id),
  KEY idx_type (type),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水表';

-- 采购单主表
CREATE TABLE eb_purchase (
  id INT(11) PRIMARY KEY AUTO_INCREMENT,
  purchase_no VARCHAR(32) NOT NULL COMMENT '采购单号',
  supplier_id INT(11) NOT NULL COMMENT '供应商ID',
  total_quantity INT(11) DEFAULT 0 COMMENT '商品总数量',
  total_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '采购总金额',
  status TINYINT(1) DEFAULT 0 COMMENT '状态：0-待入库 1-部分入库 2-已入库 3-已取消',
  remark VARCHAR(500) NULL COMMENT '备注',
  operator_id INT(11) NULL COMMENT '创建人ID',
  operator_name VARCHAR(50) NULL COMMENT '创建人姓名',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_purchase_no (purchase_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购单表';

-- 采购单明细表
CREATE TABLE eb_purchase_item (
  id INT(11) PRIMARY KEY AUTO_INCREMENT,
  purchase_id INT(11) NOT NULL COMMENT '采购单ID',
  product_id INT(11) NOT NULL COMMENT '商品ID',
  attr_value_id INT(11) DEFAULT 0 COMMENT '规格ID',
  product_name VARCHAR(200) NULL COMMENT '商品名称',
  sku_name VARCHAR(200) NULL COMMENT '规格名称',
  quantity INT(11) NOT NULL COMMENT '采购数量',
  in_quantity INT(11) DEFAULT 0 COMMENT '已入库数量',
  price DECIMAL(10,2) DEFAULT 0.00 COMMENT '采购单价',
  amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '小计金额',
  KEY idx_purchase_id (purchase_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购单明细表';

-- 盘点单主表
CREATE TABLE eb_stock_check (
  id INT(11) PRIMARY KEY AUTO_INCREMENT,
  check_no VARCHAR(32) NOT NULL COMMENT '盘点单号',
  status TINYINT(1) DEFAULT 0 COMMENT '状态：0-盘点中 1-已完成 2-已取消',
  total_profit INT(11) DEFAULT 0 COMMENT '盘盈总数',
  total_loss INT(11) DEFAULT 0 COMMENT '盘亏总数',
  remark VARCHAR(500) NULL COMMENT '备注',
  operator_id INT(11) NULL COMMENT '创建人ID',
  operator_name VARCHAR(50) NULL COMMENT '创建人姓名',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  finish_time DATETIME NULL COMMENT '完成时间',
  UNIQUE KEY uk_check_no (check_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点单表';

-- 盘点单明细表
CREATE TABLE eb_stock_check_item (
  id INT(11) PRIMARY KEY AUTO_INCREMENT,
  check_id INT(11) NOT NULL COMMENT '盘点单ID',
  product_id INT(11) NOT NULL COMMENT '商品ID',
  attr_value_id INT(11) DEFAULT 0 COMMENT '规格ID',
  product_name VARCHAR(200) NULL COMMENT '商品名称',
  sku_name VARCHAR(200) NULL COMMENT '规格名称',
  system_stock INT(11) DEFAULT 0 COMMENT '系统库存',
  actual_stock INT(11) NULL COMMENT '实际库存',
  diff_quantity INT(11) DEFAULT 0 COMMENT '差异数量',
  KEY idx_check_id (check_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点单明细表';
```

---

## 三、业务流程设计

### 3.1 采购入库流程

```
┌─────────────────────────────────────────────────────────────┐
│                      采购入库流程                            │
└─────────────────────────────────────────────────────────────┘

    创建采购单              入库操作                 库存更新
        │                     │                       │
        ▼                     ▼                       ▼
┌───────────────┐     ┌───────────────┐     ┌───────────────┐
│ 1. 选择供应商  │     │ 4. 点击入库    │     │ 6. 更新eb_stock│
│ 2. 添加商品    │────►│ 5. 确认入库数量│────►│ 7. 写入流水    │
│ 3. 保存采购单  │     │   （支持分批） │     │ 8. 同步商品库存│
└───────────────┘     └───────────────┘     └───────────────┘
     状态：待入库           状态：部分入库/已入库
```

**关键点**：
- 支持分批入库（入库数量 ≤ 采购数量 - 已入库数量）
- 入库时自动生成库存流水（类型=采购入库）
- 同步更新 `eb_store_product.stock`

### 3.2 手动出入库流程

```
操作员选择操作类型
        │
        ├── 入库（退货入库/其他入库）
        │       │
        │       ▼
        │   选择商品 → 输入数量 → 确认
        │       │
        │       ▼
        │   eb_stock +数量 → 写入流水 → 同步商品库存
        │
        └── 出库（报损出库/其他出库）
                │
                ▼
            选择商品 → 输入数量 → 校验库存 → 确认
                │
                ▼
            eb_stock -数量 → 写入流水 → 同步商品库存
```

### 3.3 销售出库流程（自动）

```
订单发货触发
      │
      ▼
┌─────────────────────────────────────────┐
│ OrderService.send() 发货方法            │
│      │                                  │
│      ▼                                  │
│ StockService.salesOut(orderItems)       │
│      │                                  │
│      ├── 扣减 eb_stock                  │
│      ├── 写入流水（类型=销售出库）        │
│      └── 同步 eb_store_product.stock    │
└─────────────────────────────────────────┘
```

### 3.4 库存盘点流程

```
┌─────────────────────────────────────────────────────────────┐
│                      库存盘点流程                            │
└─────────────────────────────────────────────────────────────┘

  创建盘点单           录入实际库存           确认盘点
      │                    │                    │
      ▼                    ▼                    ▼
┌────────────┐      ┌────────────┐      ┌────────────────┐
│ 1. 选择商品 │      │ 3. 逐个录入 │      │ 5. 确认完成     │
│   (可全选)  │─────►│   实际库存  │─────►│ 6. 系统自动：   │
│ 2. 生成盘点单│      │ 4. 自动计算 │      │   - 计算盘盈盘亏│
│   状态:盘点中│      │   差异数量  │      │   - 调整库存    │
└────────────┘      └────────────┘      │   - 写入流水    │
                                        │ 7. 状态:已完成  │
                                        └────────────────┘
```

**盘点确认时的库存调整逻辑**：

```java
for (盘点明细 item : 盘点单明细) {
    差异 = item.actual_stock - item.system_stock;

    if (差异 > 0) {
        // 盘盈：实际比系统多，增加库存
        库存 += 差异;
        写入流水(类型=盘盈入库, 数量=+差异);
    } else if (差异 < 0) {
        // 盘亏：实际比系统少，减少库存
        库存 += 差异;  // 差异为负数
        写入流水(类型=盘亏出库, 数量=差异);
    }
    // 差异=0 不处理
}
```

### 3.5 库存同步机制

**核心原则**：`eb_stock` 为主，`eb_store_product.stock` 为从

```
┌─────────────────────────────────────────────────────────────┐
│                    库存同步机制                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   任何库存变动                                               │
│        │                                                    │
│        ▼                                                    │
│   ┌─────────────┐                                          │
│   │ StockService│ ← 统一入口                                │
│   │ .change()   │                                          │
│   └──────┬──────┘                                          │
│          │                                                  │
│          ├── 1. 更新 eb_stock                               │
│          ├── 2. 写入 eb_stock_log                           │
│          └── 3. 同步 eb_store_product.stock                 │
│                   └── 汇总该商品所有规格的库存                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**并发控制**：使用 `UPDATE ... SET stock = stock + ? WHERE stock >= ?` 防止超卖

---

## 四、API 接口设计

### 4.1 供应商管理接口

| 接口 | 方法 | 说明 |
|-----|------|------|
| `/api/admin/stock/supplier/list` | GET | 供应商列表（分页） |
| `/api/admin/stock/supplier/add` | POST | 添加供应商 |
| `/api/admin/stock/supplier/update` | POST | 编辑供应商 |
| `/api/admin/stock/supplier/delete/{id}` | POST | 删除供应商 |
| `/api/admin/stock/supplier/updateStatus` | POST | 启用/禁用 |

### 4.2 采购管理接口

| 接口 | 方法 | 说明 |
|-----|------|------|
| `/api/admin/stock/purchase/list` | GET | 采购单列表 |
| `/api/admin/stock/purchase/detail/{id}` | GET | 采购单详情 |
| `/api/admin/stock/purchase/add` | POST | 创建采购单 |
| `/api/admin/stock/purchase/cancel/{id}` | POST | 取消采购单 |
| `/api/admin/stock/purchase/inStock` | POST | 采购入库 |

### 4.3 库存管理接口

| 接口 | 方法 | 说明 |
|-----|------|------|
| `/api/admin/stock/list` | GET | 库存列表 |
| `/api/admin/stock/log/list` | GET | 库存流水列表 |
| `/api/admin/stock/in` | POST | 手动入库 |
| `/api/admin/stock/out` | POST | 手动出库 |
| `/api/admin/stock/warning/update` | POST | 设置预警库存 |

### 4.4 盘点管理接口

| 接口 | 方法 | 说明 |
|-----|------|------|
| `/api/admin/stock/check/list` | GET | 盘点单列表 |
| `/api/admin/stock/check/detail/{id}` | GET | 盘点单详情 |
| `/api/admin/stock/check/create` | POST | 创建盘点单 |
| `/api/admin/stock/check/updateItem` | POST | 更新盘点数量 |
| `/api/admin/stock/check/confirm/{id}` | POST | 确认盘点完成 |
| `/api/admin/stock/check/cancel/{id}` | POST | 取消盘点 |

### 4.5 报表接口

| 接口 | 方法 | 说明 |
|-----|------|------|
| `/api/admin/stock/report/summary` | GET | 库存变动汇总 |
| `/api/admin/stock/report/purchase` | GET | 采购统计 |
| `/api/admin/stock/report/checkDiff` | GET | 盘点差异报表 |

---

## 五、前端设计

### 5.1 菜单结构

```
库存管理（一级菜单）
├── 库存查询
│   ├── 库存列表          → /stock/list
│   └── 库存流水          → /stock/log
├── 采购管理
│   ├── 供应商管理        → /stock/supplier
│   └── 采购单管理        → /stock/purchase
├── 库存操作
│   ├── 手动入库          → /stock/in
│   ├── 手动出库          → /stock/out
│   └── 库存盘点          → /stock/check
└── 库存报表
    ├── 变动汇总          → /stock/report/summary
    ├── 采购统计          → /stock/report/purchase
    └── 盘点差异          → /stock/report/checkDiff
```

### 5.2 核心页面说明

| 页面 | 主要功能 |
|-----|---------|
| **库存列表** | 商品库存一览，支持搜索、预警筛选、导出 |
| **库存流水** | 所有进出库记录，支持按类型/时间/商品筛选 |
| **供应商管理** | 供应商 CRUD，启用/禁用 |
| **采购单管理** | 采购单列表、创建、入库操作 |
| **手动入库** | 选择商品 → 选择入库类型 → 输入数量 → 确认 |
| **手动出库** | 选择商品 → 选择出库类型 → 输入数量 → 校验库存 → 确认 |
| **库存盘点** | 盘点单列表、创建盘点、录入实际库存、确认 |
| **报表页面** | 图表 + 表格展示统计数据 |

---

## 六、实现规划

### 6.1 阶段划分（3-4 周）

| 阶段 | 时间 | 内容 |
|-----|------|------|
| **第1周** | Day 1-5 | 基础设施：数据库表、实体类、供应商管理、库存初始化 |
| **第2周** | Day 1-5 | 核心功能：采购单、采购入库、手动出入库 |
| **第3周** | Day 1-5 | 盘点与联动：库存盘点、销售出库联动、库存/流水查询 |
| **第4周** | Day 1-5 | 报表与优化：报表开发、测试、Bug修复 |

### 6.2 技术实现要点

| 要点 | 说明 |
|-----|------|
| **包结构** | `com.zbkj.service.service.stock.*` 新建 stock 子包 |
| **事务控制** | 库存变动操作使用 `@Transactional` 保证原子性 |
| **并发安全** | 库存扣减使用 `UPDATE ... WHERE stock >= ?` |
| **初始化** | 提供一次性脚本，从 `eb_store_product` 同步到 `eb_stock` |
| **权限配置** | 新增菜单后在系统配置权限 |

### 6.3 风险控制

| 风险 | 应对措施 |
|-----|---------|
| 库存数据不一致 | 统一通过 StockService 操作，禁止直接改表 |
| 销售出库失败 | 发货前校验库存，不足时提示 |
| 历史数据迁移 | 初始化时记录日志，支持回滚 |

---

*本设计方案基于需求讨论生成，用于指导后续开发实施*
