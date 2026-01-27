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

      <el-form-item v-if="form.scopeType !== 1" label="关联ID列表" prop="relationIdsStr">
        <el-input
          v-model="form.relationIdsStr"
          type="textarea"
          :rows="3"
          placeholder="请输入关联的ID，多个用英文逗号分隔（scopeType=2为品类ID，scopeType=3为商品ID）"
        />
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
      rules: {
        name: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
        scopeType: [{ required: true, message: '请选择活动范围', trigger: 'change' }],
        relationIdsStr: [
          {
            validator: (rule, value, callback) => {
              if (this.form.scopeType === 1) return callback();
              if (!value) return callback(new Error('请输入关联ID列表'));
              return callback();
            },
            trigger: 'blur',
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
  methods: {
    getDefaultForm() {
      return {
        id: null,
        name: '',
        scopeType: 1,
        startTime: '',
        endTime: '',
        allowCoupon: true,
        relationIdsStr: '',
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
          this.form.relationIdsStr = (res.relationIds || []).join(',');
          if (!this.form.levels.length) {
            this.form.levels = [{ fullAmount: 0, reduceAmount: 0 }];
          }
        });
      }
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
    parseRelationIds() {
      if (!this.form.relationIdsStr) return [];
      return this.form.relationIdsStr
        .split(',')
        .map((s) => Number(String(s).trim()))
        .filter((n) => Number.isInteger(n) && n > 0);
    },
    handleSubmit() {
      this.$refs.formRef.validate((valid) => {
        if (!valid) return;
        if (!this.form.startTime || !this.form.endTime) {
          this.$message.error('请选择时间范围');
          return;
        }
        const payload = {
          id: this.form.id,
          name: this.form.name,
          scopeType: this.form.scopeType,
          startTime: this.form.startTime,
          endTime: this.form.endTime,
          allowCoupon: this.form.allowCoupon,
          relationIds: this.form.scopeType === 1 ? [] : this.parseRelationIds(),
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

