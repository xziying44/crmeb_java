<template>
  <div class="divBox">
    <el-card :bordered="false" shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form :inline="true" :model="tableFrom" class="demo-form-inline">
          <el-form-item label="活动名称">
            <el-input v-model="tableFrom.name" placeholder="请输入活动名称" class="selWidth" clearable />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="tableFrom.status" placeholder="请选择" class="filter-item selWidth mr30" clearable>
              <el-option label="关闭" :value="0" />
              <el-option label="开启" :value="1" />
            </el-select>
          </el-form-item>
          <el-form-item label="范围">
            <el-select v-model="tableFrom.scopeType" placeholder="请选择" class="filter-item selWidth mr30" clearable>
              <el-option label="全场" :value="1" />
              <el-option label="品类" :value="2" />
              <el-option label="指定商品" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="small" @click="seachList">搜索</el-button>
            <el-button size="small" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <div slot="header" class="clearfix">
        <el-button type="primary" v-hasPermi="['admin:promotion:full-reduction:save']" @click="handleAdd">
          添加满减活动
        </el-button>
      </div>

      <el-table v-loading="listLoading" :data="tableData.data" style="width: 100%" size="mini">
        <el-table-column prop="id" label="ID" min-width="60" />
        <el-table-column prop="name" label="活动名称" min-width="180" />
        <el-table-column prop="scopeTypeName" label="范围" min-width="100" />
        <el-table-column label="时间范围" min-width="260">
          <template slot-scope="{ row }">
            <span>{{ row.startTime }} - {{ row.endTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="允许叠加优惠券" min-width="120">
          <template slot-scope="{ row }">
            <span>{{ row.allowCoupon ? '允许' : '不允许' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="是否开启" min-width="120">
          <template slot-scope="scope">
            <el-switch
              v-if="checkPermi(['admin:promotion:full-reduction:status'])"
              v-model="scope.row.status"
              :active-value="true"
              :inactive-value="false"
              active-text="开启"
              inactive-text="关闭"
              @click.native="onchangeIsShow(scope.row)"
            />
            <span v-else>{{ scope.row.status ? '开启' : '关闭' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="scope">
            <a v-hasPermi="['admin:promotion:full-reduction:info']" @click="handleEdit(scope.row)">编辑</a>
            <el-divider direction="vertical"></el-divider>
            <a v-hasPermi="['admin:promotion:full-reduction:delete']" @click="handleDelete(scope.row)">删除</a>
          </template>
        </el-table-column>
      </el-table>

      <div class="block">
        <el-pagination
          :page-sizes="[20, 40, 60, 80]"
          :page-size="tableFrom.limit"
          :current-page="tableFrom.page"
          layout="total, sizes, prev, pager, next, jumper"
          :total="tableData.total"
          @size-change="handleSizeChange"
          @current-change="pageChange"
          background
        />
      </div>
    </el-card>

    <FullReductionEdit
      ref="editRef"
      :visible.sync="editVisible"
      :edit-id="editId"
      @success="handleEditSuccess"
    />
  </div>
</template>

<script>
import { checkPermi } from '@/utils/permission';
import {
  fullReductionListApi,
  fullReductionDeleteApi,
  fullReductionUpdateStatusApi,
} from '@/api/promotion';
import FullReductionEdit from './components/edit.vue';

export default {
  name: 'FullReductionList',
  components: { FullReductionEdit },
  data() {
    return {
      listLoading: false,
      tableData: {
        data: [],
        total: 0,
      },
      tableFrom: {
        page: 1,
        limit: 20,
        name: '',
        status: undefined,
        scopeType: undefined,
      },
      editVisible: false,
      editId: null,
    };
  },
  created() {
    this.getList();
  },
  methods: {
    checkPermi,
    seachList() {
      this.tableFrom.page = 1;
      this.getList();
    },
    handleReset() {
      this.tableFrom = {
        page: 1,
        limit: 20,
        name: '',
        status: undefined,
        scopeType: undefined,
      };
      this.getList();
    },
    getList() {
      this.listLoading = true;
      fullReductionListApi(this.tableFrom)
        .then((res) => {
          this.tableData.data = res.list || [];
          this.tableData.total = res.total || 0;
          this.listLoading = false;
        })
        .catch(() => {
          this.listLoading = false;
        });
    },
    pageChange(page) {
      this.tableFrom.page = page;
      this.getList();
    },
    handleSizeChange(val) {
      this.tableFrom.limit = val;
      this.getList();
    },
    handleAdd() {
      this.editId = null;
      this.editVisible = true;
    },
    handleEdit(row) {
      this.editId = row.id;
      this.editVisible = true;
    },
    handleEditSuccess() {
      this.editVisible = false;
      this.getList();
    },
    onchangeIsShow(row) {
      fullReductionUpdateStatusApi(row.id, row.status)
        .then(() => {
          this.$message.success('修改成功');
          this.getList();
        })
        .catch(() => {
          row.status = !row.status;
        });
    },
    handleDelete(row) {
      this.$modalSure('删除当前数据?').then(() => {
        fullReductionDeleteApi(row.id).then(() => {
          this.$message.success('删除成功');
          this.tableFrom.page = 1;
          this.getList();
        });
      });
    },
  },
};
</script>

