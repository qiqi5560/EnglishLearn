<template>
  <div class="dialogue-summary">
    <AppHeader title="练习小结" back />

    <div v-loading="loading" class="summary-body">
      <template v-if="summary">
        <!-- 综合评分 -->
        <el-card shadow="never" class="total-card">
          <div class="total-score">{{ summary.total }}</div>
          <div class="text-muted">综合评分 · 练习时长 {{ Math.max(1, Math.round((summary.durationSec || 0) / 60)) }} 分钟</div>
        </el-card>

        <!-- 四维分项 -->
        <div class="section-title">四维评分</div>
        <div class="dimension-scores">
          <ScoreRing v-for="d in dims" :key="d.label" :score="d.score" :label="d.label" />
        </div>

        <!-- AI 小结 -->
        <div class="section-title">AI 教练小结</div>
        <el-card shadow="never">
          <template v-if="summary.highlights.length">
            <p class="summary-block">
              <strong>亮点：</strong>
              <span v-for="(h, i) in summary.highlights" :key="i" class="summary-line">· {{ h }}</span>
            </p>
          </template>
          <template v-if="summary.improvements.length">
            <p class="summary-block">
              <strong>待改进：</strong>
              <span v-for="(m, i) in summary.improvements" :key="i" class="summary-line">· {{ m }}</span>
            </p>
          </template>
          <template v-if="summary.suggestions.length">
            <p class="summary-block">
              <strong>建议：</strong>
              <span v-for="(s, i) in summary.suggestions" :key="i" class="summary-line">· {{ s }}</span>
            </p>
          </template>
          <p v-if="summary.feedbackText" class="feedback-text">{{ summary.feedbackText }}</p>
        </el-card>

        <!-- 纠错详情 -->
        <template v-if="summary.corrections.length">
          <div class="section-title">纠错详情</div>
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
        </template>

        <el-button type="primary" class="again-btn" @click="$router.push('/practice')">再练一次</el-button>
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
.summary-body {
  padding: 16px;
  min-height: 60vh;
}

.total-card {
  text-align: center;
  border-radius: var(--radius-md);

  .total-score {
    font-size: 56px;
    font-weight: 800;
    color: var(--primary);
  }
}

.dimension-scores {
  display: flex;
  justify-content: space-around;
}

.summary-block {
  strong {
    display: block;
    margin-bottom: 4px;
  }

  .summary-line {
    display: block;
    padding: 1px 0;
  }
}

.feedback-text {
  color: var(--muted);
  font-size: 13px;
}

.correction {
  padding: 4px 0;

  .correction-line {
    margin: 0 0 2px;
  }

  .correction-note {
    margin: 0 0 6px;
    font-size: 12px;
  }
}

.again-btn {
  width: 100%;
  margin-top: 20px;
}
</style>
