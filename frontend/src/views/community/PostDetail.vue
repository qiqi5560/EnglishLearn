<template>
  <div class="post-detail">
    <AppHeader title="帖子详情" back />

    <div v-loading="loading" class="detail-body">
      <template v-if="post">
        <el-card shadow="never">
          <div class="post-head">
            <el-avatar :size="40" :style="{ background: avatarColor(post.author) }">
              {{ post.author[0]?.toUpperCase() }}
            </el-avatar>
            <div class="post-head-info">
              <div class="author-name">{{ post.author }}</div>
              <div class="text-muted post-time">{{ formatDateTime(post.createTime) }}</div>
            </div>
          </div>
          <h3 class="post-title">{{ post.title }}</h3>
          <el-tag size="small" effect="plain">{{ post.topic }}</el-tag>
          <p class="post-content">{{ post.content }}</p>
          <div class="post-actions">
            <el-button
              type="primary"
              plain
              round
              :icon="post.liked ? 'StarFilled' : 'Star'"
              @click="onLike"
            >
              {{ post.likes }} 点赞
            </el-button>
          </div>
        </el-card>

        <!-- 评论 -->
        <div class="section-title">评论 {{ currentComments.length }}</div>
        <el-card shadow="never">
          <el-empty
            v-if="!currentComments.length"
            :image-size="48"
            description="暂无评论，快来抢沙发"
          />
          <div v-for="c in currentComments" :key="c.id" class="comment-item">
            <el-avatar :size="32" :style="{ background: avatarColor(c.author) }">
              {{ c.author[0]?.toUpperCase() }}
            </el-avatar>
            <div class="comment-body">
              <div class="comment-author">{{ c.author }}</div>
              <div class="comment-content">{{ c.content }}</div>
            </div>
            <span class="comment-time text-muted">{{ formatDateTime(c.createTime) }}</span>
          </div>
        </el-card>

        <!-- 评论输入 -->
        <div class="comment-input">
          <el-input v-model="draft" type="textarea" :rows="2" resize="none" placeholder="友善评论…" />
          <el-button type="primary" :loading="commenting" @click="onComment">发送</el-button>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="帖子不存在或已被删除">
        <el-button type="primary" @click="$router.push('/community')">返回社区</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
import { useCommunityStore } from '@/stores/community'

const route = useRoute()
const communityStore = useCommunityStore()

const loading = ref(false)
const commenting = ref(false)
const draft = ref('')

const post = computed(() => communityStore.currentPost)
const currentComments = computed(() => communityStore.currentComments)

const AVATAR_COLORS = ['#3b6fe0', '#22a06b', '#ff6a4d', '#e8a23d', '#7a5af8', '#2bb3c0']

function avatarColor(name: string) {
  let sum = 0
  for (const ch of name) sum += ch.charCodeAt(0)
  return AVATAR_COLORS[sum % AVATAR_COLORS.length]
}

function formatDateTime(t?: string) {
  if (!t) return ''
  return t.length >= 16 ? t.slice(0, 16) : t
}

async function onLike() {
  if (!post.value) return
  try {
    await communityStore.toggleLike(post.value)
  } catch {
    /* 错误提示已由请求层统一弹出 */
  }
}

async function onComment() {
  const text = draft.value.trim()
  if (!post.value) return
  if (!text) {
    ElMessage.warning('请输入评论内容')
    return
  }
  commenting.value = true
  try {
    await communityStore.comment(post.value.id, text)
    draft.value = ''
    ElMessage.success('评论成功')
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    commenting.value = false
  }
}

onMounted(async () => {
  const pid = Number(route.params.id)
  if (!Number.isInteger(pid)) return
  loading.value = true
  try {
    await communityStore.loadDetail(pid)
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.detail-body {
  padding: 16px;
  min-height: 60vh;
}

.post-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;

  .post-head-info {
    .author-name {
      font-weight: 600;
    }

    .post-time {
      font-size: 12px;
    }
  }
}

.post-title {
  margin: 8px 0;
}

.post-content {
  line-height: 1.8;
  color: var(--el-text-color-regular);
}

.post-actions {
  margin-top: 14px;
}

.comment-item {
  display: flex;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px dashed var(--border);

  .comment-body {
    flex: 1;
    min-width: 0;

    .comment-author {
      font-weight: 600;
      font-size: 13px;
    }

    .comment-content {
      font-size: 14px;
      line-height: 1.6;
      margin-top: 2px;
    }
  }

  .comment-time {
    font-size: 12px;
    white-space: nowrap;
  }
}

.comment-input {
  display: flex;
  gap: 8px;
  margin-top: 16px;

  :deep(.el-textarea) {
    flex: 1;
  }
}
</style>
