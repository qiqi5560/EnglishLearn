<template>
  <div class="app-layout">
    <main class="app-main">
      <router-view />
    </main>
    <AppTabBar v-if="showTabBar" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppTabBar from '@/components/base/AppTabBar.vue'

const route = useRoute()

// 底部 Tab 仅在 5 个主 Tab 页显示，压栈子页面（对话/测试/详情）全屏
const TAB_ROUTES = ['Home', 'Practice', 'Community', 'Report', 'Profile']
const showTabBar = computed(() => TAB_ROUTES.includes(route.name as string))
</script>

<style scoped lang="scss">
.app-layout {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.app-main {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}
</style>
