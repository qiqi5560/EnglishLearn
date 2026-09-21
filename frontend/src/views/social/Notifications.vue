<template>
  <div class="notifications page">
    <AppHeader title="互动通知" back>
      <template #right>
        <el-button link type="primary" :disabled="!unreadCount" @click="onReadAll">全部已读</el-button>
      </template>
    </AppHeader>

    <div class="page-shell notice-body">
      <div class="glass-card notice-card">
        <el-radio-group v-model="activeType" class="type-tabs" @change="load">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="like">点赞</el-radio-button>
          <el-radio-button value="comment">评论</el-radio-button>
          <el-radio-button value="reply">回复</el-radio-button>
        </el-radio-group>

        <div v-loading="loading" class="notice-list stagger">
          <div
            v-for="n in list"
            :key="n.id"
            class="notice-item"
            :class="{ 'is-unread': !n.read }"
            @click="onOpen(n)"
          >
            <span v-if="!n.read" class="unread-dot" />
            <el-avatar :size="40" :style="{ background: avatarColor(n.actorName) }">
              {{ n.actorName[0]?.toUpperCase() }}
            </el-avatar>
            <div class="notice-text">
              <div class="notice-line">
                <strong>{{ n.actorName }}</strong>
                <span class="type-tag" :class="`tag-${n.type}`">{{ typeLabel(n.type) }}</span>
              </div>
              <p class="notice-content">{{ n.content }}</p>
            </div>
            <span class="notice-time text-muted">{{ formatDateTime(n.createTime) }}</span>
          </div>

          <el-empty v-if="!loading && !list.length" :image-size="70" description="还没有互动通知">
            <el-button type="primary" round @click="$router.push('/community')">去社区互动</el-button>
          </el-empty>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
import * as socialApi from '@/api/modules/social'
import { useSocialStore } from '@/stores/social'
import type { NotificationDto } from '@/types/api'

const router = useRouter()
const socialStore = useSocialStore()

const list = ref<NotificationDto[]>([])
const unreadCount = ref(0)
const activeType = ref('all')
const loading = ref(false)

const AVATAR_COLORS = ['#3b6fe0', '#22a06b', '#ff6a4d', '#e8a23d', '#7a5af8', '#2bb3c0']

function avatarColor(name: string) {
  let sum = 0
  for (const ch of name) sum += ch.charCodeAt(0)
  return AVATAR_COLORS[sum % AVATAR_COLORS.length]
}

function typeLabel(type: string) {
  return { like: '赞', comment: '评论', reply: '回复' }[type] ?? '互动'
}

function formatDateTime(t?: string) {
  if (!t) return ''
  return t.length >= 16 ? t.slice(5, 16) : t
}

async function load() {
  loading.value = true
  try {
    const res = await socialApi.listNotifications(activeType.value === 'all' ? undefined : activeType.value)
    list.value = res.list
    unreadCount.value = res.unread
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    loading.value = false
  }
}

async function onOpen(item: NotificationDto) {
  if (!item.read) {
    try {
      await socialApi.readNotifications([item.id])
      item.read = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
      socialStore.markNotificationRead(1)
    } catch {
      /* 忽略已读失败 */
    }
  }
  if (item.targetType === 'post' && item.targetId) {
    router.push(`/community/post/${item.targetId}`)
  }
}

const hasUnread = computed(() => unreadCount.value > 0)

async function onReadAll() {
  if (!hasUnread.value) return
  try {
    await socialApi.readNotifications()
    list.value = list.value.map((n) => ({ ...n, read: true }))
    socialStore.markNotificationRead()
    unreadCount.value = 0
    ElMessage.success('已全部标记为已读')
  } catch {
    /* 错误提示已由请求层统一弹出 */
  }
}

onMounted(() => {
  void load()
  void socialStore.refreshUnread()
})
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);

.notice-body {
  padding-bottom: 30px;
}

.notice-card {
  padding: 22px 26px 26px;
}

.type-tabs {
  margin-bottom: 16px;

  :deep(.el-radio-button__inner) {
    border-radius: 999px;
    padding: 9px 18px;
    font-weight: 600;
  }

  :deep(.el-radio-button:first-child .el-radio-button__inner) {
    border-radius: 999px;
  }

  :deep(.el-radio-button:last-child .el-radio-button__inner) {
    border-radius: 999px;
  }
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.notice-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  border: 1px solid transparent;
  cursor: pointer;
  transition:
    background 0.3s ease,
    transform 0.35s $ease-apple,
    border-color 0.3s ease;

  &:hover {
    background: rgba(59, 111, 224, 0.06);
    transform: translateX(4px);
    border-color: rgba(59, 111, 224, 0.14);
  }

  &.is-unread {
    background: rgba(59, 111, 224, 0.09);
  }

  .unread-dot {
    position: absolute;
    left: 6px;
    top: 50%;
    margin-top: -3px;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--primary);
    box-shadow: 0 0 0 3px rgba(59, 111, 224, 0.18);
  }

  .notice-text {
    flex: 1;
    min-width: 0;
  }

  .notice-line {
    display: flex;
    align-items: center;
    gap: 8px;

    strong {
      font-size: 14px;
      font-weight: 700;
    }
  }

  .type-tag {
    padding: 1px 8px;
    border-radius: 999px;
    font-size: 11px;
    font-weight: 700;
    color: var(--primary);
    background: rgba(59, 111, 224, 0.12);

    &.tag-like {
      color: #d93a4c;
      background: rgba(232, 97, 111, 0.14);
    }

    &.tag-reply {
      color: #16845b;
      background: rgba(34, 197, 94, 0.14);
    }
  }

  .notice-content {
    margin: 4px 0 0;
    font-size: 13.5px;
    line-height: 1.65;
    color: var(--ink);
  }

  .notice-time {
    flex-shrink: 0;
    font-size: 12px;
  }
}

@media (max-width: 900px) {
  .notice-card {
    padding: 18px;
  }

  .notice-time {
    display: none;
  }
}
</style>
