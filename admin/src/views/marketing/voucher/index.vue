<template>
  <div class="divBox">
    <el-card :bordered="false" shadow="never" class="ivu-mt mb14" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <div class="demo-input-suffix acea-row">
          <span class="seachTiele">状态：</span>
          <el-select v-model="tableFrom.status" placeholder="请选择" class="filter-item selWidth mr30" @change="seachList" clearable>
            <el-option label="未开启" :value="false" />
            <el-option label="开启" :value="true" />
          </el-select>
          <span class="seachTiele">代金券名称：</span>
          <el-input v-model="tableFrom.name" placeholder="请输入代金券名称" class="selWidth" clearable></el-input>
          <el-button class="ml30" type="primary" size="small" @click="seachList">搜索</el-button>
          <el-button size="small" @click="handleReset">重置</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <div slot="header" class="clearfix">
        <el-button type="primary" v-hasPermi="['admin:marketing:voucher:save']" @click="handleAdd">新增代金券</el-button>
      </div>
      <el-table v-loading="listLoading" :data="tableData.data" style="width: 100%" size="mini">
        <el-table-column prop="id" label="ID" min-width="60" />
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="money" label="面值" min-width="100" />
        <el-table-column label="可抵扣运费" min-width="120">
          <template slot-scope="{ row }">
            <span>{{ row.canDeductFreight ? '是' : '否' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="售价" min-width="100">
          <template slot-scope="{ row }">
            <span>{{ row.canBuy ? '¥' + row.price : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="是否开启" min-width="120">
          <template slot-scope="{ row }">
            <span>{{ row.status ? '开启' : '关闭' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template slot-scope="scope">
            <a v-hasPermi="['admin:marketing:voucher:send']" @click="handleSend(scope.row)">发放</a>
            <el-divider direction="vertical"></el-divider>
            <a v-hasPermi="['admin:marketing:voucher:info']" @click="handleCopy(scope.row)">复制</a>
            <el-divider direction="vertical"></el-divider>
            <a v-hasPermi="['admin:marketing:voucher:delete']" @click="handleDelete(scope.row)">删除</a>
          </template>
        </el-table-column>
      </el-table>
      <div class="block">
        <el-pagination
          :page-sizes="[20, 40, 60, 80]"
          :page-size="tableFrom.limit"
          :current-page="tableFrom.page"
          layout="total, sizes, prev, pager, next, jumper"
          :total="tableData.total"
          @size-change="handleSizeChange"
          @current-change="pageChange"
          background
        />
      </div>
    </el-card>

    <el-dialog :title="form.id ? '复制代金券' : '新增代金券'" :visible.sync="editVisible" width="720px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入代金券名称" />
        </el-form-item>
        <el-form-item label="面值" prop="money">
          <el-input-number v-model="form.money" :min="0" :precision="2" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="有效天数" prop="day">
          <el-input-number v-model="form.day" :min="1" :max="999" controls-position="right" />
        </el-form-item>
        <el-form-item label="可抵扣运费">
          <el-switch v-model="form.canDeductFreight" :active-value="true" :inactive-value="false" active-text="是" inactive-text="否" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="是否开启" prop="status">
          <el-switch v-model="form.status" :active-value="true" :inactive-value="false" active-text="开启" inactive-text="关闭" />
        </el-form-item>
        <el-form-item label="是否可购买">
          <el-switch v-model="form.canBuy" :active-value="true" :inactive-value="false" active-text="是" inactive-text="否" />
        </el-form-item>
        <el-form-item v-if="form.canBuy" label="售价" prop="price">
          <el-input-number v-model="form.price" :min="0" :max="form.money" :precision="2" controls-position="right" />
          <span style="margin-left: 10px; color: #909399;">用户支付此金额购买面值 {{ form.money }} 元的代金券</span>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="editVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保 存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { voucherListApi, voucherInfoApi, voucherSaveApi, voucherDeleteApi, voucherSendApi } from '@/api/marketing';

export default {
  name: 'VoucherList',
  data() {
    return {
      listLoading: false,
      tableData: {
        data: [],
        total: 0,
      },
      tableFrom: {
        page: 1,
        limit: 20,
        name: '',
        status: undefined,
      },
      editVisible: false,
      saving: false,
      form: this.getDefaultForm(),
      rules: {
        name: [{ required: true, message: '请输入代金券名称', trigger: 'blur' }],
        money: [{ required: true, message: '请输入面值', trigger: 'change' }],
        day: [{ required: true, message: '请输入有效天数', trigger: 'change' }],
        sort: [{ required: true, message: '请输入排序', trigger: 'change' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }],
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
      },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getDefaultForm() {
      return {
        id: null,
        name: '',
        money: 0,
        canDeductFreight: false,
        day: 30,
        sort: 0,
        status: true,
        canBuy: false,
        price: 0,
        // 以下为后端创建所需的默认字段（代金券：全场、无门槛、领取不限时、按天有效）
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
    seachList() {
      this.tableFrom.page = 1;
      this.getList();
    },
    handleReset() {
      this.tableFrom = {
        page: 1,
        limit: 20,
        name: '',
        status: undefined,
      };
      this.getList();
    },
    getList() {
      this.listLoading = true;
      voucherListApi(this.tableFrom)
        .then((res) => {
          this.tableData.data = res.list || [];
          this.tableData.total = res.total || 0;
          this.listLoading = false;
        })
        .catch(() => {
          this.listLoading = false;
        });
    },
    pageChange(page) {
      this.tableFrom.page = page;
      this.getList();
    },
    handleSizeChange(val) {
      this.tableFrom.limit = val;
      this.getList();
    },
    handleAdd() {
      this.form = this.getDefaultForm();
      this.editVisible = true;
    },
    handleCopy(row) {
      this.form = this.getDefaultForm();
      this.editVisible = true;
      voucherInfoApi(row.id).then((res) => {
        const c = res.coupon || {};
        // 复制时不带原id，后端按新增处理
        this.form.name = c.name || '';
        this.form.money = c.money || 0;
        this.form.canDeductFreight = !!c.canDeductFreight;
        this.form.day = c.day || 30;
        this.form.sort = c.sort || 0;
        this.form.status = c.status !== undefined ? c.status : true;
      });
    },
    handleDelete(row) {
      this.$modalSure('删除当前数据?').then(() => {
        voucherDeleteApi(row.id).then(() => {
          this.$message.success('删除成功');
          this.tableFrom.page = 1;
          this.getList();
        });
      });
    },
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
    handleSubmit() {
      this.$refs.formRef.validate((valid) => {
        if (!valid) return;
        this.saving = true;
        const payload = { ...this.form };
        // 复制/新增均走新增逻辑
        payload.id = undefined;
        voucherSaveApi(payload)
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
  },
};
</script>

