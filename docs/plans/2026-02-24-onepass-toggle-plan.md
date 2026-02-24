# 一号通可选开关 实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将一号通作为可选开关，默认关闭，消除未配置时的页面报错。

**Architecture:** 在 `eb_system_config` 表新增 `one_pass_status` 配置键控制开关。后端在 `OnePassUtil.getLoginVo()` 入口统一拦截所有一号通请求。前端在配置页面添加 Switch 开关，在订单发货等页面先查状态再决定是否请求。

**Tech Stack:** Java (Spring Boot + MyBatis-Plus) / Vue 2 + Element UI

**设计文档:** `docs/plans/2026-02-24-onepass-toggle-design.md`

---

## 调用链路分析（实施参考）

所有一号通 API 调用最终都经过 `OnePassUtil.getLoginVo()` 或 `OnePassUtil.getToken()`（内部也调用 `getLoginVo()`），因此在 `getLoginVo()` 中加入开关检查即可拦截所有后端调用。

**后端调用方（无需修改，自动被拦截）：**
- `SmsServiceImpl.java` — 短信发送、模板管理
- `ExpressServiceImpl.java` — 快递公司列表、电子面单模板
- `StoreProductServiceImpl.java` — 产品复制
- `StoreOrderServiceImpl.java` — 商家寄件、电子面单发货
- `LogisticsServiceImpl.java` — 物流追踪

**前端调用方（需要加状态检查）：**
- `orderSend.vue:320` — mounted 自动调用 `getShipmentExpress()`
- `smsPay/index.vue:158` — 调用 `smsInfoApi()` 获取一号通用户信息

---

### Task 1: 添加一号通开关常量

**Files:**
- Modify: `crmeb/crmeb-common/src/main/java/com/zbkj/common/constants/OnePassConstants.java:130-132`

**Step 1: 在 OnePassConstants 中添加开关常量**

在 `ONE_PASS_ACCESS_KEY` 上方添加：

```java
    // 一号通功能开关（"0"=关闭，"1"=开启，默认关闭）
    public static final String ONE_PASS_STATUS = "one_pass_status";

```

**Step 2: 验证编译**

Run: `cd /Users/xziying/project/bespoke/crmeb_java/crmeb && mvn compile -pl crmeb-common -q`
Expected: BUILD SUCCESS

**Step 3: 提交**

```
feat: 添加一号通功能开关常量 ONE_PASS_STATUS
```

---

### Task 2: OnePassUtil 添加开关检查

**Files:**
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/util/OnePassUtil.java:49-68`

**Step 1: 添加 isEnabled() 方法**

在 `OnePassUtil` 类中 `getLoginVo()` 方法之前添加：

```java
    /**
     * 检查一号通功能是否启用
     *
     * @return true=启用，false=关闭
     */
    public boolean isEnabled() {
        String status = systemConfigService.getValueByKey(OnePassConstants.ONE_PASS_STATUS);
        return "1".equals(status);
    }

```

**Step 2: 在 getLoginVo() 开头加入开关检查**

修改 `getLoginVo()` 方法，在方法体第一行加入：

```java
    public OnePassLoginVo getLoginVo() {
        if (!isEnabled()) {
            throw new CrmebException("一号通功能未启用，请在设置中开启");
        }
        String accessKey = systemConfigService.getValueByKey(OnePassConstants.ONE_PASS_ACCESS_KEY);// 获取配置账号
        // ... 后续逻辑不变
```

**Step 3: 验证编译**

Run: `cd /Users/xziying/project/bespoke/crmeb_java/crmeb && mvn compile -pl crmeb-service -q`
Expected: BUILD SUCCESS

**Step 4: 提交**

```
feat: OnePassUtil 添加 isEnabled() 开关检查，getLoginVo() 加前置拦截
```

---

### Task 3: OnePassService 接口和实现添加 isEnabled 方法

**Files:**
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/OnePassService.java:104-106`
- Modify: `crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/OnePassServiceImpl.java:344-356`

**Step 1: 在 OnePassService 接口中添加 isEnabled() 方法签名**

在 `checkAccount()` 方法声明之后添加：

```java
    /**
     * 检查一号通功能是否启用
     *
     * @return true=启用，false=关闭
     */
    Boolean isEnabled();
```

**Step 2: 在 OnePassServiceImpl 中实现 isEnabled()**

在 `checkAccount()` 方法之后添加：

```java
    /**
     * 检查一号通功能是否启用
     */
    @Override
    public Boolean isEnabled() {
        return onePassUtil.isEnabled();
    }
```

**Step 3: 验证编译**

Run: `cd /Users/xziying/project/bespoke/crmeb_java/crmeb && mvn compile -pl crmeb-service -q`
Expected: BUILD SUCCESS

**Step 4: 提交**

```
feat: OnePassService 添加 isEnabled() 接口方法
```

---

### Task 4: OnePassController 添加开关状态查询 API

**Files:**
- Modify: `crmeb/crmeb-admin/src/main/java/com/zbkj/admin/controller/OnePassController.java:48-53`

**Step 1: 在 getOnePassApplication() 方法之后添加状态查询端点**

在 `getOnePassApplication()` 方法之后添加：

```java
    @ApiOperation(value = "一号通 开关状态查询")
    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public CommonResult<Boolean> getOnePassStatus() {
        return CommonResult.success(onePassService.isEnabled());
    }

```

注意：此端点不加 `@PreAuthorize`，所有已登录管理员均可查询开关状态（因为各页面需要用此状态判断是否显示一号通相关功能）。

**Step 2: 验证编译**

Run: `cd /Users/xziying/project/bespoke/crmeb_java/crmeb && mvn compile -pl crmeb-admin -q`
Expected: BUILD SUCCESS

**Step 3: 提交**

```
feat: 添加一号通开关状态查询 API /api/admin/pass/status
```

---

### Task 5: 前端添加一号通状态查询 API

**Files:**
- Modify: `admin/src/api/systemConfig.js:215-231`

**Step 1: 在 passAppSaveApi 之前添加 onePassStatusApi**

在 `passAppSaveApi` 函数之前添加：

```javascript
/**
 * @description 一号通 开关状态查询
 */
export function onePassStatusApi() {
  return request({
    url: '/admin/pass/status',
    method: 'get',
  });
}

/**
 * @description 一号通 开关状态保存
 */
export function onePassStatusSaveApi(data) {
  return request({
    url: '/admin/system/config/saveuniq',
    method: 'POST',
    params: { key: 'one_pass_status', value: data },
  });
}

```

**Step 2: 提交**

```
feat: 前端添加一号通开关状态查询和保存 API
```

---

### Task 6: 前端一号通配置页面添加 Switch 开关

**Files:**
- Modify: `admin/src/views/sms/smsConfig/config.vue`

**Step 1: 重写配置页面，在表单上方添加开关**

完整替换 `config.vue` 内容为：

```vue
<template>
  <div class="divBox">
    <el-card class="box-card">
      <!-- 一号通开关 -->
      <div class="onepass-switch-wrapper">
        <div class="onepass-switch-row">
          <span class="onepass-switch-label">启用一号通服务</span>
          <el-switch
            v-model="onePassEnabled"
            active-text="开启"
            inactive-text="关闭"
            @change="handleSwitchChange"
          />
        </div>
        <div class="onepass-switch-tip">关闭后，短信、物流查询、电子面单、商家寄件、产品复制等一号通功能将不可用</div>
      </div>
      <el-divider />
      <!-- 原有配置表单 -->
      <div :class="{ 'onepass-disabled': !onePassEnabled }">
        <zb-parser
          :form-id="formId"
          :is-create="isCreate"
          :edit-data="editData"
          @submit="handlerSubmit"
          @resetForm="resetForm"
          v-if="isShow && checkPermi(['admin:pass:appget'])"
        />
      </div>
    </el-card>
  </div>
</template>

<script>
import zbParser from '@/components/FormGenerator/components/parser/ZBParser';
import { passAppSaveApi, passAppInfoApi, onePassStatusApi, onePassStatusSaveApi } from '@/api/systemConfig.js';
import { checkPermi } from '@/utils/permission';
export default {
  name: 'onePassConfig',
  components: { zbParser },
  data() {
    return {
      isShow: true,
      isCreate: 0,
      editData: {},
      formId: 144,
      onePassEnabled: false,
    };
  },
  mounted() {
    this.getOnePassStatus();
    if (checkPermi(['admin:pass:appget'])) this.getPassAppInfo();
  },
  methods: {
    checkPermi,
    // 获取一号通开关状态
    getOnePassStatus() {
      onePassStatusApi()
        .then((res) => {
          this.onePassEnabled = res.data === true;
        })
        .catch(() => {
          this.onePassEnabled = false;
        });
    },
    // 切换开关
    handleSwitchChange(val) {
      onePassStatusSaveApi(val ? '1' : '0')
        .then(() => {
          this.$message.success(val ? '一号通已启用' : '一号通已关闭');
        })
        .catch(() => {
          // 保存失败，回滚开关状态
          this.onePassEnabled = !val;
          this.$message.error('保存失败，请重试');
        });
    },
    resetForm(formValue) {
      this.isShow = false;
    },
    handlerSubmit(data) {
      passAppSaveApi(data).then((res) => {
        this.getPassAppInfo();
        this.$message.success('操作成功');
      });
    },
    // 获取配置详情
    getPassAppInfo() {
      passAppInfoApi().then((res) => {
        this.isShow = false;
        this.editData = res;
        this.isCreate = 1;
        setTimeout(() => {
          this.isShow = true;
        }, 80);
      });
    },
  },
};
</script>

<style scoped lang="scss">
::v-deep .closeBtn {
  display: none;
}
::v-deep .dialog-footer-inner {
  float: left !important;
}
.onepass-switch-wrapper {
  margin-bottom: 10px;
}
.onepass-switch-row {
  display: flex;
  align-items: center;
  gap: 16px;
}
.onepass-switch-label {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}
.onepass-switch-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
.onepass-disabled {
  opacity: 0.5;
  pointer-events: none;
}
</style>
```

**Step 2: 验证前端编译**

Run: `cd /Users/xziying/project/bespoke/crmeb_java/admin && npx vue-cli-service build --no-clean 2>&1 | tail -5`
Expected: 无报错

**Step 3: 提交**

```
feat: 一号通配置页面添加启用/关闭开关
```

---

### Task 7: 前端订单发货页面添加一号通状态检查

**Files:**
- Modify: `admin/src/views/order/orderSend.vue:209,318-329`

**Step 1: 添加 onePassStatusApi 导入**

修改 import 行，添加 `onePassStatusApi`：

将：
```javascript
import { expressAllApi, exportTempApi, shipmentExpressApi } from '@/api/sms';
```
改为：
```javascript
import { expressAllApi, exportTempApi, shipmentExpressApi } from '@/api/sms';
import { onePassStatusApi } from '@/api/systemConfig';
```

**Step 2: 在 data() 中添加一号通状态字段**

在 `nowCompany: '',` 之后添加：

```javascript
      onePassEnabled: false, // 一号通是否启用
```

**Step 3: 修改 mounted() 逻辑**

将 mounted() 改为：

```javascript
  mounted() {
    this.express = this.expressListNormal;
    if (checkPermi(['admin:pass:shipment:express'])) this.checkOnePassAndLoadExpress();
  },
```

**Step 4: 添加 checkOnePassAndLoadExpress 方法**

在 `getShipmentExpress()` 方法之前添加：

```javascript
    // 检查一号通状态后加载商家寄件快递列表
    checkOnePassAndLoadExpress() {
      onePassStatusApi()
        .then((res) => {
          this.onePassEnabled = res.data === true;
          if (this.onePassEnabled) {
            this.getShipmentExpress();
          }
        })
        .catch(() => {
          this.onePassEnabled = false;
        });
    },
```

**Step 5: 修改商家寄件 Tab 的显示逻辑**

在模板中找到商家寄件的 radio 选项（约第38行）：

将：
```html
            <el-radio label="3">商家寄件</el-radio>
```
改为：
```html
            <el-radio label="3" :disabled="!onePassEnabled">商家寄件{{ !onePassEnabled ? '（未启用一号通）' : '' }}</el-radio>
```

**Step 6: 提交**

```
feat: 订单发货页面添加一号通状态检查，关闭时禁用商家寄件
```

---

### Task 8: 前端短信管理页面添加一号通状态检查

**Files:**
- Modify: `admin/src/views/sms/smsPay/index.vue`

**Step 1: 添加 onePassStatusApi 导入**

在已有的 sms API 导入行旁边添加：

```javascript
import { onePassStatusApi } from '@/api/systemConfig';
```

**Step 2: 修改 mounted 或 created 生命周期**

找到调用 `smsInfoApi()` 的地方（`getNumber` 方法），在调用前加入一号通状态检查。

在 `getNumber()` 方法的 `smsInfoApi()` 调用之前添加开关检查：

```javascript
    getNumber() {
      onePassStatusApi()
        .then((statusRes) => {
          if (statusRes.data !== true) {
            this.$message.warning('一号通功能未启用，请在设置中开启');
            return;
          }
          smsInfoApi().then(async (res) => {
            // ... 原有逻辑不变
          });
        })
        .catch(() => {
          this.$message.warning('一号通功能未启用，请在设置中开启');
        });
    },
```

**Step 3: 提交**

```
feat: 短信管理页面添加一号通状态检查
```

---

### Task 9: 全量编译验证

**Step 1: 后端全量编译**

Run: `cd /Users/xziying/project/bespoke/crmeb_java/crmeb && mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

**Step 2: 前端 lint 检查**

Run: `cd /Users/xziying/project/bespoke/crmeb_java/admin && npm run lint`
Expected: 无 error

**Step 3: 提交**

如有 lint 修复，提交修复。

---

## 手动验证清单

完成所有代码修改后，手动验证以下场景：

1. **默认状态验证**
   - 启动后端服务
   - 打开管理后台 → 订单管理 → 点击发货按钮
   - 预期：页面正常打开，不报"一号通平台接口"错误
   - 预期：商家寄件 radio 显示为禁用状态，标注"（未启用一号通）"

2. **开关 UI 验证**
   - 导航到 设置 → 短信设置 → 一号通配置
   - 预期：页面顶部有 Switch 开关，默认为关闭
   - 预期：关闭时下方 accessKey/secretKey 表单灰显不可操作

3. **开启验证**
   - 切换开关为开启
   - 预期：提示"一号通已启用"
   - 预期：表单恢复可操作
   - 刷新订单发货页面
   - 预期：商家寄件 radio 可选（前提是 key 配置正确）

4. **后端兜底验证**
   - 开关关闭时，用 Swagger 直接调用 `/api/admin/pass/shipment/express`
   - 预期：返回错误信息"一号通功能未启用，请在设置中开启"，而非"accessKey或者secretKey不正确"
