<template>
  <div class="report page">
    <AppHeader title="学习报表">
      <template #right>
        <SharePanel contentType="achievement" />
        <el-button link type="primary" @click="onExport">导出</el-button>
      </template>
    </AppHeader>

    <div v-loading="reportStore.loading" class="page-shell report-body">
      <template v-if="overview">
        <!-- 概览标题 -->
        <div class="overview-head">
          <h2 class="overview-title">学习数据总览</h2>
          <p class="overview-sub text-muted">每一次开口都会被记录，坚持练习就能看到曲线变化。</p>
        </div>

        <!-- 关键指标：四栏网格 -->
        <div class="metric-grid stagger">
          <article v-for="m in metrics" :key="m.label" class="metric-card glass-card hover-lift sheen">
            <div class="metric-value" :style="{ color: m.color }">{{ m.value }}</div>
            <div class="metric-label text-muted">{{ m.label }}</div>
            <span class="metric-bar" :style="{ background: m.color }"></span>
          </article>
        </div>

        <!-- 图表：宽屏并排两栏，窄屏降级单列 -->
        <div class="chart-grid">
          <section class="chart-card glass-card hover-lift peek-host">
            <header class="chart-head">
              <div class="chart-titles">
                <h3>口语成长曲线</h3>
                <p class="text-muted">每次练习综合分的走势</p>
              </div>
              <PeekMascot class="chart-mascot" :size="46" />
            </header>
            <GrowthLine :dates="overview.growth.dates" :scores="overview.growth.scores" />
          </section>

          <section class="chart-card glass-card hover-lift">
            <header class="chart-head">
              <div class="chart-titles">
                <h3>能力雷达图</h3>
                <p class="text-muted">发音、流利度、反应与自然度的均衡度</p>
              </div>
            </header>
            <RadarChart :indicators="overview.radar.indicators" :values="overview.radar.values" />
          </section>
        </div>
      </template>

      <el-empty v-else-if="!reportStore.loading" description="暂无学习数据，快去完成一次练习吧">
        <el-button type="primary" @click="$router.push('/practice')">去练习</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
import PeekMascot from '@/components/base/PeekMascot.vue'
import GrowthLine from '@/components/business/chart/GrowthLine.vue'
import RadarChart from '@/components/business/chart/RadarChart.vue'
import SharePanel from '@/components/business/SharePanel.vue'
import { useReportStore } from '@/stores/report'

const reportStore = useReportStore()

const overview = computed(() => reportStore.overview)

const metrics = computed(() => {
  const s = overview.value?.stats
  if (!s) return []
  return [
    { label: '累计学习（分钟）', value: s.totalMinutes, color: 'var(--primary)' },
    { label: '练习次数', value: s.totalSessions, color: 'var(--success)' },
    { label: '平均综合分', value: s.avgScore ?? '--', color: 'var(--accent)' },
    { label: '活跃天数', value: s.activeDays, color: 'var(--warning)' },
  ]
})

function onExport() {
  ElMessage.success('报表已导出为图片')
}

onMounted(() => {
  reportStore.load()
})
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);

.report-body {
  min-height: 60vh;
}

.overview-head {
  margin-bottom: 22px;
}

.overview-title {
  margin: 0;
  font-size: clamp(26px, 2.4vw, 34px);
  font-weight: 700;
  letter-spacing: -0.03em;
}

.overview-sub {
  margin: 8px 0 0;
  font-size: 14px;
}

// ---------------- 关键指标：一排四栏 ----------------
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.metric-card {
  position: relative;
  padding: 22px 24px 24px;
  overflow: hidden;
}

.metric-value {
  font-size: clamp(28px, 2.6vw, 36px);
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: -0.035em;
}

.metric-label {
  margin-top: 8px;
  font-size: 13px;
}

.metric-bar {
  display: block;
  width: 34px;
  height: 3px;
  margin-top: 16px;
  border-radius: 999px;
  opacity: 0.85;
  transition: width 0.45s $ease-apple;
}

.metric-card:hover .metric-bar {
  width: 64px;
}

// ---------------- 图表：并排两栏 ----------------
.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 22px;
  margin-top: 26px;
}

.chart-card {
  padding: 22px 24px 18px;
  min-width: 0;
}

.chart-head {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  margin-bottom: 6px;
}

.chart-titles {
  min-width: 0;

  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }

  p {
    margin: 6px 0 0;
    font-size: 13px;
  }
}

.chart-mascot {
  margin-left: auto;
  flex: 0 0 auto;
}

@media (max-width: 1080px) {
  .chart-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 900px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
  }

  .metric-card {
    padding: 18px 18px 20px;
  }
}

@media (max-width: 560px) {
  .metric-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>