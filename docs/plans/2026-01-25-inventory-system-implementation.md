# 进销存系统实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现基础版进销存系统，包含供应商管理、采购入库、手动出入库、库存盘点、销售联动和库存报表功能。

**Architecture:** 采用独立库存表模式（`eb_stock` 为主，同步到 `eb_store_product.stock`）。所有库存变动通过统一的 `StockService` 入口，保证数据一致性和并发安全。

**Tech Stack:** SpringBoot 2.2.6 + MyBatis-Plus 3.3.1 + Vue 2 + Element UI

---

## 任务概览

| 阶段 | 任务 | 预估时间 |
|-----|------|---------|
| 阶段1 | 数据库表创建 | 2小时 |
| 阶段2 | 后端实体类与DAO | 3小时 |
| 阶段3 | 供应商管理（后端+前端） | 4小时 |
| 阶段4 | 库存核心服务 | 4小时 |
| 阶段5 | 采购管理（后端+前端） | 6小时 |
| 阶段6 | 手动出入库（后端+前端） | 4小时 |
| 阶段7 | 库存盘点（后端+前端） | 6小时 |
| 阶段8 | 销售出库联动 | 2小时 |
| 阶段9 | 库存列表与流水（后端+前端） | 4小时 |
| 阶段10 | 库存报表（后端+前端） | 4小时 |
| 阶段11 | 菜单权限配置 | 1小时 |

---

## Task 1: 创建数据库表

**Files:**
- Create: `crmeb/sql/inventory_tables.sql`

**Step 1: 创建 SQL 文件**

```sql
-- ============================================
-- 进销存系统数据库表
-- 创建时间: 2026-01-25
-- ============================================

-- 供应商表
CREATE TABLE IF NOT EXISTS eb_supplier (
  id INT(11) PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL COMMENT '供应商名称',
  contact VARCHAR(50) NULL COMMENT '联系人',
  phone VARCHAR(20) NULL COMMENT '联系电话',
  address VARCHAR(255) NULL COMMENT '地址',
  remark VARCHAR(500) NULL COMMENT '备注',
  status TINYINT(1) DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  is_del TINYINT(1) DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_status (status),
  KEY idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商表';

-- 库存表
CREATE TABLE IF NOT EXISTS eb_stock (
  id INT(11) PRIMARY KEY AUTO_INCREMENT,
  product_id INT(11) NOT NULL COMMENT '商品ID',
  attr_value_id INT(11) DEFAULT 0 COMMENT '规格ID（0=无规格）',
  stock INT(11) DEFAULT 0 COMMENT '当前库存数量',
  warning_stock INT(11) DEFAULT 0 COMMENT '预警库存',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_product_attr (product_id, attr_value_id),
  KEY idx_product_id (product_id),
  KEY idx_warning (warning_stock)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

-- 库存流水表
CREATE TABLE IF NOT EXISTS eb_stock_log (
  id BIGINT(20) PRIMARY KEY AUTO_INCREMENT,
  product_id INT(11) NOT NULL COMMENT '商品ID',
  attr_value_id INT(11) DEFAULT 0 COMMENT '规格ID',
  type TINYINT(2) NOT NULL COMMENT '类型：1-采购入库 2-退货入库 3-盘盈入库 4-其他入库 5-销售出库 6-报损出库 7-盘亏出库 8-其他出库',
  quantity INT(11) NOT NULL COMMENT '数量（正=入库，负=出库）',
  before_stock INT(11) NOT NULL COMMENT '变动前库存',
  after_stock INT(11) NOT NULL COMMENT '变动后库存',
  relation_id BIGINT(20) NULL COMMENT '关联单据ID',
  relation_type VARCHAR(20) NULL COMMENT '关联单据类型：purchase/order/check',
  operator_id INT(11) NULL COMMENT '操作人ID',
  operator_name VARCHAR(50) NULL COMMENT '操作人姓名',
  remark VARCHAR(255) NULL COMMENT '备注',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_product (product_id, attr_value_id),
  KEY idx_type (type),
  KEY idx_relation (relation_type, relation_id),
  KEY idx_create_time (create_time),
  KEY idx_operator (operator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水表';

-- 采购单主表
CREATE TABLE IF NOT EXISTS eb_purchase (
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
  UNIQUE KEY uk_purchase_no (purchase_no),
  KEY idx_supplier (supplier_id),
  KEY idx_status (status),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购单表';

-- 采购单明细表
CREATE TABLE IF NOT EXISTS eb_purchase_item (
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
  KEY idx_purchase_id (purchase_id),
  KEY idx_product (product_id, attr_value_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购单明细表';

-- 盘点单主表
CREATE TABLE IF NOT EXISTS eb_stock_check (
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
  UNIQUE KEY uk_check_no (check_no),
  KEY idx_status (status),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点单表';

-- 盘点单明细表
CREATE TABLE IF NOT EXISTS eb_stock_check_item (
  id INT(11) PRIMARY KEY AUTO_INCREMENT,
  check_id INT(11) NOT NULL COMMENT '盘点单ID',
  product_id INT(11) NOT NULL COMMENT '商品ID',
  attr_value_id INT(11) DEFAULT 0 COMMENT '规格ID',
  product_name VARCHAR(200) NULL COMMENT '商品名称',
  sku_name VARCHAR(200) NULL COMMENT '规格名称',
  system_stock INT(11) DEFAULT 0 COMMENT '系统库存',
  actual_stock INT(11) NULL COMMENT '实际库存',
  diff_quantity INT(11) DEFAULT 0 COMMENT '差异数量',
  KEY idx_check_id (check_id),
  KEY idx_product (product_id, attr_value_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点单明细表';
```

**Step 2: 执行 SQL 创建表**

Run: `docker-compose exec mysql mysql -u single_open -p'你的密码' single_open < crmeb/sql/inventory_tables.sql`

或在 MySQL 客户端中直接执行 SQL。

**Step 3: 验证表创建成功**

Run: `docker-compose exec mysql mysql -u single_open -p'你的密码' single_open -e "SHOW TABLES LIKE 'eb_%stock%'; SHOW TABLES LIKE 'eb_supplier'; SHOW TABLES LIKE 'eb_purchase%';"`

Expected: 显示 7 张新表

**Step 4: Commit**

```bash
git add crmeb/sql/inventory_tables.sql
git commit -m "feat(inventory): 创建进销存系统数据库表"
```

---

## Task 2: 创建后端实体类

**Files:**
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/model/stock/Supplier.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/model/stock/Stock.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/model/stock/StockLog.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/model/stock/Purchase.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/model/stock/PurchaseItem.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/model/stock/StockCheck.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/model/stock/StockCheckItem.java`

**Step 1: 创建 Supplier 实体类**

```java
package com.zbkj.common.model.stock;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 供应商表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_supplier")
@ApiModel(value = "Supplier对象", description = "供应商表")
public class Supplier implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "供应商名称")
    private String name;

    @ApiModelProperty(value = "联系人")
    private String contact;

    @ApiModelProperty(value = "联系电话")
    private String phone;

    @ApiModelProperty(value = "地址")
    private String address;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "状态：0-禁用 1-启用")
    private Boolean status;

    @ApiModelProperty(value = "是否删除：0-否 1-是")
    private Boolean isDel;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    private Date updateTime;
}
```

**Step 2: 创建 Stock 实体类**

```java
package com.zbkj.common.model.stock;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 库存表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_stock")
@ApiModel(value = "Stock对象", description = "库存表")
public class Stock implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "商品ID")
    private Integer productId;

    @ApiModelProperty(value = "规格ID（0=无规格）")
    private Integer attrValueId;

    @ApiModelProperty(value = "当前库存数量")
    private Integer stock;

    @ApiModelProperty(value = "预警库存")
    private Integer warningStock;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    private Date updateTime;
}
```

**Step 3: 创建 StockLog 实体类**

```java
package com.zbkj.common.model.stock;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 库存流水表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_stock_log")
@ApiModel(value = "StockLog对象", description = "库存流水表")
public class StockLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "商品ID")
    private Integer productId;

    @ApiModelProperty(value = "规格ID")
    private Integer attrValueId;

    @ApiModelProperty(value = "类型：1-采购入库 2-退货入库 3-盘盈入库 4-其他入库 5-销售出库 6-报损出库 7-盘亏出库 8-其他出库")
    private Integer type;

    @ApiModelProperty(value = "数量（正=入库，负=出库）")
    private Integer quantity;

    @ApiModelProperty(value = "变动前库存")
    private Integer beforeStock;

    @ApiModelProperty(value = "变动后库存")
    private Integer afterStock;

    @ApiModelProperty(value = "关联单据ID")
    private Long relationId;

    @ApiModelProperty(value = "关联单据类型：purchase/order/check")
    private String relationType;

    @ApiModelProperty(value = "操作人ID")
    private Integer operatorId;

    @ApiModelProperty(value = "操作人姓名")
    private String operatorName;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;
}
```

**Step 4: 创建 Purchase 实体类**

```java
package com.zbkj.common.model.stock;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 采购单表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_purchase")
@ApiModel(value = "Purchase对象", description = "采购单表")
public class Purchase implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "采购单号")
    private String purchaseNo;

    @ApiModelProperty(value = "供应商ID")
    private Integer supplierId;

    @ApiModelProperty(value = "商品总数量")
    private Integer totalQuantity;

    @ApiModelProperty(value = "采购总金额")
    private BigDecimal totalAmount;

    @ApiModelProperty(value = "状态：0-待入库 1-部分入库 2-已入库 3-已取消")
    private Integer status;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "创建人ID")
    private Integer operatorId;

    @ApiModelProperty(value = "创建人姓名")
    private String operatorName;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    private Date updateTime;
}
```

**Step 5: 创建 PurchaseItem 实体类**

```java
package com.zbkj.common.model.stock;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 采购单明细表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_purchase_item")
@ApiModel(value = "PurchaseItem对象", description = "采购单明细表")
public class PurchaseItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "采购单ID")
    private Integer purchaseId;

    @ApiModelProperty(value = "商品ID")
    private Integer productId;

    @ApiModelProperty(value = "规格ID")
    private Integer attrValueId;

    @ApiModelProperty(value = "商品名称")
    private String productName;

    @ApiModelProperty(value = "规格名称")
    private String skuName;

    @ApiModelProperty(value = "采购数量")
    private Integer quantity;

    @ApiModelProperty(value = "已入库数量")
    private Integer inQuantity;

    @ApiModelProperty(value = "采购单价")
    private BigDecimal price;

    @ApiModelProperty(value = "小计金额")
    private BigDecimal amount;
}
```

**Step 6: 创建 StockCheck 实体类**

```java
package com.zbkj.common.model.stock;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 盘点单表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_stock_check")
@ApiModel(value = "StockCheck对象", description = "盘点单表")
public class StockCheck implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "盘点单号")
    private String checkNo;

    @ApiModelProperty(value = "状态：0-盘点中 1-已完成 2-已取消")
    private Integer status;

    @ApiModelProperty(value = "盘盈总数")
    private Integer totalProfit;

    @ApiModelProperty(value = "盘亏总数")
    private Integer totalLoss;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "创建人ID")
    private Integer operatorId;

    @ApiModelProperty(value = "创建人姓名")
    private String operatorName;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "完成时间")
    private Date finishTime;
}
```

**Step 7: 创建 StockCheckItem 实体类**

```java
package com.zbkj.common.model.stock;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 盘点单明细表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_stock_check_item")
@ApiModel(value = "StockCheckItem对象", description = "盘点单明细表")
public class StockCheckItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "盘点单ID")
    private Integer checkId;

    @ApiModelProperty(value = "商品ID")
    private Integer productId;

    @ApiModelProperty(value = "规格ID")
    private Integer attrValueId;

    @ApiModelProperty(value = "商品名称")
    private String productName;

    @ApiModelProperty(value = "规格名称")
    private String skuName;

    @ApiModelProperty(value = "系统库存")
    private Integer systemStock;

    @ApiModelProperty(value = "实际库存")
    private Integer actualStock;

    @ApiModelProperty(value = "差异数量")
    private Integer diffQuantity;
}
```

**Step 8: Commit**

```bash
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/model/stock/
git commit -m "feat(inventory): 创建进销存实体类"
```

---

## Task 3: 创建 DAO 层

**Files:**
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/SupplierDao.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/StockDao.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/StockLogDao.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/PurchaseDao.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/PurchaseItemDao.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/StockCheckDao.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/StockCheckItemDao.java`

**Step 1: 创建 SupplierDao**

```java
package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.stock.Supplier;

/**
 * 供应商 Mapper 接口
 */
public interface SupplierDao extends BaseMapper<Supplier> {
}
```

**Step 2: 创建 StockDao**

```java
package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.stock.Stock;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 库存 Mapper 接口
 */
public interface StockDao extends BaseMapper<Stock> {

    /**
     * 安全扣减库存（防止超卖）
     * @param productId 商品ID
     * @param attrValueId 规格ID
     * @param quantity 扣减数量（正数）
     * @return 影响行数
     */
    @Update("UPDATE eb_stock SET stock = stock - #{quantity}, update_time = NOW() " +
            "WHERE product_id = #{productId} AND attr_value_id = #{attrValueId} AND stock >= #{quantity}")
    int deductStock(@Param("productId") Integer productId,
                    @Param("attrValueId") Integer attrValueId,
                    @Param("quantity") Integer quantity);

    /**
     * 增加库存
     * @param productId 商品ID
     * @param attrValueId 规格ID
     * @param quantity 增加数量（正数）
     * @return 影响行数
     */
    @Update("UPDATE eb_stock SET stock = stock + #{quantity}, update_time = NOW() " +
            "WHERE product_id = #{productId} AND attr_value_id = #{attrValueId}")
    int addStock(@Param("productId") Integer productId,
                 @Param("attrValueId") Integer attrValueId,
                 @Param("quantity") Integer quantity);
}
```

**Step 3: 创建 StockLogDao**

```java
package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.stock.StockLog;

/**
 * 库存流水 Mapper 接口
 */
public interface StockLogDao extends BaseMapper<StockLog> {
}
```

**Step 4: 创建 PurchaseDao**

```java
package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.stock.Purchase;

/**
 * 采购单 Mapper 接口
 */
public interface PurchaseDao extends BaseMapper<Purchase> {
}
```

**Step 5: 创建 PurchaseItemDao**

```java
package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.stock.PurchaseItem;

/**
 * 采购单明细 Mapper 接口
 */
public interface PurchaseItemDao extends BaseMapper<PurchaseItem> {
}
```

**Step 6: 创建 StockCheckDao**

```java
package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.stock.StockCheck;

/**
 * 盘点单 Mapper 接口
 */
public interface StockCheckDao extends BaseMapper<StockCheck> {
}
```

**Step 7: 创建 StockCheckItemDao**

```java
package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.stock.StockCheckItem;

/**
 * 盘点单明细 Mapper 接口
 */
public interface StockCheckItemDao extends BaseMapper<StockCheckItem> {
}
```

**Step 8: Commit**

```bash
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/SupplierDao.java
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/StockDao.java
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/StockLogDao.java
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/PurchaseDao.java
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/PurchaseItemDao.java
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/StockCheckDao.java
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/StockCheckItemDao.java
git commit -m "feat(inventory): 创建进销存DAO层"
```

---

## Task 4: 创建常量和枚举

**Files:**
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/constants/StockConstants.java`

**Step 1: 创建库存常量类**

```java
package com.zbkj.common.constants;

/**
 * 库存相关常量
 */
public class StockConstants {

    // ========== 库存流水类型 ==========
    /** 采购入库 */
    public static final int LOG_TYPE_PURCHASE_IN = 1;
    /** 退货入库 */
    public static final int LOG_TYPE_RETURN_IN = 2;
    /** 盘盈入库 */
    public static final int LOG_TYPE_CHECK_PROFIT_IN = 3;
    /** 其他入库 */
    public static final int LOG_TYPE_OTHER_IN = 4;
    /** 销售出库 */
    public static final int LOG_TYPE_SALES_OUT = 5;
    /** 报损出库 */
    public static final int LOG_TYPE_DAMAGE_OUT = 6;
    /** 盘亏出库 */
    public static final int LOG_TYPE_CHECK_LOSS_OUT = 7;
    /** 其他出库 */
    public static final int LOG_TYPE_OTHER_OUT = 8;

    // ========== 采购单状态 ==========
    /** 待入库 */
    public static final int PURCHASE_STATUS_PENDING = 0;
    /** 部分入库 */
    public static final int PURCHASE_STATUS_PARTIAL = 1;
    /** 已入库 */
    public static final int PURCHASE_STATUS_COMPLETED = 2;
    /** 已取消 */
    public static final int PURCHASE_STATUS_CANCELLED = 3;

    // ========== 盘点单状态 ==========
    /** 盘点中 */
    public static final int CHECK_STATUS_CHECKING = 0;
    /** 已完成 */
    public static final int CHECK_STATUS_COMPLETED = 1;
    /** 已取消 */
    public static final int CHECK_STATUS_CANCELLED = 2;

    // ========== 关联单据类型 ==========
    /** 采购单 */
    public static final String RELATION_TYPE_PURCHASE = "purchase";
    /** 订单 */
    public static final String RELATION_TYPE_ORDER = "order";
    /** 盘点单 */
    public static final String RELATION_TYPE_CHECK = "check";
    /** 手动操作 */
    public static final String RELATION_TYPE_MANUAL = "manual";

    // ========== 单号前缀 ==========
    /** 采购单前缀 */
    public static final String PURCHASE_NO_PREFIX = "PO";
    /** 盘点单前缀 */
    public static final String CHECK_NO_PREFIX = "SC";
}
```

**Step 2: Commit**

```bash
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/constants/StockConstants.java
git commit -m "feat(inventory): 创建库存常量类"
```

---

## Task 5: 创建请求和响应对象

**Files:**
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/SupplierRequest.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/SupplierSearchRequest.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/StockInRequest.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/StockOutRequest.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/PurchaseAddRequest.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/PurchaseInStockRequest.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/StockCheckCreateRequest.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/StockCheckUpdateItemRequest.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/response/StockResponse.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/response/PurchaseResponse.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/response/StockCheckResponse.java`

**Step 1: 创建 SupplierRequest**

```java
package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 供应商请求对象
 */
@Data
@ApiModel(value = "SupplierRequest", description = "供应商请求对象")
public class SupplierRequest {

    @ApiModelProperty(value = "供应商ID（编辑时必填）")
    private Integer id;

    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 100, message = "供应商名称不能超过100个字符")
    @ApiModelProperty(value = "供应商名称", required = true)
    private String name;

    @Size(max = 50, message = "联系人不能超过50个字符")
    @ApiModelProperty(value = "联系人")
    private String contact;

    @Size(max = 20, message = "联系电话不能超过20个字符")
    @ApiModelProperty(value = "联系电话")
    private String phone;

    @Size(max = 255, message = "地址不能超过255个字符")
    @ApiModelProperty(value = "地址")
    private String address;

    @Size(max = 500, message = "备注不能超过500个字符")
    @ApiModelProperty(value = "备注")
    private String remark;
}
```

**Step 2: 创建 SupplierSearchRequest**

```java
package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 供应商搜索请求对象
 */
@Data
@ApiModel(value = "SupplierSearchRequest", description = "供应商搜索请求对象")
public class SupplierSearchRequest {

    @ApiModelProperty(value = "关键字（名称/联系人/电话）")
    private String keywords;

    @ApiModelProperty(value = "状态：0-禁用 1-启用")
    private Boolean status;
}
```

**Step 3: 创建 StockInRequest**

```java
package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 手动入库请求对象
 */
@Data
@ApiModel(value = "StockInRequest", description = "手动入库请求对象")
public class StockInRequest {

    @NotNull(message = "商品ID不能为空")
    @ApiModelProperty(value = "商品ID", required = true)
    private Integer productId;

    @ApiModelProperty(value = "规格ID（0=无规格）")
    private Integer attrValueId = 0;

    @NotNull(message = "入库类型不能为空")
    @ApiModelProperty(value = "入库类型：2-退货入库 4-其他入库", required = true)
    private Integer type;

    @NotNull(message = "入库数量不能为空")
    @Min(value = 1, message = "入库数量必须大于0")
    @ApiModelProperty(value = "入库数量", required = true)
    private Integer quantity;

    @ApiModelProperty(value = "备注")
    private String remark;
}
```

**Step 4: 创建 StockOutRequest**

```java
package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 手动出库请求对象
 */
@Data
@ApiModel(value = "StockOutRequest", description = "手动出库请求对象")
public class StockOutRequest {

    @NotNull(message = "商品ID不能为空")
    @ApiModelProperty(value = "商品ID", required = true)
    private Integer productId;

    @ApiModelProperty(value = "规格ID（0=无规格）")
    private Integer attrValueId = 0;

    @NotNull(message = "出库类型不能为空")
    @ApiModelProperty(value = "出库类型：6-报损出库 8-其他出库", required = true)
    private Integer type;

    @NotNull(message = "出库数量不能为空")
    @Min(value = 1, message = "出库数量必须大于0")
    @ApiModelProperty(value = "出库数量", required = true)
    private Integer quantity;

    @ApiModelProperty(value = "备注")
    private String remark;
}
```

**Step 5: 创建 PurchaseAddRequest**

```java
package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 创建采购单请求对象
 */
@Data
@ApiModel(value = "PurchaseAddRequest", description = "创建采购单请求对象")
public class PurchaseAddRequest {

    @NotNull(message = "供应商ID不能为空")
    @ApiModelProperty(value = "供应商ID", required = true)
    private Integer supplierId;

    @ApiModelProperty(value = "备注")
    private String remark;

    @NotEmpty(message = "采购商品不能为空")
    @Valid
    @ApiModelProperty(value = "采购商品列表", required = true)
    private List<PurchaseItemRequest> items;

    @Data
    @ApiModel(value = "PurchaseItemRequest", description = "采购商品明细")
    public static class PurchaseItemRequest {

        @NotNull(message = "商品ID不能为空")
        @ApiModelProperty(value = "商品ID", required = true)
        private Integer productId;

        @ApiModelProperty(value = "规格ID（0=无规格）")
        private Integer attrValueId = 0;

        @NotNull(message = "采购数量不能为空")
        @ApiModelProperty(value = "采购数量", required = true)
        private Integer quantity;

        @NotNull(message = "采购单价不能为空")
        @ApiModelProperty(value = "采购单价", required = true)
        private BigDecimal price;
    }
}
```

**Step 6: 创建 PurchaseInStockRequest**

```java
package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 采购入库请求对象
 */
@Data
@ApiModel(value = "PurchaseInStockRequest", description = "采购入库请求对象")
public class PurchaseInStockRequest {

    @NotNull(message = "采购单ID不能为空")
    @ApiModelProperty(value = "采购单ID", required = true)
    private Integer purchaseId;

    @NotEmpty(message = "入库明细不能为空")
    @Valid
    @ApiModelProperty(value = "入库明细列表", required = true)
    private List<InStockItemRequest> items;

    @Data
    @ApiModel(value = "InStockItemRequest", description = "入库明细")
    public static class InStockItemRequest {

        @NotNull(message = "采购明细ID不能为空")
        @ApiModelProperty(value = "采购明细ID", required = true)
        private Integer itemId;

        @NotNull(message = "入库数量不能为空")
        @ApiModelProperty(value = "本次入库数量", required = true)
        private Integer quantity;
    }
}
```

**Step 7: 创建 StockCheckCreateRequest**

```java
package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 创建盘点单请求对象
 */
@Data
@ApiModel(value = "StockCheckCreateRequest", description = "创建盘点单请求对象")
public class StockCheckCreateRequest {

    @ApiModelProperty(value = "备注")
    private String remark;

    @NotEmpty(message = "盘点商品不能为空")
    @ApiModelProperty(value = "盘点商品ID列表（商品主键），空则盘点全部", required = true)
    private List<Integer> productIds;
}
```

**Step 8: 创建 StockCheckUpdateItemRequest**

```java
package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 更新盘点数量请求对象
 */
@Data
@ApiModel(value = "StockCheckUpdateItemRequest", description = "更新盘点数量请求对象")
public class StockCheckUpdateItemRequest {

    @NotNull(message = "盘点明细ID不能为空")
    @ApiModelProperty(value = "盘点明细ID", required = true)
    private Integer itemId;

    @NotNull(message = "实际库存不能为空")
    @Min(value = 0, message = "实际库存不能小于0")
    @ApiModelProperty(value = "实际库存", required = true)
    private Integer actualStock;
}
```

**Step 9: 创建 StockResponse**

```java
package com.zbkj.common.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 库存响应对象
 */
@Data
@ApiModel(value = "StockResponse", description = "库存响应对象")
public class StockResponse {

    @ApiModelProperty(value = "库存ID")
    private Integer id;

    @ApiModelProperty(value = "商品ID")
    private Integer productId;

    @ApiModelProperty(value = "商品名称")
    private String productName;

    @ApiModelProperty(value = "商品图片")
    private String productImage;

    @ApiModelProperty(value = "规格ID")
    private Integer attrValueId;

    @ApiModelProperty(value = "规格名称")
    private String skuName;

    @ApiModelProperty(value = "当前库存")
    private Integer stock;

    @ApiModelProperty(value = "预警库存")
    private Integer warningStock;

    @ApiModelProperty(value = "是否预警")
    private Boolean isWarning;

    @ApiModelProperty(value = "更新时间")
    private Date updateTime;
}
```

**Step 10: 创建 PurchaseResponse**

```java
package com.zbkj.common.response;

import com.zbkj.common.model.stock.PurchaseItem;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 采购单响应对象
 */
@Data
@ApiModel(value = "PurchaseResponse", description = "采购单响应对象")
public class PurchaseResponse {

    @ApiModelProperty(value = "采购单ID")
    private Integer id;

    @ApiModelProperty(value = "采购单号")
    private String purchaseNo;

    @ApiModelProperty(value = "供应商ID")
    private Integer supplierId;

    @ApiModelProperty(value = "供应商名称")
    private String supplierName;

    @ApiModelProperty(value = "商品总数量")
    private Integer totalQuantity;

    @ApiModelProperty(value = "采购总金额")
    private BigDecimal totalAmount;

    @ApiModelProperty(value = "状态：0-待入库 1-部分入库 2-已入库 3-已取消")
    private Integer status;

    @ApiModelProperty(value = "状态名称")
    private String statusName;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "创建人")
    private String operatorName;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "采购明细")
    private List<PurchaseItem> items;
}
```

**Step 11: 创建 StockCheckResponse**

```java
package com.zbkj.common.response;

import com.zbkj.common.model.stock.StockCheckItem;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 盘点单响应对象
 */
@Data
@ApiModel(value = "StockCheckResponse", description = "盘点单响应对象")
public class StockCheckResponse {

    @ApiModelProperty(value = "盘点单ID")
    private Integer id;

    @ApiModelProperty(value = "盘点单号")
    private String checkNo;

    @ApiModelProperty(value = "状态：0-盘点中 1-已完成 2-已取消")
    private Integer status;

    @ApiModelProperty(value = "状态名称")
    private String statusName;

    @ApiModelProperty(value = "盘盈总数")
    private Integer totalProfit;

    @ApiModelProperty(value = "盘亏总数")
    private Integer totalLoss;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "创建人")
    private String operatorName;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "完成时间")
    private Date finishTime;

    @ApiModelProperty(value = "盘点明细")
    private List<StockCheckItem> items;
}
```

**Step 12: Commit**

```bash
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/request/Supplier*.java
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/request/Stock*.java
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/request/Purchase*.java
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/response/Stock*.java
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/response/Purchase*.java
git commit -m "feat(inventory): 创建请求和响应对象"
```

---

## Task 6-11: 后续任务概要

由于篇幅限制，以下任务将在执行时详细展开：

### Task 6: 供应商管理服务

- 创建 `SupplierService` 接口和实现
- 创建 `SupplierController`
- 前端 API 封装和页面

### Task 7: 库存核心服务

- 创建 `StockService` 接口和实现（统一入口）
- 实现库存变动、同步商品库存、写入流水

### Task 8: 采购管理

- 创建 `PurchaseService` 接口和实现
- 创建 `PurchaseController`
- 前端 API 封装和页面

### Task 9: 手动出入库

- 创建出入库接口
- 前端页面

### Task 10: 库存盘点

- 创建 `StockCheckService` 接口和实现
- 创建 `StockCheckController`
- 前端页面

### Task 11: 销售出库联动

- 修改 `OrderService`，在发货时调用 `StockService.salesOut()`

### Task 12: 库存列表与流水

- 库存列表接口和页面
- 流水查询接口和页面

### Task 13: 库存报表

- 变动汇总报表
- 采购统计报表
- 盘点差异报表

### Task 14: 菜单权限配置

- 在数据库 `eb_system_menu` 表中添加菜单
- 配置权限标识

---

## 验证清单

每个阶段完成后进行验证：

- [ ] 数据库表创建成功
- [ ] 后端编译通过
- [ ] 前端编译通过
- [ ] 供应商 CRUD 功能正常
- [ ] 采购单创建和入库功能正常
- [ ] 手动出入库功能正常
- [ ] 盘点功能正常
- [ ] 订单发货时库存正确扣减
- [ ] 库存列表和流水查询正常
- [ ] 报表数据正确

---

*计划生成时间: 2026-01-25*
*设计文档: docs/plans/2026-01-25-inventory-system-design.md*
