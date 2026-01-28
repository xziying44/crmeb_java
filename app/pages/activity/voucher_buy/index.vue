<template>
  <view class="voucher-buy-page" :data-theme="theme">
    <!-- 导航栏 -->
    <view class="header">
      <text class="title">代金券购买</text>
    </view>

    <!-- 代金券列表 -->
    <view class="voucher-list" v-if="voucherList.length">
      <view class="voucher-card" v-for="item in voucherList" :key="item.id">
        <!-- 左侧面值 -->
        <view class="left">
          <view class="face-value">
            <text class="symbol">¥</text>
            <text class="num">{{ item.money }}</text>
          </view>
          <view class="label">面值</view>
        </view>

        <!-- 中间信息 -->
        <view class="center">
          <view class="name">{{ item.name }}</view>
          <view class="price">售价 ¥{{ item.price }}</view>
          <view class="stock" v-if="item.isLimited">
            {{ item.lastTotal > 0 ? '剩余 ' + item.lastTotal + ' 张' : '已售罄' }}
          </view>
          <view class="tags">
            <text class="tag">无门槛</text>
            <text class="tag" v-if="item.canDeductFreight">可抵运费</text>
          </view>
        </view>

        <!-- 右侧按钮 -->
        <view class="right">
          <view
            class="buy-btn"
            :class="{ disabled: item.isLimited && item.lastTotal <= 0 }"
            @tap="handleBuy(item)"
          >
            {{ item.isLimited && item.lastTotal <= 0 ? '已售罄' : '立即购买' }}
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty" v-else-if="!loading">
      <image src="/static/images/noCart.png" mode="aspectFit"></image>
      <text>暂无可购买的代金券</text>
    </view>

    <!-- 购买确认弹窗 -->
    <uni-popup ref="buyPopup" type="center">
      <view class="buy-confirm-popup">
        <view class="popup-title">确认购买</view>
        <view class="popup-content" v-if="currentVoucher">
          <view>确认花费 <text class="price">¥{{ currentVoucher.price }}</text></view>
          <view>购买面值 <text class="money">¥{{ currentVoucher.money }}</text> 的代金券？</view>
        </view>
        <view class="popup-btns">
          <view class="btn cancel" @tap="cancelBuy">取消</view>
          <view class="btn confirm" @tap="confirmBuy">确认购买</view>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script>
import { getVoucherBuyList, buyVoucher } from '@/api/api.js';
import { mapGetters } from 'vuex';

export default {
  data() {
    return {
      voucherList: [],
      loading: false,
      currentVoucher: null
    };
  },
  computed: {
    ...mapGetters(['theme'])
  },
  onLoad() {
    this.loadList();
  },
  methods: {
    // 加载列表
    async loadList() {
      this.loading = true;
      try {
        const res = await getVoucherBuyList();
        this.voucherList = res.data || [];
      } catch (e) {
        this.$util.Tips({ title: '加载失败' });
      } finally {
        this.loading = false;
      }
    },

    // 点击购买
    handleBuy(item) {
      if (item.isLimited && item.lastTotal <= 0) {
        this.$util.Tips({ title: '该代金券已售罄' });
        return;
      }
      this.currentVoucher = item;
      this.$refs.buyPopup.open();
    },

    // 取消购买
    cancelBuy() {
      this.$refs.buyPopup.close();
      this.currentVoucher = null;
    },

    // 确认购买
    async confirmBuy() {
      if (!this.currentVoucher) return;

      try {
        uni.showLoading({ title: '正在创建订单...' });
        const res = await buyVoucher(this.currentVoucher.id, 'weixin');
        uni.hideLoading();

        this.$refs.buyPopup.close();

        // 跳转支付页
        uni.navigateTo({
          url: `/pages/order/order_payment/index?orderNo=${res.data.orderNo}&payPrice=${res.data.payPrice}`
        });
      } catch (e) {
        uni.hideLoading();
        this.$util.Tips({ title: e.message || '购买失败' });
      }
    }
  }
};
</script>

<style lang="scss" scoped>
.voucher-buy-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 40rpx;
}

.header {
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
  padding: 40rpx 30rpx;

  .title {
    font-size: 36rpx;
    color: #fff;
    font-weight: bold;
  }
}

.voucher-list {
  padding: 20rpx;
}

.voucher-card {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.05);

  .left {
    width: 140rpx;
    text-align: center;
    border-right: 1px dashed #eee;
    padding-right: 20rpx;

    .face-value {
      color: #52c41a;

      .symbol {
        font-size: 24rpx;
      }

      .num {
        font-size: 48rpx;
        font-weight: bold;
      }
    }

    .label {
      font-size: 22rpx;
      color: #999;
      margin-top: 8rpx;
    }
  }

  .center {
    flex: 1;
    padding: 0 20rpx;

    .name {
      font-size: 28rpx;
      font-weight: bold;
      color: #333;
      margin-bottom: 10rpx;
    }

    .price {
      font-size: 30rpx;
      color: #ff4d4f;
      font-weight: bold;
      margin-bottom: 8rpx;
    }

    .stock {
      font-size: 22rpx;
      color: #999;
      margin-bottom: 8rpx;
    }

    .tags {
      display: flex;
      gap: 10rpx;

      .tag {
        font-size: 20rpx;
        color: #52c41a;
        background: rgba(82, 196, 26, 0.1);
        padding: 4rpx 12rpx;
        border-radius: 4rpx;
      }
    }
  }

  .right {
    .buy-btn {
      background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
      color: #fff;
      font-size: 26rpx;
      padding: 16rpx 30rpx;
      border-radius: 30rpx;

      &.disabled {
        background: #ccc;
      }
    }
  }
}

.empty {
  padding: 100rpx 0;
  text-align: center;

  image {
    width: 300rpx;
    height: 300rpx;
  }

  text {
    display: block;
    color: #999;
    font-size: 28rpx;
    margin-top: 20rpx;
  }
}

.buy-confirm-popup {
  background: #fff;
  border-radius: 16rpx;
  padding: 40rpx;
  width: 560rpx;

  .popup-title {
    font-size: 32rpx;
    font-weight: bold;
    text-align: center;
    margin-bottom: 30rpx;
  }

  .popup-content {
    text-align: center;
    font-size: 28rpx;
    color: #666;
    line-height: 1.8;

    .price {
      color: #ff4d4f;
      font-weight: bold;
    }

    .money {
      color: #52c41a;
      font-weight: bold;
    }
  }

  .popup-btns {
    display: flex;
    gap: 20rpx;
    margin-top: 40rpx;

    .btn {
      flex: 1;
      text-align: center;
      padding: 20rpx;
      border-radius: 40rpx;
      font-size: 28rpx;

      &.cancel {
        background: #f5f5f5;
        color: #666;
      }

      &.confirm {
        background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
        color: #fff;
      }
    }
  }
}
</style>
