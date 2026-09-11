<template>
  <div class="audio-wave" :class="{ recording }">
    <span v-for="i in 24" :key="i" class="wave-bar" :style="{ height: `${barHeight(i)}%` }" />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  recording?: boolean
}>()

// 静态模拟波形；接入 ASR 后替换为真实音量数据
function barHeight(i: number) {
  const base = Math.abs(Math.sin(i * 0.7)) * 80 + 12
  return base
}
</script>

<style scoped lang="scss">
.audio-wave {
  display: flex;
  align-items: center;
  gap: 3px;
  height: 40px;

  .wave-bar {
    flex: 1;
    background: var(--primary);
    border-radius: 2px;
    transition: background 0.2s;
  }

  &.recording .wave-bar {
    background: var(--accent);
  }
}
</style>
