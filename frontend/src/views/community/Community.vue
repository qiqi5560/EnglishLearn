<template>
  <div class="community page">
    <AppHeader title="学习社区">
      <template #right>
        <el-button link type="primary" @click="$router.push('/community/post/new')">发帖</el-button>
      </template>
    </AppHeader>

    <div class="community-body">
      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane label="信息流" name="feed">
          <!-- 话题筛选 -->
          <div v-if="communityStore.topics.length" class="topic-row">
            <el-check-tag :checked="!topic" @change="(v: boolean) => v && switchTopic('')">全部</el-check-tag>
            <el-check-tag
              v-for="t in communityStore.topics"
              :key="t"
              :checked="topic === t"
              @change="(v: boolean) => onTopicToggle(t, v)"
            >
              {{ t }}
            </el-check-tag>
          </div>

          <div v-loading="communityStore.loading">
            <el-card
              v-for="post in communityStore.posts"
              :key="post.id"
              shadow="never"
              class="post-card"
              @click="$router.push(`/community/post/${post.id}`)"
            >
              <div class="post-head">
                <el-avatar :size="36" :style="{ background: avatarColor(post.author) }">
                  {{ post.author[0]?.toUpperCase() }}
                </el-avatar>
                <div class="post-author">
                  <div class="author-name">{{ post.author }}</div>
                  <el-tag size="small" effect="plain">{{ post.topic }}</el-tag>
                </div>
              </div>
              <div class="post-title">{{ post.title }}</div>
              <p class="post-content text-muted">{{ post.content }}</p>
              <div class="post-foot text-muted">
                <span><el-icon><Star /></el-icon> {{ post.likes }}</span>
                <span><el-icon><ChatDotRound /></el-icon> {{ post.comments }}</span>
                <span class="post-time">{{ formatDateTime(post.createTime) }}</span>
              </div>
            </el-card>
          </div>
          <el-empty
            v-if="!communityStore.loading && !communityStore.posts.length"
            description="还没有帖子，来发布第一篇吧"
          >
            <el-button type="primary" @click="$router.push('/community/post/new')">去发帖</el-button>
          </el-empty>
        </el-tab-pane>

        <el-tab-pane label="结伴练习" name="pair">
          <el-empty description="找一位口语搭子一起练习">
            <el-button type="primary" @click="$router.push('/community/pair')">进入结伴大厅</el-button>
          </el-empty>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppHeader from '@/components/base/AppHeader.vue'
import { useCommunityStore } from '@/stores/community'

const communityStore = useCommunityStore()

const activeTab = ref('feed')
const topic = ref('')

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

function switchTopic(t: string) {
  topic.value = t
  communityStore.loadPosts({ topic: t || undefined, page: 1, pageSize: 20 })
}

function onTopicToggle(t: string, checked: boolean) {
  if (checked) {
    switchTopic(t)
  } else if (topic.value === t) {
    switchTopic('')
  }
}

onMounted(() => {
  communityStore.loadTopics()
  communityStore.loadPosts({ page: 1, pageSize: 20 })
})
</script>

<style scoped lang="scss">
.community-body {
  padding: 0 16px;
}

.topic-row {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 4px 0 12px;
}

.post-card {
  border-radius: var(--radius-md);
  margin-bottom: 12px;
  cursor: pointer;
}

.post-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.post-author {
  .author-name {
    font-weight: 600;
    font-size: 14px;
  }
}

.post-title {
  font-size: 16px;
  font-weight: 700;
  margin: 10px 0 4px;
}

.post-content {
  font-size: 13px;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-foot {
  display: flex;
  gap: 20px;
  margin-top: 10px;
  font-size: 12px;

  .post-time {
    margin-left: auto;
  }
}
</style>
