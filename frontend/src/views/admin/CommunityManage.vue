<template>
  <div>
    <el-radio-group v-model="statusFilter" class="status-filter" @change="loadRows(1)">
      <el-radio-button :value="-1">全部</el-radio-button>
      <el-radio-button :value="0">待审核</el-radio-button>
      <el-radio-button :value="1">已通过</el-radio-button>
      <el-radio-button :value="2">已驳回</el-radio-button>
    </el-radio-group>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="author" label="作者" width="100" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="topic" label="话题" width="90" />
        <el-table-column prop="likes" label="点赞" width="70" align="center" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusMeta[row.status]?.type ?? 'info'" size="small">
              {{ statusMeta[row.status]?.label ?? '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button v-if="row.status !== 1" link type="success" size="small" @click="review(row, 1)">
              通过
            </el-button>
            <el-button v-if="row.status !== 2" link type="danger" size="small" @click="review(row, 2)">
              驳回
            </el-button>
            <el-button link type="warning" size="small" @click="toggleTop(row)">
              {{ row.isTop ? '取消置顶' : '置顶' }}
            </el-button>
            <el-button link type="info" size="small" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !rows.length" description="暂无帖子数据" />
      <el-pagination
        class="pager"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @current-change="loadRows"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminDeletePost, adminListPosts, adminReviewPost } from '@/api/modules/admin'
import type { PostDto } from '@/types/api'

const statusMeta: Record<number, { label: string; type: 'success' | 'warning' | 'danger' }> = {
  0: { label: '待审核', type: 'warning' },
  1: { label: '已通过', type: 'success' },
  2: { label: '已驳回', type: 'danger' },
}

const rows = ref<PostDto[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)
const statusFilter = ref(-1)

async function loadRows(target = 1) {
  page.value = target
  loading.value = true
  try {
    const res = await adminListPosts({
      status: statusFilter.value === -1 ? undefined : statusFilter.value,
      page: page.value,
      pageSize,
    })
    rows.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function review(row: PostDto, status: number) {
  try {
    await adminReviewPost(row.id, { status })
    ElMessage.success(status === 1 ? '已通过' : '已驳回')
    loadRows(page.value)
  } catch {
    /* 错误提示已由请求层统一弹出 */
  }
}

async function toggleTop(row: PostDto) {
  try {
    await adminReviewPost(row.id, { isTop: !row.isTop })
    ElMessage.success(row.isTop ? '已取消置顶' : '已置顶')
    loadRows(page.value)
  } catch {
    /* 错误提示已由请求层统一弹出 */
  }
}

function onDelete(row: PostDto) {
  ElMessageBox.confirm(`确定删除帖子「${row.title}」吗？`, '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      try {
        await adminDeletePost(row.id)
        ElMessage.success('删除成功')
        loadRows(page.value)
      } catch {
        /* 错误提示已由请求层统一弹出 */
      }
    })
    .catch(() => {})
}

onMounted(() => loadRows(1))
</script>

<style scoped lang="scss">
.status-filter {
  margin-bottom: 16px;
}

.table-card {
  .pager {
    margin-top: 16px;
    justify-content: flex-end;
  }
}
</style>
