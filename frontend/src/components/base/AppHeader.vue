<template>
  <header ref="headerRef" class="page-header" :class="{ 'is-hidden': hidden }">
    <div class="page-header-inner">
      <div class="header-left">
        <button v-if="back" class="back-pill" type="button" @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回</span>
        </button>
        <h1 class="header-title">{{ title }}</h1>
      </div>
      <div class="header-right">
        <slot name="right" />
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

defineProps<{
  title: string
  back?: boolean
}>()

/**
 * 页头在滚动容器里吸顶：只要离开页面顶部就收起，滑回顶部才重新滑出。
 * 用 sticky 保持占位，收起时不会在顶部留下空白。
 * 注意：本项目的滚动发生在 .app-main 内部，不是 window，所以监听的是最近的滚动祖先。
 */
const headerRef = ref<HTMLElement | null>(null)
const hidden = ref(false)

let scroller: HTMLElement | Window = window
let lastY = 0
let ticking = false

const TOP_ZONE = 48 // 回到这个位置以内才显示
const DELTA = 4 // 忽略细碎抖动

function readY() {
  return scroller instanceof HTMLElement ? scroller.scrollTop : window.scrollY || 0
}

function update() {
  ticking = false
  const y = readY()
  // 只在「确实滑到页面顶部」时才显示；往下滑或往上滑都保持收起
  if (y <= TOP_ZONE) {
    hidden.value = false
  } else if (y - lastY > DELTA) {
    hidden.value = true
  }
  lastY = y
}

function onScroll() {
  if (ticking) return
  ticking = true
  requestAnimationFrame(update)
}

/** 向上找到最近的滚动容器 */
function findScroller(el: HTMLElement | null): HTMLElement | Window {
  let node = el?.parentElement ?? null
  while (node) {
    const oy = getComputedStyle(node).overflowY
    if (oy === 'auto' || oy === 'scroll') return node
    node = node.parentElement
  }
  return window
}

onMounted(() => {
  scroller = findScroller(headerRef.value)
  lastY = readY()
  scroller.addEventListener('scroll', onScroll, { passive: true })
})

onBeforeUnmount(() => {
  scroller.removeEventListener('scroll', onScroll)
})
</script>

<style scoped lang="scss">
// 桌面端页头：定宽居中 + 毛玻璃吸顶（替代原 52px 手机小标题栏）
// 下滑时整体上移收起（sticky 保持占位，无空白），回顶/上滑再滑出
.page-header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(246, 247, 251, 0.82);
  border-bottom: 1px solid rgba(31, 42, 68, 0.06);
  backdrop-filter: blur(16px) saturate(160%);
  -webkit-backdrop-filter: blur(16px) saturate(160%);
  transition:
    transform 0.5s cubic-bezier(0.22, 1, 0.36, 1),
    opacity 0.36s ease;
  will-change: transform;

  &.is-hidden {
    transform: translateY(-104%);
    opacity: 0;
    pointer-events: none;
  }
}

.page-header-inner {
  width: min(1180px, calc(100% - 80px));
  min-height: 66px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.back-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 14px 7px 11px;
  color: #55617e;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(31, 42, 68, 0.08);
  border-radius: 999px;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition:
    transform 0.35s cubic-bezier(0.22, 1, 0.36, 1),
    color 0.28s ease,
    border-color 0.28s ease,
    box-shadow 0.35s ease;

  .el-icon {
    font-size: 15px;
    transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  }

  &:hover {
    color: var(--primary);
    border-color: rgba(59, 111, 224, 0.3);
    transform: translateX(-2px);
    box-shadow: 0 10px 22px rgba(31, 42, 68, 0.1);

    .el-icon {
      transform: translateX(-3px);
    }
  }
}

.header-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.03em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

@media (max-width: 900px) {
  .page-header-inner {
    width: calc(100% - 32px);
    min-height: 58px;
  }

  .header-title {
    font-size: 18px;
  }

  .back-pill span {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .page-header {
    transition: none;
  }
}
</style>