<template>
  <div class="community page">
    <AppHeader title="学习社区">
      <template #right>
        <el-button class="header-post-btn" link type="primary" @click="$router.push('/community/post/new')">
          发帖
        </el-button>
      </template>
    </AppHeader>

    <div class="page-shell community-body">
      <header class="community-hero">
        <div>
          <h2>和同学一起开口练习</h2>
          <p class="text-muted">看看大家的练习心得，找到水平相近的口语搭子，或者分享你的今天。</p>
        </div>
        <div class="hero-stats">
          <div class="hero-stat">
            <strong>{{ communityStore.posts.length }}</strong>
            <span class="text-muted">当前帖子</span>
          </div>
          <div class="hero-stat">
            <strong>{{ communityStore.topics.length }}</strong>
            <span class="text-muted">热门话题</span>
          </div>
        </div>
      </header>

      <!-- 发帖处罚提示：处罚期内禁止发帖与评论 -->
      <el-alert
        v-if="isPunished"
        class="punish-banner"
        type="error"
        :closable="false"
        show-icon
        :title="punishMessage"
      />

      <div class="community-layout">
        <!-- 中间：信息流 -->
        <main class="feed-col">
          <el-tabs v-model="activeTab" class="feed-tabs">
            <el-tab-pane label="信息流" name="feed">
              <div class="feed-head">
                <h3>最新讨论</h3>
                <span class="text-muted">
                  {{ topic ? `#${topic}` : '全部话题' }} · 共 {{ communityStore.posts.length }} 篇
                </span>
              </div>

              <div v-loading="communityStore.loading" class="post-list stagger">
                <article
                  v-for="post in communityStore.posts"
                  :key="post.id"
                  class="glass-card hover-lift post-card"
                  @click="$router.push(`/community/post/${post.id}`)"
                >
                  <div class="post-head">
                    <el-avatar :size="42" :style="{ background: avatarColor(post.author) }">
                      {{ post.author[0]?.toUpperCase() }}
                    </el-avatar>
                    <div class="author-block">
                      <div class="author-line">
                        <span class="author-name author-link" @click.stop="$router.push(`/user/${post.authorId}`)">{{ post.author }}</span>
                        <span class="level-badge">Lv.{{ activityLevel(post.likes, post.comments) }}</span>
                      </div>
                      <div class="post-meta text-muted">
                        <span class="topic-tag">#{{ post.topic }}</span>
                        <span>{{ formatDateTime(post.createTime) }}</span>
                      </div>
                    </div>
                  </div>

                  <h4 class="post-title">{{ post.title }}</h4>
                  <p class="post-content text-muted">{{ post.content }}</p>

                  <div class="post-foot">
                    <span class="stat"><el-icon><Star /></el-icon>{{ post.likes }}</span>
                    <span class="stat"><el-icon><ChatDotRound /></el-icon>{{ post.comments }}</span>
                    <span class="read-more">
                      查看详情
                      <el-icon><ArrowRight /></el-icon>
                    </span>
                  </div>
                </article>
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
        </main>

        <!-- 右栏：发帖入口 + 热门话题 + 推荐搭子 -->
        <aside class="side-col stagger">
          <div class="glass-card peek-host write-card">
            <PeekMascot :size="50" />
            <span class="write-icon"><el-icon><EditPen /></el-icon></span>
            <h3>分享你的学习心得</h3>
            <p class="text-muted">记录一次练习收获，或者向搭子们提个问题。</p>
            <button class="write-btn" type="button" @click="$router.push('/community/post/new')">
              <el-icon><Plus /></el-icon>
              发布新帖子
            </button>
          </div>

          <div v-if="communityStore.topics.length" class="glass-card topic-card">
            <div class="side-title">热门话题</div>
            <div class="topic-chips">
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
          </div>

          <div class="glass-card partner-card">
            <div class="side-title">推荐搭子</div>
            <ul class="partner-list">
              <li v-for="p in partners" :key="p.name" class="partner-item">
                <el-avatar :size="36" :style="{ background: avatarColor(p.name) }">
                  {{ p.name[0] }}
                </el-avatar>
                <div class="partner-text">
                  <strong>{{ p.name }}</strong>
                  <span class="text-muted">{{ p.topic }}</span>
                </div>
                <span class="partner-level">{{ p.level }}</span>
              </li>
            </ul>
            <button class="pair-btn" type="button" @click="$router.push('/community/pair')">
              进入结伴大厅
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppHeader from '@/components/base/AppHeader.vue'
import PeekMascot from '@/components/base/PeekMascot.vue'
import { useCommunityStore } from '@/stores/community'
import { useUserStore } from '@/stores/user'

const communityStore = useCommunityStore()
const userStore = useUserStore()

const activeTab = ref('feed')
const topic = ref('')

/** 处罚期内禁止发帖：由 users.ban_until / ban_reason 驱动 */
const isPunished = computed(() => !!userStore.userInfo?.punished)
const punishMessage = computed(() => {
  const u = userStore.userInfo
  if (!u?.punished) return ''
  const reason = u.banReason || '违反社区规范'
  return `您因「${reason}」被限制发帖至 ${u.banUntil || '--'}，期间无法发帖或评论`
})

const AVATAR_COLORS = ['#3b6fe0', '#22a06b', '#ff6a4d', '#e8a23d', '#7a5af8', '#2bb3c0']

// 纯展示：推荐搭子
const partners = [
  { name: 'Leo', level: 'B1', topic: '商务英语 · 模拟会议' },
  { name: 'Momo', level: 'B2', topic: '雅思口语 · Part 3' },
  { name: 'Cici', level: 'A2', topic: '日常闲聊 · 周末计划' },
]

function avatarColor(name: string) {
  let sum = 0
  for (const ch of name) sum += ch.charCodeAt(0)
  return AVATAR_COLORS[sum % AVATAR_COLORS.length]
}

// 纯展示：按互动量折算的活跃等级徽标
function activityLevel(likes: number, comments: number) {
  const score = likes + comments * 2
  if (score >= 60) return 5
  if (score >= 30) return 4
  if (score >= 14) return 3
  if (score >= 5) return 2
  return 1
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
  // 拉取最新用户信息，确保处罚状态与后台一致
  userStore.fetchMe().catch(() => {})
})
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

.header-post-btn {
  padding: 8px 20px;
  border-radius: 999px;
  font-weight: 600;
  background: rgba(59, 111, 224, 0.08);
  transition:
    transform 0.35s $ease-apple,
    background 0.3s ease;

  &:hover {
    transform: translateY(-1px);
    background: rgba(59, 111, 224, 0.14);
  }
}

// ---------------- 发帖处罚提示 ----------------
.punish-banner {
  margin-bottom: 20px;
  border-radius: var(--radius-lg);
}

// ---------------- 页头 ----------------
.community-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 28px;
  margin-bottom: 26px;

  h2 {
    margin: 0;
    font-size: 34px;
    font-weight: 700;
    letter-spacing: -0.03em;
  }

  p {
    margin: 10px 0 0;
    font-size: 14.5px;
    max-width: 560px;
  }
}

.hero-stats {
  display: flex;
  gap: 12px;
}

.hero-stat {
  min-width: 108px;
  padding: 12px 18px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
  transition:
    transform 0.35s $ease-apple,
    box-shadow 0.35s $ease-apple;

  &:hover {
    transform: translateY(-3px);
    box-shadow: var(--shadow-md);
  }

  strong {
    display: block;
    font-size: 22px;
    font-weight: 800;
    letter-spacing: -0.02em;
    color: var(--primary);
  }

  span {
    font-size: 12.5px;
  }
}

// ---------------- 两栏布局 ----------------
.community-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 28px;
  align-items: start;
}

.feed-col {
  min-width: 0;

  :deep(.el-tabs__header) {
    margin-bottom: 20px;
  }

  :deep(.el-tabs__nav-wrap::after) {
    background-color: var(--border);
  }

  :deep(.el-tabs__item) {
    height: 42px;
    font-size: 15px;
    font-weight: 600;
  }
}

.feed-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;

  h3 {
    margin: 0;
    font-size: 20px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }

  span {
    font-size: 12.5px;
  }
}

// ---------------- 帖子卡片 ----------------
.post-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-card {
  padding: 20px 24px 18px;
  cursor: pointer;

  .post-head {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .author-block {
    min-width: 0;
  }

  .author-line {
    display: flex;
    align-items: center;
    gap: 8px;

    .author-name {
      font-size: 14.5px;
      font-weight: 700;

      &.author-link {
        cursor: pointer;
        transition: color 0.3s ease;
      }

      &.author-link:hover {
        color: var(--primary);
        text-decoration: underline;
      }
      letter-spacing: -0.01em;
    }

    .level-badge {
      padding: 1px 8px;
      border-radius: 999px;
      font-size: 11px;
      font-weight: 700;
      color: var(--primary);
      background: rgba(59, 111, 224, 0.1);
    }
  }

  .post-meta {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-top: 3px;
    font-size: 12.5px;

    .topic-tag {
      font-weight: 600;
      color: var(--primary);
    }
  }

  .post-title {
    margin: 14px 0 6px;
    font-size: 18px;
    font-weight: 700;
    line-height: 1.4;
    letter-spacing: -0.02em;
    transition: color 0.3s ease;
  }

  &:hover .post-title {
    color: var(--primary);
  }

  .post-content {
    margin: 0;
    font-size: 14px;
    line-height: 1.7;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .post-foot {
    display: flex;
    align-items: center;
    gap: 20px;
    margin-top: 16px;
    padding-top: 14px;
    border-top: 1px solid var(--border);
    font-size: 13px;
    color: var(--muted);

    .stat {
      display: inline-flex;
      align-items: center;
      gap: 5px;
    }

    .read-more {
      margin-left: auto;
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font-weight: 600;
      color: var(--primary);
      opacity: 0.85;

      .el-icon {
        transition: transform 0.4s $ease-apple;
      }
    }
  }

  &:hover .read-more .el-icon {
    transform: translateX(4px);
  }
}

// ---------------- 右栏 ----------------
.side-col {
  display: flex;
  flex-direction: column;
  gap: 18px;
  position: sticky;
  top: 96px;
}

.side-title {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.01em;
  margin-bottom: 14px;
}

.write-card {
  position: relative;
  padding: 22px 22px 24px;

  .write-icon {
    display: grid;
    place-items: center;
    width: 42px;
    height: 42px;
    font-size: 21px;
    border-radius: 14px;
    color: var(--primary);
    background: rgba(59, 111, 224, 0.1);
    transition: transform 0.5s $ease-spring;
  }

  &:hover .write-icon {
    transform: scale(1.08) rotate(-6deg);
  }

  h3 {
    margin: 14px 0 8px;
    font-size: 18px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }

  p {
    margin: 0;
    font-size: 13px;
    line-height: 1.7;
  }

  .write-btn {
    margin-top: 18px;
    width: 100%;
    height: 44px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 7px;
    border: none;
    border-radius: 999px;
    font: inherit;
    font-size: 14.5px;
    font-weight: 600;
    color: #fff;
    cursor: pointer;
    background: var(--primary);
    box-shadow: 0 10px 22px rgba(59, 111, 224, 0.26);
    transition:
      transform 0.35s $ease-apple,
      box-shadow 0.35s $ease-apple;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 16px 30px rgba(59, 111, 224, 0.36);
    }
  }
}

.topic-card {
  padding: 20px 22px 22px;

  .topic-chips {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    :deep(.el-check-tag) {
      border-radius: 999px;
      padding: 6px 14px;
      font-size: 13px;
      transition: transform 0.35s $ease-apple;

      &:hover {
        transform: translateY(-2px);
      }
    }
  }
}

.partner-card {
  padding: 20px 22px 22px;

  .partner-list {
    margin: 0;
    padding: 0;
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .partner-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 10px;
    border-radius: var(--radius-md);
    transition:
      transform 0.35s $ease-apple,
      background 0.3s ease;

    &:hover {
      transform: translateX(4px);
      background: rgba(59, 111, 224, 0.06);
    }

    .partner-text {
      display: flex;
      flex-direction: column;
      min-width: 0;

      strong {
        font-size: 13.5px;
        font-weight: 700;
      }

      span {
        font-size: 12px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }

    .partner-level {
      margin-left: auto;
      font-size: 12px;
      font-weight: 700;
      color: var(--success);
    }
  }

  .pair-btn {
    margin-top: 14px;
    width: 100%;
    height: 40px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    border-radius: 999px;
    border: 1px solid rgba(59, 111, 224, 0.24);
    background: rgba(255, 255, 255, 0.9);
    font: inherit;
    font-size: 13.5px;
    font-weight: 600;
    color: var(--primary);
    cursor: pointer;
    transition:
      transform 0.35s $ease-apple,
      box-shadow 0.35s $ease-apple,
      background 0.3s ease;

    .el-icon {
      transition: transform 0.4s $ease-apple;
    }

    &:hover {
      transform: translateY(-2px);
      background: #fff;
      box-shadow: 0 12px 24px rgba(31, 42, 68, 0.12);

      .el-icon {
        transform: translateX(3px);
      }
    }
  }
}

// ---------------- 窄屏兜底 ----------------
@media (max-width: 900px) {
  .community-hero {
    flex-direction: column;
    align-items: flex-start;

    h2 {
      font-size: 24px;
    }
  }

  .community-layout {
    grid-template-columns: 1fr;
  }

  .side-col {
    position: static;
  }

  .post-card {
    padding: 16px 18px;
  }
}
</style>