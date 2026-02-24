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
