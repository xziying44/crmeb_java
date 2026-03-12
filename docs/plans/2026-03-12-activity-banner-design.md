# 活动横幅功能设计文档

## 概述

在首页装修中新增"活动横幅"占位组件，实际横幅内容在后台营销模块中配置。用户点击横幅跳转到对应的活动页面（秒杀、砍价、拼团、买赠、满减）。同时隐藏现有的"活动边框"菜单。

## 需求要点

- 首页装修添加占位组件，不在装修中配置内容
- 后台营销模块新增"活动横幅"管理页面
- 隐藏现有"活动边框"菜单
- 横幅配置：图片 + 活动类型 + 排序
- 支持上线/下线和删除操作
- 多个横幅可同时上线，首页卡片样式纵向排列
- 手动排序控制显示顺序

## 数据库设计

新建 `eb_activity_banner` 表：

```sql
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

## 后端 API 设计

### 管理端（crmeb-admin）

新建 `ActivityBannerController`，路径前缀 `/api/admin/activity/banner`：

| 接口 | 方法 | 功能 |
|------|------|------|
| `/list` | GET | 分页列表查询（支持按状态筛选） |
| `/save` | POST | 新增横幅 |
| `/update` | POST | 编辑横幅 |
| `/delete/{id}` | GET | 删除横幅 |
| `/status` | POST | 上线/下线切换 |

### 移动端（crmeb-front）

| 接口 | 方法 | 功能 |
|------|------|------|
| `/api/front/activity/banner/list` | GET | 获取所有上线横幅（按 sort 升序） |

### 后端分层

- **Model**: `ActivityBanner`（crmeb-common/model/activity/）
- **Request**: `ActivityBannerRequest`、`ActivityBannerSearchRequest`（crmeb-common/request/）
- **Service**: `ActivityBannerService` + `ActivityBannerServiceImpl`（crmeb-service）
- **DAO**: `ActivityBannerDao`（crmeb-service）
- **Controller**: 管理端 + 移动端各一个

## 管理后台前端设计

### 营销菜单调整

- 隐藏"活动边框"菜单（`/marketing/border`，设置 `hidden: true`）
- 新增"活动横幅"菜单（`/marketing/banner`）

### 活动横幅管理页面

**操作栏**：「新增横幅」按钮

**表格列**：

| 列 | 内容 |
|----|------|
| ID | 自增 ID |
| 横幅图片 | 缩略图预览 |
| 横幅名称 | 文本 |
| 活动类型 | 标签（秒杀/砍价/拼团/买赠/满减） |
| 排序 | 数字（支持行内编辑） |
| 状态 | 上线/下线标签 |
| 操作 | 上线/下线按钮、编辑、删除 |

### 新增/编辑弹窗

- 横幅名称：输入框（必填）
- 横幅图片：图片上传（必填，建议尺寸 750×200）
- 活动类型：下拉选择（秒杀/砍价/拼团/买赠/满减，必填）
- 排序：数字输入框（默认 0）

## 首页装修组件设计

### 管理后台装修组件（占位符）

- 组件名：`home_activity_banner`
- 类型：type=1（营销组件）
- 行为：纯占位符，预览区显示"活动横幅"提示文字
- 配置面板：仅显示说明文字"活动横幅内容在 营销 > 活动横幅 中配置"
- 不提供任何配置项

### 移动端首页组件

新增 `app/components/homeIndex/activityBanner.vue`：

- created 中调用 `/api/front/activity/banner/list` 获取上线横幅
- 卡片样式渲染（带左右边距 + 圆角），纵向排列
- 点击跳转逻辑：
  - 1=秒杀 → `/pages/activity/goods_seckill/index`
  - 2=砍价 → `/pages/activity/goods_bargain/index`
  - 3=拼团 → `/pages/activity/goods_combination/index`
  - 4=买赠 → `/pages/activity/promotionList/index?name=买赠活动&type=4`
  - 5=满减 → `/pages/activity/promotionList/index?name=满减活动&type=4`
- 无上线横幅时不渲染（不占空间）

## 涉及文件清单

### 新增文件

**后端**：
- `crmeb-common/src/main/java/com/zbkj/common/model/activity/ActivityBanner.java`
- `crmeb-common/src/main/java/com/zbkj/common/request/ActivityBannerRequest.java`
- `crmeb-common/src/main/java/com/zbkj/common/request/ActivityBannerSearchRequest.java`
- `crmeb-service/src/main/java/com/zbkj/service/dao/ActivityBannerDao.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/ActivityBannerService.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/impl/ActivityBannerServiceImpl.java`
- `crmeb-admin/src/main/java/com/zbkj/admin/controller/ActivityBannerController.java`
- `crmeb-front/src/main/java/com/zbkj/front/controller/ActivityBannerController.java`
- SQL 迁移脚本

**管理前端**：
- `admin/src/views/marketing/banner/index.vue`（路由容器）
- `admin/src/views/marketing/banner/bannerList/index.vue`（列表页）
- `admin/src/api/activityBanner.js`

**装修组件**：
- `admin/src/views/design/components/mobilePage/home_activity_banner.vue`
- `admin/src/views/design/components/mobileConfig/c_home_activity_banner.vue`

**移动端**：
- `app/components/homeIndex/activityBanner.vue`

### 修改文件

- `admin/src/router/modules/marketing.js`（隐藏活动边框 + 新增活动横幅路由）
- `app/pages/index/index.vue`（注册并渲染 activityBanner 组件）
- MyBatis XML 映射（如需要）
