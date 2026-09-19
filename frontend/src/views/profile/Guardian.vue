<template>
  <div class="guardian page">
    <AppHeader title="家长管控" back />

    <div class="page-shell guardian-body">
      <header class="guardian-head">
        <h2 class="guardian-title">守护孩子的练习节奏</h2>
        <p class="guardian-sub text-muted">绑定监护人后，家长可以查看练习报表并设置使用时长与提醒。</p>
      </header>

      <!-- ==================== 已绑定 ==================== -->
      <div v-if="userStore.userInfo?.guardianId" class="guardian-grid">
        <section class="status-card glass-card hover-lift">
          <span class="status-icon"><el-icon><CircleCheckFilled /></el-icon></span>
          <h3>已绑定监护人</h3>
          <p class="text-muted">监护授权已生效，可在家长端查看孩子的学习情况。</p>
          <el-button class="unbind-btn" @click="unbindVisible = true">解除绑定</el-button>
        </section>

        <section class="scope-card glass-card hover-lift">
          <header class="card-head">
            <h3>监护权限范围</h3>
            <p class="text-muted">以下能力在绑定后对监护人开放</p>
          </header>
          <ul class="scope-list stagger">
            <li>
              <span class="scope-icon"><el-icon><DataAnalysis /></el-icon></span>
              <div>
                <strong>查看练习报表</strong>
                <span class="text-muted">了解练习次数、时长与评分变化</span>
              </div>
            </li>
            <li>
              <span class="scope-icon"><el-icon><Timer /></el-icon></span>
              <div>
                <strong>设置使用时长</strong>
                <span class="text-muted">限制每日练习时间，避免过度使用</span>
              </div>
            </li>
            <li>
              <span class="scope-icon"><el-icon><Bell /></el-icon></span>
              <div>
                <strong>接收练习提醒</strong>
                <span class="text-muted">及时掌握孩子的学习进度</span>
              </div>
            </li>
          </ul>
        </section>
      </div>

      <!-- ==================== 未绑定：绑定流程 ==================== -->
      <div v-else class="guardian-grid">
        <section class="guide-card glass-card hover-lift">
          <header class="card-head">
            <h3>绑定前请准备</h3>
            <p class="text-muted">该账号尚未绑定监护人</p>
          </header>
          <ol class="guide-steps stagger">
            <li>
              <span class="step-num">1</span>
              <div>
                <strong>填写监护人手机号</strong>
                <span class="text-muted">需为监护人本人常用手机号</span>
              </div>
            </li>
            <li>
              <span class="step-num">2</span>
              <div>
                <strong>获取并填写验证码</strong>
                <span class="text-muted">演示环境验证码固定为 123456</span>
              </div>
            </li>
            <li>
              <span class="step-num">3</span>
              <div>
                <strong>完成授权绑定</strong>
                <span class="text-muted">绑定后家长端即可查看报表</span>
              </div>
            </li>
          </ol>
        </section>

        <section class="form-card glass-card hover-lift sheen">
          <header class="card-head">
            <h3>绑定监护人</h3>
            <p class="text-muted">输入监护人手机号并完成验证码校验</p>
          </header>

          <el-form class="bind-form" label-position="top">
            <el-form-item label="监护人手机号">
              <el-input v-model="form.guardianPhone" placeholder="请输入监护人手机号" maxlength="11" />
            </el-form-item>
            <el-form-item label="验证码">
              <el-input v-model="form.code" placeholder="验证码" maxlength="6">
                <template #append>
                  <el-button :disabled="sending || countdown > 0" @click="onSendCode">
                    {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
                  </el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-form>

          <el-button type="primary" class="bind-btn" :loading="binding" @click="onBind">立即绑定</el-button>
        </section>
      </div>
    </div>

    <!-- 解除绑定确认 -->
    <el-dialog v-model="unbindVisible" title="解除绑定" width="440px" align-center>
      <p>确定要解除与监护人的绑定吗？解除后监护管理功能将关闭。</p>
      <template #footer>
        <el-button @click="unbindVisible = false">取消</el-button>
        <el-button type="danger" :loading="unbinding" @click="onUnbind">解除绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onUnmounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const sending = ref(false)
const binding = ref(false)
const unbinding = ref(false)
const unbindVisible = ref(false)
const countdown = ref(0)
const form = reactive({ guardianPhone: '', code: '' })

let timer: ReturnType<typeof setInterval> | undefined

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

async function onSendCode() {
  if (sending.value || countdown.value > 0) return
  if (!/^1\d{10}$/.test(form.guardianPhone)) {
    ElMessage.warning('请输入正确的 11 位手机号')
    return
  }
  sending.value = true
  try {
    await userStore.sendCode(form.guardianPhone)
    ElMessage.success('验证码已发送（演示固定为 123456）')
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0 && timer) {
        clearInterval(timer)
        timer = undefined
      }
    }, 1000)
  } finally {
    sending.value = false
  }
}

async function onBind() {
  if (!/^1\d{10}$/.test(form.guardianPhone)) {
    ElMessage.warning('请输入正确的 11 位手机号')
    return
  }
  if (!form.code) {
    ElMessage.warning('请输入监护人验证码')
    return
  }
  binding.value = true
  try {
    await userStore.bindGuardian({ guardianPhone: form.guardianPhone, code: form.code })
    ElMessage.success('绑定成功')
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    binding.value = false
  }
}

async function onUnbind() {
  unbinding.value = true
  try {
    // 演示环境暂未提供解绑接口，仅提示说明
    ElMessage.info('演示环境暂不支持解除绑定，请与监护人沟通处理')
    unbindVisible.value = false
  } finally {
    unbinding.value = false
  }
}
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

.guardian-body {
  min-height: 60vh;
}

.guardian-head {
  margin-bottom: 24px;
}

.guardian-title {
  margin: 0;
  font-size: clamp(26px, 2.4vw, 34px);
  font-weight: 700;
  letter-spacing: -0.03em;
}

.guardian-sub {
  margin: 8px 0 0;
  font-size: 14px;
}

// ---------------- 桌面两栏 ----------------
.guardian-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.card-head {
  margin-bottom: 18px;

  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }

  p {
    margin: 6px 0 0;
    font-size: 12.5px;
  }
}

// ==================== 已绑定：状态卡 ====================
.status-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 34px 28px 30px;
  text-align: center;

  h3 {
    margin: 18px 0 0;
    font-size: 20px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }

  p {
    margin: 10px 0 0;
    max-width: 380px;
    font-size: 13.5px;
    line-height: 1.7;
  }
}

.status-icon {
  width: 64px;
  height: 64px;
  display: grid;
  place-items: center;
  font-size: 32px;
  border-radius: 20px;
  color: var(--success);
  background: rgba(34, 160, 107, 0.12);
  transition: transform 0.5s $ease-spring;
}

.status-card:hover .status-icon {
  transform: scale(1.08) rotate(-5deg);
}

.unbind-btn {
  height: 42px;
  min-width: 180px;
  margin-top: 26px;
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

.scope-card {
  padding: 24px 26px 26px;
}

.scope-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;

  li {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 14px 16px;
    border: 1px solid var(--border);
    border-radius: var(--radius-md);
    background: rgba(255, 255, 255, 0.6);
    transition:
      transform 0.35s $ease-apple,
      border-color 0.3s ease,
      box-shadow 0.35s $ease-apple;

    &:hover {
      transform: translateY(-3px);
      border-color: rgba(59, 111, 224, 0.28);
      box-shadow: var(--shadow-md);
    }

    div {
      display: flex;
      flex-direction: column;
      gap: 3px;
      min-width: 0;
    }

    strong {
      font-size: 14px;
      font-weight: 650;
    }

    span {
      font-size: 12.5px;
    }
  }
}

.scope-icon {
  flex: 0 0 auto;
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  font-size: 19px;
  border-radius: 13px;
  color: var(--primary);
  background: rgba(59, 111, 224, 0.1);
}

// ==================== 未绑定：说明 + 表单 ====================
.guide-card,
.form-card {
  padding: 24px 26px 26px;
}

.guide-steps {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;

  li {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 14px 16px;
    border-radius: var(--radius-md);
    background: rgba(59, 111, 224, 0.05);
    transition: transform 0.35s $ease-apple;

    &:hover {
      transform: translateX(4px);
    }

    div {
      display: flex;
      flex-direction: column;
      gap: 3px;
      min-width: 0;
    }

    strong {
      font-size: 14px;
      font-weight: 650;
    }

    span {
      font-size: 12.5px;
    }
  }
}

.step-num {
  flex: 0 0 auto;
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: 11px;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(150deg, var(--primary), var(--ink));
  box-shadow: 0 8px 18px rgba(59, 111, 224, 0.26);
}

.bind-form {
  max-width: 460px;
}

.bind-btn {
  height: 44px;
  min-width: 180px;
  margin-top: 6px;
  border-radius: 999px;
  font-weight: 600;
  transition:
    transform 0.35s $ease-apple,
    box-shadow 0.35s $ease-apple;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 14px 28px rgba(59, 111, 224, 0.3);
  }
}

@media (max-width: 900px) {
  .guardian-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 18px;
  }

  .status-card {
    padding: 26px 20px 24px;
  }

  .guide-card,
  .form-card,
  .scope-card {
    padding: 20px 18px 22px;
  }
}
</style>