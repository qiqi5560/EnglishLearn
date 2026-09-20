<template>
  <div v-loading="loading" class="home page">
    <AppHeader title="首页" />

    <!-- ==================== 顶部：流动的英语励志词 ==================== -->
    <div class="ticker">
      <div class="ticker-track">
        <span v-for="(m, i) in tickerRow" :key="`ta${i}`" class="ticker-item">{{ m }}<i>✦</i></span>
        <span v-for="(m, i) in tickerRow" :key="`tb${i}`" class="ticker-item">{{ m }}<i>✦</i></span>
      </div>
    </div>

    <main class="page-shell home-shell">
      <!-- ==================== Hero：左文案 + 右进度 ==================== -->
      <section class="hero-grid" aria-labelledby="home-heading">
        <div class="hero-copy">
          <p class="eyebrow">{{ todayLabel }}</p>
          <p class="greeting text-muted">Hi，{{ nickname }}</p>
          <h1 id="home-heading">今天也要开口说英语</h1>
          <p class="hero-subtitle">用一点点练习，把表达变成你的自然反应。</p>
          <div class="hero-meta">
            <LevelTag :level="currentLevel || 'A2'" />
            <span class="meta-divider" aria-hidden="true"></span>
            <span class="text-muted">今日完成 {{ doneCount }}/{{ totalTasks }}</span>
          </div>
          <button class="primary-action" type="button" @click="onPrimaryAction">
            <span>{{ primaryActionLabel }}</span>
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>

        <aside class="progress-panel glass-card peek-host" aria-label="今日学习进度">
          <PeekMascot variant="cat" :size="54" />
          <div class="progress-body">
            <div class="progress-ring" :style="progressRingStyle">
              <div class="progress-ring-inner">
                <strong>{{ progressPercent }}<small>%</small></strong>
                <span>今日进度</span>
              </div>
            </div>
            <div class="progress-caption">
              <strong>{{ remainingLabel }}</strong>
              <span class="text-muted">保持节奏，完成今天的学习计划</span>
              <div class="mini-stats">
                <div><b>{{ doneCount }}</b><span>已完成</span></div>
                <div><b>{{ totalTasks - doneCount }}</b><span>待完成</span></div>
              </div>
            </div>
          </div>
        </aside>
      </section>

      <!-- ==================== 主体两栏：任务 + 侧栏 ==================== -->
      <div class="content-grid">
        <section class="task-section" aria-labelledby="tasks-heading">
          <div class="section-heading">
            <div>
              <p class="eyebrow">YOUR PLAN</p>
              <h2 id="tasks-heading">今日任务</h2>
            </div>
            <span class="task-count text-muted">{{ doneCount }}/{{ totalTasks }}</span>
          </div>

          <div v-if="planStore.dailyTasks.length" class="task-list">
            <div
              v-for="task in planStore.dailyTasks"
              :key="task.taskId"
              class="task-row"
              :class="{ 'is-done': task.done }"
              role="button"
              tabindex="0"
              @click="onStartTask(task)"
              @keydown.enter="onStartTask(task)"
              @keydown.space.prevent="onStartTask(task)"
            >
              <el-checkbox
                class="task-check"
                :model-value="task.done"
                :aria-label="`标记任务 ${task.title}`"
                @click.stop
                @change="(val: string | number | boolean) => onToggleTask(task, !!val)"
              />
              <div class="task-index">{{ String(task.taskId).padStart(2, '0') }}</div>
              <div class="task-main">
                <div class="task-title">{{ task.title }}</div>
                <div class="task-meta">
                  <span>{{ task.type }}</span>
                  <span class="dot">·</span>
                  <span>{{ task.durationMin }} 分钟</span>
                </div>
              </div>
              <el-button text class="task-cta" @click.stop="onStartTask(task)">
                {{ task.done ? '再练' : '开始' }}
                <el-icon><ArrowUpRight /></el-icon>
              </el-button>
            </div>
          </div>

          <div v-else class="empty-plan">
            <div class="empty-icon"><el-icon><MagicStick /></el-icon></div>
            <div>
              <strong>为你制定专属学习计划</strong>
              <p class="text-muted">完成一次测评，开始你的每日练习。</p>
            </div>
            <el-button type="primary" @click="$router.push('/entrance-test')">去测评</el-button>
          </div>

          <!-- 填满左栏留白：练完之后的去处 -->
          <section class="quick-section" aria-labelledby="quick-heading">
            <div class="section-heading compact">
              <div>
                <p class="eyebrow">KEEP GOING</p>
                <h2 id="quick-heading">练完这些还能去</h2>
              </div>
            </div>
            <div class="quick-grid">
              <button
                v-for="q in quickLinks"
                :key="q.path"
                class="quick-tile hover-lift"
                type="button"
                @click="$router.push(q.path)"
              >
                <span class="quick-icon"><el-icon><component :is="q.icon" /></el-icon></span>
                <span class="quick-text">
                  <strong>{{ q.title }}</strong>
                  <em>{{ q.desc }}</em>
                </span>
                <el-icon class="quick-arrow"><ArrowUpRight /></el-icon>
              </button>
            </div>
          </section>
        </section>

        <aside class="side-column">
          <section
            class="plan-link"
            role="button"
            tabindex="0"
            @click="$router.push('/plan')"
            @keydown.enter="$router.push('/plan')"
          >
            <div>
              <p class="eyebrow">YOUR GOAL</p>
              <strong>{{ planStore.targetGoal || '还没有学习目标' }}</strong>
              <span class="text-muted">当前等级 {{ currentLevel || '--' }}</span>
            </div>
            <el-icon><ArrowUpRight /></el-icon>
          </section>

          <section class="recommend-section" aria-labelledby="recommend-heading">
            <div class="section-heading compact">
              <div>
                <p class="eyebrow">CURATED FOR YOU</p>
                <h2 id="recommend-heading">为你推荐</h2>
              </div>
              <el-button text type="primary" @click="$router.push('/practice')">全部</el-button>
            </div>

            <!-- AI 口语水平预测 -->
            <div v-if="levelPrediction" class="predict-card" aria-label="AI 预测口语水平">
              <span class="predict-level">{{ levelPrediction.level }}</span>
              <div class="predict-main">
                <p class="predict-title">AI 预测水平 · {{ levelPrediction.band }}</p>
                <div class="predict-bar" role="img" :aria-label="`置信度 ${levelPrediction.confidence}%`">
                  <i :style="{ width: `${Math.min(100, levelPrediction.confidence)}%` }"></i>
                </div>
                <p class="predict-meta text-muted">
                  置信度 {{ levelPrediction.confidence }}% · 基于近 {{ levelPrediction.sampleCount }} 次练习 ·
                  {{ levelPrediction.source === 'model' ? '模型预测' : '规则估算' }}
                </p>
              </div>
            </div>

            <el-tabs v-model="recommendTab" class="recommend-tabs">
              <el-tab-pane label="场景" name="scene">
                <div v-if="scenes.length" class="scene-grid">
                  <article
                    v-for="(scene, index) in scenes"
                    :key="scene.id"
                    class="scene-tile"
                    :class="`tone-${index % 4}`"
                    role="button"
                    tabindex="0"
                    @click="goScene(scene)"
                    @keydown.enter="goScene(scene)"
                  >
                    <div class="scene-topline">
                      <span>{{ scene.category }}</span>
                      <LevelTag :level="scene.level" />
                    </div>
                    <div class="scene-symbol" aria-hidden="true">{{ scene.name.slice(0, 1) }}</div>
                    <div class="scene-name">{{ scene.name }}</div>
                    <div class="scene-desc text-muted">{{ scene.desc }}</div>
                    <div class="scene-footer">
                      <span>{{ scene.role || 'AI 搭档' }}</span>
                      <span v-if="scene.reason" class="reason-tag">{{ scene.reason }}</span>
                      <el-icon><ArrowUpRight /></el-icon>
                    </div>
                  </article>
                </div>
                <div v-else-if="!loading" class="recommend-empty text-muted">暂时没有推荐场景</div>
              </el-tab-pane>

              <el-tab-pane label="素材" name="resource">
                <div v-if="resources.length" class="rec-grid">
                  <article
                    v-for="(res, index) in resources"
                    :key="res.id"
                    class="rec-card"
                    :class="`tone-${index % 4}`"
                    role="button"
                    tabindex="0"
                    @click="goResource(res)"
                    @keydown.enter="goResource(res)"
                  >
                    <div class="scene-topline">
                      <span>{{ res.category }}</span>
                      <LevelTag :level="res.level" />
                    </div>
                    <div class="scene-name">{{ res.title }}</div>
                    <div class="scene-desc text-muted">{{ res.type }} · {{ Math.round((res.durationSec ?? 0) / 60) }} 分钟</div>
                    <div class="scene-footer">
                      <span v-if="res.reason" class="reason-tag">{{ res.reason }}</span>
                      <el-icon><ArrowUpRight /></el-icon>
                    </div>
                  </article>
                </div>
                <div v-else-if="!loading" class="recommend-empty text-muted">暂时没有推荐素材</div>
              </el-tab-pane>

              <el-tab-pane label="任务" name="task">
                <div v-if="tasks.length" class="rec-list">
                  <div v-for="(task, index) in tasks" :key="`${task.type}-${index}`" class="rec-row">
                    <span class="rec-dot" :class="`tone-${index % 4}`" aria-hidden="true"></span>
                    <div class="rec-row-main">
                      <p class="rec-title">{{ task.title }}</p>
                      <p class="scene-desc text-muted">
                        {{ task.type }} · {{ task.durationMin }} 分钟
                        <span v-if="task.reason" class="reason-tag">{{ task.reason }}</span>
                      </p>
                    </div>
                    <button class="rec-action" type="button" @click="onRecommendTask(task)">开始</button>
                  </div>
                </div>
                <div v-else-if="!loading" class="recommend-empty text-muted">暂时没有推荐任务</div>
              </el-tab-pane>
            </el-tabs>
          </section>
        </aside>
      </div>

      <!-- ==================== 台词角：流动的英语台词框 ==================== -->
      <section class="lines-panel glass-card" aria-labelledby="lines-heading">
        <header class="lines-head">
          <div>
            <p class="eyebrow">MOVIE LINES</p>
            <h2 id="lines-heading">台词角 · 跟着电影学地道表达</h2>
          </div>
          <span class="lines-hint text-muted">点任意台词进入「名句跟读」· 鼠标移入暂停滚动</span>
        </header>

        <div class="lines-lane">
          <div class="lines-track">
            <article
              v-for="(q, i) in laneCards"
              :key="`la${i}`"
              class="line-card peek-host"
              role="button"
              tabindex="0"
              @click="jumpToQuote(q.film)"
              @keydown.enter="jumpToQuote(q.film)"
            >
              <span class="line-tag"><el-icon><Sort /></el-icon>跟读</span>
              <p class="line-en">{{ q.en }}</p>
              <p class="line-zh">{{ q.zh }}</p>
              <footer class="line-film">— {{ q.film }}</footer>
            </article>
            <article
              v-for="(q, i) in laneCards"
              :key="`lb${i}`"
              class="line-card peek-host"
              role="button"
              tabindex="0"
              @click="jumpToQuote(q.film)"
              @keydown.enter="jumpToQuote(q.film)"
            >
              <span class="line-tag"><el-icon><Sort /></el-icon>跟读</span>
              <p class="line-en">{{ q.en }}</p>
              <p class="line-zh">{{ q.zh }}</p>
              <footer class="line-film">— {{ q.film }}</footer>
            </article>
          </div>
        </div>
      </section>

      <!-- ==================== 练习小贴士：填满留白 ==================== -->
      <section class="tips-section" aria-labelledby="tips-heading">
        <div class="section-heading">
          <div>
            <p class="eyebrow">HOW TO PRACTICE</p>
            <h2 id="tips-heading">三个让练习更有效的小习惯</h2>
          </div>
        </div>

        <div class="tips-grid stagger">
          <article v-for="tip in tips" :key="tip.title" class="tip-card glass-card hover-lift peek-host">
            <div class="pet-corner">
              <PeekMascot :variant="tip.pet" :size="46" />
            </div>
            <span class="tip-icon"><el-icon><component :is="tip.icon" /></el-icon></span>
            <h3>{{ tip.title }}</h3>
            <p class="text-muted">{{ tip.desc }}</p>
          </article>
        </div>
      </section>
    </main>

    <!-- ==================== 底部：反向流动的励志语（收尾不留白） ==================== -->
    <div class="ticker ticker-tail">
      <div class="ticker-track">
        <span v-for="(m, i) in tailRow" :key="`fa${i}`" class="ticker-item">{{ m }}<i>✦</i></span>
        <span v-for="(m, i) in tailRow" :key="`fb${i}`" class="ticker-item">{{ m }}<i>✦</i></span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
import PeekMascot from '@/components/base/PeekMascot.vue'
import LevelTag from '@/components/business/LevelTag.vue'
import { useUserStore } from '@/stores/user'
import { usePlanStore } from '@/stores/plan'
import { useTaskJump } from '@/composables/useTaskJump'
import { track } from '@/composables/useTracker'
import { recommendedScenes } from '@/api/modules/scene'
import { recommendOverview } from '@/api/modules/recommend'
import type {
  DailyTaskDto,
  LevelPrediction,
  RecommendResourceItem,
  RecommendSceneItem,
  RecommendTaskItem,
} from '@/types/api'

const router = useRouter(); const userStore = useUserStore(); const planStore = usePlanStore(); const { startTask } = useTaskJump(); const loading = ref(false)
const scenes = ref<RecommendSceneItem[]>([])
const resources = ref<RecommendResourceItem[]>([])
const tasks = ref<RecommendTaskItem[]>([])
const levelPrediction = ref<LevelPrediction | null>(null)
const recommendTab = ref<'scene' | 'resource' | 'task'>('scene')
const currentLevel = computed(() => planStore.level ?? userStore.level ?? null)
const totalTasks = computed(() => planStore.dailyTasks.length)
const doneCount = computed(() => planStore.dailyTasks.filter((task) => task.done).length)
const progressPercent = computed(() => (totalTasks.value ? Math.round((doneCount.value / totalTasks.value) * 100) : 0))
const nextTask = computed(() => planStore.dailyTasks.find((task) => !task.done) || planStore.dailyTasks[0])
const primaryActionLabel = computed(() => nextTask.value ? (nextTask.value.done ? '复习今日任务' : `继续：${nextTask.value.title}`) : '开始入学测评')
const remainingLabel = computed(() => totalTasks.value ? (doneCount.value === totalTasks.value ? '今日任务已完成' : `还有 ${totalTasks.value - doneCount.value} 项待完成`) : '从测评开始')
const todayLabel = computed(() => new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }).format(new Date()))
const progressRingStyle = computed(() => ({ '--progress': `${progressPercent.value * 3.6}deg` }))
const nickname = computed(() => userStore.userInfo?.nickname || '学习者')

/** 英语励志短句：上下两条流动带各自洗牌 */
const TICKER_WORDS = [
  'Practice makes progress',
  '开口就是进步',
  'Speak it out, own it',
  '每天十分钟，胜过周末三小时',
  'Mistakes are proof you are trying',
  '不怕说错，就怕不说',
  'Small steps, every day',
  '先完成，再完美',
  'Your voice deserves to be heard',
  '语感是练出来的，不是背出来的',
]

/** 电影台词库：每次进入首页都会重新洗牌，保证顺序与开场句都不一样 */
const MOVIE_LINES = [
  { en: 'Just keep swimming.', zh: '只管一直往前游就好。', film: '《海底总动员》' },
  { en: 'Yesterday is history, tomorrow is a mystery.', zh: '昨天已成历史，明天仍是谜团。', film: '《功夫熊猫》' },
  { en: 'Why do we fall? So we can learn to pick ourselves up.', zh: '我们为什么会跌倒？是为了学会自己站起来。', film: '《蝙蝠侠：侠影之谜》' },
  { en: 'May the Force be with you.', zh: '愿原力与你同在。', film: '《星球大战》' },
  { en: 'Life is like a box of chocolates.', zh: '生活就像一盒巧克力，你永远不知道下一颗是什么味道。', film: '《阿甘正传》' },
  { en: 'To infinity and beyond!', zh: '飞向无限，超越极限！', film: '《玩具总动员》' },
  { en: 'It is not our abilities that show what we truly are, it is our choices.', zh: '决定我们成为什么样的人的，不是能力，而是选择。', film: '《哈利·波特》' },
  { en: 'The past can hurt. But you can either run from it, or learn from it.', zh: '过去会让人痛。你可以选择逃避，也可以选择从中学习。', film: '《狮子王》' },
  { en: 'Hakuna Matata! It means no worries.', zh: '哈库那玛塔塔！意思是没有烦恼。', film: '《狮子王》' },
  { en: 'Do or do not. There is no try.', zh: '要么做，要么不做，没有"试试看"。', film: '《星球大战》' },
  { en: 'With great power comes great responsibility.', zh: '能力越大，责任越大。', film: '《蜘蛛侠》' },
  { en: 'Every great dream begins with a dreamer.', zh: '每一个伟大的梦想都始于一个敢于做梦的人。', film: '《寻梦环游记》' },
  { en: 'You are braver than you believe, stronger than you seem.', zh: '你比你相信的更勇敢，比你看起来的更强壮。', film: '《小熊维尼》' },
  { en: 'The flower that blooms in adversity is the rarest of all.', zh: '逆境中绽放的花朵，最为珍贵。', film: '《花木兰》' },
  { en: 'Keep your eyes on the stars and your feet on the ground.', zh: '目光望向星辰，脚步踩在大地。', film: '《飞屋环游记》' },
  { en: 'Our fate lives within us. You only have to be brave enough to see it.', zh: '命运就在我们心里，你只需要足够勇敢去看见它。', film: '《勇敢传说》' },
  { en: 'Hope is a good thing, maybe the best of things.', zh: '希望是美好的，也许是最美好的事物。', film: '《肖申克的救赎》' },
  { en: 'Do not ever let somebody tell you that you can not do something.', zh: '永远别让别人告诉你，你做不到。', film: '《当幸福来敲门》' },
  { en: 'It is what you do that defines you.', zh: '你的所作所为，才定义了你是谁。', film: '《蝙蝠侠：侠影之谜》' },
  { en: 'All we have to decide is what to do with the time that is given to us.', zh: '我们要决定的，只是如何用好被给予的时间。', film: '《指环王》' },
  { en: 'Adventure is out there!', zh: '冒险就在前方！', film: '《飞屋环游记》' },
  { en: 'Keep moving forward.', zh: '继续往前走，别停下来。', film: '《拜见罗宾逊一家》' },
  { en: 'Great men are not born great, they grow great.', zh: '伟人并非生来伟大，而是逐渐成长为伟大。', film: '《教父》' },
  { en: 'I am the master of my fate, I am the captain of my soul.', zh: '我是自己命运的主宰，我是自己灵魂的船长。', film: '《成事在人》' },
  { en: 'A dream is a wish your heart makes.', zh: '梦想是你心底许下的愿望。', film: '《灰姑娘》' },
]

/** 练完之后的去处（填满左栏留白，全部为已有入口） */
const quickLinks = [
  { path: '/practice', icon: 'Microphone', title: '场景对话', desc: '挑个场景，跟 AI 搭档开口聊' },
  { path: '/community/pair', icon: 'User', title: '结伴练习', desc: '找同学对练，互相听互相纠' },
  { path: '/plan', icon: 'Notebook', title: '学习方案', desc: '看看接下来的学习节奏' },
  { path: '/report', icon: 'TrendCharts', title: '学习报表', desc: '回看这段时间的进步曲线' },
]

/** 练习小贴士（纯展示文案，含探头小动物） */
const tips = [
  { icon: 'Microphone', title: '先开口，再求准', desc: '发音不完美没关系，敢说才会形成语感，系统会帮你逐句纠正。', pet: 'cat' as const },
  { icon: 'Headset', title: '精听三遍法', desc: '第一遍盲听抓大意，第二遍逐句对照，第三遍跟读模仿语调。', pet: 'dog' as const },
  { icon: 'Notebook', title: '把句子搬进生活', desc: '每学一句就试着在真实场景里用一次，记得最牢也最自然。', pet: 'cat' as const },
]

/** Fisher-Yates 洗牌：让每次进入首页的流动顺序都不同 */
function shuffled<T>(list: readonly T[]): T[] {
  const arr = [...list]
  for (let i = arr.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1))
    const tmp = arr[i]
    arr[i] = arr[j]
    arr[j] = tmp
  }
  return arr
}

const tickerRow = shuffled(TICKER_WORDS)
const tailRow = shuffled(TICKER_WORDS)
const laneCards = shuffled(MOVIE_LINES)

async function onToggleTask(task: DailyTaskDto, val: boolean) { try { await planStore.toggleTask(task.taskId, val) } catch { ElMessage.error('任务状态更新失败，请稍后重试') } }
async function onStartTask(task: DailyTaskDto) { try { await startTask(task) } catch { /* 请求层负责提示 */ } }
/** 点击台词卡片：带着片名进入名句跟读，自动定位到对应名句 */
function jumpToQuote(film: string) { router.push({ path: '/quotes', query: { film } }) }
function onPrimaryAction() { if (nextTask.value) return onStartTask(nextTask.value); return router.push('/entrance-test') }
/** 推荐项点击：一期仅留埋点钩子，二期上报真实曝光 / 点击事件 */
function goScene(scene: RecommendSceneItem) {
  track('click', { target: 'scene', sceneId: scene.id })
  router.push(`/scene/${scene.id}`)
}
function goResource(resource: RecommendResourceItem) {
  track('click', { target: 'resource', resourceId: resource.id })
  router.push(`/resource/${resource.id}/read`)
}
async function onRecommendTask(task: RecommendTaskItem) {
  track('click', { target: 'task', type: task.type, sceneId: task.sceneId, resourceId: task.resourceId })
  try {
    await startTask({
      taskId: 0,
      type: task.type,
      title: task.title,
      durationMin: task.durationMin,
      sceneId: task.sceneId,
      resourceId: task.resourceId,
      done: false,
      taskDate: '',
    } as unknown as DailyTaskDto)
  } catch { /* 请求层负责提示 */ }
}

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([
      planStore.loadTodayTasks().catch(() => null),
      userStore.fetchMe().catch(() => null),
    ])
    // 已登录走个性化推荐；未登录或接口异常时降级为热门场景
    try {
      const overview = await recommendOverview(4)
      levelPrediction.value = overview?.level ?? null
      scenes.value = overview?.scenes ?? []
      resources.value = overview?.resources ?? []
      tasks.value = overview?.tasks ?? []
    } catch {
      levelPrediction.value = null
      scenes.value = await recommendedScenes(4).catch(() => [])
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.home {
  background: var(--surface);
}

.home-shell {
  padding-top: 26px;
  padding-bottom: 64px;
}

// ------------------------------------------------------------
// 流动励志语带
// ------------------------------------------------------------
.ticker {
  overflow: hidden;
  background: linear-gradient(90deg, #2f5bb3, #4a7ce4 45%, #2f5bb3);
}

.ticker-track {
  display: flex;
  width: max-content;
  animation: ticker-flow 52s linear infinite;
}

.ticker:hover .ticker-track {
  animation-play-state: paused;
}

.ticker-tail .ticker-track {
  animation-direction: reverse;
}

.ticker-item {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 9px 30px 9px 0;
  color: #eaf2ff;
  font-size: 13px;
  letter-spacing: 0.01em;
  white-space: nowrap;

  i {
    color: #a9c6ff;
    font-style: normal;
    font-size: 10px;
  }
}

@keyframes ticker-flow {
  to {
    transform: translateX(-50%);
  }
}

// ------------------------------------------------------------
// 标题与通用排版（网页端小字号）
// ------------------------------------------------------------
.eyebrow {
  margin: 0 0 7px;
  color: var(--muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

h1,
h2,
h3 {
  color: var(--ink);
  letter-spacing: -0.03em;
}

h1 {
  margin: 0;
  font-size: clamp(26px, 2.1vw, 31px);
  font-weight: 700;
  line-height: 1.18;
}

h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
}

.section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 14px;

  &.compact {
    align-items: center;
  }
}

// ------------------------------------------------------------
// Hero
// ------------------------------------------------------------
.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 40px;
  align-items: center;
  padding-bottom: 34px;
}

.greeting {
  margin: 0 0 6px;
  font-size: 14px;
}

.hero-subtitle {
  max-width: 430px;
  margin: 12px 0 18px;
  color: var(--muted);
  font-size: 14px;
  line-height: 1.7;
}

.hero-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
}

.meta-divider {
  width: 1px;
  height: 16px;
  background: var(--border);
}

.primary-action {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  margin-top: 22px;
  padding: 12px 20px;
  color: #fff;
  background: linear-gradient(135deg, #5b8bf0, #3b6fe0 60%, #2f5bb3);
  border: 0;
  border-radius: 999px;
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 10px 24px rgba(59, 111, 224, 0.28);
  transition:
    transform 0.35s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.35s ease;

  .el-icon {
    transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 16px 30px rgba(59, 111, 224, 0.34);

    .el-icon {
      transform: translateX(4px);
    }
  }

  &:active {
    transform: scale(0.98);
  }
}

// 今日进度卡（顶部是小猫探头）
.progress-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 26px 24px;
}

.progress-body {
  display: flex;
  align-items: center;
  gap: 22px;
  width: 100%;
}

.progress-ring {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 132px;
  aspect-ratio: 1;
  border-radius: 50%;
  background: conic-gradient(var(--primary) var(--progress), var(--border) 0);
  transition: background 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}

.progress-ring-inner {
  display: grid;
  place-items: center;
  width: 106px;
  aspect-ratio: 1;
  background: var(--card);
  border-radius: 50%;
}

.progress-ring strong {
  color: var(--ink);
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.05em;

  small {
    margin-left: 1px;
    font-size: 14px;
  }
}

.progress-ring span {
  margin-top: -20px;
  color: var(--muted);
  font-size: 11px;
}

.progress-caption {
  display: grid;
  gap: 5px;
  min-width: 0;
  font-size: 13px;

  strong {
    color: var(--ink);
    font-size: 15px;
  }

  > span {
    font-size: 12px;
  }
}

.mini-stats {
  display: flex;
  gap: 18px;
  margin-top: 10px;

  div {
    display: flex;
    align-items: baseline;
    gap: 5px;
  }

  b {
    color: var(--primary);
    font-size: 17px;
  }

  span {
    color: var(--muted);
    font-size: 11px;
  }
}

// ------------------------------------------------------------
// 主体两栏
// ------------------------------------------------------------
.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.72fr);
  gap: 44px;
  align-items: start;
}

.task-count {
  font-size: 12px;
}

.task-list {
  overflow: hidden;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.task-row {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 68px;
  padding: 12px 18px;
  cursor: pointer;
  transition:
    background 0.28s ease,
    opacity 0.28s ease;

  & + .task-row {
    border-top: 1px solid var(--border);
  }

  &:hover,
  &:focus-visible {
    background: rgba(59, 111, 224, 0.05);
    outline: none;
  }

  &.is-done {
    opacity: 0.5;

    .task-title {
      text-decoration: line-through;
    }
  }
}

.task-index {
  width: 22px;
  color: #b6bed0;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.task-main {
  flex: 1;
  min-width: 0;
}

.task-title {
  overflow: hidden;
  color: var(--ink);
  font-size: 15px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-meta {
  display: flex;
  gap: 6px;
  margin-top: 3px;
  color: var(--muted);
  font-size: 12px;
}

.dot {
  color: var(--border);
}

.task-cta {
  flex-shrink: 0;
  color: var(--primary);
  font-size: 13px;

  .el-icon {
    transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  }

  &:hover .el-icon {
    transform: translate(2px, -2px);
  }
}

.empty-plan {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);

  > div:nth-child(2) {
    flex: 1;
  }

  strong {
    font-size: 15px;
  }

  p {
    margin: 4px 0 0;
    font-size: 13px;
  }
}

.empty-icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  color: var(--primary);
  background: rgba(59, 111, 224, 0.1);
  border-radius: 50%;
}

.quick-section {
  margin-top: 30px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.quick-tile {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 15px;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.quick-icon {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  color: var(--primary);
  background: rgba(59, 111, 224, 0.1);
  border-radius: 10px;
  font-size: 16px;
  transition: transform 0.45s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.quick-tile:hover .quick-icon {
  transform: rotate(-8deg) scale(1.06);
}

.quick-text {
  display: grid;
  gap: 3px;
  min-width: 0;

  strong {
    color: var(--ink);
    font-size: 14px;
    font-weight: 600;
  }

  em {
    color: var(--muted);
    font-size: 11px;
    font-style: normal;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.quick-arrow {
  flex-shrink: 0;
  margin-left: auto;
  color: var(--primary);
  font-size: 15px;
  transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.quick-tile:hover .quick-arrow {
  transform: translate(3px, -3px);
}

.side-column {
  display: grid;
  gap: 30px;
}

.plan-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 0;
  border-top: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
  cursor: pointer;
  transition: padding-left 0.35s cubic-bezier(0.22, 1, 0.36, 1);

  strong,
  span {
    display: block;
  }

  strong {
    margin-bottom: 3px;
    font-size: 15px;
  }

  span {
    font-size: 12px;
  }

  > .el-icon {
    color: var(--primary);
    font-size: 20px;
  }

  &:hover {
    padding-left: 8px;
  }
}

.scene-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.scene-tile {
  display: flex;
  flex-direction: column;
  min-height: 176px;
  padding: 15px;
  border: 1px solid rgba(31, 42, 68, 0.06);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition:
    transform 0.35s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.35s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 16px 32px rgba(31, 42, 68, 0.12);
  }

  &:focus-visible {
    outline: 2px solid var(--primary);
    outline-offset: 3px;
  }
}

.tone-0 {
  background: rgba(59, 111, 224, 0.09);
}

.tone-1 {
  background: rgba(120, 108, 224, 0.09);
}

.tone-2 {
  background: rgba(34, 160, 107, 0.09);
}

.tone-3 {
  background: rgba(255, 106, 77, 0.09);
}

.scene-topline,
.scene-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--muted);
  font-size: 11px;
}

.scene-symbol {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  margin: 18px 0 12px;
  color: #fff;
  background: linear-gradient(135deg, #5b8bf0, #2f5bb3);
  border-radius: 12px;
  font-size: 19px;
  font-weight: 700;
}

.scene-name {
  font-size: 15px;
  font-weight: 700;
}

.scene-desc {
  display: -webkit-box;
  margin-top: 4px;
  overflow: hidden;
  font-size: 12px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.scene-footer {
  margin-top: auto;
  padding-top: 14px;

  .el-icon {
    color: var(--primary);
    font-size: 15px;
    transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  }
}

.scene-tile:hover .scene-footer .el-icon {
  transform: translate(3px, -3px);
}

.recommend-empty {
  padding: 28px 0;
  text-align: center;
  font-size: 13px;
}

// ------------------------------------------------------------
// AI 水平预测卡与推荐素材 / 任务
// ------------------------------------------------------------
.predict-card {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
  padding: 14px 16px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(59, 111, 224, 0.1), rgba(91, 139, 240, 0.04));
  border: 1px solid rgba(59, 111, 224, 0.16);
}

.predict-level {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b6fe0, #5b8bf0);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  box-shadow: 0 6px 14px rgba(59, 111, 224, 0.28);
}

.predict-main {
  flex: 1;
  min-width: 0;
}

.predict-title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
}

.predict-bar {
  height: 6px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(59, 111, 224, 0.14);
}

.predict-bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #3b6fe0, #5b8bf0);
  transition: width 0.6s var(--ease-apple, cubic-bezier(0.22, 1, 0.36, 1));
}

.predict-meta {
  margin: 8px 0 0;
  font-size: 12px;
}

.reason-tag {
  padding: 1px 8px;
  border-radius: 999px;
  background: rgba(34, 160, 107, 0.12);
  color: #22a06b;
  font-size: 11px;
  font-weight: 600;
}

.rec-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.rec-card {
  padding: 16px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(59, 111, 224, 0.08), rgba(91, 139, 240, 0.03));
  border: 1px solid rgba(31, 42, 68, 0.06);
  cursor: pointer;
  transition: transform 0.28s var(--ease-apple, cubic-bezier(0.22, 1, 0.36, 1)), box-shadow 0.28s ease;
}

.rec-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 26px rgba(31, 42, 68, 0.1);
}

.rec-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rec-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid rgba(31, 42, 68, 0.06);
  transition: transform 0.24s var(--ease-apple, cubic-bezier(0.22, 1, 0.36, 1)), box-shadow 0.24s ease;
}

.rec-row:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 22px rgba(31, 42, 68, 0.08);
}

.rec-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b6fe0, #5b8bf0);
}

.rec-row-main {
  flex: 1;
  min-width: 0;
}

.rec-title {
  margin: 0 0 4px;
  font-size: 13px;
  font-weight: 600;
}

.rec-action {
  flex-shrink: 0;
  padding: 6px 14px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #3b6fe0, #5b8bf0);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s var(--ease-apple, cubic-bezier(0.22, 1, 0.36, 1));
}

.rec-action:hover {
  transform: translateY(-1px);
}

.rec-action:active {
  transform: translateY(0);
}

@media (max-width: 900px) {
  .rec-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

// ------------------------------------------------------------
// 台词角
// ------------------------------------------------------------
.lines-panel {
  margin-top: 40px;
  padding: 20px 0 22px;
  overflow: hidden;
}

.lines-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding: 0 24px 14px;
}

.lines-hint {
  font-size: 12px;
}

.lines-lane {
  overflow: hidden;
  mask-image: linear-gradient(90deg, transparent, #000 5%, #000 95%, transparent);
  -webkit-mask-image: linear-gradient(90deg, transparent, #000 5%, #000 95%, transparent);
}

.lines-track {
  display: flex;
  width: max-content;
  animation: lines-flow 80s linear infinite;
}

.lines-lane:hover .lines-track {
  animation-play-state: paused;
}

@keyframes lines-flow {
  to {
    transform: translateX(-50%);
  }
}

.line-card {
  position: relative;
  flex: 0 0 292px;
  margin-right: 14px;
  padding: 15px 16px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition:
    transform 0.35s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.35s ease,
    border-color 0.35s ease;

  &:hover {
    transform: translateY(-4px);
    border-color: rgba(59, 111, 224, 0.3);
    box-shadow: 0 14px 28px rgba(31, 42, 68, 0.12);
  }

  &:focus-visible {
    outline: 2px solid var(--primary);
    outline-offset: 3px;
  }
}

// 卡片右下角的「跟读」角标：hover 时淡入
.line-tag {
  position: absolute;
  right: 12px;
  bottom: 12px;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 2px 8px;
  color: var(--primary);
  background: rgba(59, 111, 224, 0.12);
  border-radius: 999px;
  font-size: 10px;
  font-weight: 700;
  opacity: 0;
  transform: translateY(-4px);
  transition:
    opacity 0.3s ease,
    transform 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}

.line-card:hover .line-tag {
  opacity: 1;
  transform: none;
}

.line-en {
  margin: 0;
  color: var(--ink);
  font-family: var(--font-reading);
  font-size: 15px;
  line-height: 1.5;
}

.line-zh {
  margin: 5px 0 0;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.6;
}

.line-film {
  margin-top: 9px;
  color: var(--primary);
  font-size: 11px;
  font-weight: 600;
}

// ------------------------------------------------------------
// 练习小贴士
// ------------------------------------------------------------
.tips-section {
  margin-top: 34px;
}

.tips-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.tip-card {
  position: relative;
  padding: 18px 20px 20px;
}

.pet-corner {
  position: absolute;
  top: 0;
  right: 14px;
  z-index: 2;
}

.tip-icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  margin-bottom: 12px;
  color: var(--primary);
  background: rgba(59, 111, 224, 0.1);
  border-radius: 12px;
  font-size: 18px;
  transition: transform 0.45s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.tip-card:hover .tip-icon {
  transform: rotate(-8deg) scale(1.06);
}

.tip-card p {
  margin: 7px 0 0;
  font-size: 13px;
  line-height: 1.7;
}

// ------------------------------------------------------------
// 窄屏降级（仍保持桌面信息密度，不做移动端堆叠观感）
// ------------------------------------------------------------
@media (max-width: 1000px) {
  .hero-grid,
  .content-grid {
    grid-template-columns: 1fr;
    gap: 26px;
  }

  .hero-grid {
    padding-bottom: 26px;
  }

  .tips-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .tips-grid {
    grid-template-columns: 1fr;
  }

  .progress-panel {
    padding: 12px 18px 20px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .ticker-track,
  .lines-track {
    animation: none;
  }
}
</style>