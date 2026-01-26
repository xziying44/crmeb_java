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
        <el-table-column prop="checkNo" label="盘点单号" min-width="220" />
        <el-table-column prop="productName" label="商品" min-width="200" />
        <el-table-column prop="skuName" label="规格" min-width="160" />
        <el-table-column prop="systemStock" label="系统库存" width="90" />
        <el-table-column prop="actualStock" label="实际库存" width="90" />
        <el-table-column prop="diffQuantity" label="差异" width="80" />
        <el-table-column prop="createTime" label="创建时间" min-width="150" />
        <el-table-column prop="finishTime" label="完成时间" min-width="150" />
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { stockReportCheckDiffApi } from '@/api/stock.js';

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
      stockReportCheckDiffApi(params)
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

