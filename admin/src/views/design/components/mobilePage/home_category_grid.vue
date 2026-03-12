<template>
  <div v-if="configObj" :style="boxStyle">
    <div class="category-grid" :style="gridStyle">
      <template v-for="(item, index) in categoryList">
        <div class="grid-item" :key="index">
          <div class="img-box">
            <img :src="item.extra" alt="" v-if="item.extra" :style="contentStyle" />
            <div class="empty-box" v-else :style="contentStyle">
              <span class="iconfont-diy iconfont icontupian"></span>
            </div>
          </div>
          <p :style="titleColor">{{ item.name }}</p>
        </div>
      </template>
      <!-- 未选择分类时的占位提示 -->
      <template v-if="!categoryList.length">
        <div class="grid-item" v-for="n in 5" :key="'empty-'+n">
          <div class="img-box">
            <div class="empty-box" :style="contentStyle">
              <span class="iconfont-diy iconfont icontupian"></span>
            </div>
          </div>
          <p :style="titleColor">分类{{ n }}</p>
        </div>
      </template>
    </div>
  </div>
</template>

<script>
import { mapState } from 'vuex';
import { categroyByIds } from '@/api/categoryApi';
export default {
  name: 'home_category_grid',
  cname: '分类宫格',
  icon: 't-icon-zujian-shangpinfenlei',
  configName: 'c_home_category_grid',
  type: 0, // 0 基础组件
  defaultName: 'categoryGrid', // 移动端匹配名称
  props: {
    index: {
      type: null,
    },
    num: {
      type: null,
    },
  },
  computed: {
    ...mapState('mobildConfig', ['defaultArray']),
    boxStyle() {
      return [
        { 'border-radius': this.configObj.bgStyle.val ? this.configObj.bgStyle.val + 'px' : '0' },
        {
          background: `linear-gradient(${this.configObj.bgColor.color[0].item}, ${this.configObj.bgColor.color[1].item})`,
        },
        { margin: this.configObj.mbConfig.val + 'px' + ' ' + this.configObj.lrConfig.val + 'px' + ' ' + 0 },
        { padding: this.configObj.upConfig.val + 'px' + ' ' + 0 + ' ' + this.configObj.downConfig.val + 'px' },
      ];
    },
    gridStyle() {
      return [
        { 'grid-row-gap': this.configObj.contentConfig.val + 'px' },
        { 'grid-template-columns': 'repeat(5, 1fr)' },
      ];
    },
    titleColor() {
      return {
        color: this.configObj.titleColor.color[0].item,
      };
    },
    contentStyle() {
      return {
        'border-radius': this.configObj.contentStyle.val ? this.configObj.contentStyle.val + 'px' : '0',
      };
    },
  },
  watch: {
    num: {
      handler(nVal) {
        let data = this.$store.state.mobildConfig.defaultArray[nVal];
        this.setConfig(data);
      },
      deep: true,
    },
    defaultArray: {
      handler() {
        let data = this.$store.state.mobildConfig.defaultArray[this.num];
        this.setConfig(data);
      },
      deep: true,
    },
  },
  data() {
    return {
      // 默认初始化数据（定义组件的配置结构）
      defaultConfig: {
        isHide: false,
        name: 'categoryGrid',
        timestamp: this.num,
        setUp: {
          tabVal: 0,
          cname: '分类宫格',
        },
        categoryConfig: {
          tabTitle: '分类选择',
          title: '选择要展示的二级商品分类',
          categoryIds: [],
        },
        bgStyle: {
          tabTitle: '圆角设置',
          title: '背景圆角',
          name: 'bgStyle',
          val: 0,
          min: 0,
          max: 30,
        },
        contentStyle: {
          title: '图标圆角',
          name: 'contentStyle',
          val: 30,
          min: 0,
          max: 30,
        },
        bgColor: {
          tabTitle: '颜色设置',
          title: '背景颜色',
          name: 'bgColor',
          color: [
            { item: '#fff' },
            { item: '#fff' },
          ],
          default: [
            { item: '#fff' },
            { item: '#fff' },
          ],
        },
        titleColor: {
          title: '文字颜色',
          name: 'titleColor',
          color: [
            { item: '#282828' },
          ],
          default: [
            { item: '#282828' },
          ],
        },
        contentConfig: {
          title: '内容间距',
          val: 10,
          min: 0,
          max: 30,
        },
        upConfig: {
          tabTitle: '边距设置',
          title: '上边距',
          val: 10,
          min: 0,
          max: 100,
        },
        downConfig: {
          title: '下边距',
          val: 10,
          min: 0,
        },
        lrConfig: {
          title: '左右边距',
          val: 12,
          min: 0,
          max: 25,
        },
        mbConfig: {
          title: '页面间距',
          val: 10,
          min: 0,
        },
      },
      configObj: null,
      categoryList: [],
      pageData: {},
    };
  },
  mounted() {
    this.$nextTick(() => {
      if (this.num) {
        this.pageData = this.$store.state.mobildConfig.defaultArray[this.num];
        this.setConfig(this.pageData);
      }
    });
  },
  methods: {
    setConfig(data) {
      if (!data) return;
      this.configObj = data;
      // 根据 categoryIds 获取分类详情用于预览
      let ids = data.categoryConfig ? data.categoryConfig.categoryIds : [];
      if (ids && ids.length) {
        this.fetchCategories(ids);
      } else {
        this.categoryList = [];
      }
    },
    fetchCategories(ids) {
      categroyByIds({ ids: ids.join(',') }).then((res) => {
        // 按 categoryIds 的顺序排列结果
        let idOrder = ids;
        let map = {};
        res.forEach((item) => { map[item.id] = item; });
        this.categoryList = idOrder.map((id) => map[id]).filter(Boolean);
      }).catch(() => {
        this.categoryList = [];
      });
    },
  },
};
</script>

<style scoped lang="scss">
.category-grid {
  padding: 0 12px;
  display: grid;
  grid-template-rows: auto;

  .grid-item {
    font-size: 11px;
    color: #282828;
    text-align: center;

    .img-box {
      width: 50px;
      height: 50px;
      margin: 0 auto 7px auto;

      img {
        width: 100%;
        height: 100%;
      }
    }

    .empty-box {
      width: 100%;
      height: 100%;
      background: #f5f5f5;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  .icontupian {
    font-size: 16px;
  }
}
</style>
