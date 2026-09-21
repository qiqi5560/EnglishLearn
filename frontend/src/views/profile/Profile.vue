<template>
  <div class="profile page">
    <AppHeader title="我的" />

    <div v-loading="loading" class="page-shell profile-body">
      <template v-if="userStore.userInfo">
        <div class="profile-grid">
          <!-- ==================== 左栏：用户信息卡 ==================== -->
          <section class="user-card glass-card hover-lift peek-host">
            <PeekMascot class="user-mascot" :size="58" />

            <div
              class="avatar-wrap"
              role="button"
              :title="uploading ? '正在上传…' : '点击更换头像'"
              @click="pickAvatar"
            >
              <el-avatar :size="76" class="user-avatar" :src="avatarSrc || undefined">
                {{ userStore.userInfo.nickname?.[0]?.toUpperCase() || 'U' }}
              </el-avatar>
              <span class="avatar-camera">
                <el-icon><Camera /></el-icon>
              </span>
              <span v-if="uploading" class="avatar-mask">
                <el-icon class="is-loading"><Loading /></el-icon>
              </span>
            </div>

            <!-- 本地选图：隐藏原生 input，由头像点击触发 -->
            <input
              ref="fileInput"
              type="file"
              accept="image/*"
              class="avatar-input"
              @change="onFilePicked"
            />

            <button
              v-if="avatarSrc"
              type="button"
              class="avatar-remove"
              :disabled="uploading"
              @click="removeAvatar"
            >
              移除头像
            </button>

            <h2 class="user-nickname">{{ userStore.userInfo.nickname || '未设置昵称' }}</h2>
            <p class="user-phone text-muted">{{ maskPhone(userStore.userInfo.phone) }}</p>

            <div class="user-tags">
              <el-tag type="info" size="small" effect="plain">{{ ageGroupLabel }}</el-tag>
              <LevelTag :level="userStore.level || 'A1'" />
            </div>

            <el-button class="edit-btn" @click="openEdit">
              <el-icon><EditPen /></el-icon>
              编辑资料
            </el-button>
          </section>

          <!-- ==================== 右栏：学习导航 ==================== -->
          <section class="nav-card glass-card">
            <header class="nav-head">
              <h3>学习导航</h3>
              <p class="text-muted">方案、报表、设置与搭子都在这里</p>
            </header>

            <div class="nav-grid stagger">
              <div class="nav-item" @click="$router.push('/plan')">
                <span class="nav-icon"><el-icon><Notebook /></el-icon></span>
                <span class="nav-label">我的学习方案</span>
                <el-icon class="nav-arrow"><ArrowRight /></el-icon>
              </div>
              <div class="nav-item" @click="$router.push('/report')">
                <span class="nav-icon"><el-icon><TrendCharts /></el-icon></span>
                <span class="nav-label">学习报表</span>
                <el-icon class="nav-arrow"><ArrowRight /></el-icon>
              </div>
              <div class="nav-item" @click="$router.push('/settings')">
                <span class="nav-icon"><el-icon><Setting /></el-icon></span>
                <span class="nav-label">设置</span>
                <el-icon class="nav-arrow"><ArrowRight /></el-icon>
              </div>
              <div class="nav-item" @click="$router.push('/partners')">
                <span class="nav-icon"><el-icon><UserFilled /></el-icon></span>
                <span class="nav-label">搭子组队</span>
                <el-icon class="nav-arrow"><ArrowRight /></el-icon>
              </div>
            </div>

            <div class="nav-foot">
              <span class="text-muted">退出后需要重新登录</span>
              <el-button class="logout-btn" @click="onLogout">退出登录</el-button>
            </div>
          </section>
        </div>

        <!-- 编辑资料弹窗 -->
        <el-dialog v-model="editVisible" title="编辑资料" width="440px" align-center>
          <el-form label-position="top">
            <el-form-item label="昵称">
              <el-input v-model="editForm.nickname" maxlength="20" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="个性签名">
              <el-input
                v-model="editForm.bio"
                type="textarea"
                :rows="2"
                maxlength="100"
                show-word-limit
                placeholder="写一句话介绍自己，会展示在你的公开主页"
              />
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
import PeekMascot from '@/components/base/PeekMascot.vue'
import LevelTag from '@/components/business/LevelTag.vue'
import { useUserStore } from '@/stores/user'
import { uploadAvatar } from '@/api/modules/auth'
import { compressImage } from '@/utils/image'

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
const editForm = reactive({ nickname: '', ageGroup: 'adult', bio: '' })

// ---------------- 自定义头像 ----------------
const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const avatarSrc = computed(() => userStore.userInfo?.avatarUrl || '')

function pickAvatar() {
  if (uploading.value) return
  fileInput.value?.click()
}

/** 选中本地图片：前端压缩后上传，成功后直接用返回的用户信息刷新本地缓存 */
async function onFilePicked(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = '' // 清空，允许连续选同一张图
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  uploading.value = true
  try {
    const blob = await compressImage(file)
    const me = await uploadAvatar(
      new File([blob], 'avatar.jpg', { type: blob.type || 'image/jpeg' }),
    )
    userStore.setUserInfo(me)
    ElMessage.success('头像已更新')
  } catch (err) {
    // 网络/业务错误已由请求层统一提示，这里只补前端本地处理失败的情况
    if (err instanceof Error && !('isAxiosError' in err)) {
      ElMessage.error(err.message)
    }
  } finally {
    uploading.value = false
  }
}

/** 恢复默认头像（清空 avatarUrl 后显示昵称首字母） */
async function removeAvatar() {
  uploading.value = true
  try {
    await userStore.updateProfile({ avatarUrl: '' })
    ElMessage.success('已恢复默认头像')
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    uploading.value = false
  }
}

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
  editForm.bio = userStore.userInfo?.bio ?? ''
  editVisible.value = true
}

async function onSave() {
  if (!editForm.nickname.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  saving.value = true
  try {
    await userStore.updateProfile({
      nickname: editForm.nickname.trim(),
      ageGroup: editForm.ageGroup,
      bio: editForm.bio.trim(),
    })
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
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

.profile-body {
  min-height: 60vh;
}

// ---------------- 桌面两栏：左信息卡 + 右导航 ----------------
.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 360px) minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

// ==================== 左栏：用户信息卡 ====================
.user-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 28px 28px;
  text-align: center;
}

.user-mascot {
  margin-bottom: -6px;
}

.user-avatar {
  flex: 0 0 auto;
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(150deg, var(--primary), var(--ink));
  color: #fff;
  border: 3px solid rgba(255, 255, 255, 0.95);
  box-shadow: 0 12px 26px rgba(31, 42, 68, 0.14);
  transition: transform 0.5s $ease-spring;
}

.user-card:hover .user-avatar {
  transform: scale(1.05);
}

// ---------------- 自定义头像：点击更换 ----------------
.avatar-wrap {
  position: relative;
  display: inline-flex;
  cursor: pointer;
}

.avatar-input {
  display: none;
}

.avatar-camera {
  position: absolute;
  right: -2px;
  bottom: -2px;
  width: 26px;
  height: 26px;
  display: grid;
  place-items: center;
  font-size: 13px;
  color: #fff;
  border-radius: 50%;
  border: 2px solid #fff;
  background: linear-gradient(150deg, var(--primary), var(--ink));
  box-shadow: 0 4px 12px rgba(31, 42, 68, 0.22);
  transition: transform 0.4s $ease-spring;
}

.avatar-wrap:hover .avatar-camera {
  transform: scale(1.14);
}

.avatar-mask {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  font-size: 24px;
  color: #fff;
  border-radius: 50%;
  background: rgba(20, 26, 40, 0.42);
}

.avatar-remove {
  margin-top: 10px;
  padding: 0;
  border: none;
  background: none;
  font-size: 12px;
  color: var(--muted);
  cursor: pointer;
  transition: color 0.25s ease;

  &:hover:not(:disabled) {
    color: var(--primary);
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

.user-nickname {
  margin: 16px 0 0;
  font-size: 21px;
  font-weight: 700;
  letter-spacing: -0.02em;
  word-break: break-all;
}

.user-phone {
  margin: 6px 0 0;
  font-size: 13px;
  letter-spacing: 0.02em;
}

.user-tags {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
}

.edit-btn {
  width: 100%;
  height: 42px;
  margin-top: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: 999px;
  border: 1px solid rgba(59, 111, 224, 0.24);
  background: rgba(255, 255, 255, 0.86);
  color: var(--primary);
  font-weight: 600;
  transition:
    transform 0.35s $ease-apple,
    box-shadow 0.35s $ease-apple,
    background 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    background: #fff;
    border-color: rgba(59, 111, 224, 0.4);
    box-shadow: 0 14px 28px rgba(59, 111, 224, 0.2);
  }
}

// ==================== 右栏：学习导航 ====================
.nav-card {
  padding: 26px 28px 22px;
}

.nav-head {
  h3 {
    margin: 0;
    font-size: 20px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }

  p {
    margin: 7px 0 0;
    font-size: 13px;
  }
}

.nav-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 20px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 15px 16px;
  min-width: 0;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.62);
  cursor: pointer;
  transition:
    transform 0.35s $ease-apple,
    box-shadow 0.35s $ease-apple,
    border-color 0.3s ease,
    background 0.3s ease;

  &:hover {
    transform: translateY(-3px);
    background: #fff;
    border-color: rgba(59, 111, 224, 0.28);
    box-shadow: var(--shadow-md);

    .nav-icon {
      color: #fff;
      background: linear-gradient(150deg, var(--primary), var(--ink));
    }

    .nav-arrow {
      color: var(--primary);
      transform: translateX(4px);
    }
  }
}

.nav-icon {
  flex: 0 0 auto;
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  font-size: 18px;
  border-radius: 12px;
  color: var(--primary);
  background: rgba(59, 111, 224, 0.1);
  transition:
    background 0.35s ease,
    color 0.35s ease,
    transform 0.45s $ease-spring;
}

.nav-label {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav-arrow {
  flex: 0 0 auto;
  font-size: 14px;
  color: var(--muted);
  transition:
    transform 0.35s $ease-apple,
    color 0.3s ease;
}

.nav-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--border);
  font-size: 12.5px;
}

.logout-btn {
  height: 40px;
  padding: 0 24px;
  border-radius: 999px;
  font-weight: 600;
  transition:
    transform 0.35s $ease-apple,
    box-shadow 0.35s $ease-apple;

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }
}

@media (max-width: 900px) {
  .profile-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 18px;
  }

  .user-card {
    padding: 18px 20px 24px;
  }

  .nav-card {
    padding: 22px 20px 18px;
  }

  .nav-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>