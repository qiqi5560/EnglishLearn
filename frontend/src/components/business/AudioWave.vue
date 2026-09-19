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
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);

.audio-wave {
  display: flex;
  align-items: center;
  gap: 3px;
  height: 40px;

  .wave-bar {
    flex: 1;
    min-width: 2px;
    background: var(--primary);
    border-radius: 999px;
    opacity: 0.85;
    transition: background 0.2s, opacity 0.2s, transform 0.35s $ease-apple;
  }

  &.recording .wave-bar {
    background: var(--accent);
    opacity: 1;
    transform-origin: center;
    animation: wave-dance 1s ease-in-out infinite alternate;
  }
}

// 录音时逐条错峰律动（高度仍沿用静态模拟逻辑）
@for $i from 1 through 24 {
  .audio-wave.recording .wave-bar:nth-child(#{$i}) {
    animation-delay: -$i * 0.05s;
  }
}

@keyframes wave-dance {
  from {
    transform: scaleY(0.5);
  }
  to {
    transform: scaleY(1.15);
  }
}

@media (prefers-reduced-motion: reduce) {
  .audio-wave.recording .wave-bar {
    animation: none;
  }
}
</style>