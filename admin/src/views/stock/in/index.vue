<template>
  <div class="divBox">
    <el-card class="box-card">
      <div class="padding-add">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" size="small">
          <el-form-item label="选择商品" prop="productId">
            <div class="upLoadPicBox" @click="changeGood">
              <div v-if="form.productImage" class="pictrue"><img :src="form.productImage" /></div>
              <div v-else class="upLoad">
                <i class="el-icon-camera cameraIconfont" />
              </div>
            </div>
            <div v-if="form.productName" class="mt10">{{ form.productName }}</div>
          </el-form-item>
          <el-form-item label="规格" v-if="skuOptions.length">
            <el-select v-model="form.attrValueId" placeholder="请选择规格（无规格可不选）" clearable class="selWidth">
              <el-option v-for="s in skuOptions" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="入库类型" prop="type">
            <el-select v-model="form.type" placeholder="请选择入库类型" class="selWidth">
              <el-option label="退货入库" :value="2" />
              <el-option label="其他入库" :value="4" />
            </el-select>
          </el-form-item>
          <el-form-item label="数量" prop="quantity">
            <el-input-number v-model="form.quantity" :min="1" :max="999999" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="255" placeholder="请输入备注" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="small" :loading="submitLoading" @click="handleSubmit" v-hasPermi="['admin:stock:in']"
              >提交入库</el-button
            >
            <el-button size="small" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script>
import { stockInApi } from '@/api/stock.js';
import { productDetailApi } from '@/api/store.js';

export default {
  data() {
    return {
      submitLoading: false,
      skuOptions: [],
      form: {
        productId: null,
        productName: '',
        productImage: '',
        attrValueId: null,
        type: 4,
        quantity: 1,
        remark: '',
      },
      rules: {
        productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
        type: [{ required: true, message: '请选择入库类型', trigger: 'change' }],
        quantity: [{ required: true, message: '请输入数量', trigger: 'change' }],
      },
    };
  },
  methods: {
    changeGood() {
      const _this = this;
      this.$modalGoodList(function (row) {
        _this.form.productId = row.id;
        _this.form.productName = row.storeName || row.store_name || row.name || '';
        _this.form.productImage = row.image || '';
        _this.loadSku(row.id);
      });
    },
    loadSku(productId) {
      this.skuOptions = [];
      this.form.attrValueId = null;
      if (!productId) return;
      productDetailApi(productId).then((info) => {
        const list = (info && info.attrValue) || [];
        const options = list.map((v) => {
          let label = '';
          try {
            const av = typeof v.attrValue === 'string' ? JSON.parse(v.attrValue) : v.attrValue;
            label = av && typeof av === 'object' ? Object.values(av).join('-') : '';
          } catch (e) {
            label = '';
          }
          if (!label) label = v.suk || v.unique || `规格${v.id}`;
          return { value: v.id, label };
        });
        this.skuOptions = options;
        if (!info || !info.specType) {
          // 单规格默认选中唯一一条
          if (options.length === 1) this.form.attrValueId = options[0].value;
        }
      });
    },
    handleSubmit() {
      this.$refs.formRef.validate((valid) => {
        if (!valid) return;
        this.submitLoading = true;
        stockInApi({
          productId: this.form.productId,
          attrValueId: this.form.attrValueId || 0,
          type: this.form.type,
          quantity: this.form.quantity,
          remark: this.form.remark,
        })
          .then(() => {
            this.$message.success('入库成功');
            this.handleReset();
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },
    handleReset() {
      this.skuOptions = [];
      this.form = {
        productId: null,
        productName: '',
        productImage: '',
        attrValueId: null,
        type: 4,
        quantity: 1,
        remark: '',
      };
      this.$nextTick(() => {
        this.$refs.formRef && this.$refs.formRef.clearValidate();
      });
    },
  },
};
</script>

<style scoped>
.upLoadPicBox {
  width: 80px;
  height: 80px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.pictrue img {
  width: 80px;
  height: 80px;
  object-fit: cover;
}
</style>

