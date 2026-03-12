<template>
	<!-- 分类宫格 -->
	<view v-show="categoryList.length" :style="[boxStyle]">
		<view class="category-grid" :style="[gridStyle]">
			<view class="grid-item" v-for="(item, index) in categoryList" :key="index"
				@click="goCategory(item)">
				<view class="pictrue skeleton-radius">
					<easy-loadimage :image-src="item.extra" :radius="dataConfig.contentStyle.val">
					</easy-loadimage>
				</view>
				<view class="grid-txt" :style="[titleColor]">{{ item.name }}</view>
			</view>
		</view>
	</view>
</template>

<script>
	import easyLoadimage from '@/components/base/easy-loadimage.vue';
	import { getCategoryByIds } from '@/api/api.js';
	export default {
		name: 'categoryGrid',
		props: {
			dataConfig: {
				type: Object,
				default: () => {}
			}
		},
		components: {
			easyLoadimage
		},
		data() {
			return {
				categoryList: []
			};
		},
		computed: {
			boxStyle() {
				return {
					borderRadius: this.dataConfig.bgStyle.val * 2 + 'rpx',
					background: `linear-gradient(${this.dataConfig.bgColor.color[0].item}, ${this.dataConfig.bgColor.color[1].item})`,
					margin: this.dataConfig.mbConfig.val * 2 + 'rpx' + ' ' + this.dataConfig.lrConfig.val * 2 + 'rpx' + ' ' + 0,
					padding: this.dataConfig.upConfig.val * 2 + 'rpx' + ' ' + 0 + ' ' + this.dataConfig.downConfig.val * 2 + 'rpx'
				}
			},
			gridStyle() {
				return {
					gridRowGap: this.dataConfig.contentConfig.val * 2 + 'rpx',
					gridTemplateColumns: 'repeat(5, 1fr)'
				}
			},
			titleColor() {
				return {
					'color': this.dataConfig.titleColor.color[0].item
				}
			}
		},
		mounted() {
			this.loadCategories();
		},
		methods: {
			loadCategories() {
				let ids = this.dataConfig.categoryConfig ? this.dataConfig.categoryConfig.categoryIds : [];
				if (!ids || !ids.length) return;
				getCategoryByIds(ids.join(',')).then(res => {
					// 按配置中的 ID 顺序排列
					let map = {};
					res.data.forEach(item => { map[item.id] = item; });
					this.categoryList = ids.map(id => map[id]).filter(Boolean);
				}).catch(() => {
					this.categoryList = [];
				});
			},
			goCategory(item) {
				uni.navigateTo({
					url: '/pages/goods/goods_list/index?cid=' + item.id + '&title=' + item.name
				});
			}
		}
	};
</script>

<style lang="scss" scoped>
	.category-grid {
		display: grid;
		grid-template-rows: auto;
		padding: 0 20rpx;

		.grid-item {
			text-align: center;

			.pictrue {
				width: 90rpx;
				height: 90rpx;
				margin: 0 auto;

				image {
					width: 100%;
					height: 100%;
				}
			}

			.grid-txt {
				font-size: 12px;
				margin-top: 14rpx;
			}
		}
	}
</style>
