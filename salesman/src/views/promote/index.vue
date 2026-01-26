<template>
  <div class="min-h-screen bg-gray-100 p-4">
    <!-- 二维码卡片 -->
    <QrcodeCard
      :code="userStore.userInfo?.salesmanCode || ''"
      :qrcode-url="userStore.userInfo?.salesmanQrcode || null"
      @refresh="handleRefresh"
      @save="handleSave"
      @share="handleShare"
    />

    <!-- 使用说明 -->
    <div class="bg-white rounded-lg p-4 mt-4">
      <div class="font-medium mb-3">使用说明</div>
      <div class="space-y-2 text-sm text-gray-600">
        <div class="flex items-start">
          <van-icon name="checked" class="text-primary mr-2 mt-0.5" />
          <span>客户扫描二维码可直接绑定为您的客户</span>
        </div>
        <div class="flex items-start">
          <van-icon name="checked" class="text-primary mr-2 mt-0.5" />
          <span>也可以让客户在注册时输入您的邀请码</span>
        </div>
        <div class="flex items-start">
          <van-icon name="checked" class="text-primary mr-2 mt-0.5" />
          <span>绑定成功后，客户的消费将计入您的业绩</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { showToast } from 'vant'
import { useUserStore } from '@/stores/user'
import { refreshQrcodeApi } from '@/api/promote'
import QrcodeCard from '@/components/QrcodeCard.vue'

const userStore = useUserStore()

async function handleRefresh() {
  try {
    const qrcodeUrl = await refreshQrcodeApi()
    if (userStore.userInfo) {
      userStore.userInfo.salesmanQrcode = qrcodeUrl
    }
    showToast('二维码已刷新')
  } catch (error) {
    // 错误已处理
  }
}

function handleSave() {
  const url = userStore.userInfo?.salesmanQrcode
  if (!url) {
    showToast('请先生成二维码')
    return
  }
  // 创建下载链接
  const link = document.createElement('a')
  link.href = url
  link.download = `推广码_${userStore.userInfo?.salesmanCode}.png`
  link.click()
  showToast('已保存到相册')
}

function handleShare() {
  const code = userStore.userInfo?.salesmanCode
  if (!code) return

  // 尝试使用 Web Share API
  if (navigator.share) {
    navigator.share({
      title: '邀请您加入',
      text: `使用邀请码 ${code} 注册，即可成为我的专属客户`,
    }).catch(() => {
      // 用户取消分享
    })
  } else {
    // 复制到剪贴板
    navigator.clipboard.writeText(code).then(() => {
      showToast('邀请码已复制')
    })
  }
}

onMounted(() => {
  userStore.getUserInfo()
})
</script>
