<template>
  <div class="profile page">
    <AppHeader title="我的" />

    <div v-loading="loading" class="profile-body">
      <template v-if="userStore.userInfo">
        <!-- 用户信息卡 -->
        <el-card shadow="never" class="user-card">
          <div class="user-row">
            <el-avatar :size="56" class="user-avatar" :src="userStore.userInfo.avatarUrl || undefined">
              {{ userStore.userInfo.nickname?.[0]?.toUpperCase() || 'U' }}
            </el-avatar>
            <div class="user-meta">
              <div class="user-nickname">{{ userStore.userInfo.nickname || '未设置昵称' }}</div>
              <div class="text-muted user-phone">{{ maskPhone(userStore.userInfo.phone) }}</div>
            </div>
            <el-button size="small" @click="editVisible = true">编辑</el-button>
          </div>
          <div class="user-tags">
            <el-tag type="info" size="small" effect="plain">{{ ageGroupLabel }}</el-tag>
            <span class="text-muted">水平等级</span>
            <LevelTag :level="userStore.level || 'A1'" />
          </div>
        </el-card>

        <!-- 学习导航 -->
        <el-card shadow="never" class="menu-card">
          <div class="menu-item" @click="$router.push('/plan')">
            <span class="menu-label">📋 我的学习方案</span>
            <el-icon><ArrowRight /></el-icon>
          </div>
          <div class="menu-item" @click="$router.push('/report')">
            <span class="menu-label">📊 学习报表</span>
            <el-icon><ArrowRight /></el-icon>
          </div>
          <div class="menu-item" @click="$router.push('/settings')">
            <span class="menu-label">⚙️ 设置</span>
            <el-icon><ArrowRight /></el-icon>
          </div>
          <div class="menu-item" @click="$router.push('/partners')">
            <span class="menu-label">👥 搭子组队</span>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </el-card>

        <el-button class="logout-btn" @click="onLogout">退出登录</el-button>

        <!-- 编辑资料弹窗 -->
        <el-dialog v-model="editVisible" title="编辑资料" width="80%" align-center>
          <el-form label-position="top">
            <el-form-item label="昵称">
              <el-input v-model="editForm.nickname" maxlength="20" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="年龄段">
              <el-select v-model="editForm.ageGroup" style="width: 100%">
                <el-option v-for="g in AGE_GROUPS" :key="g.value" :label="g.label" :value="g.value" />
              </el-select>
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="editVisible = false">取消</el-button>
            <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
          </template>
        </el-dialog>
      </template>

      <el-empty v-else-if="!loading" description="未登录">
        <el-button type="primary" @click="$router.push('/login')">去登录</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
import LevelTag from '@/components/business/LevelTag.vue'
import { useUserStore } from '@/stores/user'

const AGE_GROUPS = [
  { value: 'child', label: '儿童' },
  { value: 'k12', label: 'K12 学生' },
  { value: 'adult', label: '成人' },
  { value: 'senior', label: '中老年' },
]

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const editVisible = ref(false)
const editForm = reactive({ nickname: '', ageGroup: 'adult' })

const ageGroupLabel = computed(() => {
  const v = userStore.userInfo?.ageGroup
  return AGE_GROUPS.find((g) => g.value === v)?.label ?? '未设置'
})

function maskPhone(phone?: string) {
  if (!phone || phone.length < 11) return phone ?? ''
  return `${phone.slice(0, 3)}****${phone.slice(-4)}`
}

function openEdit() {
  editForm.nickname = userStore.userInfo?.nickname ?? ''
  editForm.ageGroup = userStore.userInfo?.ageGroup ?? 'adult'
  editVisible.value = true
}

async function onSave() {
  if (!editForm.nickname.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  saving.value = true
  try {
    await userStore.updateProfile({ nickname: editForm.nickname.trim(), ageGroup: editForm.ageGroup })
    editVisible.value = false
    ElMessage.success('保存成功')
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    saving.value = false
  }
}

function onLogout() {
  ElMessageBox.confirm('确定退出当前账号吗？', '提示', {
    confirmButtonText: '退出',
    cancelButtonText: '取消',
  })
    .then(() => {
      userStore.logout()
      router.replace('/login')
    })
    .catch(() => {})
}

onMounted(async () => {
  if (!userStore.userInfo) {
    loading.value = true
    try {
      await userStore.fetchMe()
    } catch {
      /* 错误提示已由请求层统一弹出 */
    } finally {
      loading.value = false
    }
  }
})
</script>

<style scoped lang="scss">
.profile-body {
  padding: 16px;
  min-height: 60vh;
}

.user-card {
  border-radius: var(--radius-md);

  .user-row {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .user-avatar {
    background: var(--primary);
    color: #fff;
    flex-shrink: 0;
  }

  .user-meta {
    flex: 1;
    min-width: 0;

    .user-nickname {
      font-size: 18px;
      font-weight: 700;
    }

    .user-phone {
      font-size: 12px;
    }
  }

  .user-tags {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 12px;
  }
}

.menu-card {
  border-radius: var(--radius-md);
  margin-top: 12px;

  .menu-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 0;
    cursor: pointer;

    & + .menu-item {
      border-top: 1px solid var(--border);
    }
  }
}

.logout-btn {
  width: 100%;
  margin-top: 16px;
}
</style>
