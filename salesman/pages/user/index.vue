<template>
  <view class="container">
    <view class="title">我的</view>
    <view class="btns">
      <button type="default" @click="go('/pages/user/password')">修改密码</button>
      <button type="warn" @click="logout">退出登录</button>
    </view>
  </view>
</template>

<script>
const TOKEN_KEY = 'salesman_token';

export default {
  methods: {
    go(url) {
      uni.navigateTo({ url });
    },
    logout() {
      uni.removeStorageSync(TOKEN_KEY);
      uni.reLaunch({ url: '/pages/login/index' });
    },
  },
};
</script>

<style scoped>
.container {
  padding: 30rpx;
}
.title {
  font-size: 34rpx;
  font-weight: 600;
  margin-bottom: 20rpx;
}
.btns button {
  margin-bottom: 16rpx;
}
</style>

