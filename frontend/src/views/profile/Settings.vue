<template>
  <div class="settings">
    <AppHeader title="设置" back />

    <div class="settings-body">
      <div class="section-title">全年龄段学习模式（F009）</div>
      <el-card shadow="never">
        <el-radio-group v-model="ageMode" class="mode-group" @change="onModeChange">
          <el-radio value="normal">普通模式</el-radio>
          <el-radio value="child">少儿模式</el-radio>
          <el-radio value="senior">中老年模式</el-radio>
        </el-radio-group>
        <p class="text-muted mode-hint">
          {{
            ageMode === 'senior'
              ? '中老年模式：大字体、高对比度、慢速语料'
              : ageMode === 'child'
                ? '少儿模式：游戏化趣味内容与家长管控'
                : '普通模式：标准界面与语速'
          }}
        </p>
      </el-card>

      <div class="section-title">偏好设置</div>
      <el-card shadow="never">
        <div class="setting-row">
          <span>默认语速</span>
          <el-slider v-model="speed" :min="0.5" :max="2" :step="0.25" style="width: 160px" />
        </div>
        <div class="setting-row">
          <span>消息提醒</span>
          <el-switch v-model="notify" />
        </div>
        <div class="setting-row">
          <span>双语字幕</span>
          <el-switch v-model="subtitle" />
        </div>
      </el-card>

      <div class="section-title">账号安全</div>
      <el-card shadow="never">
        <div class="setting-row">
          <span>登录手机号</span>
          <span class="text-muted">{{ maskPhone(userStore.userInfo?.phone) }}</span>
        </div>
        <div class="setting-row">
          <span>登录密码</span>
          <el-button size="small" text type="primary" @click="pwdVisible = true">修改</el-button>
        </div>
      </el-card>

      <!-- 修改密码 -->
      <el-dialog v-model="pwdVisible" title="修改登录密码" width="80%" align-center>
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
.settings-body {
  padding: 16px;
}

.mode-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mode-hint {
  margin: 12px 0 0;
  font-size: 12px;
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid var(--border);

  &:last-child {
    border-bottom: none;
  }
}
</style>
