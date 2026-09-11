<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="admin-aside">
      <div class="admin-logo">口语训练系统 · 后台</div>
      <el-menu :default-active="route.path" router class="admin-menu">
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据看板</span>
        </el-menu-item>
        <el-menu-item index="/admin/resources">
          <el-icon><FolderOpened /></el-icon>
          <span>资源管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/community">
          <el-icon><ChatDotRound /></el-icon>
          <span>社区管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/config">
          <el-icon><Setting /></el-icon>
          <span>系统配置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <span class="admin-title">{{ route.meta.title }}</span>
        <el-button link @click="onLogout">退出登录</el-button>
      </el-header>
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

function onLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/admin/login')
}
</script>

<style scoped lang="scss">
.admin-layout {
  height: 100%;
}

.admin-aside {
  background: var(--card);
  border-right: 1px solid var(--border);

  .admin-logo {
    height: 56px;
    display: flex;
    align-items: center;
    padding: 0 20px;
    font-weight: 700;
    color: var(--primary);
    border-bottom: 1px solid var(--border);
  }

  .admin-menu {
    border-right: none;
  }
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--card);
  border-bottom: 1px solid var(--border);

  .admin-title {
    font-weight: 700;
    font-size: 16px;
  }
}

.admin-main {
  background: var(--surface);
}
</style>
