<template>
  <div class="chat-notice" role="note">
    <el-icon :size="14"><WarningFilled /></el-icon>
    <span class="notice-text">{{ text }}</span>
    <span v-if="time" class="notice-time">{{ time }}</span>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  /** 提示文案，如「该消息含不当用语，已被撤回」 */
  text: string
  time?: string
}>()
</script>

<style scoped lang="scss">
.chat-notice {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 4px auto 20px;
  padding: 7px 16px;
  max-width: 78%;
  border-radius: 999px;
  font-size: 12.5px;
  line-height: 1.5;
  color: var(--muted);
  background: rgba(31, 42, 68, 0.05);
  border: 1px dashed var(--border);
  animation: notice-in 0.28s cubic-bezier(0.22, 1, 0.36, 1);

  .el-icon {
    flex-shrink: 0;
    opacity: 0.7;
  }
}

.notice-text {
  text-align: center;
}

.notice-time {
  flex-shrink: 0;
  font-size: 11.5px;
  opacity: 0.65;
}

@keyframes notice-in {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
