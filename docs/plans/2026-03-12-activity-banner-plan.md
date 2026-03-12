# 活动横幅功能实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在首页装修中新增活动横幅占位组件，后台营销模块新增活动横幅管理页面，移动端渲染横幅并跳转到对应活动页面。

**Architecture:** 新建独立的 `eb_activity_banner` 表存储横幅数据，后端遵循 Controller→Service→DAO 三层架构，管理端前端使用 Element UI 表格+弹窗，移动端首页通过 API 获取上线横幅并渲染为卡片列表。

**Tech Stack:** SpringBoot 2.2.6 + MyBatis-Plus 3.3.1 + Vue 2 + Element UI + uni-app

---

### Task 1: 数据库迁移脚本

**Files:**
- Create: `crmeb/sql/activity_banner.sql`

**Step 1: 创建 SQL 迁移脚本**

```sql
-- 活动横幅表
CREATE TABLE `eb_activity_banner` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) NOT NULL DEFAULT '' COMMENT '横幅名称',
  `image` varchar(500) NOT NULL DEFAULT '' COMMENT '横幅图片地址',
  `activity_type` tinyint(4) NOT NULL COMMENT '活动类型：1=秒杀 2=砍价 3=拼团 4=买赠 5=满减',
  `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '状态：0=下线 1=上线',
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT '排序值（越小越靠前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动横幅表';
```

**Step 2: 执行 SQL**

```bash
docker-compose exec mysql mysql -u single_open -p single_open < crmeb/sql/activity_banner.sql
```

Expected: Query OK

**Step 3: 提交**

```bash
git add crmeb/sql/activity_banner.sql
git commit -m "feat: 添加活动横幅数据库表"
```

---

### Task 2: 后端 Model 和 Request 类 (crmeb-common)

**Files:**
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/model/activity/ActivityBanner.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/ActivityBannerRequest.java`
- Create: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/ActivityBannerSearchRequest.java`

**Step 1: 创建 Model 实体类**

参考 `ActivityStyle.java` 的风格，创建 `ActivityBanner.java`：

```java
package com.zbkj.common.model.activity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 活动横幅
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_activity_banner")
@ApiModel(value = "ActivityBanner对象", description = "活动横幅")
public class ActivityBanner implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "横幅名称")
    private String name;

    @ApiModelProperty(value = "横幅图片地址")
    private String image;

    @ApiModelProperty(value = "活动类型：1=秒杀 2=砍价 3=拼团 4=买赠 5=满减")
    private Integer activityType;

    @ApiModelProperty(value = "状态：0=下线 1=上线")
    private Integer status;

    @ApiModelProperty(value = "排序值（越小越靠前）")
    private Integer sort;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;
}
```

**Step 2: 创建 Request 类**

`ActivityBannerRequest.java`（新增/编辑请求）：

```java
package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 活动横幅请求对象
 */
@Data
@ApiModel(value = "ActivityBannerRequest对象", description = "活动横幅请求")
public class ActivityBannerRequest {

    @ApiModelProperty(value = "主键（编辑时必填）")
    private Integer id;

    @NotEmpty(message = "横幅名称不能为空")
    @ApiModelProperty(value = "横幅名称", required = true)
    private String name;

    @NotEmpty(message = "横幅图片不能为空")
    @ApiModelProperty(value = "横幅图片地址", required = true)
    private String image;

    @NotNull(message = "活动类型不能为空")
    @ApiModelProperty(value = "活动类型：1=秒杀 2=砍价 3=拼团 4=买赠 5=满减", required = true)
    private Integer activityType;

    @ApiModelProperty(value = "排序值（越小越靠前）")
    private Integer sort;
}
```

`ActivityBannerSearchRequest.java`（列表查询请求）：

```java
package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 活动横幅搜索请求对象
 */
@Data
@ApiModel(value = "ActivityBannerSearchRequest对象", description = "活动横幅搜索请求")
public class ActivityBannerSearchRequest {

    @ApiModelProperty(value = "状态：0=下线 1=上线")
    private Integer status;

    @ApiModelProperty(value = "横幅名称（模糊查询）")
    private String name;
}
```

**Step 3: 编译验证**

```bash
cd crmeb && mvn compile -pl crmeb-common -DskipTests
```

Expected: BUILD SUCCESS

**Step 4: 提交**

```bash
git add crmeb/crmeb-common/src/main/java/com/zbkj/common/model/activity/ActivityBanner.java crmeb/crmeb-common/src/main/java/com/zbkj/common/request/ActivityBannerRequest.java crmeb/crmeb-common/src/main/java/com/zbkj/common/request/ActivityBannerSearchRequest.java
git commit -m "feat: 添加活动横幅 Model 和 Request 类"
```

---

### Task 3: 后端 DAO 和 Service 层 (crmeb-service)

**Files:**
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/ActivityBannerDao.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/ActivityBannerService.java`
- Create: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/ActivityBannerServiceImpl.java`

**Step 1: 创建 DAO**

参考 `ActivityStyleDao.java`：

```java
package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.activity.ActivityBanner;

/**
 * 活动横幅 Mapper 接口
 */
public interface ActivityBannerDao extends BaseMapper<ActivityBanner> {
}
```

**Step 2: 创建 Service 接口**

```java
package com.zbkj.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.activity.ActivityBanner;
import com.zbkj.common.request.ActivityBannerSearchRequest;
import com.zbkj.common.request.PageParamRequest;

import java.util.List;

/**
 * 活动横幅 Service 接口
 */
public interface ActivityBannerService extends IService<ActivityBanner> {

    /**
     * 分页查询活动横幅
     */
    PageInfo<ActivityBanner> getList(ActivityBannerSearchRequest request, PageParamRequest pageParamRequest);

    /**
     * 更新状态（上线/下线）
     */
    boolean updateStatus(Integer id, Integer status);

    /**
     * 获取所有上线横幅（按 sort 升序），用于移动端展示
     */
    List<ActivityBanner> getOnlineList();
}
```

**Step 3: 创建 Service 实现**

参考 `ActivityStyleServiceImpl.java` 风格：

```java
package com.zbkj.service.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.activity.ActivityBanner;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.ActivityBannerSearchRequest;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.service.dao.ActivityBannerDao;
import com.zbkj.service.service.ActivityBannerService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 活动横幅 Service 实现
 */
@Service
public class ActivityBannerServiceImpl extends ServiceImpl<ActivityBannerDao, ActivityBanner> implements ActivityBannerService {

    @Resource
    private ActivityBannerDao dao;

    @Override
    public PageInfo<ActivityBanner> getList(ActivityBannerSearchRequest request, PageParamRequest pageParamRequest) {
        Page<ActivityBanner> page = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<ActivityBanner> wrapper = Wrappers.lambdaQuery();
        if (ObjectUtil.isNotEmpty(request.getStatus())) {
            wrapper.eq(ActivityBanner::getStatus, request.getStatus());
        }
        if (ObjectUtil.isNotEmpty(request.getName())) {
            wrapper.like(ActivityBanner::getName, URLUtil.decode(request.getName()));
        }
        wrapper.orderByAsc(ActivityBanner::getSort);
        wrapper.orderByDesc(ActivityBanner::getCreateTime);
        List<ActivityBanner> list = dao.selectList(wrapper);
        return CommonPage.copyPageInfo(page, list);
    }

    @Override
    public boolean updateStatus(Integer id, Integer status) {
        ActivityBanner banner = new ActivityBanner();
        banner.setId(id);
        banner.setStatus(status);
        banner.setUpdateTime(DateUtil.date());
        return updateById(banner);
    }

    @Override
    public List<ActivityBanner> getOnlineList() {
        LambdaQueryWrapper<ActivityBanner> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ActivityBanner::getStatus, 1);
        wrapper.orderByAsc(ActivityBanner::getSort);
        wrapper.orderByDesc(ActivityBanner::getCreateTime);
        return dao.selectList(wrapper);
    }
}
```

**Step 4: 编译验证**

```bash
cd crmeb && mvn compile -pl crmeb-common,crmeb-service -DskipTests
```

Expected: BUILD SUCCESS

**Step 5: 提交**

```bash
git add crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/ActivityBannerDao.java crmeb/crmeb-service/src/main/java/com/zbkj/service/service/ActivityBannerService.java crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/ActivityBannerServiceImpl.java
git commit -m "feat: 添加活动横幅 DAO 和 Service 层"
```

---

### Task 4: 管理端 Controller (crmeb-admin)

**Files:**
- Create: `crmeb/crmeb-admin/src/main/java/com/zbkj/admin/controller/ActivityBannerController.java`

**Step 1: 创建 Controller**

参考 `ActivityStyleController.java` 的风格：

```java
package com.zbkj.admin.controller;

import cn.hutool.core.date.DateUtil;
import com.zbkj.common.model.activity.ActivityBanner;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.ActivityBannerRequest;
import com.zbkj.common.request.ActivityBannerSearchRequest;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.ActivityBannerService;
import com.zbkj.service.service.SystemAttachmentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 活动横幅管理 控制器
 */
@Slf4j
@RestController
@RequestMapping("api/admin/activity/banner")
@Api(tags = "活动横幅")
public class ActivityBannerController {

    @Autowired
    private ActivityBannerService activityBannerService;

    @Autowired
    private SystemAttachmentService systemAttachmentService;

    /**
     * 分页列表
     */
    @PreAuthorize("hasAuthority('admin:activity:banner:list')")
    @ApiOperation(value = "分页列表")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public CommonResult<CommonPage<ActivityBanner>> getList(@Validated ActivityBannerSearchRequest request,
                                                            @ModelAttribute PageParamRequest pageParamRequest) {
        CommonPage<ActivityBanner> page = CommonPage.restPage(activityBannerService.getList(request, pageParamRequest));
        return CommonResult.success(page);
    }

    /**
     * 新增
     */
    @PreAuthorize("hasAuthority('admin:activity:banner:save')")
    @ApiOperation(value = "新增")
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public CommonResult<String> save(@RequestBody @Validated ActivityBannerRequest request) {
        ActivityBanner banner = new ActivityBanner();
        BeanUtils.copyProperties(request, banner);
        banner.setImage(systemAttachmentService.clearPrefix(banner.getImage()));
        banner.setStatus(0);
        if (banner.getSort() == null) {
            banner.setSort(0);
        }
        if (activityBannerService.save(banner)) {
            return CommonResult.success();
        }
        return CommonResult.failed();
    }

    /**
     * 修改
     */
    @PreAuthorize("hasAuthority('admin:activity:banner:update')")
    @ApiOperation(value = "修改")
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public CommonResult<String> update(@RequestBody @Validated ActivityBannerRequest request) {
        ActivityBanner banner = new ActivityBanner();
        BeanUtils.copyProperties(request, banner);
        banner.setId(request.getId());
        banner.setImage(systemAttachmentService.clearPrefix(banner.getImage()));
        banner.setUpdateTime(DateUtil.date());
        if (activityBannerService.updateById(banner)) {
            return CommonResult.success();
        }
        return CommonResult.failed();
    }

    /**
     * 删除
     */
    @PreAuthorize("hasAuthority('admin:activity:banner:delete')")
    @ApiOperation(value = "删除")
    @RequestMapping(value = "/delete/{id}", method = RequestMethod.GET)
    public CommonResult<String> delete(@PathVariable Integer id) {
        if (activityBannerService.removeById(id)) {
            return CommonResult.success();
        }
        return CommonResult.failed();
    }

    /**
     * 更新状态（上线/下线）
     */
    @PreAuthorize("hasAuthority('admin:activity:banner:status')")
    @ApiOperation(value = "更新状态")
    @RequestMapping(value = "/status", method = RequestMethod.POST)
    public CommonResult<String> updateStatus(@RequestParam Integer id, @RequestParam Integer status) {
        if (activityBannerService.updateStatus(id, status)) {
            return CommonResult.success();
        }
        return CommonResult.failed();
    }
}
```

**Step 2: 编译验证**

```bash
cd crmeb && mvn compile -DskipTests
```

Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git add crmeb/crmeb-admin/src/main/java/com/zbkj/admin/controller/ActivityBannerController.java
git commit -m "feat: 添加活动横幅管理端 Controller"
```

---

### Task 5: 移动端 Controller (crmeb-front)

**Files:**
- Create: `crmeb/crmeb-front/src/main/java/com/zbkj/front/controller/ActivityBannerController.java`

**Step 1: 创建移动端 Controller**

```java
package com.zbkj.front.controller;

import com.zbkj.common.model.activity.ActivityBanner;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.ActivityBannerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 活动横幅 移动端控制器
 */
@Slf4j
@RestController
@RequestMapping("api/front/activity/banner")
@Api(tags = "活动横幅")
public class ActivityBannerController {

    @Autowired
    private ActivityBannerService activityBannerService;

    /**
     * 获取所有上线横幅
     */
    @ApiOperation(value = "获取上线横幅列表")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public CommonResult<List<ActivityBanner>> getOnlineList() {
        return CommonResult.success(activityBannerService.getOnlineList());
    }
}
```

**Step 2: 将该接口添加到白名单**

检查 `crmeb-front` 的安全配置，确认 `/api/front/**` 路径是否已在白名单中（通常 front API 不需要管理员权限，但可能需要确认 noAuth 配置）。

```bash
cd crmeb && mvn compile -DskipTests
```

Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git add crmeb/crmeb-front/src/main/java/com/zbkj/front/controller/ActivityBannerController.java
git commit -m "feat: 添加活动横幅移动端 Controller"
```

---

### Task 6: 管理前端 - API 封装和路由配置

**Files:**
- Create: `admin/src/api/activityBanner.js`
- Modify: `admin/src/router/modules/marketing.js`

**Step 1: 创建 API 封装文件**

参考 `admin/src/api/marketing.js` 中的 `atuosphereList` 等方法：

```javascript
// +----------------------------------------------------------------------
// | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
// +----------------------------------------------------------------------
// | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
// +----------------------------------------------------------------------
// | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
// +----------------------------------------------------------------------
// | Author: CRMEB Team <admin@crmeb.com>
// +----------------------------------------------------------------------

import request from '@/utils/request'

/**
 * 活动横幅列表
 */
export function activityBannerListApi(params) {
  return request({
    url: '/admin/activity/banner/list',
    method: 'get',
    params,
  })
}

/**
 * 新增活动横幅
 */
export function activityBannerSaveApi(data) {
  return request({
    url: '/admin/activity/banner/save',
    method: 'post',
    data,
  })
}

/**
 * 编辑活动横幅
 */
export function activityBannerUpdateApi(data) {
  return request({
    url: '/admin/activity/banner/update',
    method: 'post',
    data,
  })
}

/**
 * 删除活动横幅
 */
export function activityBannerDeleteApi(id) {
  return request({
    url: `/admin/activity/banner/delete/${id}`,
    method: 'get',
  })
}

/**
 * 更新活动横幅状态
 */
export function activityBannerStatusApi(params) {
  return request({
    url: '/admin/activity/banner/status',
    method: 'post',
    params,
  })
}
```

**Step 2: 修改路由配置**

修改 `admin/src/router/modules/marketing.js`：

1. 隐藏"活动边框"：在 `border` 路由对象上添加 `hidden: true`
2. 新增"活动横幅"路由

在 `border` 路由块（约第 231 行）添加 `hidden: true`：

```javascript
    {
      path: 'border',
      name: 'border',
      meta: { title: '活动边框', icon: '' },
      hidden: true,  // 隐藏活动边框菜单
      component: () => import('@/views/marketing/border/index'),
      // ...children 保持不变
    },
```

在 `border` 路由后面添加活动横幅路由：

```javascript
    {
      path: 'banner',
      name: 'activityBanner',
      meta: { title: '活动横幅', icon: '' },
      component: () => import('@/views/marketing/banner/index'),
      children: [
        {
          path: 'list',
          name: 'activityBannerList',
          meta: {
            title: '活动横幅列表',
            noCache: true,
          },
          component: () => import('@/views/marketing/banner/bannerList/index'),
        },
      ],
    },
```

**Step 3: 提交**

```bash
git add admin/src/api/activityBanner.js admin/src/router/modules/marketing.js
git commit -m "feat: 添加活动横幅 API 封装和路由配置，隐藏活动边框菜单"
```

---

### Task 7: 管理前端 - 活动横幅管理页面

**Files:**
- Create: `admin/src/views/marketing/banner/index.vue`
- Create: `admin/src/views/marketing/banner/bannerList/index.vue`

**Step 1: 创建路由容器页面**

`admin/src/views/marketing/banner/index.vue`（参考 `admin/src/views/marketing/border/index.vue`）：

```vue
<template>
  <router-view />
</template>
<script>
export default {
  name: 'activityBanner',
}
</script>
```

**Step 2: 创建横幅列表管理页面**

`admin/src/views/marketing/banner/bannerList/index.vue`：

参考 `admin/src/views/marketing/atmosphere/atmosphereList/list.vue` 的结构，简化为横幅管理所需的功能。

页面包含：
- 搜索栏：横幅名称、状态筛选
- 新增按钮 → 打开弹窗
- 表格：ID、图片、名称、活动类型、排序（行内编辑）、状态标签、操作按钮
- 新增/编辑弹窗：名称、图片上传、活动类型下拉、排序输入

```vue
<template>
  <div class="divBox">
    <el-card :bordered="false" shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form size="small" :inline="true" @submit.native.prevent>
          <el-form-item label="横幅名称：">
            <el-input
              v-model="searchName"
              placeholder="请输入横幅名称"
              class="selWidth"
              clearable
              @keyup.enter.native="getList(1)"
            />
          </el-form-item>
          <el-form-item label="状态：">
            <el-select v-model="tableFrom.status" placeholder="全部" class="selWidth" clearable @change="getList(1)">
              <el-option label="上线" :value="1" />
              <el-option label="下线" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="getList(1)">搜索</el-button>
            <el-button @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
    <el-card class="box-card mt14" :body-style="{ padding: '20px' }" :bordered="false" shadow="never">
      <el-button type="primary" @click="handleAdd">新增横幅</el-button>
      <el-table :data="tableData.data" style="width: 100%" class="mt20" v-loading="listLoading">
        <el-table-column prop="id" label="ID" min-width="60" />
        <el-table-column label="横幅图片" min-width="120">
          <template slot-scope="scope">
            <el-image
              style="width: 80px; height: 30px"
              :src="scope.row.image"
              :preview-src-list="[scope.row.image]"
              fit="cover"
            />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="横幅名称" min-width="150" :show-overflow-tooltip="true" />
        <el-table-column label="活动类型" min-width="100">
          <template slot-scope="scope">
            <el-tag :type="activityTypeTag(scope.row.activityType)">
              {{ activityTypeLabel(scope.row.activityType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="排序" min-width="100">
          <template slot-scope="scope">
            <el-input-number
              v-model="scope.row.sort"
              :min="0"
              size="mini"
              controls-position="right"
              @change="handleSortChange(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="80">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '上线' : '下线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template slot-scope="scope">
            <el-button
              type="text"
              size="small"
              @click="handleStatusChange(scope.row)"
            >
              {{ scope.row.status === 1 ? '下线' : '上线' }}
            </el-button>
            <el-divider direction="vertical" />
            <el-button type="text" size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-button type="text" size="small" style="color: #f56c6c" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="block">
        <el-pagination
          background
          :page-sizes="[20, 40, 60, 80]"
          :page-size="tableFrom.limit"
          :current-page="tableFrom.page"
          layout="total, sizes, prev, pager, next, jumper"
          :total="tableData.total"
          @size-change="handleSizeChange"
          @current-change="pageChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="600px" @close="handleDialogClose">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="横幅名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入横幅名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="横幅图片" prop="image">
          <div class="upLoadPicBox" @click="modalPicTap">
            <div v-if="formData.image" class="pictrue">
              <img :src="formData.image" />
            </div>
            <div v-else class="upLoad">
              <i class="el-icon-camera cameraIconfont" />
            </div>
          </div>
          <div class="tips">建议尺寸 750×200px</div>
        </el-form-item>
        <el-form-item label="活动类型" prop="activityType">
          <el-select v-model="formData.activityType" placeholder="请选择活动类型">
            <el-option label="秒杀" :value="1" />
            <el-option label="砍价" :value="2" />
            <el-option label="拼团" :value="3" />
            <el-option label="买赠" :value="4" />
            <el-option label="满减" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="formData.sort" :min="0" controls-position="right" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  activityBannerListApi,
  activityBannerSaveApi,
  activityBannerUpdateApi,
  activityBannerDeleteApi,
  activityBannerStatusApi,
} from '@/api/activityBanner'

const activityTypeMap = {
  1: { label: '秒杀', tag: 'danger' },
  2: { label: '砍价', tag: 'warning' },
  3: { label: '拼团', tag: 'success' },
  4: { label: '买赠', tag: '' },
  5: { label: '满减', tag: 'info' },
}

export default {
  name: 'ActivityBannerList',
  data() {
    return {
      listLoading: false,
      tableData: { data: [], total: 0 },
      tableFrom: { page: 1, limit: 20, status: '', name: '' },
      searchName: '',
      dialogVisible: false,
      dialogTitle: '新增横幅',
      submitLoading: false,
      isEdit: false,
      formData: {
        id: null,
        name: '',
        image: '',
        activityType: null,
        sort: 0,
      },
      formRules: {
        name: [{ required: true, message: '请输入横幅名称', trigger: 'blur' }],
        image: [{ required: true, message: '请上传横幅图片', trigger: 'change' }],
        activityType: [{ required: true, message: '请选择活动类型', trigger: 'change' }],
      },
    }
  },
  mounted() {
    this.getList(1)
  },
  methods: {
    activityTypeLabel(type) {
      return activityTypeMap[type] ? activityTypeMap[type].label : '未知'
    },
    activityTypeTag(type) {
      return activityTypeMap[type] ? activityTypeMap[type].tag : ''
    },
    getList(num) {
      this.listLoading = true
      this.tableFrom.page = num || this.tableFrom.page
      this.tableFrom.name = this.searchName ? encodeURIComponent(this.searchName) : ''
      activityBannerListApi(this.tableFrom)
        .then((res) => {
          this.tableData.data = res.list
          this.tableData.total = res.total
          this.listLoading = false
        })
        .catch(() => {
          this.listLoading = false
        })
    },
    reset() {
      this.searchName = ''
      this.tableFrom = { page: 1, limit: 20, status: '', name: '' }
      this.getList(1)
    },
    pageChange(page) {
      this.tableFrom.page = page
      this.getList()
    },
    handleSizeChange(val) {
      this.tableFrom.limit = val
      this.getList(1)
    },
    handleAdd() {
      this.isEdit = false
      this.dialogTitle = '新增横幅'
      this.formData = { id: null, name: '', image: '', activityType: null, sort: 0 }
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.isEdit = true
      this.dialogTitle = '编辑横幅'
      this.formData = {
        id: row.id,
        name: row.name,
        image: row.image,
        activityType: row.activityType,
        sort: row.sort,
      }
      this.dialogVisible = true
    },
    handleDialogClose() {
      this.$refs.formRef && this.$refs.formRef.resetFields()
    },
    handleSubmit() {
      this.$refs.formRef.validate((valid) => {
        if (!valid) return
        this.submitLoading = true
        const api = this.isEdit ? activityBannerUpdateApi : activityBannerSaveApi
        api(this.formData)
          .then(() => {
            this.$message.success(this.isEdit ? '编辑成功' : '新增成功')
            this.dialogVisible = false
            this.getList()
          })
          .finally(() => {
            this.submitLoading = false
          })
      })
    },
    handleDelete(row) {
      this.$modalSure('删除后将无法恢复，请谨慎操作!').then(() => {
        activityBannerDeleteApi(row.id).then(() => {
          this.$message.success('删除成功')
          this.getList()
        })
      })
    },
    handleStatusChange(row) {
      const newStatus = row.status === 1 ? 0 : 1
      const text = newStatus === 1 ? '上线' : '下线'
      activityBannerStatusApi({ id: row.id, status: newStatus }).then(() => {
        this.$message.success(`${text}成功`)
        this.getList()
      })
    },
    handleSortChange(row) {
      activityBannerUpdateApi({ id: row.id, name: row.name, image: row.image, activityType: row.activityType, sort: row.sort }).then(() => {
        this.$message.success('排序已更新')
      })
    },
    // 选择图片
    modalPicTap() {
      this.$modalUpload(function (img) {
        this.formData.image = img[0]
      }.bind(this), 1)
    },
  },
}
</script>

<style scoped lang="scss">
.upLoadPicBox {
  display: inline-block;
  cursor: pointer;
  .pictrue {
    width: 120px;
    height: 50px;
    border: 1px dotted rgba(0, 0, 0, 0.1);
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
  .upLoad {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 120px;
    height: 50px;
    border: 1px dotted rgba(0, 0, 0, 0.1);
    .cameraIconfont {
      font-size: 24px;
      color: #c0c4cc;
    }
  }
}
.tips {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>
```

**Step 3: 验证前端编译**

```bash
cd admin && npm run lint -- --fix
```

**Step 4: 提交**

```bash
git add admin/src/views/marketing/banner/
git commit -m "feat: 添加活动横幅管理页面"
```

---

### Task 8: 管理前端 - 装修占位组件

**Files:**
- Create: `admin/src/views/design/components/mobilePage/home_activity_banner.vue`
- Create: `admin/src/views/design/components/mobileConfig/c_home_activity_banner.vue`

**Step 1: 创建装修预览组件**

`home_activity_banner.vue` - 纯占位符，参考 `home_seckill.vue` 的元数据结构但极简化：

```vue
<template>
  <div class="activity-banner-placeholder" :style="boxStyle" v-if="configObj">
    <div class="placeholder-content">
      <i class="el-icon-picture-outline"></i>
      <span>活动横幅</span>
      <p class="tip">在 营销 > 活动横幅 中配置内容</p>
    </div>
  </div>
</template>

<script>
import { mapState } from 'vuex';
export default {
  name: 'home_activity_banner',
  cname: '活动横幅',
  icon: 't-icon-zujian-lunbotu',
  configName: 'c_home_activity_banner',
  type: 1,
  defaultName: 'activityBanner',
  props: {
    index: { type: null },
    num: { type: null },
  },
  computed: {
    ...mapState('mobildConfig', ['defaultArray']),
    boxStyle() {
      return [
        { margin: this.configObj.mbConfig.val + 'px' + ' ' + this.configObj.lrConfig.val + 'px' + ' 0' },
      ];
    },
  },
  watch: {
    num: {
      handler(nVal) {
        let data = this.$store.state.mobildConfig.defaultArray[nVal];
        this.setConfig(data);
      },
      deep: true,
    },
    defaultArray: {
      handler() {
        let data = this.$store.state.mobildConfig.defaultArray[this.num];
        this.setConfig(data);
      },
      deep: true,
    },
  },
  data() {
    return {
      defaultConfig: {
        isHide: false,
        name: 'activityBanner',
        timestamp: this.num,
        setUp: {
          tabVal: 0,
          cname: '活动横幅',
        },
        mbConfig: {
          title: '页面间距',
          val: 10,
          min: 0,
        },
        lrConfig: {
          title: '左右边距',
          val: 12,
          min: 0,
          max: 25,
        },
      },
      configObj: null,
    };
  },
  mounted() {
    this.$nextTick(() => {
      if (this.num) {
        let data = this.$store.state.mobildConfig.defaultArray[this.num];
        this.setConfig(data);
      }
    });
  },
  methods: {
    setConfig(data) {
      if (!data) return;
      this.configObj = data;
    },
  },
};
</script>

<style scoped lang="scss">
.activity-banner-placeholder {
  background: #f5f5f5;
  border: 1px dashed #ddd;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  .placeholder-content {
    color: #999;
    i {
      font-size: 28px;
      display: block;
      margin-bottom: 6px;
    }
    span {
      font-size: 14px;
      font-weight: bold;
    }
    .tip {
      font-size: 12px;
      margin-top: 4px;
      color: #bbb;
    }
  }
}
</style>
```

**Step 2: 创建装修配置组件**

`c_home_activity_banner.vue` - 极简配置面板，只显示说明文字：

```vue
<template>
  <div class="mobile-config">
    <Form ref="formInline">
      <div v-for="(item, key) in rCom" :key="key">
        <component
          :is="item.components.name"
          :configObj="configObj"
          ref="childData"
          :configNme="item.configNme"
          :key="key"
          @getConfig="getConfig"
          :index="activeIndex"
          :num="item.num"
        ></component>
      </div>
      <div class="config-tip">
        <el-alert
          title="活动横幅内容在 营销 > 活动横幅 中配置"
          type="info"
          :closable="false"
          show-icon
        />
      </div>
      <rightBtn :activeIndex="activeIndex" :configObj="configObj"></rightBtn>
    </Form>
  </div>
</template>

<script>
import toolCom from '../mobileConfigRight/index.js';
import rightBtn from '../rightBtn/index.vue';
export default {
  name: 'c_home_activity_banner',
  componentsName: 'home_activity_banner',
  cname: '活动横幅',
  props: {
    activeIndex: { type: null },
    num: { type: null },
    index: { type: null },
  },
  components: {
    ...toolCom,
    rightBtn,
  },
  data() {
    return {
      configObj: {},
      rCom: [
        {
          components: toolCom.c_checked_tab,
          configNme: 'setUp',
        },
      ],
    };
  },
  watch: {
    num(nVal) {
      let value = JSON.parse(JSON.stringify(this.$store.state.mobildConfig.defaultArray[nVal]));
      this.configObj = value;
    },
    configObj: {
      handler(nVal) {
        this.$store.commit('mobildConfig/UPDATEARR', { num: this.num, val: nVal });
      },
      deep: true,
    },
    'configObj.setUp.tabVal': {
      handler(nVal) {
        var arr = [this.rCom[0]];
        if (nVal == 1) {
          let tempArr = [
            { components: toolCom.c_title, configNme: 'mbConfig' },
            { components: toolCom.c_slider, configNme: 'mbConfig' },
            { components: toolCom.c_slider, configNme: 'lrConfig' },
          ];
          this.rCom = arr.concat(tempArr);
        } else {
          this.rCom = arr;
        }
      },
      deep: true,
    },
  },
  mounted() {
    this.$nextTick(() => {
      let value = JSON.parse(JSON.stringify(this.$store.state.mobildConfig.defaultArray[this.num]));
      this.configObj = value;
    });
  },
  methods: {
    getConfig() {},
  },
};
</script>

<style scoped lang="scss">
.config-tip {
  padding: 10px 15px;
}
</style>
```

**Step 3: 提交**

```bash
git add admin/src/views/design/components/mobilePage/home_activity_banner.vue admin/src/views/design/components/mobileConfig/c_home_activity_banner.vue
git commit -m "feat: 添加活动横幅装修占位组件"
```

---

### Task 9: 移动端 - 活动横幅组件和首页集成

**Files:**
- Create: `app/components/homeIndex/activityBanner.vue`
- Modify: `app/api/activity.js`（约第 232 行后添加）
- Modify: `app/pages/index/index.vue`（导入区和模板区）

**Step 1: 在活动 API 中添加横幅接口**

在 `app/api/activity.js` 末尾添加：

```javascript
/**
 * 获取上线活动横幅列表
 */
export function getActivityBannerListApi() {
  return request.get('activity/banner/list', {}, { noAuth: true });
}
```

**Step 2: 创建活动横幅组件**

`app/components/homeIndex/activityBanner.vue`：

```vue
<template>
  <view v-if="bannerList.length > 0" class="activity-banner-wrap" :style="wrapStyle">
    <view
      v-for="(item, index) in bannerList"
      :key="item.id"
      class="banner-card"
      :style="[cardStyle, index > 0 ? { marginTop: '16rpx' } : {}]"
      @click="handleClick(item)"
    >
      <image
        :src="item.image"
        mode="widthFix"
        class="banner-image"
        :style="imageStyle"
      />
    </view>
  </view>
</template>

<script>
import { getActivityBannerListApi } from '@/api/activity.js';

const activityRouteMap = {
  1: '/pages/activity/goods_seckill/index',
  2: '/pages/activity/goods_bargain/index',
  3: '/pages/activity/goods_combination/index',
  4: '/pages/activity/promotionList/index?name=买赠活动&type=4',
  5: '/pages/activity/promotionList/index?name=满减活动&type=4',
};

export default {
  name: 'activityBanner',
  props: {
    dataConfig: {
      type: Object,
      default: () => ({}),
    },
  },
  data() {
    return {
      bannerList: [],
    };
  },
  computed: {
    wrapStyle() {
      const lr = this.dataConfig.lrConfig ? this.dataConfig.lrConfig.val * 2 : 24;
      const mb = this.dataConfig.mbConfig ? this.dataConfig.mbConfig.val * 2 : 20;
      return {
        padding: `0 ${lr}rpx`,
        marginTop: `${mb}rpx`,
      };
    },
    cardStyle() {
      return {
        borderRadius: '16rpx',
        overflow: 'hidden',
      };
    },
    imageStyle() {
      return {
        width: '100%',
        display: 'block',
        borderRadius: '16rpx',
      };
    },
  },
  created() {
    this.loadBanners();
  },
  methods: {
    loadBanners() {
      getActivityBannerListApi()
        .then((res) => {
          this.bannerList = res.data || [];
        })
        .catch(() => {
          this.bannerList = [];
        });
    },
    handleClick(item) {
      const url = activityRouteMap[item.activityType];
      if (url) {
        uni.navigateTo({ url });
      }
    },
  },
};
</script>

<style scoped>
.activity-banner-wrap {
  box-sizing: border-box;
}
.banner-card {
  width: 100%;
}
</style>
```

**Step 3: 在首页注册和渲染组件**

修改 `app/pages/index/index.vue`：

1. 在 import 区域（约第 154 行 `categoryGrid` 之后）添加：

```javascript
import activityBanner from '@/components/homeIndex/activityBanner.vue';
```

2. 在 `components` 对象中（约第 213 行 `categoryGrid` 之后）添加：

```javascript
activityBanner,
```

3. 在模板的 `v-for` 循环中（约第 54 行 `categoryGrid` 之后）添加：

```vue
<!-- 活动横幅 -->
<activityBanner v-if="item.name == 'activityBanner'&&!item.isHide" :dataConfig="item"></activityBanner>
```

**Step 4: 提交**

```bash
git add app/components/homeIndex/activityBanner.vue app/api/activity.js app/pages/index/index.vue
git commit -m "feat: 添加移动端活动横幅组件并集成到首页"
```

---

### Task 10: 全量编译验证

**Step 1: 后端全量编译**

```bash
cd crmeb && mvn clean compile -DskipTests
```

Expected: BUILD SUCCESS（所有 4 个模块编译通过）

**Step 2: 前端 lint 检查**

```bash
cd admin && npm run lint -- --fix
```

Expected: 无严重错误

**Step 3: 提交并检查所有变更**

```bash
git status
git log --oneline -10
```

Expected: 所有 Task 的提交记录都在
