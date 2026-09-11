<template>
  <header class="app-header">
    <div class="header-left">
      <el-button v-if="back" link class="header-back" @click="$router.back()">
        <el-icon :size="20"><ArrowLeft /></el-icon>
      </el-button>
      <span class="header-title">{{ title }}</span>
    </div>
    <div class="header-right">
      <slot name="right" />
    </div>
  </header>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  back?: boolean
}>()
</script>

<style scoped lang="scss">
.app-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 52px;
  padding: 0 16px;
  background: var(--card);
  border-bottom: 1px solid var(--border);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

.header-title {
  font-size: 17px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
