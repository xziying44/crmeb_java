<template>
  <div class="divBox">
    <el-card :bordered="false" shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form size="small" label-width="95px">
          <el-form-item label="业务员：">
            <el-select v-model="listPram.salesmanId" placeholder="全部" clearable class="selWidth" size="small">
              <el-option v-for="item in salesmanOptions" :key="item.id" :label="item.realName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="关键字：">
            <el-input v-model="listPram.keywords" placeholder="昵称/手机号" class="selWidth" size="small" clearable />
          </el-form-item>
          <div class="ml30">
            <el-button type="primary" size="small" @click="handleSearch">搜索</el-button>
            <el-button size="small" @click="handleReset">重置</el-button>
          </div>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <el-table v-loading="listLoading" :data="listData.list" style="width: 100%" size="mini" class="table">
        <el-table-column prop="uid" label="UID" width="90" />
        <el-table-column label="头像" min-width="80">
          <template slot-scope="scope">
            <div class="demo-image__preview" v-if="scope.row.avatar">
              <el-image :src="scope.row.avatar" :preview-src-list="[scope.row.avatar]" />
            </div>
            <span v-else class="gray">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="昵称" min-width="140" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="salesmanName" label="业务员" min-width="120" />
        <el-table-column prop="bindTime" label="绑定时间" min-width="160" />
        <el-table-column prop="totalAmount" label="消费总额" min-width="120" />
        <el-table-column prop="orderCount" label="订单数" min-width="100" />
        <el-table-column label="操作" width="120" fixed="right">
          <template slot-scope="scope">
            <a @click="openTransfer(scope.row)" v-hasPermi="['admin:salesman:transfer']">转移</a>
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

    <el-dialog title="转移客户" :visible.sync="transferVisible" width="520px" :close-on-click-modal="false">
      <el-form :model="transferForm" label-width="110px" size="small">
        <el-form-item label="客户UID：">
          <el-input v-model="transferForm.uid" disabled />
        </el-form-item>
        <el-form-item label="目标业务员：">
          <el-select v-model="transferForm.targetSalesmanId" placeholder="请选择业务员" class="selWidth" size="small">
            <el-option v-for="item in salesmanOptions" :key="item.id" :label="item.realName" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button size="small" @click="transferVisible = false">取消</el-button>
        <el-button type="primary" size="small" :loading="transferLoading" @click="submitTransfer">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { salesmanAllListApi, salesmanBindListApi, salesmanTransferApi } from '@/api/salesman.js';

export default {
  data() {
    return {
      constants: this.$constants,
      listLoading: false,
      listData: { list: [], total: 0 },
      listPram: {
        salesmanId: null,
        keywords: null,
        page: 1,
        limit: this.$constants.page.limit[0],
      },
      salesmanOptions: [],
      transferVisible: false,
      transferLoading: false,
      transferForm: {
        uid: null,
        targetSalesmanId: null,
      },
    };
  },
  mounted() {
    this.getSalesmanOptions();
    this.getList();
  },
  methods: {
    handleSearch() {
      this.listPram.page = 1;
      this.getList();
    },
    handleReset() {
      this.listPram.salesmanId = null;
      this.listPram.keywords = null;
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
    getSalesmanOptions() {
      salesmanAllListApi().then((list) => {
        this.salesmanOptions = list || [];
      });
    },
    getList() {
      this.listLoading = true;
      salesmanBindListApi(this.listPram)
        .then((data) => {
          this.listData = data || { list: [], total: 0 };
        })
        .finally(() => {
          this.listLoading = false;
        });
    },
    openTransfer(row) {
      this.transferForm = {
        uid: row.uid,
        targetSalesmanId: null,
      };
      this.transferVisible = true;
    },
    submitTransfer() {
      if (!this.transferForm.targetSalesmanId) {
        return this.$message.warning('请选择目标业务员');
      }
      this.transferLoading = true;
      salesmanTransferApi({
        uid: this.transferForm.uid,
        targetSalesmanId: this.transferForm.targetSalesmanId,
      })
        .then(() => {
          this.$message.success('转移成功');
          this.transferVisible = false;
          this.getList();
        })
        .finally(() => {
          this.transferLoading = false;
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

