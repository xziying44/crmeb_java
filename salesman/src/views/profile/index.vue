<template>
  <div class="min-h-screen bg-gray-100">
    <!-- 用户信息卡片 -->
    <div class="bg-primary text-white p-6">
      <div class="flex items-center">
        <div class="w-16 h-16 rounded-full bg-white/20 flex items-center justify-center">
          <van-icon name="manager" size="32" />
        </div>
        <div class="ml-4">
          <div class="text-xl font-medium">{{ userStore.userInfo?.realName || '业务员' }}</div>
          <div class="text-white/80 mt-1">{{ userStore.userInfo?.phone }}</div>
        </div>
      </div>
    </div>

    <!-- 统计数据 -->
    <div class="bg-white -mt-2 rounded-t-lg">
      <div class="grid grid-cols-2 py-4">
        <div class="text-center border-r border-gray-100">
          <div class="text-2xl font-bold text-primary">
            {{ userStore.userInfo?.customerCount || 0 }}
          </div>
          <div class="text-sm text-gray-500 mt-1">我的客户</div>
        </div>
        <div class="text-center">
          <div class="text-2xl font-bold text-primary">
            {{ userStore.userInfo?.monthNewCustomerCount || 0 }}
          </div>
          <div class="text-sm text-gray-500 mt-1">本月新增</div>
        </div>
      </div>
    </div>

    <!-- 功能菜单 -->
    <van-cell-group inset class="mt-3">
      <van-cell title="我的邀请码" is-link @click="router.push('/promote')">
        <template #value>
          <span class="text-primary">{{ userStore.userInfo?.salesmanCode }}</span>
        </template>
      </van-cell>
      <van-cell title="修改密码" is-link @click="router.push('/profile/password')" />
      <van-cell title="关于我们" is-link @click="showAbout = true" />
    </van-cell-group>

    <!-- 退出登录 -->
    <div class="p-4 mt-4">
      <van-button block type="danger" plain @click="handleLogout">
        退出登录
      </van-button>
    </div>

    <!-- 版本信息 -->
    <div class="text-center text-gray-400 text-sm py-4">
      版本 1.0.0
    </div>

    <!-- 关于我们弹窗 -->
    <van-popup v-model:show="showAbout" round position="bottom">
      <div class="p-6 text-center">
        <div class="text-lg font-medium mb-4">关于我们</div>
        <div class="text-gray-600">
          <p>业务员助手 v1.0.0</p>
          <p class="mt-2">CRMEB 开源商城系统</p>
          <p class="mt-2 text-sm text-gray-400">© 2026 CRMEB. All rights reserved.</p>
        </div>
        <van-button block class="mt-6" @click="showAbout = false">关闭</van-button>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog } from 'vant'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const showAbout = ref(false)

function handleLogout() {
  showConfirmDialog({
    title: '提示',
    message: '确定要退出登录吗？',
  }).then(() => {
    userStore.logout()
    router.replace('/login')
  }).catch(() => {
    // 取消
  })
}

onMounted(() => {
  userStore.getUserInfo()
})
</script>
