<template>
  <div class="user-profile page">
    <AppHeader :title="profile?.user.nickname || '用户主页'" back />

    <div v-loading="loading" class="page-shell profile-body">
      <template v-if="profile">
        <!-- 顶部资料卡 -->
        <header class="glass-card hero-card">
          <div class="hero-bg" />
          <div class="hero-main">
            <el-avatar :size="82" :style="{ background: avatarColor(profile.user.nickname) }">
              {{ profile.user.nickname[0]?.toUpperCase() }}
            </el-avatar>

            <div class="hero-info">
              <div class="hero-name">
                <h2>{{ profile.user.nickname }}</h2>
                <LevelTag v-if="profile.user.level" :level="profile.user.level" />
                <span v-if="profile.followedMe" class="follow-flag">关注了你</span>
              </div>
              <p class="hero-bio text-muted">
                {{ profile.user.bio || '这位同学还没有填写个性签名～' }}
              </p>
              <div class="hero-counts">
                <span><strong>{{ profile.followers }}</strong> 粉丝</span>
                <span><strong>{{ profile.following }}</strong> 关注</span>
                <span><strong>{{ profile.stats.postCount }}</strong> 帖子</span>
              </div>
            </div>

            <div v-if="!profile.isSelf" class="hero-actions">
              <button
                class="follow-btn"
                :class="{ 'is-following': profile.followingByMe }"
                type="button"
                @click="onToggleFollow"
                @mouseenter="hoverFollow = true"
                @mouseleave="hoverFollow = false"
              >
                {{ followText }}
              </button>
              <button class="dm-btn" type="button" @click="goMessage">
                <el-icon><ChatDotRound /></el-icon>
                发私信
              </button>
            </div>
            <div v-else class="hero-actions">
              <button class="dm-btn" type="button" @click="$router.push('/profile')">编辑我的资料</button>
            </div>
          </div>
        </header>

        <div class="profile-layout">
          <main class="main-col">
            <!-- 学习统计 -->
            <section class="glass-card stat-card">
              <h3 class="card-title">学习数据</h3>
              <div class="stat-grid">
                <div class="stat-cell">
                  <strong>{{ profile.stats.studyDays }}</strong>
                  <span class="text-muted">学习天数</span>
                </div>
                <div class="stat-cell">
                  <strong>{{ profile.stats.totalMinutes }}</strong>
                  <span class="text-muted">累计分钟</span>
                </div>
                <div class="stat-cell">
                  <strong>{{ profile.stats.sessionCount }}</strong>
                  <span class="text-muted">完成对话</span>
                </div>
                <div class="stat-cell">
                  <strong>{{ profile.stats.avgScore }}</strong>
                  <span class="text-muted">平均综合分</span>
                </div>
              </div>
            </section>

            <!-- 成就徽章 -->
            <section class="glass-card badge-card">
              <h3 class="card-title">成就徽章</h3>
              <div class="badge-grid">
                <div
                  v-for="b in profile.achievements"
                  :key="b.key"
                  class="badge-item"
                  :class="{ 'is-locked': !b.achieved }"
                >
                  <span class="badge-medal">{{ b.achieved ? '🏅' : '🔒' }}</span>
                  <div class="badge-text">
                    <strong>{{ b.name }}</strong>
                    <span class="text-muted">{{ b.desc }}</span>
                  </div>
                  <div class="badge-progress">
                    <div class="progress-bar">
                      <i :style="{ width: `${b.progress}%` }" />
                    </div>
                    <span class="progress-text text-muted">{{ b.value }}/{{ b.target }}</span>
                  </div>
                </div>
              </div>
            </section>
          </main>

          <aside class="side-col">
            <section class="glass-card tabs-card">
              <el-tabs v-model="activeTab" class="profile-tabs">
                <el-tab-pane label="动态" name="posts">
                  <ul v-if="profile.posts.length" class="post-list">
                    <li
                      v-for="p in profile.posts"
                      :key="p.id"
                      class="post-item"
                      @click="$router.push(`/community/post/${p.id}`)"
                    >
                      <div class="post-title">{{ p.title }}</div>
                      <div class="post-meta text-muted">
                        <span>#{{ p.topic }}</span>
                        <span>{{ formatDateTime(p.createTime) }}</span>
                      </div>
                    </li>
                  </ul>
                  <el-empty v-else :image-size="56" description="还没有发布过帖子" />
                </el-tab-pane>

                <el-tab-pane :label="`粉丝 ${profile.followers}`" name="followers">
                  <ul v-if="followers.length" class="user-list">
                    <li v-for="u in followers" :key="u.userId" class="user-item">
                      <el-avatar :size="34" :style="{ background: avatarColor(u.nickname) }">
                        {{ u.nickname[0]?.toUpperCase() }}
                      </el-avatar>
                      <div class="user-text">
                        <strong @click="goProfile(u.userId)">{{ u.nickname }}</strong>
                        <span class="text-muted">{{ u.bio || '暂无签名' }}</span>
                      </div>
                      <span v-if="u.level" class="user-level">{{ u.level }}</span>
                    </li>
                  </ul>
                  <el-empty v-else :image-size="56" description="还没有粉丝" />
                </el-tab-pane>

                <el-tab-pane :label="`关注 ${profile.following}`" name="following">
                  <ul v-if="following.length" class="user-list">
                    <li v-for="u in following" :key="u.userId" class="user-item">
                      <el-avatar :size="34" :style="{ background: avatarColor(u.nickname) }">
                        {{ u.nickname[0]?.toUpperCase() }}
                      </el-avatar>
                      <div class="user-text">
                        <strong @click="goProfile(u.userId)">{{ u.nickname }}</strong>
                        <span class="text-muted">{{ u.bio || '暂无签名' }}</span>
                      </div>
                      <span v-if="u.level" class="user-level">{{ u.level }}</span>
                    </li>
                  </ul>
                  <el-empty v-else :image-size="56" description="还没有关注的人" />
                </el-tab-pane>
              </el-tabs>
            </section>
          </aside>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="用户不存在">
        <el-button type="primary" @click="$router.push('/community')">返回社区</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
import LevelTag from '@/components/business/LevelTag.vue'
import * as socialApi from '@/api/modules/social'
import type { FollowUserDto, UserProfileResult } from '@/types/api'

const route = useRoute()
const router = useRouter()

const profile = ref<UserProfileResult | null>(null)
const followers = ref<FollowUserDto[]>([])
const following = ref<FollowUserDto[]>([])
const loading = ref(false)
const followingPending = ref(false)
const hoverFollow = ref(false)
const activeTab = ref('posts')

const AVATAR_COLORS = ['#3b6fe0', '#22a06b', '#ff6a4d', '#e8a23d', '#7a5af8', '#2bb3c0']

const followText = computed(() => {
  if (!profile.value) return '关注'
  if (!profile.value.followingByMe) return '关注'
  return hoverFollow.value ? '取消关注' : '已关注'
})

function avatarColor(name: string) {
  let sum = 0
  for (const ch of name) sum += ch.charCodeAt(0)
  return AVATAR_COLORS[sum % AVATAR_COLORS.length]
}

function formatDateTime(t?: string) {
  if (!t) return ''
  return t.length >= 16 ? t.slice(5, 16) : t
}

async function loadRelations(userId: number) {
  try {
    const [fs, fw] = await Promise.all([socialApi.listFollowers(userId), socialApi.listFollowing(userId)])
    followers.value = fs
    following.value = fw
  } catch {
    /* 错误提示已由请求层统一弹出 */
  }
}

async function load() {
  const id = Number(route.params.id)
  if (!Number.isInteger(id)) return
  loading.value = true
  try {
    profile.value = await socialApi.userProfile(id)
    await loadRelations(id)
  } catch {
    profile.value = null
  } finally {
    loading.value = false
  }
}

async function onToggleFollow() {
  if (!profile.value || followingPending.value) return
  const id = profile.value.user.userId
  followingPending.value = true
  try {
    if (profile.value.followingByMe) {
      const res = await socialApi.unfollowUser(id)
      profile.value.followingByMe = false
      profile.value.followers = res.followers
      ElMessage.success('已取消关注')
    } else {
      const res = await socialApi.followUser(id)
      profile.value.followingByMe = true
      profile.value.followers = res.followers
      ElMessage.success('关注成功')
    }
    await loadRelations(id)
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    followingPending.value = false
    hoverFollow.value = false
  }
}

function goMessage() {
  if (!profile.value) return
  router.push(`/messages?peer=${profile.value.user.userId}`)
}

function goProfile(userId: number) {
  router.push(`/user/${userId}`)
}

watch(() => route.params.id, () => void load())
onMounted(() => void load())
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

.profile-body {
  padding-bottom: 30px;
}

// ---------------- 顶部资料卡 ----------------
.hero-card {
  position: relative;
  overflow: hidden;
  padding: 30px 32px 28px;
  margin-bottom: 22px;
}

.hero-bg {
  position: absolute;
  inset: -40% -10% auto -10%;
  height: 220px;
  background:
    radial-gradient(520px 200px at 18% 0%, rgba(91, 139, 240, 0.24), transparent 70%),
    radial-gradient(420px 180px at 82% 10%, rgba(124, 92, 255, 0.18), transparent 72%);
  pointer-events: none;
}

.hero-main {
  position: relative;
  display: flex;
  align-items: center;
  gap: 20px;
}

.hero-info {
  flex: 1;
  min-width: 0;
}

.hero-name {
  display: flex;
  align-items: center;
  gap: 10px;

  h2 {
    margin: 0;
    font-size: 26px;
    font-weight: 700;
    letter-spacing: -0.03em;
  }

  .follow-flag {
    padding: 2px 9px;
    border-radius: 999px;
    font-size: 11.5px;
    font-weight: 700;
    color: var(--primary);
    background: rgba(59, 111, 224, 0.12);
  }
}

.hero-bio {
  margin: 8px 0 0;
  font-size: 13.5px;
  line-height: 1.7;
}

.hero-counts {
  display: flex;
  gap: 18px;
  margin-top: 12px;
  font-size: 12.5px;
  color: var(--muted);

  strong {
    font-size: 15px;
    font-weight: 800;
    color: var(--ink);
    margin-right: 3px;
  }
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.follow-btn {
  height: 40px;
  padding: 0 24px;
  border: none;
  border-radius: 999px;
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #5b8bf0, #2f5bb3);
  box-shadow: 0 10px 22px rgba(59, 111, 224, 0.26);
  cursor: pointer;
  transition:
    transform 0.32s $ease-spring,
    box-shadow 0.32s $ease-apple,
    background 0.3s ease;

  &:hover {
    transform: translateY(-2px) scale(1.02);
    box-shadow: 0 16px 28px rgba(59, 111, 224, 0.34);
  }

  &.is-following {
    color: #55617e;
    background: rgba(255, 255, 255, 0.94);
    border: 1px solid rgba(31, 42, 68, 0.12);
    box-shadow: none;

    &:hover {
      color: #d93a4c;
      border-color: rgba(217, 58, 76, 0.4);
      background: rgba(217, 58, 76, 0.08);
    }
  }
}

.dm-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 40px;
  padding: 0 20px;
  border: 1px solid rgba(59, 111, 224, 0.24);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  color: var(--primary);
  cursor: pointer;
  transition:
    transform 0.32s $ease-apple,
    box-shadow 0.32s $ease-apple;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 24px rgba(31, 42, 68, 0.12);
  }
}

// ---------------- 两栏 ----------------
.profile-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 22px;
  align-items: start;
}

.main-col {
  display: flex;
  flex-direction: column;
  gap: 22px;
  min-width: 0;
}

.card-title {
  margin: 0 0 16px;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.stat-card {
  padding: 22px 24px 24px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.stat-cell {
  padding: 16px 14px;
  border-radius: var(--radius-md);
  background: rgba(59, 111, 224, 0.07);
  text-align: center;
  transition: transform 0.35s $ease-apple;

  &:hover {
    transform: translateY(-3px);
  }

  strong {
    display: block;
    font-size: 22px;
    font-weight: 800;
    letter-spacing: -0.02em;
    color: var(--primary);
  }

  span {
    font-size: 12px;
  }
}

// ---------------- 成就 ----------------
.badge-card {
  padding: 22px 24px 24px;
}

.badge-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.badge-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.75);
  transition:
    transform 0.35s $ease-apple,
    box-shadow 0.35s $ease-apple;

  &:hover {
    transform: translateY(-3px);
    box-shadow: var(--shadow-md);
  }

  &.is-locked {
    opacity: 0.62;
    filter: grayscale(0.4);
  }

  .badge-medal {
    font-size: 22px;
  }

  .badge-text {
    min-width: 0;

    strong {
      display: block;
      font-size: 13.5px;
      font-weight: 700;
    }

    span {
      font-size: 11.5px;
    }
  }

  .badge-progress {
    margin-left: auto;
    width: 66px;
    flex-shrink: 0;

    .progress-bar {
      height: 5px;
      border-radius: 999px;
      background: rgba(31, 42, 68, 0.09);
      overflow: hidden;

      i {
        display: block;
        height: 100%;
        border-radius: 999px;
        background: linear-gradient(90deg, #5b8bf0, #2f5bb3);
        transition: width 0.5s $ease-apple;
      }
    }

    .progress-text {
      display: block;
      margin-top: 4px;
      font-size: 10.5px;
      text-align: right;
    }
  }
}

// ---------------- 右栏 Tabs ----------------
.tabs-card {
  padding: 18px 20px 22px;
}

.post-list,
.user-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.post-item {
  padding: 12px 14px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition:
    transform 0.35s $ease-apple,
    border-color 0.3s ease;

  &:hover {
    transform: translateX(4px);
    border-color: rgba(59, 111, 224, 0.28);
  }

  .post-title {
    font-size: 13.5px;
    font-weight: 700;
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .post-meta {
    display: flex;
    gap: 12px;
    margin-top: 6px;
    font-size: 11.5px;
  }
}

.user-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: var(--radius-md);
  transition:
    background 0.3s ease,
    transform 0.35s $ease-apple;

  &:hover {
    background: rgba(59, 111, 224, 0.07);
    transform: translateX(3px);
  }

  .user-text {
    flex: 1;
    min-width: 0;

    strong {
      display: block;
      font-size: 13.5px;
      font-weight: 700;
      cursor: pointer;
    }

    span {
      display: block;
      font-size: 11.5px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }

  .user-level {
    font-size: 11.5px;
    font-weight: 700;
    color: var(--primary);
  }
}

@media (max-width: 900px) {
  .hero-main {
    flex-wrap: wrap;
  }

  .hero-actions {
    width: 100%;
  }

  .profile-layout {
    grid-template-columns: 1fr;
  }

  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .badge-grid {
    grid-template-columns: 1fr;
  }
}
</style>
