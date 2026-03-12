<template>
  <div class="divBox">
    <el-card :bordered="false" shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form size="small" :inline="true" @submit.native.prevent>
          <el-form-item label="横幅名称：">
            <el-input
              v-model="searchName"
              placeholder="请输入横幅名称"
              class="selWidth"
              clearable
              @keyup.enter.native="getList(1)"
            />
          </el-form-item>
          <el-form-item label="状态：">
            <el-select v-model="tableFrom.status" placeholder="全部" class="selWidth" clearable @change="getList(1)">
              <el-option label="上线" :value="1" />
              <el-option label="下线" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="getList(1)">搜索</el-button>
            <el-button @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
    <el-card class="box-card mt14" :body-style="{ padding: '20px' }" :bordered="false" shadow="never">
      <el-button type="primary" @click="handleAdd">新增横幅</el-button>
      <el-table :data="tableData.data" style="width: 100%" class="mt20" v-loading="listLoading">
        <el-table-column prop="id" label="ID" min-width="60" />
        <el-table-column label="横幅图片" min-width="120">
          <template slot-scope="scope">
            <el-image
              style="width: 80px; height: 30px"
              :src="scope.row.image"
              :preview-src-list="[scope.row.image]"
              fit="cover"
            />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="横幅名称" min-width="150" :show-overflow-tooltip="true" />
        <el-table-column label="活动类型" min-width="100">
          <template slot-scope="scope">
            <el-tag :type="activityTypeTag(scope.row.activityType)">
              {{ activityTypeLabel(scope.row.activityType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="排序" min-width="100">
          <template slot-scope="scope">
            <el-input-number
              v-model="scope.row.sort"
              :min="0"
              size="mini"
              controls-position="right"
              @change="handleSortChange(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="80">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '上线' : '下线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template slot-scope="scope">
            <el-button
              type="text"
              size="small"
              @click="handleStatusChange(scope.row)"
            >
              {{ scope.row.status === 1 ? '下线' : '上线' }}
            </el-button>
            <el-divider direction="vertical" />
            <el-button type="text" size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-button type="text" size="small" style="color: #f56c6c" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="block">
        <el-pagination
          background
          :page-sizes="[20, 40, 60, 80]"
          :page-size="tableFrom.limit"
          :current-page="tableFrom.page"
          layout="total, sizes, prev, pager, next, jumper"
          :total="tableData.total"
          @size-change="handleSizeChange"
          @current-change="pageChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="600px" @close="handleDialogClose">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="横幅名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入横幅名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="横幅图片" prop="image">
          <div class="upLoadPicBox" @click="modalPicTap">
            <div v-if="formData.image" class="pictrue">
              <img :src="formData.image" />
            </div>
            <div v-else class="upLoad">
              <i class="el-icon-camera cameraIconfont" />
            </div>
          </div>
          <div class="tips">建议尺寸 750×200px</div>
        </el-form-item>
        <el-form-item label="活动类型" prop="activityType">
          <el-select v-model="formData.activityType" placeholder="请选择活动类型">
            <el-option label="秒杀" :value="1" />
            <el-option label="砍价" :value="2" />
            <el-option label="拼团" :value="3" />
            <el-option label="买赠" :value="4" />
            <el-option label="满减" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="formData.sort" :min="0" controls-position="right" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  activityBannerListApi,
  activityBannerSaveApi,
  activityBannerUpdateApi,
  activityBannerDeleteApi,
  activityBannerStatusApi,
} from '@/api/activityBanner'

const activityTypeMap = {
  1: { label: '秒杀', tag: 'danger' },
  2: { label: '砍价', tag: 'warning' },
  3: { label: '拼团', tag: 'success' },
  4: { label: '买赠', tag: '' },
  5: { label: '满减', tag: 'info' },
}

export default {
  name: 'ActivityBannerList',
  data() {
    return {
      listLoading: false,
      tableData: { data: [], total: 0 },
      tableFrom: { page: 1, limit: 20, status: '', name: '' },
      searchName: '',
      dialogVisible: false,
      dialogTitle: '新增横幅',
      submitLoading: false,
      isEdit: false,
      formData: {
        id: null,
        name: '',
        image: '',
        activityType: null,
        sort: 0,
      },
      formRules: {
        name: [{ required: true, message: '请输入横幅名称', trigger: 'blur' }],
        image: [{ required: true, message: '请上传横幅图片', trigger: 'change' }],
        activityType: [{ required: true, message: '请选择活动类型', trigger: 'change' }],
      },
    }
  },
  mounted() {
    this.getList(1)
  },
  methods: {
    activityTypeLabel(type) {
      return activityTypeMap[type] ? activityTypeMap[type].label : '未知'
    },
    activityTypeTag(type) {
      return activityTypeMap[type] ? activityTypeMap[type].tag : ''
    },
    getList(num) {
      this.listLoading = true
      this.tableFrom.page = num || this.tableFrom.page
      this.tableFrom.name = this.searchName ? encodeURIComponent(this.searchName) : ''
      activityBannerListApi(this.tableFrom)
        .then((res) => {
          this.tableData.data = res.list
          this.tableData.total = res.total
          this.listLoading = false
        })
        .catch(() => {
          this.listLoading = false
        })
    },
    reset() {
      this.searchName = ''
      this.tableFrom = { page: 1, limit: 20, status: '', name: '' }
      this.getList(1)
    },
    pageChange(page) {
      this.tableFrom.page = page
      this.getList()
    },
    handleSizeChange(val) {
      this.tableFrom.limit = val
      this.getList(1)
    },
    handleAdd() {
      this.isEdit = false
      this.dialogTitle = '新增横幅'
      this.formData = { id: null, name: '', image: '', activityType: null, sort: 0 }
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.isEdit = true
      this.dialogTitle = '编辑横幅'
      this.formData = {
        id: row.id,
        name: row.name,
        image: row.image,
        activityType: row.activityType,
        sort: row.sort,
      }
      this.dialogVisible = true
    },
    handleDialogClose() {
      this.$refs.formRef && this.$refs.formRef.resetFields()
    },
    handleSubmit() {
      this.$refs.formRef.validate((valid) => {
        if (!valid) return
        this.submitLoading = true
        const api = this.isEdit ? activityBannerUpdateApi : activityBannerSaveApi
        api(this.formData)
          .then(() => {
            this.$message.success(this.isEdit ? '编辑成功' : '新增成功')
            this.dialogVisible = false
            this.getList()
          })
          .finally(() => {
            this.submitLoading = false
          })
      })
    },
    handleDelete(row) {
      this.$modalSure('删除后将无法恢复，请谨慎操作!').then(() => {
        activityBannerDeleteApi(row.id).then(() => {
          this.$message.success('删除成功')
          this.getList()
        })
      })
    },
    handleStatusChange(row) {
      const newStatus = row.status === 1 ? 0 : 1
      const text = newStatus === 1 ? '上线' : '下线'
      activityBannerStatusApi({ id: row.id, status: newStatus }).then(() => {
        this.$message.success(`${text}成功`)
        this.getList()
      })
    },
    handleSortChange(row) {
      activityBannerUpdateApi({ id: row.id, name: row.name, image: row.image, activityType: row.activityType, sort: row.sort }).then(() => {
        this.$message.success('排序已更新')
      })
    },
    // 选择图片
    modalPicTap() {
      this.$modalUpload(function(img) {
        this.formData.image = img[0]
      }.bind(this), 1)
    },
  },
}
</script>

<style scoped lang="scss">
.upLoadPicBox {
  display: inline-block;
  cursor: pointer;
  .pictrue {
    width: 120px;
    height: 50px;
    border: 1px dotted rgba(0, 0, 0, 0.1);
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
  .upLoad {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 120px;
    height: 50px;
    border: 1px dotted rgba(0, 0, 0, 0.1);
    .cameraIconfont {
      font-size: 24px;
      color: #c0c4cc;
    }
  }
}
.tips {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>
