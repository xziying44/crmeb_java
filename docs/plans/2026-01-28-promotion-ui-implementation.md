# 促销模块 UI/UX 优化实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 优化满减活动、买赠活动、代金券三个模块的 UI/UX，将 ID 手动输入改为可视化选择器，并为代金券添加购买功能。

**Architecture:** 复用现有 `$modalGoodList` 商品选择弹框和 `userList` 用户列表组件，扩展支持多选模式。后端仅需扩展 `StoreCoupon` 模型增加售价字段。

**Tech Stack:** Vue 2.x + Element UI 2.15 / Spring Boot 2.2.6 + MyBatis-Plus

---

## Task 1: 代金券布局修复（热身任务）

**Files:**
- Modify: `admin/src/views/marketing/voucher/index.vue:3`

**Step 1: 修改搜索卡片样式类**

在第 3 行的 `el-card` 添加 `mb14` 类名以增加底部间距：

```vue
<!-- 原代码 -->
<el-card :bordered="false" shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">

<!-- 改为 -->
<el-card :bordered="false" shadow="never" class="ivu-mt mb14" :body-style="{ padding: 0 }">
```

**Step 2: 验证修改**

Run: `cd admin && npm run dev`

打开浏览器访问 `http://localhost:9527`，导航至 营销管理 > 代金券，确认搜索栏与表格之间有明显间距。

**Step 3: 手动记录验证结果**

确认视觉效果正常后继续下一任务。

---

## Task 2: 用户选择组件 - 扩展多选功能

**Files:**
- Modify: `admin/src/components/userList/index.vue`

**Step 1: 添加 props 支持多选模式**

在 `props` 对象中添加 `handleNum` 属性（参考 `goodList/index.vue` 的实现）：

```javascript
// 在 export default 的 props 中添加（约第 64 行后）
props: {
  handleNum: {
    type: String,
    default: '',
  },
},
```

**Step 2: 添加多选所需的 data 属性**

在 `data()` 返回对象中添加：

```javascript
data() {
  return {
    templateRadio: 0,
    loading: false,
    tableData: {
      data: [],
      total: 0,
    },
    tableFrom: {
      page: 1,
      limit: 10,
      keywords: '',
    },
    // 新增：多选相关
    checkedIds: [],
    checkBox: [],
    checkedPage: [],
    isChecked: false,
  };
},
```

**Step 3: 修改表格列，支持单选/多选切换**

将原有的 radio 列改为条件渲染（修改第 13-21 行的 `el-table-column`）：

```vue
<!-- 多选模式 -->
<el-table-column key="multi" v-if="handleNum === 'many'" width="55">
  <template slot="header">
    <el-checkbox
      :value="isChecked && checkedPage.indexOf(tableFrom.page) > -1"
      @change="changeType"
    />
  </template>
  <template slot-scope="scope">
    <el-checkbox
      :value="checkedIds.indexOf(scope.row.uid) > -1"
      @change="(v) => changeOne(v, scope.row)"
    />
  </template>
</el-table-column>

<!-- 单选模式 -->
<el-table-column key="single" v-else label="" width="40">
  <template slot-scope="scope">
    <el-radio
      v-model="templateRadio"
      :label="scope.row.uid"
      @change.native="getTemplateRow(scope.$index, scope.row)"
    >&nbsp;</el-radio>
  </template>
</el-table-column>
```

**Step 4: 添加多选相关方法**

在 `methods` 对象中添加（参考 `goodList/index.vue` 的实现）：

```javascript
// 全选/取消全选
changeType(v) {
  this.isChecked = v;
  const index = this.checkedPage.indexOf(this.tableFrom.page);
  if (v) {
    if (index === -1) this.checkedPage.push(this.tableFrom.page);
  } else {
    if (index > -1) this.checkedPage.splice(index, 1);
  }
  this.syncCheckedId(v);
},

// 单个选中/取消
changeOne(v, user) {
  if (v) {
    const index = this.checkedIds.indexOf(user.uid);
    if (index === -1) {
      this.checkedIds.push(user.uid);
      this.checkBox.push(user);
    }
  } else {
    const index = this.checkedIds.indexOf(user.uid);
    if (index > -1) {
      this.checkedIds.splice(index, 1);
      this.checkBox.splice(index, 1);
    }
  }
},

// 同步当前页选中状态
syncCheckedId(checked) {
  this.tableData.data.forEach((item) => {
    const index = this.checkedIds.indexOf(item.uid);
    if (checked) {
      if (index === -1) {
        this.checkedIds.push(item.uid);
        this.checkBox.push(item);
      }
    } else {
      if (index > -1) {
        this.checkedIds.splice(index, 1);
        this.checkBox.splice(index, 1);
      }
    }
  });
},

// 多选确认
ok() {
  this.$emit('getUsers', this.checkBox);
},
```

**Step 5: 修改底部按钮区域**

修改底部按钮区域（约第 43-57 行），根据模式显示不同按钮：

```vue
<div class="acea-row row-between">
  <el-pagination
    :page-sizes="[10, 20, 30, 40]"
    :page-size="tableFrom.limit"
    :current-page="tableFrom.page"
    layout=" sizes, prev, pager, next, jumper"
    :total="tableData.total"
    @size-change="handleSizeChange"
    @current-change="pageChange"
  />
  <div class="mt30">
    <el-button @click="closeDialog">取消</el-button>
    <el-button v-if="handleNum === 'many'" type="primary" @click="ok">确定({{ checkedIds.length }})</el-button>
    <el-button v-else type="primary" @click="closeDialog">确定</el-button>
  </div>
</div>
```

**Step 6: 验证组件修改**

暂时无法独立测试，将在 Task 3 完成后一并验证。

---

## Task 3: 用户选择弹框 - 创建全局方法

**Files:**
- Create: `admin/src/components/userList/userListFrom/index.js`
- Create: `admin/src/components/userList/userListFrom/index.vue`
- Modify: `admin/src/main.js:41` (添加 import)
- Modify: `admin/src/main.js:89` (添加 Vue.use)

**Step 1: 创建弹框组件 `index.vue`**

创建文件 `admin/src/components/userList/userListFrom/index.vue`：

```vue
<template>
  <div>
    <el-dialog title="选择用户" :visible.sync="visible" width="900px" :before-close="handleClose">
      <user-list
        v-if="visible"
        :handleNum="handleNum"
        @getTemplateRow="getTemplateRow"
        @getUsers="getUsers"
        @closeDialog="handleClose"
      />
    </el-dialog>
  </div>
</template>

<script>
import userList from '@/components/userList/index.vue';

export default {
  name: 'UserListFrom',
  components: { userList },
  data() {
    return {
      handleNum: '',
      visible: false,
      callback: function () {},
    };
  },
  methods: {
    handleClose() {
      this.visible = false;
    },
    // 单选回调
    getTemplateRow(row) {
      this.callback([row]);
      this.visible = false;
    },
    // 多选回调
    getUsers(users) {
      this.callback(users);
      this.visible = false;
    },
  },
};
</script>

<style scoped>
::v-deep .el-dialog__body {
  padding: 20px 24px 0 24px !important;
}
</style>
```

**Step 2: 创建插件注册文件 `index.js`**

创建文件 `admin/src/components/userList/userListFrom/index.js`：

```javascript
import uploadFromComponent from './index.vue';

const userListFrom = {};

userListFrom.install = function (Vue) {
  const ToastConstructor = Vue.extend(uploadFromComponent);
  const instance = new ToastConstructor();
  instance.$mount(document.createElement('div'));
  document.body.appendChild(instance.$el);

  Vue.prototype.$modalUserList = function (callback, handleNum) {
    instance.visible = true;
    instance.callback = callback;
    instance.handleNum = handleNum || '';
  };
};

export default userListFrom;
```

**Step 3: 在 main.js 中注册插件**

修改 `admin/src/main.js`，在第 41 行附近（goodListFrom 下方）添加 import：

```javascript
import goodListFrom from './components/goodList/goodListFrom';
import userListFrom from './components/userList/userListFrom';  // 新增
import couponFrom from './components/couponList/couponFrom';
```

在第 89 行附近（`Vue.use(goodListFrom)` 下方）添加：

```javascript
Vue.use(goodListFrom);
Vue.use(userListFrom);  // 新增
Vue.use(couponFrom);
```

**Step 4: 验证用户选择弹框**

Run: `cd admin && npm run dev`

在浏览器控制台执行测试：
```javascript
this.$modalUserList((users) => { console.log('选中用户:', users) }, 'many')
```

Expected: 弹出用户选择对话框，支持多选，点击确定后控制台打印选中的用户数组。

---

## Task 4: 代金券发放 - 使用用户选择弹框

**Files:**
- Modify: `admin/src/views/marketing/voucher/index.vue:205-218`

**Step 1: 修改 handleSend 方法**

将原有的 `$prompt` 方式改为 `$modalUserList`（修改第 205-218 行）：

```javascript
// 原代码
handleSend(row) {
  this.$prompt('请输入用户ID（多个用英文逗号分隔）', '发放代金券', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /\d+(,\d+)*/,
    inputErrorMessage: '用户ID格式不正确',
  })
    .then(({ value }) => {
      return voucherSendApi(row.id, value);
    })
    .then(() => {
      this.$message.success('发放成功');
    })
    .catch(() => {});
},

// 改为
handleSend(row) {
  this.$modalUserList((users) => {
    if (!users || users.length === 0) {
      return this.$message.warning('请选择用户');
    }
    const uids = users.map((u) => u.uid).join(',');
    voucherSendApi(row.id, uids)
      .then(() => {
        this.$message.success('发放成功');
      })
      .catch(() => {});
  }, 'many');
},
```

**Step 2: 验证发放功能**

Run: `cd admin && npm run dev`

1. 导航至 营销管理 > 代金券
2. 点击任意代金券的"发放"按钮
3. Expected: 弹出用户选择对话框（支持搜索和多选）
4. 选择用户后点击确定，Expected: 显示"发放成功"提示

---

## Task 5: 代金券购买功能 - 后端模型扩展

**Files:**
- Modify: `crmeb/crmeb-common/src/main/java/com/zbkj/common/model/coupon/StoreCoupon.java:107` (在 isDel 字段后添加)
- Modify: `crmeb/crmeb-common/src/main/java/com/zbkj/common/request/StoreCouponRequest.java:108` (在 status 字段后添加)

**Step 1: 扩展 StoreCoupon 模型**

在 `StoreCoupon.java` 的 `isDel` 字段后（约第 100 行后）添加新字段：

```java
@ApiModelProperty(value = "是否删除 状态（0：否，1：是）")
private Boolean isDel;

// ===== 新增字段 =====
@ApiModelProperty(value = "是否可购买（0：否，1：是）")
private Boolean canBuy;

@ApiModelProperty(value = "售价（用户购买价格）")
private BigDecimal price;
// ===== 新增结束 =====

@ApiModelProperty(value = "创建时间")
private Date createTime;
```

**Step 2: 扩展 StoreCouponRequest 请求对象**

在 `StoreCouponRequest.java` 的 `status` 字段后（约第 107 行后）添加：

```java
@ApiModelProperty(value = "状态（0：关闭，1：开启）")
@NotNull(message = "优惠券状态不能为空")
private Boolean status;

// ===== 新增字段 =====
@ApiModelProperty(value = "是否可购买（0：否，1：是）")
private Boolean canBuy = false;

@ApiModelProperty(value = "售价（用户购买价格）")
@DecimalMin(value = "0", message = "售价不能小于0")
@DecimalMax(value = "99999.99", message = "售价不能大于99999.99")
private BigDecimal price;
// ===== 新增结束 =====
```

**Step 3: 添加数据库字段**

在 `crmeb/crmeb-common/src/main/resources/sql/promotion_tables.sql` 末尾添加：

```sql
-- 代金券购买功能扩展
ALTER TABLE `eb_store_coupon` ADD COLUMN `can_buy` TINYINT(1) DEFAULT 0 COMMENT '是否可购买（0：否，1：是）' AFTER `is_del`;
ALTER TABLE `eb_store_coupon` ADD COLUMN `price` DECIMAL(10,2) DEFAULT 0.00 COMMENT '售价（用户购买价格）' AFTER `can_buy`;
```

**Step 4: 验证后端编译**

Run: `cd crmeb && mvn compile -DskipTests`

Expected: BUILD SUCCESS

---

## Task 6: 代金券购买功能 - 前端表单扩展

**Files:**
- Modify: `admin/src/views/marketing/voucher/index.vue`

**Step 1: 扩展 getDefaultForm 方法**

在 `getDefaultForm()` 返回对象中添加新字段（约第 124-144 行）：

```javascript
getDefaultForm() {
  return {
    id: null,
    name: '',
    money: 0,
    canDeductFreight: false,
    day: 30,
    sort: 0,
    status: true,
    // ===== 新增字段 =====
    canBuy: false,
    price: 0,
    // ===== 新增结束 =====
    // 以下为后端创建所需的默认字段
    couponType: 2,
    useType: 1,
    primaryKey: '',
    minPrice: 0,
    isLimited: false,
    total: 0,
    isForever: false,
    isFixedTime: false,
    type: 1,
  };
},
```

**Step 2: 在表单中添加购买相关字段**

在弹窗表单中，`是否开启` 字段后（约第 78-80 行后）添加：

```vue
<el-form-item label="是否开启" prop="status">
  <el-switch v-model="form.status" :active-value="true" :inactive-value="false" active-text="开启" inactive-text="关闭" />
</el-form-item>

<!-- ===== 新增：购买功能 ===== -->
<el-form-item label="是否可购买">
  <el-switch v-model="form.canBuy" :active-value="true" :inactive-value="false" active-text="是" inactive-text="否" />
</el-form-item>
<el-form-item v-if="form.canBuy" label="售价" prop="price">
  <el-input-number v-model="form.price" :min="0" :max="form.money" :precision="2" controls-position="right" />
  <span style="margin-left: 10px; color: #909399;">用户支付此金额购买面值 {{ form.money }} 元的代金券</span>
</el-form-item>
<!-- ===== 新增结束 ===== -->
```

**Step 3: 在表格中添加售价列**

在表格的 `可抵扣运费` 列后（约第 27-31 行后）添加：

```vue
<el-table-column label="可抵扣运费" min-width="120">
  <template slot-scope="{ row }">
    <span>{{ row.canDeductFreight ? '是' : '否' }}</span>
  </template>
</el-table-column>

<!-- ===== 新增：售价列 ===== -->
<el-table-column label="售价" min-width="100">
  <template slot-scope="{ row }">
    <span>{{ row.canBuy ? '¥' + row.price : '-' }}</span>
  </template>
</el-table-column>
<!-- ===== 新增结束 ===== -->
```

**Step 4: 添加售价校验规则**

在 `rules` 对象中添加（约第 111-117 行）：

```javascript
rules: {
  name: [{ required: true, message: '请输入代金券名称', trigger: 'blur' }],
  money: [{ required: true, message: '请输入面值', trigger: 'change' }],
  day: [{ required: true, message: '请输入有效天数', trigger: 'change' }],
  sort: [{ required: true, message: '请输入排序', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  // ===== 新增 =====
  price: [
    {
      validator: (rule, value, callback) => {
        if (this.form.canBuy && value > this.form.money) {
          return callback(new Error('售价不能大于面值'));
        }
        return callback();
      },
      trigger: 'change',
    },
  ],
  // ===== 新增结束 =====
},
```

**Step 5: 验证代金券购买功能**

Run: `cd admin && npm run dev`

1. 导航至 营销管理 > 代金券
2. 点击"新增代金券"
3. 开启"是否可购买"开关
4. Expected: 显示"售价"输入框
5. 输入售价（应小于等于面值）
6. 保存后查看列表，Expected: 售价列显示设置的价格

---

## Task 7: 满减活动 - 品类/商品选择器

**Files:**
- Modify: `admin/src/views/marketing/promotion/fullReduction/components/edit.vue`

**Step 1: 添加 import 和 data 属性**

在 `<script>` 中添加 import（约第 84 行）：

```javascript
import { fullReductionDetailApi, fullReductionSaveApi } from '@/api/promotion';
import { categoryApi } from '@/api/store';  // 新增
```

在 `data()` 返回对象中添加（约第 99-116 行后）：

```javascript
data() {
  return {
    loading: false,
    form: this.getDefaultForm(),
    // ===== 新增 =====
    merCateList: [],  // 品类列表
    props: {
      children: 'child',
      label: 'name',
      value: 'id',
      multiple: true,
      emitPath: false,
    },
    // ===== 新增结束 =====
    rules: {
      // ...
    },
  };
},
```

**Step 2: 修改 getDefaultForm 方法**

修改 `getDefaultForm()` 方法（约第 126-137 行）：

```javascript
getDefaultForm() {
  return {
    id: null,
    name: '',
    scopeType: 1,
    startTime: '',
    endTime: '',
    allowCoupon: true,
    // ===== 修改：移除 relationIdsStr，添加新字段 =====
    selectedCategoryIds: [],  // 品类 ID 数组
    selectedProducts: [],      // 商品对象数组
    // ===== 修改结束 =====
    levels: [{ fullAmount: 0, reduceAmount: 0 }],
  };
},
```

**Step 3: 添加 mounted 钩子获取品类列表**

在 `watch` 同级添加 `mounted` 钩子：

```javascript
mounted() {
  this.getCategoryList();
},
methods: {
  // 获取品类列表
  getCategoryList() {
    categoryApi({ status: -1, type: 1 }).then((res) => {
      this.merCateList = res || [];
    });
  },
  // ... 其他方法
},
```

**Step 4: 替换模板中的关联 ID 输入框**

将原有的 textarea 输入框（约第 43-50 行）替换为：

```vue
<!-- 原代码删除 -->
<!-- <el-form-item v-if="form.scopeType !== 1" label="关联ID列表" prop="relationIdsStr">
  <el-input ... />
</el-form-item> -->

<!-- ===== 新增：品类选择器 ===== -->
<el-form-item v-if="form.scopeType === 2" label="选择品类" prop="selectedCategoryIds">
  <el-cascader
    v-model="form.selectedCategoryIds"
    :options="merCateList"
    :props="props"
    clearable
    style="width: 100%"
    placeholder="请选择品类"
  />
</el-form-item>

<!-- ===== 新增：商品选择器 ===== -->
<el-form-item v-if="form.scopeType === 3" label="选择商品" prop="selectedProducts">
  <div class="acea-row">
    <template v-if="form.selectedProducts.length">
      <div class="pictrue" v-for="(item, index) in form.selectedProducts" :key="item.id">
        <img :src="item.image" style="width: 60px; height: 60px; object-fit: cover;" />
        <i class="el-icon-error btndel" @click="removeProduct(index)" />
      </div>
    </template>
    <div class="upLoadPicBox" @click="selectProducts">
      <div class="upLoad">
        <i class="el-icon-plus" style="font-size: 24px; color: #c0c4cc;" />
      </div>
    </div>
  </div>
</el-form-item>
```

**Step 5: 添加商品选择相关方法**

在 `methods` 中添加：

```javascript
// 打开商品选择弹框
selectProducts() {
  const _this = this;
  this.$modalGoodList(function (products) {
    _this.form.selectedProducts = products;
  }, 'many', this.form.selectedProducts);
},

// 移除已选商品
removeProduct(index) {
  this.form.selectedProducts.splice(index, 1);
},
```

**Step 6: 修改 handleSubmit 方法**

修改提交逻辑，将选择的数据转换为 `relationIds`（约第 176-203 行）：

```javascript
handleSubmit() {
  this.$refs.formRef.validate((valid) => {
    if (!valid) return;
    if (!this.form.startTime || !this.form.endTime) {
      this.$message.error('请选择时间范围');
      return;
    }

    // ===== 新增：计算 relationIds =====
    let relationIds = [];
    if (this.form.scopeType === 2) {
      relationIds = this.form.selectedCategoryIds;
      if (!relationIds.length) {
        return this.$message.error('请选择品类');
      }
    } else if (this.form.scopeType === 3) {
      relationIds = this.form.selectedProducts.map((p) => p.id);
      if (!relationIds.length) {
        return this.$message.error('请选择商品');
      }
    }
    // ===== 新增结束 =====

    const payload = {
      id: this.form.id,
      name: this.form.name,
      scopeType: this.form.scopeType,
      startTime: this.form.startTime,
      endTime: this.form.endTime,
      allowCoupon: this.form.allowCoupon,
      relationIds: relationIds,  // 修改
      levels: this.form.levels,
    };
    this.loading = true;
    fullReductionSaveApi(payload)
      .then(() => {
        this.$message.success('保存成功');
        this.loading = false;
        this.$emit('success');
      })
      .catch(() => {
        this.loading = false;
      });
  });
},
```

**Step 7: 修改 init 方法，回显已选数据**

修改 `init()` 方法以正确回显数据（约第 138-159 行）：

```javascript
init() {
  this.loading = false;
  this.form = this.getDefaultForm();
  if (this.editId) {
    fullReductionDetailApi(this.editId).then((res) => {
      this.form.id = res.id;
      this.form.name = res.name;
      this.form.scopeType = res.scopeType;
      this.form.startTime = res.startTime;
      this.form.endTime = res.endTime;
      this.form.allowCoupon = !!res.allowCoupon;
      this.form.levels = (res.levels || []).map((l) => ({
        fullAmount: l.fullAmount,
        reduceAmount: l.reduceAmount,
      }));

      // ===== 新增：回显品类/商品 =====
      if (res.scopeType === 2) {
        this.form.selectedCategoryIds = res.relationIds || [];
      } else if (res.scopeType === 3) {
        // 商品需要包含图片等信息，从 res.products 获取（如后端返回）
        // 否则只能显示 ID
        this.form.selectedProducts = (res.products || []).map((p) => ({
          id: p.id || p.productId,
          image: p.image,
          storeName: p.storeName,
        }));
      }
      // ===== 新增结束 =====

      if (!this.form.levels.length) {
        this.form.levels = [{ fullAmount: 0, reduceAmount: 0 }];
      }
    });
  }
},
```

**Step 8: 添加样式**

在 `</script>` 后添加样式：

```vue
<style scoped>
.pictrue {
  position: relative;
  width: 60px;
  height: 60px;
  margin-right: 10px;
  margin-bottom: 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}
.pictrue .btndel {
  position: absolute;
  top: -4px;
  right: -4px;
  font-size: 18px;
  color: #f56c6c;
  cursor: pointer;
}
.upLoadPicBox {
  width: 60px;
  height: 60px;
  border: 1px dashed #c0c4cc;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.upLoadPicBox:hover {
  border-color: #409eff;
}
</style>
```

**Step 9: 验证满减活动选择器**

Run: `cd admin && npm run dev`

1. 导航至 营销管理 > 满减活动
2. 点击"添加"按钮
3. 选择活动范围为"品类"，Expected: 显示品类级联选择器
4. 选择活动范围为"指定商品"，Expected: 显示商品选择区域，点击可弹出商品选择框

---

## Task 8: 买赠活动 - 可视化商品配置

**Files:**
- Modify: `admin/src/views/marketing/promotion/buyGift/index.vue`

**Step 1: 添加 import**

在 `<script>` 中添加 import（约第 155 行）：

```javascript
import { checkPermi } from '@/utils/permission';
import {
  buyGiftListApi,
  buyGiftDeleteApi,
  buyGiftUpdateStatusApi,
  buyGiftDetailApi,
  buyGiftSaveApi,
} from '@/api/promotion';
import { productDetailApi } from '@/api/store';  // 新增
```

**Step 2: 修改 getDefaultForm 方法**

修改 `getDefaultForm()` 方法（约第 205-217 行）：

```javascript
getDefaultForm() {
  return {
    id: null,
    name: '',
    giftType: 1,
    startTime: '',
    endTime: '',
    limitType: 0,
    limitNum: 0,
    // ===== 修改：移除 JSON 字段，使用数组 =====
    buyProducts: [],
    giftProducts: [],
    // ===== 修改结束 =====
  };
},
```

**Step 3: 替换模板中的 JSON 输入框**

将原有的 textarea 输入框（约第 127-143 行）替换为：

```vue
<!-- 删除原有的 JSON 输入框 -->

<!-- ===== 新增：购买商品配置表格 ===== -->
<el-form-item label="购买商品" required>
  <el-table :data="form.buyProducts" size="mini" border style="margin-bottom: 10px;">
    <el-table-column label="商品图" width="80">
      <template slot-scope="{ row }">
        <el-image :src="row.image" style="width: 50px; height: 50px; object-fit: cover;" />
      </template>
    </el-table-column>
    <el-table-column prop="storeName" label="商品名称" min-width="150" show-overflow-tooltip />
    <el-table-column label="规格" min-width="150">
      <template slot-scope="{ row }">
        <el-select v-model="row.attrValueId" placeholder="请选择规格" size="small" style="width: 100%;">
          <el-option
            v-for="attr in row.attrValues"
            :key="attr.id"
            :label="attr.sku || '默认'"
            :value="attr.id"
          />
        </el-select>
      </template>
    </el-table-column>
    <el-table-column label="购买数量" width="120">
      <template slot-scope="{ row }">
        <el-input-number v-model="row.buyNum" :min="1" size="small" controls-position="right" />
      </template>
    </el-table-column>
    <el-table-column label="操作" width="80">
      <template slot-scope="{ $index }">
        <el-button type="text" size="small" @click="removeBuyProduct($index)">删除</el-button>
      </template>
    </el-table-column>
  </el-table>
  <el-button size="small" icon="el-icon-plus" @click="addBuyProduct">添加购买商品</el-button>
</el-form-item>

<!-- ===== 新增：赠品配置表格 ===== -->
<el-form-item label="赠品配置" required>
  <el-table :data="form.giftProducts" size="mini" border style="margin-bottom: 10px;">
    <el-table-column label="商品图" width="80">
      <template slot-scope="{ row }">
        <el-image :src="row.image" style="width: 50px; height: 50px; object-fit: cover;" />
      </template>
    </el-table-column>
    <el-table-column prop="storeName" label="商品名称" min-width="150" show-overflow-tooltip />
    <el-table-column label="规格" min-width="150">
      <template slot-scope="{ row }">
        <el-select v-model="row.attrValueId" placeholder="请选择规格" size="small" style="width: 100%;">
          <el-option
            v-for="attr in row.attrValues"
            :key="attr.id"
            :label="attr.sku || '默认'"
            :value="attr.id"
          />
        </el-select>
      </template>
    </el-table-column>
    <el-table-column label="赠送数量" width="120">
      <template slot-scope="{ row }">
        <el-input-number v-model="row.giftNum" :min="1" size="small" controls-position="right" />
      </template>
    </el-table-column>
    <el-table-column label="操作" width="80">
      <template slot-scope="{ $index }">
        <el-button type="text" size="small" @click="removeGiftProduct($index)">删除</el-button>
      </template>
    </el-table-column>
  </el-table>
  <el-button size="small" icon="el-icon-plus" @click="addGiftProduct">添加赠品</el-button>
</el-form-item>
```

**Step 4: 添加商品操作方法**

在 `methods` 中添加（删除原有的 `parseJsonArray` 方法）：

```javascript
// 添加购买商品
addBuyProduct() {
  const _this = this;
  this.$modalGoodList(function (product) {
    _this.fetchProductAttr(product, 'buy');
  });
},

// 添加赠品
addGiftProduct() {
  const _this = this;
  this.$modalGoodList(function (product) {
    _this.fetchProductAttr(product, 'gift');
  });
},

// 获取商品规格信息
fetchProductAttr(product, type) {
  productDetailApi(product.id).then((res) => {
    const attrValues = (res.attrValue || []).map((attr) => ({
      id: attr.id,
      sku: attr.sku,
    }));
    const newProduct = {
      productId: product.id,
      storeName: product.storeName,
      image: product.image,
      attrValueId: attrValues.length ? attrValues[0].id : 0,
      attrValues: attrValues,
      buyNum: 1,
      giftNum: 1,
    };
    if (type === 'buy') {
      this.form.buyProducts.push(newProduct);
    } else {
      this.form.giftProducts.push(newProduct);
    }
  });
},

// 移除购买商品
removeBuyProduct(index) {
  this.form.buyProducts.splice(index, 1);
},

// 移除赠品
removeGiftProduct(index) {
  this.form.giftProducts.splice(index, 1);
},
```

**Step 5: 修改 handleSubmit 方法**

修改提交逻辑（约第 296-342 行）：

```javascript
handleSubmit() {
  this.$refs.formRef.validate((valid) => {
    if (!valid) return;
    if (!this.form.startTime || !this.form.endTime) {
      this.$message.error('请选择时间范围');
      return;
    }

    // ===== 修改：直接使用数组 =====
    if (!this.form.buyProducts.length) {
      this.$message.error('请配置购买商品');
      return;
    }
    if (!this.form.giftProducts.length) {
      this.$message.error('请配置赠品');
      return;
    }

    const buyProducts = this.form.buyProducts.map((p) => ({
      productId: p.productId,
      attrValueId: p.attrValueId,
      buyNum: p.buyNum,
    }));
    const giftProducts = this.form.giftProducts.map((p) => ({
      productId: p.productId,
      attrValueId: p.attrValueId,
      giftNum: p.giftNum,
    }));
    // ===== 修改结束 =====

    const payload = {
      id: this.form.id,
      name: this.form.name,
      giftType: this.form.giftType,
      startTime: this.form.startTime,
      endTime: this.form.endTime,
      limitType: this.form.limitType,
      limitNum: this.form.limitType === 0 ? 0 : this.form.limitNum,
      buyProducts,
      giftProducts,
    };
    this.saving = true;
    buyGiftSaveApi(payload)
      .then(() => {
        this.$message.success('保存成功');
        this.saving = false;
        this.editVisible = false;
        this.getList();
      })
      .catch(() => {
        this.saving = false;
      });
  });
},
```

**Step 6: 修改 handleEdit 方法**

修改编辑回显逻辑（约第 267-282 行）：

```javascript
handleEdit(row) {
  this.editId = row.id;
  this.form = this.getDefaultForm();
  this.editVisible = true;
  buyGiftDetailApi(row.id).then((res) => {
    this.form.id = res.id;
    this.form.name = res.name;
    this.form.giftType = res.giftType;
    this.form.startTime = res.startTime;
    this.form.endTime = res.endTime;
    this.form.limitType = res.limitType;
    this.form.limitNum = res.limitNum;

    // ===== 修改：回显商品数据 =====
    this.form.buyProducts = (res.buyProducts || []).map((p) => ({
      productId: p.productId,
      storeName: p.storeName || '',
      image: p.image || '',
      attrValueId: p.attrValueId,
      attrValues: p.attrValues || [{ id: p.attrValueId, sku: '默认' }],
      buyNum: p.buyNum,
      giftNum: 1,
    }));
    this.form.giftProducts = (res.giftProducts || []).map((p) => ({
      productId: p.productId,
      storeName: p.storeName || '',
      image: p.image || '',
      attrValueId: p.attrValueId,
      attrValues: p.attrValues || [{ id: p.attrValueId, sku: '默认' }],
      buyNum: 1,
      giftNum: p.giftNum,
    }));
    // ===== 修改结束 =====
  });
},
```

**Step 7: 验证买赠活动配置**

Run: `cd admin && npm run dev`

1. 导航至 营销管理 > 买赠活动
2. 点击"添加买赠活动"
3. Expected: 显示可视化的商品配置表格
4. 点击"添加购买商品"，Expected: 弹出商品选择框
5. 选择商品后，Expected: 表格显示商品图片、名称、规格下拉框、数量输入

---

## Task 9: 最终验证与清理

**Step 1: 编译后端**

Run: `cd crmeb && mvn clean compile -DskipTests`

Expected: BUILD SUCCESS

**Step 2: 启动后端服务**

Run: `cd crmeb/crmeb-admin && mvn spring-boot:run`

Expected: 服务启动成功，监听 8080 端口

**Step 3: 启动前端服务**

Run: `cd admin && npm run dev`

Expected: 服务启动成功

**Step 4: 全面功能验证**

检查清单：
- [ ] 代金券搜索栏与表格有间距
- [ ] 代金券发放弹出用户选择框（支持多选）
- [ ] 代金券支持设置售价
- [ ] 满减活动品类选择使用级联选择器
- [ ] 满减活动商品选择使用弹框多选
- [ ] 买赠活动使用可视化表格配置

**Step 5: 输出 Commit 信息**

验证通过后，输出以下提交信息供手动提交：

```
优化促销模块 UI/UX 交互体验

- 代金券：修复搜索栏布局间距，发放改用用户选择弹框
- 代金券：新增购买功能，支持设置售价
- 满减活动：关联 ID 输入改为品类级联选择器/商品弹框多选
- 买赠活动：JSON 配置改为可视化表格，支持商品规格选择
- 新增 $modalUserList 全局方法，支持用户多选
```

---

## 文件修改汇总

| 序号 | 文件路径 | 操作 | 任务 |
|------|---------|------|------|
| 1 | `admin/src/views/marketing/voucher/index.vue` | 修改 | T1, T4, T6 |
| 2 | `admin/src/components/userList/index.vue` | 修改 | T2 |
| 3 | `admin/src/components/userList/userListFrom/index.js` | 新建 | T3 |
| 4 | `admin/src/components/userList/userListFrom/index.vue` | 新建 | T3 |
| 5 | `admin/src/main.js` | 修改 | T3 |
| 6 | `crmeb/.../model/coupon/StoreCoupon.java` | 修改 | T5 |
| 7 | `crmeb/.../request/StoreCouponRequest.java` | 修改 | T5 |
| 8 | `crmeb/.../sql/promotion_tables.sql` | 修改 | T5 |
| 9 | `admin/src/views/marketing/promotion/fullReduction/components/edit.vue` | 修改 | T7 |
| 10 | `admin/src/views/marketing/promotion/buyGift/index.vue` | 修改 | T8 |
