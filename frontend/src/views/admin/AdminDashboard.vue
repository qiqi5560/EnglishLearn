<template>
  <div v-loading="loading">
    <div class="stat-grid">
      <el-card v-for="s in stats" :key="s.label" shadow="never" class="stat-card">
        <div class="stat-value" :style="{ color: s.color }">{{ s.value }}</div>
        <div class="stat-label text-muted">{{ s.label }}</div>
      </el-card>
    </div>

    <el-card shadow="never" class="trend-card">
      <div class="card-title">近 7 日活跃用户</div>
      <GrowthLine :dates="dates" :scores="values" />
    </el-card>

    <el-card shadow="never" class="table-card">
      <div class="card-title">待处理事项</div>
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
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  .stat-value {
    font-size: 28px;
    font-weight: 800;
  }
}

.card-title {
  font-weight: 700;
  margin-bottom: 12px;
}

.trend-card,
.table-card {
  margin-top: 16px;
}
</style>
