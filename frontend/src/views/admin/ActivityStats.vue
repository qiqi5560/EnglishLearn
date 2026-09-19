<template>
  <div class="page page-shell page-shell--wide">
    <header class="page-head">
      <h1 class="page-title">日活动量</h1>
      <p class="page-desc">按天统计活跃用户、新增注册、对话场次与学习记录</p>
    </header>

    <div class="toolbar">
      <div class="toolbar-left">
        <span class="toolbar-label">统计范围</span>
        <el-select v-model="days" class="toolbar-select" @change="load">
          <el-option :value="7" label="近 7 天" />
          <el-option :value="14" label="近 14 天" />
          <el-option :value="30" label="近 30 天" />
        </el-select>
      </div>
      <div class="toolbar-right">
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <div v-loading="loading" class="stat-grid stagger">
      <el-card v-for="s in stats" :key="s.label" shadow="never" class="stat-card hover-lift sheen">
        <div class="stat-value" :style="{ color: s.color }">{{ s.value }}</div>
        <div class="stat-label text-muted">{{ s.label }}</div>
      </el-card>
    </div>

    <el-card shadow="never" class="panel-card">
      <div class="card-title">
        <span>活动趋势</span>
        <em class="text-muted">最近 {{ days }} 天</em>
      </div>
      <TrendChart v-if="trend" :labels="trend.dates" :series="series" :height="280" />
    </el-card>

    <el-card shadow="never" class="table-card">
      <div class="card-title">
        <span>每日明细</span>
        <em class="text-muted">共 {{ rows.length }} 天</em>
      </div>
      <el-table :data="rows" stripe>
        <el-table-column prop="date" label="日期" width="130" />
        <el-table-column prop="activeUsers" label="活跃用户" width="100" align="center" />
        <el-table-column prop="newUsers" label="新增注册" width="100" align="center" />
        <el-table-column prop="sessions" label="对话场次" width="100" align="center" />
        <el-table-column prop="studyRecords" label="学习记录" width="100" align="center" />
        <el-table-column label="练习时长" min-width="110" align="center">
          <template #default="{ row }">{{ row.durationMin }} 分钟</template>
        </el-table-column>
        <el-table-column label="活跃度" min-width="180">
          <template #default="{ row }">
            <div class="bar-cell">
              <span class="bar" :style="{ width: `${barWidth(row.activeUsers)}%` }" />
              <em class="text-muted">{{ row.activeUsers }}</em>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !rows.length" description="暂无日活动数据" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import TrendChart from '@/components/business/chart/TrendChart.vue'
import { adminActivity } from '@/api/modules/admin'
import type { AdminActivity } from '@/types/api'

const loading = ref(false)
const days = ref(14)
const data = ref<AdminActivity | null>(null)

const rows = computed(() => data.value?.list ?? [])
const trend = computed(() => data.value?.trend ?? null)

const series = computed(() => {
  const t = data.value?.trend
  if (!t) return []
  return [
    { name: '活跃用户', values: t.activeUsers, color: '#3b6fe0', type: 'line' as const, area: true },
    { name: '对话场次', values: t.sessions, color: '#ff6a4d', type: 'bar' as const },
    { name: '学习记录', values: t.studyRecords, color: '#22a06b', type: 'line' as const },
    { name: '新增注册', values: t.newUsers, color: '#ffb347', type: 'line' as const },
  ]
})

const stats = computed(() => {
  const s = data.value?.summary
  if (!s) return []
  return [
    { label: '今日活跃用户', value: String(s.todayActive), color: 'var(--primary)' },
    { label: '日均活跃用户', value: String(s.avgActive), color: 'var(--success)' },
    { label: '峰值活跃', value: String(s.peakActive), color: 'var(--accent)' },
    { label: '今日对话场次', value: String(s.todaySessions), color: 'var(--warning)' },
  ]
})

const maxActive = computed(() => Math.max(1, ...rows.value.map((r) => r.activeUsers)))

function barWidth(value: number) {
  return Math.round((value / maxActive.value) * 100)
}

async function load() {
  loading.value = true
  try {
    data.value = await adminActivity(days.value)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);

.page-head {
  margin-bottom: 24px;
}

.page-title {
  margin: 0;
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.page-desc {
  margin: 7px 0 0;
  font-size: 13.5px;
  color: var(--muted);
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  padding: 12px 16px;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(31, 42, 68, 0.07);
  background: rgba(255, 255, 255, 0.78);
  box-shadow: var(--shadow-sm);
  backdrop-filter: blur(14px) saturate(150%);
  -webkit-backdrop-filter: blur(14px) saturate(150%);
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar-label {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--muted);
}

.toolbar-select {
  width: 130px;
}

.toolbar-right {
  flex-shrink: 0;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 18px;
}

.stat-card {
  border-radius: var(--radius-lg);
  border: 1px solid rgba(31, 42, 68, 0.07);
  background: var(--card);
  box-shadow: var(--shadow-sm);

  :deep(.el-card__body) {
    padding: 20px 22px 18px;
  }

  .stat-value {
    font-size: 28px;
    font-weight: 800;
    line-height: 1.1;
    letter-spacing: -0.03em;
  }

  .stat-label {
    margin-top: 7px;
    font-size: 12.5px;
    font-weight: 600;
  }
}

.panel-card,
.table-card {
  border-radius: var(--radius-lg);
  border: 1px solid rgba(31, 42, 68, 0.07);
  background: var(--card);
  box-shadow: var(--shadow-sm);

  :deep(.el-card__body) {
    padding: 20px 22px;
  }
}

.table-card {
  margin-top: 18px;
}

.card-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 14px;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.02em;

  em {
    font-size: 12px;
    font-style: normal;
    font-weight: 500;
  }
}

.bar-cell {
  display: flex;
  align-items: center;
  gap: 10px;

  .bar {
    display: block;
    height: 8px;
    min-width: 4px;
    border-radius: 999px;
    background: linear-gradient(90deg, #5b8bf0, #3b6fe0);
    transition: width 0.6s $ease-apple;
  }

  em {
    font-size: 12px;
    font-style: normal;
  }
}

:deep(.el-table) {
  --el-table-border-color: var(--border);
  --el-table-header-bg-color: var(--surface);
  --el-table-row-hover-bg-color: rgba(59, 111, 224, 0.06);
  --el-table-striped-bg-color: rgba(246, 247, 251, 0.6);
  --el-table-text-color: var(--ink);
  font-size: 13.5px;
  border-radius: var(--radius-md);
}

:deep(.el-table th.el-table__cell) {
  background: var(--surface);
  color: #55617e;
  font-size: 12.5px;
  font-weight: 600;
}

:deep(.el-table .el-table__cell) {
  padding: 10px 0;
}

:deep(.el-table__inner-wrapper::before) {
  display: none;
}

@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
