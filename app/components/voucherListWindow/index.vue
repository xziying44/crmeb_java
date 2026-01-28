<template>
  <view class="voucher-list-window" :class="{ on: visible }">
    <!-- 遮罩层 -->
    <view class="mask" @tap="close"></view>

    <!-- 弹窗内容 -->
    <view class="content">
      <!-- 标题栏 -->
      <view class="header">
        <text class="title">选择代金券</text>
        <text class="close-btn" @tap="close">×</text>
      </view>

      <!-- 代金券列表 -->
      <scroll-view scroll-y class="voucher-list" v-if="voucherList.length">
        <view
          class="voucher-item"
          :class="{ selected: selectedId === item.id, disabled: !isUsable(item) }"
          v-for="item in voucherList"
          :key="item.id"
          @tap="selectVoucher(item)"
        >
          <!-- 左侧金额 -->
          <view class="left">
            <view class="money">
              <text class="symbol">¥</text>
              <text class="num">{{ item.money }}</text>
            </view>
            <view class="tag" v-if="item.canDeductFreight">可抵运费</view>
          </view>

          <!-- 右侧信息 -->
          <view class="right">
            <view class="name">{{ item.name }}</view>
            <view class="condition">无门槛</view>
            <view class="date">{{ formatDate(item.startTime) }} - {{ formatDate(item.endTime) }}</view>
          </view>

          <!-- 选中标记 -->
          <view class="check" v-if="selectedId === item.id">
            <text class="iconfont icon-xuanzhong"></text>
          </view>
        </view>
      </scroll-view>

      <!-- 空状态 -->
      <view class="empty" v-else>
        <text>暂无可用代金券</text>
      </view>

      <!-- 不使用代金券 -->
      <view class="no-use" @tap="clearSelection">
        <text>不使用代金券</text>
      </view>

      <!-- 确认按钮 -->
      <view class="confirm-btn" @tap="confirm">确认</view>
    </view>
  </view>
</template>

<script>
import { getOrderVouchers } from '@/api/api.js';

export default {
  name: 'voucherListWindow',
  props: {
    // 是否显示
    visible: {
      type: Boolean,
      default: false
    },
    // 预下单号
    preOrderNo: {
      type: String,
      default: ''
    },
    // 当前选中的代金券ID
    currentVoucherId: {
      type: Number,
      default: 0
    }
  },
  data() {
    return {
      voucherList: [],
      selectedId: 0,
      loading: false
    };
  },
  watch: {
    visible(val) {
      if (val && this.preOrderNo) {
        this.loadVouchers();
      }
    },
    currentVoucherId: {
      immediate: true,
      handler(val) {
        this.selectedId = val || 0;
      }
    }
  },
  methods: {
    // 加载代金券列表
    async loadVouchers() {
      if (this.loading) return;
      this.loading = true;
      try {
        const res = await getOrderVouchers(this.preOrderNo);
        this.voucherList = res.data || [];
      } catch (e) {
        console.error('加载代金券失败', e);
        this.voucherList = [];
      } finally {
        this.loading = false;
      }
    },

    // 判断是否可用
    isUsable(item) {
      return item.status === 0;
    },

    // 选择代金券
    selectVoucher(item) {
      if (!this.isUsable(item)) return;
      this.selectedId = item.id;
    },

    // 清除选择
    clearSelection() {
      this.selectedId = 0;
    },

    // 确认选择
    confirm() {
      const selected = this.voucherList.find(v => v.id === this.selectedId);
      this.$emit('change', {
        voucherId: this.selectedId,
        voucher: selected || null
      });
      this.close();
    },

    // 关闭弹窗
    close() {
      this.$emit('update:visible', false);
      this.$emit('close');
    },

    // 格式化日期
    formatDate(dateStr) {
      if (!dateStr) return '';
      const date = new Date(dateStr);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    }
  }
};
</script>

<style lang="scss" scoped>
.voucher-list-window {
  position: fixed;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
  visibility: hidden;

  &.on {
    visibility: visible;

    .mask {
      opacity: 1;
    }

    .content {
      transform: translateY(0);
    }
  }

  .mask {
    position: absolute;
    left: 0;
    top: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    opacity: 0;
    transition: opacity 0.3s;
  }

  .content {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    background: #fff;
    border-radius: 24rpx 24rpx 0 0;
    transform: translateY(100%);
    transition: transform 0.3s;
    max-height: 70vh;
    display: flex;
    flex-direction: column;
  }

  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 30rpx;
    border-bottom: 1px solid #eee;

    .title {
      font-size: 32rpx;
      font-weight: bold;
    }

    .close-btn {
      font-size: 40rpx;
      color: #999;
    }
  }

  .voucher-list {
    flex: 1;
    padding: 20rpx;
    max-height: 50vh;
  }

  .voucher-item {
    display: flex;
    align-items: center;
    padding: 20rpx;
    margin-bottom: 20rpx;
    background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
    border-radius: 16rpx;
    position: relative;

    &.selected {
      box-shadow: 0 0 0 4rpx #52c41a;
    }

    &.disabled {
      opacity: 0.5;
    }

    .left {
      width: 160rpx;
      text-align: center;
      border-right: 1px dashed rgba(255, 255, 255, 0.5);
      padding-right: 20rpx;

      .money {
        color: #fff;

        .symbol {
          font-size: 24rpx;
        }

        .num {
          font-size: 48rpx;
          font-weight: bold;
        }
      }

      .tag {
        font-size: 20rpx;
        color: #fff;
        background: rgba(255, 255, 255, 0.3);
        padding: 4rpx 12rpx;
        border-radius: 20rpx;
        margin-top: 10rpx;
        display: inline-block;
      }
    }

    .right {
      flex: 1;
      padding-left: 20rpx;
      color: #fff;

      .name {
        font-size: 28rpx;
        font-weight: bold;
        margin-bottom: 8rpx;
      }

      .condition {
        font-size: 24rpx;
        opacity: 0.9;
      }

      .date {
        font-size: 22rpx;
        opacity: 0.8;
        margin-top: 8rpx;
      }
    }

    .check {
      position: absolute;
      right: 20rpx;
      top: 50%;
      transform: translateY(-50%);

      .iconfont {
        font-size: 40rpx;
        color: #fff;
      }
    }
  }

  .empty {
    padding: 60rpx;
    text-align: center;
    color: #999;
  }

  .no-use {
    padding: 20rpx 30rpx;
    text-align: center;
    color: #666;
    border-top: 1px solid #eee;
  }

  .confirm-btn {
    margin: 20rpx 30rpx 40rpx;
    background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
    color: #fff;
    text-align: center;
    padding: 24rpx;
    border-radius: 44rpx;
    font-size: 30rpx;
  }
}
</style>
