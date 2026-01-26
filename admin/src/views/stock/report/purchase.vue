<template>
  <div class="divBox">
    <el-card class="box-card">
      <div class="padding-add">
        <el-form size="small" label-width="90px" inline>
          <el-form-item label="时间范围：">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              value-format="yyyy-MM-dd"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              unlink-panels
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="small" @click="getList">查询</el-button>
            <el-button size="small" @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <el-table v-loading="loading" :data="list" style="width: 100%" size="mini" border>
        <el-table-column prop="supplierId" label="供应商ID" width="90" />
        <el-table-column prop="supplierName" label="供应商" min-width="180" />
        <el-table-column prop="purchaseCount" label="采购单数" min-width="90" />
        <el-table-column prop="totalQuantity" label="采购数量" min-width="90" />
        <el-table-column prop="totalAmount" label="采购金额" min-width="110" />
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { stockReportPurchaseApi } from '@/api/stock.js';

export default {
  data() {
    return {
      loading: false,
      dateRange: null,
      list: [],
    };
  },
  mounted() {
    this.getList();
  },
  methods: {
    reset() {
      this.dateRange = null;
      this.getList();
    },
    getList() {
      this.loading = true;
      const params = {};
      if (this.dateRange && this.dateRange.length === 2) {
        params.startTime = this.dateRange[0];
        params.endTime = this.dateRange[1];
      }
      stockReportPurchaseApi(params)
        .then((data) => {
          this.list = data || [];
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
};
</script>

