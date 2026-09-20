<template>
  <div v-loading="loading" class="metrics">
    <AppHeader title="推荐效果指标" />

    <main class="page-shell">
      <header class="page-head">
        <div>
          <p class="eyebrow">RECOMMENDATION METRICS</p>
          <h1>推荐效果与水平预测模型</h1>
          <p class="text-muted stage-note">{{ metrics?.stageNote || '四项指标按统一口径统计，样本不足时显示「—」。' }}</p>
        </div>
        <el-radio-group v-model="days" size="small" @change="load">
          <el-radio-button :value="7">近 7 天</el-radio-button>
          <el-radio-button :value="14">近 14 天</el-radio-button>
          <el-radio-button :value="30">近 30 天</el-radio-button>
        </el-radio-group>
      </header>

      <!-- ==================== 四指标卡 ==================== -->
      <section class="stat-grid">
        <article v-for="card in cards" :key="card.key" class="stat-card">
          <span v-if="card.insufficient" class="stat-flag">样本不足</span>
          <p class="stat-label">{{ card.name }}</p>
          <p class="stat-value" :style="{ color: card.color }">
            {{ card.value === null ? '—' : `${card.value}%` }}
          </p>
          <p class="stat-hint text-muted">{{ card.hint }}</p>
        </article>
      </section>

      <!-- ==================== 趋势 ==================== -->
      <section class="panel glass-card">
        <header class="panel-head">
          <h2>指标趋势</h2>
          <span class="text-muted">近 {{ days }} 天</span>
        </header>
        <TrendChart v-if="metrics" :labels="metrics.trend.dates" :series="series" :height="280" />
      </section>

      <!-- ==================== 口径定义 ==================== -->
      <section class="panel glass-card">
        <header class="panel-head">
          <h2>指标口径（检测标准）</h2>
          <span class="text-muted">共 {{ metrics?.definitions.length ?? 0 }} 项</span>
        </header>
        <el-table :data="metrics?.definitions ?? []" size="small" stripe>
          <el-table-column prop="name" label="指标" width="96" />
          <el-table-column prop="formula" label="计算公式" min-width="280" />
          <el-table-column prop="source" label="数据来源" min-width="190" />
        </el-table>
      </section>

      <!-- ==================== 模型 ==================== -->
      <section class="panel glass-card">
        <header class="panel-head">
          <h2>口语水平预测模型</h2>
          <el-button type="primary" :loading="training" @click="train">重新训练</el-button>
        </header>
        <div class="model-grid">
          <div class="model-item">
            <span>当前状态</span>
            <strong>{{ model?.ready ? '已训练 · 模型预测' : '未训练 · 规则估算' }}</strong>
          </div>
          <div class="model-item">
            <span>训练样本</span>
            <strong>{{ model?.sampleCount ?? '—' }} 条</strong>
          </div>
          <div class="model-item">
            <span>验证准确率</span>
            <strong>{{ model?.accuracy != null ? `${model.accuracy}%` : '—' }}</strong>
          </div>
          <div class="model-item">
            <span>训练时间</span>
            <strong>{{ formatTime(model?.trainedAt) }}</strong>
          </div>
          <div class="model-item">
            <span>预测档位</span>
            <strong>{{ model?.bands?.length ? model.bands.join(' / ') : '—' }}</strong>
          </div>
          <div class="model-item">
            <span>训练样本下限</span>
            <strong>{{ model?.minSamples ?? '—' }} 条</strong>
          </div>
        </div>
        <p class="text-muted model-note">
          训练以「有评测记录的会话」为样本，标签由综合评分分箱自动生成（发音 35% / 流利度 25% / 反应 20% / 自然度 20%），
          与会话得分口径严格一致。
        </p>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
import TrendChart from '@/components/business/chart/TrendChart.vue'
import { adminMetrics, adminModelStatus, adminTrainModel } from '@/api/modules/recommend'
import type { AdminMetrics, ModelStatus } from '@/types/api'

const METRIC_META = [
  { key: 'ctr', name: '点击率', color: '#3b6fe0', hint: '已产生学习行为的任务占比' },
  { key: 'completionRate', name: '完成率', color: '#22a06b', hint: '已完成的每日任务占比' },
  { key: 'interactionRate', name: '互动率', color: '#f5a623', hint: '有社区互动的活跃用户占比' },
  { key: 'bounceRate', name: '跳出率', color: '#e5484d', hint: '未开口或停留过短的会话占比' },
] as const

const loading = ref(false)
const training = ref(false)
const days = ref(14)
const metrics = ref<AdminMetrics | null>(null)
const model = ref<ModelStatus | null>(null)

const cards = computed(() => {
  const summary = metrics.value?.summary
  const insufficient = metrics.value?.sample.insufficient ?? true
  return METRIC_META.map((meta) => {
    const value = summary ? summary[meta.key] ?? null : null
    return { ...meta, value, insufficient: insufficient && value === null }
  })
})

const series = computed(() => {
  const trend = metrics.value?.trend
  if (!trend) {
    return []
  }
  return METRIC_META.map((meta) => ({
    name: meta.name,
    color: meta.color,
    values: (trend[meta.key] ?? []).map((value) => Number(value ?? 0)),
  }))
})

async function load() {
  loading.value = true
  try {
    metrics.value = await adminMetrics(days.value)
  } catch {
    ElMessage.error('指标加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function loadModel() {
  try {
    model.value = await adminModelStatus()
  } catch {
    model.value = null
  }
}

async function train() {
  training.value = true
  try {
    const result = await adminTrainModel()
    if (result.trained) {
      ElMessage.success(`训练完成：样本 ${result.sampleCount} 条，验证准确率 ${result.accuracy}%`)
    } else {
      ElMessage.warning(result.reason || '样本不足，暂未训练')
    }
    await Promise.all([loadModel(), load()])
  } catch {
    ElMessage.error('模型训练失败，请稍后重试')
  } finally {
    training.value = false
  }
}

function formatTime(value?: string | null) {
  if (!value) {
    return '—'
  }
  return value.replace('T', ' ').slice(0, 16)
}

onMounted(async () => {
  await Promise.all([load(), loadModel()])
})
</script>

<style scoped lang="scss">
.metrics {
  background: var(--surface);
}

.page-shell {
  padding: 24px 0 60px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.page-head h1 {
  margin: 6px 0;
  font-size: 22px;
  font-weight: 700;
}

.stage-note {
  margin: 0;
  font-size: 13px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.stat-card {
  position: relative;
  padding: 16px 18px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid rgba(31, 42, 68, 0.06);
  box-shadow: 0 8px 22px rgba(31, 42, 68, 0.05);
  transition: transform 0.28s var(--ease-apple, cubic-bezier(0.22, 1, 0.36, 1));
}

.stat-card:hover {
  transform: translateY(-3px);
}

.stat-flag {
  position: absolute;
  top: 10px;
  right: 12px;
  padding: 1px 8px;
  border-radius: 999px;
  background: rgba(163, 170, 185, 0.18);
  color: #7a8399;
  font-size: 11px;
}

.stat-label {
  margin: 0 0 6px;
  font-size: 13px;
  font-weight: 600;
  color: #55617e;
}

.stat-value {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
}

.stat-hint {
  margin: 6px 0 0;
  font-size: 12px;
}

.panel {
  margin-bottom: 18px;
  padding: 18px 20px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-head h2 {
  font-size: 16px;
  font-weight: 700;
}

.model-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.model-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(246, 247, 251, 0.9);
}

.model-item span {
  font-size: 12px;
  color: #8a93a6;
}

.model-item strong {
  font-size: 14px;
  font-weight: 600;
}

.model-note {
  margin: 12px 0 0;
  font-size: 12px;
}

@media (max-width: 1080px) {
  .stat-grid,
  .model-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
