<template>
  <el-card class="scene-card" shadow="hover" :body-style="{ padding: '0' }" @click="$emit('click')">
    <div class="scene-cover" :style="coverStyle">
      <LevelTag :level="level" />
      <span class="cover-role">{{ role }}</span>
    </div>
    <div class="scene-info">
      <div class="scene-name">{{ name }}</div>
      <div class="scene-desc text-muted">{{ desc }}</div>
      <el-tag size="small" effect="plain">{{ category }}</el-tag>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import LevelTag from './LevelTag.vue'

const props = defineProps<{
  name: string
  desc: string
  category: string
  level: string
  role: string
  cover?: string
}>()

defineEmits<{ (e: 'click'): void }>()

// 无封面时使用分级渐变占位
const coverStyle = computed(() =>
  props.cover
    ? { backgroundImage: `url(${props.cover})` }
    : { background: 'linear-gradient(135deg, #3b6fe0 0%, #6c93e8 60%, #9bb5f1 100%)' },
)
</script>

<style scoped lang="scss">
.scene-card {
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
}

.scene-cover {
  position: relative;
  height: 96px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 10px;
  background-size: cover;
  background-position: center;
}

.cover-role {
  color: #fff;
  font-size: 12px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.scene-info {
  padding: 12px;

  .scene-name {
    font-size: 15px;
    font-weight: 700;
    margin-bottom: 4px;
  }

  .scene-desc {
    font-size: 12px;
    margin-bottom: 8px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}
</style>
