<template>
  <div class="divBox">
    <el-card class="box-card">
      <div class="flex-row">
        <el-button type="primary" size="small" @click="refresh">刷新</el-button>
        <span class="gray ml10">提示：阶段一后端统计/排行榜为占位实现，页面用于预留入口。</span>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <div class="title">业绩统计汇总</div>
      <pre class="json">{{ statisticsText }}</pre>
    </el-card>

    <el-card class="box-card mt14">
      <div class="title">业务员排行榜</div>
      <el-table :data="rankingList" size="mini" style="width: 100%">
        <el-table-column prop="id" label="业务员ID" width="100" />
        <el-table-column prop="realName" label="姓名" min-width="140" />
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column prop="customerCount" label="客户数" min-width="100" />
        <el-table-column prop="monthNewCustomerCount" label="本月新增客户" min-width="120" />
      </el-table>
      <el-empty v-if="!rankingList.length" description="暂无数据" />
    </el-card>
  </div>
</template>

<script>
import { salesmanStatisticsApi, salesmanRankingApi } from '@/api/salesman.js';

export default {
  data() {
    return {
      statistics: null,
      rankingList: [],
    };
  },
  computed: {
    statisticsText() {
      try {
        return JSON.stringify(this.statistics || {}, null, 2);
      } catch (e) {
        return '{}';
      }
    },
  },
  mounted() {
    this.refresh();
  },
  methods: {
    refresh() {
      salesmanStatisticsApi().then((data) => {
        this.statistics = data;
      });
      salesmanRankingApi(10).then((list) => {
        this.rankingList = list || [];
      });
    },
  },
};
</script>

<style scoped>
.title {
  font-weight: 600;
  margin-bottom: 10px;
}
.json {
  background: #f6f8fa;
  padding: 12px;
  border-radius: 4px;
  overflow: auto;
}
.gray {
  color: #909399;
}
.ml10 {
  margin-left: 10px;
}
.flex-row {
  display: flex;
  align-items: center;
}
</style>

