<template>
  <view class="container">
    <view class="title">业务员首页</view>
    <view class="btns">
      <button type="default" @click="go('/pages/customer/list')">我的客户</button>
      <button type="default" @click="go('/pages/promote/index')">推广</button>
      <button type="default" @click="go('/pages/user/index')">我的</button>
    </view>
    <view class="card">
      <view class="card-title">看板数据（占位）</view>
      <view class="text">阶段一仅完成后端接口骨架与前端项目结构。</view>
    </view>
  </view>
</template>

<script>
export default {
  methods: {
    go(url) {
      uni.navigateTo({ url });
    },
  },
};
</script>

<style scoped>
.container {
  padding: 30rpx;
}
.title {
  font-size: 36rpx;
  font-weight: 600;
  margin-bottom: 20rpx;
}
.btns button {
  margin-bottom: 16rpx;
}
.card {
  margin-top: 20rpx;
  padding: 20rpx;
  background: #fff;
  border-radius: 12rpx;
}
.card-title {
  font-weight: 600;
  margin-bottom: 10rpx;
}
.text {
  color: #666;
  font-size: 26rpx;
}
</style>

