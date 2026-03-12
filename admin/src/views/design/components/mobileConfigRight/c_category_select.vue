<template>
  <!-- 分类宫格选择器 -->
  <div class="c_category_select borderPadding" v-if="configData">
    <div class="title">{{ configData.title }}</div>
    <div class="select-box">
      <el-select
        v-model="configData.categoryIds"
        multiple
        filterable
        placeholder="请选择要展示的二级分类"
        style="width: 100%"
        @change="onCategoryChange"
      >
        <el-option-group
          v-for="group in categoryTree"
          :key="group.id"
          :label="group.name"
        >
          <el-option
            v-for="item in group.child"
            :key="item.id"
            :label="item.name"
            :value="item.id"
            :disabled="!item.status"
          >
          </el-option>
        </el-option-group>
      </el-select>
    </div>
    <div class="tips" v-if="configData.categoryIds && configData.categoryIds.length">
      已选 {{ configData.categoryIds.length }} 个分类
    </div>
    <!-- 已选分类排序列表 -->
    <div class="selected-list" v-if="configData.categoryIds && configData.categoryIds.length">
      <div class="sub-title">拖拽调整显示顺序</div>
      <draggable class="dragArea" :list="configData.categoryIds" handle=".drag-handle">
        <div class="selected-item" v-for="(id, index) in configData.categoryIds" :key="id">
          <span class="drag-handle">
            <span class="iconfont icontuozhuaitubiao" style="font-size: 20px; color: #dddddd; cursor: move;"></span>
          </span>
          <span class="cat-name">{{ getCategoryName(id) }}</span>
          <i class="el-icon-close delete-btn" @click="removeCategory(index)"></i>
        </div>
      </draggable>
    </div>
  </div>
</template>

<script>
import vuedraggable from 'vuedraggable';
import { mapGetters } from 'vuex';
export default {
  name: 'c_category_select',
  props: {
    configObj: {
      type: Object,
    },
    configNme: {
      type: String,
    },
    index: {
      type: null,
    },
  },
  components: {
    draggable: vuedraggable,
  },
  data() {
    return {
      configData: {},
      categoryMap: {},
    };
  },
  computed: {
    ...mapGetters(['adminProductClassify']),
    // 过滤出有子分类的一级分类，构建分组数据
    categoryTree() {
      if (!this.adminProductClassify) return [];
      return this.adminProductClassify.filter((item) => item.child && item.child.length > 0);
    },
  },
  mounted() {
    // 确保分类数据已加载
    if (!localStorage.getItem('adminProductClassify')) {
      this.$store.dispatch('product/getAdminProductClassify');
    }
    this.$nextTick(() => {
      this.configData = this.configObj[this.configNme];
      this.buildCategoryMap();
    });
  },
  watch: {
    configObj: {
      handler(nVal) {
        this.configData = nVal[this.configNme];
      },
      deep: true,
    },
    adminProductClassify: {
      handler() {
        this.buildCategoryMap();
      },
      deep: true,
    },
  },
  methods: {
    // 构建 id -> name 映射，用于显示已选分类名称
    buildCategoryMap() {
      let map = {};
      if (this.adminProductClassify) {
        this.adminProductClassify.forEach((parent) => {
          if (parent.child) {
            parent.child.forEach((child) => {
              map[child.id] = child.name;
            });
          }
        });
      }
      this.categoryMap = map;
    },
    getCategoryName(id) {
      return this.categoryMap[id] || '未知分类';
    },
    onCategoryChange() {
      this.$emit('getConfig', { name: 'categorySelect', values: this.configData.categoryIds });
    },
    removeCategory(index) {
      this.configData.categoryIds.splice(index, 1);
      this.onCategoryChange();
    },
  },
};
</script>

<style scoped lang="scss">
.c_category_select {
  padding-bottom: 20px;

  .title {
    font-size: 12px;
    color: #bbbbbb;
  }

  .select-box {
    margin-top: 12px;
  }

  .tips {
    margin-top: 8px;
    font-size: 12px;
    color: #999;
  }

  .selected-list {
    margin-top: 12px;

    .sub-title {
      font-size: 12px;
      color: #bbbbbb;
      margin-bottom: 8px;
    }

    .selected-item {
      display: flex;
      align-items: center;
      padding: 8px 10px;
      background: #f9f9f9;
      border-radius: 3px;
      margin-bottom: 6px;

      .drag-handle {
        margin-right: 8px;
      }

      .cat-name {
        flex: 1;
        font-size: 13px;
        color: #333;
      }

      .delete-btn {
        color: #999;
        cursor: pointer;
        font-size: 16px;

        &:hover {
          color: #E93323;
        }
      }
    }
  }
}
</style>
