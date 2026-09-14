<template>
  <article class="scene-card" tabindex="0" role="button" @click="$emit('click')" @keydown.enter="$emit('click')">
    <div class="scene-cover" :style="coverStyle"><div class="scene-orb">{{ name.slice(0, 1) }}</div><LevelTag :level="level" /></div>
    <div class="scene-info"><div class="scene-kicker">{{ category }} · {{ role }}</div><div class="scene-name">{{ name }}</div><div class="scene-desc text-muted">{{ desc }}</div><div class="scene-actions"><button type="button" class="plan-button" :class="{ added }" :disabled="added || adding" @click.stop="$emit('add-to-plan')">{{ adding ? '加入中…' : added ? '已加入' : '加入计划' }}</button><span class="scene-arrow"><el-icon><ArrowRight /></el-icon></span></div></div>
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
.scene-card { overflow: hidden; min-width: 0; background: #fff; border: 1px solid #e8e8ed; border-radius: 20px; cursor: pointer; transition: transform .2s ease, box-shadow .2s ease; }.scene-card:hover { transform: translateY(-3px); box-shadow: 0 14px 30px rgba(0,0,0,.08); }.scene-card:focus-visible { outline: 2px solid #0071e3; outline-offset: 3px; }.scene-cover { position: relative; display: flex; align-items: flex-start; justify-content: space-between; height: 118px; padding: 14px; background: linear-gradient(135deg, #eaf3ff, #d9e9ff); background-position: center; background-size: cover; }.scene-orb { display: grid; place-items: center; width: 48px; height: 48px; color: #fff; background: #0071e3; border-radius: 16px; font-size: 23px; font-weight: 700; }.scene-info { position: relative; padding: 15px 16px 17px; }.scene-kicker { overflow: hidden; color: #86868b; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }.scene-name { margin-top: 7px; overflow: hidden; color: #171717; font-size: 18px; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }.scene-desc { display: -webkit-box; min-height: 35px; margin-top: 5px; overflow: hidden; font-size: 12px; line-height: 1.45; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }.scene-actions { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-top: 13px; }.plan-button { padding: 6px 10px; color: #0071e3; background: #eef6ff; border: 0; border-radius: 999px; font: inherit; font-size: 11px; cursor: pointer; }.plan-button.added { color: #16845b; background: #eaf8f0; cursor: default; }.plan-button:disabled { opacity: .9; }.scene-arrow { color: #0071e3; font-size: 18px; }
</style>
