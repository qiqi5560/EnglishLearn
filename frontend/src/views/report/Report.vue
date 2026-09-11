<template>
  <div class="report page">
    <AppHeader title="学习报表">
      <template #right>
        <el-button link type="primary" @click="onExport">导出</el-button>
      </template>
    </AppHeader>

    <div v-loading="reportStore.loading" class="report-body">
      <template v-if="overview">
        <!-- 关键指标 -->
        <div class="metric-row">
          <el-card v-for="m in metrics" :key="m.label" shadow="never" class="metric-card">
            <div class="metric-value" :style="{ color: m.color }">{{ m.value }}</div>
            <div class="text-muted">{{ m.label }}</div>
          </el-card>
        </div>

        <!-- 成长曲线 -->
        <div class="section-title">口语成长曲线</div>
        <el-card shadow="never">
          <GrowthLine :dates="overview.growth.dates" :scores="overview.growth.scores" />
        </el-card>

        <!-- 能力雷达图 -->
        <div class="section-title">能力雷达图</div>
        <el-card shadow="never">
          <RadarChart :indicators="overview.radar.indicators" :values="overview.radar.values" />
        </el-card>
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
import GrowthLine from '@/components/business/chart/GrowthLine.vue'
import RadarChart from '@/components/business/chart/RadarChart.vue'
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
.report-body {
  padding: 0 16px;
  min-height: 60vh;
}

.metric-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 16px;
}

.metric-card {
  text-align: center;
  border-radius: var(--radius-md);

  .metric-value {
    font-size: 28px;
    font-weight: 800;
  }
}
</style>
