<template>
  <div class="mobile-config pro">
    <div v-for="(item, key) in rCom" :key="key">
      <component
        :is="item.components.name"
        :configObj="configObj"
        ref="childData"
        :configNme="item.configNme"
        :key="key"
        @getConfig="getConfig"
        :index="activeIndex"
        :num="item.num"
      ></component>
    </div>
    <rightBtn :activeIndex="activeIndex" :configObj="configObj"></rightBtn>
  </div>
</template>

<script>
import toolCom from '../mobileConfigRight/index.js';
import rightBtn from '../rightBtn/index.vue';
export default {
  name: 'c_home_category_grid',
  cname: '分类宫格',
  componentsName: 'home_category_grid',
  props: {
    activeIndex: {
      type: null,
    },
    num: {
      type: null,
    },
    index: {
      type: null,
    },
  },
  components: {
    ...toolCom,
    rightBtn,
  },
  data() {
    return {
      configObj: {},
      rCom: [
        {
          components: toolCom.c_checked_tab,
          configNme: 'setUp',
        },
      ],
      // 内容设置（分类选择）
      contentStyle: [
        {
          components: toolCom.c_title,
          configNme: 'categoryConfig',
        },
        {
          components: toolCom.c_category_select,
          configNme: 'categoryConfig',
        },
      ],
      // 样式设置
      configStyle: [
        {
          components: toolCom.c_title,
          configNme: 'bgColor',
        },
        {
          components: toolCom.c_bg_color,
          configNme: 'bgColor',
        },
        {
          components: toolCom.c_bg_color,
          configNme: 'titleColor',
        },
        {
          components: toolCom.c_title,
          configNme: 'upConfig',
        },
        {
          components: toolCom.c_slider,
          configNme: 'upConfig',
        },
        {
          components: toolCom.c_slider,
          configNme: 'downConfig',
        },
        {
          components: toolCom.c_slider,
          configNme: 'lrConfig',
        },
        {
          components: toolCom.c_slider,
          configNme: 'contentConfig',
        },
        {
          components: toolCom.c_slider,
          configNme: 'mbConfig',
        },
        {
          components: toolCom.c_title,
          configNme: 'bgStyle',
        },
        {
          components: toolCom.c_slider,
          configNme: 'bgStyle',
        },
        {
          components: toolCom.c_slider,
          configNme: 'contentStyle',
        },
      ],
      setUp: 0,
    };
  },
  watch: {
    num(nVal) {
      let value = JSON.parse(JSON.stringify(this.$store.state.mobildConfig.defaultArray[nVal]));
      this.configObj = value;
    },
    configObj: {
      handler(nVal) {
        this.$store.commit('mobildConfig/UPDATEARR', { num: this.num, val: nVal });
      },
      deep: true,
    },
    'configObj.setUp.tabVal': {
      handler(nVal) {
        this.setUp = nVal;
        var arr = [this.rCom[0]];
        if (nVal == 0) {
          this.rCom = arr.concat(this.contentStyle);
        } else {
          this.rCom = arr.concat(this.configStyle);
        }
      },
      deep: true,
    },
  },
  mounted() {
    this.$nextTick(() => {
      let value = JSON.parse(JSON.stringify(this.$store.state.mobildConfig.defaultArray[this.num]));
      this.configObj = value;
    });
  },
  methods: {
    getConfig(data) {},
  },
};
</script>

<style scoped></style>
