<template>
  <canvas ref="canvasRef" class="fireworks-canvas" aria-hidden="true"></canvas>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * 烟花绽放：完成一次阅读/跟读后调用 burst()。
 * 纯 Canvas 粒子实现（无第三方依赖），覆盖全屏但 pointer-events: none，不影响操作。
 */
const props = withDefaults(defineProps<{ shells?: number }>(), { shells: 9 })

const canvasRef = ref<HTMLCanvasElement | null>(null)

type Particle = {
  x: number
  y: number
  px: number
  py: number
  vx: number
  vy: number
  life: number
  maxLife: number
  size: number
  color: string
}

type Rocket = {
  x: number
  y: number
  vx: number
  vy: number
  targetY: number
  color: string
}

/** 爆炸瞬间的光晕闪光 */
type Flash = {
  x: number
  y: number
  life: number
  maxLife: number
  color: string
  radius: number
}

const PALETTE = [
  '#3b6fe0', '#5b8bf0', '#ff6a4d', '#ffb347', '#22a06b',
  '#a06ae0', '#ff8fb1', '#ffffff', '#38d6c4', '#ffd166',
]

let ctx: CanvasRenderingContext2D | null = null
let raf = 0
let particles: Particle[] = []
let rockets: Rocket[] = []
let flashes: Flash[] = []
let running = false
/** 尚未发射的烟花弹数量：避免首帧还没有粒子时动画提前结束 */
let pending = 0

function resize() {
  const canvas = canvasRef.value
  if (!canvas) return
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  canvas.width = Math.floor(window.innerWidth * dpr)
  canvas.height = Math.floor(window.innerHeight * dpr)
  canvas.style.width = `${window.innerWidth}px`
  canvas.style.height = `${window.innerHeight}px`
  ctx = canvas.getContext('2d')
  ctx?.setTransform(dpr, 0, 0, dpr, 0, 0)
}

function launchRocket() {
  const w = window.innerWidth
  const h = window.innerHeight
  const x = w * (0.12 + Math.random() * 0.76)
  const targetY = h * (0.08 + Math.random() * 0.36)
  rockets.push({
    x,
    y: h + 8,
    vx: (Math.random() - 0.5) * 1.4,
    vy: -(11 + Math.random() * 4.2),
    targetY,
    color: PALETTE[Math.floor(Math.random() * PALETTE.length)],
  })
}

function explode(x: number, y: number, color: string) {
  const count = 96 + Math.floor(Math.random() * 56)
  const power = 4.1 + Math.random() * 2.7
  for (let i = 0; i < count; i += 1) {
    const angle = (Math.PI * 2 * i) / count + Math.random() * 0.18
    const speed = power * (0.36 + Math.random() * 0.94)
    const maxLife = 84 + Math.random() * 60
    particles.push({
      x,
      y,
      px: x,
      py: y,
      vx: Math.cos(angle) * speed,
      vy: Math.sin(angle) * speed,
      life: maxLife,
      maxLife,
      size: 2 + Math.random() * 2.6,
      color: Math.random() < 0.24 ? '#ffffff' : color,
    })
  }
  // 爆炸瞬间的光晕：让整屏亮一下，烟花更抢眼
  flashes.push({
    x,
    y,
    life: 18,
    maxLife: 18,
    color,
    radius: power * 26,
  })
}

function frame() {
  const canvas = canvasRef.value
  if (!canvas || !ctx) return

  ctx.clearRect(0, 0, window.innerWidth, window.innerHeight)
  ctx.globalCompositeOperation = 'lighter'

  // 上升的烟花弹
  for (let i = rockets.length - 1; i >= 0; i -= 1) {
    const r = rockets[i]
    r.x += r.vx
    r.y += r.vy
    r.vy += 0.14

    ctx.beginPath()
    ctx.fillStyle = r.color
    ctx.arc(r.x, r.y, 2.8, 0, Math.PI * 2)
    ctx.fill()

    // 拖尾
    ctx.beginPath()
    ctx.strokeStyle = r.color
    ctx.globalAlpha = 0.55
    ctx.lineWidth = 2.2
    ctx.moveTo(r.x, r.y)
    ctx.lineTo(r.x - r.vx * 5, r.y - r.vy * 5)
    ctx.stroke()
    ctx.globalAlpha = 1

    if (r.y <= r.targetY || r.vy >= 0) {
      explode(r.x, r.y, r.color)
      rockets.splice(i, 1)
    }
  }

  // 爆炸光晕
  for (let i = flashes.length - 1; i >= 0; i -= 1) {
    const f = flashes[i]
    f.life -= 1
    if (f.life <= 0) {
      flashes.splice(i, 1)
      continue
    }
    const t = f.life / f.maxLife
    const radius = f.radius * (1.25 - t * 0.25)
    const grad = ctx.createRadialGradient(f.x, f.y, 0, f.x, f.y, radius)
    grad.addColorStop(0, hexToRgba(f.color, 0.5 * t))
    grad.addColorStop(0.45, hexToRgba(f.color, 0.16 * t))
    grad.addColorStop(1, hexToRgba(f.color, 0))
    ctx.globalAlpha = 1
    ctx.fillStyle = grad
    ctx.beginPath()
    ctx.arc(f.x, f.y, radius, 0, Math.PI * 2)
    ctx.fill()
  }

  // 爆炸粒子
  for (let i = particles.length - 1; i >= 0; i -= 1) {
    const p = particles[i]
    p.px = p.x
    p.py = p.y
    p.x += p.vx
    p.y += p.vy
    p.vy += 0.052
    p.vx *= 0.987
    p.vy *= 0.987
    p.life -= 1

    if (p.life <= 0) {
      particles.splice(i, 1)
      continue
    }
    const alpha = Math.max(0, p.life / p.maxLife)
    const core = p.size * (0.45 + alpha * 0.7)

    // 光轨：拉出运动方向的亮线，形成流星般的拖尾
    ctx.globalAlpha = alpha * 0.5
    ctx.strokeStyle = p.color
    ctx.lineWidth = core * 0.9
    ctx.lineCap = 'round'
    ctx.beginPath()
    ctx.moveTo(p.px, p.py)
    ctx.lineTo(p.x, p.y)
    ctx.stroke()

    // 外圈柔光：让粒子更亮更“炸”
    ctx.globalAlpha = alpha * 0.22
    ctx.fillStyle = p.color
    ctx.beginPath()
    ctx.arc(p.x, p.y, core * 2.8, 0, Math.PI * 2)
    ctx.fill()

    // 核心
    ctx.globalAlpha = alpha
    ctx.fillStyle = p.color
    ctx.beginPath()
    ctx.arc(p.x, p.y, core, 0, Math.PI * 2)
    ctx.fill()
  }

  ctx.globalAlpha = 1
  ctx.globalCompositeOperation = 'source-over'

  if (rockets.length || particles.length || flashes.length || pending > 0) {
    raf = requestAnimationFrame(frame)
  } else {
    running = false
    canvas.style.opacity = '0'
  }
}

/** #rrggbb → rgba(...)，供径向渐变使用 */
function hexToRgba(hex: string, alpha: number) {
  const h = hex.replace('#', '')
  const full = h.length === 3 ? h.split('').map((c) => c + c).join('') : h
  const num = parseInt(full, 16)
  const r = (num >> 16) & 255
  const g = (num >> 8) & 255
  const b = num & 255
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

function start() {
  if (running) return
  running = true
  const canvas = canvasRef.value
  if (canvas) canvas.style.opacity = '1'
  raf = requestAnimationFrame(frame)
}

/** 绽放烟花：默认连发 shells 枚 */
function burst(shells = props.shells) {
  start()
  for (let i = 0; i < shells; i += 1) {
    pending += 1
    window.setTimeout(() => {
      pending -= 1
      launchRocket()
    }, i * 170 + Math.random() * 110)
  }
}

defineExpose({ burst })

onMounted(() => {
  resize()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  cancelAnimationFrame(raf)
  particles = []
  rockets = []
  flashes = []
})
</script>

<style scoped lang="scss">
.fireworks-canvas {
  position: fixed;
  inset: 0;
  z-index: 2000;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.18s ease-in;
}

@media (prefers-reduced-motion: reduce) {
  .fireworks-canvas {
    display: none;
  }
}
</style>
