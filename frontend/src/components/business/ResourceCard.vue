<template>
  <el-card class="resource-card" shadow="hover" @click="$emit('click')">
    <div class="resource-head">
      <span class="resource-title">{{ title }}</span>
      <LevelTag :level="level" />
    </div>
    <div class="resource-meta">
      <el-tag size="small" effect="plain">{{ type }}</el-tag>
      <el-tag size="small" type="info" effect="plain">{{ category }}</el-tag>
      <span class="text-muted">{{ durationText }}</span>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import LevelTag from './LevelTag.vue'

const props = defineProps<{
  title: string
  type: string
  category: string
  level: string
  durationSec?: number
}>()

defineEmits<{ (e: 'click'): void }>()

const durationText = computed(() => {
  if (!props.durationSec) return ''
  const m = Math.floor(props.durationSec / 60)
  const s = props.durationSec % 60
  return `${m}:${String(s).padStart(2, '0')}`
})
</script>

<style scoped lang="scss">
.resource-card {
  border-radius: var(--radius-md);
  cursor: pointer;
}

.resource-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.resource-title {
  font-size: 14px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}
</style>
