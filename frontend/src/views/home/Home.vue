<template>
  <div class="home page" v-loading="loading">
    <AppHeader title="首页" />
    <main class="home-shell">
      <section class="hero-grid" aria-labelledby="home-heading">
        <div class="hero-copy">
          <p class="eyebrow">{{ todayLabel }}</p>
          <p class="greeting text-muted">Hi，{{ userStore.userInfo?.nickname || '学习者' }}</p>
          <h1 id="home-heading">今天也要<br class="mobile-break" />开口说英语</h1>
          <p class="hero-subtitle">用一点点练习，把表达变成你的自然反应。</p>
          <div class="hero-meta"><LevelTag :level="currentLevel || 'A2'" /><span class="meta-divider" aria-hidden="true"></span><span class="text-muted">今日完成 {{ doneCount }}/{{ totalTasks }}</span></div>
          <button class="primary-action" type="button" @click="onPrimaryAction"><span>{{ primaryActionLabel }}</span><el-icon><ArrowRight /></el-icon></button>
        </div>
        <div class="progress-panel" aria-label="今日学习进度">
          <div class="progress-ring" :style="progressRingStyle"><div class="progress-ring-inner"><strong>{{ progressPercent }}<small>%</small></strong><span>今日进度</span></div></div>
          <div class="progress-caption"><strong>{{ remainingLabel }}</strong><span class="text-muted">保持节奏，完成今天的学习计划</span></div>
        </div>
      </section>

      <div class="content-grid">
        <section class="task-section" aria-labelledby="tasks-heading">
          <div class="section-heading"><div><p class="eyebrow">YOUR PLAN</p><h2 id="tasks-heading">今日任务</h2></div><span class="task-count text-muted">{{ doneCount }}/{{ totalTasks }}</span></div>
          <div v-if="planStore.dailyTasks.length" class="task-list">
            <div v-for="task in planStore.dailyTasks" :key="task.taskId" class="task-row" :class="{ 'is-done': task.done }" role="button" tabindex="0" @click="onStartTask(task)" @keydown.enter="onStartTask(task)" @keydown.space.prevent="onStartTask(task)">
              <el-checkbox class="task-check" :model-value="task.done" :aria-label="`标记任务 ${task.title}`" @click.stop @change="(val: string | number | boolean) => onToggleTask(task, !!val)" />
              <div class="task-index">{{ String(task.taskId).padStart(2, '0') }}</div>
              <div class="task-main"><div class="task-title">{{ task.title }}</div><div class="task-meta"><span>{{ task.type }}</span><span class="dot">·</span><span>{{ task.durationMin }} 分钟</span></div></div>
              <el-button text class="task-cta" @click.stop="onStartTask(task)">{{ task.done ? '再练' : '开始' }}<el-icon><ArrowUpRight /></el-icon></el-button>
            </div>
          </div>
          <div v-else class="empty-plan"><div class="empty-icon"><el-icon><MagicStick /></el-icon></div><div><strong>为你制定专属学习计划</strong><p class="text-muted">完成一次测评，开始你的每日练习。</p></div><el-button type="primary" @click="$router.push('/entrance-test')">去测评</el-button></div>
        </section>

        <aside class="side-column">
          <section class="plan-link" role="button" tabindex="0" @click="$router.push('/plan')" @keydown.enter="$router.push('/plan')"><div><p class="eyebrow">YOUR GOAL</p><strong>{{ planStore.targetGoal || '还没有学习目标' }}</strong><span class="text-muted">当前等级 {{ currentLevel || '--' }}</span></div><el-icon><ArrowUpRight /></el-icon></section>
          <section class="recommend-section" aria-labelledby="recommend-heading">
            <div class="section-heading compact"><div><p class="eyebrow">CURATED FOR YOU</p><h2 id="recommend-heading">为你推荐</h2></div><el-button text type="primary" @click="$router.push('/practice')">全部</el-button></div>
            <div v-if="scenes.length" class="scene-grid">
              <article v-for="(scene, index) in scenes" :key="scene.id" class="scene-tile" :class="`tone-${index % 4}`" role="button" tabindex="0" @click="$router.push(`/scene/${scene.id}`)" @keydown.enter="$router.push(`/scene/${scene.id}`)">
                <div class="scene-topline"><span>{{ scene.category }}</span><LevelTag :level="scene.level" /></div><div class="scene-symbol" aria-hidden="true">{{ scene.name.slice(0, 1) }}</div><div class="scene-name">{{ scene.name }}</div><div class="scene-desc text-muted">{{ scene.desc }}</div><div class="scene-footer"><span>{{ scene.role || 'AI 搭档' }}</span><el-icon><ArrowUpRight /></el-icon></div>
              </article>
            </div>
            <div v-else-if="!loading" class="recommend-empty text-muted">暂时没有推荐场景</div>
          </section>
        </aside>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
import LevelTag from '@/components/business/LevelTag.vue'
import { useUserStore } from '@/stores/user'
import { usePlanStore } from '@/stores/plan'
import { useTaskJump } from '@/composables/useTaskJump'
import { recommendedScenes } from '@/api/modules/scene'
import type { DailyTaskDto, SceneDto } from '@/types/api'

const router = useRouter(); const userStore = useUserStore(); const planStore = usePlanStore(); const { startTask } = useTaskJump(); const loading = ref(false); const scenes = ref<SceneDto[]>([])
const currentLevel = computed(() => planStore.level ?? userStore.level ?? null)
const totalTasks = computed(() => planStore.dailyTasks.length)
const doneCount = computed(() => planStore.dailyTasks.filter((task) => task.done).length)
const progressPercent = computed(() => (totalTasks.value ? Math.round((doneCount.value / totalTasks.value) * 100) : 0))
const nextTask = computed(() => planStore.dailyTasks.find((task) => !task.done) || planStore.dailyTasks[0])
const primaryActionLabel = computed(() => nextTask.value ? (nextTask.value.done ? '复习今日任务' : `继续：${nextTask.value.title}`) : '开始入学测评')
const remainingLabel = computed(() => totalTasks.value ? (doneCount.value === totalTasks.value ? '今日任务已完成' : `还有 ${totalTasks.value - doneCount.value} 项待完成`) : '从测评开始')
const todayLabel = computed(() => new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }).format(new Date()))
const progressRingStyle = computed(() => ({ '--progress': `${progressPercent.value * 3.6}deg` }))
async function onToggleTask(task: DailyTaskDto, val: boolean) { try { await planStore.toggleTask(task.taskId, val) } catch { ElMessage.error('任务状态更新失败，请稍后重试') } }
async function onStartTask(task: DailyTaskDto) { try { await startTask(task) } catch { /* 请求层负责提示 */ } }
function onPrimaryAction() { if (nextTask.value) return onStartTask(nextTask.value); return router.push('/entrance-test') }
onMounted(async () => { loading.value = true; try { const [sceneRes] = await Promise.all([recommendedScenes(4), planStore.loadTodayTasks().catch(() => null), userStore.fetchMe().catch(() => null)]); scenes.value = sceneRes ?? [] } finally { loading.value = false } })
</script>

<style scoped lang="scss">
.home { --home-ink: #171717; --home-muted: #86868b; --home-line: #e8e8ed; --home-accent: #0071e3; background: #f5f5f7; }
.home.page { padding-bottom: 0; }
.home-shell { width: min(1180px, calc(100% - 40px)); margin: 0 auto; padding: 54px 0 24px; }
.hero-grid { display: grid; grid-template-columns: minmax(0, 1.15fr) minmax(280px, .85fr); gap: 48px; align-items: end; padding: 18px 0 62px; }
.eyebrow { margin: 0 0 9px; color: var(--home-muted); font-size: 11px; font-weight: 700; letter-spacing: .14em; text-transform: uppercase; }.greeting { margin: 0 0 8px; font-size: 18px; } h1, h2 { color: var(--home-ink); letter-spacing: -.045em; } h1 { margin: 0; font-size: clamp(42px, 6vw, 76px); line-height: 1.02; font-weight: 700; }.mobile-break { display: none; }.hero-subtitle { max-width: 410px; margin: 22px 0 24px; color: #6e6e73; font-size: 17px; }.hero-meta { display: flex; align-items: center; gap: 13px; font-size: 13px; }.meta-divider { width: 1px; height: 18px; background: #d2d2d7; }.primary-action { display: inline-flex; align-items: center; gap: 16px; margin-top: 31px; padding: 14px 20px; color: #fff; background: var(--home-accent); border: 0; border-radius: 999px; font: inherit; font-weight: 600; cursor: pointer; transition: transform .2s ease, background .2s ease; }.primary-action:hover { background: #0077ed; transform: translateY(-1px); }.primary-action:active { transform: scale(.98); }
.progress-panel { display: flex; align-items: center; justify-content: center; gap: 24px; min-height: 230px; padding: 28px; background: rgba(255,255,255,.72); border: 1px solid rgba(0,0,0,.06); border-radius: 28px; box-shadow: 0 16px 40px rgba(0,0,0,.05); }.progress-ring { display: grid; place-items: center; width: 154px; aspect-ratio: 1; border-radius: 50%; background: conic-gradient(var(--home-accent) var(--progress), #e8e8ed 0); }.progress-ring-inner { display: grid; place-items: center; width: 126px; aspect-ratio: 1; border-radius: 50%; background: #fff; }.progress-ring strong { color: var(--home-ink); font-size: 34px; letter-spacing: -.06em; }.progress-ring small { margin-left: 2px; font-size: 16px; }.progress-ring span { margin-top: -24px; color: var(--home-muted); font-size: 11px; }.progress-caption { display: grid; gap: 6px; max-width: 150px; font-size: 13px; }.progress-caption strong { color: var(--home-ink); font-size: 17px; }
.content-grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(340px, .78fr); gap: 56px; align-items: start; }.section-heading { display: flex; align-items: end; justify-content: space-between; margin-bottom: 18px; }.section-heading.compact { align-items: center; } h2 { margin: 0; font-size: 28px; }.task-count { font-size: 13px; }.task-list { background: #fff; border: 1px solid var(--home-line); border-radius: 20px; overflow: hidden; }.task-row { display: flex; align-items: center; gap: 14px; min-height: 82px; padding: 14px 18px; cursor: pointer; transition: background .2s ease, opacity .2s ease; }.task-row + .task-row { border-top: 1px solid var(--home-line); }.task-row:hover, .task-row:focus-visible { background: #fafafa; outline: none; }.task-row:focus-visible { box-shadow: inset 0 0 0 2px var(--home-accent); }.task-row.is-done { opacity: .52; }.task-index { width: 25px; color: #b1b1b6; font-size: 12px; font-variant-numeric: tabular-nums; }.task-main { min-width: 0; flex: 1; }.task-title { overflow: hidden; color: var(--home-ink); font-size: 16px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }.task-row.is-done .task-title { text-decoration: line-through; }.task-meta { display: flex; gap: 7px; margin-top: 5px; color: var(--home-muted); font-size: 12px; }.dot { color: #c7c7cc; }.task-cta { flex-shrink: 0; color: var(--home-accent); font-size: 13px; }
.empty-plan { display: flex; align-items: center; gap: 16px; padding: 23px; background: #fff; border: 1px solid var(--home-line); border-radius: 20px; }.empty-plan > div:nth-child(2) { flex: 1; }.empty-plan strong { color: var(--home-ink); }.empty-plan p { margin: 5px 0 0; font-size: 13px; }.empty-icon { display: grid; place-items: center; width: 42px; height: 42px; color: var(--home-accent); background: #eef6ff; border-radius: 50%; }.side-column { display: grid; gap: 42px; }.plan-link { display: flex; align-items: center; justify-content: space-between; padding: 21px 0; border-top: 1px solid #d2d2d7; border-bottom: 1px solid #d2d2d7; color: var(--home-accent); cursor: pointer; }.plan-link strong, .plan-link span { display: block; }.plan-link strong { margin-bottom: 4px; color: var(--home-ink); font-size: 17px; }.plan-link span { font-size: 13px; }.plan-link > .el-icon { font-size: 22px; }
.scene-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }.scene-tile { min-height: 210px; display: flex; flex-direction: column; padding: 17px; border: 1px solid rgba(0,0,0,.05); border-radius: 20px; cursor: pointer; transition: transform .2s ease, box-shadow .2s ease; }.scene-tile:hover { transform: translateY(-3px); box-shadow: 0 12px 24px rgba(0,0,0,.08); }.scene-tile:focus-visible { outline: 2px solid var(--home-accent); outline-offset: 3px; }.tone-0 { background: #eaf3ff; }.tone-1 { background: #f1f0ff; }.tone-2 { background: #edf8f2; }.tone-3 { background: #fff3e8; }.scene-topline, .scene-footer { display: flex; align-items: center; justify-content: space-between; color: var(--home-muted); font-size: 11px; }.scene-symbol { display: grid; place-items: center; width: 43px; height: 43px; margin: 25px 0 15px; color: #fff; background: var(--home-accent); border-radius: 14px; font-size: 21px; font-weight: 700; }.scene-name { color: var(--home-ink); font-size: 17px; font-weight: 700; }.scene-desc { display: -webkit-box; margin-top: 5px; overflow: hidden; font-size: 12px; line-height: 1.45; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }.scene-footer { margin-top: auto; padding-top: 16px; }.scene-footer .el-icon { color: var(--home-accent); font-size: 17px; }.recommend-empty { padding: 34px 0; text-align: center; font-size: 13px; }
@media (max-width: 900px) { .home-shell { width: min(100% - 32px, 680px); padding-top: 32px; }.hero-grid, .content-grid { grid-template-columns: 1fr; gap: 30px; }.hero-grid { padding-bottom: 38px; }.progress-panel { justify-content: flex-start; min-height: 0; }.side-column { gap: 30px; } }
@media (max-width: 560px) { .home-shell { width: calc(100% - 32px); padding: 25px 0 16px; }.mobile-break { display: initial; } h1 { font-size: 45px; }.hero-subtitle { margin: 17px 0 21px; font-size: 15px; }.progress-panel { gap: 18px; padding: 19px; border-radius: 22px; }.progress-ring { width: 116px; }.progress-ring-inner { width: 94px; }.progress-ring strong { font-size: 27px; }.progress-ring span { margin-top: -20px; font-size: 10px; }.progress-caption strong { font-size: 15px; }.content-grid { gap: 34px; }.scene-grid { display: flex; overflow-x: auto; gap: 12px; margin-right: -16px; padding-right: 16px; scroll-snap-type: x mandatory; scrollbar-width: none; }.scene-grid::-webkit-scrollbar { display: none; }.scene-tile { flex: 0 0 78%; scroll-snap-align: start; }.task-row { min-height: 76px; gap: 10px; padding: 12px; }.task-index { display: none; }.task-title { font-size: 15px; }.task-cta { padding: 6px 2px; }.empty-plan { align-items: flex-start; flex-wrap: wrap; }.empty-plan > .el-button { width: 100%; margin-left: 58px; } }
</style>
