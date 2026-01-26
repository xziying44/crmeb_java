<template>
  <div class="min-h-screen flex flex-col justify-center px-6 bg-white">
    <!-- Logo 区域 -->
    <div class="text-center mb-10">
      <div class="w-20 h-20 mx-auto mb-4 bg-primary rounded-full flex items-center justify-center">
        <van-icon name="manager" size="40" color="#fff" />
      </div>
      <h1 class="text-2xl font-bold text-gray-800">业务员助手</h1>
      <p class="text-gray-500 mt-2">专属业务员管理平台</p>
    </div>

    <!-- 登录表单 -->
    <van-form @submit="handleLogin" class="w-full">
      <van-cell-group inset>
        <van-field
          v-model="form.account"
          name="account"
          label="账号"
          placeholder="请输入账号"
          :rules="[{ required: true, message: '请输入账号' }]"
          clearable
        />
        <van-field
          v-model="form.pwd"
          type="password"
          name="pwd"
          label="密码"
          placeholder="请输入密码"
          :rules="[{ required: true, message: '请输入密码' }]"
          clearable
        />
      </van-cell-group>

      <div class="mt-6 px-4">
        <van-button
          round
          block
          type="primary"
          native-type="submit"
          :loading="loading"
          loading-text="登录中..."
        >
          登录
        </van-button>
      </div>
    </van-form>

    <!-- 底部版权 -->
    <div class="text-center text-gray-400 text-sm mt-10">
      © 2026 CRMEB. All rights reserved.
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)

const form = reactive({
  account: '',
  pwd: ''
})

async function handleLogin() {
  if (loading.value) return

  loading.value = true
  try {
    await userStore.login({
      account: form.account,
      pwd: form.pwd
    })
    showToast('登录成功')
    router.replace('/')
  } catch (error) {
    // 错误已在 request 中处理
  } finally {
    loading.value = false
  }
}
</script>
