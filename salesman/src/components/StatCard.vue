<template>
  <div class="bg-white rounded-lg p-4 shadow-sm">
    <div class="text-gray-500 text-sm mb-2">{{ title }}</div>
    <div class="flex items-end justify-between">
      <div class="text-2xl font-bold" :class="valueClass">
        {{ prefix }}{{ displayValue }}{{ suffix }}
      </div>
      <div v-if="subValue !== undefined" class="text-xs text-gray-400">
        {{ subLabel }}: {{ subValue }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  title: string
  value: number | string
  prefix?: string
  suffix?: string
  subLabel?: string
  subValue?: number | string
  type?: 'default' | 'primary' | 'success' | 'warning'
}

const props = withDefaults(defineProps<Props>(), {
  prefix: '',
  suffix: '',
  type: 'default'
})

const displayValue = computed(() => {
  if (typeof props.value === 'number') {
    return props.value.toLocaleString()
  }
  return props.value
})

const valueClass = computed(() => {
  const classes: Record<string, string> = {
    default: 'text-gray-800',
    primary: 'text-primary',
    success: 'text-success',
    warning: 'text-warning'
  }
  return classes[props.type] || classes.default
})
</script>
