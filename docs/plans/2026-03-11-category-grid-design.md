# 首页装修组件：分类宫格 (category_grid) 设计文档

**日期**：2026-03-11
**状态**：已确认

## 需求概述

新增一个首页装修组件「分类宫格」，允许管理员从商品二级分类中选择要展示的热门分类，组件自动使用分类的图标和名称进行宫格显示。固定 5 列，行数根据选择的分类数量自动增长。点击分类项跳转到对应分类的商品列表页。

## 核心需求

1. **管理员手动选择**要展示的二级商品分类（从所有二级分类中勾选热门的）
2. **图标和名称自动获取**（从分类数据 `extra` 字段和 `name` 字段读取）
3. **固定 5 列**宫格布局，行数 = ceil(选中分类数 / 5)
4. **点击跳转**到该分类的商品列表页

## 技术方案

### 方案选择

采用 **新增独立组件** 方案（而非扩展 home_menu），原因：
- 现有系统每种功能为独立组件，符合设计哲学
- 不影响现有 `home_menu` 功能
- `mobilePage/index.js` 使用 `require.context` 自动扫描，零注册成本

### 数据流

```
管理端选择分类 → categoryIds 存入 PageDiy.value (JSON) → 移动端读取 → 调用 API 获取分类详情 → 渲染宫格
```

### 组件配置数据结构

存储在 `PageDiy.value` JSON 中的组件配置：

```json
{
  "name": "categoryGrid",
  "setUp": { "tabVal": 0, "cname": "分类宫格" },
  "categoryIds": [12, 15, 23, 45, 67, 88, 91, 102, 33, 56],
  "bgStyle": { "val": 0, "tabTitle": "圆角设置", "title": "背景圆角", "name": "bgStyle", "min": 0, "max": 30 },
  "bgColor": { "tabTitle": "颜色设置", "title": "背景颜色", "name": "bgColor", "color": [{"item": "#fff"}, {"item": "#fff"}] },
  "titleColor": { "title": "文字颜色", "name": "titleColor", "color": [{"item": "#282828"}] },
  "contentStyle": { "title": "图标圆角", "name": "contentStyle", "val": 30, "min": 0, "max": 30 },
  "contentConfig": { "title": "内容间距", "val": 10, "min": 0, "max": 30 },
  "upConfig": { "tabTitle": "边距设置", "title": "上边距", "val": 10, "min": 0, "max": 100 },
  "downConfig": { "title": "下边距", "val": 10, "min": 0 },
  "lrConfig": { "title": "左右边距", "val": 12, "min": 0, "max": 25 },
  "mbConfig": { "title": "页面间距", "val": 10, "min": 0 }
}
```

核心字段 `categoryIds` 存储管理员选择的二级分类 ID 数组。

### 后端 API

新增前端 API 端点：

```
GET /api/front/category/listByIds?ids=12,15,23,45
```

- 复用已有的 `CategoryService.getByIds(ids)` 方法
- 返回分类列表：`[{ id, name, extra(图标URL), pid }]`
- 无需认证（首页公开数据）

### 涉及文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `admin/src/views/design/components/mobilePage/home_category_grid.vue` | 新增 | 管理端中间预览组件 |
| `admin/src/views/design/components/mobileConfig/c_home_category_grid.vue` | 新增 | 管理端右侧配置面板（含分类选择器） |
| `app/components/homeIndex/categoryGrid.vue` | 新增 | 移动端渲染组件 |
| `app/pages/index/index.vue` | 修改 | 注册并渲染新组件 |
| `app/api/api.js` | 修改 | 添加批量获取分类 API |
| `crmeb-front/.../CategoryController.java` | 修改 | 新增 listByIds 端点 |

### 移动端渲染

- CSS Grid 布局：`grid-template-columns: repeat(5, 1fr)`
- 图标通过 `easy-loadimage` 组件加载
- 点击跳转：`/pages/goods/goods_list/index?cid=分类ID`
- 复用现有的样式体系（圆角、间距、背景色渐变等）

### 管理端预览

- 在 `mobilePage/home_category_grid.vue` 中预览宫格效果
- 预览时直接使用配置中的分类 ID 调用管理端分类 API 获取名称和图标
- 支持拖拽排序调整分类显示顺序
