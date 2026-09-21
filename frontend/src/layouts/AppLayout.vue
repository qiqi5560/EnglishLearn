<template>
  <div class="app-layout">
    <!-- ==================== 桌面端顶部导航（替代原手机底部 Tab 栏） ==================== -->
    <header class="top-nav">
      <div class="nav-inner">
        <router-link to="/home" class="nav-brand">
          <span class="brand-mark">🎙️</span>
          <span class="brand-text">
            <strong>英语口语训练系统</strong>
            <em>AI Scene Role-play</em>
          </span>
        </router-link>

        <nav class="nav-menu" aria-label="主导航">
          <router-link v-for="item in navItems" :key="item.path" :to="item.path" class="nav-item">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </router-link>

          <!-- 站内信入口：私信 + 通知合计未读红点（轮询刷新） -->
          <router-link to="/messages" class="nav-item nav-item--badge">
            <el-badge :value="socialStore.unread.total" :hidden="!socialStore.unread.total" :max="99">
              <span class="badge-host">
                <el-icon><Message /></el-icon>
                <span>消息</span>
              </span>
            </el-badge>
          </router-link>
        </nav>

        <div class="nav-user">
          <LevelTag :level="userStore.level || 'A1'" />
          <el-dropdown trigger="click" placement="bottom-end" @command="onCommand">
            <button class="user-chip" type="button">
              <el-avatar :size="30" :src="userStore.userInfo?.avatarUrl || undefined">
                {{ userStore.userInfo?.nickname?.[0]?.toUpperCase() || 'U' }}
              </el-avatar>
              <span class="user-name">{{ userStore.userInfo?.nickname || '学习者' }}</span>
              <el-icon class="user-chevron"><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="/profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="/plan">
                  <el-icon><Notebook /></el-icon>我的学习方案
                </el-dropdown-item>
                <el-dropdown-item command="/profile/settings">
                  <el-icon><Setting /></el-icon>设置
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import LevelTag from '@/components/business/LevelTag.vue'
import { useUserStore } from '@/stores/user'
import { useSocialStore } from '@/stores/social'

const router = useRouter()
const userStore = useUserStore()
const socialStore = useSocialStore()

const navItems = [
  { path: '/home', label: '首页', icon: 'House' },
  { path: '/practice', label: '练习', icon: 'Microphone' },
  { path: '/quotes', label: '名句跟读', icon: 'Reading' },
  { path: '/community', label: '社区', icon: 'ChatDotRound' },
  { path: '/report', label: '报表', icon: 'TrendCharts' },
  { path: '/profile', label: '我的', icon: 'User' },
]

function onCommand(command: string) {
  if (command === 'logout') {
    ElMessageBox.confirm('确定退出当前账号吗？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
    })
      .then(() => {
        userStore.logout()
        ElMessage.success('已退出登录')
        router.replace('/welcome')
      })
      .catch(() => {})
    return
  }
  router.push(command)
}

onMounted(() => {
  if (!userStore.userInfo) userStore.fetchMe().catch(() => null)
  if (userStore.token) socialStore.startPolling()
})

onUnmounted(() => {
  socialStore.stopPolling()
})
</script>

<style scoped lang="scss">
.app-layout {
  height: 100%;
  display: flex;
  flex-direction: column;
}

// ------------------------------------------------------------
// 顶部导航：毛玻璃吸顶，苹果风胶囊交互
// ------------------------------------------------------------
.top-nav {
  flex-shrink: 0;
  z-index: 30;
  background: rgba(246, 247, 251, 0.78);
  border-bottom: 1px solid rgba(31, 42, 68, 0.07);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
}

.nav-inner {
  width: min(1280px, calc(100% - 64px));
  height: 68px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 40px;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 11px;
  flex-shrink: 0;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  font-size: 19px;
  background: linear-gradient(135deg, #5b8bf0, #3b6fe0 55%, #2f5bb3);
  border-radius: 12px;
  box-shadow: 0 8px 18px rgba(59, 111, 224, 0.28);
  transition: transform 0.45s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.nav-brand:hover .brand-mark {
  transform: rotate(-8deg) scale(1.06);
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;

  strong {
    font-size: 15px;
    letter-spacing: -0.02em;
  }

  em {
    font-size: 10px;
    font-style: normal;
    letter-spacing: 0.12em;
    text-transform: uppercase;
    color: var(--muted);
  }
}

.nav-menu {
  display: flex;
  align-items: center;
  gap: 6px;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 9px 16px;
  color: #55617e;
  font-size: 14px;
  font-weight: 600;
  border-radius: 999px;
  transition:
    color 0.28s ease,
    background 0.28s ease,
    transform 0.35s cubic-bezier(0.22, 1, 0.36, 1);

  .el-icon {
    font-size: 16px;
    transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  }

  &:hover {
    color: var(--primary);
    background: rgba(59, 111, 224, 0.08);

    .el-icon {
      transform: translateY(-2px);
    }
  }

  &.router-link-active {
    color: var(--primary);
    background: rgba(59, 111, 224, 0.12);

    &::after {
      content: '';
      position: absolute;
      left: 50%;
      bottom: -13px;
      width: 18px;
      height: 3px;
      border-radius: 3px;
      background: linear-gradient(90deg, #5b8bf0, #2f5bb3);
      transform: translateX(-50%);
      animation: nav-underline 0.4s cubic-bezier(0.22, 1, 0.36, 1) both;
    }
  }
}

@keyframes nav-underline {
  from {
    width: 0;
    opacity: 0;
  }
  to {
    width: 18px;
    opacity: 1;
  }
}

.nav-item--badge {
  padding: 0;

  :deep(.el-badge) {
    display: block;
  }

  .badge-host {
    display: flex;
    align-items: center;
    gap: 7px;
    padding: 9px 16px;
  }
}

.nav-user {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-left: auto;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 5px 12px 5px 5px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(31, 42, 68, 0.08);
  border-radius: 999px;
  font: inherit;
  color: var(--ink);
  cursor: pointer;
  transition:
    transform 0.35s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.35s ease,
    border-color 0.35s ease;

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(59, 111, 224, 0.3);
    box-shadow: 0 10px 22px rgba(31, 42, 68, 0.11);
  }

  :deep(.el-avatar) {
    background: linear-gradient(135deg, #5b8bf0, #2f5bb3);
    color: #fff;
    font-size: 13px;
  }
}

.user-name {
  max-width: 120px;
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-chevron {
  font-size: 12px;
  color: var(--muted);
}

.app-main {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  scroll-behavior: smooth;
}

@media (max-width: 1080px) {
  .nav-inner {
    width: calc(100% - 40px);
    gap: 22px;
  }

  .brand-text em {
    display: none;
  }

  .user-name {
    display: none;
  }
}

@media (max-width: 900px) {
  .nav-inner {
    height: 60px;
  }

  .nav-menu {
    gap: 2px;
  }

  .nav-item {
    padding: 8px 11px;

    span {
      display: none;
    }
  }
}
</style>