<template>
  <div class="bg-white rounded-lg p-6 text-center">
    <div class="text-lg font-medium mb-4">我的推广码</div>

    <!-- 二维码 -->
    <div class="inline-block p-4 bg-gray-50 rounded-lg">
      <van-image
        v-if="qrcodeUrl"
        width="180"
        height="180"
        :src="qrcodeUrl"
        fit="contain"
      >
        <template #loading>
          <van-loading type="spinner" size="40" />
        </template>
        <template #error>
          <div class="text-gray-400 text-sm">加载失败</div>
        </template>
      </van-image>
      <div v-else class="w-[180px] h-[180px] flex items-center justify-center">
        <van-button size="small" type="primary" @click="emit('refresh')">
          生成二维码
        </van-button>
      </div>
    </div>

    <!-- 邀请码 -->
    <div class="mt-4">
      <div class="text-gray-500 text-sm">邀请码</div>
      <div class="text-2xl font-bold text-primary mt-2 tracking-widest">
        {{ code }}
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="flex gap-3 mt-6">
      <van-button block icon="photograph" @click="emit('save')">
        保存图片
      </van-button>
      <van-button block icon="share-o" type="primary" @click="emit('share')">
        分享
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  code: string
  qrcodeUrl: string | null
}

defineProps<Props>()

const emit = defineEmits<{
  refresh: []
  save: []
  share: []
}>()
</script>
