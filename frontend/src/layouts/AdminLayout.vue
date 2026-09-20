<template>
  <el-container class="admin-layout">
    <el-aside :width="collapsed ? '80px' : '248px'" class="admin-aside" :class="{ 'is-collapsed': collapsed }">
      <div class="admin-logo">
        <span class="logo-mark">🎙️</span>
        <span class="logo-text">
          <strong>英语口语训练系统</strong>
          <em>Admin Console</em>
        </span>
      </div>

      <el-menu
        :default-active="route.path"
        :collapse="collapsed"
        :collapse-transition="false"
        router
        class="admin-menu"
      >
        <el-menu-item-group title="运营总览">
          <el-menu-item index="/admin/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <template #title>数据看板</template>
          </el-menu-item>
          <el-menu-item index="/admin/usage">
            <el-icon><Histogram /></el-icon>
            <template #title>用户使用报表</template>
          </el-menu-item>
          <el-menu-item index="/admin/activity">
            <el-icon><TrendCharts /></el-icon>
            <template #title>日活动量</template>
          </el-menu-item>
          <el-menu-item index="/admin/metrics">
            <el-icon><Histogram /></el-icon>
            <template #title>推荐效果指标</template>
          </el-menu-item>
        </el-menu-item-group>

        <el-menu-item-group title="系统监控">
          <el-menu-item index="/admin/compute">
            <el-icon><Cpu /></el-icon>
            <template #title>算力监控</template>
          </el-menu-item>
          <el-menu-item index="/admin/system">
            <el-icon><Monitor /></el-icon>
            <template #title>系统概览</template>
          </el-menu-item>
        </el-menu-item-group>

        <el-menu-item-group title="内容运营">
          <el-menu-item index="/admin/quotes">
            <el-icon><Reading /></el-icon>
            <template #title>名句素材</template>
          </el-menu-item>
          <el-menu-item index="/admin/scenes">
            <el-icon><ChatLineSquare /></el-icon>
            <template #title>场景管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/resources">
            <el-icon><FolderOpened /></el-icon>
            <template #title>资源管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/community">
            <el-icon><ChatDotRound /></el-icon>
            <template #title>社区管理</template>
          </el-menu-item>
        </el-menu-item-group>

        <el-menu-item-group title="用户与设置">
          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon>
            <template #title>用户管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/config">
            <el-icon><Setting /></el-icon>
            <template #title>系统配置</template>
          </el-menu-item>
          <el-menu-item index="/admin/audit">
            <el-icon><Document /></el-icon>
            <template #title>操作日志</template>
          </el-menu-item>
        </el-menu-item-group>
      </el-menu>

      <div class="aside-foot">
        <span class="foot-dot" aria-hidden="true"></span>
        <span class="foot-text">服务运行正常</span>
      </div>
    </el-aside>

    <el-container class="admin-body">
      <el-header class="admin-header">
        <div class="header-left">
          <el-button class="collapse-btn" circle :title="collapsed ? '展开侧边栏' : '收起侧边栏'" @click="toggleCollapse">
            <el-icon>
              <Expand v-if="collapsed" />
              <Fold v-else />
            </el-icon>
          </el-button>
          <div class="header-left-text">
            <div class="crumb">
              <span class="crumb-root">后台</span>
              <span class="crumb-sep">/</span>
              <span class="crumb-current">{{ route.meta.title }}</span>
            </div>
            <h1 class="header-title">{{ route.meta.title }}</h1>
          </div>
        </div>

        <div class="header-right">
          <div class="admin-user">
            <el-avatar :size="30" class="admin-avatar">{{ adminInitial }}</el-avatar>
            <span class="admin-user-name">{{ adminName }}</span>
          </div>
          <el-button class="logout-btn" @click="onLogout">退出登录</el-button>
        </div>
      </el-header>

      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Expand, Fold } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 侧边栏收缩状态：记住上次选择，刷新后保持 */
const COLLAPSE_KEY = 'adminSidebarCollapsed'
const collapsed = ref(localStorage.getItem(COLLAPSE_KEY) === '1')
watch(collapsed, (v) => localStorage.setItem(COLLAPSE_KEY, v ? '1' : '0'))

function toggleCollapse() {
  collapsed.value = !collapsed.value
}

const adminName = computed(() => userStore.userInfo?.nickname || '管理员')
const adminInitial = computed(() => (userStore.userInfo?.nickname?.[0] || 'A').toUpperCase())

function onLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/admin/login')
}
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

.admin-layout {
  height: 100%;
  background: var(--surface);
}

// ==================== 侧边栏 ====================
.admin-aside {
  display: flex;
  flex-direction: column;
  padding: 20px 14px 16px;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(246, 247, 251, 0.9));
  border-right: 1px solid var(--border);
  transition: width 0.34s $ease-apple;
}

// ---------------- 收缩态：只留图标，菜单项居中 ----------------
.admin-aside.is-collapsed {
  padding: 20px 10px 16px;

  .admin-logo {
    justify-content: center;
    padding: 2px 0 20px;
  }

  .logo-text {
    display: none;
  }

  .aside-foot {
    justify-content: center;
    padding: 14px 0 4px;
  }

  .foot-text {
    display: none;
  }

  :deep(.el-menu--collapse) {
    width: 100%;
  }

  :deep(.el-menu-item) {
    padding: 0;
    justify-content: center;
  }

  :deep(.el-menu-item .el-icon) {
    margin-right: 0;
  }

  // 收缩后分组标题无处安放，直接隐藏避免留白
  :deep(.el-menu-item-group__title) {
    display: none;
  }

  // 指示条挪到图标左侧，避免贴边被裁切
  :deep(.el-menu-item::before) {
    left: 1px;
  }

  :deep(.el-menu-item:hover) {
    transform: none;
  }
}

.admin-logo {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 2px 8px 20px;
}

.logo-mark {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  font-size: 19px;
  border-radius: 13px;
  background: linear-gradient(135deg, #5b8bf0, #3b6fe0 55%, #2f5bb3);
  box-shadow: 0 10px 22px rgba(59, 111, 224, 0.3);
  transition: transform 0.5s $ease-spring;
}

.admin-logo:hover .logo-mark {
  transform: rotate(-8deg) scale(1.07);
}

.logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
  min-width: 0;

  strong {
    font-size: 15px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }

  em {
    font-size: 10px;
    font-style: normal;
    letter-spacing: 0.14em;
    text-transform: uppercase;
    color: var(--muted);
  }
}

// 菜单：大圆角胶囊 + 位移反馈 + 选中主色底与左侧指示条
.admin-menu {
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #55617e;
  --el-menu-hover-bg-color: transparent;
  --el-menu-active-color: var(--primary);
  --el-menu-base-level-padding: 18px;
  --el-menu-item-height: 46px;
  --el-menu-item-font-size: 14px;

  border-right: none;
  background: transparent;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  scrollbar-width: thin;

  // 分组标题：小号大写，弱化存在感
  :deep(.el-menu-item-group__title) {
    padding: 14px 18px 6px;
    font-size: 10.5px;
    font-weight: 700;
    letter-spacing: 0.14em;
    text-transform: uppercase;
    color: #a3aab9;
  }

  :deep(.el-menu-item-group:first-child .el-menu-item-group__title) {
    padding-top: 4px;
  }

  :deep(.el-menu-item) {
    position: relative;
    margin-bottom: 6px;
    border-radius: 14px;
    font-weight: 600;
    transition:
      color 0.28s ease,
      background 0.35s $ease-apple,
      transform 0.35s $ease-apple,
      box-shadow 0.35s $ease-apple;

    .el-icon {
      margin-right: 10px;
      font-size: 17px;
      transition: transform 0.4s $ease-spring;
    }

    // 左侧指示条
    &::before {
      content: '';
      position: absolute;
      left: 5px;
      top: 50%;
      width: 3px;
      height: 0;
      border-radius: 3px;
      background: var(--primary);
      transform: translateY(-50%);
      transition: height 0.35s $ease-apple;
    }

    &:hover {
      color: var(--primary);
      background: rgba(59, 111, 224, 0.08);
      transform: translateX(4px);

      .el-icon {
        transform: translateY(-2px);
      }
    }

    &.is-active {
      color: var(--primary);
      background: rgba(59, 111, 224, 0.12);
      box-shadow: 0 10px 22px rgba(59, 111, 224, 0.14);

      &::before {
        height: 22px;
      }

      &:hover {
        transform: none;
      }
    }
  }
}

.aside-foot {
  margin-top: auto;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 12px 4px;
  font-size: 12px;
  color: var(--muted);

  .foot-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--success);
    box-shadow: 0 0 0 3px rgba(34, 160, 107, 0.16);
  }
}

// ==================== 顶栏 ====================
.admin-body {
  min-width: 0;
}

.admin-header {
  height: 68px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 0 32px;
  background: rgba(255, 255, 255, 0.78);
  border-bottom: 1px solid rgba(31, 42, 68, 0.07);
  backdrop-filter: blur(18px) saturate(170%);
  -webkit-backdrop-filter: blur(18px) saturate(170%);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.header-left-text {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 3px;
  min-width: 0;
}

// 侧边栏收缩开关
.collapse-btn.el-button {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border: 1px solid rgba(31, 42, 68, 0.08);
  background: rgba(255, 255, 255, 0.9);
  color: #55617e;
  transition:
    transform 0.35s $ease-apple,
    color 0.28s ease,
    border-color 0.28s ease,
    box-shadow 0.35s $ease-apple;

  :deep(.el-icon) {
    font-size: 17px;
  }

  &:hover,
  &:focus {
    color: var(--primary);
    border-color: rgba(59, 111, 224, 0.34);
    background: #fff;
    transform: translateY(-2px);
    box-shadow: 0 10px 22px rgba(59, 111, 224, 0.18);
  }

  &:active {
    transform: translateY(0);
  }
}

.crumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  line-height: 1.2;

  .crumb-root {
    color: var(--muted);
  }

  .crumb-sep {
    color: #c6ccda;
  }

  .crumb-current {
    color: var(--primary);
    font-weight: 600;
  }
}

.header-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.02em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 5px 14px 5px 5px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(31, 42, 68, 0.08);
  border-radius: 999px;
  transition:
    transform 0.35s $ease-apple,
    border-color 0.3s ease,
    box-shadow 0.35s $ease-apple;

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(59, 111, 224, 0.3);
    box-shadow: 0 10px 22px rgba(31, 42, 68, 0.11);
  }

  :deep(.el-avatar) {
    background: linear-gradient(135deg, #5b8bf0, #2f5bb3);
    color: #fff;
    font-size: 13px;
    font-weight: 700;
  }
}

.admin-user-name {
  max-width: 140px;
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout-btn.el-button {
  height: 36px;
  padding: 0 18px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  color: #55617e;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(31, 42, 68, 0.08);
  transition:
    transform 0.35s $ease-apple,
    color 0.28s ease,
    background 0.28s ease,
    border-color 0.28s ease,
    box-shadow 0.35s $ease-apple;

  &:hover,
  &:focus {
    color: var(--danger);
    background: rgba(229, 72, 77, 0.07);
    border-color: rgba(229, 72, 77, 0.32);
    transform: translateY(-2px);
    box-shadow: 0 10px 22px rgba(229, 72, 77, 0.18);
  }

  &:active {
    transform: translateY(0);
  }
}

// ==================== 主区域 ====================
.admin-main {
  padding: 0;
  background: var(--surface);
  overflow-y: auto;
}

@media (max-width: 1080px) {
  .admin-aside {
    width: 210px;
  }

  .admin-header {
    padding: 0 20px;
  }

  .admin-user-name {
    display: none;
  }
}
</style>