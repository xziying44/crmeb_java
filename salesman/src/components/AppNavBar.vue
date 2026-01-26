<template>
  <van-nav-bar
    :title="title"
    :left-arrow="showBack"
    @click-left="handleBack"
    :fixed="fixed"
    :placeholder="fixed"
  >
    <template #right v-if="$slots.right">
      <slot name="right" />
    </template>
  </van-nav-bar>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

interface Props {
  title?: string
  showBack?: boolean
  fixed?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  title: '',
  showBack: true,
  fixed: true
})

const router = useRouter()

function handleBack() {
  router.back()
}
</script>
