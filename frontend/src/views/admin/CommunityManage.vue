<template>
  <div class="page page-shell page-shell--wide">
    <header class="page-head">
      <h1 class="page-title">社区管理</h1>
      <p class="page-desc">审核帖子内容、管理置顶与清除违规信息</p>
    </header>

    <div class="toolbar">
      <el-radio-group v-model="statusFilter" class="status-filter" @change="loadRows(1)">
        <el-radio-button :value="-1">全部</el-radio-button>
        <el-radio-button :value="0">待审核</el-radio-button>
        <el-radio-button :value="1">已通过</el-radio-button>
        <el-radio-button :value="2">已驳回</el-radio-button>
      </el-radio-group>
    </div>

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
        <el-table-column label="操作" width="320">
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
            <el-button link type="primary" size="small" @click="openComments(row)">评论</el-button>
            <el-button link type="danger" size="small" @click="onPunish(row)">处罚作者</el-button>
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

    <!-- 发帖处罚对话框 -->
    <el-dialog v-model="punishVisible" title="发起发帖处罚" width="440px" :close-on-click-modal="false">
      <p class="punish-tip text-muted">
        处罚期间，该用户无法发帖与评论，登录与学习功能不受影响。
      </p>
      <el-form label-position="top">
        <el-form-item label="处罚天数">
          <div class="punish-days">
            <el-radio-group v-model="punishDays" size="small">
              <el-radio-button :value="1">1 天</el-radio-button>
              <el-radio-button :value="3">3 天</el-radio-button>
              <el-radio-button :value="7">7 天</el-radio-button>
              <el-radio-button :value="30">30 天</el-radio-button>
            </el-radio-group>
            <el-input-number v-model="punishDays" :min="1" :max="365" size="small" class="punish-num" />
          </div>
        </el-form-item>
        <el-form-item label="处罚原因">
          <el-input v-model="punishReason" placeholder="如：恶意发帖 / 广告刷屏" maxlength="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="punishVisible = false">取消</el-button>
        <el-button type="danger" @click="submitPunish">确认处罚</el-button>
      </template>
    </el-dialog>

    <!-- 评论治理抽屉：逐条删除违规评论 -->
    <el-drawer v-model="commentDrawer" :title="`评论管理 · ${activePost?.title ?? ''}`" size="520px">
      <div v-loading="commentLoading" class="comment-list">
        <article v-for="c in comments" :key="c.id" class="comment-item">
          <div class="comment-head">
            <strong>{{ c.author }}</strong>
            <span class="comment-time text-muted">{{ c.createTime }}</span>
          </div>
          <p class="comment-body">{{ c.content }}</p>
          <el-button link type="danger" size="small" @click="onDeleteComment(c.id)">删除该评论</el-button>
        </article>
        <el-empty v-if="!commentLoading && !comments.length" description="该帖子暂无评论" />
      </div>
      <template #footer>
        <el-button v-if="activePost" type="danger" plain @click="onPunish(activePost)">
          处罚作者「{{ activePost.author }}」
        </el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminBanUser,
  adminDeleteComment,
  adminDeletePost,
  adminListPosts,
  adminReviewPost,
} from '@/api/modules/admin'
import { postDetail } from '@/api/modules/community'
import type { CommentDto, PostDto } from '@/types/api'

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

// 评论治理
const commentDrawer = ref(false)
const commentLoading = ref(false)
const activePost = ref<PostDto | null>(null)
const comments = ref<CommentDto[]>([])

// 发帖处罚
const punishVisible = ref(false)
const punishTarget = ref<PostDto | null>(null)
const punishDays = ref(3)
const punishReason = ref('恶意发帖')

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

/** 打开评论抽屉，加载该帖子的全部评论 */
async function openComments(row: PostDto) {
  activePost.value = row
  commentDrawer.value = true
  commentLoading.value = true
  try {
    const res = await postDetail(row.id)
    comments.value = res.comments
  } catch {
    comments.value = []
  } finally {
    commentLoading.value = false
  }
}

function onDeleteComment(commentId: number) {
  ElMessageBox.confirm('确定删除这条评论吗？', '删除评论', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      try {
        await adminDeleteComment(commentId)
        ElMessage.success('评论已删除')
        if (activePost.value) openComments(activePost.value)
      } catch {
        /* 错误提示已由请求层统一弹出 */
      }
    })
    .catch(() => {})
}

/**
 * 对帖子作者发起发帖处罚：选择天数 + 填写原因，处罚期内该用户无法发帖与评论。
 */
function onPunish(row: PostDto) {
  if (!row.authorId) {
    ElMessage.warning('该帖子作者信息缺失，无法处罚')
    return
  }
  punishTarget.value = row
  punishDays.value = 3
  punishReason.value = '恶意发帖'
  punishVisible.value = true
}

async function submitPunish() {
  const target = punishTarget.value
  if (!target?.authorId) return
  try {
    await adminBanUser(target.authorId, { days: punishDays.value, reason: punishReason.value })
    ElMessage.success(`已处罚「${target.author}」${punishDays.value} 天`)
    punishVisible.value = false
    loadRows(page.value)
  } catch {
    /* 错误提示已由请求层统一弹出 */
  }
}

onMounted(() => loadRows(1))
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

// ---------------- 工具条：状态筛选 ----------------
.toolbar {
  display: flex;
  align-items: center;
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

.status-filter {
  :deep(.el-radio-button__inner) {
    padding: 9px 20px;
    font-size: 13px;
    font-weight: 600;
    color: #55617e;
    background: transparent;
    border: none;
    box-shadow: none;
    transition:
      color 0.28s ease,
      background 0.35s $ease-apple,
      box-shadow 0.35s $ease-apple;
  }

  :deep(.el-radio-button__inner:hover) {
    color: var(--primary);
    background: rgba(59, 111, 224, 0.08);
  }

  :deep(.el-radio-button:first-child .el-radio-button__inner) {
    border-radius: 999px 0 0 999px;
  }

  :deep(.el-radio-button:last-child .el-radio-button__inner) {
    border-radius: 0 999px 999px 0;
  }

  :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
    color: #fff;
    background: linear-gradient(135deg, #5b8bf0, #3b6fe0 60%, #2f5bb3);
    border: none;
    box-shadow: 0 8px 18px rgba(59, 111, 224, 0.28);
  }
}

// ---------------- 表格卡片 ----------------
.table-card {
  border-radius: var(--radius-lg);
  border: 1px solid rgba(31, 42, 68, 0.07);
  background: var(--card);
  box-shadow: var(--shadow-sm);

  :deep(.el-card__body) {
    padding: 16px 20px 18px;
  }
}

:deep(.el-table) {
  --el-table-border-color: var(--border);
  --el-table-header-bg-color: var(--surface);
  --el-table-row-hover-bg-color: rgba(59, 111, 224, 0.06);
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
  padding: 12px 0;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background: rgba(246, 247, 251, 0.6);
}

:deep(.el-table__body tr.el-table__row td.el-table__cell) {
  transition: background 0.28s $ease-apple;
}

:deep(.el-table__inner-wrapper::before) {
  display: none;
}

.pager {
  margin-top: 18px;
  justify-content: flex-end;
}

// ---------------- 评论治理抽屉 ----------------
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-item {
  padding: 12px 14px;
  border-radius: var(--radius-md);
  border: 1px solid rgba(31, 42, 68, 0.07);
  background: rgba(246, 247, 251, 0.6);
  transition: box-shadow 0.35s $ease-apple;

  &:hover {
    box-shadow: var(--shadow-sm);
  }
}

.comment-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;

  strong {
    font-size: 13.5px;
  }
}

.comment-time {
  font-size: 12px;
}

.comment-body {
  margin: 7px 0 6px;
  font-size: 13.5px;
  line-height: 1.6;
  color: #3c4763;
  word-break: break-word;
}

// ---------------- 发帖处罚 ----------------
.punish-tip {
  margin: 0 0 14px;
  font-size: 13px;
  line-height: 1.6;
}

.punish-days {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.punish-num {
  width: 110px;
}
</style>