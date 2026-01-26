<template>
  <div class="divBox">
    <el-card :bordered="false" shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form size="small" label-width="80px">
          <el-form-item label="关键字：">
            <el-input v-model="listPram.keywords" placeholder="请输入商品名称" class="selWidth" clearable />
          </el-form-item>
          <el-form-item label="预警：">
            <el-select v-model="listPram.warning" placeholder="全部" clearable class="selWidth">
              <el-option label="仅预警" :value="true" />
              <el-option label="全部" :value="false" />
            </el-select>
          </el-form-item>
          <div class="ml30 mb20">
            <el-button type="primary" size="small" @click="handleSearch">搜索</el-button>
            <el-button size="small" @click="handleReset">重置</el-button>
            <el-button type="warning" size="small" @click="handleInit" v-hasPermi="['admin:stock:init']"
              >初始化库存</el-button
            >
          </div>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <el-table v-loading="listLoading" :data="listData.list" style="width: 100%" size="mini" class="table">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="productId" label="商品ID" width="90" />
        <el-table-column prop="productName" label="商品名称" min-width="220" />
        <el-table-column prop="skuName" label="规格" min-width="180" />
        <el-table-column prop="stock" label="库存" width="90" />
        <el-table-column prop="warningStock" label="预警库存" width="90" />
        <el-table-column label="预警" width="70">
          <template slot-scope="scope">
            <span :class="scope.row.isWarning ? 'warn' : ''">{{ scope.row.isWarning ? '是' : '否' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" min-width="150" />
        <el-table-column label="操作" width="140" fixed="right">
          <template slot-scope="scope">
            <a @click="openWarning(scope.row)" v-hasPermi="['admin:stock:warning:update']">设置预警</a>
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

    <el-dialog title="设置预警库存" :visible.sync="warningDialogVisible" width="420px" :close-on-click-modal="false">
      <el-form ref="warningFormRef" :model="warningForm" label-width="90px" size="small">
        <el-form-item label="商品：" class="mb10">
          <span>{{ warningForm.productName }}</span>
        </el-form-item>
        <el-form-item label="规格：" class="mb10">
          <span>{{ warningForm.skuName || '无' }}</span>
        </el-form-item>
        <el-form-item label="预警库存：" prop="warningStock">
          <el-input-number v-model="warningForm.warningStock" :min="0" :max="999999" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button size="small" @click="warningDialogVisible = false">取消</el-button>
        <el-button type="primary" size="small" :loading="submitLoading" @click="submitWarning">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { stockListApi, stockWarningUpdateApi, stockInitApi } from '@/api/stock.js';

export default {
  data() {
    return {
      constants: this.$constants,
      listLoading: false,
      submitLoading: false,
      listData: { list: [], total: 0 },
      listPram: {
        keywords: null,
        warning: null,
        page: 1,
        limit: this.$constants.page.limit[0],
      },
      warningDialogVisible: false,
      warningForm: {
        productId: null,
        attrValueId: 0,
        productName: '',
        skuName: '',
        warningStock: 0,
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
      this.listPram.warning = null;
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
      stockListApi(this.listPram)
        .then((data) => {
          this.listData = data || { list: [], total: 0 };
        })
        .finally(() => {
          this.listLoading = false;
        });
    },
    openWarning(row) {
      this.warningForm = {
        productId: row.productId,
        attrValueId: row.attrValueId || 0,
        productName: row.productName,
        skuName: row.skuName,
        warningStock: row.warningStock || 0,
      };
      this.warningDialogVisible = true;
    },
    submitWarning() {
      this.submitLoading = true;
      stockWarningUpdateApi({
        productId: this.warningForm.productId,
        attrValueId: this.warningForm.attrValueId,
        warningStock: this.warningForm.warningStock,
      })
        .then(() => {
          this.$message.success('设置成功');
          this.warningDialogVisible = false;
          this.getList();
        })
        .finally(() => {
          this.submitLoading = false;
        });
    },
    handleInit() {
      this.$modalSure('初始化会将商品/规格库存同步到进销存库存（请谨慎操作）').then(() => {
        stockInitApi().then(() => {
          this.$message.success('初始化成功');
          this.getList();
        });
      });
    },
  },
};
</script>

<style scoped>
.warn {
  color: #f56c6c;
  font-weight: 600;
}
</style>

