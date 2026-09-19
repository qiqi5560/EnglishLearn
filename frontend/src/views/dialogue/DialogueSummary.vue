<template>
  <div class="dialogue-summary page">
    <AppHeader title="练习小结" back />

    <div v-loading="loading" class="page-shell summary-shell">
      <template v-if="summary">
        <div class="sum-head">
          <h1 class="sum-title">本次练习小结</h1>
          <p class="sum-sub text-muted">
            练习时长 {{ durationMin }} 分钟 · 共 {{ summary.corrections.length }} 处待纠正表达
          </p>
        </div>

        <!-- 综合评分 + 四维评分 -->
        <div class="overview-grid">
          <section class="total-card glass-card hover-lift">
            <span class="total-label">综合评分</span>
            <div class="total-score">{{ summary.total }}</div>
            <span class="total-foot text-muted">练习时长 {{ durationMin }} 分钟</span>
            <span class="total-glow" aria-hidden="true"></span>
          </section>

          <section class="dim-card glass-card">
            <div class="card-title">四维评分</div>
            <div class="dim-grid stagger">
              <div v-for="d in dims" :key="d.label" class="dim-item hover-lift">
                <ScoreRing :score="d.score" :label="d.label" :size="86" :stroke="7" />
              </div>
            </div>
          </section>
        </div>

        <!-- AI 教练小结 -->
        <section class="coach-card glass-card">
          <div class="card-title">AI 教练小结</div>
          <div class="coach-grid">
            <div v-if="summary.highlights.length" class="coach-block">
              <div class="coach-head is-good"><el-icon><Sunny /></el-icon>亮点</div>
              <ul class="coach-list">
                <li v-for="(h, i) in summary.highlights" :key="i">{{ h }}</li>
              </ul>
            </div>
            <div v-if="summary.improvements.length" class="coach-block">
              <div class="coach-head is-warn"><el-icon><Warning /></el-icon>待改进</div>
              <ul class="coach-list">
                <li v-for="(m, i) in summary.improvements" :key="i">{{ m }}</li>
              </ul>
            </div>
            <div v-if="summary.suggestions.length" class="coach-block">
              <div class="coach-head is-info"><el-icon><Opportunity /></el-icon>建议</div>
              <ul class="coach-list">
                <li v-for="(s, i) in summary.suggestions" :key="i">{{ s }}</li>
              </ul>
            </div>
          </div>
          <p v-if="summary.feedbackText" class="feedback-text">{{ summary.feedbackText }}</p>
        </section>

        <!-- 纠错详情 -->
        <section v-if="summary.corrections.length" class="fix-card glass-card">
          <div class="card-title">纠错详情</div>
          <el-collapse>
            <el-collapse-item title="逐句纠错" name="1">
              <div v-for="(c, i) in summary.corrections" :key="i" class="correction">
                <p class="correction-line">
                  <el-tag type="warning" size="small">{{ c.word || '—' }}</el-tag>
                  <span v-if="c.correct"> → {{ c.correct }}</span>
                </p>
                <p v-if="c.note" class="correction-note text-muted">{{ c.note }}</p>
              </div>
            </el-collapse-item>
          </el-collapse>
        </section>

        <div class="summary-actions">
          <el-button type="primary" size="large" class="again-btn" @click="$router.push('/practice')">
            再练一次
          </el-button>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="暂无本次练习的小结数据">
        <el-button type="primary" @click="$router.push('/practice')">返回练习</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/base/AppHeader.vue'
import ScoreRing from '@/components/base/ScoreRing.vue'
import { useSessionStore } from '@/stores/session'
import type { DialogueSummaryDto } from '@/types/api'

const route = useRoute()
const sessionStore = useSessionStore()
const loading = ref(false)

const summary = computed<DialogueSummaryDto | null>(() => sessionStore.summary)

const dims = computed(() => {
  const d = summary.value?.dimensions
  return [
    { label: '发音', score: d?.pron ?? 0 },
    { label: '流利度', score: d?.fluency ?? 0 },
    { label: '反应', score: d?.reaction ?? 0 },
    { label: '自然度', score: d?.natural ?? 0 },
  ]
})

/** 纯展示：练习时长（分钟，至少 1 分钟） */
const durationMin = computed(() => Math.max(1, Math.round((summary.value?.durationSec || 0) / 60)))

onMounted(async () => {
  const sid = Number(route.params.sessionId)
  const cached = sessionStore.summary
  if (cached && (cached.sessionId === sid || !sid)) return
  if (!Number.isInteger(sid)) return
  loading.value = true
  try {
    await sessionStore.loadSummary(sid)
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

.summary-shell {
  min-height: 60vh;
}

.sum-head {
  margin-bottom: 22px;

  .sum-title {
    margin: 0;
    font-size: 32px;
    font-weight: 700;
    letter-spacing: -0.035em;
    color: var(--ink);
  }

  .sum-sub {
    margin: 8px 0 0;
    font-size: 13.5px;
  }
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--ink);
}

// ---------------- 报表首屏：综合评分 + 四维 ----------------
.overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 320px) minmax(0, 1fr);
  gap: 22px;
  align-items: stretch;
}

.total-card {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 34px 26px;
  text-align: center;

  .total-label {
    font-size: 13px;
    font-weight: 700;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: var(--muted);
  }

  .total-score {
    margin: 6px 0 2px;
    font-size: 82px;
    font-weight: 800;
    line-height: 1.05;
    letter-spacing: -0.05em;
    color: var(--primary);
  }

  .total-foot {
    font-size: 12.5px;
  }

  .total-glow {
    position: absolute;
    bottom: -150px;
    left: 50%;
    width: 320px;
    height: 320px;
    border-radius: 50%;
    transform: translateX(-50%);
    background: radial-gradient(circle, rgba(59, 111, 224, 0.16), rgba(59, 111, 224, 0) 70%);
    pointer-events: none;
  }
}

.dim-card {
  padding: 26px 28px;
}

.dim-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.dim-item {
  display: grid;
  place-items: center;
  padding: 18px 8px 14px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.66);
}

// ---------------- AI 教练小结 ----------------
.coach-card {
  margin-top: 22px;
  padding: 26px 28px;
}

.coach-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
  margin-top: 18px;
}

.coach-block {
  padding: 18px 20px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.66);
  border: 1px solid var(--border);
  transition:
    transform 0.4s $ease-apple,
    box-shadow 0.4s $ease-apple;

  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-md);
  }
}

.coach-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 10px;

  &.is-good {
    color: var(--success);
  }

  &.is-warn {
    color: var(--warning);
  }

  &.is-info {
    color: var(--primary);
  }
}

.coach-list {
  list-style: none;
  margin: 0;
  padding: 0;

  li {
    position: relative;
    padding: 4px 0 4px 16px;
    font-size: 13.5px;
    line-height: 1.65;
    color: var(--ink);

    &::before {
      content: '';
      position: absolute;
      left: 2px;
      top: 12px;
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: var(--primary);
      opacity: 0.65;
    }
  }
}

.feedback-text {
  margin: 20px 0 0;
  padding: 16px 20px;
  border-radius: var(--radius-md);
  background: rgba(59, 111, 224, 0.07);
  font-size: 13.5px;
  line-height: 1.75;
  color: var(--ink);
}

// ---------------- 纠错详情 ----------------
.fix-card {
  margin-top: 22px;
  padding: 26px 28px;
}

.correction {
  padding: 10px 0 8px;

  & + .correction {
    border-top: 1px solid var(--border);
  }

  .correction-line {
    margin: 0 0 4px;
    font-size: 14px;
    font-weight: 600;
    color: var(--ink);
  }

  .correction-note {
    margin: 0;
    font-size: 12.5px;
    line-height: 1.6;
  }
}

// ---------------- 底部操作 ----------------
.summary-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;

  .again-btn {
    height: 48px;
    padding: 0 34px;
    border-radius: 999px;
    font-size: 15px;
    font-weight: 600;
    border-color: transparent;
    background: var(--primary);
    box-shadow: 0 12px 26px rgba(59, 111, 224, 0.28);
    transition:
      transform 0.35s $ease-spring,
      box-shadow 0.35s $ease-apple;

    &:hover {
      transform: translateY(-3px);
      background: var(--primary);
      box-shadow: 0 18px 34px rgba(59, 111, 224, 0.38);
    }
  }
}

// ---------------- 窄屏降级 ----------------
@media (max-width: 900px) {
  .overview-grid,
  .coach-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .dim-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .sum-head .sum-title {
    font-size: 25px;
  }

  .total-card .total-score {
    font-size: 64px;
  }
}
</style>