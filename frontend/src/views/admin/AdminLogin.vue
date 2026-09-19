<template>
  <div class="login-page">
    <!-- 背景光斑 -->
    <span class="bg-orb orb-1" aria-hidden="true"></span>
    <span class="bg-orb orb-2" aria-hidden="true"></span>
    <span class="bg-orb orb-3" aria-hidden="true"></span>

    <div class="login-shell">
      <!-- ==================== 左侧：后台能力概览 ==================== -->
      <section class="quote-panel">
        <!-- 沿边框滚动的风车 -->
        <div class="windmill" aria-hidden="true">
          <div class="blades">
            <span class="blade" style="--r: 0deg"></span>
            <span class="blade" style="--r: 60deg"></span>
            <span class="blade" style="--r: 120deg"></span>
            <span class="blade" style="--r: 180deg"></span>
            <span class="blade" style="--r: 240deg"></span>
            <span class="blade" style="--r: 300deg"></span>
          </div>
          <span class="hub"></span>
        </div>

        <div class="panel-glow" aria-hidden="true"></div>

        <div class="quote-inner">
          <div class="brand-row">
            <div class="brand-mark-lg">🛡️</div>
            <div class="brand-text">
              <h2>英语口语训练系统</h2>
              <p>Admin Console</p>
            </div>
            <button class="back-link" @click="router.push('/welcome')">
              <el-icon><Back /></el-icon>
              返回首页
            </button>
          </div>

          <h1 class="quote-title">
            运营、治理与监控，<br />
            尽在<em>一块面板</em>
          </h1>

          <p class="quote-desc">
            这里是管理员专属控制台，登录后可管理全站用户、内容与算力资源：
          </p>

          <div class="movie-quotes">
            <blockquote v-for="f in features" :key="f.title" class="quote-card">
              <span class="quote-mark" aria-hidden="true">{{ f.icon }}</span>
              <p class="quote-en">{{ f.title }}</p>
              <p class="quote-zh">{{ f.desc }}</p>
            </blockquote>
          </div>

          <div class="motto-row">
            <span v-for="m in mottos" :key="m">{{ m }}</span>
          </div>
        </div>
      </section>

      <!-- ==================== 右侧：管理员登录 ==================== -->
      <section class="auth-panel">
        <div class="auth-stack">
          <!-- 探头的吉祥物：鼠标移入登录框即探头 -->
          <div class="mascot-stage" aria-hidden="true">
            <div class="mascot">
              <span class="antenna"><i></i></span>
              <span class="ear ear-l"></span>
              <span class="ear ear-r"></span>
              <div class="head">
                <span class="eye eye-l"><i></i></span>
                <span class="eye eye-r"><i></i></span>
                <span class="blush blush-l"></span>
                <span class="blush blush-r"></span>
                <span class="mouth"></span>
              </div>
            </div>
          </div>

          <el-card class="auth-card" shadow="never">
            <div class="card-head">
              <h3>管理后台登录</h3>
              <p>请使用管理员账号进入控制台</p>
            </div>

            <el-form label-position="top" @submit.prevent>
              <el-form-item label="管理员手机号">
                <el-input v-model="form.phone" placeholder="请输入管理员手机号" maxlength="11" clearable />
              </el-form-item>
              <el-form-item label="密码">
                <el-input
                  v-model="form.password"
                  type="password"
                  placeholder="请输入密码"
                  show-password
                  @keyup.enter="onLogin"
                />
              </el-form-item>
              <el-button type="primary" class="login-btn" :loading="loading" @click="onLogin">登 录</el-button>
            </el-form>

            <p class="form-hint text-muted">演示账号 13900000000 / admin123</p>

            <div class="switch-row">
              <el-button class="switch-btn" :disabled="loading" @click="router.push('/login')">
                <el-icon><Switch /></el-icon>
                切换为普通用户登录
              </el-button>
            </div>
          </el-card>
        </div>

        <p class="guardian-hint text-muted">管理后台操作全程留痕，可在「操作日志」中追溯</p>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({ phone: '', password: '' })

/** 后台能力概览（替代用户端的电影台词，保持版式一致但内容完全不同） */
const features = [
  { icon: '📊', title: '用户使用报表', desc: '注册与活跃明细精确到年月日时分，近 12 个月增长一目了然。' },
  { icon: '📚', title: '内容运营', desc: '名句素材与对话场景随时增删改，保存后对全量用户立即生效。' },
  { icon: '🛡️', title: '社区治理', desc: '删帖删评论，对恶意发帖用户处以 1 ~ 365 天禁言处罚。' },
  { icon: '⚙️', title: '算力与系统', desc: 'LLM 调用时延、成功率与运行状态实时监控，操作全程留痕。' },
]

/** 后台标语 */
const mottos = ['管理员专属', '操作可追溯', '改动即生效', '数据驱动运营']

async function onLogin() {
  if (!/^1\d{10}$/.test(form.phone)) {
    ElMessage.warning('请输入正确的 11 位手机号')
    return
  }
  if (!form.password) {
    ElMessage.warning('请输入密码')
    return
  }
  loading.value = true
  try {
    await userStore.adminLogin({ phone: form.phone, password: form.password })
    ElMessage.success('管理员登录成功')
    router.push('/admin/dashboard')
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

// 单屏铺满：整页不出现纵向滚动条
.login-page {
  position: relative;
  height: 100vh;
  height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(16px, 2.6vh, 36px) clamp(18px, 3.4vw, 56px);
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'PingFang SC',
    'Microsoft YaHei', sans-serif;
  background:
    radial-gradient(1200px 620px at 10% -12%, #e6efff 0%, rgba(230, 239, 255, 0) 62%),
    radial-gradient(900px 520px at 102% -6%, #eaf2ff 0%, rgba(234, 242, 255, 0) 58%),
    linear-gradient(168deg, #f8fbff 0%, #eff4fd 46%, #e5edfb 100%);
}

// ---------------- 背景光斑 ----------------
.bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(72px);
  opacity: 0.55;
  pointer-events: none;
  animation: orbFloat 16s ease-in-out infinite;
}

.orb-1 {
  width: 440px;
  height: 440px;
  background: #b9d2ff;
  top: -150px;
  left: -120px;
}

.orb-2 {
  width: 380px;
  height: 380px;
  background: #cde0ff;
  bottom: -160px;
  right: -90px;
  animation-delay: -5s;
}

.orb-3 {
  width: 300px;
  height: 300px;
  background: #dbe8ff;
  top: 38%;
  left: 45%;
  animation-delay: -10s;
}

@keyframes orbFloat {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(24px, -28px, 0) scale(1.07);
  }
}

// ---------------- 两栏：左宽右窄，等高铺满 ----------------
.login-shell {
  position: relative;
  z-index: 2;
  width: min(1360px, 100%);
  height: 100%;
  display: flex;
  align-items: stretch;
  gap: clamp(18px, 2.4vw, 38px);
}

// ==================== 左侧：后台能力概览 ====================
.quote-panel {
  position: relative;
  flex: 0 0 52%;
  display: flex;
  flex-direction: column;
  padding: clamp(22px, 3.2vh, 42px) clamp(24px, 2.6vw, 44px);
  border-radius: 32px;
  border: 1px solid rgba(59, 111, 224, 0.16);
  background: rgba(255, 255, 255, 0.58);
  box-shadow: 0 26px 64px rgba(43, 79, 158, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.76);
  backdrop-filter: blur(20px) saturate(150%);
  transition: box-shadow 0.6s $ease-apple, transform 0.6s $ease-apple;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 34px 78px rgba(43, 79, 158, 0.16), inset 0 1px 0 rgba(255, 255, 255, 0.85);
  }
}

.panel-glow {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  overflow: hidden;
  pointer-events: none;

  &::after {
    content: '';
    position: absolute;
    width: 420px;
    height: 420px;
    right: -160px;
    top: -170px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(120, 165, 245, 0.22), rgba(120, 165, 245, 0) 70%);
  }
}

.quote-inner {
  position: relative;
  z-index: 2;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 14px;

  .brand-mark-lg {
    width: 52px;
    height: 52px;
    flex: 0 0 auto;
    display: grid;
    place-items: center;
    font-size: 25px;
    border-radius: 17px;
    background: linear-gradient(150deg, #ffffff, #dfe9fd);
    box-shadow: 0 10px 22px rgba(43, 79, 158, 0.16), inset 0 1px 0 #fff;
    transition: transform 0.5s $ease-spring;
  }

  &:hover .brand-mark-lg {
    transform: rotate(-8deg) scale(1.08);
  }

  .brand-text {
    min-width: 0;

    h2 {
      margin: 0;
      font-size: 17px;
      font-weight: 700;
      letter-spacing: -0.01em;
    }

    p {
      margin: 2px 0 0;
      font-size: 12px;
      color: var(--muted);
      letter-spacing: 0.08em;
      text-transform: uppercase;
    }
  }
}

.back-link {
  margin-left: auto;
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 8px 14px;
  border-radius: 12px;
  border: 1px solid rgba(59, 111, 224, 0.18);
  background: rgba(255, 255, 255, 0.85);
  font-size: 12.5px;
  font-weight: 600;
  color: var(--primary);
  cursor: pointer;
  transition: transform 0.35s $ease-apple, box-shadow 0.35s $ease-apple, background 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    background: #fff;
    box-shadow: 0 10px 22px rgba(59, 111, 224, 0.2);
  }
}

.quote-title {
  margin: clamp(14px, 2.6vh, 30px) 0 0;
  font-size: clamp(25px, 2.3vw, 38px);
  line-height: 1.16;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: #1b2745;

  em {
    font-style: normal;
    background: linear-gradient(96deg, #5b8bf0, #2f5bb3);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.quote-desc {
  margin: clamp(10px, 1.5vh, 16px) 0 0;
  font-size: 14px;
  line-height: 1.7;
  color: #66708b;
}

// ---------------- 能力卡片 ----------------
.movie-quotes {
  margin-top: clamp(14px, 2.2vh, 26px);
  display: flex;
  flex-direction: column;
  gap: clamp(8px, 1.2vh, 12px);
}

.quote-card {
  position: relative;
  margin: 0;
  padding: clamp(11px, 1.5vh, 16px) 18px clamp(11px, 1.5vh, 16px) 52px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.92);
  box-shadow: 0 10px 26px rgba(43, 79, 158, 0.07), inset 0 1px 0 #fff;
  transition: transform 0.45s $ease-apple, box-shadow 0.45s $ease-apple, background 0.4s ease;

  &:hover {
    transform: translateX(8px);
    background: #fff;
    box-shadow: 0 18px 38px rgba(43, 79, 158, 0.16), inset 0 1px 0 #fff;

    .quote-mark {
      transform: rotate(-8deg) scale(1.14);
      opacity: 1;
    }
  }

  .quote-mark {
    position: absolute;
    left: 15px;
    top: 50%;
    margin-top: -15px;
    font-size: 26px;
    line-height: 1;
    opacity: 0.72;
    transition: transform 0.5s $ease-spring, opacity 0.3s ease;
  }

  .quote-en {
    margin: 0;
    font-size: clamp(13px, 1.05vw, 15px);
    font-weight: 700;
    line-height: 1.45;
    color: #25314f;
  }

  .quote-zh {
    margin: 4px 0 0;
    font-size: 12.5px;
    line-height: 1.55;
    color: #66708b;
  }
}

// ---------------- 后台标语 ----------------
.motto-row {
  margin-top: auto;
  padding-top: clamp(12px, 2vh, 22px);
  display: flex;
  flex-wrap: wrap;
  gap: 9px;

  span {
    padding: 7px 15px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 600;
    color: var(--primary);
    background: rgba(255, 255, 255, 0.88);
    box-shadow: 0 8px 18px rgba(43, 79, 158, 0.1), inset 0 1px 0 #fff;
    transition: transform 0.4s $ease-apple, box-shadow 0.4s $ease-apple;

    &:hover {
      transform: translateY(-3px);
      box-shadow: 0 14px 28px rgba(59, 111, 224, 0.22);
    }
  }
}

// ---------------- 沿边框滚动的风车 ----------------
.windmill {
  position: absolute;
  top: 0;
  left: 0;
  width: 38px;
  height: 38px;
  z-index: 3;
  pointer-events: none;
  offset-path: border-box;
  offset-anchor: 50% 50%;
  offset-rotate: 0deg;
  offset-distance: 0%;
  animation: windmillRun 11s linear infinite;
  filter: drop-shadow(0 8px 14px rgba(59, 111, 224, 0.3));

  .blades {
    position: absolute;
    inset: 0;
    animation: windmillSpin 1.9s linear infinite;
  }

  .blade {
    position: absolute;
    left: 50%;
    top: 50%;
    width: 9px;
    height: 15px;
    margin: -15px 0 0 -4.5px;
    border-radius: 6px 6px 2px 2px;
    background: linear-gradient(180deg, #ffffff, #c9dcff);
    box-shadow: inset 0 0 0 1px rgba(59, 111, 224, 0.22);
    transform-origin: 50% 100%;
    transform: rotate(var(--r));
  }

  .hub {
    position: absolute;
    left: 50%;
    top: 50%;
    width: 12px;
    height: 12px;
    margin: -6px 0 0 -6px;
    border-radius: 50%;
    background: linear-gradient(150deg, #6d9bf2, #2f5bb3);
    box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.92);
  }
}

@keyframes windmillRun {
  to {
    offset-distance: 100%;
  }
}

@keyframes windmillSpin {
  to {
    transform: rotate(360deg);
  }
}

// ==================== 右侧：登录（更窄，等高） ====================
.auth-panel {
  flex: 1 1 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.auth-stack {
  position: relative;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

// 吉祥物舞台：超出部分被裁掉，形成“从登录框后面探出头”的效果
.mascot-stage {
  position: relative;
  flex: 0 0 clamp(70px, 10vh, 94px);
  width: 140px;
  margin: 0 auto;
  overflow: hidden;
}

.mascot {
  position: absolute;
  left: 50%;
  bottom: 0;
  width: 78px;
  height: 74px;
  margin-left: -39px;
  transform: translateY(56%);
  transition: transform 0.62s $ease-spring;

  .antenna {
    position: absolute;
    left: 50%;
    top: 1px;
    width: 3px;
    height: 15px;
    margin-left: -1.5px;
    border-radius: 2px;
    background: linear-gradient(180deg, #8fb4f7, #5b8bf0);

    i {
      position: absolute;
      left: 50%;
      top: -6px;
      width: 11px;
      height: 11px;
      margin-left: -5.5px;
      border-radius: 50%;
      background: radial-gradient(circle at 34% 32%, #ffffff, #6d9bf2);
      box-shadow: 0 0 10px rgba(91, 139, 240, 0.85);
      animation: glowPulse 2.2s ease-in-out infinite;
    }
  }

  .ear {
    position: absolute;
    top: 13px;
    width: 20px;
    height: 22px;
    background: linear-gradient(160deg, #eaf2ff, #c9dcff);
    box-shadow: inset 0 0 0 1px rgba(59, 111, 224, 0.18);
  }

  .ear-l {
    left: 3px;
    border-radius: 72% 28% 62% 38%;
    transform: rotate(-18deg);
  }

  .ear-r {
    right: 3px;
    border-radius: 28% 72% 38% 62%;
    transform: rotate(18deg);
  }

  .head {
    position: absolute;
    inset: 21px 5px 0 5px;
    border-radius: 46% 46% 44% 44%;
    background: linear-gradient(168deg, #f6faff 0%, #dfeafe 55%, #c7dbff 100%);
    box-shadow: inset 0 -6px 14px rgba(59, 111, 224, 0.16), inset 0 2px 0 rgba(255, 255, 255, 0.95),
      0 12px 24px rgba(43, 79, 158, 0.22);
  }

  // 默认闭眼（睡着的小助手），探头时睁眼
  .eye {
    position: absolute;
    top: 15px;
    width: 11px;
    height: 3px;
    border-radius: 6px;
    background: #3d5480;
    overflow: hidden;
    transition: height 0.35s $ease-spring;

    i {
      position: absolute;
      left: 50%;
      top: 50%;
      width: 5px;
      height: 5px;
      margin: -2.5px 0 0 -2.5px;
      border-radius: 50%;
      background: #fff;
      opacity: 0;
      transition: opacity 0.24s ease 0.1s;
    }
  }

  .eye-l {
    left: 12px;
  }

  .eye-r {
    right: 12px;
  }

  .blush {
    position: absolute;
    bottom: 13px;
    width: 11px;
    height: 7px;
    border-radius: 50%;
    background: #ffb0b0;
    opacity: 0;
    filter: blur(1px);
    transition: opacity 0.35s ease 0.08s;
  }

  .blush-l {
    left: 5px;
  }

  .blush-r {
    right: 5px;
  }

  .mouth {
    position: absolute;
    left: 50%;
    bottom: 13px;
    width: 16px;
    height: 8px;
    margin-left: -8px;
    border: 2px solid #3d5480;
    border-top: none;
    border-radius: 0 0 12px 12px;
    opacity: 0.3;
    transform: scaleY(0.45);
    transform-origin: top center;
    transition: opacity 0.32s ease, transform 0.45s $ease-spring;
  }
}

// 鼠标移入登录框 → 小助手探头
.auth-stack:hover .mascot {
  transform: translateY(0);

  .eye {
    height: 11px;

    i {
      opacity: 0.95;
    }
  }

  .blush {
    opacity: 0.75;
  }

  .mouth {
    opacity: 1;
    transform: scaleY(1);
  }
}

@keyframes glowPulse {
  0%,
  100% {
    box-shadow: 0 0 8px rgba(91, 139, 240, 0.6);
  }
  50% {
    box-shadow: 0 0 16px rgba(91, 139, 240, 1);
  }
}

// 登录卡片撑满剩余高度，内容垂直居中
.auth-card {
  position: relative;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  border-radius: 28px;
  border: 1px solid rgba(255, 255, 255, 0.92);
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 30px 70px rgba(43, 79, 158, 0.16), 0 2px 8px rgba(43, 79, 158, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(24px) saturate(180%);
  transition: transform 0.5s $ease-apple, box-shadow 0.5s $ease-apple;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 40px 90px rgba(43, 79, 158, 0.22), 0 2px 8px rgba(43, 79, 158, 0.06),
      inset 0 1px 0 rgba(255, 255, 255, 0.95);
  }

  :deep(.el-card__body) {
    width: 100%;
    margin: auto 0;
    padding: clamp(20px, 3vh, 34px) clamp(22px, 2.2vw, 38px);
  }
}

.card-head {
  margin-bottom: clamp(12px, 2vh, 20px);

  h3 {
    margin: 0;
    font-size: clamp(19px, 1.5vw, 24px);
    font-weight: 700;
    letter-spacing: -0.02em;
  }

  p {
    margin: 6px 0 0;
    font-size: 13px;
    color: var(--muted);
  }
}

.guardian-hint {
  flex: 0 0 auto;
  margin: 12px 0 0;
  text-align: center;
  font-size: 12px;
}

// ---------------- 切换到普通用户登录 ----------------
.switch-row {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--border);
}

.switch-btn.el-button {
  width: 100%;
  height: 42px;
  border-radius: 13px;
  border: 1px solid rgba(59, 111, 224, 0.2);
  background: rgba(255, 255, 255, 0.86);
  font-size: 13.5px;
  font-weight: 600;
  color: var(--primary);
  transition: transform 0.35s $ease-apple, box-shadow 0.35s $ease-apple, background 0.3s ease;

  &:not(.is-disabled):hover {
    transform: translateY(-2px);
    background: #fff;
    border-color: rgba(59, 111, 224, 0.4);
    color: var(--primary);
    box-shadow: 0 10px 22px rgba(59, 111, 224, 0.2);
  }

  .el-icon {
    margin-right: 6px;
  }
}

// ---------------- Element Plus 定制 ----------------
:deep(.el-form-item) {
  margin-bottom: clamp(11px, 1.7vh, 17px);
}

:deep(.el-form-item__label) {
  padding-bottom: 4px;
  font-size: 12.5px;
  font-weight: 600;
  color: #5c6579;
}

:deep(.el-input__wrapper) {
  padding: clamp(3px, 0.6vh, 7px) 13px;
  border-radius: 13px;
  background: #f4f7fd;
  box-shadow: inset 0 0 0 1px rgba(59, 111, 224, 0.1);
  transition: background 0.35s ease, box-shadow 0.35s $ease-apple;

  &:hover {
    background: #eef4ff;
    box-shadow: inset 0 0 0 1px rgba(59, 111, 224, 0.3);
  }

  &.is-focus {
    background: #fff;
    box-shadow: inset 0 0 0 1.5px var(--primary), 0 0 0 4px rgba(59, 111, 224, 0.13);
  }
}

.login-btn.el-button--primary {
  position: relative;
  width: 100%;
  height: clamp(42px, 5.2vh, 50px);
  margin-top: clamp(6px, 1.2vh, 12px);
  overflow: hidden;
  border: none;
  border-radius: 14px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.04em;
  background: linear-gradient(135deg, #5b8bf0, #3b6fe0 55%, #2f5bb3);
  box-shadow: 0 10px 24px rgba(59, 111, 224, 0.3);
  transition: transform 0.35s $ease-apple, box-shadow 0.35s $ease-apple;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(100deg, transparent 20%, rgba(255, 255, 255, 0.45) 50%, transparent 80%);
    transform: translateX(-130%);
    transition: transform 0.75s $ease-apple;
  }

  &:hover {
    transform: translateY(-2px);
    background: linear-gradient(135deg, #6b98f3, #4478e4 55%, #33619f);
    box-shadow: 0 16px 34px rgba(59, 111, 224, 0.4);

    &::after {
      transform: translateX(130%);
    }
  }

  &:active {
    transform: translateY(0) scale(0.99);
  }
}

.form-hint {
  margin: 9px 0 0;
  text-align: center;
  font-size: 12px;
}

// ---------------- 矮屏兜底：保证仍然一屏放下 ----------------
@media (max-height: 760px) {
  .movie-quotes .quote-card:nth-child(4) {
    display: none;
  }
}

@media (max-height: 640px) {
  .movie-quotes .quote-card:nth-child(3) {
    display: none;
  }

  .motto-row {
    display: none;
  }

  .quote-desc {
    display: none;
  }
}

// ---------------- 窄屏兜底：改为上下排列 ----------------
@media (max-width: 900px) {
  .login-page {
    height: auto;
    min-height: 100dvh;
    overflow: visible;
  }

  .login-shell {
    flex-direction: column;
    height: auto;
    max-width: 560px;
  }

  .quote-panel {
    flex: none;
    padding: 26px 24px;
    border-radius: 26px;
  }

  .quote-title {
    margin-top: 20px;
    font-size: 26px;
  }

  .motto-row {
    margin-top: 20px;
    padding-top: 0;
  }

  .auth-stack,
  .auth-card {
    flex: none;
  }

  .auth-card {
    overflow: visible;
  }

  .mascot-stage {
    flex: 0 0 84px;
  }
}
</style>
