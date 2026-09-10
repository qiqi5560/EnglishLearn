<template>
  <div class="login-page">
    <div class="login-brand">
      <div class="brand-logo">🎙️</div>
      <h1>英语口语训练系统</h1>
      <p class="text-muted">AI 陪练 · 随时随地自信开口说英语</p>
    </div>

    <el-card class="login-card" shadow="never">
      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane label="验证码登录" name="sms">
          <el-form label-position="top" @submit.prevent>
            <el-form-item>
              <el-input v-model="smsForm.phone" placeholder="请输入手机号" maxlength="11" clearable />
            </el-form-item>
            <el-form-item>
              <el-input v-model="smsForm.code" placeholder="短信验证码" maxlength="6">
                <template #append>
                  <el-button :disabled="codeSending || countdown > 0" class="code-btn" @click="onSendCode">
                    {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
                  </el-button>
                </template>
              </el-input>
            </el-form-item>
            <el-checkbox v-model="agreed">我已阅读并同意《用户协议》与《隐私政策》</el-checkbox>
            <el-button type="primary" class="login-btn" :loading="submitting" @click="onSmsLogin">
              登录 / 注册
            </el-button>
            <p class="form-hint text-muted">首次使用将自动注册并登录</p>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="密码登录" name="password">
          <el-form label-position="top" @submit.prevent>
            <template v-if="!registerMode">
              <el-form-item label="手机号">
                <el-input v-model="pwdForm.phone" placeholder="请输入手机号" maxlength="11" clearable />
              </el-form-item>
              <el-form-item label="密码">
                <el-input v-model="pwdForm.password" type="password" show-password placeholder="请输入密码" />
              </el-form-item>
              <el-button type="primary" class="login-btn" :loading="submitting" @click="onPasswordLogin">
                登 录
              </el-button>
              <div class="form-tip">
                <span class="text-muted">还没有账号？</span>
                <el-link type="primary" :disabled="submitting" @click="registerMode = true">注册新账号</el-link>
              </div>
            </template>
            <template v-else>
              <el-form-item label="昵称">
                <el-input v-model="regForm.nickname" placeholder="给自己起个昵称" maxlength="20" clearable />
              </el-form-item>
              <el-form-item label="手机号">
                <el-input v-model="regForm.phone" placeholder="请输入手机号" maxlength="11" clearable />
              </el-form-item>
              <el-form-item label="密码">
                <el-input v-model="regForm.password" type="password" show-password placeholder="至少 6 位" />
              </el-form-item>
              <el-button type="primary" class="login-btn" :loading="submitting" @click="onRegister">
                注册并登录
              </el-button>
              <div class="form-tip">
                <el-link type="primary" :disabled="submitting" @click="registerMode = false">已有账号？直接登录</el-link>
              </div>
            </template>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <p class="guardian-hint text-muted">未成年人账号需由监护人授权绑定</p>
  </div>
</template>

<script setup lang="ts">
import { onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('sms')
const agreed = ref(false)
const submitting = ref(false)
const registerMode = ref(false)
const codeSending = ref(false)
const countdown = ref(0)

const smsForm = reactive({ phone: '', code: '' })
const pwdForm = reactive({ phone: '', password: '' })
const regForm = reactive({ nickname: '', phone: '', password: '' })

let timer: ReturnType<typeof setInterval> | undefined

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

function validPhone(phone: string) {
  return /^1\d{10}$/.test(phone)
}

function gotoAfterLogin() {
  router.push((route.query.redirect as string) || '/home')
}

async function onSendCode() {
  if (codeSending.value || countdown.value > 0) return
  if (!validPhone(smsForm.phone)) {
    ElMessage.warning('请输入正确的 11 位手机号')
    return
  }
  codeSending.value = true
  try {
    await userStore.sendCode(smsForm.phone)
    ElMessage.success('验证码已发送（演示环境固定为 123456）')
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0 && timer) {
        clearInterval(timer)
        timer = undefined
      }
    }, 1000)
  } finally {
    codeSending.value = false
  }
}

async function onSmsLogin() {
  if (!validPhone(smsForm.phone)) {
    ElMessage.warning('请输入正确的 11 位手机号')
    return
  }
  if (!smsForm.code) {
    ElMessage.warning('请输入验证码')
    return
  }
  if (!agreed.value) {
    ElMessage.warning('请先阅读并同意《用户协议》与《隐私政策》')
    return
  }
  submitting.value = true
  try {
    await userStore.smsLogin({ phone: smsForm.phone, code: smsForm.code })
    ElMessage.success('登录成功')
    gotoAfterLogin()
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    submitting.value = false
  }
}

async function onPasswordLogin() {
  if (!validPhone(pwdForm.phone)) {
    ElMessage.warning('请输入正确的 11 位手机号')
    return
  }
  if (!pwdForm.password) {
    ElMessage.warning('请输入密码')
    return
  }
  submitting.value = true
  try {
    await userStore.passwordLogin({ phone: pwdForm.phone, password: pwdForm.password })
    ElMessage.success('登录成功')
    gotoAfterLogin()
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    submitting.value = false
  }
}

async function onRegister() {
  if (!validPhone(regForm.phone)) {
    ElMessage.warning('请输入正确的 11 位手机号')
    return
  }
  if (!regForm.password || regForm.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  submitting.value = true
  try {
    await userStore.register({
      phone: regForm.phone,
      password: regForm.password,
      nickname: regForm.nickname || undefined,
    })
    ElMessage.success('注册成功，已自动登录')
    gotoAfterLogin()
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
  background: linear-gradient(180deg, #eef2fd 0%, var(--surface) 60%);
}

.login-brand {
  text-align: center;
  margin-bottom: 28px;

  .brand-logo {
    font-size: 48px;
  }

  h1 {
    margin: 8px 0 4px;
    font-size: 24px;
  }

  p {
    margin: 0;
  }
}

.login-card {
  width: 100%;
  max-width: 360px;
  border-radius: var(--radius-lg);
}

.login-btn {
  width: 100%;
  margin-top: 8px;
}

.code-btn {
  width: 96px;
}

.form-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  margin-top: 12px;
  font-size: 13px;
}

.form-hint {
  margin: 8px 0 0;
  text-align: center;
  font-size: 12px;
}

.guardian-hint {
  margin-top: 20px;
  font-size: 12px;
}
</style>
