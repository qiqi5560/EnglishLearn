<template>
  <div class="settings page">
    <AppHeader title="设置" back />

    <div class="page-shell settings-body">
      <header class="settings-head">
        <h2 class="settings-title">个性化你的学习体验</h2>
        <p class="settings-sub text-muted">切换学习模式、调整练习偏好，并管理账号安全。</p>
      </header>

      <div class="settings-grid">
        <!-- ==================== 学习模式 ==================== -->
        <section class="setting-card glass-card hover-lift sheen">
          <header class="card-head">
            <span class="card-icon"><el-icon><MagicStick /></el-icon></span>
            <div>
              <h3>全年龄段学习模式</h3>
              <p class="text-muted">F009 · 按年龄切换到更合适的界面与语速</p>
            </div>
          </header>

          <el-radio-group v-model="ageMode" class="mode-group" @change="onModeChange">
            <el-radio value="normal">普通模式</el-radio>
            <el-radio value="child">少儿模式</el-radio>
            <el-radio value="senior">中老年模式</el-radio>
          </el-radio-group>

          <p class="mode-hint">
            <el-icon><InfoFilled /></el-icon>
            <span>
              {{
                ageMode === 'senior'
                  ? '中老年模式：大字体、高对比度、慢速语料'
                  : ageMode === 'child'
                    ? '少儿模式：游戏化趣味内容与家长管控'
                    : '普通模式：标准界面与语速'
              }}
            </span>
          </p>
        </section>

        <!-- ==================== 偏好设置 & 账号安全 ==================== -->
        <div class="settings-col">
          <section class="setting-card glass-card hover-lift">
            <header class="card-head">
              <span class="card-icon"><el-icon><Headset /></el-icon></span>
              <div>
                <h3>偏好设置</h3>
                <p class="text-muted">练习时的默认播放与提醒行为</p>
              </div>
            </header>

            <div class="setting-list stagger">
              <div class="setting-row">
                <span class="row-label">默认语速</span>
                <el-slider v-model="speed" :min="0.5" :max="2" :step="0.25" style="width: 160px" />
              </div>
              <div class="setting-row">
                <span class="row-label">消息提醒</span>
                <el-switch v-model="notify" />
              </div>
              <div class="setting-row">
                <span class="row-label">双语字幕</span>
                <el-switch v-model="subtitle" />
              </div>
            </div>
          </section>

          <section class="setting-card glass-card hover-lift">
            <header class="card-head">
              <span class="card-icon"><el-icon><Lock /></el-icon></span>
              <div>
                <h3>账号安全</h3>
                <p class="text-muted">登录方式与密码管理</p>
              </div>
            </header>

            <div class="setting-list stagger">
              <div class="setting-row">
                <span class="row-label">登录手机号</span>
                <span class="text-muted row-value">{{ maskPhone(userStore.userInfo?.phone) }}</span>
              </div>
              <div class="setting-row">
                <span class="row-label">登录密码</span>
                <el-button size="small" text type="primary" @click="pwdVisible = true">修改</el-button>
              </div>
            </div>
          </section>
        </div>
      </div>

      <!-- 修改密码 -->
      <el-dialog v-model="pwdVisible" title="修改登录密码" width="440px" align-center>
        <el-form label-position="top">
          <el-form-item label="原密码">
            <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少 6 位" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="pwdVisible = false">取消</el-button>
          <el-button type="primary" :loading="savingPwd" @click="onSavePassword">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const MODE_TO_AGE_GROUP: Record<string, string> = { normal: 'adult', child: 'child', senior: 'senior' }

const appStore = useAppStore()
const userStore = useUserStore()

const accountAge = userStore.userInfo?.ageGroup ?? 'adult'
const ageMode = ref(accountAge === 'child' ? 'child' : accountAge === 'senior' ? 'senior' : 'normal')
const speed = ref(1)
const notify = ref(true)
const subtitle = ref(true)
const savingPwd = ref(false)
const pwdVisible = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '' })

watch(ageMode, (val) => {
  appStore.setAgeMode(val as 'normal' | 'child' | 'senior')
})

function onModeChange(val: string | number | boolean | undefined) {
  const mode = (val as 'normal' | 'child' | 'senior') || 'normal'
  appStore.setAgeMode(mode)
  userStore
    .updateProfile({ ageGroup: MODE_TO_AGE_GROUP[mode] })
    .then(() => ElMessage.success('模式已切换并保存到账号'))
    .catch(() => {})
}

function maskPhone(phone?: string) {
  if (!phone || phone.length < 11) return phone ?? ''
  return `${phone.slice(0, 3)}****${phone.slice(-4)}`
}

async function onSavePassword() {
  if (pwdForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  savingPwd.value = true
  try {
    await userStore.updatePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    pwdVisible.value = false
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    ElMessage.success('密码修改成功')
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    savingPwd.value = false
  }
}
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);

.settings-head {
  margin-bottom: 24px;
}

.settings-title {
  margin: 0;
  font-size: clamp(26px, 2.4vw, 34px);
  font-weight: 700;
  letter-spacing: -0.03em;
}

.settings-sub {
  margin: 8px 0 0;
  font-size: 14px;
}

// ---------------- 桌面两栏 ----------------
.settings-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.settings-col {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.setting-card {
  padding: 24px 26px 26px;
}

.card-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;

  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }

  p {
    margin: 5px 0 0;
    font-size: 12.5px;
  }
}

.card-icon {
  flex: 0 0 auto;
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  font-size: 20px;
  border-radius: 13px;
  color: var(--primary);
  background: rgba(59, 111, 224, 0.1);
  transition:
    background 0.35s ease,
    color 0.35s ease,
    transform 0.45s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.setting-card:hover .card-icon {
  color: #fff;
  background: linear-gradient(150deg, var(--primary), var(--ink));
  transform: rotate(-6deg) scale(1.06);
}

// ---------------- 学习模式：分段控件 ----------------
.mode-group {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  padding: 6px;
  border-radius: 16px;
  background: rgba(59, 111, 224, 0.08);

  :deep(.el-radio) {
    position: relative;
    height: 62px;
    margin: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 12px;
    transition:
      background 0.35s $ease-apple,
      box-shadow 0.35s ease,
      color 0.3s ease;

    &:hover {
      background: rgba(255, 255, 255, 0.7);
    }

    &.is-checked {
      background: #fff;
      box-shadow: 0 4px 12px rgba(31, 42, 68, 0.1);
    }
  }

  // 隐藏原生圆点，保留可聚焦能力（键盘可达）
  :deep(.el-radio__input) {
    position: absolute;
    width: 0;
    height: 0;
    opacity: 0;
    pointer-events: none;
  }

  :deep(.el-radio:focus-within) {
    box-shadow: 0 0 0 3px rgba(59, 111, 224, 0.18);
  }

  :deep(.el-radio__label) {
    padding: 0;
    font-size: 14px;
    font-weight: 600;
    color: var(--muted);
  }

  :deep(.el-radio.is-checked .el-radio__label) {
    color: var(--primary);
  }
}

.mode-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 16px 0 0;
  padding: 12px 16px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--muted);
  border-radius: var(--radius-md);
  background: rgba(59, 111, 224, 0.06);

  .el-icon {
    flex: 0 0 auto;
    color: var(--primary);
  }
}

// ---------------- 设置行 ----------------
.setting-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  min-height: 52px;
  padding: 8px 14px;
  border-radius: var(--radius-md);
  transition:
    background 0.3s ease,
    transform 0.35s $ease-apple;

  &:hover {
    background: rgba(59, 111, 224, 0.06);
    transform: translateX(3px);
  }
}

.row-label {
  font-size: 14px;
  font-weight: 600;
}

.row-value {
  font-size: 13px;
  letter-spacing: 0.02em;
}

@media (max-width: 900px) {
  .settings-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 18px;
  }

  .setting-card {
    padding: 20px 18px 22px;
  }

  .mode-group {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>