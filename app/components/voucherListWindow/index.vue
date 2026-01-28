<template>
	<view>
		<view class='voucher-list-window' :class='visible==true?"on":""'>
			<view class='voucher-list' style="margin-top: 50rpx;">
				<block v-if="voucherList.length">
					<view class='item acea-row row-center-wrapper' v-for="(item,index) in voucherList"
						@click="selectVoucher(index, item.id)" :key='index'>
						<view class='money acea-row row-column row-center-wrapper' :class='item.isUse?"moneyGray":"voucher_bg"'>
							<view>￥<text class='num' :style="[{'font-size': String(item.money).length>=7?'42rpx':'60rpx'}]">{{item.money?Number(item.money):''}}</text></view>
							<view class="pic-num">无门槛</view>
						</view>
						<view class='text'>
							<view class='condition line2'>
								<span class='line-title' :class='item.isUse?"gray":"voucher_tag"'>代金券</span>
								<span>{{item.name}}</span>
							</view>
							<view class='data acea-row row-between-wrapper'>
								<view>{{ formatDate(item.startTime) }} - {{ formatDate(item.endTime) }}</view>
								<view class='bnt gray' v-if="item.isUse">{{item.use_title || '已选择'}}</view>
								<view class='bnt voucher_bg' v-else>立即使用</view>
							</view>
						</view>
					</view>
				</block>
				<!-- 无代金券 -->
				<view class='pictrue' v-else>
					<view class="empty-text">暂无可用代金券</view>
				</view>
			</view>
		</view>
		<view class='mask' catchtouchmove="true" :hidden='visible==false' @click='close'></view>
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
				// 处理列表数据，添加选中状态
				let list = res.data || [];
				list = list.map(item => {
					return {
						...item,
						isUse: this.selectedId === item.id ? 1 : 0,
						use_title: this.selectedId === item.id ? '不使用' : ''
					};
				});
				this.voucherList = list;
			} catch (e) {
				console.error('加载代金券失败', e);
				this.voucherList = [];
			} finally {
				this.loading = false;
			}
		},

		// 选择代金券
		selectVoucher(index, id) {
			let list = this.voucherList;

			// 清除其他选中状态
			for (let i = 0, len = list.length; i < len; i++) {
				if (i !== index) {
					list[i].use_title = '';
					list[i].isUse = 0;
				}
			}

			if (list[index].isUse) {
				// 取消选择代金券
				list[index].use_title = '';
				list[index].isUse = 0;
				this.selectedId = 0;
				this.$emit('change', {
					voucherId: 0,
					voucher: null
				});
			} else {
				// 选择代金券
				list[index].use_title = '不使用';
				list[index].isUse = 1;
				this.selectedId = id;
				this.$emit('change', {
					voucherId: id,
					voucher: list[index]
				});
			}

			this.voucherList = [...list];
			// 关闭弹窗
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

<style scoped lang="scss">
	.voucher-list-window {
		position: fixed;
		bottom: 0;
		left: 0;
		width: 100%;
		background-color: #f5f5f5;
		border-radius: 16rpx 16rpx 0 0;
		z-index: 555;
		transform: translate3d(0, 100%, 0);
		transition: all .3s cubic-bezier(.25, .5, .5, .9);
	}

	.voucher-list-window.on {
		transform: translate3d(0, 0, 0);
	}

	.voucher-list-window .voucher-list {
		margin: 0 0 30rpx 0;
		height: 823rpx;
		overflow: auto;
		padding-top: 30rpx;
	}

	.voucher-list-window .pictrue {
		padding: 100rpx 0;
		text-align: center;
	}

	.empty-text {
		color: #999;
		font-size: 28rpx;
	}

	.pic-num {
		color: #fff;
		font-size: 24rpx;
	}

	.line-title {
		width: 90rpx;
		padding: 0 10rpx;
		box-sizing: border-box;
		background: #fff;
		border: 1px solid #52c41a;
		opacity: 1;
		border-radius: 20rpx;
		font-size: 20rpx;
		color: #52c41a;
		margin-right: 12rpx;
	}

	.line-title.gray {
		border-color: #BBB;
		color: #bbb;
		background-color: #F5F5F5;
	}

	.voucher-list .item {
		margin: 0 20rpx 20rpx;
		box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.06);
		background: #fff;
		border-radius: 14rpx;
		overflow: hidden;
	}

	.voucher-list .item .money {
		width: 200rpx;
		height: 160rpx;
		font-weight: normal;
		color: #fff;
	}

	.voucher-list .item .money .num {
		font-size: 60rpx;
		font-weight: bold;
	}

	.voucher-list .item .text {
		flex: 1;
		padding: 20rpx;
	}

	.voucher-list .item .text .condition {
		font-size: 26rpx;
		color: #333;
		margin-bottom: 16rpx;
	}

	.voucher-list .item .text .data {
		font-size: 22rpx;
		color: #999;
	}

	.voucher-list .item .text .data .bnt {
		padding: 6rpx 20rpx;
		border-radius: 30rpx;
		font-size: 22rpx;
		color: #fff;
	}

	.voucher-list .item .text .data .bnt.gray {
		background-color: #ccc;
	}

	.voucher_bg {
		background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
	}

	.voucher_tag {
		color: #52c41a;
		border-color: #52c41a;
	}

	.moneyGray {
		background: #ccc;
	}

	.mask {
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
		bottom: 0;
		background-color: rgba(0, 0, 0, 0.5);
		z-index: 500;
	}
</style>
