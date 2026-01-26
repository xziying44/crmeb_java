<template>
  <div class="divBox">
    <el-card :bordered="false" shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form size="small" label-width="75px">
          <el-form-item label="关键字：">
            <el-input v-model="listPram.keywords" placeholder="名称/联系人/电话" class="selWidth" clearable />
          </el-form-item>
          <el-form-item label="状态：">
            <el-select v-model="listPram.status" placeholder="全部" clearable class="selWidth">
              <el-option label="启用" :value="true" />
              <el-option label="禁用" :value="false" />
            </el-select>
          </el-form-item>
          <div class="ml30">
            <el-button type="primary" size="small" @click="handleSearch">搜索</el-button>
            <el-button size="small" @click="handleReset">重置</el-button>
            <el-button type="success" size="small" @click="openCreate" v-hasPermi="['admin:stock:supplier:add']"
              >新增供应商</el-button
            >
          </div>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <el-table v-loading="listLoading" :data="listData.list" style="width: 100%" size="mini" class="table">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="供应商名称" min-width="200" />
        <el-table-column prop="contact" label="联系人" min-width="100" />
        <el-table-column prop="phone" label="联系电话" min-width="120" />
        <el-table-column prop="address" label="地址" min-width="220" />
        <el-table-column label="状态" width="90">
          <template slot-scope="scope">
            <el-switch
              v-model="scope.row.status"
              :active-value="true"
              :inactive-value="false"
              @change="onChangeStatus(scope.row)"
              v-hasPermi="['admin:stock:supplier:updateStatus']"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="150" />
        <el-table-column label="操作" width="160" fixed="right">
          <template slot-scope="scope">
            <a @click="openEdit(scope.row)" v-hasPermi="['admin:stock:supplier:update']">编辑</a>
            <el-divider direction="vertical" />
            <a @click="handleDelete(scope.row)" v-hasPermi="['admin:stock:supplier:delete']">删除</a>
          </template>
        </el-table-column>
      </el-table>

      <div class="block">
        <el-pagination
          :page-sizes="constants.page.limit"
          :page-size="listPram.limit"
          :current-page="listPram.page"
          :layout="constants.page.layout"
          :total="listData.total"
          background
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" size="small">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="100" placeholder="请输入供应商名称" />
        </el-form-item>
        <el-form-item label="联系人" prop="contact">
          <el-input v-model="form.contact" maxlength="50" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" maxlength="20" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" maxlength="255" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" maxlength="500" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button size="small" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" size="small" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  supplierListApi,
  supplierAddApi,
  supplierUpdateApi,
  supplierDeleteApi,
  supplierUpdateStatusApi,
} from '@/api/stock.js';

export default {
  data() {
    return {
      constants: this.$constants,
      listLoading: false,
      submitLoading: false,
      listData: { list: [], total: 0 },
      listPram: {
        keywords: null,
        status: null,
        page: 1,
        limit: this.$constants.page.limit[0],
      },
      dialogVisible: false,
      dialogTitle: '新增供应商',
      isCreate: true,
      form: {
        id: null,
        name: '',
        contact: '',
        phone: '',
        address: '',
        remark: '',
      },
      rules: {
        name: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
      },
    };
  },
  mounted() {
    this.getList();
  },
  methods: {
    handleSearch() {
      this.listPram.page = 1;
      this.getList();
    },
    handleReset() {
      this.listPram.keywords = null;
      this.listPram.status = null;
      this.listPram.page = 1;
      this.getList();
    },
    handleSizeChange(val) {
      this.listPram.limit = val;
      this.getList();
    },
    handleCurrentChange(val) {
      this.listPram.page = val;
      this.getList();
    },
    getList() {
      this.listLoading = true;
      supplierListApi(this.listPram)
        .then((data) => {
          this.listData = data || { list: [], total: 0 };
        })
        .finally(() => {
          this.listLoading = false;
        });
    },
    openCreate() {
      this.isCreate = true;
      this.dialogTitle = '新增供应商';
      this.form = { id: null, name: '', contact: '', phone: '', address: '', remark: '' };
      this.dialogVisible = true;
      this.$nextTick(() => {
        this.$refs.formRef && this.$refs.formRef.clearValidate();
      });
    },
    openEdit(row) {
      this.isCreate = false;
      this.dialogTitle = '编辑供应商';
      this.form = {
        id: row.id,
        name: row.name,
        contact: row.contact,
        phone: row.phone,
        address: row.address,
        remark: row.remark,
      };
      this.dialogVisible = true;
      this.$nextTick(() => {
        this.$refs.formRef && this.$refs.formRef.clearValidate();
      });
    },
    handleSubmit() {
      this.$refs.formRef.validate((valid) => {
        if (!valid) return;
        this.submitLoading = true;
        const api = this.isCreate ? supplierAddApi : supplierUpdateApi;
        api(this.form)
          .then(() => {
            this.$message.success(this.isCreate ? '新增成功' : '更新成功');
            this.dialogVisible = false;
            this.getList();
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },
    handleDelete(row) {
      this.$modalSure('删除当前供应商').then(() => {
        supplierDeleteApi(row.id).then(() => {
          this.$message.success('删除成功');
          this.getList();
        });
      });
    },
    onChangeStatus(row) {
      supplierUpdateStatusApi(row.id, row.status)
        .then(() => {
          this.$message.success('修改成功');
        })
        .catch(() => {
          row.status = !row.status;
        });
    },
  },
};
</script>

