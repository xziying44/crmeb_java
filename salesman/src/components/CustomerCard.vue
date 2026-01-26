<template>
  <div class="bg-white rounded-lg p-4 shadow-sm" @click="handleClick">
    <div class="flex items-center">
      <!-- 头像 -->
      <van-image
        round
        width="48"
        height="48"
        :src="customer.avatar || defaultAvatar"
        fit="cover"
      />
      <!-- 信息 -->
      <div class="flex-1 ml-3 min-w-0">
        <div class="flex items-center justify-between">
          <div class="font-medium text-gray-800 truncate">
            {{ customer.nickname || customer.phone }}
          </div>
          <div class="text-primary font-medium">
            ¥{{ formatAmount(customer.totalAmount) }}
          </div>
        </div>
        <div class="flex items-center justify-between mt-1">
          <div class="text-sm text-gray-500">{{ customer.phone }}</div>
          <div class="text-xs text-gray-400">{{ customer.orderCount }}笔订单</div>
        </div>
        <div class="text-xs text-gray-400 mt-1">
          绑定时间: {{ formatDate(customer.bindTime) }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Customer } from '@/types/customer'

interface Props {
  customer: Customer
}

const props = defineProps<Props>()

const emit = defineEmits<{
  click: [customer: Customer]
}>()

const defaultAvatar = 'https://img.yzcdn.cn/vant/cat.jpeg'

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString()
}

/**
 * 格式化金额，处理 BigDecimal 序列化为字符串的情况
 */
function formatAmount(amount: number | string | null | undefined): string {
  if (amount == null) return '0.00'
  const num = typeof amount === 'string' ? parseFloat(amount) : amount
  return isNaN(num) ? '0.00' : num.toFixed(2)
}

function handleClick() {
  emit('click', props.customer)
}
</script>
