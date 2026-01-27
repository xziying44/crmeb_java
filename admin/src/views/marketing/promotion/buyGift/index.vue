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
          <el-form-item label="类型">
            <el-select v-model="tableFrom.giftType" placeholder="请选择" class="filter-item selWidth mr30" clearable>
              <el-option label="同商品买N送M" :value="1" />
              <el-option label="跨商品买A送B" :value="2" />
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
        <el-button type="primary" v-hasPermi="['admin:promotion:buy-gift:save']" @click="handleAdd">
          添加买赠活动
        </el-button>
      </div>

      <el-table v-loading="listLoading" :data="tableData.data" style="width: 100%" size="mini">
        <el-table-column prop="id" label="ID" min-width="60" />
        <el-table-column prop="name" label="活动名称" min-width="180" />
        <el-table-column prop="giftTypeName" label="类型" min-width="140" />
        <el-table-column prop="limitTypeName" label="限制" min-width="140" />
        <el-table-column label="时间范围" min-width="260">
          <template slot-scope="{ row }">
            <span>{{ row.startTime }} - {{ row.endTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="是否开启" min-width="120">
          <template slot-scope="scope">
            <el-switch
              v-if="checkPermi(['admin:promotion:buy-gift:status'])"
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
            <a v-hasPermi="['admin:promotion:buy-gift:info']" @click="handleEdit(scope.row)">编辑</a>
            <el-divider direction="vertical"></el-divider>
            <a v-hasPermi="['admin:promotion:buy-gift:delete']" @click="handleDelete(scope.row)">删除</a>
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

    <el-dialog
      :title="editId ? '编辑买赠活动' : '新增买赠活动'"
      :visible.sync="editVisible"
      width="900px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="活动名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入活动名称" />
        </el-form-item>
        <el-form-item label="类型" prop="giftType">
          <el-radio-group v-model="form.giftType">
            <el-radio :label="1">同商品买N送M</el-radio>
            <el-radio :label="2">跨商品买A送B</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="时间范围" required>
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            placeholder="开始时间"
            style="width: 260px"
          />
          <span style="margin: 0 10px">-</span>
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            placeholder="结束时间"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item label="限制类型" prop="limitType">
          <el-select v-model="form.limitType" placeholder="请选择" style="width: 260px">
            <el-option label="不限" :value="0" />
            <el-option label="总次数限制" :value="1" />
            <el-option label="每日次数限制" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.limitType !== 0" label="限制次数" prop="limitNum">
          <el-input-number v-model="form.limitNum" :min="1" controls-position="right" />
        </el-form-item>

        <el-form-item label="购买商品配置" required>
          <el-input
            v-model="form.buyProductsJson"
            type="textarea"
            :rows="5"
            placeholder='请输入JSON数组，如：[{"productId":1,"attrValueId":0,"buyNum":2}]'
          />
        </el-form-item>
        <el-form-item label="赠品配置" required>
          <el-input
            v-model="form.giftProductsJson"
            type="textarea"
            :rows="5"
            placeholder='请输入JSON数组，如：[{"productId":2,"attrValueId":0,"giftNum":1}]'
          />
        </el-form-item>
      </el-form>

      <span slot="footer" class="dialog-footer">
        <el-button @click="editVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保 存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { checkPermi } from '@/utils/permission';
import {
  buyGiftListApi,
  buyGiftDeleteApi,
  buyGiftUpdateStatusApi,
  buyGiftDetailApi,
  buyGiftSaveApi,
} from '@/api/promotion';

export default {
  name: 'BuyGiftList',
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
        giftType: undefined,
      },
      editVisible: false,
      editId: null,
      saving: false,
      form: this.getDefaultForm(),
      rules: {
        name: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
        giftType: [{ required: true, message: '请选择类型', trigger: 'change' }],
        limitType: [{ required: true, message: '请选择限制类型', trigger: 'change' }],
        limitNum: [
          {
            validator: (rule, value, callback) => {
              if (this.form.limitType === 0) return callback();
              if (!value || value <= 0) return callback(new Error('请输入限制次数'));
              return callback();
            },
            trigger: 'change',
          },
        ],
      },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    checkPermi,
    getDefaultForm() {
      return {
        id: null,
        name: '',
        giftType: 1,
        startTime: '',
        endTime: '',
        limitType: 0,
        limitNum: 0,
        buyProductsJson: '[{"productId":1,"attrValueId":0,"buyNum":1}]',
        giftProductsJson: '[{"productId":1,"attrValueId":0,"giftNum":1}]',
      };
    },
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
        giftType: undefined,
      };
      this.getList();
    },
    getList() {
      this.listLoading = true;
      buyGiftListApi(this.tableFrom)
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
    onchangeIsShow(row) {
      buyGiftUpdateStatusApi(row.id, row.status)
        .then(() => {
          this.$message.success('修改成功');
          this.getList();
        })
        .catch(() => {
          row.status = !row.status;
        });
    },
    handleAdd() {
      this.editId = null;
      this.form = this.getDefaultForm();
      this.editVisible = true;
    },
    handleEdit(row) {
      this.editId = row.id;
      this.form = this.getDefaultForm();
      this.editVisible = true;
      buyGiftDetailApi(row.id).then((res) => {
        this.form.id = res.id;
        this.form.name = res.name;
        this.form.giftType = res.giftType;
        this.form.startTime = res.startTime;
        this.form.endTime = res.endTime;
        this.form.limitType = res.limitType;
        this.form.limitNum = res.limitNum;
        this.form.buyProductsJson = JSON.stringify(res.buyProducts || []);
        this.form.giftProductsJson = JSON.stringify(res.giftProducts || []);
      });
    },
    handleDelete(row) {
      this.$modalSure('删除当前数据?').then(() => {
        buyGiftDeleteApi(row.id).then(() => {
          this.$message.success('删除成功');
          this.tableFrom.page = 1;
          this.getList();
        });
      });
    },
    parseJsonArray(text) {
      const v = JSON.parse(text || '[]');
      return Array.isArray(v) ? v : [];
    },
    handleSubmit() {
      this.$refs.formRef.validate((valid) => {
        if (!valid) return;
        if (!this.form.startTime || !this.form.endTime) {
          this.$message.error('请选择时间范围');
          return;
        }
        let buyProducts = [];
        let giftProducts = [];
        try {
          buyProducts = this.parseJsonArray(this.form.buyProductsJson);
          giftProducts = this.parseJsonArray(this.form.giftProductsJson);
        } catch (e) {
          this.$message.error('商品配置JSON格式不正确');
          return;
        }
        if (!buyProducts.length) {
          this.$message.error('请配置购买商品');
          return;
        }
        if (!giftProducts.length) {
          this.$message.error('请配置赠品');
          return;
        }
        const payload = {
          id: this.form.id,
          name: this.form.name,
          giftType: this.form.giftType,
          startTime: this.form.startTime,
          endTime: this.form.endTime,
          limitType: this.form.limitType,
          limitNum: this.form.limitType === 0 ? 0 : this.form.limitNum,
          buyProducts,
          giftProducts,
        };
        this.saving = true;
        buyGiftSaveApi(payload)
          .then(() => {
            this.$message.success('保存成功');
            this.saving = false;
            this.editVisible = false;
            this.getList();
          })
          .catch(() => {
            this.saving = false;
          });
      });
    },
  },
};
</script>

