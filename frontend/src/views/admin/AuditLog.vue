<template>
  <div class="page page-shell page-shell--wide">
    <header class="page-head">
      <h1 class="page-title">操作日志</h1>
      <p class="page-desc">追溯管理员对内容、用户与配置的每一次写操作，便于责任到人</p>
    </header>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-select v-model="moduleFilter" class="toolbar-select" @change="load">
          <el-option label="全部模块" value="all" />
          <el-option v-for="m in modules" :key="m.name" :label="`${m.name}（${m.count}）`" :value="m.name" />
        </el-select>
        <el-select v-model="limit" class="toolbar-select" @change="load">
          <el-option :value="50" label="最近 50 条" />
          <el-option :value="100" label="最近 100 条" />
          <el-option :value="200" label="最近 200 条" />
        </el-select>
      </div>
      <div class="toolbar-right">
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="stat-grid stagger">
      <el-card v-for="s in stats" :key="s.label" shadow="never" class="stat-card hover-lift sheen">
        <div class="stat-value" :style="{ color: s.color }">{{ s.value }}</div>
        <div class="stat-label text-muted">{{ s.label }}</div>
      </el-card>
    </div>

    <el-card shadow="never" class="table-card">
      <div class="card-title">
        <span>操作记录</span>
        <em class="text-muted">共 {{ rows.length }} 条</em>
      </div>
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="time" label="操作时间" width="170" />
        <el-table-column label="操作人" width="130">
          <template #default="{ row }">
            <span class="operator">{{ row.operator }}</span>
            <em v-if="row.operatorId" class="text-muted">#{{ row.operatorId }}</em>
          </template>
        </el-table-column>
        <el-table-column label="模块" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="moduleTagType(row.module)">{{ row.module }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作" width="130" />
        <el-table-column prop="detail" label="详情" min-width="240" show-overflow-tooltip />
      </el-table>
      <el-empty v-if="!loading && !rows.length" description="暂无操作记录（管理员写操作后自动记录）" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminAuditLogs } from '@/api/modules/admin'
import type { AuditLogEntry, AuditLogModuleStat } from '@/types/api'

const loading = ref(false)
const limit = ref(100)
const moduleFilter = ref('all')
const total = ref(0)
const modules = ref<AuditLogModuleStat[]>([])
const rows = ref<AuditLogEntry[]>([])

const stats = computed(() => {
  const top = [...modules.value].sort((a, b) => b.count - a.count)[0]
  return [
    { label: '日志总量', value: String(total.value), color: 'var(--primary)' },
    { label: '覆盖模块', value: String(modules.value.length), color: 'var(--accent)' },
    { label: '当前筛选', value: moduleFilter.value === 'all' ? '全部' : moduleFilter.value, color: 'var(--success)' },
    { label: '最活跃模块', value: top ? `${top.name} · ${top.count}` : '--', color: 'var(--warning)' },
  ]
})

const TAG_TYPES = ['primary', 'success', 'warning', 'danger', 'info'] as const
const MODULE_TAG: Record<string, (typeof TAG_TYPES)[number]> = {
  用户管理: 'danger',
  社区管理: 'warning',
  场景管理: 'primary',
  名句素材: 'success',
  资源管理: 'info',
}

function moduleTagType(module: string) {
  return MODULE_TAG[module] ?? 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await adminAuditLogs({
      module: moduleFilter.value === 'all' ? undefined : moduleFilter.value,
      limit: limit.value,
    })
    total.value = res.total
    modules.value = res.modules
    rows.value = res.list
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

.toolbar-select {
  width: 180px;
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

.operator {
  font-weight: 600;

  & + em {
    margin-left: 6px;
    font-size: 11.5px;
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

  .toolbar-select {
    width: 100%;
  }

  .toolbar-right {
    display: flex;
    justify-content: flex-end;
  }
}
</style>
