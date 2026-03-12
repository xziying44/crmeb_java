<template>
  <div class="activity-banner-placeholder" :style="boxStyle" v-if="configObj">
    <div class="placeholder-content">
      <i class="el-icon-picture-outline"></i>
      <span>活动横幅</span>
      <p class="tip">在 营销 > 活动横幅 中配置内容</p>
    </div>
  </div>
</template>

<script>
import { mapState } from 'vuex';
export default {
  name: 'home_activity_banner',
  cname: '活动横幅',
  icon: 't-icon-zujian-lunbotu',
  configName: 'c_home_activity_banner',
  type: 1,
  defaultName: 'activityBanner',
  props: {
    index: { type: null },
    num: { type: null },
  },
  computed: {
    ...mapState('mobildConfig', ['defaultArray']),
    boxStyle() {
      return [
        { margin: this.configObj.mbConfig.val + 'px' + ' ' + this.configObj.lrConfig.val + 'px' + ' 0' },
      ];
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
      defaultConfig: {
        isHide: false,
        name: 'activityBanner',
        timestamp: this.num,
        setUp: {
          tabVal: 0,
          cname: '活动横幅',
        },
        mbConfig: {
          title: '页面间距',
          val: 10,
          min: 0,
        },
        lrConfig: {
          title: '左右边距',
          val: 12,
          min: 0,
          max: 25,
        },
      },
      configObj: null,
    };
  },
  mounted() {
    this.$nextTick(() => {
      if (this.num) {
        let data = this.$store.state.mobildConfig.defaultArray[this.num];
        this.setConfig(data);
      }
    });
  },
  methods: {
    setConfig(data) {
      if (!data) return;
      this.configObj = data;
    },
  },
};
</script>

<style scoped lang="scss">
.activity-banner-placeholder {
  background: #f5f5f5;
  border: 1px dashed #ddd;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  .placeholder-content {
    color: #999;
    i {
      font-size: 28px;
      display: block;
      margin-bottom: 6px;
    }
    span {
      font-size: 14px;
      font-weight: bold;
    }
    .tip {
      font-size: 12px;
      margin-top: 4px;
      color: #bbb;
    }
  }
}
</style>
