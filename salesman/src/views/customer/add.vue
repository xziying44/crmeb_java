<template>
  <div class="min-h-screen bg-gray-100">
    <!-- 导航栏 -->
    <van-nav-bar title="添加客户" left-arrow @click-left="router.back()" fixed placeholder />

    <!-- 提示 -->
    <div class="bg-blue-50 text-blue-600 text-sm p-3 mx-4 mt-4 rounded-lg">
      <van-icon name="info-o" class="mr-1" />
      输入客户手机号，发送验证码后由客户提供验证码完成绑定
    </div>

    <!-- 表单 -->
    <van-form @submit="handleSubmit" class="mt-4">
      <van-cell-group inset>
        <van-field
          v-model="form.phone"
          type="tel"
          label="手机号"
          placeholder="请输入客户手机号"
          maxlength="11"
          :rules="[
            { required: true, message: '请输入手机号' },
            { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号' }
          ]"
          clearable
        >
          <template #button>
            <van-button
              size="small"
              type="primary"
              :disabled="!isPhoneValid || countDown.isCounting.value"
              @click="handleSendCode"
            >
              {{ countDown.isCounting.value ? `${countDown.count.value}s` : '发送验证码' }}
            </van-button>
          </template>
        </van-field>

        <van-field
          v-model="form.code"
          type="digit"
          label="验证码"
          placeholder="请输入验证码"
          maxlength="6"
          :rules="[{ required: true, message: '请输入验证码' }]"
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
          :disabled="!form.phone || !form.code"
        >
          确认绑定
        </van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { sendCodeApi, bindByCodeApi } from '@/api/customer'
import { useCountDown } from '@/composables/useCountDown'

const router = useRouter()
const countDown = useCountDown(60)

const form = reactive({
  phone: '',
  code: ''
})

const submitting = ref(false)

const isPhoneValid = computed(() => /^1[3-9]\d{9}$/.test(form.phone))

async function handleSendCode() {
  if (!isPhoneValid.value || countDown.isCounting.value) return

  try {
    await sendCodeApi(form.phone)
    showToast('验证码已发送')
    countDown.start()
  } catch (error) {
    // 错误已处理
  }
}

async function handleSubmit() {
  if (submitting.value) return

  submitting.value = true
  try {
    await bindByCodeApi(form.phone, form.code)
    showToast('绑定成功')
    router.back()
  } catch (error) {
    // 错误已处理
  } finally {
    submitting.value = false
  }
}
</script>
