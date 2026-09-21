<template>
  <div class="post-detail page">
    <AppHeader title="帖子详情" back />

    <div v-loading="loading" class="page-shell detail-body">
      <template v-if="post">
        <div class="detail-layout">
          <!-- 左栏：正文 + 评论 -->
          <main class="main-col">
            <article class="glass-card peek-host post-card">
              <PeekMascot :size="48" />

              <div class="post-head">
                <el-avatar :size="44" :style="{ background: avatarColor(post.author) }">
                  {{ post.author[0]?.toUpperCase() }}
                </el-avatar>
                <div class="post-head-info">
                  <div class="author-name author-link" @click.stop="goAuthor(post.authorId)">{{ post.author }}</div>
                  <div class="text-muted post-time">{{ formatDateTime(post.createTime) }}</div>
                </div>
                <span class="topic-tag">#{{ post.topic }}</span>
              </div>

              <h1 class="post-title">{{ post.title }}</h1>
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
                <SharePanel contentType="post" :content-id="post.id" />
                <span class="action-hint text-muted">友善交流，一起进步</span>
              </div>
            </article>

            <!-- 评论 -->
            <section class="glass-card comment-card">
              <div class="card-head">
                <h3>评论</h3>
                <span class="text-muted">{{ currentComments.length }} 条</span>
              </div>

              <el-empty
                v-if="!currentComments.length"
                :image-size="60"
                description="暂无评论，快来抢沙发"
              />

              <div v-else class="comment-list stagger">
                <div v-for="c in currentComments" :key="c.id" class="comment-item">
                  <el-avatar :size="34" :style="{ background: avatarColor(c.author) }">
                    {{ c.author[0]?.toUpperCase() }}
                  </el-avatar>
                  <div class="comment-body">
                    <div class="comment-meta">
                      <span class="comment-author">{{ c.author }}</span>
                      <span class="comment-time text-muted">{{ formatDateTime(c.createTime) }}</span>
                    </div>
                    <div class="comment-content">{{ c.content }}</div>
                  </div>
                </div>
              </div>

              <div class="comment-input">
                <el-input v-model="draft" type="textarea" :rows="3" resize="none" placeholder="友善评论…" />
                <el-button type="primary" class="send-btn" :loading="commenting" @click="onComment">
                  发送
                </el-button>
              </div>
            </section>
          </main>

          <!-- 右栏：作者信息 + 相关推荐 -->
          <aside class="side-col stagger">
            <div class="glass-card author-card">
              <div class="side-title">关于作者</div>
              <div class="author-row">
                <el-avatar :size="52" :style="{ background: avatarColor(post.author) }" class="author-avatar" @click="goAuthor(post.authorId)">
                  {{ post.author[0]?.toUpperCase() }}
                </el-avatar>
                <div class="author-text">
                  <strong>{{ post.author }}</strong>
                  <span class="text-muted">发布于 {{ formatDateTime(post.createTime) }}</span>
                </div>
              </div>

              <button class="author-home-btn" type="button" @click="goAuthor(post.authorId)">
                查看主页
                <el-icon><ArrowRight /></el-icon>
              </button>

              <div class="author-stats">
                <div class="stat-item">
                  <strong>{{ post.likes }}</strong>
                  <span class="text-muted">获赞</span>
                </div>
                <div class="stat-item">
                  <strong>{{ post.comments }}</strong>
                  <span class="text-muted">评论</span>
                </div>
              </div>
            </div>

            <div class="glass-card related-card">
              <div class="side-title">相关推荐</div>

              <ul v-if="relatedPosts.length" class="related-list">
                <li
                  v-for="p in relatedPosts"
                  :key="p.id"
                  class="related-item"
                  @click="$router.push(`/community/post/${p.id}`)"
                >
                  <div class="related-title">{{ p.title }}</div>
                  <div class="related-meta text-muted">
                    <span>#{{ p.topic }}</span>
                    <span><el-icon><Star /></el-icon>{{ p.likes }}</span>
                  </div>
                  <el-icon class="related-arrow"><ArrowRight /></el-icon>
                </li>
              </ul>

              <p v-else class="related-empty text-muted">这个话题下还没有更多帖子。</p>

              <button class="back-btn" type="button" @click="$router.push('/community')">
                返回社区
                <el-icon><ArrowRight /></el-icon>
              </button>
            </div>
          </aside>
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
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
import PeekMascot from '@/components/base/PeekMascot.vue'
import SharePanel from '@/components/business/SharePanel.vue'
import { useCommunityStore } from '@/stores/community'

const route = useRoute()
const router = useRouter()
const communityStore = useCommunityStore()

const loading = ref(false)
const commenting = ref(false)
const draft = ref('')

const post = computed(() => communityStore.currentPost)
const currentComments = computed(() => communityStore.currentComments)

// 纯展示：同话题下的相关帖子
const relatedPosts = computed(() =>
  communityStore.posts
    .filter((p) => p.id !== post.value?.id && p.topic === post.value?.topic)
    .slice(0, 3),
)

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

function goAuthor(authorId?: number | null) {
  if (authorId) router.push(`/user/${authorId}`)
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
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

.detail-body {
  min-height: 60vh;
}

// ---------------- 两栏布局 ----------------
.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 28px;
  align-items: start;
}

.main-col {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

// ---------------- 正文卡片 ----------------
.post-card {
  position: relative;
  padding: 24px 32px 26px;

  .post-head {
    display: flex;
    align-items: center;
    gap: 12px;
    padding-bottom: 18px;
    border-bottom: 1px solid var(--border);

    .post-head-info {
      min-width: 0;

      .author-name {
        font-size: 14.5px;
        font-weight: 700;
        letter-spacing: -0.01em;
      }

      .author-link {
        cursor: pointer;
        transition: color 0.3s ease;
      }

      .author-link:hover {
        color: var(--primary);
        text-decoration: underline;
      }

      .post-time {
        font-size: 12.5px;
      }
    }

    .topic-tag {
      margin-left: auto;
      padding: 4px 12px;
      border-radius: 999px;
      font-size: 12.5px;
      font-weight: 600;
      color: var(--primary);
      background: rgba(59, 111, 224, 0.1);
      transition: transform 0.35s $ease-apple;
    }

    &:hover .topic-tag {
      transform: translateY(-2px);
    }
  }

  .post-title {
    margin: 22px 0 14px;
    font-size: 30px;
    line-height: 1.35;
    font-weight: 700;
    letter-spacing: -0.03em;
  }

  .post-content {
    margin: 0;
    font-size: 15px;
    line-height: 1.85;
    color: var(--ink);
  }

  .post-actions {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-top: 26px;
    padding-top: 20px;
    border-top: 1px solid var(--border);

    .el-button {
      transition:
        transform 0.35s $ease-spring,
        box-shadow 0.35s $ease-apple;

      &:hover {
        transform: translateY(-2px);
        box-shadow: var(--shadow-md);
      }
    }

    .action-hint {
      font-size: 12.5px;
    }
  }
}

// ---------------- 评论 ----------------
.comment-card {
  padding: 24px 32px 26px;
}

.card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;

  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }

  span {
    font-size: 12.5px;
  }
}

.comment-list {
  display: flex;
  flex-direction: column;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px 12px;
  border-radius: var(--radius-md);
  border-bottom: 1px solid var(--border);
  transition:
    background 0.3s ease,
    transform 0.35s $ease-apple;

  &:hover {
    background: rgba(59, 111, 224, 0.05);
    transform: translateX(4px);
  }

  .comment-body {
    flex: 1;
    min-width: 0;

    .comment-meta {
      display: flex;
      align-items: baseline;
      gap: 10px;

      .comment-author {
        font-size: 13.5px;
        font-weight: 700;
      }

      .comment-time {
        font-size: 12px;
      }
    }

    .comment-content {
      margin-top: 4px;
      font-size: 14.5px;
      line-height: 1.7;
      color: var(--ink);
    }
  }
}

.comment-input {
  display: flex;
  gap: 12px;
  margin-top: 20px;

  :deep(.el-textarea) {
    flex: 1;
  }

  :deep(.el-textarea__inner) {
    border-radius: var(--radius-md);
    font-size: 14.5px;
    line-height: 1.7;
    padding: 12px 14px;
  }

  .send-btn {
    align-self: flex-end;
    height: 42px;
    padding: 0 26px;
    border-radius: 999px;
    font-weight: 600;
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

.author-card {
  padding: 22px 22px 24px;

  .author-row {
    display: flex;
    align-items: center;
    gap: 12px;

    .author-text {
      display: flex;
      flex-direction: column;
      gap: 3px;
      min-width: 0;

      strong {
        font-size: 15px;
        font-weight: 700;
      }

      span {
        font-size: 12.5px;
      }
    }

    .author-avatar {
      cursor: pointer;
      transition: transform 0.4s $ease-spring;
    }

    .author-avatar:hover {
      transform: scale(1.06);
    }
  }

  .author-home-btn {
    width: 100%;
    height: 38px;
    margin-top: 14px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    border: 1px solid rgba(59, 111, 224, 0.24);
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.9);
    font: inherit;
    font-size: 13px;
    font-weight: 600;
    color: var(--primary);
    cursor: pointer;
    transition:
      transform 0.35s $ease-apple,
      box-shadow 0.35s $ease-apple;

    .el-icon {
      transition: transform 0.4s $ease-apple;
    }

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 10px 20px rgba(31, 42, 68, 0.12);

      .el-icon {
        transform: translateX(3px);
      }
    }
  }

  .author-stats {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
    margin-top: 18px;

    .stat-item {
      padding: 12px 14px;
      border-radius: var(--radius-md);
      background: rgba(59, 111, 224, 0.07);
      transition: transform 0.35s $ease-apple;

      &:hover {
        transform: translateY(-3px);
      }

      strong {
        display: block;
        font-size: 20px;
        font-weight: 800;
        letter-spacing: -0.02em;
        color: var(--primary);
      }

      span {
        font-size: 12px;
      }
    }
  }
}

.related-card {
  padding: 22px 22px 24px;

  .related-list {
    margin: 0;
    padding: 0;
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .related-item {
    position: relative;
    padding: 12px 34px 12px 14px;
    border-radius: var(--radius-md);
    border: 1px solid var(--border);
    background: rgba(255, 255, 255, 0.7);
    cursor: pointer;
    transition:
      transform 0.35s $ease-apple,
      box-shadow 0.35s $ease-apple,
      border-color 0.35s ease;

    &:hover {
      transform: translateY(-3px);
      border-color: rgba(59, 111, 224, 0.26);
      box-shadow: var(--shadow-md);

      .related-arrow {
        transform: translateX(4px);
        opacity: 1;
      }
    }

    .related-title {
      font-size: 13.5px;
      font-weight: 700;
      line-height: 1.5;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .related-meta {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-top: 6px;
      font-size: 12px;

      span {
        display: inline-flex;
        align-items: center;
        gap: 4px;
      }
    }

    .related-arrow {
      position: absolute;
      right: 12px;
      top: 50%;
      margin-top: -8px;
      color: var(--primary);
      opacity: 0.4;
      transition:
        transform 0.4s $ease-apple,
        opacity 0.3s ease;
    }
  }

  .related-empty {
    margin: 0;
    font-size: 13px;
    line-height: 1.7;
  }

  .back-btn {
    margin-top: 16px;
    width: 100%;
    height: 42px;
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
      box-shadow 0.35s $ease-apple;

    .el-icon {
      transition: transform 0.4s $ease-apple;
    }

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 12px 24px rgba(31, 42, 68, 0.12);

      .el-icon {
        transform: translateX(3px);
      }
    }
  }
}

// ---------------- 窄屏兜底 ----------------
@media (max-width: 900px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .side-col {
    position: static;
  }

  .post-card,
  .comment-card {
    padding: 20px;
  }

  .post-card .post-title {
    font-size: 22px;
  }
}
</style>