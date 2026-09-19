<template>
  <article class="resource-card" tabindex="0" role="button" @click="$emit('click')" @keydown.enter="$emit('click')">
    <div class="resource-icon"><el-icon><Headset /></el-icon></div>
    <div class="resource-content">
      <div class="resource-kicker">{{ type }} · {{ category }}</div>
      <div class="resource-title">{{ title }}</div>
      <div class="resource-meta">
        <LevelTag :level="level" />
        <span v-if="durationText" class="text-muted">{{ durationText }}</span>
      </div>
    </div>
    <el-icon class="resource-arrow"><ArrowRight /></el-icon>
  </article>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import LevelTag from './LevelTag.vue'
const props = defineProps<{ title: string; type: string; category: string; level: string; durationSec?: number }>()
defineEmits<{ (e: 'click'): void }>()
const durationText = computed(() => { if (!props.durationSec) return ''; const m = Math.floor(props.durationSec / 60); const s = props.durationSec % 60; return `${m}:${String(s).padStart(2, '0')}` })
</script>
<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

.resource-card {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 15px;
  min-width: 0;
  padding: 20px;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition:
    transform 0.35s $ease-apple,
    box-shadow 0.35s $ease-apple,
    border-color 0.3s ease;

  &:hover {
    transform: translateY(-4px);
    border-color: rgba(59, 111, 224, 0.26);
    box-shadow: 0 18px 38px rgba(31, 42, 68, 0.12);

    .resource-icon {
      transform: rotate(-6deg) scale(1.08);
    }

    .resource-arrow {
      transform: translateX(4px);
    }
  }

  &:focus-visible {
    outline: 2px solid var(--primary);
    outline-offset: 3px;
  }
}

.resource-icon {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 46px;
  height: 46px;
  color: var(--primary);
  font-size: 20px;
  background: rgba(59, 111, 224, 0.1);
  border-radius: var(--radius-md);
  transition: transform 0.5s $ease-spring;
}

.resource-content {
  min-width: 0;
  padding-right: 20px;
}

.resource-kicker {
  overflow: hidden;
  color: var(--muted);
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-title {
  margin: 7px 0 12px;
  overflow: hidden;
  color: var(--ink);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.01em;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.resource-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
}

.resource-arrow {
  position: absolute;
  top: 20px;
  right: 18px;
  color: var(--primary);
  font-size: 17px;
  transition: transform 0.45s $ease-spring;
}
</style>