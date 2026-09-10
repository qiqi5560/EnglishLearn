<template>
  <div class="digital-human">
    <div class="human-placeholder">
      <el-icon :size="56"><MagicStick /></el-icon>
      <p class="human-hint">AI 数字人</p>
      <p class="human-sub text-muted">{{ role || '教师' }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ role?: string }>()
</script>

<style scoped lang="scss">
.digital-human {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.human-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: var(--muted);
}

.human-hint {
  margin: 8px 0 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}

.human-sub {
  margin: 2px 0 0;
  font-size: 12px;
}
</style>
