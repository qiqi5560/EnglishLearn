<template>
  <div class="entrance-result page">
    <AppHeader title="测试结果" back>
      <template #right>
        <span v-if="level" class="level-chip">{{ level }}</span>
      </template>
    </AppHeader>

    <div v-loading="loading" class="page-shell result-body">
      <template v-if="level">
        <!-- 等级大卡片 -->
        <section class="glass-card sheen level-hero">
          <div class="hero-main">
            <p class="eyebrow">测评完成 · 当前英语水平</p>
            <div class="level-display">
              <span class="level">{{ level }}</span>
              <span class="level-label">{{ levelDesc }}</span>
            </div>
            <p class="summary-text">{{ summary }}</p>
          </div>

          <aside class="hero-side">
            <div class="side-item">
              <span class="side-label text-muted">学习目标</span>
              <el-tag effect="light" size="large">{{ targetGoal || '待制定' }}</el-tag>
            </div>
            <div class="side-item">
              <span class="side-label text-muted">起始等级</span>
              <span class="side-value">{{ level }} 起点</span>
            </div>
          </aside>
        </section>

        <div class="result-grid">
          <!-- 各维度评分 -->
          <section v-if="speakingEval" class="glass-card eval-card hover-lift">
            <div class="card-head">
              <h3>看图描述 · 口语评价</h3>
              <span class="card-sub text-muted">四维评分</span>
            </div>

            <div class="eval-dims">
              <ScoreRing
                v-for="d in evalDims"
                :key="d.label"
                :score="d.score"
                :label="d.label"
                :size="76"
                :stroke="6"
              />
            </div>

            <p v-if="speakingEval.grammarFeedback" class="eval-feedback">
              {{ speakingEval.grammarFeedback }}
            </p>
            <p v-if="speakingEval.betterExpression" class="eval-better">
              更地道的表达：{{ speakingEval.betterExpression }}
            </p>
          </section>

          <!-- 后续建议 -->
          <aside class="glass-card advice-card peek-host">
            <PeekMascot :size="50" />
            <div class="card-head">
              <h3>后续建议</h3>
              <span class="card-sub text-muted">按顺序推进</span>
            </div>

            <ul class="advice-list stagger">
              <li v-for="a in advices" :key="a.title" class="advice-item">
                <span class="advice-icon"><el-icon><component :is="a.icon" /></el-icon></span>
                <div class="advice-text">
                  <strong>{{ a.title }}</strong>
                  <span>{{ a.desc }}</span>
                </div>
                <el-icon class="advice-arrow"><ArrowRight /></el-icon>
              </li>
            </ul>
          </aside>
        </div>

        <div class="result-footer">
          <el-button type="primary" size="large" class="next-btn" @click="$router.push('/plan')">
            查看我的学习方案
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="还没有测评记录">
        <el-button type="primary" @click="$router.push('/entrance-test')">开始测评</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowRight, Microphone, Reading, TrendCharts } from '@element-plus/icons-vue'
import AppHeader from '@/components/base/AppHeader.vue'
import PeekMascot from '@/components/base/PeekMascot.vue'
import ScoreRing from '@/components/base/ScoreRing.vue'
import { usePlanStore } from '@/stores/plan'

const LEVEL_DESC: Record<string, string> = {
  A1: '入门 · 能进行非常基础的问候与自我介绍',
  A2: '初级 · 能进行日常简单交流',
  B1: '中级 · 可围绕熟悉话题进行讨论',
  B2: '中高级 · 能较流利地表达观点',
  C1: '高级 · 语言运用自如、表达准确',
  C2: '精通 · 接近母语水平',
}

// 纯展示：后续建议
const advices = [
  { icon: TrendCharts, title: '完成每日任务', desc: '按方案推送的场景对话，每天开口 10 分钟' },
  { icon: Microphone, title: '优先补足低分维度', desc: '针对评分较低的维度做专项复练' },
  { icon: Reading, title: '精听 + 跟读', desc: '把听到的地道表达逐句内化成自己的' },
]

const planStore = usePlanStore()
const loading = ref(false)

const level = computed(() => planStore.entranceResult?.level ?? planStore.level ?? null)
const targetGoal = computed(() => planStore.entranceResult?.targetGoal ?? planStore.targetGoal ?? '')
const summary = computed(
  () =>
    planStore.entranceResult?.summary ??
    `根据当前表现，已为你定位 ${level.value || 'A1'} 起始等级并生成学习方案。`,
)
const levelDesc = computed(() => LEVEL_DESC[level.value ?? ''] ?? '')

const speakingEval = computed(() => planStore.entranceResult?.speakingEval ?? null)
const evalDims = computed(() => {
  const e = speakingEval.value
  if (!e) return []
  return [
    { label: '发音', score: e.pron },
    { label: '流利度', score: e.fluency },
    { label: '反应', score: e.reaction },
    { label: '自然度', score: e.natural },
  ]
})

onMounted(async () => {
  if (!level.value) {
    loading.value = true
    try {
      await planStore.loadAll()
    } catch {
      /* 错误提示已由请求层统一弹出 */
    } finally {
      loading.value = false
    }
  }
})
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

.level-chip {
  padding: 7px 18px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 0.02em;
  color: var(--primary);
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(59, 111, 224, 0.24);
  box-shadow: var(--shadow-sm);
}

.result-body {
  min-height: 60vh;
}

// ---------------- 等级大卡片 ----------------
.level-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 268px;
  gap: 32px;
  align-items: center;
  padding: 36px 40px;

  .eyebrow {
    margin: 0;
    font-size: 12.5px;
    font-weight: 700;
    letter-spacing: 0.1em;
    text-transform: uppercase;
    color: var(--muted);
  }

  .level-display {
    display: flex;
    align-items: baseline;
    gap: 18px;
    margin: 14px 0 18px;

    .level {
      font-size: 76px;
      line-height: 1;
      font-weight: 800;
      letter-spacing: -0.04em;
      color: var(--primary);
    }

    .level-label {
      font-size: 15px;
      color: var(--muted);
    }
  }

  .summary-text {
    margin: 0;
    max-width: 560px;
    font-size: 14.5px;
    line-height: 1.75;
    color: var(--ink);
  }

  .hero-side {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding-left: 32px;
    border-left: 1px solid var(--border);
  }

  .side-item {
    display: flex;
    flex-direction: column;
    gap: 8px;

    .side-label {
      font-size: 12.5px;
      font-weight: 600;
    }

    .side-value {
      font-size: 16px;
      font-weight: 700;
      letter-spacing: -0.01em;
    }
  }
}

// ---------------- 报表网格 ----------------
.result-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(0, 1fr);
  gap: 26px;
  align-items: start;
  margin-top: 26px;
}

.card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;

  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }

  .card-sub {
    font-size: 12.5px;
  }
}

.eval-card {
  padding: 26px 30px 30px;

  .eval-dims {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
    padding: 20px 0 22px;
    border-top: 1px solid var(--border);
    border-bottom: 1px solid var(--border);
  }

  .eval-feedback {
    margin: 18px 0 0;
    font-size: 14.5px;
    line-height: 1.75;
    color: var(--ink);
  }

  .eval-better {
    margin: 10px 0 0;
    padding: 12px 16px;
    border-radius: var(--radius-md);
    font-size: 13.5px;
    line-height: 1.7;
    color: var(--primary);
    background: rgba(59, 111, 224, 0.08);
  }
}

.advice-card {
  padding: 22px 24px 24px;

  .advice-list {
    margin: 0;
    padding: 0;
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .advice-item {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 14px 16px;
    border-radius: var(--radius-md);
    background: rgba(255, 255, 255, 0.7);
    border: 1px solid var(--border);
    transition:
      transform 0.35s $ease-apple,
      box-shadow 0.35s $ease-apple,
      border-color 0.35s ease;

    &:hover {
      transform: translateY(-3px);
      border-color: rgba(59, 111, 224, 0.26);
      box-shadow: var(--shadow-md);

      .advice-icon {
        transform: scale(1.08) rotate(-6deg);
      }

      .advice-arrow {
        transform: translateX(4px);
        opacity: 1;
      }
    }

    .advice-icon {
      flex: 0 0 auto;
      width: 38px;
      height: 38px;
      display: grid;
      place-items: center;
      font-size: 19px;
      border-radius: 12px;
      color: var(--primary);
      background: rgba(59, 111, 224, 0.1);
      transition: transform 0.5s $ease-spring;
    }

    .advice-text {
      display: flex;
      flex-direction: column;
      gap: 3px;
      min-width: 0;

      strong {
        font-size: 14px;
        font-weight: 700;
        letter-spacing: -0.01em;
      }

      span {
        font-size: 12.5px;
        line-height: 1.6;
        color: var(--muted);
      }
    }

    .advice-arrow {
      margin-left: auto;
      color: var(--primary);
      opacity: 0.4;
      transition:
        transform 0.4s $ease-apple,
        opacity 0.3s ease;
    }
  }
}

// ---------------- 底部动作 ----------------
.result-footer {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

.next-btn {
  border-radius: 999px;
  padding: 0 30px;
  height: 48px;
  font-size: 15px;
  font-weight: 600;

  .el-icon {
    margin-left: 6px;
    transition: transform 0.4s $ease-apple;
  }

  &:hover .el-icon {
    transform: translateX(4px);
  }
}

// ---------------- 窄屏兜底 ----------------
@media (max-width: 900px) {
  .level-hero {
    grid-template-columns: 1fr;
    padding: 24px;

    .level-display .level {
      font-size: 54px;
    }

    .hero-side {
      padding-left: 0;
      padding-top: 18px;
      border-left: none;
      border-top: 1px solid var(--border);
    }
  }

  .result-grid {
    grid-template-columns: 1fr;
  }

  .eval-card .eval-dims {
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;
  }
}
</style>