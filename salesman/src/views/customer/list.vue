<template>
  <div class="min-h-screen bg-gray-100">
    <!-- 搜索栏 -->
    <div class="bg-white sticky top-0 z-10">
      <van-search
        v-model="keywords"
        placeholder="搜索客户姓名/手机号"
        @search="handleSearch"
        @clear="handleSearch"
      />
    </div>

    <!-- 列表 -->
    <van-pull-refresh v-model="refreshing" @refresh="handleRefresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="loadMore"
        class="px-4 py-3"
      >
        <div class="space-y-3">
          <CustomerCard
            v-for="item in list"
            :key="item.uid"
            :customer="item"
            @click="goDetail"
          />
        </div>

        <!-- 空状态 -->
        <van-empty
          v-if="!loading && list.length === 0"
          description="暂无客户"
          :image-size="120"
        />
      </van-list>
    </van-pull-refresh>

    <!-- 添加按钮 -->
    <div class="fixed bottom-20 right-4 lg:right-[calc(50%-240px+16px)]">
      <van-button
        round
        type="primary"
        icon="plus"
        @click="goAdd"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCustomerListApi } from '@/api/customer'
import CustomerCard from '@/components/CustomerCard.vue'
import type { Customer } from '@/types/customer'

const router = useRouter()

const keywords = ref('')
const list = ref<Customer[]>([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const page = ref(1)
const limit = 10

async function fetchList(isRefresh = false) {
  if (isRefresh) {
    page.value = 1
    finished.value = false
  }

  try {
    const result = await getCustomerListApi({
      page: page.value,
      limit,
      keywords: keywords.value || undefined
    })

    if (isRefresh) {
      list.value = result.list
    } else {
      list.value.push(...result.list)
    }

    if (list.value.length >= result.total) {
      finished.value = true
    }

    page.value++
  } catch (error) {
    finished.value = true
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function loadMore() {
  fetchList()
}

function handleRefresh() {
  fetchList(true)
}

function handleSearch() {
  fetchList(true)
}

function goDetail(customer: Customer) {
  router.push(`/customer/${customer.uid}`)
}

function goAdd() {
  router.push('/customer/add')
}

onMounted(() => {
  fetchList(true)
})
</script>
