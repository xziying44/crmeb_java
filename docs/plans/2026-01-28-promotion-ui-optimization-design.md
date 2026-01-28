# 促销模块 UI/UX 优化设计文档

> 创建日期：2026-01-28
> 状态：已确认，待实施

## 1. 背景与目标

### 问题概述

最近一次提交（`8ffcc6a3 促销拓展初步实施`）中的促销功能存在以下 UI/UX 问题：

| 模块 | 问题 | 严重程度 |
|-----|------|---------|
| 满减活动 | 关联 ID 列表需手动输入，用户不友好 | 高 |
| 买赠活动 | 商品配置需手动输入 JSON 格式 | 高 |
| 代金券 | 搜索栏与表格间距过小 | 低 |
| 代金券 | 发放交互需手动输入用户 ID | 中 |
| 代金券 | 缺少用户购买功能 | 中 |

### 设计原则

- **最小改动**：复用现有组件，避免大规模重构
- **一致性**：与项目现有交互模式保持一致
- **用户友好**：运营人员无需了解技术细节即可操作

---

## 2. 详细设计

### 2.1 满减活动 - 商品/品类选择器改造

**文件**：`admin/src/views/marketing/promotion/fullReduction/components/edit.vue`

**当前实现**（第 43-50 行）：
```html
<el-form-item v-if="form.scopeType !== 1" label="关联ID列表" prop="relationIdsStr">
  <el-input v-model="form.relationIdsStr" type="textarea" :rows="3"
    placeholder="请输入关联的ID，多个用英文逗号分隔..." />
</el-form-item>
```

**改造方案**：

根据 `scopeType` 的值动态展示不同的选择器：

1. **scopeType=2（品类）**：
   - 复用 `categoryApi` + `el-cascader` 级联选择器
   - 参考 `creatCoupon.vue:16-23` 的实现
   - 支持多选品类

2. **scopeType=3（指定商品）**：
   - 复用 `$modalGoodList` 弹框，传入 `'many'` 参数启用多选
   - 选中的商品以缩略图列表形式展示
   - 参考 `creatCoupon.vue:26-38` 的商品图展示模式

**数据结构变更**：
```javascript
// 原
form.relationIdsStr: ''  // 字符串，逗号分隔

// 新
form.selectedCategoryIds: []  // 品类 ID 数组
form.selectedProducts: []     // 商品对象数组 [{id, image, storeName}]
```

**提交时转换**：
```javascript
const relationIds = this.form.scopeType === 2
  ? this.form.selectedCategoryIds
  : this.form.selectedProducts.map(p => p.id)
```

---

### 2.2 买赠活动 - 可视化商品配置

**文件**：`admin/src/views/marketing/promotion/buyGift/index.vue`

**当前实现**（第 127-143 行）：
```html
<el-form-item label="购买商品配置" required>
  <el-input v-model="form.buyProductsJson" type="textarea" :rows="5"
    placeholder='请输入JSON数组，如：[{"productId":1,"attrValueId":0,"buyNum":2}]' />
</el-form-item>
```

**改造方案**：

将 JSON 输入改为可视化表格配置：

**购买商品配置表格**：
```html
<el-table :data="form.buyProducts" size="mini" border>
  <el-table-column label="商品图" width="80">
    <template slot-scope="{ row }">
      <el-image :src="row.image" style="width:50px;height:50px" />
    </template>
  </el-table-column>
  <el-table-column prop="storeName" label="商品名称" min-width="150" />
  <el-table-column label="规格" min-width="120">
    <template slot-scope="{ row }">
      <el-select v-model="row.attrValueId" placeholder="请选择">
        <el-option v-for="attr in row.attrValues" :key="attr.id"
          :label="attr.sku" :value="attr.id" />
      </el-select>
    </template>
  </el-table-column>
  <el-table-column label="购买数量" width="120">
    <template slot-scope="{ row }">
      <el-input-number v-model="row.buyNum" :min="1" size="small" />
    </template>
  </el-table-column>
  <el-table-column label="操作" width="80">
    <template slot-scope="{ $index }">
      <el-button type="text" @click="removeBuyProduct($index)">删除</el-button>
    </template>
  </el-table-column>
</el-table>
<el-button size="small" @click="addBuyProduct">添加购买商品</el-button>
```

**赠品配置表格**：结构相同，`buyNum` 改为 `giftNum`

**数据结构**：
```javascript
form.buyProducts = [
  {
    productId: 1,
    storeName: 'xxx',
    image: 'xxx',
    attrValueId: 0,
    attrValues: [],  // 从商品详情 API 获取
    buyNum: 1
  }
]

form.giftProducts = [
  { productId: 2, storeName: 'xxx', image: 'xxx', attrValueId: 0, attrValues: [], giftNum: 1 }
]
```

**添加商品流程**：
1. 点击"添加购买商品" → 调用 `$modalGoodList` 选择商品
2. 选中后调用 `productDetailApi(id)` 获取商品规格信息
3. 填充到表格中，用户选择规格和数量

---

### 2.3 代金券 - 布局修复

**文件**：`admin/src/views/marketing/voucher/index.vue`

**当前问题**：搜索卡片与列表卡片之间间距过小。

**修复方案**：

第 3 行的 `el-card` 添加 `mb14` 类名：
```html
<!-- 原 -->
<el-card :bordered="false" shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">

<!-- 改 -->
<el-card :bordered="false" shadow="never" class="ivu-mt mb14" :body-style="{ padding: 0 }">
```

---

### 2.4 代金券 - 发放交互改进

**文件**：
- `admin/src/components/userList/userListFrom/index.js`（新建）
- `admin/src/components/userList/userListFrom/index.vue`（新建）
- `admin/src/views/marketing/voucher/index.vue`（修改）

**设计思路**：

参考 `$modalGoodList` 的实现模式，封装 `$modalUserList` 全局方法。

**新建 `userListFrom/index.js`**：
```javascript
import uploadFromComponent from './index.vue'

const userListFrom = {}

userListFrom.install = function (Vue) {
  const ToastConstructor = Vue.extend(uploadFromComponent)
  const instance = new ToastConstructor()
  instance.$mount(document.createElement('div'))
  document.body.appendChild(instance.$el)

  Vue.prototype.$modalUserList = function (callback, handleNum) {
    instance.visible = true
    instance.callback = callback
    instance.handleNum = handleNum || ''  // 'many' 为多选模式
  }
}

export default userListFrom
```

**新建 `userListFrom/index.vue`**：
```html
<template>
  <el-dialog title="选择用户" :visible.sync="visible" width="900px">
    <user-list-multi
      v-if="visible"
      :handleNum="handleNum"
      @getUsers="getUsers"
      @close="visible = false"
    />
  </el-dialog>
</template>
```

**修改 `userList/index.vue`**：
- 添加 `handleNum` prop 支持多选模式
- 多选模式下显示 checkbox 列
- 添加"确定"按钮返回选中的用户列表

**调用方式**：
```javascript
handleSend(row) {
  this.$modalUserList((users) => {
    const uids = users.map(u => u.uid).join(',')
    voucherSendApi(row.id, uids).then(() => {
      this.$message.success('发放成功')
    })
  }, 'many')
}
```

---

### 2.5 代金券 - 购买功能扩展

**文件**：
- `admin/src/views/marketing/voucher/index.vue`（前端）
- `crmeb/crmeb-common/src/main/java/com/zbkj/common/model/coupon/StoreCoupon.java`（后端模型）
- `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/StoreCouponRequest.java`（后端请求）

**前端表单扩展**：

在新增/编辑弹窗中添加：
```html
<el-form-item label="是否可购买">
  <el-switch v-model="form.canBuy" :active-value="true" :inactive-value="false"
    active-text="是" inactive-text="否" />
</el-form-item>

<el-form-item v-if="form.canBuy" label="售价" prop="price">
  <el-input-number v-model="form.price" :min="0" :max="form.money" :precision="2"
    controls-position="right" />
  <span class="form-tip">用户支付此金额购买面值 {{ form.money }} 元的代金券</span>
</el-form-item>
```

**表格列表扩展**：
```html
<el-table-column label="售价" min-width="100">
  <template slot-scope="{ row }">
    <span>{{ row.canBuy ? '¥' + row.price : '-' }}</span>
  </template>
</el-table-column>
```

**后端模型扩展**（`StoreCoupon.java`）：
```java
/** 是否可购买 */
private Boolean canBuy;

/** 售价（用户购买价格） */
private BigDecimal price;
```

**默认值**：
```javascript
getDefaultForm() {
  return {
    // ... 现有字段
    canBuy: false,
    price: 0,
  }
}
```

**校验规则**：
- 售价必须 ≤ 面值
- 售价必须 ≥ 0

---

## 3. 涉及文件清单

### 前端文件

| 文件 | 操作 |
|-----|------|
| `admin/src/views/marketing/promotion/fullReduction/components/edit.vue` | 修改 |
| `admin/src/views/marketing/promotion/buyGift/index.vue` | 修改 |
| `admin/src/views/marketing/voucher/index.vue` | 修改 |
| `admin/src/components/userList/index.vue` | 修改（支持多选） |
| `admin/src/components/userList/userListFrom/index.js` | 新建 |
| `admin/src/components/userList/userListFrom/index.vue` | 新建 |
| `admin/src/main.js` | 修改（注册 userListFrom 插件） |

### 后端文件

| 文件 | 操作 |
|-----|------|
| `crmeb/crmeb-common/src/main/java/com/zbkj/common/model/coupon/StoreCoupon.java` | 修改 |
| `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/StoreCouponRequest.java` | 修改 |
| `crmeb/crmeb-common/src/main/resources/sql/promotion_tables.sql` | 修改（新增字段） |

---

## 4. 风险与注意事项

1. **数据兼容性**：新增的 `canBuy` 和 `price` 字段需要设置默认值，确保旧数据正常展示
2. **用户选择组件**：首次实现多选模式，需要充分测试分页场景下的选中状态保持
3. **商品规格获取**：买赠活动选择商品后需要额外调用详情 API，注意加载状态提示

---

## 5. 验收标准

- [ ] 满减活动：品类选择使用级联选择器，商品选择使用弹框多选
- [ ] 买赠活动：商品配置使用可视化表格，无需手动输入 JSON
- [ ] 代金券搜索栏：与列表卡片有明显间距
- [ ] 代金券发放：支持弹框搜索和多选用户
- [ ] 代金券购买：支持设置售价，用户可在前端商城购买
