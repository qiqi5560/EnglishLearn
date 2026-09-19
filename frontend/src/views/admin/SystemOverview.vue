<template>
  <div class="page page-shell page-shell--wide">
    <header class="page-head">
      <h1 class="page-title">系统概览</h1>
      <p class="page-desc">服务运行状态、数据规模与管理员操作审计</p>
    </header>

    <div class="toolbar">
      <div class="toolbar-left">
        <span class="status-pill" :class="data?.status === 'healthy' ? 'is-ok' : 'is-down'">
          <i class="dot" />
          {{ data?.status === 'healthy' ? '服务运行正常' : '状态未知' }}
        </span>
        <span class="text-muted checked">启动于 {{ data?.runtime.startedAt || '--' }}</span>
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

    <div class="panel-grid">
      <el-card shadow="never" class="panel-card">
        <div class="card-title">
          <span>数据规模</span>
          <em class="text-muted">共 {{ totalRows }} 条记录</em>
        </div>
        <ul class="data-list">
          <li v-for="t in data?.database.tables ?? []" :key="t.name">
            <span class="data-name">{{ t.name }}</span>
            <span class="data-count">{{ t.count.toLocaleString() }}</span>
          </li>
        </ul>
      </el-card>

      <el-card shadow="never" class="panel-card">
        <div class="card-title">
          <span>运行环境</span>
        </div>
        <ul class="info-list">
          <li>
            <span class="info-key">Java 版本</span>
            <span class="info-val">{{ data?.runtime.javaVersion || '--' }}</span>
          </li>
          <li>
            <span class="info-key">操作系统</span>
            <span class="info-val">{{ data?.runtime.os || '--' }}</span>
          </li>
          <li>
            <span class="info-key">CPU 核心</span>
            <span class="info-val">{{ data?.runtime.processors ?? '--' }} 核</span>
          </li>
          <li>
            <span class="info-key">活跃线程</span>
            <span class="info-val">{{ data?.runtime.threads ?? '--' }}</span>
          </li>
          <li>
            <span class="info-key">数据库文件</span>
            <span class="info-val mono">{{ data?.database.file || '--' }}</span>
          </li>
        </ul>
        <div class="heap-block">
          <div class="heap-head">
            <span>堆内存占用</span>
            <em>{{ data?.runtime.heapUsedMb ?? 0 }} / {{ data?.runtime.heapMaxMb ?? 0 }} MB</em>
          </div>
          <div class="heap-track">
            <span class="heap-fill" :style="{ width: `${heapPercent}%` }" />
          </div>
        </div>
      </el-card>
    </div>

    <el-card shadow="never" class="table-card">
      <div class="card-title">
        <span>管理员操作审计</span>
        <em class="text-muted">最近 {{ data?.audit.recent.length ?? 0 }} 条 / 累计 {{ data?.audit.total ?? 0 }} 条</em>
      </div>
      <el-table :data="data?.audit.recent ?? []" stripe>
        <el-table-column prop="time" label="时间" width="170" />
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="module" label="模块" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.module }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作" width="120" />
        <el-table-column prop="detail" label="详情" min-width="200" />
      </el-table>
      <el-empty v-if="!loading && !data?.audit.recent.length" description="暂无管理员操作记录" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminSystemOverview } from '@/api/modules/admin'
import type { AdminSystemOverview } from '@/types/api'

const loading = ref(false)
const data = ref<AdminSystemOverview | null>(null)

const heapPercent = computed(() => Math.min(100, data.value?.runtime.heapUsedPercent ?? 0))

const totalRows = computed(() =>
  (data.value?.database.tables ?? []).reduce((sum, t) => sum + t.count, 0),
)

const stats = computed(() => {
  const r = data.value?.runtime
  if (!r) return []
  return [
    { label: '服务运行时长', value: formatUptime(r.uptimeSec), color: 'var(--primary)' },
    { label: '堆内存占用', value: `${r.heapUsedMb} MB`, color: 'var(--success)' },
    { label: '活跃线程数', value: String(r.threads), color: 'var(--accent)' },
    { label: '数据库大小', value: `${data.value?.database.sizeMb ?? 0} MB`, color: 'var(--warning)' },
  ]
})

function formatUptime(sec: number) {
  if (!sec) return '0 分钟'
  const d = Math.floor(sec / 86400)
  const h = Math.floor((sec % 86400) / 3600)
  const m = Math.floor((sec % 3600) / 60)
  if (d > 0) return `${d} 天 ${h} 小时`
  if (h > 0) return `${h} 小时 ${m} 分`
  return `${m} 分钟`
}

async function load() {
  loading.value = true
  try {
    data.value = await adminSystemOverview()
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
  gap: 12px;
  min-width: 0;
}

.toolbar-right {
  flex-shrink: 0;
}

.checked {
  font-size: 12.5px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 5px 13px;
  font-size: 12.5px;
  font-weight: 600;
  border-radius: 999px;

  .dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: currentColor;
  }

  &.is-ok {
    color: var(--success);
    background: rgba(34, 160, 107, 0.1);
  }

  &.is-down {
    color: var(--danger);
    background: rgba(229, 72, 77, 0.1);
  }
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
    font-size: 26px;
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

.panel-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
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

.data-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 2px 26px;

  li {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 9px 0;
    border-bottom: 1px dashed var(--border);
  }

  .data-name {
    font-size: 12.5px;
    color: var(--muted);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .data-count {
    font-size: 13px;
    font-weight: 700;
    color: var(--ink);
    flex-shrink: 0;
  }
}

.info-list {
  margin: 0;
  padding: 0;
  list-style: none;

  li {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 14px;
    padding: 10px 0;
    border-bottom: 1px dashed var(--border);
  }

  .info-key {
    font-size: 13px;
    color: var(--muted);
    flex-shrink: 0;
  }

  .info-val {
    font-size: 13px;
    font-weight: 600;
    text-align: right;
    word-break: break-all;

    &.mono {
      font-family: var(--font-reading);
      font-size: 11.5px;
      font-weight: 500;
      color: var(--muted);
    }
  }
}

.heap-block {
  margin-top: 16px;
}

.heap-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 12.5px;
  color: var(--muted);

  em {
    font-style: normal;
    font-weight: 600;
    color: var(--ink);
  }
}

.heap-track {
  height: 8px;
  border-radius: 999px;
  background: var(--surface);
  overflow: hidden;
}

.heap-fill {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #5b8bf0, #3b6fe0);
  transition: width 0.6s $ease-apple;
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

  .panel-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .data-list {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
