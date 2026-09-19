<template>
  <div class="page page-shell page-shell--wide">
    <header class="page-head">
      <h1 class="page-title">算力监控</h1>
      <p class="page-desc">大模型调用量、响应耗时与 Provider 运行状态实时监控</p>
    </header>

    <div class="toolbar">
      <div class="toolbar-left">
        <span class="status-pill" :class="providerClass">
          <i class="dot" />
          {{ providerText }}
        </span>
        <span class="text-muted checked">采样于 {{ data?.checkedAt || '--' }}</span>
      </div>
      <div class="toolbar-right">
        <el-switch v-model="autoRefresh" active-text="自动刷新" inline-prompt />
        <el-button :loading="loading" @click="load">立即刷新</el-button>
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
        <span>调用量趋势</span>
        <em class="text-muted">最近 30 分钟 · 每分钟</em>
      </div>
      <TrendChart
        v-if="data"
        :labels="data.timeline.labels"
        :series="[{ name: '调用次数', values: data.timeline.values, color: '#3b6fe0', type: 'bar' }]"
        :height="230"
      />
    </el-card>

    <div class="panel-grid">
      <el-card shadow="never" class="panel-card">
        <div class="card-title">
          <span>按能力分布</span>
          <em class="text-muted">{{ data?.methods.length ?? 0 }} 项</em>
        </div>
        <el-table :data="data?.methods ?? []" stripe>
          <el-table-column prop="method" label="调用能力" min-width="130" />
          <el-table-column prop="count" label="次数" width="80" align="center" />
          <el-table-column prop="failed" label="失败" width="72" align="center">
            <template #default="{ row }">
              <span :class="{ 'is-fail': row.failed > 0 }">{{ row.failed }}</span>
            </template>
          </el-table-column>
          <el-table-column label="平均耗时" width="100" align="center">
            <template #default="{ row }">{{ row.avgLatencyMs }} ms</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!data?.methods.length" description="暂无调用记录" />
      </el-card>

      <el-card shadow="never" class="panel-card">
        <div class="card-title">
          <span>Provider 信息</span>
        </div>
        <ul class="info-list">
          <li>
            <span class="info-key">提供方</span>
            <span class="info-val">{{ data?.provider.provider || '--' }}</span>
          </li>
          <li>
            <span class="info-key">模型</span>
            <span class="info-val">{{ data?.provider.model || '--' }}</span>
          </li>
          <li>
            <span class="info-key">服务地址</span>
            <span class="info-val mono">{{ data?.provider.baseUrl || '--' }}</span>
          </li>
          <li>
            <span class="info-key">连通状态</span>
            <span class="info-val" :class="providerClass">{{ providerText }}</span>
          </li>
          <li>
            <span class="info-key">窗口内调用</span>
            <span class="info-val">{{ data?.windowCalls ?? 0 }} 次</span>
          </li>
        </ul>
        <p v-if="data?.provider.degraded" class="degrade-tip">
          <el-icon><WarningFilled /></el-icon>
          大模型服务不可达，当前已自动降级为内置规则引擎，翻译与评测仍可正常返回。
        </p>
      </el-card>
    </div>

    <el-card shadow="never" class="table-card">
      <div class="card-title">
        <span>最近调用明细</span>
        <em class="text-muted">最新 20 条</em>
      </div>
      <el-table :data="data?.recent ?? []" stripe>
        <el-table-column prop="time" label="时间" width="110" />
        <el-table-column prop="method" label="调用能力" min-width="130" />
        <el-table-column prop="provider" label="提供方" width="100" />
        <el-table-column label="耗时" width="100" align="center">
          <template #default="{ row }">{{ row.latencyMs }} ms</template>
        </el-table-column>
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.ok ? 'success' : 'danger'" size="small" effect="plain">
              {{ row.ok ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="140">
          <template #default="{ row }">{{ row.note || '--' }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !data?.recent.length" description="暂无调用明细" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import TrendChart from '@/components/business/chart/TrendChart.vue'
import { adminCompute } from '@/api/modules/admin'
import type { AdminCompute } from '@/types/api'

const loading = ref(false)
const autoRefresh = ref(true)
const data = ref<AdminCompute | null>(null)
let timer: number | null = null

const providerText = computed(() => {
  const p = data.value?.provider
  if (!p) return '读取中'
  if (p.provider !== 'ollama') return '内置规则引擎（mock）'
  return p.reachable ? 'Ollama 在线' : 'Ollama 不可达'
})

const providerClass = computed(() => {
  const p = data.value?.provider
  if (!p) return 'is-idle'
  if (p.provider !== 'ollama') return 'is-mock'
  return p.reachable ? 'is-ok' : 'is-down'
})

const stats = computed(() => {
  const d = data.value
  if (!d) return []
  return [
    { label: '累计调用次数', value: String(d.totalCalls), color: 'var(--primary)' },
    { label: '调用成功率', value: `${d.successRate}%`, color: 'var(--success)' },
    { label: '平均响应耗时', value: `${d.avgLatencyMs} ms`, color: 'var(--accent)' },
    { label: '峰值耗时 / 失败数', value: `${d.maxLatencyMs} ms · ${d.failedCalls}`, color: 'var(--warning)' },
  ]
})

async function load() {
  loading.value = true
  try {
    data.value = await adminCompute()
  } finally {
    loading.value = false
  }
}

function startTimer() {
  stopTimer()
  timer = window.setInterval(load, 10_000)
}

function stopTimer() {
  if (timer !== null) {
    window.clearInterval(timer)
    timer = null
  }
}

watch(autoRefresh, (on) => (on ? startTimer() : stopTimer()))

onMounted(() => {
  load()
  if (autoRefresh.value) startTimer()
})

onBeforeUnmount(stopTimer)
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
  display: flex;
  align-items: center;
  gap: 12px;
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
    box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.05);
  }

  &.is-ok {
    color: var(--success);
    background: rgba(34, 160, 107, 0.1);
  }

  &.is-down {
    color: var(--danger);
    background: rgba(229, 72, 77, 0.1);
  }

  &.is-mock,
  &.is-idle {
    color: var(--warning);
    background: rgba(255, 179, 71, 0.14);
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
  grid-template-columns: minmax(0, 1.3fr) minmax(0, 1fr);
  gap: 18px;
  margin-top: 18px;
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

.info-list {
  margin: 0;
  padding: 0;
  list-style: none;

  li {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 14px;
    padding: 11px 0;
    border-bottom: 1px dashed var(--border);

    &:last-child {
      border-bottom: none;
    }
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
      font-size: 12px;
      font-weight: 500;
    }

    &.is-ok {
      color: var(--success);
    }

    &.is-down {
      color: var(--danger);
    }

    &.is-mock,
    &.is-idle {
      color: var(--warning);
    }
  }
}

.degrade-tip {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  margin: 14px 0 0;
  padding: 11px 13px;
  font-size: 12.5px;
  line-height: 1.6;
  color: #8a5a00;
  background: rgba(255, 179, 71, 0.13);
  border-radius: var(--radius-md);
}

.is-fail {
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

  .panel-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
