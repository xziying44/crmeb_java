<template>
  <div class="min-h-screen bg-gray-100 lg:flex lg:justify-center lg:items-start lg:py-4">
    <div class="w-full lg:max-w-[480px] lg:min-h-screen lg:shadow-xl bg-white relative">
      <!-- 页面内容 -->
      <div class="pb-[50px]">
        <slot />
      </div>
      <!-- TabBar -->
      <AppTabBar v-if="showTabBar" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppTabBar from '@/components/AppTabBar.vue'

const route = useRoute()

const showTabBar = computed(() => route.meta.tabbar === true)
</script>
