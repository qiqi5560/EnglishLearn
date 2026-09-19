<template>
  <div class="peek-stage" :style="{ '--mascot': `${size}px` }" aria-hidden="true">
    <div class="peek-body" :class="`v-${variant}`">
      <span v-if="variant === 'robot'" class="antenna"><i></i></span>
      <span class="ear ear-l"></span>
      <span class="ear ear-r"></span>
      <div class="head">
        <span class="eye eye-l"><i></i></span>
        <span class="eye eye-r"><i></i></span>
        <span v-if="variant !== 'robot'" class="nose"></span>
        <template v-if="variant === 'cat'">
          <span class="whisker whisker-l"></span>
          <span class="whisker whisker-r"></span>
        </template>
        <span v-if="variant === 'dog'" class="tongue"></span>
        <span class="blush blush-l"></span>
        <span class="blush blush-r"></span>
        <span class="mouth"></span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 探头吉祥物：把它放进任意容器，并给该容器加 class="peek-host"。
 * 鼠标移入宿主容器时，藏在下方的小头会弹性地探出来（含眨眼、摇头等小动作）。
 * variant 可选 robot（默认机器人）/ cat（小猫）/ dog（小狗）。
 */
withDefaults(defineProps<{ size?: number; variant?: 'robot' | 'cat' | 'dog' }>(), {
  size: 54,
  variant: 'robot',
})
</script>

<style scoped lang="scss">
.peek-stage {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  height: calc(var(--mascot) * 1.12);
  padding: 0 calc(var(--mascot) * 0.2);
  overflow: hidden;
  pointer-events: none;
}

.peek-body {
  position: relative;
  width: var(--mascot);
  height: calc(var(--mascot) * 1.12);
  transform: translateY(30%);
  transition: transform 0.55s cubic-bezier(0.34, 1.56, 0.64, 1);
}

// 宿主容器 hover 时探头（子选择器加 scope 后仍可跨组件匹配宿主）
.peek-host:hover .peek-body,
.peek-host:focus-within .peek-body {
  transform: translateY(0);
}

.antenna {
  position: absolute;
  top: 0;
  left: 50%;
  width: 2px;
  height: calc(var(--mascot) * 0.26);
  background: rgba(59, 111, 224, 0.45);
  border-radius: 2px;
  transform: translateX(-50%);

  i {
    position: absolute;
    top: -3px;
    left: 50%;
    width: calc(var(--mascot) * 0.13);
    height: calc(var(--mascot) * 0.13);
    background: radial-gradient(circle at 34% 30%, #ffb3a0, #ff6a4d 62%);
    border-radius: 50%;
    transform: translateX(-50%);
    box-shadow: 0 0 0 0 rgba(255, 106, 77, 0.5);
  }
}

.peek-host:hover .antenna i {
  animation: peek-pulse 1.1s ease-in-out infinite;
}

.ear {
  position: absolute;
  bottom: calc(var(--mascot) * 0.38);
  width: calc(var(--mascot) * 0.24);
  height: calc(var(--mascot) * 0.24);
  background: linear-gradient(150deg, #ffffff, #bed5fb);
  border: 1.5px solid rgba(59, 111, 224, 0.4);
  border-radius: 50%;
  z-index: 0;
}

.ear-l {
  left: calc(var(--mascot) * -0.12);
}

.ear-r {
  right: calc(var(--mascot) * -0.12);
}

.head {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: calc(var(--mascot) * 0.86);
  background: linear-gradient(168deg, #ffffff, #dbe7fd 58%, #bed5fb);
  border: 1.5px solid rgba(59, 111, 224, 0.42);
  border-radius: 46% 46% 44% 44%;
  box-shadow:
    0 -3px 14px rgba(59, 111, 224, 0.18),
    inset 0 -8px 14px rgba(59, 111, 224, 0.12);
  z-index: 1;
}

.peek-host:hover .head {
  animation: peek-bob 2.4s ease-in-out 0.6s infinite;
}

.eye {
  position: absolute;
  top: 26%;
  width: calc(var(--mascot) * 0.13);
  height: calc(var(--mascot) * 0.17);
  background: #2f5bb3;
  border-radius: 50%;
  display: grid;
  place-items: center;
  transform-origin: center;

  i {
    width: 44%;
    height: 34%;
    margin: -20% 0 0 -20%;
    background: #fff;
    border-radius: 50%;
  }
}

.eye-l {
  left: 26%;
}

.eye-r {
  right: 26%;
}

.peek-host:hover .eye {
  animation: peek-blink 3.2s ease-in-out infinite;
}

.blush {
  position: absolute;
  top: 50%;
  width: calc(var(--mascot) * 0.15);
  height: calc(var(--mascot) * 0.1);
  background: rgba(255, 106, 77, 0.35);
  border-radius: 50%;
  filter: blur(1px);
}

.blush-l {
  left: 12%;
}

.blush-r {
  right: 12%;
}

.mouth {
  position: absolute;
  left: 50%;
  top: 63%;
  width: calc(var(--mascot) * 0.2);
  height: calc(var(--mascot) * 0.1);
  background: rgba(47, 91, 179, 0.5);
  border-radius: 0 0 999px 999px;
  transform: translateX(-50%);
}

// ------------------------------------------------------------
// 动物变体：小猫 / 小狗
// ------------------------------------------------------------
.nose {
  position: absolute;
  left: 50%;
  top: 45%;
  width: calc(var(--mascot) * 0.11);
  height: calc(var(--mascot) * 0.08);
  background: #ff9db3;
  border-radius: 50%;
  transform: translateX(-50%);
}

// --- 小猫：三角形立耳 + 胡须 ---
.v-cat .head {
  background: linear-gradient(168deg, #fffaf2, #ffe2c0 58%, #ffcb95);
  border-color: rgba(206, 134, 56, 0.45);
  box-shadow:
    0 -3px 14px rgba(206, 134, 56, 0.22),
    inset 0 -8px 14px rgba(206, 134, 56, 0.12);
}

.v-cat .ear {
  top: calc(var(--mascot) * 0.1);
  bottom: auto;
  width: calc(var(--mascot) * 0.32);
  height: calc(var(--mascot) * 0.3);
  background: linear-gradient(180deg, #ffd9ab, #ffbf85);
  border: 0;
  border-radius: 0 0 26% 26%;
  clip-path: polygon(50% 0%, 100% 100%, 0% 100%);
  z-index: 2;
}

.v-cat .ear-l {
  left: calc(var(--mascot) * 0.04);
  transform: rotate(-14deg);
}

.v-cat .ear-r {
  right: calc(var(--mascot) * 0.04);
  transform: rotate(14deg);
}

.whisker {
  position: absolute;
  top: 58%;
  width: calc(var(--mascot) * 0.2);
  height: 1px;
  background: rgba(150, 106, 60, 0.5);
  border-radius: 2px;
}

.whisker-l {
  left: calc(var(--mascot) * -0.15);
  transform: rotate(-7deg);
}

.whisker-r {
  right: calc(var(--mascot) * -0.15);
  transform: rotate(7deg);
}

// --- 小狗：垂耳 + 舌头 ---
.v-dog .head {
  background: linear-gradient(168deg, #fffdf7, #f6e5c8 58%, #e8cfa4);
  border-color: rgba(174, 130, 62, 0.45);
  box-shadow:
    0 -3px 14px rgba(174, 130, 62, 0.22),
    inset 0 -8px 14px rgba(174, 130, 62, 0.12);
}

.v-dog .ear {
  bottom: calc(var(--mascot) * 0.18);
  width: calc(var(--mascot) * 0.25);
  height: calc(var(--mascot) * 0.42);
  background: linear-gradient(180deg, #e6c99f, #c9a672);
  border-color: rgba(160, 118, 56, 0.5);
  border-radius: 50% 50% 46% 46%;
  z-index: 2;
}

.v-dog .ear-l {
  left: calc(var(--mascot) * -0.07);
}

.v-dog .ear-r {
  right: calc(var(--mascot) * -0.07);
}

.tongue {
  position: absolute;
  left: 50%;
  top: 73%;
  width: calc(var(--mascot) * 0.14);
  height: calc(var(--mascot) * 0.13);
  background: #ff8fa3;
  border-radius: 0 0 46% 46%;
  transform: translateX(-50%);
}

.v-cat .blush-l,
.v-dog .blush-l {
  left: 14%;
}

.v-cat .blush-r,
.v-dog .blush-r {
  right: 14%;
}

.peek-host:hover .tongue {
  animation: peek-tongue 1.4s ease-in-out infinite;
}

@keyframes peek-tongue {
  0%,
  100% {
    height: calc(var(--mascot) * 0.13);
  }
  50% {
    height: calc(var(--mascot) * 0.18);
  }
}

@keyframes peek-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(255, 106, 77, 0.5);
  }
  50% {
    box-shadow: 0 0 0 5px rgba(255, 106, 77, 0);
  }
}

@keyframes peek-bob {
  0%,
  100% {
    transform: rotate(-2.5deg);
  }
  50% {
    transform: rotate(2.5deg);
  }
}

@keyframes peek-blink {
  0%,
  92%,
  100% {
    transform: scaleY(1);
  }
  95% {
    transform: scaleY(0.12);
  }
}

@media (prefers-reduced-motion: reduce) {
  .peek-body,
  .peek-host:hover .antenna i,
  .peek-host:hover .head,
  .peek-host:hover .eye {
    animation: none;
    transition: none;
  }
}
</style>