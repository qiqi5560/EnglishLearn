<template>
  <div class="score-ring">
    <el-progress
      type="circle"
      :percentage="score"
      :width="size"
      :stroke-width="stroke"
      :color="color"
    >
      <span class="score-value">{{ score }}</span>
    </el-progress>
    <span class="score-label">{{ label }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    score: number
    label: string
    size?: number
    stroke?: number
  }>(),
  { size: 64, stroke: 6 },
)

// 分数分级着色：>=80 优秀 / >=60 达标 / 其余待改进
const color = computed(() => {
  if (props.score >= 80) return 'var(--success)'
  if (props.score >= 60) return 'var(--primary)'
  return 'var(--warning)'
})
</script>

<style scoped lang="scss">
.score-ring {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.score-value {
  font-size: 16px;
  font-weight: 700;
  color: var(--ink);
}

.score-label {
  font-size: 12px;
  color: var(--muted);
}
</style>
