<template>
  <router-view />
</template>

<script setup lang="ts">
import { watchEffect } from 'vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()

// 全年龄段模式：切换根节点 class，驱动 CSS 变量覆盖（F009）
watchEffect(() => {
  const el = document.documentElement
  el.classList.toggle('mode-child', appStore.ageMode === 'child')
  el.classList.toggle('mode-senior', appStore.ageMode === 'senior')
})
</script>
