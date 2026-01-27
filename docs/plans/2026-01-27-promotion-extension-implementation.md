# 促销活动扩展实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现满减活动、买赠活动、代金券三个促销功能模块

**Architecture:**
- 新增 `promotion` 子包处理满减和买赠活动
- 扩展现有 `StoreCoupon` 模型支持代金券
- 修改 `OrderServiceImpl.computedPrice()` 集成新的价格计算逻辑

**Tech Stack:** Spring Boot 2.2.6 + MyBatis-Plus 3.3.1 + Vue 2 + Element UI

**设计文档:** `docs/plans/2026-01-27-promotion-extension-design.md`

---

## 阶段一：满减活动（Day 1-3）

### Task 1: 创建满减活动数据库表

**Files:**
- Create: `crmeb/crmeb-common/src/main/resources/sql/promotion_tables.sql`

**Step 1: 创建 SQL 文件**

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
```

**Step 2: 执行 SQL 创建表**

Run: `docker-compose exec mysql mysql -u single_open -p single_open < crmeb/crmeb-common/src/main/resources/sql/promotion_tables.sql`

或手动在 MySQL 客户端执行。

**Step 3: 提交数据库脚本**

```bash
git add crmeb/crmeb-common/src/main/resources/sql/promotion_tables.sql
git commit -m "feat(promotion): 添加满减活动数据库表结构"
```

---

### Task 2: 创建满减活动实体类

**Files:**
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/model/promotion/FullReduction.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/model/promotion/FullReductionLevel.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/model/promotion/FullReductionProduct.java`

**Step 1: 创建 FullReduction 实体类**

```java
package com.zbkj.common.model.promotion;

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
 * 满减活动主表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_full_reduction")
@ApiModel(value = "FullReduction对象", description = "满减活动主表")
public class FullReduction implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "活动名称")
    private String name;

    @ApiModelProperty(value = "范围：1-全场 2-品类 3-指定商品")
    private Integer scopeType;

    @ApiModelProperty(value = "开始时间")
    private Date startTime;

    @ApiModelProperty(value = "结束时间")
    private Date endTime;

    @ApiModelProperty(value = "是否允许叠加优惠券：0-否 1-是")
    private Boolean allowCoupon;

    @ApiModelProperty(value = "状态：0-关闭 1-开启")
    private Boolean status;

    @ApiModelProperty(value = "是否删除：0-否 1-是")
    private Boolean isDel;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    private Date updateTime;
}
```

**Step 2: 创建 FullReductionLevel 实体类**

```java
package com.zbkj.common.model.promotion;

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
 * 满减阶梯表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_full_reduction_level")
@ApiModel(value = "FullReductionLevel对象", description = "满减阶梯表")
public class FullReductionLevel implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "满减活动ID")
    private Integer reductionId;

    @ApiModelProperty(value = "满足金额")
    private BigDecimal fullAmount;

    @ApiModelProperty(value = "减免金额")
    private BigDecimal reduceAmount;
}
```

**Step 3: 创建 FullReductionProduct 实体类**

```java
package com.zbkj.common.model.promotion;

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
 * 满减关联表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_full_reduction_product")
@ApiModel(value = "FullReductionProduct对象", description = "满减关联表")
public class FullReductionProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "满减活动ID")
    private Integer reductionId;

    @ApiModelProperty(value = "关联类型：1-品类 2-商品")
    private Integer relationType;

    @ApiModelProperty(value = "品类ID或商品ID")
    private Integer relationId;
}
```

**Step 4: 提交实体类**

```bash
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/model/promotion/
git commit -m "feat(promotion): 添加满减活动实体类"
```

---

### Task 3: 创建满减活动 DAO 层

**Files:**
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/promotion/FullReductionDao.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/promotion/FullReductionLevelDao.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/promotion/FullReductionProductDao.java`

**Step 1: 创建 FullReductionDao**

```java
package com.zbkj.service.dao.promotion;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.promotion.FullReduction;

/**
 * 满减活动 DAO
 */
public interface FullReductionDao extends BaseMapper<FullReduction> {
}
```

**Step 2: 创建 FullReductionLevelDao**

```java
package com.zbkj.service.dao.promotion;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.promotion.FullReductionLevel;

/**
 * 满减阶梯 DAO
 */
public interface FullReductionLevelDao extends BaseMapper<FullReductionLevel> {
}
```

**Step 3: 创建 FullReductionProductDao**

```java
package com.zbkj.service.dao.promotion;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.promotion.FullReductionProduct;

/**
 * 满减关联 DAO
 */
public interface FullReductionProductDao extends BaseMapper<FullReductionProduct> {
}
```

**Step 4: 提交 DAO 层**

```bash
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/promotion/
git commit -m "feat(promotion): 添加满减活动 DAO 层"
```

---

### Task 4: 创建满减活动请求/响应对象

**Files:**
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/FullReductionRequest.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/FullReductionSearchRequest.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/response/FullReductionResponse.java`

**Step 1: 创建 FullReductionRequest**

```java
package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 满减活动请求对象
 */
@Data
@ApiModel(value = "FullReductionRequest", description = "满减活动请求对象")
public class FullReductionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "活动ID（编辑时必填）")
    private Integer id;

    @ApiModelProperty(value = "活动名称", required = true)
    @NotBlank(message = "活动名称不能为空")
    private String name;

    @ApiModelProperty(value = "范围：1-全场 2-品类 3-指定商品", required = true)
    @NotNull(message = "请选择活动范围")
    private Integer scopeType;

    @ApiModelProperty(value = "开始时间", required = true)
    @NotBlank(message = "请选择开始时间")
    private String startTime;

    @ApiModelProperty(value = "结束时间", required = true)
    @NotBlank(message = "请选择结束时间")
    private String endTime;

    @ApiModelProperty(value = "是否允许叠加优惠券：0-否 1-是")
    private Boolean allowCoupon = true;

    @ApiModelProperty(value = "关联的品类/商品ID列表（scopeType=2或3时必填）")
    private List<Integer> relationIds;

    @ApiModelProperty(value = "阶梯满减列表", required = true)
    @NotNull(message = "请设置满减阶梯")
    private List<LevelItem> levels;

    /**
     * 阶梯项
     */
    @Data
    public static class LevelItem {
        @ApiModelProperty(value = "满足金额")
        private BigDecimal fullAmount;

        @ApiModelProperty(value = "减免金额")
        private BigDecimal reduceAmount;
    }
}
```

**Step 2: 创建 FullReductionSearchRequest**

```java
package com.zbkj.common.request;

import com.zbkj.common.page.PageParamRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 满减活动搜索请求对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "FullReductionSearchRequest", description = "满减活动搜索请求对象")
public class FullReductionSearchRequest extends PageParamRequest {

    @ApiModelProperty(value = "活动名称（模糊搜索）")
    private String name;

    @ApiModelProperty(value = "状态：0-关闭 1-开启")
    private Integer status;

    @ApiModelProperty(value = "范围类型：1-全场 2-品类 3-指定商品")
    private Integer scopeType;
}
```

**Step 3: 创建 FullReductionResponse**

```java
package com.zbkj.common.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 满减活动响应对象
 */
@Data
@ApiModel(value = "FullReductionResponse", description = "满减活动响应对象")
public class FullReductionResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "活动ID")
    private Integer id;

    @ApiModelProperty(value = "活动名称")
    private String name;

    @ApiModelProperty(value = "范围：1-全场 2-品类 3-指定商品")
    private Integer scopeType;

    @ApiModelProperty(value = "范围类型名称")
    private String scopeTypeName;

    @ApiModelProperty(value = "开始时间")
    private Date startTime;

    @ApiModelProperty(value = "结束时间")
    private Date endTime;

    @ApiModelProperty(value = "是否允许叠加优惠券")
    private Boolean allowCoupon;

    @ApiModelProperty(value = "状态：0-关闭 1-开启")
    private Boolean status;

    @ApiModelProperty(value = "活动状态：0-未开始 1-进行中 2-已结束")
    private Integer activityStatus;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "阶梯满减列表")
    private List<LevelItem> levels;

    @ApiModelProperty(value = "关联的品类/商品ID列表")
    private List<Integer> relationIds;

    /**
     * 阶梯项
     */
    @Data
    public static class LevelItem {
        @ApiModelProperty(value = "满足金额")
        private BigDecimal fullAmount;

        @ApiModelProperty(value = "减免金额")
        private BigDecimal reduceAmount;
    }
}
```

**Step 4: 提交请求/响应对象**

```bash
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/request/FullReduction*.java
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/response/FullReductionResponse.java
git commit -m "feat(promotion): 添加满减活动请求/响应对象"
```

---

### Task 5: 创建满减活动 Service 层

**Files:**
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/FullReductionService.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/FullReductionLevelService.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/FullReductionProductService.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/impl/FullReductionServiceImpl.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/impl/FullReductionLevelServiceImpl.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/impl/FullReductionProductServiceImpl.java`

**Step 1: 创建 FullReductionService 接口**

```java
package com.zbkj.service.service.promotion;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.promotion.FullReduction;
import com.zbkj.common.request.FullReductionRequest;
import com.zbkj.common.request.FullReductionSearchRequest;
import com.zbkj.common.response.FullReductionResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * 满减活动 Service 接口
 */
public interface FullReductionService extends IService<FullReduction> {

    /**
     * 分页列表
     */
    PageInfo<FullReductionResponse> getList(FullReductionSearchRequest request);

    /**
     * 详情
     */
    FullReductionResponse getDetail(Integer id);

    /**
     * 新增/编辑
     */
    Boolean save(FullReductionRequest request);

    /**
     * 删除
     */
    Boolean delete(Integer id);

    /**
     * 更新状态
     */
    Boolean updateStatus(Integer id, Boolean status);

    /**
     * 根据商品ID列表查询可用的满减活动
     */
    FullReduction getAvailableByProductIds(List<Integer> productIds, List<Integer> categoryIds);

    /**
     * 计算满减金额
     */
    BigDecimal calculateReduction(FullReduction reduction, BigDecimal totalAmount);
}
```

**Step 2: 创建 FullReductionLevelService 接口**

```java
package com.zbkj.service.service.promotion;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.promotion.FullReductionLevel;

import java.util.List;

/**
 * 满减阶梯 Service 接口
 */
public interface FullReductionLevelService extends IService<FullReductionLevel> {

    /**
     * 根据满减活动ID获取阶梯列表
     */
    List<FullReductionLevel> getByReductionId(Integer reductionId);

    /**
     * 删除满减活动的阶梯
     */
    Boolean deleteByReductionId(Integer reductionId);
}
```

**Step 3: 创建 FullReductionProductService 接口**

```java
package com.zbkj.service.service.promotion;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.promotion.FullReductionProduct;

import java.util.List;

/**
 * 满减关联 Service 接口
 */
public interface FullReductionProductService extends IService<FullReductionProduct> {

    /**
     * 根据满减活动ID获取关联列表
     */
    List<FullReductionProduct> getByReductionId(Integer reductionId);

    /**
     * 删除满减活动的关联
     */
    Boolean deleteByReductionId(Integer reductionId);

    /**
     * 根据商品ID查询关联的满减活动ID列表
     */
    List<Integer> getReductionIdsByProductId(Integer productId);

    /**
     * 根据品类ID查询关联的满减活动ID列表
     */
    List<Integer> getReductionIdsByCategoryId(Integer categoryId);
}
```

**Step 4: 提交 Service 接口**

```bash
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/
git commit -m "feat(promotion): 添加满减活动 Service 接口"
```

---

### Task 6: 实现满减活动 Service 实现类

**Files:**
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/impl/FullReductionServiceImpl.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/impl/FullReductionLevelServiceImpl.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/impl/FullReductionProductServiceImpl.java`

**Step 1: 创建 FullReductionLevelServiceImpl**

```java
package com.zbkj.service.service.promotion.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.model.promotion.FullReductionLevel;
import com.zbkj.service.dao.promotion.FullReductionLevelDao;
import com.zbkj.service.service.promotion.FullReductionLevelService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 满减阶梯 Service 实现类
 */
@Service
public class FullReductionLevelServiceImpl extends ServiceImpl<FullReductionLevelDao, FullReductionLevel>
        implements FullReductionLevelService {

    @Override
    public List<FullReductionLevel> getByReductionId(Integer reductionId) {
        LambdaQueryWrapper<FullReductionLevel> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FullReductionLevel::getReductionId, reductionId);
        wrapper.orderByAsc(FullReductionLevel::getFullAmount);
        return list(wrapper);
    }

    @Override
    public Boolean deleteByReductionId(Integer reductionId) {
        LambdaQueryWrapper<FullReductionLevel> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FullReductionLevel::getReductionId, reductionId);
        return remove(wrapper);
    }
}
```

**Step 2: 创建 FullReductionProductServiceImpl**

```java
package com.zbkj.service.service.promotion.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.model.promotion.FullReductionProduct;
import com.zbkj.service.dao.promotion.FullReductionProductDao;
import com.zbkj.service.service.promotion.FullReductionProductService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 满减关联 Service 实现类
 */
@Service
public class FullReductionProductServiceImpl extends ServiceImpl<FullReductionProductDao, FullReductionProduct>
        implements FullReductionProductService {

    @Override
    public List<FullReductionProduct> getByReductionId(Integer reductionId) {
        LambdaQueryWrapper<FullReductionProduct> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FullReductionProduct::getReductionId, reductionId);
        return list(wrapper);
    }

    @Override
    public Boolean deleteByReductionId(Integer reductionId) {
        LambdaQueryWrapper<FullReductionProduct> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FullReductionProduct::getReductionId, reductionId);
        return remove(wrapper);
    }

    @Override
    public List<Integer> getReductionIdsByProductId(Integer productId) {
        LambdaQueryWrapper<FullReductionProduct> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FullReductionProduct::getRelationType, 2); // 商品
        wrapper.eq(FullReductionProduct::getRelationId, productId);
        return list(wrapper).stream()
                .map(FullReductionProduct::getReductionId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Integer> getReductionIdsByCategoryId(Integer categoryId) {
        LambdaQueryWrapper<FullReductionProduct> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FullReductionProduct::getRelationType, 1); // 品类
        wrapper.eq(FullReductionProduct::getRelationId, categoryId);
        return list(wrapper).stream()
                .map(FullReductionProduct::getReductionId)
                .collect(Collectors.toList());
    }
}
```

**Step 3: 提交基础 Service 实现**

```bash
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/impl/FullReductionLevelServiceImpl.java
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/impl/FullReductionProductServiceImpl.java
git commit -m "feat(promotion): 添加满减阶梯和关联 Service 实现"
```

---

### Task 7: 实现 FullReductionServiceImpl 核心逻辑

**Files:**
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/impl/FullReductionServiceImpl.java`

**Step 1: 创建 FullReductionServiceImpl 实现类**

```java
package com.zbkj.service.service.promotion.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.promotion.FullReduction;
import com.zbkj.common.model.promotion.FullReductionLevel;
import com.zbkj.common.model.promotion.FullReductionProduct;
import com.zbkj.common.request.FullReductionRequest;
import com.zbkj.common.request.FullReductionSearchRequest;
import com.zbkj.common.response.FullReductionResponse;
import com.zbkj.common.utils.CrmebDateUtil;
import com.zbkj.service.dao.promotion.FullReductionDao;
import com.zbkj.service.service.promotion.FullReductionLevelService;
import com.zbkj.service.service.promotion.FullReductionProductService;
import com.zbkj.service.service.promotion.FullReductionService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 满减活动 Service 实现类
 */
@Service
public class FullReductionServiceImpl extends ServiceImpl<FullReductionDao, FullReduction>
        implements FullReductionService {

    @Autowired
    private FullReductionLevelService levelService;

    @Autowired
    private FullReductionProductService productService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public PageInfo<FullReductionResponse> getList(FullReductionSearchRequest request) {
        PageHelper.startPage(request.getPage(), request.getLimit());
        LambdaQueryWrapper<FullReduction> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FullReduction::getIsDel, false);
        if (StrUtil.isNotBlank(request.getName())) {
            wrapper.like(FullReduction::getName, request.getName());
        }
        if (ObjectUtil.isNotNull(request.getStatus())) {
            wrapper.eq(FullReduction::getStatus, request.getStatus() == 1);
        }
        if (ObjectUtil.isNotNull(request.getScopeType())) {
            wrapper.eq(FullReduction::getScopeType, request.getScopeType());
        }
        wrapper.orderByDesc(FullReduction::getId);
        List<FullReduction> list = list(wrapper);
        PageInfo<FullReduction> pageInfo = new PageInfo<>(list);

        // 转换为响应对象
        List<FullReductionResponse> responseList = list.stream().map(this::convertToResponse).collect(Collectors.toList());
        PageInfo<FullReductionResponse> resultPage = new PageInfo<>();
        BeanUtils.copyProperties(pageInfo, resultPage, "list");
        resultPage.setList(responseList);
        return resultPage;
    }

    @Override
    public FullReductionResponse getDetail(Integer id) {
        FullReduction reduction = getById(id);
        if (ObjectUtil.isNull(reduction) || reduction.getIsDel()) {
            throw new CrmebException("满减活动不存在");
        }
        FullReductionResponse response = convertToResponse(reduction);
        // 获取阶梯列表
        List<FullReductionLevel> levels = levelService.getByReductionId(id);
        response.setLevels(levels.stream().map(l -> {
            FullReductionResponse.LevelItem item = new FullReductionResponse.LevelItem();
            item.setFullAmount(l.getFullAmount());
            item.setReduceAmount(l.getReduceAmount());
            return item;
        }).collect(Collectors.toList()));
        // 获取关联商品/品类
        List<FullReductionProduct> products = productService.getByReductionId(id);
        response.setRelationIds(products.stream().map(FullReductionProduct::getRelationId).collect(Collectors.toList()));
        return response;
    }

    @Override
    public Boolean save(FullReductionRequest request) {
        // 校验阶梯
        if (CollUtil.isEmpty(request.getLevels())) {
            throw new CrmebException("请设置满减阶梯");
        }
        // 校验范围
        if (request.getScopeType() != 1 && CollUtil.isEmpty(request.getRelationIds())) {
            throw new CrmebException("请选择关联的商品或品类");
        }

        FullReduction reduction = new FullReduction();
        BeanUtils.copyProperties(request, reduction);
        reduction.setStartTime(CrmebDateUtil.strToDate(request.getStartTime(), "yyyy-MM-dd HH:mm:ss"));
        reduction.setEndTime(CrmebDateUtil.strToDate(request.getEndTime(), "yyyy-MM-dd HH:mm:ss"));
        reduction.setIsDel(false);
        reduction.setStatus(false); // 默认关闭

        return transactionTemplate.execute(status -> {
            if (ObjectUtil.isNotNull(request.getId())) {
                // 编辑
                reduction.setId(request.getId());
                updateById(reduction);
                // 删除旧的阶梯和关联
                levelService.deleteByReductionId(request.getId());
                productService.deleteByReductionId(request.getId());
            } else {
                // 新增
                save(reduction);
            }
            // 保存阶梯
            List<FullReductionLevel> levels = request.getLevels().stream().map(l -> {
                FullReductionLevel level = new FullReductionLevel();
                level.setReductionId(reduction.getId());
                level.setFullAmount(l.getFullAmount());
                level.setReduceAmount(l.getReduceAmount());
                return level;
            }).collect(Collectors.toList());
            levelService.saveBatch(levels);
            // 保存关联（非全场时）
            if (request.getScopeType() != 1 && CollUtil.isNotEmpty(request.getRelationIds())) {
                int relationType = request.getScopeType() == 2 ? 1 : 2; // 2-品类 3-商品
                List<FullReductionProduct> products = request.getRelationIds().stream().map(rid -> {
                    FullReductionProduct p = new FullReductionProduct();
                    p.setReductionId(reduction.getId());
                    p.setRelationType(relationType);
                    p.setRelationId(rid);
                    return p;
                }).collect(Collectors.toList());
                productService.saveBatch(products);
            }
            return true;
        });
    }

    @Override
    public Boolean delete(Integer id) {
        FullReduction reduction = getById(id);
        if (ObjectUtil.isNull(reduction) || reduction.getIsDel()) {
            throw new CrmebException("满减活动不存在");
        }
        LambdaUpdateWrapper<FullReduction> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(FullReduction::getId, id);
        wrapper.set(FullReduction::getIsDel, true);
        return update(wrapper);
    }

    @Override
    public Boolean updateStatus(Integer id, Boolean status) {
        FullReduction reduction = getById(id);
        if (ObjectUtil.isNull(reduction) || reduction.getIsDel()) {
            throw new CrmebException("满减活动不存在");
        }
        LambdaUpdateWrapper<FullReduction> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(FullReduction::getId, id);
        wrapper.set(FullReduction::getStatus, status);
        return update(wrapper);
    }

    @Override
    public FullReduction getAvailableByProductIds(List<Integer> productIds, List<Integer> categoryIds) {
        Date now = new Date();
        // 1. 查询指定商品的满减活动
        if (CollUtil.isNotEmpty(productIds)) {
            Set<Integer> reductionIds = new HashSet<>();
            for (Integer productId : productIds) {
                reductionIds.addAll(productService.getReductionIdsByProductId(productId));
            }
            if (CollUtil.isNotEmpty(reductionIds)) {
                FullReduction reduction = getAvailableReduction(reductionIds, now);
                if (ObjectUtil.isNotNull(reduction)) {
                    return reduction;
                }
            }
        }
        // 2. 查询品类的满减活动
        if (CollUtil.isNotEmpty(categoryIds)) {
            Set<Integer> reductionIds = new HashSet<>();
            for (Integer categoryId : categoryIds) {
                reductionIds.addAll(productService.getReductionIdsByCategoryId(categoryId));
            }
            if (CollUtil.isNotEmpty(reductionIds)) {
                FullReduction reduction = getAvailableReduction(reductionIds, now);
                if (ObjectUtil.isNotNull(reduction)) {
                    return reduction;
                }
            }
        }
        // 3. 查询全场满减活动
        LambdaQueryWrapper<FullReduction> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FullReduction::getScopeType, 1); // 全场
        wrapper.eq(FullReduction::getStatus, true);
        wrapper.eq(FullReduction::getIsDel, false);
        wrapper.le(FullReduction::getStartTime, now);
        wrapper.ge(FullReduction::getEndTime, now);
        wrapper.orderByDesc(FullReduction::getId);
        wrapper.last("LIMIT 1");
        return getOne(wrapper);
    }

    @Override
    public BigDecimal calculateReduction(FullReduction reduction, BigDecimal totalAmount) {
        if (ObjectUtil.isNull(reduction)) {
            return BigDecimal.ZERO;
        }
        List<FullReductionLevel> levels = levelService.getByReductionId(reduction.getId());
        if (CollUtil.isEmpty(levels)) {
            return BigDecimal.ZERO;
        }
        // 找到满足条件的最高档位
        BigDecimal reduceAmount = BigDecimal.ZERO;
        for (FullReductionLevel level : levels) {
            if (totalAmount.compareTo(level.getFullAmount()) >= 0) {
                reduceAmount = level.getReduceAmount();
            }
        }
        return reduceAmount;
    }

    /**
     * 根据活动ID集合查询可用的活动
     */
    private FullReduction getAvailableReduction(Set<Integer> reductionIds, Date now) {
        LambdaQueryWrapper<FullReduction> wrapper = Wrappers.lambdaQuery();
        wrapper.in(FullReduction::getId, reductionIds);
        wrapper.eq(FullReduction::getStatus, true);
        wrapper.eq(FullReduction::getIsDel, false);
        wrapper.le(FullReduction::getStartTime, now);
        wrapper.ge(FullReduction::getEndTime, now);
        wrapper.orderByDesc(FullReduction::getId);
        wrapper.last("LIMIT 1");
        return getOne(wrapper);
    }

    /**
     * 转换为响应对象
     */
    private FullReductionResponse convertToResponse(FullReduction reduction) {
        FullReductionResponse response = new FullReductionResponse();
        BeanUtils.copyProperties(reduction, response);
        // 设置范围类型名称
        String[] scopeNames = {"", "全场", "品类", "指定商品"};
        response.setScopeTypeName(scopeNames[reduction.getScopeType()]);
        // 计算活动状态
        Date now = new Date();
        if (now.before(reduction.getStartTime())) {
            response.setActivityStatus(0); // 未开始
        } else if (now.after(reduction.getEndTime())) {
            response.setActivityStatus(2); // 已结束
        } else {
            response.setActivityStatus(1); // 进行中
        }
        return response;
    }
}
```

**Step 2: 提交核心 Service 实现**

```bash
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/impl/FullReductionServiceImpl.java
git commit -m "feat(promotion): 实现满减活动核心 Service 逻辑"
```

---

### Task 8: 创建满减活动 Controller

**Files:**
- Create: `crmeb/crmeb-admin/src/main/java/com/zbkj/admin/controller/FullReductionController.java`

**Step 1: 创建 FullReductionController**

```java
package com.zbkj.admin.controller;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.request.FullReductionRequest;
import com.zbkj.common.request.FullReductionSearchRequest;
import com.zbkj.common.response.FullReductionResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.promotion.FullReductionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 满减活动管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("api/admin/promotion/full-reduction")
@Api(tags = "满减活动管理")
public class FullReductionController {

    @Autowired
    private FullReductionService fullReductionService;

    @PreAuthorize("hasAuthority('admin:promotion:full-reduction:list')")
    @ApiOperation(value = "满减活动列表")
    @GetMapping("/list")
    public CommonResult<PageInfo<FullReductionResponse>> getList(@Validated FullReductionSearchRequest request) {
        return CommonResult.success(fullReductionService.getList(request));
    }

    @PreAuthorize("hasAuthority('admin:promotion:full-reduction:info')")
    @ApiOperation(value = "满减活动详情")
    @GetMapping("/detail/{id}")
    public CommonResult<FullReductionResponse> getDetail(@PathVariable Integer id) {
        return CommonResult.success(fullReductionService.getDetail(id));
    }

    @PreAuthorize("hasAuthority('admin:promotion:full-reduction:save')")
    @ApiOperation(value = "新增/编辑满减活动")
    @PostMapping("/save")
    public CommonResult<Boolean> save(@RequestBody @Validated FullReductionRequest request) {
        return CommonResult.success(fullReductionService.save(request));
    }

    @PreAuthorize("hasAuthority('admin:promotion:full-reduction:delete')")
    @ApiOperation(value = "删除满减活动")
    @PostMapping("/delete/{id}")
    public CommonResult<Boolean> delete(@PathVariable Integer id) {
        return CommonResult.success(fullReductionService.delete(id));
    }

    @PreAuthorize("hasAuthority('admin:promotion:full-reduction:status')")
    @ApiOperation(value = "更新满减活动状态")
    @PostMapping("/updateStatus")
    public CommonResult<Boolean> updateStatus(@RequestParam Integer id, @RequestParam Boolean status) {
        return CommonResult.success(fullReductionService.updateStatus(id, status));
    }
}
```

**Step 2: 提交 Controller**

```bash
git add crmeb/crmeb-admin/src/main/java/com/zbkj/admin/controller/FullReductionController.java
git commit -m "feat(promotion): 添加满减活动 Controller"
```

---

### Task 9: 编译验证满减活动后端代码

**Step 1: 编译全部后端模块**

Run: `cd crmeb && mvn clean compile -DskipTests`

Expected: BUILD SUCCESS

**Step 2: 修复编译错误（如有）**

根据编译输出修复任何语法或导入错误。

**Step 3: 提交修复（如有）**

```bash
git add -A
git commit -m "fix(promotion): 修复满减活动编译错误"
```

---

## 阶段二：买赠活动（Day 4-6）

### Task 10: 创建买赠活动数据库表

**Files:**
- Modify: `crmeb/crmeb-common/src/main/resources/sql/promotion_tables.sql`

**Step 1: 追加买赠活动 SQL**

```sql
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
```

**Step 2: 执行 SQL 创建表**

手动在 MySQL 客户端执行上述 SQL。

**Step 3: 提交 SQL 变更**

```bash
git add crmeb/crmeb-common/src/main/resources/sql/promotion_tables.sql
git commit -m "feat(promotion): 添加买赠活动数据库表结构"
```

---

### Task 11: 创建买赠活动实体类

**Files:**
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/model/promotion/BuyGift.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/model/promotion/BuyGiftProduct.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/model/promotion/BuyGiftRecord.java`

**Step 1: 创建 BuyGift 实体类**

```java
package com.zbkj.common.model.promotion;

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
 * 买赠活动主表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_buy_gift")
@ApiModel(value = "BuyGift对象", description = "买赠活动主表")
public class BuyGift implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "活动名称")
    private String name;

    @ApiModelProperty(value = "类型：1-同商品买N送M 2-跨商品买A送B")
    private Integer giftType;

    @ApiModelProperty(value = "开始时间")
    private Date startTime;

    @ApiModelProperty(value = "结束时间")
    private Date endTime;

    @ApiModelProperty(value = "限制类型：0-不限 1-总次数 2-每日次数")
    private Integer limitType;

    @ApiModelProperty(value = "限制次数")
    private Integer limitNum;

    @ApiModelProperty(value = "状态：0-关闭 1-开启")
    private Boolean status;

    @ApiModelProperty(value = "是否删除：0-否 1-是")
    private Boolean isDel;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    private Date updateTime;
}
```

**Step 2: 创建 BuyGiftProduct 实体类**

```java
package com.zbkj.common.model.promotion;

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
 * 买赠商品关联表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_buy_gift_product")
@ApiModel(value = "BuyGiftProduct对象", description = "买赠商品关联表")
public class BuyGiftProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "买赠活动ID")
    private Integer giftId;

    @ApiModelProperty(value = "商品ID")
    private Integer productId;

    @ApiModelProperty(value = "规格ID（0=不限规格）")
    private Integer attrValueId;

    @ApiModelProperty(value = "类型：1-购买商品 2-赠品")
    private Integer productType;

    @ApiModelProperty(value = "购买数量")
    private Integer buyNum;

    @ApiModelProperty(value = "赠送数量")
    private Integer giftNum;
}
```

**Step 3: 创建 BuyGiftRecord 实体类**

```java
package com.zbkj.common.model.promotion;

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
 * 买赠参与记录表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_buy_gift_record")
@ApiModel(value = "BuyGiftRecord对象", description = "买赠参与记录表")
public class BuyGiftRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "买赠活动ID")
    private Integer giftId;

    @ApiModelProperty(value = "用户ID")
    private Integer uid;

    @ApiModelProperty(value = "订单号")
    private String orderId;

    @ApiModelProperty(value = "参与时间")
    private Date createTime;
}
```

**Step 4: 提交实体类**

```bash
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/model/promotion/BuyGift*.java
git commit -m "feat(promotion): 添加买赠活动实体类"
```

---

### Task 12: 创建买赠活动 DAO 和基础 Service

**Files:**
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/promotion/BuyGiftDao.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/promotion/BuyGiftProductDao.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/promotion/BuyGiftRecordDao.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/BuyGiftService.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/BuyGiftProductService.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/BuyGiftRecordService.java`

**Step 1: 创建 DAO 接口**

```java
// BuyGiftDao.java
package com.zbkj.service.dao.promotion;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.promotion.BuyGift;

public interface BuyGiftDao extends BaseMapper<BuyGift> {
}

// BuyGiftProductDao.java
package com.zbkj.service.dao.promotion;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.promotion.BuyGiftProduct;

public interface BuyGiftProductDao extends BaseMapper<BuyGiftProduct> {
}

// BuyGiftRecordDao.java
package com.zbkj.service.dao.promotion;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.promotion.BuyGiftRecord;

public interface BuyGiftRecordDao extends BaseMapper<BuyGiftRecord> {
}
```

**Step 2: 创建 Service 接口**

```java
// BuyGiftService.java
package com.zbkj.service.service.promotion;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.promotion.BuyGift;
import com.zbkj.common.request.BuyGiftRequest;
import com.zbkj.common.request.BuyGiftSearchRequest;
import com.zbkj.common.response.BuyGiftResponse;

import java.util.List;

public interface BuyGiftService extends IService<BuyGift> {
    PageInfo<BuyGiftResponse> getList(BuyGiftSearchRequest request);
    BuyGiftResponse getDetail(Integer id);
    Boolean save(BuyGiftRequest request);
    Boolean delete(Integer id);
    Boolean updateStatus(Integer id, Boolean status);
    BuyGift getAvailableByProductId(Integer productId, Integer uid);
    Boolean checkUserLimit(Integer giftId, Integer uid);
}

// BuyGiftProductService.java
package com.zbkj.service.service.promotion;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.promotion.BuyGiftProduct;
import java.util.List;

public interface BuyGiftProductService extends IService<BuyGiftProduct> {
    List<BuyGiftProduct> getByGiftId(Integer giftId);
    Boolean deleteByGiftId(Integer giftId);
    List<Integer> getGiftIdsByProductId(Integer productId);
}

// BuyGiftRecordService.java
package com.zbkj.service.service.promotion;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.promotion.BuyGiftRecord;

public interface BuyGiftRecordService extends IService<BuyGiftRecord> {
    Integer countByGiftAndUser(Integer giftId, Integer uid);
    Integer countTodayByGiftAndUser(Integer giftId, Integer uid);
    Boolean addRecord(Integer giftId, Integer uid, String orderId);
}
```

**Step 3: 提交 DAO 和 Service 接口**

```bash
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/promotion/BuyGift*.java
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/BuyGift*.java
git commit -m "feat(promotion): 添加买赠活动 DAO 和 Service 接口"
```

---

### Task 13: 创建买赠活动请求/响应对象

**Files:**
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/BuyGiftRequest.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/BuyGiftSearchRequest.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/response/BuyGiftResponse.java`

**Step 1: 创建请求/响应对象**

```java
// BuyGiftRequest.java
package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
@ApiModel(value = "BuyGiftRequest", description = "买赠活动请求对象")
public class BuyGiftRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "活动ID（编辑时必填）")
    private Integer id;

    @ApiModelProperty(value = "活动名称", required = true)
    @NotBlank(message = "活动名称不能为空")
    private String name;

    @ApiModelProperty(value = "类型：1-同商品买N送M 2-跨商品买A送B", required = true)
    @NotNull(message = "请选择活动类型")
    private Integer giftType;

    @ApiModelProperty(value = "开始时间", required = true)
    @NotBlank(message = "请选择开始时间")
    private String startTime;

    @ApiModelProperty(value = "结束时间", required = true)
    @NotBlank(message = "请选择结束时间")
    private String endTime;

    @ApiModelProperty(value = "限制类型：0-不限 1-总次数 2-每日次数")
    private Integer limitType = 0;

    @ApiModelProperty(value = "限制次数")
    private Integer limitNum = 0;

    @ApiModelProperty(value = "购买商品配置", required = true)
    @NotNull(message = "请配置购买商品")
    private List<ProductItem> buyProducts;

    @ApiModelProperty(value = "赠品配置", required = true)
    @NotNull(message = "请配置赠品")
    private List<ProductItem> giftProducts;

    @Data
    public static class ProductItem {
        private Integer productId;
        private Integer attrValueId = 0;
        private Integer buyNum = 0;
        private Integer giftNum = 0;
    }
}

// BuyGiftSearchRequest.java
package com.zbkj.common.request;

import com.zbkj.common.page.PageParamRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "BuyGiftSearchRequest", description = "买赠活动搜索请求")
public class BuyGiftSearchRequest extends PageParamRequest {
    @ApiModelProperty(value = "活动名称")
    private String name;
    @ApiModelProperty(value = "状态")
    private Integer status;
    @ApiModelProperty(value = "类型")
    private Integer giftType;
}

// BuyGiftResponse.java
package com.zbkj.common.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@ApiModel(value = "BuyGiftResponse", description = "买赠活动响应对象")
public class BuyGiftResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String name;
    private Integer giftType;
    private String giftTypeName;
    private Date startTime;
    private Date endTime;
    private Integer limitType;
    private String limitTypeName;
    private Integer limitNum;
    private Boolean status;
    private Integer activityStatus;
    private Date createTime;
    private List<ProductItem> buyProducts;
    private List<ProductItem> giftProducts;

    @Data
    public static class ProductItem {
        private Integer productId;
        private String productName;
        private String productImage;
        private Integer attrValueId;
        private String skuName;
        private Integer buyNum;
        private Integer giftNum;
    }
}
```

**Step 2: 提交请求/响应对象**

```bash
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/request/BuyGift*.java
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/response/BuyGiftResponse.java
git commit -m "feat(promotion): 添加买赠活动请求/响应对象"
```

---

### Task 14: 实现买赠活动 Service 实现类

**Files:**
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/impl/BuyGiftProductServiceImpl.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/impl/BuyGiftRecordServiceImpl.java`

**Step 1: 创建 BuyGiftProductServiceImpl**

```java
package com.zbkj.service.service.promotion.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.model.promotion.BuyGiftProduct;
import com.zbkj.service.dao.promotion.BuyGiftProductDao;
import com.zbkj.service.service.promotion.BuyGiftProductService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 买赠商品关联 Service 实现类
 */
@Service
public class BuyGiftProductServiceImpl extends ServiceImpl<BuyGiftProductDao, BuyGiftProduct>
        implements BuyGiftProductService {

    @Override
    public List<BuyGiftProduct> getByGiftId(Integer giftId) {
        LambdaQueryWrapper<BuyGiftProduct> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(BuyGiftProduct::getGiftId, giftId);
        return list(wrapper);
    }

    @Override
    public Boolean deleteByGiftId(Integer giftId) {
        LambdaQueryWrapper<BuyGiftProduct> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(BuyGiftProduct::getGiftId, giftId);
        return remove(wrapper);
    }

    @Override
    public List<Integer> getGiftIdsByProductId(Integer productId) {
        LambdaQueryWrapper<BuyGiftProduct> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(BuyGiftProduct::getProductId, productId);
        wrapper.eq(BuyGiftProduct::getProductType, 1); // 购买商品
        return list(wrapper).stream()
                .map(BuyGiftProduct::getGiftId)
                .distinct()
                .collect(Collectors.toList());
    }
}
```

**Step 2: 创建 BuyGiftRecordServiceImpl**

```java
package com.zbkj.service.service.promotion.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.model.promotion.BuyGiftRecord;
import com.zbkj.service.dao.promotion.BuyGiftRecordDao;
import com.zbkj.service.service.promotion.BuyGiftRecordService;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 买赠参与记录 Service 实现类
 */
@Service
public class BuyGiftRecordServiceImpl extends ServiceImpl<BuyGiftRecordDao, BuyGiftRecord>
        implements BuyGiftRecordService {

    @Override
    public Integer countByGiftAndUser(Integer giftId, Integer uid) {
        LambdaQueryWrapper<BuyGiftRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(BuyGiftRecord::getGiftId, giftId);
        wrapper.eq(BuyGiftRecord::getUid, uid);
        return count(wrapper);
    }

    @Override
    public Integer countTodayByGiftAndUser(Integer giftId, Integer uid) {
        LambdaQueryWrapper<BuyGiftRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(BuyGiftRecord::getGiftId, giftId);
        wrapper.eq(BuyGiftRecord::getUid, uid);
        // 今日开始时间
        Date todayStart = DateUtil.beginOfDay(new Date());
        Date todayEnd = DateUtil.endOfDay(new Date());
        wrapper.between(BuyGiftRecord::getCreateTime, todayStart, todayEnd);
        return count(wrapper);
    }

    @Override
    public Boolean addRecord(Integer giftId, Integer uid, String orderId) {
        BuyGiftRecord record = new BuyGiftRecord();
        record.setGiftId(giftId);
        record.setUid(uid);
        record.setOrderId(orderId);
        record.setCreateTime(new Date());
        return save(record);
    }
}
```

**Step 3: 提交基础 Service 实现**

```bash
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/impl/BuyGiftProductServiceImpl.java
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/impl/BuyGiftRecordServiceImpl.java
git commit -m "feat(promotion): 添加买赠商品和记录 Service 实现"
```

---

### Task 15: 实现 BuyGiftServiceImpl 核心逻辑

**Files:**
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/impl/BuyGiftServiceImpl.java`

**Step 1: 创建 BuyGiftServiceImpl**

```java
package com.zbkj.service.service.promotion.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.promotion.BuyGift;
import com.zbkj.common.model.promotion.BuyGiftProduct;
import com.zbkj.common.request.BuyGiftRequest;
import com.zbkj.common.request.BuyGiftSearchRequest;
import com.zbkj.common.response.BuyGiftResponse;
import com.zbkj.common.utils.CrmebDateUtil;
import com.zbkj.service.dao.promotion.BuyGiftDao;
import com.zbkj.service.service.StoreProductService;
import com.zbkj.service.service.promotion.BuyGiftProductService;
import com.zbkj.service.service.promotion.BuyGiftRecordService;
import com.zbkj.service.service.promotion.BuyGiftService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 买赠活动 Service 实现类
 */
@Service
public class BuyGiftServiceImpl extends ServiceImpl<BuyGiftDao, BuyGift>
        implements BuyGiftService {

    @Autowired
    private BuyGiftProductService productService;

    @Autowired
    private BuyGiftRecordService recordService;

    @Autowired
    private StoreProductService storeProductService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public PageInfo<BuyGiftResponse> getList(BuyGiftSearchRequest request) {
        PageHelper.startPage(request.getPage(), request.getLimit());
        LambdaQueryWrapper<BuyGift> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(BuyGift::getIsDel, false);
        if (StrUtil.isNotBlank(request.getName())) {
            wrapper.like(BuyGift::getName, request.getName());
        }
        if (ObjectUtil.isNotNull(request.getStatus())) {
            wrapper.eq(BuyGift::getStatus, request.getStatus() == 1);
        }
        if (ObjectUtil.isNotNull(request.getGiftType())) {
            wrapper.eq(BuyGift::getGiftType, request.getGiftType());
        }
        wrapper.orderByDesc(BuyGift::getId);
        List<BuyGift> list = list(wrapper);
        PageInfo<BuyGift> pageInfo = new PageInfo<>(list);

        List<BuyGiftResponse> responseList = list.stream().map(this::convertToResponse).collect(Collectors.toList());
        PageInfo<BuyGiftResponse> resultPage = new PageInfo<>();
        BeanUtils.copyProperties(pageInfo, resultPage, "list");
        resultPage.setList(responseList);
        return resultPage;
    }

    @Override
    public BuyGiftResponse getDetail(Integer id) {
        BuyGift gift = getById(id);
        if (ObjectUtil.isNull(gift) || gift.getIsDel()) {
            throw new CrmebException("买赠活动不存在");
        }
        BuyGiftResponse response = convertToResponse(gift);
        // 获取关联商品
        List<BuyGiftProduct> products = productService.getByGiftId(id);
        List<BuyGiftResponse.ProductItem> buyProducts = new ArrayList<>();
        List<BuyGiftResponse.ProductItem> giftProducts = new ArrayList<>();
        for (BuyGiftProduct p : products) {
            BuyGiftResponse.ProductItem item = new BuyGiftResponse.ProductItem();
            item.setProductId(p.getProductId());
            item.setAttrValueId(p.getAttrValueId());
            item.setBuyNum(p.getBuyNum());
            item.setGiftNum(p.getGiftNum());
            // 获取商品信息
            StoreProduct product = storeProductService.getById(p.getProductId());
            if (product != null) {
                item.setProductName(product.getStoreName());
                item.setProductImage(product.getImage());
            }
            if (p.getProductType() == 1) {
                buyProducts.add(item);
            } else {
                giftProducts.add(item);
            }
        }
        response.setBuyProducts(buyProducts);
        response.setGiftProducts(giftProducts);
        return response;
    }

    @Override
    public Boolean save(BuyGiftRequest request) {
        if (CollUtil.isEmpty(request.getBuyProducts())) {
            throw new CrmebException("请配置购买商品");
        }
        if (CollUtil.isEmpty(request.getGiftProducts())) {
            throw new CrmebException("请配置赠品");
        }

        BuyGift gift = new BuyGift();
        BeanUtils.copyProperties(request, gift);
        gift.setStartTime(CrmebDateUtil.strToDate(request.getStartTime(), "yyyy-MM-dd HH:mm:ss"));
        gift.setEndTime(CrmebDateUtil.strToDate(request.getEndTime(), "yyyy-MM-dd HH:mm:ss"));
        gift.setIsDel(false);
        gift.setStatus(false);

        return transactionTemplate.execute(status -> {
            if (ObjectUtil.isNotNull(request.getId())) {
                gift.setId(request.getId());
                updateById(gift);
                productService.deleteByGiftId(request.getId());
            } else {
                save(gift);
            }
            // 保存购买商品
            List<BuyGiftProduct> productList = new ArrayList<>();
            for (BuyGiftRequest.ProductItem item : request.getBuyProducts()) {
                BuyGiftProduct p = new BuyGiftProduct();
                p.setGiftId(gift.getId());
                p.setProductId(item.getProductId());
                p.setAttrValueId(item.getAttrValueId());
                p.setProductType(1);
                p.setBuyNum(item.getBuyNum());
                p.setGiftNum(0);
                productList.add(p);
            }
            // 保存赠品
            for (BuyGiftRequest.ProductItem item : request.getGiftProducts()) {
                BuyGiftProduct p = new BuyGiftProduct();
                p.setGiftId(gift.getId());
                p.setProductId(item.getProductId());
                p.setAttrValueId(item.getAttrValueId());
                p.setProductType(2);
                p.setBuyNum(0);
                p.setGiftNum(item.getGiftNum());
                productList.add(p);
            }
            productService.saveBatch(productList);
            return true;
        });
    }

    @Override
    public Boolean delete(Integer id) {
        BuyGift gift = getById(id);
        if (ObjectUtil.isNull(gift) || gift.getIsDel()) {
            throw new CrmebException("买赠活动不存在");
        }
        LambdaUpdateWrapper<BuyGift> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(BuyGift::getId, id);
        wrapper.set(BuyGift::getIsDel, true);
        return update(wrapper);
    }

    @Override
    public Boolean updateStatus(Integer id, Boolean status) {
        BuyGift gift = getById(id);
        if (ObjectUtil.isNull(gift) || gift.getIsDel()) {
            throw new CrmebException("买赠活动不存在");
        }
        LambdaUpdateWrapper<BuyGift> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(BuyGift::getId, id);
        wrapper.set(BuyGift::getStatus, status);
        return update(wrapper);
    }

    @Override
    public BuyGift getAvailableByProductId(Integer productId, Integer uid) {
        Date now = new Date();
        List<Integer> giftIds = productService.getGiftIdsByProductId(productId);
        if (CollUtil.isEmpty(giftIds)) {
            return null;
        }
        LambdaQueryWrapper<BuyGift> wrapper = Wrappers.lambdaQuery();
        wrapper.in(BuyGift::getId, giftIds);
        wrapper.eq(BuyGift::getStatus, true);
        wrapper.eq(BuyGift::getIsDel, false);
        wrapper.le(BuyGift::getStartTime, now);
        wrapper.ge(BuyGift::getEndTime, now);
        wrapper.orderByDesc(BuyGift::getId);
        List<BuyGift> gifts = list(wrapper);
        // 过滤已达限制的活动
        for (BuyGift gift : gifts) {
            if (checkUserLimit(gift.getId(), uid)) {
                return gift;
            }
        }
        return null;
    }

    @Override
    public Boolean checkUserLimit(Integer giftId, Integer uid) {
        BuyGift gift = getById(giftId);
        if (gift == null || gift.getLimitType() == 0) {
            return true; // 不限制
        }
        if (gift.getLimitType() == 1) {
            // 总次数限制
            Integer count = recordService.countByGiftAndUser(giftId, uid);
            return count < gift.getLimitNum();
        } else if (gift.getLimitType() == 2) {
            // 每日次数限制
            Integer count = recordService.countTodayByGiftAndUser(giftId, uid);
            return count < gift.getLimitNum();
        }
        return true;
    }

    private BuyGiftResponse convertToResponse(BuyGift gift) {
        BuyGiftResponse response = new BuyGiftResponse();
        BeanUtils.copyProperties(gift, response);
        String[] typeNames = {"", "同商品买N送M", "跨商品买A送B"};
        response.setGiftTypeName(typeNames[gift.getGiftType()]);
        String[] limitNames = {"不限", "总次数限制", "每日次数限制"};
        response.setLimitTypeName(limitNames[gift.getLimitType()]);
        Date now = new Date();
        if (now.before(gift.getStartTime())) {
            response.setActivityStatus(0);
        } else if (now.after(gift.getEndTime())) {
            response.setActivityStatus(2);
        } else {
            response.setActivityStatus(1);
        }
        return response;
    }
}
```

**Step 2: 提交核心 Service 实现**

```bash
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/impl/BuyGiftServiceImpl.java
git commit -m "feat(promotion): 实现买赠活动核心 Service 逻辑"
```

---

### Task 16: 创建买赠活动 Controller

**Files:**
- Create: `crmeb/crmeb-admin/src/main/java/com/zbkj/admin/controller/BuyGiftController.java`

**Step 1: 创建 BuyGiftController**

```java
package com.zbkj.admin.controller;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.request.BuyGiftRequest;
import com.zbkj.common.request.BuyGiftSearchRequest;
import com.zbkj.common.response.BuyGiftResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.promotion.BuyGiftService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 买赠活动管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("api/admin/promotion/buy-gift")
@Api(tags = "买赠活动管理")
public class BuyGiftController {

    @Autowired
    private BuyGiftService buyGiftService;

    @PreAuthorize("hasAuthority('admin:promotion:buy-gift:list')")
    @ApiOperation(value = "买赠活动列表")
    @GetMapping("/list")
    public CommonResult<PageInfo<BuyGiftResponse>> getList(@Validated BuyGiftSearchRequest request) {
        return CommonResult.success(buyGiftService.getList(request));
    }

    @PreAuthorize("hasAuthority('admin:promotion:buy-gift:info')")
    @ApiOperation(value = "买赠活动详情")
    @GetMapping("/detail/{id}")
    public CommonResult<BuyGiftResponse> getDetail(@PathVariable Integer id) {
        return CommonResult.success(buyGiftService.getDetail(id));
    }

    @PreAuthorize("hasAuthority('admin:promotion:buy-gift:save')")
    @ApiOperation(value = "新增/编辑买赠活动")
    @PostMapping("/save")
    public CommonResult<Boolean> save(@RequestBody @Validated BuyGiftRequest request) {
        return CommonResult.success(buyGiftService.save(request));
    }

    @PreAuthorize("hasAuthority('admin:promotion:buy-gift:delete')")
    @ApiOperation(value = "删除买赠活动")
    @PostMapping("/delete/{id}")
    public CommonResult<Boolean> delete(@PathVariable Integer id) {
        return CommonResult.success(buyGiftService.delete(id));
    }

    @PreAuthorize("hasAuthority('admin:promotion:buy-gift:status')")
    @ApiOperation(value = "更新买赠活动状态")
    @PostMapping("/updateStatus")
    public CommonResult<Boolean> updateStatus(@RequestParam Integer id, @RequestParam Boolean status) {
        return CommonResult.success(buyGiftService.updateStatus(id, status));
    }
}
```

**Step 2: 提交 Controller**

```bash
git add crmeb/crmeb-admin/src/main/java/com/zbkj/admin/controller/BuyGiftController.java
git commit -m "feat(promotion): 添加买赠活动 Controller"
```

---

### Task 17: 编译验证买赠活动后端代码

**Step 1: 编译全部后端模块**

Run: `cd crmeb && mvn clean compile -DskipTests`

Expected: BUILD SUCCESS

**Step 2: 修复编译错误（如有）**

**Step 3: 提交修复（如有）**

```bash
git add -A
git commit -m "fix(promotion): 修复买赠活动编译错误"
```

---

## 阶段三：代金券扩展（Day 7-8）

### Task 18: 扩展优惠券表结构

**Files:**
- Modify: `crmeb/crmeb-common/src/main/resources/sql/promotion_tables.sql`

**Step 1: 追加代金券扩展 SQL**

```sql
-- 扩展优惠券表（添加代金券相关字段）
ALTER TABLE eb_store_coupon
ADD COLUMN coupon_type TINYINT(1) DEFAULT 1 COMMENT '券类型：1-优惠券 2-代金券' AFTER id,
ADD COLUMN can_deduct_freight TINYINT(1) DEFAULT 0 COMMENT '是否可抵扣运费：0-否 1-是' AFTER min_price;

-- 扩展订单表（添加代金券和满减相关字段）
ALTER TABLE eb_store_order
ADD COLUMN voucher_id INT(11) DEFAULT 0 COMMENT '代金券ID' AFTER coupon_price,
ADD COLUMN voucher_price DECIMAL(8,2) DEFAULT 0.00 COMMENT '代金券抵扣金额' AFTER voucher_id,
ADD COLUMN full_reduction_id INT(11) DEFAULT 0 COMMENT '满减活动ID' AFTER voucher_price,
ADD COLUMN full_reduction_price DECIMAL(8,2) DEFAULT 0.00 COMMENT '满减金额' AFTER full_reduction_id;
```

**Step 2: 执行 SQL 变更**

手动在 MySQL 客户端执行上述 ALTER TABLE 语句。

**Step 3: 提交 SQL 变更**

```bash
git add crmeb/crmeb-common/src/main/resources/sql/promotion_tables.sql
git commit -m "feat(promotion): 扩展优惠券表和订单表支持代金券"
```

---

### Task 19: 扩展 StoreCoupon 实体类

**Files:**
- Modify: `crmeb/crmeb-common/src/main/java/com/zbkj/common/model/coupon/StoreCoupon.java`

**Step 1: 添加代金券字段**

在 `StoreCoupon.java` 中添加以下字段（在 `id` 字段后）：

```java
@ApiModelProperty(value = "券类型：1-优惠券 2-代金券")
private Integer couponType;
```

在 `minPrice` 字段后添加：

```java
@ApiModelProperty(value = "是否可抵扣运费：0-否 1-是")
private Boolean canDeductFreight;
```

**Step 2: 提交实体类变更**

```bash
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/model/coupon/StoreCoupon.java
git commit -m "feat(promotion): StoreCoupon 实体添加代金券字段"
```

---

### Task 20: 扩展 StoreOrder 实体类

**Files:**
- Modify: `crmeb/crmeb-common/src/main/java/com/zbkj/common/model/order/StoreOrder.java`

**Step 1: 添加促销相关字段**

在 `StoreOrder.java` 中 `couponPrice` 字段后添加以下字段：

```java
@ApiModelProperty(value = "代金券ID")
private Integer voucherId;

@ApiModelProperty(value = "代金券抵扣金额")
private BigDecimal voucherPrice;

@ApiModelProperty(value = "满减活动ID")
private Integer fullReductionId;

@ApiModelProperty(value = "满减金额")
private BigDecimal fullReductionPrice;
```

**Step 2: 提交实体类变更**

```bash
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/model/order/StoreOrder.java
git commit -m "feat(promotion): StoreOrder 实体添加代金券和满减字段"
```

---

### Task 21: 扩展优惠券请求/响应对象

**Files:**
- Modify: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/StoreCouponRequest.java`
- Modify: `crmeb/crmeb-common/src/main/java/com/zbkj/common/response/StoreCouponInfoResponse.java`

**Step 1: 扩展 StoreCouponRequest**

在请求对象中添加：

```java
@ApiModelProperty(value = "券类型：1-优惠券 2-代金券")
private Integer couponType = 1;

@ApiModelProperty(value = "是否可抵扣运费：0-否 1-是（仅代金券有效）")
private Boolean canDeductFreight = false;
```

**Step 2: 扩展 StoreCouponInfoResponse**

在响应对象中添加：

```java
@ApiModelProperty(value = "券类型：1-优惠券 2-代金券")
private Integer couponType;

@ApiModelProperty(value = "券类型名称")
private String couponTypeName;

@ApiModelProperty(value = "是否可抵扣运费")
private Boolean canDeductFreight;
```

**Step 3: 提交变更**

```bash
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/request/StoreCouponRequest.java
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/response/StoreCouponInfoResponse.java
git commit -m "feat(promotion): 扩展优惠券请求/响应对象支持代金券"
```

---

### Task 22: 扩展 StoreCouponService 支持代金券

**Files:**
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/StoreCouponService.java`
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/StoreCouponServiceImpl.java`

**Step 1: 扩展 Service 接口**

在 `StoreCouponService.java` 中添加方法：

```java
/**
 * 获取代金券列表（后台）
 */
PageInfo<StoreCouponInfoResponse> getVoucherList(StoreCouponSearchRequest request);

/**
 * 获取用户可用的代金券列表
 */
List<StoreCouponUserResponse> getUserAvailableVouchers(Integer uid, BigDecimal totalPrice);
```

**Step 2: 扩展 Service 实现**

在 `StoreCouponServiceImpl.java` 中实现：

```java
@Override
public PageInfo<StoreCouponInfoResponse> getVoucherList(StoreCouponSearchRequest request) {
    PageHelper.startPage(request.getPage(), request.getLimit());
    LambdaQueryWrapper<StoreCoupon> wrapper = Wrappers.lambdaQuery();
    wrapper.eq(StoreCoupon::getCouponType, 2); // 代金券
    wrapper.eq(StoreCoupon::getIsDel, false);
    if (StrUtil.isNotBlank(request.getName())) {
        wrapper.like(StoreCoupon::getName, request.getName());
    }
    if (ObjectUtil.isNotNull(request.getStatus())) {
        wrapper.eq(StoreCoupon::getStatus, request.getStatus());
    }
    wrapper.orderByDesc(StoreCoupon::getId);
    List<StoreCoupon> list = dao.selectList(wrapper);
    // 转换为响应对象...
    return convertToResponsePage(list);
}

@Override
public List<StoreCouponUserResponse> getUserAvailableVouchers(Integer uid, BigDecimal totalPrice) {
    // 查询用户持有的未使用代金券
    LambdaQueryWrapper<StoreCouponUser> wrapper = Wrappers.lambdaQuery();
    wrapper.eq(StoreCouponUser::getUid, uid);
    wrapper.eq(StoreCouponUser::getStatus, 0); // 未使用
    List<StoreCouponUser> userCoupons = storeCouponUserService.list(wrapper);

    List<StoreCouponUserResponse> result = new ArrayList<>();
    Date now = new Date();
    for (StoreCouponUser userCoupon : userCoupons) {
        StoreCoupon coupon = getById(userCoupon.getCouponId());
        if (coupon == null || coupon.getCouponType() != 2) {
            continue; // 不是代金券
        }
        // 检查有效期
        if (now.before(userCoupon.getStartTime()) || now.after(userCoupon.getEndTime())) {
            continue;
        }
        // 代金券无门槛，不检查 minPrice
        StoreCouponUserResponse response = new StoreCouponUserResponse();
        BeanUtils.copyProperties(userCoupon, response);
        response.setName(coupon.getName());
        response.setMoney(coupon.getMoney());
        response.setCanDeductFreight(coupon.getCanDeductFreight());
        result.add(response);
    }
    return result;
}
```

**Step 3: 提交 Service 变更**

```bash
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/StoreCouponService.java
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/StoreCouponServiceImpl.java
git commit -m "feat(promotion): 扩展 StoreCouponService 支持代金券"
```

---

### Task 23: 创建代金券管理 Controller

**Files:**
- Create: `crmeb/crmeb-admin/src/main/java/com/zbkj/admin/controller/VoucherController.java`

**Step 1: 创建 VoucherController**

```java
package com.zbkj.admin.controller;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.request.StoreCouponRequest;
import com.zbkj.common.request.StoreCouponSearchRequest;
import com.zbkj.common.response.StoreCouponInfoResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.StoreCouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 代金券管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("api/admin/marketing/voucher")
@Api(tags = "代金券管理")
public class VoucherController {

    @Autowired
    private StoreCouponService storeCouponService;

    @PreAuthorize("hasAuthority('admin:marketing:voucher:list')")
    @ApiOperation(value = "代金券列表")
    @GetMapping("/list")
    public CommonResult<PageInfo<StoreCouponInfoResponse>> getList(@Validated StoreCouponSearchRequest request) {
        return CommonResult.success(storeCouponService.getVoucherList(request));
    }

    @PreAuthorize("hasAuthority('admin:marketing:voucher:info')")
    @ApiOperation(value = "代金券详情")
    @GetMapping("/info/{id}")
    public CommonResult<StoreCouponInfoResponse> getInfo(@PathVariable Integer id) {
        return CommonResult.success(storeCouponService.getInfoException(id));
    }

    @PreAuthorize("hasAuthority('admin:marketing:voucher:save')")
    @ApiOperation(value = "新增/编辑代金券")
    @PostMapping("/save")
    public CommonResult<Boolean> save(@RequestBody @Validated StoreCouponRequest request) {
        // 强制设置为代金券类型
        request.setCouponType(2);
        return CommonResult.success(storeCouponService.create(request));
    }

    @PreAuthorize("hasAuthority('admin:marketing:voucher:delete')")
    @ApiOperation(value = "删除代金券")
    @PostMapping("/delete/{id}")
    public CommonResult<Boolean> delete(@PathVariable Integer id) {
        return CommonResult.success(storeCouponService.delete(id));
    }

    @PreAuthorize("hasAuthority('admin:marketing:voucher:send')")
    @ApiOperation(value = "发放代金券给用户")
    @PostMapping("/send")
    public CommonResult<Boolean> send(@RequestParam Integer couponId, @RequestParam String userIds) {
        return CommonResult.success(storeCouponService.sendCouponToUsers(couponId, userIds));
    }
}
```

**Step 2: 提交 Controller**

```bash
git add crmeb/crmeb-admin/src/main/java/com/zbkj/admin/controller/VoucherController.java
git commit -m "feat(promotion): 添加代金券管理 Controller"
```

---

### Task 24: 编译验证代金券后端代码

**Step 1: 编译全部后端模块**

Run: `cd crmeb && mvn clean compile -DskipTests`

Expected: BUILD SUCCESS

**Step 2: 修复编译错误（如有）**

**Step 3: 提交修复（如有）**

```bash
git add -A
git commit -m "fix(promotion): 修复代金券编译错误"
```

---

## 阶段四：订单价格计算集成（Day 9-10）

### Task 25: 创建促销计算服务

**Files:**
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/PromotionCalculateService.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/impl/PromotionCalculateServiceImpl.java`

**Step 1: 创建 PromotionCalculateService 接口**

```java
package com.zbkj.service.service.promotion;

import com.zbkj.common.model.promotion.BuyGift;
import com.zbkj.common.model.promotion.FullReduction;
import com.zbkj.common.vo.MyRecord;

import java.math.BigDecimal;
import java.util.List;

/**
 * 促销计算服务
 */
public interface PromotionCalculateService {

    /**
     * 计算满减金额
     * @param productIds 商品ID列表
     * @param categoryIds 品类ID列表
     * @param totalAmount 商品总金额
     * @return 满减结果 {reduction: FullReduction, reduceAmount: BigDecimal}
     */
    MyRecord calculateFullReduction(List<Integer> productIds, List<Integer> categoryIds, BigDecimal totalAmount);

    /**
     * 计算代金券抵扣金额
     * @param voucherId 代金券ID
     * @param productPayable 商品应付金额（扣除满减和优惠券后）
     * @param freightFee 运费
     * @return 代金券抵扣金额
     */
    BigDecimal calculateVoucherDeduction(Integer voucherId, BigDecimal productPayable, BigDecimal freightFee);

    /**
     * 检查满减活动是否允许使用优惠券
     */
    Boolean isAllowCoupon(Integer fullReductionId);

    /**
     * 获取商品的买赠活动
     */
    BuyGift getProductBuyGift(Integer productId, Integer uid);

    /**
     * 计算买赠赠品
     * @param giftId 买赠活动ID
     * @param productId 购买商品ID
     * @param buyQuantity 购买数量
     * @return 赠品信息列表
     */
    List<MyRecord> calculateGiftProducts(Integer giftId, Integer productId, Integer buyQuantity);
}
```

**Step 2: 创建 PromotionCalculateServiceImpl 实现**

```java
package com.zbkj.service.service.promotion.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.zbkj.common.model.coupon.StoreCoupon;
import com.zbkj.common.model.coupon.StoreCouponUser;
import com.zbkj.common.model.promotion.BuyGift;
import com.zbkj.common.model.promotion.BuyGiftProduct;
import com.zbkj.common.model.promotion.FullReduction;
import com.zbkj.common.vo.MyRecord;
import com.zbkj.service.service.StoreCouponService;
import com.zbkj.service.service.StoreCouponUserService;
import com.zbkj.service.service.promotion.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 促销计算服务实现
 */
@Service
public class PromotionCalculateServiceImpl implements PromotionCalculateService {

    @Autowired
    private FullReductionService fullReductionService;

    @Autowired
    private BuyGiftService buyGiftService;

    @Autowired
    private BuyGiftProductService buyGiftProductService;

    @Autowired
    private StoreCouponUserService storeCouponUserService;

    @Autowired
    private StoreCouponService storeCouponService;

    @Override
    public MyRecord calculateFullReduction(List<Integer> productIds, List<Integer> categoryIds, BigDecimal totalAmount) {
        MyRecord result = new MyRecord();
        result.set("reduction", null);
        result.set("reduceAmount", BigDecimal.ZERO);

        // 查找可用的满减活动
        FullReduction reduction = fullReductionService.getAvailableByProductIds(productIds, categoryIds);
        if (ObjectUtil.isNull(reduction)) {
            return result;
        }

        // 计算满减金额
        BigDecimal reduceAmount = fullReductionService.calculateReduction(reduction, totalAmount);
        result.set("reduction", reduction);
        result.set("reduceAmount", reduceAmount);
        return result;
    }

    @Override
    public BigDecimal calculateVoucherDeduction(Integer voucherId, BigDecimal productPayable, BigDecimal freightFee) {
        if (ObjectUtil.isNull(voucherId) || voucherId <= 0) {
            return BigDecimal.ZERO;
        }

        // 获取用户代金券
        StoreCouponUser couponUser = storeCouponUserService.getById(voucherId);
        if (ObjectUtil.isNull(couponUser)) {
            return BigDecimal.ZERO;
        }

        // 获取代金券信息
        StoreCoupon coupon = storeCouponService.getById(couponUser.getCouponId());
        if (ObjectUtil.isNull(coupon) || coupon.getCouponType() != 2) {
            return BigDecimal.ZERO; // 不是代金券
        }

        BigDecimal voucherMoney = coupon.getMoney();

        if (coupon.getCanDeductFreight() != null && coupon.getCanDeductFreight()) {
            // 代金券可抵扣运费：可抵扣总金额 = 商品应付 + 运费
            BigDecimal totalPayable = productPayable.add(freightFee);
            if (voucherMoney.compareTo(totalPayable) >= 0) {
                return totalPayable; // 代金券足够抵扣全部
            } else {
                return voucherMoney;
            }
        } else {
            // 不可抵扣运费：只抵扣商品
            if (voucherMoney.compareTo(productPayable) >= 0) {
                return productPayable;
            } else {
                return voucherMoney;
            }
        }
    }

    @Override
    public Boolean isAllowCoupon(Integer fullReductionId) {
        if (ObjectUtil.isNull(fullReductionId) || fullReductionId <= 0) {
            return true;
        }
        FullReduction reduction = fullReductionService.getById(fullReductionId);
        if (ObjectUtil.isNull(reduction)) {
            return true;
        }
        return reduction.getAllowCoupon();
    }

    @Override
    public BuyGift getProductBuyGift(Integer productId, Integer uid) {
        return buyGiftService.getAvailableByProductId(productId, uid);
    }

    @Override
    public List<MyRecord> calculateGiftProducts(Integer giftId, Integer productId, Integer buyQuantity) {
        List<MyRecord> giftList = new ArrayList<>();

        BuyGift gift = buyGiftService.getById(giftId);
        if (ObjectUtil.isNull(gift)) {
            return giftList;
        }

        List<BuyGiftProduct> products = buyGiftProductService.getByGiftId(giftId);
        if (CollUtil.isEmpty(products)) {
            return giftList;
        }

        // 获取购买商品配置
        BuyGiftProduct buyConfig = products.stream()
                .filter(p -> p.getProductType() == 1 && p.getProductId().equals(productId))
                .findFirst()
                .orElse(null);

        if (buyConfig == null || buyConfig.getBuyNum() <= 0) {
            return giftList;
        }

        // 计算可赠送次数
        int giftTimes = buyQuantity / buyConfig.getBuyNum();
        if (giftTimes <= 0) {
            return giftList;
        }

        // 获取赠品配置
        List<BuyGiftProduct> giftConfigs = products.stream()
                .filter(p -> p.getProductType() == 2)
                .toList();

        if (gift.getGiftType() == 1) {
            // 同商品买N送M：赠品就是购买商品
            for (BuyGiftProduct giftConfig : giftConfigs) {
                MyRecord record = new MyRecord();
                record.set("productId", productId);
                record.set("attrValueId", buyConfig.getAttrValueId());
                record.set("giftNum", giftConfig.getGiftNum() * giftTimes);
                giftList.add(record);
            }
        } else {
            // 跨商品买A送B
            for (BuyGiftProduct giftConfig : giftConfigs) {
                MyRecord record = new MyRecord();
                record.set("productId", giftConfig.getProductId());
                record.set("attrValueId", giftConfig.getAttrValueId());
                record.set("giftNum", giftConfig.getGiftNum() * giftTimes);
                giftList.add(record);
            }
        }

        return giftList;
    }
}
```

**Step 3: 提交促销计算服务**

```bash
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/PromotionCalculateService.java
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/impl/PromotionCalculateServiceImpl.java
git commit -m "feat(promotion): 添加促销计算服务"
```

---

### Task 26: 扩展订单计算请求对象

**Files:**
- Modify: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/OrderComputedPriceRequest.java`

**Step 1: 添加促销相关字段**

在订单计算请求对象中添加：

```java
@ApiModelProperty(value = "代金券ID（用户持有的代金券记录ID）")
private Integer voucherId;
```

**Step 2: 提交变更**

```bash
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/request/OrderComputedPriceRequest.java
git commit -m "feat(promotion): 订单计算请求添加代金券字段"
```

---

### Task 27: 扩展订单计算响应对象

**Files:**
- Modify: `crmeb/crmeb-common/src/main/java/com/zbkj/common/response/ComputedOrderPriceResponse.java`

**Step 1: 添加促销相关字段**

在订单计算响应对象中添加：

```java
@ApiModelProperty(value = "满减活动ID")
private Integer fullReductionId;

@ApiModelProperty(value = "满减活动名称")
private String fullReductionName;

@ApiModelProperty(value = "满减金额")
private BigDecimal fullReductionPrice;

@ApiModelProperty(value = "代金券ID")
private Integer voucherId;

@ApiModelProperty(value = "代金券抵扣金额")
private BigDecimal voucherPrice;

@ApiModelProperty(value = "是否允许使用优惠券（满减活动配置）")
private Boolean allowCoupon;

@ApiModelProperty(value = "买赠赠品列表")
private List<GiftProductVo> giftProducts;

@Data
public static class GiftProductVo {
    private Integer productId;
    private String productName;
    private String productImage;
    private Integer attrValueId;
    private String skuName;
    private Integer giftNum;
}
```

**Step 2: 提交变更**

```bash
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/response/ComputedOrderPriceResponse.java
git commit -m "feat(promotion): 订单计算响应添加促销相关字段"
```

---

### Task 28: 修改 OrderServiceImpl 集成促销计算

**Files:**
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/OrderServiceImpl.java`

**Step 1: 注入促销计算服务**

在 `OrderServiceImpl` 中注入：

```java
@Autowired
private PromotionCalculateService promotionCalculateService;
```

**Step 2: 修改 computedPrice 方法**

在 `computedPrice` 方法中，找到优惠券计算逻辑的位置，按以下顺序插入促销计算：

```java
// === 促销计算开始 ===

// 1. 计算满减
List<Integer> productIds = orderInfoList.stream()
    .map(StoreOrderInfo::getProductId)
    .collect(Collectors.toList());
List<Integer> categoryIds = orderInfoList.stream()
    .map(info -> storeProductService.getById(info.getProductId()).getCateId())
    .distinct()
    .collect(Collectors.toList());

MyRecord fullReductionResult = promotionCalculateService.calculateFullReduction(
    productIds, categoryIds, proTotalFee);
FullReduction fullReduction = fullReductionResult.get("reduction");
BigDecimal fullReductionPrice = fullReductionResult.getBigDecimal("reduceAmount");

response.setFullReductionId(fullReduction != null ? fullReduction.getId() : 0);
response.setFullReductionName(fullReduction != null ? fullReduction.getName() : "");
response.setFullReductionPrice(fullReductionPrice);

// 商品应付金额（扣除满减后）
BigDecimal productPayableAfterReduction = proTotalFee.subtract(fullReductionPrice);

// 2. 检查是否允许使用优惠券
Boolean allowCoupon = promotionCalculateService.isAllowCoupon(
    fullReduction != null ? fullReduction.getId() : null);
response.setAllowCoupon(allowCoupon);

// 如果满减活动不允许叠加优惠券，强制清除优惠券
if (!allowCoupon && request.getCouponId() != null && request.getCouponId() > 0) {
    request.setCouponId(0);
}

// 3. 计算优惠券（原有逻辑，但基于扣除满减后的金额）
BigDecimal couponPrice = BigDecimal.ZERO;
if (allowCoupon && request.getCouponId() != null && request.getCouponId() > 0) {
    // ... 原有优惠券计算逻辑 ...
}

// 商品应付金额（扣除满减和优惠券后）
BigDecimal productPayableAfterCoupon = productPayableAfterReduction.subtract(couponPrice);

// 4. 计算代金券（可与优惠券叠加）
BigDecimal voucherPrice = promotionCalculateService.calculateVoucherDeduction(
    request.getVoucherId(), productPayableAfterCoupon, freightFee);
response.setVoucherId(request.getVoucherId() != null ? request.getVoucherId() : 0);
response.setVoucherPrice(voucherPrice);

// 5. 计算买赠赠品
List<ComputedOrderPriceResponse.GiftProductVo> giftProducts = new ArrayList<>();
for (StoreOrderInfo orderInfo : orderInfoList) {
    BuyGift buyGift = promotionCalculateService.getProductBuyGift(orderInfo.getProductId(), uid);
    if (buyGift != null) {
        List<MyRecord> gifts = promotionCalculateService.calculateGiftProducts(
            buyGift.getId(), orderInfo.getProductId(), orderInfo.getPayNum());
        for (MyRecord gift : gifts) {
            ComputedOrderPriceResponse.GiftProductVo giftVo = new ComputedOrderPriceResponse.GiftProductVo();
            giftVo.setProductId(gift.getInt("productId"));
            giftVo.setGiftNum(gift.getInt("giftNum"));
            // 获取商品信息填充名称和图片
            StoreProduct product = storeProductService.getById(gift.getInt("productId"));
            if (product != null) {
                giftVo.setProductName(product.getStoreName());
                giftVo.setProductImage(product.getImage());
            }
            giftProducts.add(giftVo);
        }
    }
}
response.setGiftProducts(giftProducts);

// === 促销计算结束 ===

// 6. 最终实付金额计算（修改原有逻辑）
// payPrice = 商品金额 - 满减 - 优惠券 - 代金券 - 积分抵扣 + 运费
BigDecimal payPrice = proTotalFee
    .subtract(fullReductionPrice)
    .subtract(couponPrice)
    .subtract(voucherPrice)
    .subtract(deductionPrice) // 积分抵扣
    .add(freightFee);

// 确保不为负数
if (payPrice.compareTo(BigDecimal.ZERO) < 0) {
    payPrice = BigDecimal.ZERO;
}

response.setPayPrice(payPrice);
```

**Step 3: 提交变更**

```bash
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/OrderServiceImpl.java
git commit -m "feat(promotion): OrderServiceImpl 集成促销价格计算"
```

---

### Task 29: 修改订单创建逻辑保存促销信息

**Files:**
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/OrderServiceImpl.java`

**Step 1: 在订单创建方法中保存促销信息**

在 `createOrder` 方法中，创建订单时保存促销信息：

```java
// 保存促销信息到订单
storeOrder.setFullReductionId(computedResult.getFullReductionId());
storeOrder.setFullReductionPrice(computedResult.getFullReductionPrice());
storeOrder.setVoucherId(computedResult.getVoucherId());
storeOrder.setVoucherPrice(computedResult.getVoucherPrice());

// 标记代金券已使用
if (computedResult.getVoucherId() != null && computedResult.getVoucherId() > 0) {
    storeCouponUserService.useCoupon(computedResult.getVoucherId());
}

// 记录买赠活动参与（如有赠品）
if (CollUtil.isNotEmpty(computedResult.getGiftProducts())) {
    // 为每个涉及买赠的商品记录参与
    for (StoreOrderInfo orderInfo : orderInfoList) {
        BuyGift buyGift = promotionCalculateService.getProductBuyGift(orderInfo.getProductId(), uid);
        if (buyGift != null) {
            buyGiftRecordService.addRecord(buyGift.getId(), uid, storeOrder.getOrderId());
        }
    }
}

// 处理赠品：将赠品加入订单明细（价格为0）
if (CollUtil.isNotEmpty(computedResult.getGiftProducts())) {
    for (ComputedOrderPriceResponse.GiftProductVo gift : computedResult.getGiftProducts()) {
        StoreOrderInfo giftInfo = new StoreOrderInfo();
        giftInfo.setOrderId(storeOrder.getId());
        giftInfo.setProductId(gift.getProductId());
        giftInfo.setPayNum(gift.getGiftNum());
        giftInfo.setPrice(BigDecimal.ZERO); // 赠品价格为0
        giftInfo.setIsGift(true); // 标记为赠品
        // ... 其他必要字段 ...
        storeOrderInfoService.save(giftInfo);

        // 扣减赠品库存（使用进销存服务）
        stockService.salesOut(gift.getProductId(), gift.getAttrValueId(), gift.getGiftNum(),
            storeOrder.getOrderId(), "买赠赠品出库");
    }
}
```

**Step 2: 提交变更**

```bash
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/OrderServiceImpl.java
git commit -m "feat(promotion): 订单创建保存促销信息和处理赠品"
```

---

### Task 30: 扩展 StoreOrderInfo 实体支持赠品标记

**Files:**
- Modify: `crmeb/crmeb-common/src/main/java/com/zbkj/common/model/order/StoreOrderInfo.java`
- Modify: `crmeb/crmeb-common/src/main/resources/sql/promotion_tables.sql`

**Step 1: 添加赠品标记字段 SQL**

```sql
-- 订单明细表添加赠品标记
ALTER TABLE eb_store_order_info
ADD COLUMN is_gift TINYINT(1) DEFAULT 0 COMMENT '是否赠品：0-否 1-是' AFTER price;
```

**Step 2: 扩展实体类**

在 `StoreOrderInfo.java` 中添加：

```java
@ApiModelProperty(value = "是否赠品：0-否 1-是")
private Boolean isGift;
```

**Step 3: 提交变更**

```bash
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/model/order/StoreOrderInfo.java
git add crmeb/crmeb-common/src/main/resources/sql/promotion_tables.sql
git commit -m "feat(promotion): StoreOrderInfo 添加赠品标记字段"
```

---

### Task 31: 编译验证订单集成代码

**Step 1: 编译全部后端模块**

Run: `cd crmeb && mvn clean compile -DskipTests`

Expected: BUILD SUCCESS

**Step 2: 修复编译错误（如有）**

**Step 3: 提交修复（如有）**

```bash
git add -A
git commit -m "fix(promotion): 修复订单集成编译错误"
```

---

## 阶段五：Admin 前端页面开发（Day 11-13）

### Task 32: 添加促销活动路由配置

**Files:**
- Modify: `admin/src/router/modules/marketing.js`

**Step 1: 添加促销活动路由**

在 `marketingRouter` 中添加：

```javascript
{
  path: 'promotion',
  name: 'promotion',
  meta: {
    title: '促销活动'
  },
  component: () => import('@/views/marketing/promotion/index'),
  children: [
    {
      path: 'fullReduction',
      name: 'fullReduction',
      meta: {
        title: '满减活动',
        noCache: true
      },
      component: () => import('@/views/marketing/promotion/fullReduction/index')
    },
    {
      path: 'buyGift',
      name: 'buyGift',
      meta: {
        title: '买赠活动',
        noCache: true
      },
      component: () => import('@/views/marketing/promotion/buyGift/index')
    }
  ]
},
{
  path: 'voucher',
  name: 'voucher',
  meta: {
    title: '代金券列表',
    noCache: true
  },
  component: () => import('@/views/marketing/voucher/index')
}
```

**Step 2: 提交路由配置**

```bash
git add admin/src/router/modules/marketing.js
git commit -m "feat(admin): 添加促销活动路由配置"
```

---

### Task 33: 添加促销活动 API 封装

**Files:**
- Create: `admin/src/api/promotion.js`
- Modify: `admin/src/api/marketing.js`

**Step 1: 创建 promotion.js API 文件**

```javascript
import request from '@/utils/request'

/**
 * 满减活动 API
 */
export function fullReductionListApi(params) {
  return request({
    url: '/api/admin/promotion/full-reduction/list',
    method: 'get',
    params
  })
}

export function fullReductionDetailApi(id) {
  return request({
    url: `/api/admin/promotion/full-reduction/detail/${id}`,
    method: 'get'
  })
}

export function fullReductionSaveApi(data) {
  return request({
    url: '/api/admin/promotion/full-reduction/save',
    method: 'post',
    data
  })
}

export function fullReductionDeleteApi(id) {
  return request({
    url: `/api/admin/promotion/full-reduction/delete/${id}`,
    method: 'post'
  })
}

export function fullReductionUpdateStatusApi(id, status) {
  return request({
    url: '/api/admin/promotion/full-reduction/updateStatus',
    method: 'post',
    params: { id, status }
  })
}

/**
 * 买赠活动 API
 */
export function buyGiftListApi(params) {
  return request({
    url: '/api/admin/promotion/buy-gift/list',
    method: 'get',
    params
  })
}

export function buyGiftDetailApi(id) {
  return request({
    url: `/api/admin/promotion/buy-gift/detail/${id}`,
    method: 'get'
  })
}

export function buyGiftSaveApi(data) {
  return request({
    url: '/api/admin/promotion/buy-gift/save',
    method: 'post',
    data
  })
}

export function buyGiftDeleteApi(id) {
  return request({
    url: `/api/admin/promotion/buy-gift/delete/${id}`,
    method: 'post'
  })
}

export function buyGiftUpdateStatusApi(id, status) {
  return request({
    url: '/api/admin/promotion/buy-gift/updateStatus',
    method: 'post',
    params: { id, status }
  })
}
```

**Step 2: 在 marketing.js 中添加代金券 API**

```javascript
/**
 * 代金券 API
 */
export function voucherListApi(params) {
  return request({
    url: '/api/admin/marketing/voucher/list',
    method: 'get',
    params
  })
}

export function voucherInfoApi(id) {
  return request({
    url: `/api/admin/marketing/voucher/info/${id}`,
    method: 'get'
  })
}

export function voucherSaveApi(data) {
  return request({
    url: '/api/admin/marketing/voucher/save',
    method: 'post',
    data
  })
}

export function voucherDeleteApi(id) {
  return request({
    url: `/api/admin/marketing/voucher/delete/${id}`,
    method: 'post'
  })
}

export function voucherSendApi(couponId, userIds) {
  return request({
    url: '/api/admin/marketing/voucher/send',
    method: 'post',
    params: { couponId, userIds }
  })
}
```

**Step 3: 提交 API 封装**

```bash
git add admin/src/api/promotion.js
git add admin/src/api/marketing.js
git commit -m "feat(admin): 添加促销活动和代金券 API 封装"
```

---

### Task 34: 创建满减活动列表页面

**Files:**
- Create: `admin/src/views/marketing/promotion/index.vue`
- Create: `admin/src/views/marketing/promotion/fullReduction/index.vue`

**Step 1: 创建促销活动容器页面**

```vue
<!-- admin/src/views/marketing/promotion/index.vue -->
<template>
  <div class="app-container">
    <router-view />
  </div>
</template>
```

**Step 2: 创建满减活动列表页面**

```vue
<!-- admin/src/views/marketing/promotion/fullReduction/index.vue -->
<template>
  <div class="divBox">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>满减活动</span>
        <el-button type="primary" size="small" @click="handleAdd">
          添加满减活动
        </el-button>
      </div>
      <!-- 搜索区域 -->
      <el-form :inline="true" :model="tableFrom" class="demo-form-inline">
        <el-form-item label="活动名称">
          <el-input v-model="tableFrom.name" placeholder="活动名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="tableFrom.status" placeholder="状态" clearable>
            <el-option label="开启" :value="1" />
            <el-option label="关闭" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="范围">
          <el-select v-model="tableFrom.scopeType" placeholder="范围" clearable>
            <el-option label="全场" :value="1" />
            <el-option label="品类" :value="2" />
            <el-option label="指定商品" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="getList">搜索</el-button>
        </el-form-item>
      </el-form>
      <!-- 表格区域 -->
      <el-table v-loading="listLoading" :data="tableData.data" border>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="活动名称" min-width="150" />
        <el-table-column prop="scopeTypeName" label="范围" width="100" />
        <el-table-column label="活动时间" min-width="180">
          <template slot-scope="scope">
            {{ scope.row.startTime | formatDate }} - {{ scope.row.endTime | formatDate }}
          </template>
        </el-table-column>
        <el-table-column label="活动状态" width="100">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.activityStatus === 0" type="info">未开始</el-tag>
            <el-tag v-else-if="scope.row.activityStatus === 1" type="success">进行中</el-tag>
            <el-tag v-else type="danger">已结束</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开启状态" width="100">
          <template slot-scope="scope">
            <el-switch
              v-model="scope.row.status"
              @change="handleStatusChange(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="叠加优惠券" width="100">
          <template slot-scope="scope">
            <span>{{ scope.row.allowCoupon ? '允许' : '不允许' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="text" class="red" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <!-- 分页 -->
      <el-pagination
        :current-page="tableFrom.page"
        :page-sizes="[10, 20, 50]"
        :page-size="tableFrom.limit"
        layout="total, sizes, prev, pager, next, jumper"
        :total="tableData.total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>
    <!-- 编辑弹窗 -->
    <full-reduction-form
      v-if="dialogVisible"
      :visible.sync="dialogVisible"
      :edit-data="editData"
      @success="getList"
    />
  </div>
</template>

<script>
import { fullReductionListApi, fullReductionDeleteApi, fullReductionUpdateStatusApi } from '@/api/promotion'
import FullReductionForm from './components/form'

export default {
  name: 'FullReduction',
  components: { FullReductionForm },
  data() {
    return {
      tableFrom: {
        page: 1,
        limit: 10,
        name: '',
        status: '',
        scopeType: ''
      },
      tableData: {
        data: [],
        total: 0
      },
      listLoading: false,
      dialogVisible: false,
      editData: null
    }
  },
  mounted() {
    this.getList()
  },
  methods: {
    async getList() {
      this.listLoading = true
      try {
        const res = await fullReductionListApi(this.tableFrom)
        this.tableData.data = res.list
        this.tableData.total = res.total
      } finally {
        this.listLoading = false
      }
    },
    handleAdd() {
      this.editData = null
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.editData = row
      this.dialogVisible = true
    },
    async handleDelete(row) {
      await this.$confirm('确认删除该满减活动?', '提示', { type: 'warning' })
      await fullReductionDeleteApi(row.id)
      this.$message.success('删除成功')
      this.getList()
    },
    async handleStatusChange(row) {
      await fullReductionUpdateStatusApi(row.id, row.status)
      this.$message.success('状态更新成功')
    },
    handleSizeChange(val) {
      this.tableFrom.limit = val
      this.getList()
    },
    handleCurrentChange(val) {
      this.tableFrom.page = val
      this.getList()
    }
  }
}
</script>
```

**Step 3: 提交列表页面**

```bash
git add admin/src/views/marketing/promotion/
git commit -m "feat(admin): 创建满减活动列表页面"
```

---

### Task 35: 创建满减活动编辑表单组件

**Files:**
- Create: `admin/src/views/marketing/promotion/fullReduction/components/form.vue`

**Step 1: 创建编辑表单组件**

```vue
<template>
  <el-dialog
    :title="editData ? '编辑满减活动' : '新增满减活动'"
    :visible.sync="visible"
    width="700px"
    @close="handleClose"
  >
    <el-form ref="form" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="活动名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入活动名称" />
      </el-form-item>
      <el-form-item label="活动范围" prop="scopeType">
        <el-radio-group v-model="form.scopeType">
          <el-radio :label="1">全场</el-radio>
          <el-radio :label="2">指定品类</el-radio>
          <el-radio :label="3">指定商品</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="form.scopeType === 2" label="选择品类">
        <el-cascader
          v-model="form.relationIds"
          :options="categoryOptions"
          :props="{ multiple: true, value: 'id', label: 'name' }"
          placeholder="请选择品类"
        />
      </el-form-item>
      <el-form-item v-if="form.scopeType === 3" label="选择商品">
        <el-button type="primary" size="small" @click="openProductSelect">
          选择商品
        </el-button>
        <div v-if="selectedProducts.length">
          已选 {{ selectedProducts.length }} 个商品
        </div>
      </el-form-item>
      <el-form-item label="活动时间" prop="startTime">
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="yyyy-MM-dd HH:mm:ss"
          @change="handleDateChange"
        />
      </el-form-item>
      <el-form-item label="满减阶梯" prop="levels">
        <div v-for="(level, index) in form.levels" :key="index" class="level-item">
          满 <el-input-number v-model="level.fullAmount" :min="0" :precision="2" /> 元
          减 <el-input-number v-model="level.reduceAmount" :min="0" :precision="2" /> 元
          <el-button v-if="form.levels.length > 1" type="text" class="red" @click="removeLevel(index)">
            删除
          </el-button>
        </div>
        <el-button type="text" @click="addLevel">+ 添加阶梯</el-button>
      </el-form-item>
      <el-form-item label="叠加优惠券">
        <el-switch v-model="form.allowCoupon" />
        <span class="tip">开启后，该满减活动可与优惠券同时使用</span>
      </el-form-item>
    </el-form>
    <div slot="footer">
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">确定</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { fullReductionDetailApi, fullReductionSaveApi } from '@/api/promotion'

export default {
  props: {
    visible: Boolean,
    editData: Object
  },
  data() {
    return {
      form: {
        name: '',
        scopeType: 1,
        startTime: '',
        endTime: '',
        allowCoupon: true,
        relationIds: [],
        levels: [{ fullAmount: 0, reduceAmount: 0 }]
      },
      dateRange: [],
      selectedProducts: [],
      categoryOptions: [],
      loading: false,
      rules: {
        name: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
        scopeType: [{ required: true, message: '请选择活动范围', trigger: 'change' }],
        startTime: [{ required: true, message: '请选择活动时间', trigger: 'change' }]
      }
    }
  },
  watch: {
    visible(val) {
      if (val && this.editData) {
        this.loadDetail()
      }
    }
  },
  methods: {
    async loadDetail() {
      const res = await fullReductionDetailApi(this.editData.id)
      this.form = {
        id: res.id,
        name: res.name,
        scopeType: res.scopeType,
        startTime: res.startTime,
        endTime: res.endTime,
        allowCoupon: res.allowCoupon,
        relationIds: res.relationIds || [],
        levels: res.levels || [{ fullAmount: 0, reduceAmount: 0 }]
      }
      this.dateRange = [res.startTime, res.endTime]
    },
    handleDateChange(val) {
      if (val) {
        this.form.startTime = val[0]
        this.form.endTime = val[1]
      }
    },
    addLevel() {
      this.form.levels.push({ fullAmount: 0, reduceAmount: 0 })
    },
    removeLevel(index) {
      this.form.levels.splice(index, 1)
    },
    openProductSelect() {
      // 打开商品选择弹窗（复用现有组件）
    },
    async handleSubmit() {
      await this.$refs.form.validate()
      this.loading = true
      try {
        await fullReductionSaveApi(this.form)
        this.$message.success('保存成功')
        this.$emit('success')
        this.handleClose()
      } finally {
        this.loading = false
      }
    },
    handleClose() {
      this.$emit('update:visible', false)
      this.form = {
        name: '',
        scopeType: 1,
        startTime: '',
        endTime: '',
        allowCoupon: true,
        relationIds: [],
        levels: [{ fullAmount: 0, reduceAmount: 0 }]
      }
      this.dateRange = []
    }
  }
}
</script>

<style scoped>
.level-item {
  margin-bottom: 10px;
}
.tip {
  color: #999;
  margin-left: 10px;
}
</style>
```

**Step 2: 提交表单组件**

```bash
git add admin/src/views/marketing/promotion/fullReduction/components/
git commit -m "feat(admin): 创建满减活动编辑表单组件"
```

---

### Task 36: 创建买赠活动页面（参照满减结构）

**Files:**
- Create: `admin/src/views/marketing/promotion/buyGift/index.vue`
- Create: `admin/src/views/marketing/promotion/buyGift/components/form.vue`

**Step 1: 创建买赠活动列表页面**（参照满减活动列表结构，调整字段）

**Step 2: 创建买赠活动编辑表单**（包含购买商品和赠品选择）

**Step 3: 提交买赠页面**

```bash
git add admin/src/views/marketing/promotion/buyGift/
git commit -m "feat(admin): 创建买赠活动管理页面"
```

---

### Task 37: 创建代金券管理页面

**Files:**
- Create: `admin/src/views/marketing/voucher/index.vue`

**Step 1: 创建代金券列表页面**

```vue
<template>
  <div class="divBox">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>代金券管理</span>
        <el-button type="primary" size="small" @click="handleAdd">
          添加代金券
        </el-button>
      </div>
      <!-- 表格区域 -->
      <el-table v-loading="listLoading" :data="tableData.data" border>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="代金券名称" min-width="150" />
        <el-table-column prop="money" label="面额" width="100">
          <template slot-scope="scope">
            ¥{{ scope.row.money }}
          </template>
        </el-table-column>
        <el-table-column label="可抵扣运费" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.canDeductFreight ? 'success' : 'info'">
              {{ scope.row.canDeductFreight ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="total" label="发行量" width="100" />
        <el-table-column prop="used" label="已使用" width="100" />
        <el-table-column label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status ? 'success' : 'danger'">
              {{ scope.row.status ? '开启' : '关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="text" @click="handleSend(scope.row)">发放</el-button>
            <el-button type="text" class="red" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        :current-page="tableFrom.page"
        :page-sizes="[10, 20, 50]"
        :page-size="tableFrom.limit"
        layout="total, sizes, prev, pager, next, jumper"
        :total="tableData.total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>
  </div>
</template>

<script>
import { voucherListApi, voucherDeleteApi } from '@/api/marketing'

export default {
  name: 'Voucher',
  data() {
    return {
      tableFrom: { page: 1, limit: 10 },
      tableData: { data: [], total: 0 },
      listLoading: false
    }
  },
  mounted() {
    this.getList()
  },
  methods: {
    async getList() {
      this.listLoading = true
      try {
        const res = await voucherListApi(this.tableFrom)
        this.tableData.data = res.list
        this.tableData.total = res.total
      } finally {
        this.listLoading = false
      }
    },
    handleAdd() {
      // 打开新增弹窗
    },
    handleEdit(row) {
      // 打开编辑弹窗
    },
    handleSend(row) {
      // 打开发放弹窗
    },
    async handleDelete(row) {
      await this.$confirm('确认删除该代金券?', '提示', { type: 'warning' })
      await voucherDeleteApi(row.id)
      this.$message.success('删除成功')
      this.getList()
    },
    handleSizeChange(val) {
      this.tableFrom.limit = val
      this.getList()
    },
    handleCurrentChange(val) {
      this.tableFrom.page = val
      this.getList()
    }
  }
}
</script>
```

**Step 2: 提交代金券页面**

```bash
git add admin/src/views/marketing/voucher/
git commit -m "feat(admin): 创建代金券管理页面"
```

---

### Task 38: 前端代码检查和构建验证

**Step 1: ESLint 检查**

Run: `cd admin && npm run lint`

**Step 2: 修复 lint 错误（如有）**

**Step 3: 构建验证**

Run: `cd admin && npm run build:stage`

Expected: Build 成功

**Step 4: 提交修复（如有）**

```bash
git add -A
git commit -m "fix(admin): 修复前端 lint 和构建错误"
```

---

## 阶段六：联调测试与优化（Day 14）

### Task 39: 添加系统菜单配置 SQL

**Files:**
- Modify: `crmeb/crmeb-common/src/main/resources/sql/promotion_tables.sql`

**Step 1: 添加菜单数据**

```sql
-- 添加促销活动菜单
INSERT INTO eb_system_menu (pid, name, icon, perms, component, menu_type, sort, is_show)
VALUES
-- 促销活动一级菜单
(营销菜单ID, '促销活动', 'el-icon-present', '', '', 'M', 10, 1),
-- 满减活动
((SELECT id FROM (SELECT id FROM eb_system_menu WHERE name='促销活动') t), '满减活动', '', 'admin:promotion:full-reduction:list', 'marketing/promotion/fullReduction/index', 'C', 1, 1),
-- 买赠活动
((SELECT id FROM (SELECT id FROM eb_system_menu WHERE name='促销活动') t), '买赠活动', '', 'admin:promotion:buy-gift:list', 'marketing/promotion/buyGift/index', 'C', 2, 1),
-- 代金券（放在优惠券下）
(优惠券菜单ID, '代金券列表', '', 'admin:marketing:voucher:list', 'marketing/voucher/index', 'C', 2, 1);
```

**Step 2: 提交菜单配置**

```bash
git add crmeb/crmeb-common/src/main/resources/sql/promotion_tables.sql
git commit -m "feat(promotion): 添加系统菜单配置"
```

---

### Task 40: 功能验证清单

**Step 1: 后端 API 测试**

使用 Swagger 或 Postman 测试以下 API：

- [ ] 满减活动 CRUD
- [ ] 买赠活动 CRUD
- [ ] 代金券 CRUD
- [ ] 订单价格计算（包含满减、代金券）

**Step 2: 前端页面测试**

- [ ] 满减活动列表显示正常
- [ ] 满减活动新增/编辑/删除功能正常
- [ ] 买赠活动列表显示正常
- [ ] 买赠活动新增/编辑/删除功能正常
- [ ] 代金券列表显示正常
- [ ] 代金券新增/编辑/删除/发放功能正常

**Step 3: 集成测试**

- [ ] 创建满减活动，下单验证满减生效
- [ ] 创建买赠活动，下单验证赠品添加
- [ ] 发放代金券，下单验证代金券抵扣
- [ ] 测试优惠券和代金券叠加使用
- [ ] 测试满减活动禁用优惠券叠加场景

---

### Task 41: 提交最终代码

**Step 1: 全量编译验证**

```bash
cd crmeb && mvn clean package -DskipTests
cd admin && npm run build:prod
```

**Step 2: 最终提交**

```bash
git add -A
git commit -m "feat(promotion): 完成促销活动扩展功能"
```

---

## 总结

### 文件清单

**后端新增文件：**
```
crmeb/crmeb-common/src/main/java/com/zbkj/common/model/promotion/
├── FullReduction.java
├── FullReductionLevel.java
├── FullReductionProduct.java
├── BuyGift.java
├── BuyGiftProduct.java
└── BuyGiftRecord.java

crmeb/crmeb-common/src/main/java/com/zbkj/common/request/
├── FullReductionRequest.java
├── FullReductionSearchRequest.java
├── BuyGiftRequest.java
└── BuyGiftSearchRequest.java

crmeb/crmeb-common/src/main/java/com/zbkj/common/response/
├── FullReductionResponse.java
└── BuyGiftResponse.java

crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/promotion/
├── FullReductionDao.java
├── FullReductionLevelDao.java
├── FullReductionProductDao.java
├── BuyGiftDao.java
├── BuyGiftProductDao.java
└── BuyGiftRecordDao.java

crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/
├── FullReductionService.java
├── FullReductionLevelService.java
├── FullReductionProductService.java
├── BuyGiftService.java
├── BuyGiftProductService.java
├── BuyGiftRecordService.java
├── PromotionCalculateService.java
└── impl/
    ├── FullReductionServiceImpl.java
    ├── FullReductionLevelServiceImpl.java
    ├── FullReductionProductServiceImpl.java
    ├── BuyGiftServiceImpl.java
    ├── BuyGiftProductServiceImpl.java
    ├── BuyGiftRecordServiceImpl.java
    └── PromotionCalculateServiceImpl.java

crmeb/crmeb-admin/src/main/java/com/zbkj/admin/controller/
├── FullReductionController.java
├── BuyGiftController.java
└── VoucherController.java
```

**后端修改文件：**
```
crmeb/crmeb-common/src/main/java/com/zbkj/common/model/coupon/StoreCoupon.java
crmeb/crmeb-common/src/main/java/com/zbkj/common/model/order/StoreOrder.java
crmeb/crmeb-common/src/main/java/com/zbkj/common/model/order/StoreOrderInfo.java
crmeb/crmeb-service/src/main/java/com/zbkj/service/service/StoreCouponService.java
crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/StoreCouponServiceImpl.java
crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/OrderServiceImpl.java
```

**前端新增文件：**
```
admin/src/api/promotion.js
admin/src/views/marketing/promotion/index.vue
admin/src/views/marketing/promotion/fullReduction/index.vue
admin/src/views/marketing/promotion/fullReduction/components/form.vue
admin/src/views/marketing/promotion/buyGift/index.vue
admin/src/views/marketing/promotion/buyGift/components/form.vue
admin/src/views/marketing/voucher/index.vue
```

**前端修改文件：**
```
admin/src/router/modules/marketing.js
admin/src/api/marketing.js
```

---

*计划生成完成，共 41 个任务，预计 14 天完成*
