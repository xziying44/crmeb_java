<template>
  <div class="divBox">
    <el-card :bordered="false" shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form size="small" label-width="90px">
          <el-form-item label="商品ID：">
            <el-input v-model="listPram.productId" placeholder="请输入商品ID" class="selWidth" clearable />
          </el-form-item>
          <el-form-item label="类型：">
            <el-select v-model="listPram.type" placeholder="全部" clearable class="selWidth">
              <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
            </el-select>
          </el-form-item>
          <div class="ml30 mb20">
            <el-button type="primary" size="small" @click="handleSearch">搜索</el-button>
            <el-button size="small" @click="handleReset">重置</el-button>
          </div>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <el-table v-loading="listLoading" :data="listData.list" style="width: 100%" size="mini" class="table">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="productId" label="商品ID" width="90" />
        <el-table-column prop="productName" label="商品名称" min-width="200" />
        <el-table-column prop="skuName" label="规格" min-width="180" />
        <el-table-column prop="typeName" label="类型" width="90" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="beforeStock" label="变动前" width="80" />
        <el-table-column prop="afterStock" label="变动后" width="80" />
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column prop="remark" label="备注" min-width="200" />
        <el-table-column prop="createTime" label="创建时间" min-width="150" />
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
  </div>
</template>

<script>
import { stockLogListApi } from '@/api/stock.js';

export default {
  data() {
    return {
      constants: this.$constants,
      listLoading: false,
      listData: { list: [], total: 0 },
      listPram: {
        productId: null,
        type: null,
        page: 1,
        limit: this.$constants.page.limit[0],
      },
      typeOptions: [
        { label: '采购入库', value: 1 },
        { label: '退货入库', value: 2 },
        { label: '盘盈入库', value: 3 },
        { label: '其他入库', value: 4 },
        { label: '销售出库', value: 5 },
        { label: '报损出库', value: 6 },
        { label: '盘亏出库', value: 7 },
        { label: '其他出库', value: 8 },
      ],
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
      this.listPram.productId = null;
      this.listPram.type = null;
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
      const params = Object.assign({}, this.listPram);
      if (params.productId === '') params.productId = null;
      stockLogListApi(params)
        .then((data) => {
          this.listData = data || { list: [], total: 0 };
        })
        .finally(() => {
          this.listLoading = false;
        });
    },
  },
};
</script>

