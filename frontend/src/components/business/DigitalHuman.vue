<template>
  <div class="digital-human">
    <div class="human-stage">
      <span class="halo halo-outer" aria-hidden="true"></span>
      <span class="halo halo-inner" aria-hidden="true"></span>
      <div class="human-avatar">
        <el-icon :size="38"><MagicStick /></el-icon>
      </div>
      <span class="live-dot" aria-hidden="true"></span>
    </div>
    <p class="human-hint">AI 数字人</p>
    <p class="human-sub text-muted">{{ role || '教师' }}</p>
  </div>
</template>

<script setup lang="ts">
defineProps<{ role?: string }>()
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

.digital-human {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 6px 0 2px;
  text-align: center;
}

.human-stage {
  position: relative;
  display: grid;
  place-items: center;
  width: 116px;
  height: 116px;
}

.halo {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.halo-outer {
  inset: 0;
  border: 1px solid rgba(59, 111, 224, 0.22);
  animation: halo-breathe 4.6s $ease-apple infinite;
}

.halo-inner {
  inset: 12px;
  background: radial-gradient(circle at 34% 28%, rgba(59, 111, 224, 0.16), rgba(59, 111, 224, 0) 68%);
  animation: halo-breathe 4.6s $ease-apple infinite 0.6s;
}

.human-avatar {
  position: relative;
  z-index: 1;
  width: 82px;
  height: 82px;
  display: grid;
  place-items: center;
  color: #fff;
  border-radius: 50%;
  background: linear-gradient(150deg, var(--primary), var(--ink));
  box-shadow:
    0 14px 30px rgba(59, 111, 224, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.34);
  transition: transform 0.55s $ease-spring, box-shadow 0.5s $ease-apple;
}

.digital-human:hover .human-avatar {
  transform: translateY(-4px) scale(1.06) rotate(-4deg);
  box-shadow: 0 22px 42px rgba(59, 111, 224, 0.4);
}

.live-dot {
  position: absolute;
  right: 16px;
  bottom: 16px;
  z-index: 2;
  width: 13px;
  height: 13px;
  border-radius: 50%;
  background: var(--success);
  box-shadow:
    0 0 0 3px var(--card),
    0 0 0 0 rgba(34, 160, 107, 0.5);
  animation: live-pulse 2.2s ease-out infinite;
}

.human-hint {
  margin: 12px 0 0;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--ink);
}

.human-sub {
  margin: 3px 0 0;
  font-size: 12.5px;
}

@keyframes halo-breathe {
  0%,
  100% {
    transform: scale(0.94);
    opacity: 0.55;
  }
  50% {
    transform: scale(1.06);
    opacity: 1;
  }
}

@keyframes live-pulse {
  0% {
    box-shadow:
      0 0 0 3px var(--card),
      0 0 0 0 rgba(34, 160, 107, 0.5);
  }
  70% {
    box-shadow:
      0 0 0 3px var(--card),
      0 0 0 12px rgba(34, 160, 107, 0);
  }
  100% {
    box-shadow:
      0 0 0 3px var(--card),
      0 0 0 0 rgba(34, 160, 107, 0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .halo-outer,
  .halo-inner,
  .live-dot {
    animation: none;
  }
}
</style>