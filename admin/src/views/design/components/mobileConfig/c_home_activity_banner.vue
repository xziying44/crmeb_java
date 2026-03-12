<template>
  <div class="mobile-config">
    <Form ref="formInline">
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
      <div class="config-tip">
        <el-alert
          title="活动横幅内容在 营销 > 活动横幅 中配置"
          type="info"
          :closable="false"
          show-icon
        />
      </div>
      <rightBtn :activeIndex="activeIndex" :configObj="configObj"></rightBtn>
    </Form>
  </div>
</template>

<script>
import toolCom from '../mobileConfigRight/index.js';
import rightBtn from '../rightBtn/index.vue';
export default {
  name: 'c_home_activity_banner',
  componentsName: 'home_activity_banner',
  cname: '活动横幅',
  props: {
    activeIndex: { type: null },
    num: { type: null },
    index: { type: null },
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
        var arr = [this.rCom[0]];
        if (nVal == 1) {
          let tempArr = [
            { components: toolCom.c_title, configNme: 'mbConfig' },
            { components: toolCom.c_slider, configNme: 'mbConfig' },
            { components: toolCom.c_slider, configNme: 'lrConfig' },
          ];
          this.rCom = arr.concat(tempArr);
        } else {
          this.rCom = arr;
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
    getConfig() {},
  },
};
</script>

<style scoped lang="scss">
.config-tip {
  padding: 10px 15px;
}
</style>
