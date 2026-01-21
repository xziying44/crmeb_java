<template>
  <view class="container">
    <view class="title">业务员登录</view>
    <view class="form-item">
      <text class="label">账号</text>
      <input class="input" v-model="account" placeholder="请输入账号" />
    </view>
    <view class="form-item">
      <text class="label">密码</text>
      <input class="input" v-model="pwd" password placeholder="请输入密码" />
    </view>
    <button class="btn" type="primary" @click="submit">登录</button>
    <view class="tips">提示：阶段一后端登录接口仍为占位实现，需配合后续完善。</view>
  </view>
</template>

<script>
import { loginApi } from '@/api/salesman.js';

const TOKEN_KEY = 'salesman_token';

export default {
  data() {
    return {
      account: '',
      pwd: '',
    };
  },
  methods: {
    submit() {
      if (!this.account || !this.pwd) {
        return uni.showToast({ title: '请输入账号和密码', icon: 'none' });
      }
      loginApi({ account: this.account, pwd: this.pwd })
        .then((data) => {
          const token = (data && (data.token || data.Token)) || '';
          if (token) uni.setStorageSync(TOKEN_KEY, token);
          uni.reLaunch({ url: '/pages/index/index' });
        })
        .catch((err) => {
          uni.showToast({ title: err || '登录失败', icon: 'none' });
        });
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
  margin-bottom: 30rpx;
}
.form-item {
  margin-bottom: 20rpx;
}
.label {
  display: block;
  margin-bottom: 10rpx;
}
.input {
  border: 1px solid #eee;
  border-radius: 8rpx;
  height: 80rpx;
  padding: 0 20rpx;
}
.btn {
  margin-top: 20rpx;
}
.tips {
  margin-top: 20rpx;
  color: #999;
  font-size: 24rpx;
}
</style>

