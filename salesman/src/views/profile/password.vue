<template>
  <div class="min-h-screen bg-gray-100">
    <!-- 导航栏 -->
    <van-nav-bar title="修改密码" left-arrow @click-left="router.back()" fixed placeholder />

    <!-- 表单 -->
    <van-form @submit="handleSubmit" class="mt-4">
      <van-cell-group inset>
        <van-field
          v-model="form.oldPassword"
          type="password"
          label="原密码"
          placeholder="请输入原密码"
          :rules="[{ required: true, message: '请输入原密码' }]"
          clearable
        />
        <van-field
          v-model="form.newPassword"
          type="password"
          label="新密码"
          placeholder="请输入新密码"
          :rules="[
            { required: true, message: '请输入新密码' },
            { pattern: /^.{6,20}$/, message: '密码长度为6-20位' }
          ]"
          clearable
        />
        <van-field
          v-model="form.confirmPassword"
          type="password"
          label="确认密码"
          placeholder="请再次输入新密码"
          :rules="[
            { required: true, message: '请确认新密码' },
            { validator: validateConfirm, message: '两次输入的密码不一致' }
          ]"
          clearable
        />
      </van-cell-group>

      <div class="p-4">
        <van-button
          round
          block
          type="primary"
          native-type="submit"
          :loading="submitting"
        >
          确认修改
        </van-button>
      </div>
    </van-form>

    <!-- 提示 -->
    <div class="px-4 text-sm text-gray-400">
      <p>• 密码长度为 6-20 位</p>
      <p>• 修改成功后需要重新登录</p>
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

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const submitting = ref(false)

function validateConfirm(value: string): boolean {
  return value === form.newPassword
}

async function handleSubmit() {
  if (submitting.value) return

  submitting.value = true
  try {
    await userStore.updatePassword(form.oldPassword, form.newPassword)
    showToast('修改成功，请重新登录')
    userStore.logout()
    router.replace('/login')
  } catch (error) {
    // 错误已处理
  } finally {
    submitting.value = false
  }
}
</script>
