<template>
  <article class="scene-card" tabindex="0" role="button" @click="$emit('click')" @keydown.enter="$emit('click')">
    <div class="scene-cover">
      <span class="cover-art" :style="coverStyle"></span>
      <div class="scene-orb">{{ name.slice(0, 1) }}</div>
      <span class="cover-tag"><LevelTag :level="level" /></span>
    </div>
    <div class="scene-info">
      <div class="scene-kicker">{{ category }} · {{ role }}</div>
      <div class="scene-name">{{ name }}</div>
      <div class="scene-desc text-muted">{{ desc }}</div>
      <div class="scene-actions">
        <button type="button" class="plan-button" :class="{ added }" :disabled="added || adding" @click.stop="$emit('add-to-plan')">{{ adding ? '加入中…' : added ? '已加入' : '加入计划' }}</button>
        <span class="scene-arrow"><el-icon><ArrowRight /></el-icon></span>
      </div>
    </div>
  </article>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import LevelTag from './LevelTag.vue'
const props = defineProps<{ name: string; desc: string; category: string; level: string; role: string; cover?: string; added?: boolean; adding?: boolean }>()
defineEmits<{ (e: 'click'): void; (e: 'add-to-plan'): void }>()
const coverStyle = computed(() => props.cover ? { backgroundImage: `url(${props.cover})` } : {})
</script>
<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

.scene-card {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition:
    transform 0.35s $ease-apple,
    box-shadow 0.35s $ease-apple,
    border-color 0.3s ease;

  &:hover {
    transform: translateY(-6px);
    border-color: rgba(59, 111, 224, 0.28);
    box-shadow: 0 22px 46px rgba(31, 42, 68, 0.14);

    .cover-art {
      transform: scale(1.07);
    }

    .scene-orb {
      transform: rotate(-6deg) scale(1.06);
    }

    .scene-arrow {
      transform: translateX(4px);
    }
  }

  &:focus-visible {
    outline: 2px solid var(--primary);
    outline-offset: 3px;
  }
}

.scene-cover {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  height: 150px;
  padding: 16px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(59, 111, 224, 0.18), rgba(59, 111, 224, 0.04));
}

.cover-art {
  position: absolute;
  inset: 0;
  background-position: center;
  background-size: cover;
  transition: transform 0.7s $ease-apple;
}

.scene-orb {
  position: relative;
  z-index: 1;
  display: grid;
  place-items: center;
  width: 50px;
  height: 50px;
  color: #fff;
  font-size: 23px;
  font-weight: 700;
  background:
    linear-gradient(150deg, rgba(255, 255, 255, 0.34), rgba(255, 255, 255, 0.04)),
    var(--primary);
  border-radius: var(--radius-md);
  box-shadow: 0 10px 22px rgba(59, 111, 224, 0.3);
  transition: transform 0.5s $ease-spring;
}

.cover-tag {
  position: relative;
  z-index: 1;
}

.scene-info {
  display: flex;
  flex: 1;
  flex-direction: column;
  padding: 16px 18px 18px;
}

.scene-kicker {
  overflow: hidden;
  color: var(--muted);
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scene-name {
  margin-top: 7px;
  overflow: hidden;
  color: var(--ink);
  font-size: 19px;
  font-weight: 700;
  letter-spacing: -0.02em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scene-desc {
  min-height: 39px;
  margin-top: 6px;
  overflow: hidden;
  font-size: 13px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.scene-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 15px;
}

.plan-button {
  padding: 7px 14px;
  color: var(--primary);
  font: inherit;
  font-size: 12px;
  font-weight: 600;
  background: rgba(59, 111, 224, 0.1);
  border: 0;
  border-radius: 999px;
  cursor: pointer;
  transition:
    background 0.3s ease,
    transform 0.35s $ease-apple,
    box-shadow 0.35s ease;

  &:not(:disabled):hover {
    background: rgba(59, 111, 224, 0.18);
    transform: translateY(-1px);
    box-shadow: 0 8px 16px rgba(59, 111, 224, 0.2);
  }

  &.added {
    color: var(--success);
    background: rgba(34, 160, 107, 0.13);
    cursor: default;
  }

  &:disabled {
    opacity: 0.9;
  }
}

.scene-arrow {
  display: grid;
  place-items: center;
  color: var(--primary);
  font-size: 18px;
  transition: transform 0.45s $ease-spring;
}
</style>