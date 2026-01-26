<template>
  <div class="min-h-screen bg-gray-100">
    <!-- 顶部欢迎区域 -->
    <div class="bg-primary text-white p-6 pb-16">
      <div class="flex items-center justify-between">
        <div>
          <div class="text-lg font-medium">{{ greeting }}</div>
          <div class="text-sm opacity-80 mt-1">{{ userStore.userInfo?.realName || '业务员' }}</div>
        </div>
        <van-icon name="bell" size="24" @click="handleNotice" />
      </div>
    </div>

    <!-- 数据卡片 -->
    <div class="px-4 -mt-10">
      <div class="grid grid-cols-2 gap-3">
        <StatCard
          title="我的客户"
          :value="dashboard.totalCustomerCount"
          suffix="人"
          type="primary"
          sub-label="本月新增"
          :sub-value="dashboard.monthNewCustomerCount"
        />
        <StatCard
          title="客户订单额"
          :value="formatAmount(dashboard.totalOrderAmount)"
          prefix="¥"
          type="success"
          sub-label="本月"
          :sub-value="'¥' + formatAmount(dashboard.monthOrderAmount)"
        />
      </div>
    </div>

    <!-- 日期类型切换 -->
    <div class="px-4 mt-4">
      <van-cell-group inset>
        <van-cell title="数据统计">
          <template #right-icon>
            <van-tag
              v-for="item in dateTypes"
              :key="item.value"
              :type="dateType === item.value ? 'primary' : 'default'"
              class="ml-2 cursor-pointer"
              @click="handleDateTypeChange(item.value)"
            >
              {{ item.label }}
            </van-tag>
          </template>
        </van-cell>
      </van-cell-group>
    </div>

    <!-- 客户消费排行 -->
    <div class="px-4 mt-4 pb-4">
      <van-cell-group inset>
        <van-cell title="客户消费排行" is-link @click="goCustomerList" />
        <template v-if="dashboard.customerRanking.length > 0">
          <van-cell
            v-for="(item, index) in dashboard.customerRanking.slice(0, 5)"
            :key="item.uid"
            :title="item.nickname || item.phone"
            :label="`订单 ${item.orderCount} 笔`"
            center
          >
            <template #icon>
              <div
                class="w-6 h-6 rounded-full flex items-center justify-center text-white text-xs mr-3"
                :class="getRankClass(index)"
              >
                {{ index + 1 }}
              </div>
            </template>
            <template #value>
              <span class="text-primary font-medium">¥{{ formatAmount(item.totalAmount) }}</span>
            </template>
          </van-cell>
        </template>
        <van-empty v-else description="暂无数据" :image-size="80" />
      </van-cell-group>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getDashboardApi } from '@/api/dashboard'
import StatCard from '@/components/StatCard.vue'
import type { DashboardData } from '@/types/dashboard'

const router = useRouter()
const userStore = useUserStore()

const dateType = ref('day')
const dateTypes = [
  { label: '日', value: 'day' },
  { label: '周', value: 'week' },
  { label: '月', value: 'month' }
]

const dashboard = reactive<DashboardData>({
  totalCustomerCount: 0,
  monthNewCustomerCount: 0,
  totalOrderAmount: 0,
  monthOrderAmount: 0,
  trendData: [],
  customerRanking: []
})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

function formatAmount(value: number): string {
  if (value >= 10000) {
    return (value / 10000).toFixed(2) + '万'
  }
  return value.toFixed(2)
}

function getRankClass(index: number): string {
  const classes = ['bg-red-500', 'bg-orange-500', 'bg-yellow-500']
  return classes[index] || 'bg-gray-400'
}

async function fetchDashboard() {
  try {
    const data = await getDashboardApi(dateType.value)
    Object.assign(dashboard, data)
  } catch (error) {
    // 错误已处理
  }
}

function handleDateTypeChange(value: string) {
  dateType.value = value
  fetchDashboard()
}

function handleNotice() {
  // TODO: 通知功能
}

function goCustomerList() {
  router.push('/customer')
}

onMounted(() => {
  fetchDashboard()
  userStore.getUserInfo()
})
</script>
