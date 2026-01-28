<template>
  <el-dialog
    :title="editId ? '编辑满减活动' : '新增满减活动'"
    :visible.sync="visible"
    width="900px"
    :close-on-click-modal="false"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="活动名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入活动名称" />
      </el-form-item>

      <el-form-item label="活动范围" prop="scopeType">
        <el-radio-group v-model="form.scopeType">
          <el-radio :label="1">全场</el-radio>
          <el-radio :label="2">品类</el-radio>
          <el-radio :label="3">指定商品</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="时间范围" required>
        <el-date-picker
          v-model="form.startTime"
          type="datetime"
          value-format="yyyy-MM-dd HH:mm:ss"
          placeholder="开始时间"
          style="width: 260px"
        />
        <span style="margin: 0 10px">-</span>
        <el-date-picker
          v-model="form.endTime"
          type="datetime"
          value-format="yyyy-MM-dd HH:mm:ss"
          placeholder="结束时间"
          style="width: 260px"
        />
      </el-form-item>

      <el-form-item label="叠加优惠券">
        <el-switch v-model="form.allowCoupon" :active-value="true" :inactive-value="false" active-text="允许" inactive-text="不允许" />
      </el-form-item>

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

      <el-form-item label="阶梯满减" required>
        <el-table :data="form.levels" size="mini" border>
          <el-table-column label="满足金额" min-width="180">
            <template slot-scope="{ row }">
              <el-input-number v-model="row.fullAmount" :min="0" :precision="2" :step="1" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="减免金额" min-width="180">
            <template slot-scope="{ row }">
              <el-input-number v-model="row.reduceAmount" :min="0" :precision="2" :step="1" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template slot-scope="{ $index }">
              <el-button type="text" @click="removeLevel($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="margin-top: 10px">
          <el-button size="small" @click="addLevel">添加阶梯</el-button>
        </div>
      </el-form-item>
    </el-form>

    <span slot="footer" class="dialog-footer">
      <el-button @click="visible = false">取 消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">保 存</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { fullReductionDetailApi, fullReductionSaveApi } from '@/api/promotion';
import { categoryApi } from '@/api/store';

export default {
  name: 'FullReductionEdit',
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    editId: {
      type: [Number, String],
      default: null,
    },
  },
  data() {
    return {
      loading: false,
      form: this.getDefaultForm(),
      merCateList: [],
      props: {
        children: 'child',
        label: 'name',
        value: 'id',
        multiple: true,
        emitPath: false,
      },
      rules: {
        name: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
        scopeType: [{ required: true, message: '请选择活动范围', trigger: 'change' }],
        selectedCategoryIds: [
          {
            validator: (rule, value, callback) => {
              if (this.form.scopeType === 2 && (!value || !value.length)) {
                return callback(new Error('请选择品类'));
              }
              return callback();
            },
            trigger: 'change',
          },
        ],
        selectedProducts: [
          {
            validator: (rule, value, callback) => {
              if (this.form.scopeType === 3 && (!value || !value.length)) {
                return callback(new Error('请选择商品'));
              }
              return callback();
            },
            trigger: 'change',
          },
        ],
      },
    };
  },
  watch: {
    visible(val) {
      if (val) {
        this.init();
      }
    },
  },
  mounted() {
    this.getCategoryList();
  },
  methods: {
    getCategoryList() {
      categoryApi({ status: -1, type: 1 }).then((res) => {
        this.merCateList = res || [];
      });
    },
    getDefaultForm() {
      return {
        id: null,
        name: '',
        scopeType: 1,
        startTime: '',
        endTime: '',
        allowCoupon: true,
        selectedCategoryIds: [],
        selectedProducts: [],
        levels: [{ fullAmount: 0, reduceAmount: 0 }],
      };
    },
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
          // 回显品类/商品
          if (res.scopeType === 2) {
            this.form.selectedCategoryIds = res.relationIds || [];
          } else if (res.scopeType === 3) {
            this.form.selectedProducts = (res.products || []).map((p) => ({
              id: p.id || p.productId,
              image: p.image,
              storeName: p.storeName,
            }));
          }
          if (!this.form.levels.length) {
            this.form.levels = [{ fullAmount: 0, reduceAmount: 0 }];
          }
        });
      }
    },
    selectProducts() {
      const _this = this;
      this.$modalGoodList(function (products) {
        _this.form.selectedProducts = products;
      }, 'many', this.form.selectedProducts);
    },
    removeProduct(index) {
      this.form.selectedProducts.splice(index, 1);
    },
    addLevel() {
      this.form.levels.push({ fullAmount: 0, reduceAmount: 0 });
    },
    removeLevel(index) {
      this.form.levels.splice(index, 1);
      if (!this.form.levels.length) {
        this.form.levels.push({ fullAmount: 0, reduceAmount: 0 });
      }
    },
    handleSubmit() {
      this.$refs.formRef.validate((valid) => {
        if (!valid) return;
        if (!this.form.startTime || !this.form.endTime) {
          this.$message.error('请选择时间范围');
          return;
        }

        // 计算 relationIds
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

        const payload = {
          id: this.form.id,
          name: this.form.name,
          scopeType: this.form.scopeType,
          startTime: this.form.startTime,
          endTime: this.form.endTime,
          allowCoupon: this.form.allowCoupon,
          relationIds: relationIds,
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
  },
};
</script>

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
