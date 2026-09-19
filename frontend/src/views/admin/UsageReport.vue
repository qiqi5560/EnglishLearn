<template>
  <div class="page page-shell page-shell--wide">
    <header class="page-head">
      <h1 class="page-title">用户使用报表</h1>
      <p class="page-desc">按用户维度查看注册明细、练习量与得分表现，时间精确到分钟</p>
    </header>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="keyword"
          class="toolbar-search"
          placeholder="搜索手机号 / 昵称"
          clearable
          @keyup.enter="load"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="days" class="toolbar-select" @change="load">
          <el-option :value="7" label="近 7 天" />
          <el-option :value="30" label="近 30 天" />
          <el-option :value="90" label="近 90 天" />
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

    <el-card shadow="never" class="chart-card">
      <div class="card-title">
        <span>近 12 个月注册趋势</span>
        <em class="text-muted">峰值 {{ data?.monthly.peak ?? 0 }} 人 / 月</em>
      </div>
      <TrendChart :labels="data?.monthly.months ?? []" :series="monthlySeries" :height="240" />
    </el-card>

    <el-card shadow="never" class="table-card">
      <div class="card-title">
        <span>用户明细</span>
        <em class="text-muted">共 {{ rows.length }} 位用户</em>
      </div>
      <el-table :data="rows" stripe>
        <el-table-column prop="userId" label="ID" width="66" />
        <el-table-column prop="nickname" label="昵称" min-width="110" />
        <el-table-column prop="phone" label="手机号" width="128" />
        <el-table-column label="等级" width="72" align="center">
          <template #default="{ row }">{{ row.level || '--' }}</template>
        </el-table-column>
        <el-table-column label="注册时间" width="152">
          <template #default="{ row }">{{ row.registerTime || '--' }}</template>
        </el-table-column>
        <el-table-column prop="sessionCount" label="对话场次" width="92" align="center" sortable />
        <el-table-column label="练习时长" width="104" align="center" sortable :sort-by="'durationMin'">
          <template #default="{ row }">{{ formatDuration(row.durationMin) }}</template>
        </el-table-column>
        <el-table-column prop="studyCount" label="学习记录" width="92" align="center" sortable />
        <el-table-column label="平均得分" width="96" align="center">
          <template #default="{ row }">
            <span v-if="row.avgScore === null || row.avgScore === undefined" class="text-muted">--</span>
            <span v-else :class="scoreClass(row.avgScore)">{{ row.avgScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="160">
          <template #default="{ row }">{{ row.lastLoginTime || '--' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="88">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'" size="small" effect="plain">
              {{ row.active ? '活跃' : '未使用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发帖权限" width="110" fixed="right">
          <template #default="{ row }">
            <el-tooltip
              v-if="row.punished"
              :content="`原因：${row.banReason || '违反社区规范'}，解禁时间：${row.banUntil || '--'}`"
              placement="top"
            >
              <el-tag type="danger" size="small" effect="dark">禁言中</el-tag>
            </el-tooltip>
            <el-tag v-else type="info" size="small" effect="plain">正常</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !rows.length" description="暂无用户使用数据" />
    </el-card>

  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminUsageReport } from '@/api/modules/admin'
import type { AdminUsageReport } from '@/types/api'
import TrendChart from '@/components/business/chart/TrendChart.vue'

const loading = ref(false)
const days = ref(30)
const keyword = ref('')
const data = ref<AdminUsageReport | null>(null)

const rows = computed(() => data.value?.list ?? [])

/** 注册月份分布柱状图数据 */
const monthlySeries = computed(() => [
  {
    name: '注册人数',
    values: data.value?.monthly?.values ?? [],
    color: '#5b8bf0',
    type: 'bar' as const,
  },
])

const stats = computed(() => {
  const s = data.value?.summary
  if (!s) return []
  return [
    { label: '累计注册用户', value: String(s.totalUsers), color: 'var(--primary)' },
    { label: `活跃用户（近 ${s.days} 天）`, value: `${s.activeUsers} · ${s.activeRate}%`, color: 'var(--success)' },
    { label: '新增注册', value: String(s.newUsers), color: 'var(--accent)' },
    { label: '发帖处罚中', value: String(s.punishedUsers), color: 'var(--danger)' },
  ]
})

function formatDuration(minutes: number) {
  if (!minutes) return '0 分钟'
  if (minutes < 60) return `${minutes} 分钟`
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return m ? `${h} 小时 ${m} 分` : `${h} 小时`
}

function scoreClass(score: number) {
  if (score >= 85) return 'score-good'
  if (score >= 70) return 'score-mid'
  return 'score-low'
}

async function load() {
  loading.value = true
  try {
    data.value = await adminUsageReport({
      days: days.value,
      keyword: keyword.value.trim() || undefined,
    })
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
  min-width: 0;
}

.toolbar-right {
  flex-shrink: 0;
}

.toolbar-search {
  width: 240px;
}

.toolbar-select {
  width: 130px;
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
    font-size: 24px;
    font-weight: 800;
    line-height: 1.15;
    letter-spacing: -0.03em;
  }

  .stat-label {
    margin-top: 7px;
    font-size: 12.5px;
    font-weight: 600;
  }
}

.table-card {
  border-radius: var(--radius-lg);
  border: 1px solid rgba(31, 42, 68, 0.07);
  background: var(--card);
  box-shadow: var(--shadow-sm);

  :deep(.el-card__body) {
    padding: 18px 20px 20px;
  }
}

// ---------------- 注册趋势图 ----------------
.chart-card {
  margin-bottom: 18px;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(31, 42, 68, 0.07);
  background: var(--card);
  box-shadow: var(--shadow-sm);

  :deep(.el-card__body) {
    padding: 18px 20px 20px;
  }
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

.score-good {
  color: var(--success);
  font-weight: 700;
}

.score-mid {
  color: var(--warning);
  font-weight: 700;
}

.score-low {
  color: var(--danger);
  font-weight: 700;
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

:deep(.el-table__body tr.el-table__row td.el-table__cell) {
  transition: background 0.28s $ease-apple;
}

@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-search,
  .toolbar-select {
    width: 100%;
  }
}
</style>
