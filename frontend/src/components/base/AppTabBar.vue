<template>
  <nav class="tab-bar">
    <router-link
      v-for="tab in tabs"
      :key="tab.path"
      :to="tab.path"
      class="tab-item"
      :class="{ active: route.name === tab.name }"
    >
      <el-icon :size="22"><component :is="tab.icon" /></el-icon>
      <span class="tab-label">{{ tab.label }}</span>
    </router-link>
  </nav>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()

const tabs = [
  { path: '/home', name: 'Home', label: '首页', icon: 'House' },
  { path: '/practice', name: 'Practice', label: '练习', icon: 'Microphone' },
  { path: '/community', name: 'Community', label: '社区', icon: 'ChatDotRound' },
  { path: '/report', name: 'Report', label: '报表', icon: 'TrendCharts' },
  { path: '/profile', name: 'Profile', label: '我的', icon: 'User' },
]
</script>

<style scoped lang="scss">
.tab-bar {
  flex-shrink: 0;
  display: flex;
  height: 56px;
  background: var(--card);
  border-top: 1px solid var(--border);
  padding-bottom: env(safe-area-inset-bottom);
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: var(--muted);
  transition: color 0.2s;

  .tab-label {
    font-size: 11px;
  }

  &.active {
    color: var(--primary);
    font-weight: 600;
  }
}
</style>
