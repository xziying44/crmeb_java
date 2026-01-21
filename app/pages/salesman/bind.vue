<template>
  <view class="container">
    <view class="card">
      <view class="title">绑定业务员</view>
      <view class="desc">可通过扫描业务员小程序码自动带入邀请码。</view>

      <view class="form-item">
        <view class="label">邀请码</view>
        <input class="input" v-model="code" placeholder="请输入邀请码" />
      </view>

      <view class="tips" v-if="checked">
        <text :class="valid ? 'ok' : 'bad'">{{ valid ? '邀请码有效，可绑定' : '邀请码无效或不可绑定' }}</text>
      </view>

      <view class="btns">
        <button class="btn" type="default" @click="doCheck">校验</button>
        <button class="btn primary" type="primary" :disabled="!code" @click="doBind">立即绑定</button>
      </view>
    </view>
  </view>
</template>

<script>
import { mapGetters } from 'vuex';
import { toLogin } from '@/libs/login';
import { checkSalesmanCode, bindSalesman } from '@/api/salesman';

const app = getApp();

export default {
  data() {
    return {
      code: '',
      checked: false,
      valid: false,
      loading: false,
    };
  },
  computed: {
    ...mapGetters(['isLogin']),
  },
  onLoad(options) {
    // 优先使用页面参数，其次使用全局带入
    const fromQuery = options && options.code ? options.code : '';
    const fromGlobal = app && app.globalData && app.globalData.salesmanCode ? app.globalData.salesmanCode : '';
    this.code = fromQuery || fromGlobal || '';
    if (this.code) this.doCheck();
  },
  methods: {
    doCheck() {
      if (!this.code) return this.$util.Tips({ title: '请输入邀请码' });
      this.loading = true;
      checkSalesmanCode(this.code)
        .then((res) => {
          this.checked = true;
          this.valid = !!res.data;
        })
        .catch((err) => {
          this.checked = true;
          this.valid = false;
          this.$util.Tips({ title: err || '校验失败' });
        })
        .finally(() => {
          this.loading = false;
        });
    },
    doBind() {
      if (!this.code) return this.$util.Tips({ title: '请输入邀请码' });
      if (!this.isLogin) {
        // 未登录先登录，登录后可再次进入本页面绑定
        toLogin();
        return;
      }
      this.loading = true;
      bindSalesman(this.code)
        .then(() => {
          this.$util.Tips({ title: '绑定成功' });
          uni.navigateBack({ delta: 1 });
        })
        .catch((err) => {
          this.$util.Tips({ title: err || '绑定失败' });
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
};
</script>

<style scoped lang="scss">
.container {
  padding: 30rpx;
}
.card {
  background: #ffffff;
  border-radius: 12rpx;
  padding: 30rpx;
}
.title {
  font-size: 34rpx;
  font-weight: 600;
  margin-bottom: 10rpx;
}
.desc {
  color: #888;
  font-size: 24rpx;
  margin-bottom: 30rpx;
}
.form-item {
  margin-bottom: 20rpx;
}
.label {
  color: #333;
  margin-bottom: 10rpx;
}
.input {
  height: 80rpx;
  border: 1px solid #eee;
  border-radius: 8rpx;
  padding: 0 20rpx;
}
.tips {
  margin-top: 10rpx;
  font-size: 24rpx;
}
.ok {
  color: #07c160;
}
.bad {
  color: #ee0a24;
}
.btns {
  display: flex;
  margin-top: 30rpx;
}
.btn {
  flex: 1;
  margin-right: 16rpx;
}
.btn:last-child {
  margin-right: 0;
}
.primary {
  background: #409eff;
  color: #fff;
}
</style>

