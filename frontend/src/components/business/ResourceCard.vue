<template>
  <article class="resource-card" tabindex="0" role="button" @click="$emit('click')" @keydown.enter="$emit('click')">
    <div class="resource-icon"><el-icon><Headset /></el-icon></div>
    <div class="resource-content"><div class="resource-kicker">{{ type }} · {{ category }}</div><div class="resource-title">{{ title }}</div><div class="resource-meta"><LevelTag :level="level" /><span class="text-muted">{{ durationText }}</span></div></div>
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
.resource-card { position: relative; display: flex; align-items: flex-start; gap: 14px; min-width: 0; padding: 18px; background: #fff; border: 1px solid #e8e8ed; border-radius: 18px; cursor: pointer; transition: transform .2s ease, box-shadow .2s ease; }.resource-card:hover { transform: translateY(-2px); box-shadow: 0 12px 24px rgba(0,0,0,.07); }.resource-card:focus-visible { outline: 2px solid #0071e3; outline-offset: 3px; }.resource-icon { display: grid; flex: 0 0 auto; place-items: center; width: 42px; height: 42px; color: #0071e3; background: #eaf3ff; border-radius: 13px; }.resource-content { min-width: 0; padding-right: 18px; }.resource-kicker { overflow: hidden; color: #86868b; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }.resource-title { display: -webkit-box; margin: 6px 0 12px; overflow: hidden; color: #171717; font-size: 15px; font-weight: 700; line-height: 1.35; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }.resource-meta { display: flex; align-items: center; gap: 10px; font-size: 12px; }.resource-arrow { position: absolute; top: 18px; right: 16px; color: #0071e3; font-size: 17px; }
</style>
