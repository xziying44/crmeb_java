<template>
  <div class="divBox">
    <el-card :bordered="false" shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form size="small" label-width="75px">
          <el-form-item label="关键字：">
            <el-input
              v-model="listPram.keywords"
              placeholder="请输入姓名/手机号/账号/邀请码"
              class="selWidth"
              size="small"
              clearable
            />
          </el-form-item>
          <el-form-item label="状态：">
            <el-select v-model="listPram.status" placeholder="全部" clearable size="small" class="selWidth">
              <el-option label="正常" :value="true" />
              <el-option label="禁用" :value="false" />
            </el-select>
          </el-form-item>
          <div class="ml30">
            <el-button type="primary" size="small" @click="handleSearch">搜索</el-button>
            <el-button size="small" @click="handleReset">重置</el-button>
            <el-button type="success" size="small" @click="openCreate" v-hasPermi="['admin:salesman:save']"
              >创建业务员</el-button
            >
          </div>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <el-table v-loading="listLoading" :data="listData.list" style="width: 100%" size="mini" class="table">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="account" label="账号" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="salesmanCode" label="邀请码" min-width="120" />
        <el-table-column label="小程序码" min-width="120">
          <template slot-scope="scope">
            <div v-if="scope.row.salesmanQrcode" class="demo-image__preview">
              <el-image :src="scope.row.salesmanQrcode" :preview-src-list="[scope.row.salesmanQrcode]" />
            </div>
            <span v-else class="gray">未生成</span>
          </template>
        </el-table-column>
        <el-table-column label="可绑定" min-width="80">
          <template slot-scope="scope">
            <span>{{ scope.row.bindable ? '是' : '否' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="90">
          <template slot-scope="scope">
            <el-switch
              v-model="scope.row.status"
              :active-value="true"
              :inactive-value="false"
              @change="onChangeStatus(scope.row)"
              v-hasPermi="['admin:salesman:update']"
            />
          </template>
        </el-table-column>
        <el-table-column prop="customerCount" label="客户数" min-width="90" />
        <el-table-column prop="monthNewCustomerCount" label="本月新增客户" min-width="110" />
        <el-table-column prop="createTime" label="创建时间" min-width="150" />
        <el-table-column label="操作" width="240" fixed="right">
          <template slot-scope="scope">
            <a @click="openEdit(scope.row)" v-hasPermi="['admin:salesman:update']">编辑</a>
            <el-divider direction="vertical" />
            <a @click="handleDelete(scope.row)" v-hasPermi="['admin:salesman:delete']">删除</a>
            <el-divider direction="vertical" />
            <el-dropdown>
              <span class="el-dropdown-link">更多<i class="el-icon-arrow-down el-icon--right" /></span>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item @click.native="handleRegenerateCode(scope.row)" v-hasPermi="['admin:salesman:update']"
                  >重新生成邀请码</el-dropdown-item
                >
                <el-dropdown-item @click.native="handleGenerateQrcode(scope.row)" v-hasPermi="['admin:salesman:update']"
                  >生成/刷新小程序码</el-dropdown-item
                >
              </el-dropdown-menu>
            </el-dropdown>
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
        <el-form-item label="账号" prop="account" v-if="isCreate">
          <el-input v-model="form.account" maxlength="32" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="账号" v-else>
          <el-input v-model="form.account" disabled />
        </el-form-item>
        <el-form-item :label="isCreate ? '密码' : '密码(选填)'" prop="pwd">
          <el-input v-model="form.pwd" maxlength="32" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" maxlength="16" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="11" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="可绑定" v-if="!isCreate">
          <el-switch v-model="form.bindable" :active-value="true" :inactive-value="false" />
        </el-form-item>
        <el-form-item label="状态" v-if="isCreate" prop="status">
          <el-switch v-model="form.status" :active-value="true" :inactive-value="false" />
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
  salesmanListApi,
  salesmanAddApi,
  salesmanUpdateApi,
  salesmanDeleteApi,
  salesmanUpdateStatusApi,
  salesmanRegenerateCodeApi,
  salesmanGenerateQrcodeApi,
} from '@/api/salesman.js';

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
      dialogTitle: '创建业务员',
      isCreate: true,
      form: {
        id: null,
        account: '',
        pwd: '',
        realName: '',
        phone: '',
        status: true,
        bindable: true,
      },
      rules: {
        account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
        pwd: [{ required: true, message: '请输入密码', trigger: 'blur' }],
        realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        phone: [
          { required: true, message: '请输入手机号', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
        ],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }],
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
      salesmanListApi(this.listPram)
        .then((data) => {
          this.listData = data || { list: [], total: 0 };
        })
        .finally(() => {
          this.listLoading = false;
        });
    },
    openCreate() {
      this.isCreate = true;
      this.dialogTitle = '创建业务员';
      this.form = {
        id: null,
        account: '',
        pwd: '',
        realName: '',
        phone: '',
        status: true,
        bindable: true,
      };
      this.rules.pwd = [{ required: true, message: '请输入密码', trigger: 'blur' }];
      this.dialogVisible = true;
      this.$nextTick(() => {
        this.$refs.formRef && this.$refs.formRef.clearValidate();
      });
    },
    openEdit(row) {
      this.isCreate = false;
      this.dialogTitle = '编辑业务员';
      this.form = {
        id: row.id,
        account: row.account,
        pwd: '',
        realName: row.realName,
        phone: row.phone,
        status: row.status,
        bindable: row.bindable,
      };
      // 编辑时密码可不填
      this.rules.pwd = [{ required: false, message: '请输入密码', trigger: 'blur' }];
      this.dialogVisible = true;
      this.$nextTick(() => {
        this.$refs.formRef && this.$refs.formRef.clearValidate();
      });
    },
    handleSubmit() {
      this.$refs.formRef.validate((valid) => {
        if (!valid) return;
        this.submitLoading = true;
        if (this.isCreate) {
          salesmanAddApi({
            account: this.form.account,
            pwd: this.form.pwd,
            realName: this.form.realName,
            phone: this.form.phone,
            status: this.form.status,
          })
            .then(() => {
              this.$message.success('创建成功');
              this.dialogVisible = false;
              this.getList();
            })
            .finally(() => {
              this.submitLoading = false;
            });
          return;
        }
        const payload = {
          id: this.form.id,
          realName: this.form.realName,
          phone: this.form.phone,
          bindable: this.form.bindable,
        };
        if (this.form.pwd) payload.pwd = this.form.pwd;
        salesmanUpdateApi(payload)
          .then(() => {
            this.$message.success('更新成功');
            this.dialogVisible = false;
            this.getList();
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },
    handleDelete(row) {
      this.$modalSure('删除当前业务员').then(() => {
        salesmanDeleteApi(row.id).then(() => {
          this.$message.success('删除成功');
          this.getList();
        });
      });
    },
    onChangeStatus(row) {
      salesmanUpdateStatusApi(row.id, row.status)
        .then(() => {
          this.$message.success('修改成功');
        })
        .catch(() => {
          row.status = !row.status;
        });
    },
    handleRegenerateCode(row) {
      this.$modalSure('重新生成邀请码将清除旧二维码（如已生成）').then(() => {
        salesmanRegenerateCodeApi(row.id).then((newCode) => {
          this.$message.success(`新邀请码：${newCode}`);
          this.getList();
        });
      });
    },
    handleGenerateQrcode(row) {
      salesmanGenerateQrcodeApi(row.id).then((url) => {
        this.$message.success('生成成功');
        row.salesmanQrcode = url;
      });
    },
  },
};
</script>

<style scoped>
.gray {
  color: #909399;
}
</style>

