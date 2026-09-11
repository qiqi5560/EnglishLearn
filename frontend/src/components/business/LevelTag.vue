<template>
  <el-tag :type="tagType" size="small" effect="light" round>
    {{ level }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ level: string }>()

const tagType = computed(() => {
  const map: Record<string, 'success' | 'primary' | 'warning'> = {
    A1: 'success',
    A2: 'success',
    B1: 'primary',
    B2: 'primary',
    C1: 'warning',
    C2: 'warning',
  }
  return map[props.level] ?? 'info'
})
</script>
