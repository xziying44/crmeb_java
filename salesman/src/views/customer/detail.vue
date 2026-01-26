<template>
  <div class="min-h-screen bg-gray-100">
    <!-- 导航栏 -->
    <van-nav-bar title="客户详情" left-arrow @click-left="router.back()" fixed placeholder />

    <!-- 加载中 -->
    <div v-if="loading" class="p-4">
      <van-skeleton title :row="5" />
    </div>

    <template v-else-if="customer">
      <!-- 客户信息卡片 -->
      <div class="bg-white p-4 mb-3">
        <div class="flex items-center">
          <van-image
            round
            width="60"
            height="60"
            :src="customer.avatar || defaultAvatar"
            fit="cover"
          />
          <div class="ml-4 flex-1">
            <div class="text-lg font-medium text-gray-800">
              {{ customer.nickname || customer.phone }}
            </div>
            <div class="text-gray-500 mt-1">{{ customer.phone }}</div>
          </div>
        </div>
      </div>

      <!-- 统计数据 -->
      <div class="grid grid-cols-3 bg-white p-4 mb-3">
        <div class="text-center">
          <div class="text-xl font-bold text-primary">{{ customer.orderCount }}</div>
          <div class="text-sm text-gray-500 mt-1">订单数</div>
        </div>
        <div class="text-center border-x border-gray-100">
          <div class="text-xl font-bold text-primary">¥{{ customer.totalAmount.toFixed(2) }}</div>
          <div class="text-sm text-gray-500 mt-1">消费金额</div>
        </div>
        <div class="text-center">
          <div class="text-xl font-bold text-primary">{{ bindDays }}</div>
          <div class="text-sm text-gray-500 mt-1">绑定天数</div>
        </div>
      </div>

      <!-- 详细信息 -->
      <van-cell-group inset class="mb-3">
        <van-cell title="绑定时间" :value="formatDate(customer.bindTime)" />
        <van-cell title="最近下单" :value="customer.lastOrderTime ? formatDate(customer.lastOrderTime) : '暂无'" />
      </van-cell-group>

      <!-- 备注 -->
      <van-cell-group inset>
        <van-cell
          title="备注"
          :value="customer.remark || '点击添加备注'"
          is-link
          @click="showRemarkPopup = true"
        />
      </van-cell-group>
    </template>

    <!-- 备注弹窗 -->
    <van-popup v-model:show="showRemarkPopup" position="bottom" round>
      <div class="p-4">
        <div class="text-lg font-medium mb-4">编辑备注</div>
        <van-field
          v-model="remarkInput"
          type="textarea"
          placeholder="请输入备注信息"
          rows="4"
          maxlength="200"
          show-word-limit
        />
        <div class="flex gap-3 mt-4">
          <van-button block @click="showRemarkPopup = false">取消</van-button>
          <van-button block type="primary" @click="handleSaveRemark">保存</van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getCustomerDetailApi, updateRemarkApi } from '@/api/customer'
import type { Customer } from '@/types/customer'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const customer = ref<Customer | null>(null)
const showRemarkPopup = ref(false)
const remarkInput = ref('')

const defaultAvatar = 'https://img.yzcdn.cn/vant/cat.jpeg'

const bindDays = computed(() => {
  if (!customer.value?.bindTime) return 0
  const bindDate = new Date(customer.value.bindTime)
  const now = new Date()
  const diff = now.getTime() - bindDate.getTime()
  return Math.floor(diff / (1000 * 60 * 60 * 24))
})

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString()
}

async function fetchDetail() {
  const uid = Number(route.params.id)
  if (!uid) {
    router.back()
    return
  }

  loading.value = true
  try {
    customer.value = await getCustomerDetailApi(uid)
    remarkInput.value = customer.value.remark || ''
  } catch (error) {
    router.back()
  } finally {
    loading.value = false
  }
}

async function handleSaveRemark() {
  if (!customer.value) return

  try {
    await updateRemarkApi(customer.value.uid, remarkInput.value)
    customer.value.remark = remarkInput.value
    showRemarkPopup.value = false
    showToast('保存成功')
  } catch (error) {
    // 错误已处理
  }
}

onMounted(() => {
  fetchDetail()
})
</script>
