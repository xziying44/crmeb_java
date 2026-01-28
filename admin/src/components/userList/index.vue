<template>
  <div class="divBox">
    <el-card class="box-card">
      <div slot="header" class="clearfix mt5">
        <el-form inline>
          <el-form-item label="用户名称：">
            <el-input v-model="tableFrom.keywords" placeholder="请输入用户名称" class="selWidth"> </el-input>
            <el-button class="ml30" type="primary" @click="search">搜索</el-button>
          </el-form-item>
        </el-form>
      </div>
      <el-table v-loading="loading" :data="tableData.data" width="800px" size="small">
        <!-- 多选模式 -->
        <el-table-column key="multi" v-if="handleNum === 'many'" width="55">
          <template slot="header">
            <el-checkbox
              :value="isChecked && checkedPage.indexOf(tableFrom.page) > -1"
              @change="changeType"
            />
          </template>
          <template slot-scope="scope">
            <el-checkbox
              :value="checkedIds.indexOf(scope.row.uid) > -1"
              @change="(v) => changeOne(v, scope.row)"
            />
          </template>
        </el-table-column>
        <!-- 单选模式 -->
        <el-table-column key="single" v-else label="" width="40">
          <template slot-scope="scope">
            <el-radio
              v-model="templateRadio"
              :label="scope.row.uid"
              @change.native="getTemplateRow(scope.$index, scope.row)"
            >&nbsp;</el-radio>
          </template>
        </el-table-column>
        <el-table-column prop="uid" label="ID" min-width="60" />
        <el-table-column prop="nickname" label="微信用户名称" min-width="130" />
        <el-table-column label="用户头像" min-width="80">
          <template slot-scope="scope">
            <div class="demo-image__preview">
              <el-image class="tabImage" :src="scope.row.avatar" :preview-src-list="[scope.row.avatar]" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="性别" min-width="80">
          <template slot-scope="scope">
            <span>{{ scope.row.sex | saxFilter }}</span>
          </template>
        </el-table-column>
        <el-table-column label="地区" min-width="130">
          <template slot-scope="scope">
            <span>{{ scope.row.addres }}</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="acea-row row-between">
        <el-pagination
          :page-sizes="[10, 20, 30, 40]"
          :page-size="tableFrom.limit"
          :current-page="tableFrom.page"
          layout=" sizes, prev, pager, next, jumper"
          :total="tableData.total"
          @size-change="handleSizeChange"
          @current-change="pageChange"
        />
        <div class="mt30">
          <el-button @click="closeDialog">取消</el-button>
          <el-button v-if="handleNum === 'many'" type="primary" @click="ok">确定({{ checkedIds.length }})</el-button>
          <el-button v-else type="primary" @click="closeDialog">确定</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { userListApi } from '@/api/user';
export default {
  name: 'UserList',
  props: {
    handleNum: {
      type: String,
      default: '',
    },
  },
  filters: {
    saxFilter(status) {
      const statusMap = {
        0: '未知',
        1: '男',
        2: '女',
      };
      return statusMap[status];
    },
    statusFilter(status) {
      const statusMap = {
        wechat: '微信用户',
        routine: '小程序用户',
      };
      return statusMap[status];
    },
  },
  data() {
    return {
      templateRadio: 0,
      loading: false,
      tableData: {
        data: [],
        total: 0,
      },
      tableFrom: {
        page: 1,
        limit: 10,
        keywords: '',
      },
      // 多选相关
      checkedIds: [],
      checkBox: [],
      checkedPage: [],
      isChecked: false,
    };
  },
  mounted() {
    this.getList();
  },
  methods: {
    closeDialog() {
      this.$emit('closeDialog');
    },
    getTemplateRow(idx, row) {
      this.$emit('getTemplateRow', row);
    },
    // 全选/取消全选
    changeType(v) {
      this.isChecked = v;
      const index = this.checkedPage.indexOf(this.tableFrom.page);
      if (v) {
        if (index === -1) this.checkedPage.push(this.tableFrom.page);
      } else {
        if (index > -1) this.checkedPage.splice(index, 1);
      }
      this.syncCheckedId(v);
    },
    // 单个选中/取消
    changeOne(v, user) {
      if (v) {
        const index = this.checkedIds.indexOf(user.uid);
        if (index === -1) {
          this.checkedIds.push(user.uid);
          this.checkBox.push(user);
        }
      } else {
        const index = this.checkedIds.indexOf(user.uid);
        if (index > -1) {
          this.checkedIds.splice(index, 1);
          this.checkBox.splice(index, 1);
        }
      }
    },
    // 同步当前页选中状态
    syncCheckedId(checked) {
      this.tableData.data.forEach((item) => {
        const index = this.checkedIds.indexOf(item.uid);
        if (checked) {
          if (index === -1) {
            this.checkedIds.push(item.uid);
            this.checkBox.push(item);
          }
        } else {
          if (index > -1) {
            this.checkedIds.splice(index, 1);
            this.checkBox.splice(index, 1);
          }
        }
      });
    },
    // 多选确认
    ok() {
      this.$emit('getUsers', this.checkBox);
    },
    // 列表
    getList() {
      this.loading = true;
      userListApi(this.tableFrom)
        .then((res) => {
          this.tableData.data = res.list;
          this.tableData.total = res.total;
          this.loading = false;
        })
        .catch((res) => {
          this.$message.error(res.message);
          this.loading = false;
        });
    },
    search() {
      this.loading = true;
      userListApi({ keywords: this.tableFrom.keywords })
        .then((res) => {
          this.tableData.data = res.list;
          this.tableData.total = res.total;
          this.loading = false;
        })
        .catch((res) => {
          this.$message.error(res.message);
          this.loading = false;
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
  },
};
</script>

<style lang="scss" scoped>
::v-deep.el-table .cell {
  padding-left: 20px !important;
}
.mt5 {
  margin-top: 5px;
}
::v-deep .el-card__body {
  padding: 30px 24px !important;
}
::v-deep .el-form-item {
  margin-bottom: 0px !important;
}
::v-deep .el-card__body {
  padding: 20px 24px !important;
}
</style>
