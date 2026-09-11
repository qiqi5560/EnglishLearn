<template>
  <div class="entrance-result">
    <AppHeader title="测试结果" back />

    <div v-loading="loading" class="result-body">
      <template v-if="level">
        <el-result icon="success" title="测评完成">
          <template #sub-title>
            <p>你的初始英语水平等级为</p>
          </template>
        </el-result>

        <div class="level-display">
          <span class="level">{{ level }}</span>
          <span class="level-label">{{ levelDesc }}</span>
        </div>

        <el-card shadow="never" class="summary-card">
          <div class="summary-row">
            <span class="text-muted">学习目标</span>
            <el-tag>{{ targetGoal || '待制定' }}</el-tag>
          </div>
          <p class="summary-text">{{ summary }}</p>
        </el-card>

        <!-- 看图描述 · 大模型口语评价 -->
        <el-card v-if="speakingEval" shadow="never" class="eval-card">
          <div class="eval-title">看图描述 · 口语评价</div>
          <div class="eval-dims">
            <ScoreRing
              v-for="d in evalDims"
              :key="d.label"
              :score="d.score"
              :label="d.label"
              :size="56"
              :stroke="5"
            />
          </div>
          <p v-if="speakingEval.grammarFeedback" class="eval-feedback">
            {{ speakingEval.grammarFeedback }}
          </p>
          <p v-if="speakingEval.betterExpression" class="eval-better">
            更地道的表达：{{ speakingEval.betterExpression }}
          </p>
        </el-card>

        <el-button type="primary" class="next-btn" @click="$router.push('/plan')">
          查看我的学习方案
        </el-button>
      </template>

      <el-empty v-else-if="!loading" description="还没有测评记录">
        <el-button type="primary" @click="$router.push('/entrance-test')">开始测评</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppHeader from '@/components/base/AppHeader.vue'
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
.result-body {
  padding: 16px;
  text-align: center;
  min-height: 60vh;
}

.level-display {
  margin: 8px 0 24px;

  .level {
    font-size: 48px;
    font-weight: 800;
    color: var(--primary);
  }

  .level-label {
    display: block;
    color: var(--muted);
  }
}

.summary-card {
  text-align: left;
  border-radius: var(--radius-md);
  margin-bottom: 24px;

  .summary-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .summary-text {
    color: var(--el-text-color-regular);
    line-height: 1.7;
  }
}

.eval-card {
  text-align: left;
  border-radius: var(--radius-md);
  margin-bottom: 24px;

  .eval-title {
    font-weight: 700;
    margin-bottom: 12px;
  }

  .eval-dims {
    display: flex;
    justify-content: space-around;
    margin-bottom: 12px;
  }

  .eval-feedback {
    color: var(--el-text-color-regular);
    line-height: 1.7;
  }

  .eval-better {
    margin-top: 8px;
    color: var(--muted);
    font-size: 13px;
  }
}

.next-btn {
  width: 100%;
}
</style>
