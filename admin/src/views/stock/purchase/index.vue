<template>
  <div class="divBox">
    <el-card :bordered="false" shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form size="small" label-width="85px">
          <el-form-item label="采购单号：">
            <el-input v-model="listPram.purchaseNo" placeholder="请输入采购单号" class="selWidth" clearable />
          </el-form-item>
          <el-form-item label="供应商：">
            <el-select v-model="listPram.supplierId" placeholder="全部" clearable class="selWidth">
              <el-option v-for="s in supplierOptions" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态：">
            <el-select v-model="listPram.status" placeholder="全部" clearable class="selWidth">
              <el-option label="待入库" :value="0" />
              <el-option label="部分入库" :value="1" />
              <el-option label="已入库" :value="2" />
              <el-option label="已取消" :value="3" />
            </el-select>
          </el-form-item>
          <div class="ml30">
            <el-button type="primary" size="small" @click="handleSearch">搜索</el-button>
            <el-button size="small" @click="handleReset">重置</el-button>
            <el-button type="success" size="small" @click="openCreate" v-hasPermi="['admin:stock:purchase:add']"
              >创建采购单</el-button
            >
          </div>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <el-table v-loading="listLoading" :data="listData.list" style="width: 100%" size="mini" class="table">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="purchaseNo" label="采购单号" min-width="220" />
        <el-table-column prop="supplierName" label="供应商" min-width="160" />
        <el-table-column prop="totalQuantity" label="总数量" width="90" />
        <el-table-column prop="totalAmount" label="总金额" width="110" />
        <el-table-column prop="statusName" label="状态" width="90" />
        <el-table-column prop="operatorName" label="创建人" width="100" />
        <el-table-column prop="createTime" label="创建时间" min-width="150" />
        <el-table-column label="操作" width="220" fixed="right">
          <template slot-scope="scope">
            <a @click="openDetail(scope.row)" v-hasPermi="['admin:stock:purchase:detail']">详情</a>
            <el-divider direction="vertical" />
            <a
              @click="openInStock(scope.row)"
              v-hasPermi="['admin:stock:purchase:inStock']"
              v-if="scope.row.status !== 2 && scope.row.status !== 3"
              >入库</a
            >
            <el-divider direction="vertical" v-if="scope.row.status !== 2 && scope.row.status !== 3" />
            <a
              @click="handleCancel(scope.row)"
              v-hasPermi="['admin:stock:purchase:cancel']"
              v-if="scope.row.status !== 2 && scope.row.status !== 3"
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

    <!-- 创建采购单 -->
    <el-dialog title="创建采购单" :visible.sync="createDialogVisible" width="860px" :close-on-click-modal="false">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px" size="small">
        <el-form-item label="供应商" prop="supplierId">
          <el-select v-model="createForm.supplierId" placeholder="请选择供应商" class="selWidth">
            <el-option v-for="s in supplierOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" maxlength="500" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>

        <el-form-item label="采购明细">
          <el-button size="mini" type="primary" @click="addItem">新增明细</el-button>
          <el-table :data="createForm.items" size="mini" class="mt10" border>
            <el-table-column label="商品" min-width="240">
              <template slot-scope="scope">
                <div class="acea-row row-middle">
                  <div class="mini-img mr10" @click="changeGood(scope.$index)">
                    <img v-if="scope.row.productImage" :src="scope.row.productImage" />
                    <i v-else class="el-icon-camera" />
                  </div>
                  <div class="line2" style="flex: 1">
                    <div>{{ scope.row.productName || '点击选择商品' }}</div>
                    <div class="gray" v-if="scope.row.productId">ID: {{ scope.row.productId }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="规格" min-width="200">
              <template slot-scope="scope">
                <el-select
                  v-model="scope.row.attrValueId"
                  placeholder="无规格可不选"
                  clearable
                  size="mini"
                  style="width: 180px"
                  :disabled="!scope.row.skuOptions || !scope.row.skuOptions.length"
                >
                  <el-option v-for="s in scope.row.skuOptions" :key="s.value" :label="s.label" :value="s.value" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="110">
              <template slot-scope="scope">
                <el-input-number v-model="scope.row.quantity" :min="1" :max="999999" size="mini" />
              </template>
            </el-table-column>
            <el-table-column label="单价" width="140">
              <template slot-scope="scope">
                <el-input-number v-model="scope.row.price" :min="0" :max="999999" :precision="2" size="mini" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template slot-scope="scope">
                <a @click="removeItem(scope.$index)">删除</a>
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button size="small" @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" size="small" :loading="submitLoading" @click="submitCreate">确定</el-button>
      </div>
    </el-dialog>

    <!-- 采购入库 -->
    <el-dialog title="采购入库" :visible.sync="inStockDialogVisible" width="860px" :close-on-click-modal="false">
      <div v-loading="detailLoading">
        <div class="mb10">采购单号：{{ detail.purchaseNo }}</div>
        <el-table :data="inStockItems" size="mini" border>
          <el-table-column prop="productName" label="商品" min-width="220" />
          <el-table-column prop="skuName" label="规格" min-width="180" />
          <el-table-column prop="quantity" label="采购数量" width="90" />
          <el-table-column prop="inQuantity" label="已入库" width="90" />
          <el-table-column label="本次入库" width="140">
            <template slot-scope="scope">
              <el-input-number v-model="scope.row.inQty" :min="0" :max="scope.row.remain" size="mini" />
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button size="small" @click="inStockDialogVisible = false">取消</el-button>
        <el-button type="primary" size="small" :loading="submitLoading" @click="submitInStock">确定</el-button>
      </div>
    </el-dialog>

    <!-- 详情 -->
    <el-dialog title="采购单详情" :visible.sync="detailDialogVisible" width="860px" :close-on-click-modal="false">
      <div v-loading="detailLoading">
        <div class="mb10">采购单号：{{ detail.purchaseNo }}</div>
        <div class="mb10">供应商：{{ detail.supplierName }}</div>
        <div class="mb10">状态：{{ detail.statusName }}</div>
        <el-table :data="detail.items || []" size="mini" border>
          <el-table-column prop="productName" label="商品" min-width="220" />
          <el-table-column prop="skuName" label="规格" min-width="180" />
          <el-table-column prop="quantity" label="采购数量" width="90" />
          <el-table-column prop="inQuantity" label="已入库" width="90" />
          <el-table-column prop="price" label="单价" width="110" />
          <el-table-column prop="amount" label="小计" width="110" />
        </el-table>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button size="small" @click="detailDialogVisible = false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  purchaseListApi,
  purchaseAddApi,
  purchaseCancelApi,
  purchaseDetailApi,
  purchaseInStockApi,
  supplierEnabledListApi,
} from '@/api/stock.js';
import { productDetailApi } from '@/api/store.js';

export default {
  data() {
    return {
      constants: this.$constants,
      listLoading: false,
      detailLoading: false,
      submitLoading: false,
      listData: { list: [], total: 0 },
      listPram: {
        purchaseNo: null,
        supplierId: null,
        status: null,
        page: 1,
        limit: this.$constants.page.limit[0],
      },
      supplierOptions: [],
      createDialogVisible: false,
      createForm: {
        supplierId: null,
        remark: '',
        items: [],
      },
      createRules: {
        supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
      },
      inStockDialogVisible: false,
      inStockItems: [],
      detailDialogVisible: false,
      detail: {},
    };
  },
  mounted() {
    this.loadSuppliers();
    this.getList();
  },
  methods: {
    loadSuppliers() {
      supplierEnabledListApi().then((list) => {
        this.supplierOptions = list || [];
      });
    },
    handleSearch() {
      this.listPram.page = 1;
      this.getList();
    },
    handleReset() {
      this.listPram.purchaseNo = null;
      this.listPram.supplierId = null;
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
      purchaseListApi(this.listPram)
        .then((data) => {
          this.listData = data || { list: [], total: 0 };
        })
        .finally(() => {
          this.listLoading = false;
        });
    },
    openCreate() {
      this.createForm = { supplierId: null, remark: '', items: [] };
      this.addItem();
      this.createDialogVisible = true;
      this.$nextTick(() => {
        this.$refs.createFormRef && this.$refs.createFormRef.clearValidate();
      });
    },
    addItem() {
      this.createForm.items.push({
        productId: null,
        productName: '',
        productImage: '',
        skuOptions: [],
        attrValueId: null,
        quantity: 1,
        price: 0,
      });
    },
    removeItem(index) {
      this.createForm.items.splice(index, 1);
    },
    changeGood(index) {
      const _this = this;
      this.$modalGoodList(function (row) {
        const item = _this.createForm.items[index];
        item.productId = row.id;
        item.productName = row.storeName || row.store_name || row.name || '';
        item.productImage = row.image || '';
        item.attrValueId = null;
        item.skuOptions = [];
        _this.loadSkuOptions(item);
      });
    },
    loadSkuOptions(item) {
      if (!item || !item.productId) return;
      productDetailApi(item.productId).then((info) => {
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
        item.skuOptions = options;
        if (!info || !info.specType) {
          if (options.length === 1) item.attrValueId = options[0].value;
        }
      });
    },
    submitCreate() {
      this.$refs.createFormRef.validate((valid) => {
        if (!valid) return;
        const items = this.createForm.items || [];
        if (!items.length) return this.$message.warning('请至少添加一条采购明细');
        for (const it of items) {
          if (!it.productId) return this.$message.warning('请先选择商品');
          if (!it.quantity || it.quantity <= 0) return this.$message.warning('采购数量必须大于0');
          if (it.price === null || it.price === undefined || it.price < 0) return this.$message.warning('采购单价不能小于0');
        }
        this.submitLoading = true;
        purchaseAddApi({
          supplierId: this.createForm.supplierId,
          remark: this.createForm.remark,
          items: items.map((it) => ({
            productId: it.productId,
            attrValueId: it.attrValueId || 0,
            quantity: it.quantity,
            price: it.price,
          })),
        })
          .then(() => {
            this.$message.success('创建成功');
            this.createDialogVisible = false;
            this.getList();
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },
    handleCancel(row) {
      this.$modalSure('取消该采购单').then(() => {
        purchaseCancelApi(row.id).then(() => {
          this.$message.success('取消成功');
          this.getList();
        });
      });
    },
    openDetail(row) {
      this.detailDialogVisible = true;
      this.loadDetail(row.id);
    },
    openInStock(row) {
      this.inStockDialogVisible = true;
      this.loadDetail(row.id, true);
    },
    loadDetail(id, forInStock = false) {
      this.detailLoading = true;
      purchaseDetailApi(id)
        .then((data) => {
          this.detail = data || {};
          if (forInStock) {
            const items = (data && data.items) || [];
            this.inStockItems = items.map((it) => {
              const remain = (it.quantity || 0) - (it.inQuantity || 0);
              return Object.assign({}, it, { remain: remain < 0 ? 0 : remain, inQty: remain < 0 ? 0 : remain });
            });
          }
        })
        .finally(() => {
          this.detailLoading = false;
        });
    },
    submitInStock() {
      const items = (this.inStockItems || [])
        .filter((it) => it.inQty && it.inQty > 0)
        .map((it) => ({ itemId: it.id, quantity: it.inQty }));
      if (!items.length) return this.$message.warning('请输入本次入库数量');
      this.submitLoading = true;
      purchaseInStockApi({ purchaseId: this.detail.id, items })
        .then(() => {
          this.$message.success('入库成功');
          this.inStockDialogVisible = false;
          this.getList();
        })
        .finally(() => {
          this.submitLoading = false;
        });
    },
  },
};
</script>

<style scoped>
.mini-img {
  width: 40px;
  height: 40px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
}
.mini-img img {
  width: 40px;
  height: 40px;
  object-fit: cover;
}
.gray {
  color: #909399;
  font-size: 12px;
}
.line2 {
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
</style>

