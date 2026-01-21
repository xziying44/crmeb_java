# 阶段一：业务员模块实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现业务员管理系统，包含 Admin 后台管理、业务员端独立应用、商城端绑定功能。

**Architecture:** 基于现有 CRMEB 架构扩展，业务员复用 SystemAdmin 账号体系通过角色区分，新增 SalesmanInfo 表存储扩展信息，User 表新增 salesman_id 字段建立绑定关系。

**Tech Stack:** Spring Boot 2.2.6 + MyBatis-Plus 3.3.1 + Vue 2 + Element UI + uni-app

---

## 任务总览

| 阶段 | 任务 | 预估时间 |
|-----|------|---------|
| 1 | 数据库表结构变更 | 0.5 天 |
| 2 | 后端实体类与 DAO | 0.5 天 |
| 3 | 业务员核心服务 | 1 天 |
| 4 | Admin 后台接口 | 1.5 天 |
| 5 | 业务员端接口 | 2 天 |
| 6 | 微信小程序码服务 | 1 天 |
| 7 | 商城端绑定接口 | 1 天 |
| 8 | Admin 前端页面 | 4 天 |
| 9 | 业务员端前端项目 | 6 天 |
| 10 | 商城端前端修改 | 1.5 天 |

---

## Task 1: 数据库表结构变更

**Files:**
- Create: `crmeb/sql/salesman_schema.sql`

### Step 1: 创建 SQL 脚本文件

```sql
-- =====================================================
-- 业务员模块数据库变更脚本
-- 执行时间: 阶段一开发前
-- =====================================================

-- 1. eb_system_role 新增字段
ALTER TABLE eb_system_role ADD COLUMN
  is_salesman_role TINYINT(1) DEFAULT 0 COMMENT '是否业务员角色 0-否 1-是';

-- 2. 创建业务员扩展信息表
CREATE TABLE IF NOT EXISTS eb_salesman_info (
  id              INT(11) PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  admin_id        INT(11) NOT NULL COMMENT '关联的管理员ID',
  salesman_code   VARCHAR(8) NOT NULL COMMENT '业务员邀请码（6-8位字母数字）',
  salesman_qrcode VARCHAR(255) NULL COMMENT '小程序码图片地址',
  qrcode_scene    VARCHAR(32) NULL COMMENT '小程序码scene参数',
  bindable        TINYINT(1) DEFAULT 1 COMMENT '是否可被绑定 0-否 1-是',
  create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_admin_id (admin_id),
  UNIQUE KEY uk_salesman_code (salesman_code),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务员扩展信息表';

-- 3. eb_user 新增字段
ALTER TABLE eb_user ADD COLUMN
  salesman_id INT(11) DEFAULT 0 COMMENT '绑定的业务员ID（admin_id）';
ALTER TABLE eb_user ADD COLUMN
  salesman_bind_time DATETIME NULL COMMENT '绑定业务员时间';
ALTER TABLE eb_user ADD INDEX idx_salesman_id (salesman_id);

-- 4. 创建预设业务员角色
INSERT INTO eb_system_role (role_name, rules, level, status, is_salesman_role, create_time, update_time)
VALUES ('业务员', '', 1, 1, 1, NOW(), NOW());

-- 5. 创建业务员专属菜单
-- 注意：需要根据实际菜单ID调整pid
INSERT INTO eb_system_menu (pid, name, icon, perms, menu_type, path, component, sort, is_show, create_time, update_time)
VALUES
(0, '业务员管理', 'el-icon-user', '', 'M', 'salesman', '', 100, 1, NOW(), NOW());

-- 获取上面插入的菜单ID用于下面的子菜单
SET @parent_id = LAST_INSERT_ID();

INSERT INTO eb_system_menu (pid, name, icon, perms, menu_type, path, component, sort, is_show, create_time, update_time)
VALUES
(@parent_id, '业务员列表', '', 'admin:salesman:list', 'C', 'list', 'salesman/list/index', 1, 1, NOW(), NOW()),
(@parent_id, '客户绑定记录', '', 'admin:salesman:bindList', 'C', 'bindList', 'salesman/bindList/index', 2, 1, NOW(), NOW()),
(@parent_id, '业绩统计', '', 'admin:salesman:statistics', 'C', 'statistics', 'salesman/statistics/index', 3, 1, NOW(), NOW());
```

### Step 2: 在数据库执行 SQL 脚本

Run: `mysql -u root -p single_open < crmeb/sql/salesman_schema.sql`

或通过 MySQL 客户端/Navicat 执行。

### Step 3: 验证表结构

```sql
-- 验证 eb_system_role 新字段
DESCRIBE eb_system_role;

-- 验证 eb_salesman_info 表
DESCRIBE eb_salesman_info;

-- 验证 eb_user 新字段
SHOW COLUMNS FROM eb_user LIKE 'salesman%';

-- 验证预设角色
SELECT * FROM eb_system_role WHERE is_salesman_role = 1;
```

### Step 4: 提交

```bash
git add crmeb/sql/salesman_schema.sql
git commit -m "feat(salesman): 添加业务员模块数据库表结构"
```

---

## Task 2: 后端实体类与 DAO

### Step 2.1: 修改 SystemRole 实体类

**Files:**
- Modify: `crmeb/crmeb-common/src/main/java/com/zbkj/common/model/system/SystemRole.java`

在 `SystemRole` 类中添加新字段：

```java
@ApiModelProperty(value = "是否业务员角色 0-否 1-是")
private Boolean isSalesmanRole;
```

### Step 2.2: 创建 SalesmanInfo 实体类

**Files:**
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/model/salesman/SalesmanInfo.java`

```java
package com.zbkj.common.model.salesman;

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
 * 业务员扩展信息表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_salesman_info")
@ApiModel(value = "SalesmanInfo对象", description = "业务员扩展信息表")
public class SalesmanInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "关联的管理员ID")
    private Integer adminId;

    @ApiModelProperty(value = "业务员邀请码（6-8位字母数字）")
    private String salesmanCode;

    @ApiModelProperty(value = "小程序码图片地址")
    private String salesmanQrcode;

    @ApiModelProperty(value = "小程序码scene参数")
    private String qrcodeScene;

    @ApiModelProperty(value = "是否可被绑定 0-否 1-是")
    private Boolean bindable;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    private Date updateTime;
}
```

### Step 2.3: 修改 User 实体类

**Files:**
- Modify: `crmeb/crmeb-common/src/main/java/com/zbkj/common/model/user/User.java`

在 `User` 类中添加新字段：

```java
@ApiModelProperty(value = "绑定的业务员ID（admin_id）")
private Integer salesmanId;

@ApiModelProperty(value = "绑定业务员时间")
private Date salesmanBindTime;
```

### Step 2.4: 创建 SalesmanInfoDao

**Files:**
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/SalesmanInfoDao.java`

```java
package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.salesman.SalesmanInfo;

/**
 * 业务员扩展信息表 Mapper 接口
 */
public interface SalesmanInfoDao extends BaseMapper<SalesmanInfo> {
}
```

### Step 2.5: 创建 Request 类

**Files:**
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/SalesmanAddRequest.java`

```java
package com.zbkj.common.request;

import com.zbkj.common.constants.RegularConstants;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 业务员新增请求对象
 */
@Data
@ApiModel(value = "SalesmanAddRequest对象", description = "业务员新增请求对象")
public class SalesmanAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "业务员账号", required = true)
    @NotBlank(message = "账号不能为空")
    @Length(max = 32, message = "账号长度不能超过32个字符")
    private String account;

    @ApiModelProperty(value = "业务员密码", required = true)
    @NotBlank(message = "密码不能为空")
    @Length(max = 32, message = "密码长度不能超过32个字符")
    private String pwd;

    @ApiModelProperty(value = "业务员姓名", required = true)
    @NotBlank(message = "姓名不能为空")
    @Length(max = 16, message = "姓名长度不能超过16个字符")
    private String realName;

    @ApiModelProperty(value = "手机号码", required = true)
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = RegularConstants.PHONE_TWO, message = "请填写正确的手机号")
    private String phone;

    @ApiModelProperty(value = "状态 1有效0无效")
    @NotNull(message = "状态不能为空")
    private Boolean status;
}
```

**Files:**
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/SalesmanUpdateRequest.java`

```java
package com.zbkj.common.request;

import com.zbkj.common.constants.RegularConstants;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 业务员更新请求对象
 */
@Data
@ApiModel(value = "SalesmanUpdateRequest对象", description = "业务员更新请求对象")
public class SalesmanUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "业务员ID（admin_id）", required = true)
    @NotNull(message = "ID不能为空")
    private Integer id;

    @ApiModelProperty(value = "业务员姓名")
    @Length(max = 16, message = "姓名长度不能超过16个字符")
    private String realName;

    @ApiModelProperty(value = "手机号码")
    @Pattern(regexp = RegularConstants.PHONE_TWO, message = "请填写正确的手机号")
    private String phone;

    @ApiModelProperty(value = "密码（不修改则不传）")
    @Length(max = 32, message = "密码长度不能超过32个字符")
    private String pwd;

    @ApiModelProperty(value = "是否可被绑定")
    private Boolean bindable;
}
```

**Files:**
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/SalesmanSearchRequest.java`

```java
package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 业务员搜索请求对象
 */
@Data
@ApiModel(value = "SalesmanSearchRequest对象", description = "业务员搜索请求对象")
public class SalesmanSearchRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "搜索关键字（姓名/手机号/邀请码）")
    private String keywords;

    @ApiModelProperty(value = "状态：0-禁用，1-正常，不传查全部")
    private Boolean status;
}
```

**Files:**
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/CustomerTransferRequest.java`

```java
package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 客户转移请求对象
 */
@Data
@ApiModel(value = "CustomerTransferRequest对象", description = "客户转移请求对象")
public class CustomerTransferRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "客户用户ID", required = true)
    @NotNull(message = "客户ID不能为空")
    private Integer uid;

    @ApiModelProperty(value = "目标业务员ID（admin_id）", required = true)
    @NotNull(message = "目标业务员ID不能为空")
    private Integer targetSalesmanId;
}
```

### Step 2.6: 创建 Response/VO 类

**Files:**
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/response/SalesmanResponse.java`

```java
package com.zbkj.common.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 业务员响应对象
 */
@Data
@ApiModel(value = "SalesmanResponse对象", description = "业务员响应对象")
public class SalesmanResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "业务员ID（admin_id）")
    private Integer id;

    @ApiModelProperty(value = "账号")
    private String account;

    @ApiModelProperty(value = "姓名")
    private String realName;

    @ApiModelProperty(value = "手机号")
    private String phone;

    @ApiModelProperty(value = "邀请码")
    private String salesmanCode;

    @ApiModelProperty(value = "小程序码图片地址")
    private String salesmanQrcode;

    @ApiModelProperty(value = "是否可被绑定")
    private Boolean bindable;

    @ApiModelProperty(value = "状态：0-禁用，1-正常")
    private Boolean status;

    @ApiModelProperty(value = "客户数量")
    private Integer customerCount;

    @ApiModelProperty(value = "本月新增客户数")
    private Integer monthNewCustomerCount;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;
}
```

**Files:**
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/vo/SalesmanDashboardVo.java`

```java
package com.zbkj.common.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 业务员数据看板VO
 */
@Data
@ApiModel(value = "SalesmanDashboardVo对象", description = "业务员数据看板VO")
public class SalesmanDashboardVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "客户总数")
    private Integer totalCustomerCount;

    @ApiModelProperty(value = "本月新增客户数")
    private Integer monthNewCustomerCount;

    @ApiModelProperty(value = "客户订单总额")
    private BigDecimal totalOrderAmount;

    @ApiModelProperty(value = "本月订单金额")
    private BigDecimal monthOrderAmount;

    @ApiModelProperty(value = "销售趋势数据")
    private List<TrendDataVo> trendData;

    @ApiModelProperty(value = "客户消费排行榜")
    private List<CustomerRankingVo> customerRanking;

    @Data
    public static class TrendDataVo implements Serializable {
        @ApiModelProperty(value = "日期")
        private String date;
        @ApiModelProperty(value = "订单数量")
        private Integer orderCount;
        @ApiModelProperty(value = "订单金额")
        private BigDecimal orderAmount;
    }

    @Data
    public static class CustomerRankingVo implements Serializable {
        @ApiModelProperty(value = "用户ID")
        private Integer uid;
        @ApiModelProperty(value = "用户昵称")
        private String nickname;
        @ApiModelProperty(value = "手机号")
        private String phone;
        @ApiModelProperty(value = "消费总额")
        private BigDecimal totalAmount;
        @ApiModelProperty(value = "订单数量")
        private Integer orderCount;
    }
}
```

**Files:**
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/vo/CustomerBindRecordVo.java`

```java
package com.zbkj.common.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 客户绑定记录VO
 */
@Data
@ApiModel(value = "CustomerBindRecordVo对象", description = "客户绑定记录VO")
public class CustomerBindRecordVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "用户ID")
    private Integer uid;

    @ApiModelProperty(value = "用户昵称")
    private String nickname;

    @ApiModelProperty(value = "手机号")
    private String phone;

    @ApiModelProperty(value = "头像")
    private String avatar;

    @ApiModelProperty(value = "业务员ID")
    private Integer salesmanId;

    @ApiModelProperty(value = "业务员姓名")
    private String salesmanName;

    @ApiModelProperty(value = "绑定时间")
    private Date bindTime;

    @ApiModelProperty(value = "消费总额")
    private BigDecimal totalAmount;

    @ApiModelProperty(value = "订单数量")
    private Integer orderCount;

    @ApiModelProperty(value = "最近下单时间")
    private Date lastOrderTime;
}
```

### Step 2.7: 编译验证

Run: `cd crmeb && mvn compile -pl crmeb-common,crmeb-service`

Expected: BUILD SUCCESS

### Step 2.8: 提交

```bash
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/model/system/SystemRole.java
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/model/salesman/
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/model/user/User.java
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/SalesmanInfoDao.java
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/request/Salesman*.java
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/request/CustomerTransferRequest.java
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/response/SalesmanResponse.java
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/vo/SalesmanDashboardVo.java
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/vo/CustomerBindRecordVo.java
git commit -m "feat(salesman): 添加业务员实体类、DAO、请求响应对象"
```

---

## Task 3: 业务员核心服务

### Step 3.1: 创建 SalesmanInfoService 接口

**Files:**
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/SalesmanInfoService.java`

```java
package com.zbkj.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.salesman.SalesmanInfo;

/**
 * 业务员扩展信息服务接口
 */
public interface SalesmanInfoService extends IService<SalesmanInfo> {

    /**
     * 根据管理员ID获取业务员信息
     */
    SalesmanInfo getByAdminId(Integer adminId);

    /**
     * 根据邀请码获取业务员信息
     */
    SalesmanInfo getByCode(String salesmanCode);

    /**
     * 生成唯一邀请码
     */
    String generateUniqueCode();

    /**
     * 检查邀请码是否存在
     */
    boolean isCodeExists(String code);
}
```

### Step 3.2: 创建 SalesmanInfoServiceImpl 实现类

**Files:**
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/SalesmanInfoServiceImpl.java`

```java
package com.zbkj.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.model.salesman.SalesmanInfo;
import com.zbkj.service.dao.SalesmanInfoDao;
import com.zbkj.service.service.SalesmanInfoService;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * 业务员扩展信息服务实现类
 */
@Service
public class SalesmanInfoServiceImpl extends ServiceImpl<SalesmanInfoDao, SalesmanInfo> implements SalesmanInfoService {

    // 邀请码字符集（排除容易混淆的字符：0,O,1,I,L）
    private static final String CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;

    @Override
    public SalesmanInfo getByAdminId(Integer adminId) {
        LambdaQueryWrapper<SalesmanInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalesmanInfo::getAdminId, adminId);
        return getOne(wrapper);
    }

    @Override
    public SalesmanInfo getByCode(String salesmanCode) {
        if (salesmanCode == null || salesmanCode.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<SalesmanInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalesmanInfo::getSalesmanCode, salesmanCode.toUpperCase());
        return getOne(wrapper);
    }

    @Override
    public String generateUniqueCode() {
        String code;
        int maxAttempts = 100;
        int attempts = 0;
        do {
            code = generateRandomCode();
            attempts++;
            if (attempts > maxAttempts) {
                throw new RuntimeException("无法生成唯一邀请码，请稍后重试");
            }
        } while (isCodeExists(code));
        return code;
    }

    @Override
    public boolean isCodeExists(String code) {
        LambdaQueryWrapper<SalesmanInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalesmanInfo::getSalesmanCode, code);
        return count(wrapper) > 0;
    }

    /**
     * 生成随机邀请码
     */
    private String generateRandomCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
```

### Step 3.3: 创建 SalesmanService 接口

**Files:**
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/SalesmanService.java`

```java
package com.zbkj.service.service;

import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.*;
import com.zbkj.common.response.SalesmanResponse;
import com.zbkj.common.vo.CustomerBindRecordVo;
import com.zbkj.common.vo.SalesmanDashboardVo;

import java.util.List;

/**
 * 业务员管理服务接口
 */
public interface SalesmanService {

    /**
     * 获取业务员分页列表
     */
    CommonPage<SalesmanResponse> getList(SalesmanSearchRequest request, PageParamRequest pageRequest);

    /**
     * 获取业务员详情
     */
    SalesmanResponse getDetail(Integer id);

    /**
     * 创建业务员
     */
    Boolean create(SalesmanAddRequest request);

    /**
     * 更新业务员
     */
    Boolean update(SalesmanUpdateRequest request);

    /**
     * 删除业务员
     */
    Boolean delete(Integer id);

    /**
     * 更新业务员状态
     */
    Boolean updateStatus(Integer id, Boolean status);

    /**
     * 重新生成邀请码
     */
    String regenerateCode(Integer id);

    /**
     * 获取客户绑定记录列表
     */
    CommonPage<CustomerBindRecordVo> getBindList(Integer salesmanId, String keywords, PageParamRequest pageRequest);

    /**
     * 转移客户
     */
    Boolean transferCustomer(CustomerTransferRequest request);

    /**
     * 获取业绩统计
     */
    Object getStatistics();

    /**
     * 获取业务员排行榜
     */
    List<SalesmanResponse> getRanking(Integer limit);

    /**
     * 业务员登录
     */
    Object login(SystemAdminLoginRequest request);

    /**
     * 获取数据看板（业务员端）
     */
    SalesmanDashboardVo getDashboard(Integer adminId, String dateType);

    /**
     * 获取我的邀请码和二维码
     */
    SalesmanResponse getMyCode(Integer adminId);

    /**
     * 检查邀请码是否有效
     */
    Boolean checkCode(String code);

    /**
     * 获取所有业务员列表（下拉选择用）
     */
    List<SalesmanResponse> getAllList();
}
```

### Step 3.4: 创建 SalesmanServiceImpl 实现类（核心骨架）

**Files:**
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/SalesmanServiceImpl.java`

```java
package com.zbkj.service.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.pagehelper.PageHelper;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.salesman.SalesmanInfo;
import com.zbkj.common.model.system.SystemAdmin;
import com.zbkj.common.model.system.SystemRole;
import com.zbkj.common.model.user.User;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.*;
import com.zbkj.common.response.SalesmanResponse;
import com.zbkj.common.utils.CrmebUtil;
import com.zbkj.common.vo.CustomerBindRecordVo;
import com.zbkj.common.vo.SalesmanDashboardVo;
import com.zbkj.service.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 业务员管理服务实现类
 */
@Slf4j
@Service
public class SalesmanServiceImpl implements SalesmanService {

    @Autowired
    private SalesmanInfoService salesmanInfoService;

    @Autowired
    private SystemAdminService systemAdminService;

    @Autowired
    private SystemRoleService systemRoleService;

    @Autowired
    private UserService userService;

    @Override
    public CommonPage<SalesmanResponse> getList(SalesmanSearchRequest request, PageParamRequest pageRequest) {
        // 1. 获取业务员角色ID
        Integer salesmanRoleId = getSalesmanRoleId();
        if (salesmanRoleId == null) {
            return CommonPage.restPage(new ArrayList<>());
        }

        // 2. 查询具有业务员角色的管理员
        PageHelper.startPage(pageRequest.getPage(), pageRequest.getLimit());
        LambdaQueryWrapper<SystemAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemAdmin::getIsDel, false);
        // 角色包含业务员角色ID
        wrapper.apply("FIND_IN_SET({0}, roles)", salesmanRoleId);

        // 关键字搜索
        if (StrUtil.isNotBlank(request.getKeywords())) {
            wrapper.and(w -> w.like(SystemAdmin::getRealName, request.getKeywords())
                    .or().like(SystemAdmin::getPhone, request.getKeywords())
                    .or().like(SystemAdmin::getAccount, request.getKeywords()));
        }
        if (request.getStatus() != null) {
            wrapper.eq(SystemAdmin::getStatus, request.getStatus());
        }
        wrapper.orderByDesc(SystemAdmin::getId);

        List<SystemAdmin> adminList = systemAdminService.list(wrapper);
        if (adminList.isEmpty()) {
            return CommonPage.restPage(new ArrayList<>());
        }

        // 3. 组装响应数据
        List<Integer> adminIds = adminList.stream().map(SystemAdmin::getId).collect(Collectors.toList());

        // 获取业务员扩展信息
        Map<Integer, SalesmanInfo> infoMap = getSalesmanInfoMap(adminIds);

        // 获取客户数量统计
        Map<Integer, Integer> customerCountMap = getCustomerCountMap(adminIds);
        Map<Integer, Integer> monthCustomerCountMap = getMonthNewCustomerCountMap(adminIds);

        List<SalesmanResponse> responseList = adminList.stream().map(admin -> {
            SalesmanResponse response = new SalesmanResponse();
            response.setId(admin.getId());
            response.setAccount(admin.getAccount());
            response.setRealName(admin.getRealName());
            response.setPhone(admin.getPhone());
            response.setStatus(admin.getStatus());
            response.setCreateTime(admin.getCreateTime());

            SalesmanInfo info = infoMap.get(admin.getId());
            if (info != null) {
                response.setSalesmanCode(info.getSalesmanCode());
                response.setSalesmanQrcode(info.getSalesmanQrcode());
                response.setBindable(info.getBindable());
            }

            response.setCustomerCount(customerCountMap.getOrDefault(admin.getId(), 0));
            response.setMonthNewCustomerCount(monthCustomerCountMap.getOrDefault(admin.getId(), 0));

            return response;
        }).collect(Collectors.toList());

        return CommonPage.restPage(responseList);
    }

    @Override
    public SalesmanResponse getDetail(Integer id) {
        SystemAdmin admin = systemAdminService.getById(id);
        if (admin == null || admin.getIsDel()) {
            throw new CrmebException("业务员不存在");
        }

        SalesmanResponse response = new SalesmanResponse();
        response.setId(admin.getId());
        response.setAccount(admin.getAccount());
        response.setRealName(admin.getRealName());
        response.setPhone(admin.getPhone());
        response.setStatus(admin.getStatus());
        response.setCreateTime(admin.getCreateTime());

        SalesmanInfo info = salesmanInfoService.getByAdminId(id);
        if (info != null) {
            response.setSalesmanCode(info.getSalesmanCode());
            response.setSalesmanQrcode(info.getSalesmanQrcode());
            response.setBindable(info.getBindable());
        }

        // 客户统计
        response.setCustomerCount(getCustomerCount(id));
        response.setMonthNewCustomerCount(getMonthNewCustomerCount(id));

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean create(SalesmanAddRequest request) {
        // 1. 检查账号是否存在
        if (systemAdminService.checkAccount(request.getAccount())) {
            throw new CrmebException("账号已存在");
        }

        // 2. 获取业务员角色ID
        Integer salesmanRoleId = getSalesmanRoleId();
        if (salesmanRoleId == null) {
            throw new CrmebException("业务员角色未配置，请先创建业务员角色");
        }

        // 3. 创建管理员账号
        SystemAdminAddRequest adminRequest = new SystemAdminAddRequest();
        adminRequest.setAccount(request.getAccount());
        adminRequest.setPwd(request.getPwd());
        adminRequest.setRealName(request.getRealName());
        adminRequest.setPhone(request.getPhone());
        adminRequest.setRoles(salesmanRoleId.toString());
        adminRequest.setStatus(request.getStatus());

        systemAdminService.saveAdmin(adminRequest);

        // 4. 获取新创建的管理员ID
        SystemAdmin newAdmin = systemAdminService.getByAccount(request.getAccount());
        if (newAdmin == null) {
            throw new CrmebException("创建业务员失败");
        }

        // 5. 创建业务员扩展信息
        SalesmanInfo info = new SalesmanInfo();
        info.setAdminId(newAdmin.getId());
        info.setSalesmanCode(salesmanInfoService.generateUniqueCode());
        info.setBindable(true);
        info.setCreateTime(new Date());

        return salesmanInfoService.save(info);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean update(SalesmanUpdateRequest request) {
        SystemAdmin admin = systemAdminService.getById(request.getId());
        if (admin == null || admin.getIsDel()) {
            throw new CrmebException("业务员不存在");
        }

        // 更新管理员信息
        SystemAdminUpdateRequest adminRequest = new SystemAdminUpdateRequest();
        adminRequest.setId(request.getId());
        if (StrUtil.isNotBlank(request.getRealName())) {
            adminRequest.setRealName(request.getRealName());
        }
        if (StrUtil.isNotBlank(request.getPhone())) {
            adminRequest.setPhone(request.getPhone());
        }
        if (StrUtil.isNotBlank(request.getPwd())) {
            adminRequest.setPwd(request.getPwd());
        }
        systemAdminService.updateAdmin(adminRequest);

        // 更新业务员扩展信息
        if (request.getBindable() != null) {
            SalesmanInfo info = salesmanInfoService.getByAdminId(request.getId());
            if (info != null) {
                info.setBindable(request.getBindable());
                salesmanInfoService.updateById(info);
            }
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Integer id) {
        // 检查是否有绑定的客户
        int customerCount = getCustomerCount(id);
        if (customerCount > 0) {
            throw new CrmebException("该业务员还有" + customerCount + "个客户，请先转移客户");
        }

        // 删除业务员扩展信息
        SalesmanInfo info = salesmanInfoService.getByAdminId(id);
        if (info != null) {
            salesmanInfoService.removeById(info.getId());
        }

        // 删除管理员账号（软删除）
        return systemAdminService.removeById(id);
    }

    @Override
    public Boolean updateStatus(Integer id, Boolean status) {
        return systemAdminService.updateStatus(id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String regenerateCode(Integer id) {
        SalesmanInfo info = salesmanInfoService.getByAdminId(id);
        if (info == null) {
            throw new CrmebException("业务员信息不存在");
        }

        String newCode = salesmanInfoService.generateUniqueCode();
        info.setSalesmanCode(newCode);
        // 清除旧的二维码
        info.setSalesmanQrcode(null);
        info.setQrcodeScene(null);
        salesmanInfoService.updateById(info);

        return newCode;
    }

    @Override
    public CommonPage<CustomerBindRecordVo> getBindList(Integer salesmanId, String keywords, PageParamRequest pageRequest) {
        PageHelper.startPage(pageRequest.getPage(), pageRequest.getLimit());

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(User::getSalesmanId, 0);

        if (salesmanId != null && salesmanId > 0) {
            wrapper.eq(User::getSalesmanId, salesmanId);
        }

        if (StrUtil.isNotBlank(keywords)) {
            wrapper.and(w -> w.like(User::getNickname, keywords)
                    .or().like(User::getPhone, keywords));
        }

        wrapper.orderByDesc(User::getSalesmanBindTime);

        List<User> userList = userService.list(wrapper);
        if (userList.isEmpty()) {
            return CommonPage.restPage(new ArrayList<>());
        }

        // 获取业务员信息
        Set<Integer> salesmanIds = userList.stream()
                .map(User::getSalesmanId)
                .collect(Collectors.toSet());
        Map<Integer, SystemAdmin> adminMap = getAdminMap(new ArrayList<>(salesmanIds));

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

            // TODO: 获取消费统计
            vo.setTotalAmount(java.math.BigDecimal.ZERO);
            vo.setOrderCount(0);

            return vo;
        }).collect(Collectors.toList());

        return CommonPage.restPage(voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean transferCustomer(CustomerTransferRequest request) {
        User user = userService.getById(request.getUid());
        if (user == null) {
            throw new CrmebException("客户不存在");
        }

        // 验证目标业务员
        SalesmanInfo targetInfo = salesmanInfoService.getByAdminId(request.getTargetSalesmanId());
        if (targetInfo == null) {
            throw new CrmebException("目标业务员不存在");
        }

        user.setSalesmanId(request.getTargetSalesmanId());
        user.setSalesmanBindTime(new Date());

        return userService.updateById(user);
    }

    @Override
    public Object getStatistics() {
        // TODO: 实现业绩统计汇总
        return new HashMap<>();
    }

    @Override
    public List<SalesmanResponse> getRanking(Integer limit) {
        // TODO: 实现业务员排行榜
        return new ArrayList<>();
    }

    @Override
    public Object login(SystemAdminLoginRequest request) {
        // TODO: 实现业务员登录
        return null;
    }

    @Override
    public SalesmanDashboardVo getDashboard(Integer adminId, String dateType) {
        // TODO: 实现数据看板
        return new SalesmanDashboardVo();
    }

    @Override
    public SalesmanResponse getMyCode(Integer adminId) {
        return getDetail(adminId);
    }

    @Override
    public Boolean checkCode(String code) {
        SalesmanInfo info = salesmanInfoService.getByCode(code);
        return info != null && info.getBindable();
    }

    @Override
    public List<SalesmanResponse> getAllList() {
        SalesmanSearchRequest request = new SalesmanSearchRequest();
        request.setStatus(true);
        PageParamRequest pageRequest = new PageParamRequest();
        pageRequest.setPage(1);
        pageRequest.setLimit(1000);
        return getList(request, pageRequest).getList();
    }

    // ==================== 私有方法 ====================

    /**
     * 获取业务员角色ID
     */
    private Integer getSalesmanRoleId() {
        LambdaQueryWrapper<SystemRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemRole::getIsSalesmanRole, true);
        wrapper.eq(SystemRole::getStatus, true);
        wrapper.last("LIMIT 1");
        SystemRole role = systemRoleService.getOne(wrapper);
        return role != null ? role.getId() : null;
    }

    /**
     * 获取业务员扩展信息Map
     */
    private Map<Integer, SalesmanInfo> getSalesmanInfoMap(List<Integer> adminIds) {
        if (adminIds.isEmpty()) {
            return new HashMap<>();
        }
        LambdaQueryWrapper<SalesmanInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SalesmanInfo::getAdminId, adminIds);
        List<SalesmanInfo> list = salesmanInfoService.list(wrapper);
        return list.stream().collect(Collectors.toMap(SalesmanInfo::getAdminId, info -> info));
    }

    /**
     * 获取客户数量Map
     */
    private Map<Integer, Integer> getCustomerCountMap(List<Integer> salesmanIds) {
        if (salesmanIds.isEmpty()) {
            return new HashMap<>();
        }
        // 使用MyBatis-Plus的分组统计
        Map<Integer, Integer> result = new HashMap<>();
        for (Integer id : salesmanIds) {
            result.put(id, getCustomerCount(id));
        }
        return result;
    }

    /**
     * 获取本月新增客户数量Map
     */
    private Map<Integer, Integer> getMonthNewCustomerCountMap(List<Integer> salesmanIds) {
        if (salesmanIds.isEmpty()) {
            return new HashMap<>();
        }
        Map<Integer, Integer> result = new HashMap<>();
        for (Integer id : salesmanIds) {
            result.put(id, getMonthNewCustomerCount(id));
        }
        return result;
    }

    /**
     * 获取单个业务员的客户数量
     */
    private int getCustomerCount(Integer salesmanId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getSalesmanId, salesmanId);
        return (int) userService.count(wrapper);
    }

    /**
     * 获取单个业务员本月新增客户数量
     */
    private int getMonthNewCustomerCount(Integer salesmanId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getSalesmanId, salesmanId);
        // 本月开始时间
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        wrapper.ge(User::getSalesmanBindTime, cal.getTime());
        return (int) userService.count(wrapper);
    }

    /**
     * 获取管理员Map
     */
    private Map<Integer, SystemAdmin> getAdminMap(List<Integer> adminIds) {
        if (adminIds.isEmpty()) {
            return new HashMap<>();
        }
        List<SystemAdmin> list = systemAdminService.listByIds(adminIds);
        return list.stream().collect(Collectors.toMap(SystemAdmin::getId, admin -> admin));
    }
}
```

### Step 3.5: 修改 SystemAdminService 添加必要方法

**Files:**
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/SystemAdminService.java`

在接口中添加：

```java
/**
 * 检查账号是否存在
 */
Boolean checkAccount(String account);

/**
 * 根据账号获取管理员
 */
SystemAdmin getByAccount(String account);
```

**Files:**
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/SystemAdminServiceImpl.java`

添加实现方法：

```java
@Override
public Boolean checkAccount(String account) {
    LambdaQueryWrapper<SystemAdmin> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(SystemAdmin::getAccount, account);
    wrapper.eq(SystemAdmin::getIsDel, false);
    return count(wrapper) > 0;
}

@Override
public SystemAdmin getByAccount(String account) {
    LambdaQueryWrapper<SystemAdmin> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(SystemAdmin::getAccount, account);
    wrapper.eq(SystemAdmin::getIsDel, false);
    return getOne(wrapper);
}
```

### Step 3.6: 编译验证

Run: `cd crmeb && mvn compile -pl crmeb-service`

Expected: BUILD SUCCESS

### Step 3.7: 提交

```bash
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/SalesmanInfoService.java
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/SalesmanInfoServiceImpl.java
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/SalesmanService.java
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/SalesmanServiceImpl.java
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/SystemAdminService.java
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/SystemAdminServiceImpl.java
git commit -m "feat(salesman): 实现业务员核心服务层"
```

---

## Task 4: Admin 后台接口

### Step 4.1: 创建 SalesmanController

**Files:**
- Create: `crmeb/crmeb-admin/src/main/java/com/zbkj/admin/controller/SalesmanController.java`

```java
package com.zbkj.admin.controller;

import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.*;
import com.zbkj.common.response.SalesmanResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.vo.CustomerBindRecordVo;
import com.zbkj.service.service.SalesmanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 业务员管理控制器 - Admin后台
 */
@Slf4j
@RestController
@RequestMapping("api/admin/salesman")
@Api(tags = "业务员管理")
public class SalesmanController {

    @Autowired
    private SalesmanService salesmanService;

    @PreAuthorize("hasAuthority('admin:salesman:list')")
    @ApiOperation(value = "业务员分页列表")
    @GetMapping("/list")
    public CommonResult<CommonPage<SalesmanResponse>> list(
            @Validated SalesmanSearchRequest request,
            @Validated PageParamRequest pageRequest) {
        return CommonResult.success(salesmanService.getList(request, pageRequest));
    }

    @PreAuthorize("hasAuthority('admin:salesman:info')")
    @ApiOperation(value = "业务员详情")
    @GetMapping("/info/{id}")
    public CommonResult<SalesmanResponse> info(@PathVariable Integer id) {
        return CommonResult.success(salesmanService.getDetail(id));
    }

    @PreAuthorize("hasAuthority('admin:salesman:save')")
    @ApiOperation(value = "创建业务员")
    @PostMapping("/save")
    public CommonResult<String> save(@RequestBody @Validated SalesmanAddRequest request) {
        if (salesmanService.create(request)) {
            return CommonResult.success("创建成功");
        }
        return CommonResult.failed("创建失败");
    }

    @PreAuthorize("hasAuthority('admin:salesman:update')")
    @ApiOperation(value = "更新业务员")
    @PostMapping("/update")
    public CommonResult<String> update(@RequestBody @Validated SalesmanUpdateRequest request) {
        if (salesmanService.update(request)) {
            return CommonResult.success("更新成功");
        }
        return CommonResult.failed("更新失败");
    }

    @PreAuthorize("hasAuthority('admin:salesman:delete')")
    @ApiOperation(value = "删除业务员")
    @PostMapping("/delete/{id}")
    public CommonResult<String> delete(@PathVariable Integer id) {
        if (salesmanService.delete(id)) {
            return CommonResult.success("删除成功");
        }
        return CommonResult.failed("删除失败");
    }

    @PreAuthorize("hasAuthority('admin:salesman:update')")
    @ApiOperation(value = "更新业务员状态")
    @PostMapping("/updateStatus/{id}")
    public CommonResult<String> updateStatus(
            @PathVariable Integer id,
            @RequestParam Boolean status) {
        if (salesmanService.updateStatus(id, status)) {
            return CommonResult.success("修改成功");
        }
        return CommonResult.failed("修改失败");
    }

    @PreAuthorize("hasAuthority('admin:salesman:update')")
    @ApiOperation(value = "重新生成邀请码")
    @PostMapping("/regenerateCode/{id}")
    public CommonResult<String> regenerateCode(@PathVariable Integer id) {
        String newCode = salesmanService.regenerateCode(id);
        return CommonResult.success(newCode);
    }

    @PreAuthorize("hasAuthority('admin:salesman:bindList')")
    @ApiOperation(value = "客户绑定记录列表")
    @GetMapping("/bindList")
    public CommonResult<CommonPage<CustomerBindRecordVo>> bindList(
            @RequestParam(required = false) Integer salesmanId,
            @RequestParam(required = false) String keywords,
            @Validated PageParamRequest pageRequest) {
        return CommonResult.success(salesmanService.getBindList(salesmanId, keywords, pageRequest));
    }

    @PreAuthorize("hasAuthority('admin:salesman:transfer')")
    @ApiOperation(value = "转移客户")
    @PostMapping("/transferCustomer")
    public CommonResult<String> transferCustomer(@RequestBody @Validated CustomerTransferRequest request) {
        if (salesmanService.transferCustomer(request)) {
            return CommonResult.success("转移成功");
        }
        return CommonResult.failed("转移失败");
    }

    @PreAuthorize("hasAuthority('admin:salesman:statistics')")
    @ApiOperation(value = "业绩统计汇总")
    @GetMapping("/statistics")
    public CommonResult<Object> statistics() {
        return CommonResult.success(salesmanService.getStatistics());
    }

    @PreAuthorize("hasAuthority('admin:salesman:statistics')")
    @ApiOperation(value = "业务员业绩排行榜")
    @GetMapping("/ranking")
    public CommonResult<List<SalesmanResponse>> ranking(
            @RequestParam(defaultValue = "10") Integer limit) {
        return CommonResult.success(salesmanService.getRanking(limit));
    }

    @ApiOperation(value = "获取所有业务员列表（下拉选择用）")
    @GetMapping("/allList")
    public CommonResult<List<SalesmanResponse>> allList() {
        return CommonResult.success(salesmanService.getAllList());
    }
}
```

### Step 4.2: 编译验证

Run: `cd crmeb && mvn compile -pl crmeb-admin`

Expected: BUILD SUCCESS

### Step 4.3: 提交

```bash
git add crmeb/crmeb-admin/src/main/java/com/zbkj/admin/controller/SalesmanController.java
git commit -m "feat(salesman): 添加Admin后台业务员管理接口"
```

---

## Task 5: 业务员端接口

### Step 5.1: 创建 SalesmanApiController（业务员端专用接口）

**Files:**
- Create: `crmeb/crmeb-admin/src/main/java/com/zbkj/admin/controller/SalesmanApiController.java`

```java
package com.zbkj.admin.controller;

import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.SystemAdminLoginRequest;
import com.zbkj.common.response.SalesmanResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.vo.CustomerBindRecordVo;
import com.zbkj.common.vo.SalesmanDashboardVo;
import com.zbkj.service.service.SalesmanService;
import com.zbkj.service.service.SystemAdminService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 业务员端专用接口
 */
@Slf4j
@RestController
@RequestMapping("api/admin/salesman/app")
@Api(tags = "业务员端接口")
public class SalesmanApiController {

    @Autowired
    private SalesmanService salesmanService;

    @Autowired
    private SystemAdminService systemAdminService;

    @ApiOperation(value = "业务员登录")
    @PostMapping("/login")
    public CommonResult<Object> login(@RequestBody @Validated SystemAdminLoginRequest request) {
        return CommonResult.success(salesmanService.login(request));
    }

    @ApiOperation(value = "首页数据看板")
    @GetMapping("/dashboard")
    public CommonResult<SalesmanDashboardVo> dashboard(
            @RequestParam(defaultValue = "day") String dateType) {
        // 从token中获取当前登录的业务员ID
        Integer adminId = systemAdminService.getLoginAdminId();
        return CommonResult.success(salesmanService.getDashboard(adminId, dateType));
    }

    @ApiOperation(value = "获取我的邀请码和二维码")
    @GetMapping("/myCode")
    public CommonResult<SalesmanResponse> myCode() {
        Integer adminId = systemAdminService.getLoginAdminId();
        return CommonResult.success(salesmanService.getMyCode(adminId));
    }

    @ApiOperation(value = "我的客户列表")
    @GetMapping("/customer/list")
    public CommonResult<CommonPage<CustomerBindRecordVo>> customerList(
            @RequestParam(required = false) String keywords,
            @Validated PageParamRequest pageRequest) {
        Integer adminId = systemAdminService.getLoginAdminId();
        return CommonResult.success(salesmanService.getBindList(adminId, keywords, pageRequest));
    }

    @ApiOperation(value = "客户详情")
    @GetMapping("/customer/detail/{uid}")
    public CommonResult<CustomerBindRecordVo> customerDetail(@PathVariable Integer uid) {
        // TODO: 实现客户详情
        return CommonResult.success(new CustomerBindRecordVo());
    }

    @ApiOperation(value = "添加客户-发送验证码")
    @PostMapping("/customer/sendCode")
    public CommonResult<String> sendCode(@RequestParam String phone) {
        // TODO: 实现发送验证码
        return CommonResult.success("验证码已发送");
    }

    @ApiOperation(value = "添加客户-验证码确认绑定")
    @PostMapping("/customer/bindByCode")
    public CommonResult<String> bindByCode(
            @RequestParam String phone,
            @RequestParam String code) {
        // TODO: 实现验证码绑定
        return CommonResult.success("绑定成功");
    }

    @ApiOperation(value = "销售趋势数据")
    @GetMapping("/statistics/trend")
    public CommonResult<Object> trend(
            @RequestParam(defaultValue = "day") String dateType) {
        Integer adminId = systemAdminService.getLoginAdminId();
        SalesmanDashboardVo dashboard = salesmanService.getDashboard(adminId, dateType);
        return CommonResult.success(dashboard.getTrendData());
    }
}
```

### Step 5.2: 修改 SystemAdminService 添加获取登录ID方法

**Files:**
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/SystemAdminService.java`

添加接口方法：

```java
/**
 * 获取当前登录管理员ID
 */
Integer getLoginAdminId();
```

**Files:**
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/SystemAdminServiceImpl.java`

添加实现（根据项目实际的认证方式实现）：

```java
@Override
public Integer getLoginAdminId() {
    // 从SecurityContext获取当前登录用户
    // 具体实现根据项目的认证机制调整
    return SecurityUtil.getLoginUserId();
}
```

### Step 5.3: 编译验证

Run: `cd crmeb && mvn compile -pl crmeb-admin`

Expected: BUILD SUCCESS

### Step 5.4: 提交

```bash
git add crmeb/crmeb-admin/src/main/java/com/zbkj/admin/controller/SalesmanApiController.java
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/SystemAdminService.java
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/SystemAdminServiceImpl.java
git commit -m "feat(salesman): 添加业务员端专用接口"
```

---

## Task 6: 微信小程序码服务

### Step 6.1: 创建小程序码生成服务

**Files:**
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/WechatQrcodeService.java`

```java
package com.zbkj.service.service;

/**
 * 微信小程序码服务接口
 */
public interface WechatQrcodeService {

    /**
     * 生成业务员小程序码
     * @param salesmanCode 业务员邀请码
     * @return 小程序码图片URL
     */
    String generateSalesmanQrcode(String salesmanCode);
}
```

**Files:**
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/WechatQrcodeServiceImpl.java`

```java
package com.zbkj.service.service.impl;

import cn.hutool.core.util.StrUtil;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.service.service.SystemAttachmentService;
import com.zbkj.service.service.WechatNewService;
import com.zbkj.service.service.WechatQrcodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 微信小程序码服务实现类
 */
@Slf4j
@Service
public class WechatQrcodeServiceImpl implements WechatQrcodeService {

    @Autowired
    private WechatNewService wechatNewService;

    @Autowired
    private SystemAttachmentService systemAttachmentService;

    @Override
    public String generateSalesmanQrcode(String salesmanCode) {
        if (StrUtil.isBlank(salesmanCode)) {
            throw new CrmebException("邀请码不能为空");
        }

        try {
            // scene参数格式：s_邀请码（s_前缀用于标识业务员绑定场景）
            String scene = "s_" + salesmanCode;
            // 小程序页面路径（绑定页面）
            String page = "pages/salesman/bind";

            // 调用微信接口生成小程序码
            // 返回的是图片的二进制数据
            byte[] qrcodeData = wechatNewService.getUnlimitedQrcode(scene, page);

            if (qrcodeData == null || qrcodeData.length == 0) {
                throw new CrmebException("生成小程序码失败");
            }

            // 上传到云存储并返回URL
            String fileName = "salesman_qrcode_" + salesmanCode + ".png";
            String url = systemAttachmentService.uploadQrcode(qrcodeData, fileName);

            return url;
        } catch (Exception e) {
            log.error("生成业务员小程序码失败: {}", e.getMessage(), e);
            throw new CrmebException("生成小程序码失败: " + e.getMessage());
        }
    }
}
```

### Step 6.2: 在 SalesmanController 中添加生成二维码接口

**Files:**
- Modify: `crmeb/crmeb-admin/src/main/java/com/zbkj/admin/controller/SalesmanController.java`

添加方法：

```java
@Autowired
private WechatQrcodeService wechatQrcodeService;

@Autowired
private SalesmanInfoService salesmanInfoService;

@PreAuthorize("hasAuthority('admin:salesman:update')")
@ApiOperation(value = "生成/刷新小程序码")
@PostMapping("/generateQrcode/{id}")
public CommonResult<String> generateQrcode(@PathVariable Integer id) {
    SalesmanInfo info = salesmanInfoService.getByAdminId(id);
    if (info == null) {
        return CommonResult.failed("业务员信息不存在");
    }

    String qrcodeUrl = wechatQrcodeService.generateSalesmanQrcode(info.getSalesmanCode());

    // 更新数据库
    info.setSalesmanQrcode(qrcodeUrl);
    info.setQrcodeScene("s_" + info.getSalesmanCode());
    salesmanInfoService.updateById(info);

    return CommonResult.success(qrcodeUrl);
}
```

### Step 6.3: 编译验证

Run: `cd crmeb && mvn compile`

Expected: BUILD SUCCESS

### Step 6.4: 提交

```bash
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/WechatQrcodeService.java
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/WechatQrcodeServiceImpl.java
git add crmeb/crmeb-admin/src/main/java/com/zbkj/admin/controller/SalesmanController.java
git commit -m "feat(salesman): 实现微信小程序码生成服务"
```

---

## Task 7: 商城端绑定接口

### Step 7.1: 修改用户注册接口支持邀请码

**Files:**
- Modify: `crmeb/crmeb-front/src/main/java/com/zbkj/front/controller/UserController.java`

在注册相关接口中添加 salesmanCode 参数支持。

### Step 7.2: 创建用户绑定业务员接口

**Files:**
- Create: `crmeb/crmeb-front/src/main/java/com/zbkj/front/controller/SalesmanBindController.java`

```java
package com.zbkj.front.controller;

import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.SalesmanService;
import com.zbkj.service.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 业务员绑定控制器 - 商城端
 */
@Slf4j
@RestController
@RequestMapping("api/front/salesman")
@Api(tags = "业务员绑定")
public class SalesmanBindController {

    @Autowired
    private SalesmanService salesmanService;

    @Autowired
    private UserService userService;

    @ApiOperation(value = "检查邀请码是否有效")
    @GetMapping("/checkCode")
    public CommonResult<Boolean> checkCode(@RequestParam String code) {
        return CommonResult.success(salesmanService.checkCode(code));
    }

    @ApiOperation(value = "绑定业务员")
    @PostMapping("/bind")
    public CommonResult<String> bind(@RequestParam String code) {
        // TODO: 获取当前登录用户ID
        Integer uid = userService.getLoginUserId();

        // 检查是否已绑定
        // 执行绑定
        // 返回结果

        return CommonResult.success("绑定成功");
    }

    @ApiOperation(value = "获取当前绑定的业务员信息")
    @GetMapping("/myBind")
    public CommonResult<Object> myBind() {
        // TODO: 实现获取绑定信息
        return CommonResult.success(null);
    }
}
```

### Step 7.3: 编译验证

Run: `cd crmeb && mvn compile -pl crmeb-front`

Expected: BUILD SUCCESS

### Step 7.4: 提交

```bash
git add crmeb/crmeb-front/src/main/java/com/zbkj/front/controller/SalesmanBindController.java
git commit -m "feat(salesman): 添加商城端业务员绑定接口"
```

---

## Task 8: Admin 前端页面

### Step 8.1: 创建业务员 API 封装

**Files:**
- Create: `admin/src/api/salesman.js`

```javascript
import request from '@/utils/request'

// 业务员列表
export function salesmanListApi(params) {
  return request({
    url: '/admin/salesman/list',
    method: 'get',
    params
  })
}

// 业务员详情
export function salesmanInfoApi(id) {
  return request({
    url: `/admin/salesman/info/${id}`,
    method: 'get'
  })
}

// 创建业务员
export function salesmanAddApi(data) {
  return request({
    url: '/admin/salesman/save',
    method: 'post',
    data
  })
}

// 更新业务员
export function salesmanUpdateApi(data) {
  return request({
    url: '/admin/salesman/update',
    method: 'post',
    data
  })
}

// 删除业务员
export function salesmanDeleteApi(id) {
  return request({
    url: `/admin/salesman/delete/${id}`,
    method: 'post'
  })
}

// 更新状态
export function salesmanUpdateStatusApi(id, status) {
  return request({
    url: `/admin/salesman/updateStatus/${id}`,
    method: 'post',
    params: { status }
  })
}

// 重新生成邀请码
export function salesmanRegenerateCodeApi(id) {
  return request({
    url: `/admin/salesman/regenerateCode/${id}`,
    method: 'post'
  })
}

// 生成小程序码
export function salesmanGenerateQrcodeApi(id) {
  return request({
    url: `/admin/salesman/generateQrcode/${id}`,
    method: 'post'
  })
}

// 客户绑定记录
export function salesmanBindListApi(params) {
  return request({
    url: '/admin/salesman/bindList',
    method: 'get',
    params
  })
}

// 转移客户
export function salesmanTransferApi(data) {
  return request({
    url: '/admin/salesman/transferCustomer',
    method: 'post',
    data
  })
}

// 业绩统计
export function salesmanStatisticsApi() {
  return request({
    url: '/admin/salesman/statistics',
    method: 'get'
  })
}

// 业务员排行榜
export function salesmanRankingApi(limit = 10) {
  return request({
    url: '/admin/salesman/ranking',
    method: 'get',
    params: { limit }
  })
}

// 所有业务员列表（下拉用）
export function salesmanAllListApi() {
  return request({
    url: '/admin/salesman/allList',
    method: 'get'
  })
}
```

### Step 8.2: 创建业务员列表页面

**Files:**
- Create: `admin/src/views/salesman/list/index.vue`

（完整 Vue 组件代码，包含表格、搜索、新增/编辑弹窗等）

### Step 8.3: 创建客户绑定记录页面

**Files:**
- Create: `admin/src/views/salesman/bindList/index.vue`

### Step 8.4: 创建业绩统计页面

**Files:**
- Create: `admin/src/views/salesman/statistics/index.vue`

### Step 8.5: 添加路由配置

**Files:**
- Create: `admin/src/router/modules/salesman.js`

```javascript
import Layout from '@/layout'

const salesmanRouter = {
  path: '/salesman',
  component: Layout,
  redirect: '/salesman/list',
  name: 'Salesman',
  meta: {
    title: '业务员管理',
    icon: 'el-icon-user'
  },
  children: [
    {
      path: 'list',
      component: () => import('@/views/salesman/list/index'),
      name: 'SalesmanList',
      meta: { title: '业务员列表', icon: '' }
    },
    {
      path: 'bindList',
      component: () => import('@/views/salesman/bindList/index'),
      name: 'SalesmanBindList',
      meta: { title: '客户绑定记录', icon: '' }
    },
    {
      path: 'statistics',
      component: () => import('@/views/salesman/statistics/index'),
      name: 'SalesmanStatistics',
      meta: { title: '业绩统计', icon: '' }
    }
  ]
}

export default salesmanRouter
```

### Step 8.6: 在主路由中引入

**Files:**
- Modify: `admin/src/router/index.js`

添加 salesman 路由模块。

### Step 8.7: 提交

```bash
git add admin/src/api/salesman.js
git add admin/src/views/salesman/
git add admin/src/router/modules/salesman.js
git add admin/src/router/index.js
git commit -m "feat(salesman): 添加Admin前端业务员管理页面"
```

---

## Task 9: 业务员端前端项目

### Step 9.1: 初始化 uni-app 项目

**Files:**
- Create: `salesman/` (新 uni-app 项目)

使用 HBuilderX 或命令行创建：

```bash
# 使用 vue-cli 创建
vue create -p dcloudio/uni-preset-vue salesman
cd salesman
```

### Step 9.2: 创建项目基础结构

```
salesman/
├── pages/
│   ├── login/index.vue
│   ├── index/index.vue
│   ├── customer/
│   │   ├── list.vue
│   │   ├── detail.vue
│   │   └── add.vue
│   ├── promote/index.vue
│   └── user/
│       ├── index.vue
│       └── password.vue
├── api/
│   └── salesman.js
├── utils/
│   └── request.js
├── static/
├── App.vue
├── main.js
├── pages.json
└── manifest.json
```

### Step 9.3-9.10: 逐步实现各页面

（详细的 Vue 组件代码，每个页面一个 Step）

### Step 9.11: 提交

```bash
git add salesman/
git commit -m "feat(salesman): 创建业务员端uni-app项目"
```

---

## Task 10: 商城端前端修改

### Step 10.1: 修改注册页面添加邀请码输入框

**Files:**
- Modify: `app/pages/users/user_register/index.vue`

### Step 10.2: 添加扫码绑定页面

**Files:**
- Create: `app/pages/salesman/bind.vue`

### Step 10.3: 修改 App.vue 处理 scene 参数

**Files:**
- Modify: `app/App.vue`

### Step 10.4: 更新 pages.json

**Files:**
- Modify: `app/pages.json`

### Step 10.5: 提交

```bash
git add app/
git commit -m "feat(salesman): 商城端添加业务员绑定功能"
```

---

## 验证清单

完成所有任务后，执行以下验证：

1. **后端编译**: `cd crmeb && mvn clean compile`
2. **后端启动**: `cd crmeb/crmeb-admin && mvn spring-boot:run`
3. **Admin 前端启动**: `cd admin && npm run dev`
4. **业务员端启动**: 使用 HBuilderX 运行
5. **功能测试**:
   - 创建业务员
   - 生成邀请码/二维码
   - 业务员登录
   - 客户绑定
   - 数据统计

---

*本计划基于设计方案 `docs/plans/2026-01-21-salesman-phase1-design.md` 生成*
