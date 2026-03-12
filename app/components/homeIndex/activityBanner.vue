<template>
  <view v-if="bannerList.length > 0" class="activity-banner-wrap" :style="wrapStyle">
    <view
      v-for="(item, index) in bannerList"
      :key="item.id"
      class="banner-card"
      :style="[cardStyle, index > 0 ? { marginTop: '16rpx' } : {}]"
      @click="handleClick(item)"
    >
      <image
        :src="item.image"
        mode="widthFix"
        class="banner-image"
        :style="imageStyle"
      />
    </view>
  </view>
</template>

<script>
import { getActivityBannerListApi } from '@/api/activity.js';

const activityRouteMap = {
  1: '/pages/activity/goods_seckill/index',
  2: '/pages/activity/goods_bargain/index',
  3: '/pages/activity/goods_combination/index',
  4: '/pages/activity/promotionList/index?name=买赠活动&type=4',
  5: '/pages/activity/promotionList/index?name=满减活动&type=4',
};

export default {
  name: 'activityBanner',
  props: {
    dataConfig: {
      type: Object,
      default: () => ({}),
    },
  },
  data() {
    return {
      bannerList: [],
    };
  },
  computed: {
    wrapStyle() {
      const lr = this.dataConfig.lrConfig ? this.dataConfig.lrConfig.val * 2 : 24;
      const mb = this.dataConfig.mbConfig ? this.dataConfig.mbConfig.val * 2 : 20;
      return {
        padding: `0 ${lr}rpx`,
        marginTop: `${mb}rpx`,
      };
    },
    cardStyle() {
      return {
        borderRadius: '16rpx',
        overflow: 'hidden',
      };
    },
    imageStyle() {
      return {
        width: '100%',
        display: 'block',
        borderRadius: '16rpx',
      };
    },
  },
  created() {
    this.loadBanners();
  },
  methods: {
    loadBanners() {
      getActivityBannerListApi()
        .then((res) => {
          this.bannerList = res.data || [];
        })
        .catch(() => {
          this.bannerList = [];
        });
    },
    handleClick(item) {
      const url = activityRouteMap[item.activityType];
      if (url) {
        uni.navigateTo({ url });
      }
    },
  },
};
</script>

<style scoped>
.activity-banner-wrap {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.banner-card {
  width: 100%;
}
.banner-image {
  width: 100%;
  display: block;
}
</style>
