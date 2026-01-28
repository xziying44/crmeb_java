<template>
  <view class="full-reduction-wrap" v-if="reduction && reduction.levels && reduction.levels.length">
    <view class="reduction-label">满减</view>
    <scroll-view scroll-x class="tags-scroll" :show-scrollbar="false">
      <view class="tags-inner">
        <view
          class="tag-item"
          v-for="(level, index) in reduction.levels"
          :key="index"
        >
          满{{ formatPrice(level.fullAmount) }}减{{ formatPrice(level.reduceAmount) }}
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
export default {
  name: 'fullReductionTags',
  props: {
    reduction: {
      type: Object,
      default: () => null
    }
  },
  methods: {
    /**
     * 格式化价格显示，去除末尾的 .00
     */
    formatPrice(price) {
      if (price === null || price === undefined) return '0';
      const num = parseFloat(price);
      if (Number.isInteger(num)) {
        return num.toString();
      }
      return num.toFixed(2).replace(/\.?0+$/, '');
    }
  }
}
</script>

<style lang="scss" scoped>
.full-reduction-wrap {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  background: linear-gradient(90deg, #FFF5F5 0%, #FFFFFF 100%);
  margin: 0 20rpx;
  border-radius: 12rpx;
  margin-top: 16rpx;

  .reduction-label {
    flex-shrink: 0;
    padding: 6rpx 14rpx;
    background: linear-gradient(90deg, #FF6B6B, #FF8E53);
    color: #fff;
    font-size: 22rpx;
    font-weight: 500;
    border-radius: 6rpx;
    margin-right: 20rpx;
  }

  .tags-scroll {
    flex: 1;
    white-space: nowrap;
    overflow: hidden;
  }

  .tags-inner {
    display: inline-flex;
    gap: 16rpx;
  }

  .tag-item {
    display: inline-block;
    padding: 8rpx 18rpx;
    background: #FFF;
    color: #FF6B6B;
    font-size: 24rpx;
    border-radius: 20rpx;
    border: 1rpx solid #FFD4D4;
    white-space: nowrap;
  }
}
</style>
