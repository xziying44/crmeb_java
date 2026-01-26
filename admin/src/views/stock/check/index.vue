<template>
  <div class="divBox">
    <el-card :bordered="false" shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form size="small" label-width="80px">
          <el-form-item label="盘点单号：">
            <el-input v-model="listPram.checkNo" placeholder="请输入盘点单号" class="selWidth" clearable />
          </el-form-item>
          <el-form-item label="状态：">
            <el-select v-model="listPram.status" placeholder="全部" clearable class="selWidth">
              <el-option label="盘点中" :value="0" />
              <el-option label="已完成" :value="1" />
              <el-option label="已取消" :value="2" />
            </el-select>
          </el-form-item>
          <div class="ml30">
            <el-button type="primary" size="small" @click="handleSearch">搜索</el-button>
            <el-button size="small" @click="handleReset">重置</el-button>
            <el-button type="success" size="small" @click="openCreate" v-hasPermi="['admin:stock:check:create']"
              >创建盘点单</el-button
            >
          </div>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <el-table v-loading="listLoading" :data="listData.list" style="width: 100%" size="mini" class="table">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="checkNo" label="盘点单号" min-width="220" />
        <el-table-column prop="statusName" label="状态" width="90" />
        <el-table-column prop="totalProfit" label="盘盈" width="80" />
        <el-table-column prop="totalLoss" label="盘亏" width="80" />
        <el-table-column prop="operatorName" label="创建人" width="100" />
        <el-table-column prop="createTime" label="创建时间" min-width="150" />
        <el-table-column prop="finishTime" label="完成时间" min-width="150" />
        <el-table-column label="操作" width="220" fixed="right">
          <template slot-scope="scope">
            <a @click="openDetail(scope.row)" v-hasPermi="['admin:stock:check:detail']">详情</a>
            <el-divider direction="vertical" />
            <a
              v-if="scope.row.status === 0"
              @click="handleConfirm(scope.row)"
              v-hasPermi="['admin:stock:check:confirm']"
              >确认</a
            >
            <el-divider direction="vertical" v-if="scope.row.status === 0" />
            <a
              v-if="scope.row.status === 0"
              @click="handleCancel(scope.row)"
              v-hasPermi="['admin:stock:check:cancel']"
              >取消</a
            >
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

    <!-- 创建盘点单 -->
    <el-dialog title="创建盘点单" :visible.sync="createDialogVisible" width="600px" :close-on-click-modal="false">
      <el-form ref="createFormRef" :model="createForm" label-width="110px" size="small">
        <el-form-item label="盘点商品ID">
          <el-input
            v-model="createForm.productIdsText"
            placeholder="可选：多个商品ID用英文逗号分隔；为空则盘点全部"
            clearable
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="3" maxlength="500" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button size="small" @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" size="small" :loading="submitLoading" @click="submitCreate">确定</el-button>
      </div>
    </el-dialog>

    <!-- 盘点单详情 -->
    <el-dialog title="盘点单详情" :visible.sync="detailDialogVisible" width="1000px" :close-on-click-modal="false">
      <div v-loading="detailLoading">
        <div class="mb10">盘点单号：{{ detail.checkNo }}</div>
        <div class="mb10">状态：{{ detail.statusName }}</div>
        <el-table :data="detail.items || []" size="mini" border>
          <el-table-column prop="productName" label="商品" min-width="220" />
          <el-table-column prop="skuName" label="规格" min-width="180" />
          <el-table-column prop="systemStock" label="系统库存" width="90" />
          <el-table-column label="实际库存" width="140">
            <template slot-scope="scope">
              <el-input-number v-model="scope.row.actualStock" :min="0" :max="999999" size="mini" :disabled="detail.status !== 0" />
            </template>
          </el-table-column>
          <el-table-column prop="diffQuantity" label="差异" width="90" />
          <el-table-column label="操作" width="120" fixed="right">
            <template slot-scope="scope">
              <el-button
                type="primary"
                size="mini"
                :disabled="detail.status !== 0"
                @click="saveItem(scope.row)"
                v-hasPermi="['admin:stock:check:updateItem']"
                >保存</el-button
              >
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button size="small" @click="detailDialogVisible = false">关闭</el-button>
        <el-button
          v-if="detail.status === 0"
          type="success"
          size="small"
          :loading="submitLoading"
          @click="handleConfirm(detail)"
          v-hasPermi="['admin:stock:check:confirm']"
          >确认盘点</el-button
        >
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  stockCheckListApi,
  stockCheckCreateApi,
  stockCheckDetailApi,
  stockCheckUpdateItemApi,
  stockCheckConfirmApi,
  stockCheckCancelApi,
} from '@/api/stock.js';

export default {
  data() {
    return {
      constants: this.$constants,
      listLoading: false,
      detailLoading: false,
      submitLoading: false,
      listData: { list: [], total: 0 },
      listPram: {
        checkNo: null,
        status: null,
        page: 1,
        limit: this.$constants.page.limit[0],
      },
      createDialogVisible: false,
      createForm: {
        productIdsText: '',
        remark: '',
      },
      detailDialogVisible: false,
      detail: {},
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
      this.listPram.checkNo = null;
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
      stockCheckListApi(this.listPram)
        .then((data) => {
          this.listData = data || { list: [], total: 0 };
        })
        .finally(() => {
          this.listLoading = false;
        });
    },
    openCreate() {
      this.createForm = { productIdsText: '', remark: '' };
      this.createDialogVisible = true;
    },
    submitCreate() {
      this.submitLoading = true;
      const text = (this.createForm.productIdsText || '').trim();
      let productIds = null;
      if (text) {
        productIds = text
          .split(',')
          .map((s) => Number(String(s).trim()))
          .filter((n) => !Number.isNaN(n) && n > 0);
      }
      stockCheckCreateApi({ remark: this.createForm.remark, productIds })
        .then(() => {
          this.$message.success('创建成功');
          this.createDialogVisible = false;
          this.getList();
        })
        .finally(() => {
          this.submitLoading = false;
        });
    },
    openDetail(row) {
      this.detailDialogVisible = true;
      this.loadDetail(row.id);
    },
    loadDetail(id) {
      this.detailLoading = true;
      stockCheckDetailApi(id)
        .then((data) => {
          this.detail = data || {};
        })
        .finally(() => {
          this.detailLoading = false;
        });
    },
    saveItem(item) {
      if (item.actualStock === null || item.actualStock === undefined) return this.$message.warning('请输入实际库存');
      this.submitLoading = true;
      stockCheckUpdateItemApi({ itemId: item.id, actualStock: item.actualStock })
        .then(() => {
          this.$message.success('保存成功');
          // 重新加载，确保差异数量准确
          this.loadDetail(this.detail.id);
        })
        .finally(() => {
          this.submitLoading = false;
        });
    },
    handleConfirm(row) {
      this.$modalSure('确认盘点完成后将自动调整库存').then(() => {
        this.submitLoading = true;
        stockCheckConfirmApi(row.id)
          .then(() => {
            this.$message.success('确认成功');
            this.detailDialogVisible = false;
            this.getList();
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },
    handleCancel(row) {
      this.$modalSure('取消当前盘点单').then(() => {
        this.submitLoading = true;
        stockCheckCancelApi(row.id)
          .then(() => {
            this.$message.success('取消成功');
            this.getList();
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },
  },
};
</script>

