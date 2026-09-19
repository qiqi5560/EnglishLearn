<template>
  <div v-loading="loading" class="page page-shell page-shell--wide">
    <header class="page-head">
      <h1 class="page-title">数据看板</h1>
      <p class="page-desc">平台核心运营指标与待处理事项一览</p>
    </header>

    <div class="stat-grid stagger">
      <el-card v-for="s in stats" :key="s.label" shadow="never" class="stat-card hover-lift sheen">
        <div class="stat-value" :style="{ color: s.color }">{{ s.value }}</div>
        <div class="stat-label text-muted">{{ s.label }}</div>
      </el-card>
    </div>

    <div class="panel-grid">
      <el-card shadow="never" class="panel-card trend-card">
        <div class="card-title">
          <span>近 7 日活跃用户</span>
          <em class="text-muted">趋势</em>
        </div>
        <GrowthLine :dates="dates" :scores="values" />
      </el-card>

      <el-card shadow="never" class="panel-card table-card">
        <div class="card-title">
          <span>待处理事项</span>
          <em class="text-muted">共 {{ todos.length }} 项</em>
        </div>
        <el-table :data="todos" stripe>
          <el-table-column prop="type" label="类型" width="120" />
          <el-table-column prop="desc" label="内容" />
          <el-table-column prop="count" label="数量" width="80" align="center" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="goTodo(row)">去处理</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && !todos.length" description="暂无待处理事项" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import GrowthLine from '@/components/business/chart/GrowthLine.vue'
import { dashboard } from '@/api/modules/admin'
import type { AdminDashboard } from '@/types/api'

const router = useRouter()
const loading = ref(false)
const data = ref<AdminDashboard | null>(null)

const stats = computed(() => data.value?.stats ?? [])
const dates = computed(() => data.value?.trend.dates ?? [])
const values = computed(() => data.value?.trend.values ?? [])
const todos = computed(() => data.value?.todos ?? [])

function goTodo(row: { type: string }) {
  if (row.type.includes('资源')) {
    router.push('/admin/resources')
  } else if (row.type.includes('社区') || row.type.includes('帖子')) {
    router.push('/admin/community')
  }
}

onMounted(async () => {
  loading.value = true
  try {
    data.value = await dashboard()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);

// ---------------- 页头 ----------------
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

// ---------------- 指标卡 ----------------
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

.stat-card {
  border-radius: var(--radius-lg);
  border: 1px solid rgba(31, 42, 68, 0.07);
  background: var(--card);
  box-shadow: var(--shadow-sm);

  :deep(.el-card__body) {
    padding: 24px 24px 22px;
  }

  .stat-value {
    font-size: 32px;
    font-weight: 800;
    line-height: 1.1;
    letter-spacing: -0.03em;
  }

  .stat-label {
    margin-top: 8px;
    font-size: 13px;
    font-weight: 600;
  }
}

// ---------------- 两栏面板 ----------------
.panel-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
  gap: 20px;
  margin-top: 20px;
  align-items: start;
}

.panel-card {
  border-radius: var(--radius-lg);
  border: 1px solid rgba(31, 42, 68, 0.07);
  background: var(--card);
  box-shadow: var(--shadow-sm);

  :deep(.el-card__body) {
    padding: 24px;
  }
}

.card-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 16px;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.02em;

  em {
    font-size: 12px;
    font-style: normal;
    font-weight: 500;
  }
}

// ---------------- 表格 ----------------
:deep(.el-table) {
  --el-table-border-color: var(--border);
  --el-table-header-bg-color: var(--surface);
  --el-table-row-hover-bg-color: rgba(59, 111, 224, 0.06);
  --el-table-striped-bg-color: rgba(246, 247, 251, 0.6);
  --el-table-text-color: var(--ink);
  font-size: 14px;
  border-radius: var(--radius-md);
}

:deep(.el-table th.el-table__cell) {
  background: var(--surface);
  color: #55617e;
  font-size: 13px;
  font-weight: 600;
}

:deep(.el-table .el-table__cell) {
  padding: 11px 0;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background: rgba(246, 247, 251, 0.6);
}

:deep(.el-table__body tr.el-table__row td.el-table__cell) {
  transition: background 0.28s $ease-apple;
}

@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .panel-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>