<template>
  <div class="landing">
    <!-- ==================== 顶部：流动的英语励志语 ==================== -->
    <div class="quote-strip">
      <div class="quote-track">
        <span v-for="(q, i) in stripQuotes" :key="`a${i}`" class="quote-item">
          {{ q }}<i>✦</i>
        </span>
        <span v-for="(q, i) in stripQuotes" :key="`b${i}`" class="quote-item">
          {{ q }}<i>✦</i>
        </span>
      </div>
    </div>

    <!-- ==================== 导航栏：右上角登录注册 ==================== -->
    <header class="landing-nav">
      <div class="nav-brand">
        <span class="brand-mark">🎙️</span>
        <span class="brand-name">英语口语训练系统</span>
      </div>

      <nav class="nav-links">
        <button v-for="l in navLinks" :key="l.id" class="nav-link" @click="scrollTo(l.id)">
          {{ l.label }}
        </button>
      </nav>

      <div class="nav-actions">
        <button class="ghost-btn" @click="goAuth('password')">登录</button>
        <button class="primary-btn" @click="goAuth('sms')">注册 / 登录</button>
      </div>
    </header>

    <main class="landing-main">
      <!-- ==================== Hero：左介绍 + 右系统展示 ==================== -->
      <section class="hero">
        <!-- 左侧：与登录页左侧一致的设计 -->
        <div class="hero-left">
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

          <div class="hero-left-inner">
            <div class="brand-row">
              <div class="brand-mark-lg">🎙️</div>
              <div class="brand-text">
                <h2>英语口语训练系统</h2>
                <p>AI Scene Role-play</p>
              </div>
            </div>

            <h1 class="hero-title">
              开口说英语，<br />
              让每一次练习<em>都被听见</em>
            </h1>

            <p class="hero-desc">
              基于大模型场景扮演的口语陪练平台。在真实情境中对话，获得即时评分与表达建议，
              让口语练习像聊天一样自然。
            </p>

            <ul class="feature-list">
              <li v-for="f in heroFeatures" :key="f.title">
                <span class="feature-icon"><el-icon><component :is="f.icon" /></el-icon></span>
                <div class="feature-text">
                  <strong>{{ f.title }}</strong>
                  <span>{{ f.desc }}</span>
                </div>
              </li>
            </ul>

            <div class="stat-row">
              <div class="stat">
                <strong>6</strong>
                <span>真实场景</span>
              </div>
              <div class="stat">
                <strong>4</strong>
                <span>评分维度</span>
              </div>
              <div class="stat">
                <strong>24h</strong>
                <span>随时可练</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：动画系统展示图 -->
        <div class="hero-right">
          <div class="showcase">
            <span class="float-chip chip-1">实时评分</span>
            <span class="float-chip chip-2">语音开口</span>
            <span class="float-chip chip-3">AI 陪练</span>

            <div class="showcase-window">
              <div class="window-bar">
                <span class="win-dot red"></span>
                <span class="win-dot yellow"></span>
                <span class="win-dot green"></span>
                <span class="window-title">{{ slides[active].title }}</span>
              </div>

              <div class="window-body">
                <transition name="slide-fade" mode="out-in">
                  <!-- 1. 场景对话 -->
                  <div v-if="active === 0" key="chat" class="slide mock-chat">
                    <div class="chat-head">
                      <span class="chat-scene">餐厅点餐 · Ordering Food</span>
                      <span class="chat-badge">AI 角色扮演</span>
                    </div>
                    <div class="bubble ai">
                      Good evening! What would you like to order?
                      <em>晚上好！您想点些什么？</em>
                    </div>
                    <div class="bubble me">I'd like a steak, please.</div>
                    <div class="bubble ai typing"><i></i><i></i><i></i></div>
                    <div class="chat-input">
                      <span class="mic-dot"></span>
                      <span>正在聆听…</span>
                      <span class="wave"><i></i><i></i><i></i><i></i><i></i></span>
                    </div>
                  </div>

                  <!-- 2. 入学智能测评 -->
                  <div v-else-if="active === 1" key="test" class="slide mock-test">
                    <div class="test-head">
                      <span>AI 教练提问 · 第 2/3 题</span>
                      <span class="test-tag">看图描述</span>
                    </div>
                    <div class="test-progress"><span></span></div>
                    <div class="test-image">
                      <span class="img-corner tl"></span>
                      <span class="img-corner tr"></span>
                      <span class="img-corner bl"></span>
                      <span class="img-corner br"></span>
                      <span class="img-hint">静态题库 · 随机抽题</span>
                    </div>
                    <div class="test-question">请用英语描述图片中的场景与细节。</div>
                    <div class="test-answer">There is a table and two people…</div>
                  </div>

                  <!-- 3. 口语评分报表 -->
                  <div v-else key="report" class="slide mock-report">
                    <div class="report-head">
                      <span>本次口语评分</span>
                      <span class="level-chip">B1</span>
                    </div>
                    <div class="bars">
                      <div v-for="d in dims" :key="d.label" class="bar-item">
                        <span class="bar-label">{{ d.label }}</span>
                        <span class="bar-track"><i :style="{ '--w': `${d.value}%` }"></i></span>
                        <b>{{ d.value }}</b>
                      </div>
                    </div>
                    <div class="report-tip">
                      <el-icon><Sunny /></el-icon>
                      发音清晰，注意 th 音的舌尖位置
                    </div>
                  </div>
                </transition>
              </div>
            </div>

            <div class="showcase-tabs">
              <button
                v-for="(s, i) in slides"
                :key="s.title"
                class="showcase-tab"
                :class="{ active: i === active }"
                @click="active = i"
              >
                <span class="tab-index">0{{ i + 1 }}</span>
                <span class="tab-text">
                  <strong>{{ s.title }}</strong>
                  <em>{{ s.sub }}</em>
                </span>
              </button>
            </div>
          </div>
        </div>
      </section>

      <!-- ==================== 核心功能三宫格 ==================== -->
      <section id="features" class="feature-grid">
        <div class="section-head">
          <h2>三种练习方式，覆盖口语提升全流程</h2>
          <p>从开口对话到智能测评，再到数据复盘，形成完整闭环</p>
        </div>

        <div class="grid">
          <article v-for="c in featureCards" :key="c.title" class="feature-card">
            <span class="card-icon"><el-icon><component :is="c.icon" /></el-icon></span>
            <h3>{{ c.title }}</h3>
            <p>{{ c.desc }}</p>
            <ul class="card-points">
              <li v-for="p in c.points" :key="p">{{ p }}</li>
            </ul>
          </article>
        </div>
      </section>

      <!-- ==================== 使用流程 ==================== -->
      <section id="how" class="steps">
        <div class="section-head">
          <h2>四步开始你的第一次开口</h2>
          <p>注册后即可进入入学测评，系统会自动生成专属学习方案</p>
        </div>
        <div class="step-row">
          <div v-for="(s, i) in steps" :key="s.title" class="step-item">
            <span class="step-num">{{ i + 1 }}</span>
            <strong>{{ s.title }}</strong>
            <span class="step-desc">{{ s.desc }}</span>
          </div>
        </div>
      </section>

      <!-- ==================== 结尾 CTA ==================== -->
      <section class="cta">
        <h2>今天，就让 AI 陪你开口说英语</h2>
        <p>免费注册，立即做一次入学口语测评</p>
        <div class="cta-actions">
          <button class="primary-btn lg" @click="goAuth('sms')">立即开始</button>
          <button class="ghost-btn lg" @click="goAuth('password')">已有账号，去登录</button>
        </div>
        <div class="cta-chips">
          <span v-for="c in ctaChips" :key="c">{{ c }}</span>
        </div>
      </section>
    </main>

    <footer id="scenes" class="landing-footer">
      <span>英语口语训练系统 · 基于大模型场景扮演</span>
      <span class="footer-tip">演示环境验证码固定为 123456</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ChatDotRound,
  DataAnalysis,
  Microphone,
  Reading,
  Sunny,
  TrendCharts,
  Trophy,
} from '@element-plus/icons-vue'

const router = useRouter()

/** 顶部流动励志语 */
const stripQuotes = [
  'The best way to predict the future is to create it.',
  'Practice makes progress, not perfect.',
  'Speak today, fluent tomorrow.',
  'Every word you say builds your confidence.',
  'Small steps every day lead to big change.',
  "Don't be afraid to make mistakes — that's how you learn.",
  'Fluency is built one conversation at a time.',
]

const navLinks = [
  { id: 'features', label: '核心功能' },
  { id: 'how', label: '使用流程' },
  { id: 'scenes', label: '关于系统' },
]

const heroFeatures = [
  { icon: ChatDotRound, title: '场景角色扮演', desc: '餐厅点餐、机场值机、商务会议等真实情境' },
  { icon: Microphone, title: '语音开口对话', desc: '浏览器直接语音输入，AI 实时回应并朗读' },
  { icon: TrendCharts, title: '四维口语评分', desc: '发音、流利度、反应、自然度即时反馈' },
  { icon: DataAnalysis, title: '学习报表追踪', desc: '入学测评定级，个性化方案与每日任务闭环' },
]

/** 右侧动画展示图：三个功能 */
const slides = [
  { title: '场景对话', sub: 'AI 角色扮演陪练' },
  { title: '智能测评', sub: '入学口语水平定级' },
  { title: '评分报表', sub: '四维数据复盘' },
]
const active = ref(0)
let timer: ReturnType<typeof setInterval> | undefined

const dims = [
  { label: '发音', value: 82 },
  { label: '流利度', value: 74 },
  { label: '反应', value: 68 },
  { label: '自然度', value: 79 },
]

const featureCards = [
  {
    icon: ChatDotRound,
    title: '场景对话练习',
    desc: '选择真实场景，与 AI 扮演的角色进行多轮英文对话，随时打断、随时重来。',
    points: ['6 大真实场景库', '语音输入 / 文字输入', 'AI 回复自动朗读'],
  },
  {
    icon: Trophy,
    title: '入学智能测评',
    desc: '三题定级：自我介绍、看图描述、自由问答。大模型只负责评价，题目来自静态题库。',
    points: ['随机抽题防背题', '自动判定 A1–C2 等级', '生成专属学习方案'],
  },
  {
    icon: Reading,
    title: '精听与跟读',
    desc: '配套学习素材支持精听训练与逐句跟读，把听到的表达真正变成自己的。',
    points: ['逐句精听拆解', '跟读评分对比', '素材收藏与复习'],
  },
]

const steps = [
  { title: '注册登录', desc: '手机号验证码，一键注册' },
  { title: '入学测评', desc: '三题定级，了解当前水平' },
  { title: '生成方案', desc: '按目标生成每日任务' },
  { title: '开口练习', desc: '场景对话 + 精听跟读' },
]

const ctaChips = ['无需下载安装', '浏览器直接开口', '边练边拿到反馈']

function goAuth(tab: 'sms' | 'password') {
  router.push({ path: '/login', query: { tab } })
}

function scrollTo(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

onMounted(() => {
  timer = setInterval(() => {
    active.value = (active.value + 1) % slides.length
  }, 4200)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

// 整页可上下滚动（独立滚动容器，避免依赖 body 高度）
.landing {
  height: 100vh;
  height: 100dvh;
  overflow-y: auto;
  overflow-x: hidden;
  scroll-behavior: smooth;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'PingFang SC',
    'Microsoft YaHei', sans-serif;
  background:
    radial-gradient(1100px 560px at 8% -6%, #e6efff 0%, rgba(230, 239, 255, 0) 60%),
    radial-gradient(900px 520px at 104% 4%, #eaf2ff 0%, rgba(234, 242, 255, 0) 58%),
    linear-gradient(172deg, #f8fbff 0%, #eff4fd 52%, #e6eefb 100%);
}

// ---------------- 顶部流动励志语 ----------------
.quote-strip {
  position: sticky;
  top: 0;
  z-index: 30;
  height: 38px;
  display: flex;
  align-items: center;
  overflow: hidden;
  background: linear-gradient(90deg, #2f5bb3, #4a7ce4 45%, #2f5bb3);
  color: #eaf2ff;
  font-size: 12.5px;
  letter-spacing: 0.02em;
}

.quote-track {
  display: flex;
  align-items: center;
  width: max-content;
  animation: marquee 46s linear infinite;
}

.quote-strip:hover .quote-track {
  animation-play-state: paused;
}

.quote-item {
  display: inline-flex;
  align-items: center;
  white-space: nowrap;

  i {
    margin: 0 26px;
    font-style: normal;
    opacity: 0.5;
    font-size: 10px;
  }
}

@keyframes marquee {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(-50%);
  }
}

// ---------------- 导航栏 ----------------
.landing-nav {
  position: sticky;
  top: 38px;
  z-index: 29;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 14px clamp(20px, 4vw, 64px);
  background: rgba(248, 251, 255, 0.78);
  border-bottom: 1px solid rgba(59, 111, 224, 0.1);
  backdrop-filter: blur(18px) saturate(160%);
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 10px;

  .brand-mark {
    width: 38px;
    height: 38px;
    display: grid;
    place-items: center;
    font-size: 19px;
    border-radius: 12px;
    background: linear-gradient(150deg, #ffffff, #dfe9fd);
    box-shadow: 0 4px 12px rgba(43, 79, 158, 0.14), inset 0 1px 0 #fff;
    transition: transform 0.5s $ease-spring;
  }

  &:hover .brand-mark {
    transform: rotate(-8deg) scale(1.08);
  }

  .brand-name {
    font-size: 15.5px;
    font-weight: 700;
    letter-spacing: -0.01em;
  }
}

.nav-links {
  display: flex;
  gap: 6px;
  margin-left: 12px;
}

.nav-link {
  padding: 8px 14px;
  border: none;
  border-radius: 11px;
  background: transparent;
  font-size: 14px;
  font-weight: 600;
  color: #5c6579;
  cursor: pointer;
  transition: color 0.3s ease, background 0.35s $ease-apple;

  &:hover {
    color: var(--primary);
    background: rgba(59, 111, 224, 0.08);
  }
}

.nav-actions {
  margin-left: auto;
  display: flex;
  gap: 10px;
}

.ghost-btn {
  padding: 10px 20px;
  border-radius: 13px;
  border: 1px solid rgba(59, 111, 224, 0.22);
  background: #fff;
  font-size: 14px;
  font-weight: 600;
  color: var(--primary);
  cursor: pointer;
  transition: transform 0.35s $ease-apple, box-shadow 0.35s $ease-apple, background 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    background: #f4f8ff;
    box-shadow: 0 10px 22px rgba(59, 111, 224, 0.2);
  }

  &.lg {
    padding: 13px 26px;
    font-size: 15px;
  }
}

.primary-btn {
  padding: 10px 22px;
  border: none;
  border-radius: 13px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
  background: linear-gradient(135deg, #5b8bf0, #3b6fe0 55%, #2f5bb3);
  box-shadow: 0 8px 20px rgba(59, 111, 224, 0.28);
  transition: transform 0.35s $ease-apple, box-shadow 0.35s $ease-apple;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 14px 30px rgba(59, 111, 224, 0.38);
  }

  &.lg {
    padding: 13px 30px;
    font-size: 15px;
  }
}

// ---------------- 主内容 ----------------
.landing-main {
  width: min(1360px, 100%);
  margin: 0 auto;
  padding: clamp(24px, 4vh, 52px) clamp(20px, 4vw, 64px) 0;
}

// ==================== Hero ====================
.hero {
  display: flex;
  align-items: stretch;
  gap: clamp(20px, 2.8vw, 44px);
}

.hero-left {
  position: relative;
  flex: 1 1 0;
  min-width: 0;
  padding: clamp(26px, 3.2vh, 44px) clamp(24px, 2.6vw, 44px);
  border-radius: 32px;
  border: 1px solid rgba(59, 111, 224, 0.16);
  background: rgba(255, 255, 255, 0.6);
  box-shadow: 0 26px 64px rgba(43, 79, 158, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.78);
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

.hero-left-inner {
  position: relative;
  z-index: 2;
  height: 100%;
  display: flex;
  flex-direction: column;
}

// 沿边框滚动的风车
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
  animation: windmillRun 12s linear infinite;
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

.hero-title {
  margin: clamp(18px, 2.6vh, 34px) 0 0;
  font-size: clamp(28px, 2.6vw, 42px);
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

.hero-desc {
  margin: 14px 0 0;
  max-width: 470px;
  font-size: 14.5px;
  line-height: 1.75;
  color: #66708b;
}

.feature-list {
  list-style: none;
  margin: clamp(16px, 2.2vh, 28px) 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;

  li {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 9px 14px;
    border-radius: 16px;
    transition: transform 0.45s $ease-apple, background 0.45s $ease-apple, box-shadow 0.45s $ease-apple;

    &:hover {
      transform: translateX(8px);
      background: rgba(255, 255, 255, 0.92);
      box-shadow: 0 12px 26px rgba(43, 79, 158, 0.12);

      .feature-icon {
        transform: scale(1.1) rotate(-6deg);
        background: linear-gradient(150deg, #5b8bf0, #3b6fe0);
        color: #fff;
      }
    }
  }
}

.feature-icon {
  flex: 0 0 auto;
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  font-size: 19px;
  border-radius: 12px;
  color: var(--primary);
  background: linear-gradient(150deg, #f3f7ff, #dde8fd);
  box-shadow: inset 0 1px 0 #fff;
  transition: transform 0.5s $ease-spring, background 0.4s ease, color 0.4s ease;
}

.feature-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;

  strong {
    font-size: 14px;
    font-weight: 650;
    letter-spacing: -0.01em;
  }

  span {
    font-size: 12.5px;
    color: var(--muted);
  }
}

.stat-row {
  margin-top: auto;
  padding-top: clamp(16px, 2.4vh, 28px);
  display: flex;
  gap: 12px;

  .stat {
    flex: 1;
    padding: 13px 16px;
    border-radius: 16px;
    background: rgba(255, 255, 255, 0.74);
    box-shadow: inset 0 1px 0 #fff, 0 6px 16px rgba(43, 79, 158, 0.06);
    transition: transform 0.45s $ease-apple, box-shadow 0.45s $ease-apple;

    &:hover {
      transform: translateY(-5px);
      box-shadow: inset 0 1px 0 #fff, 0 16px 30px rgba(43, 79, 158, 0.16);
    }

    strong {
      display: block;
      font-size: 22px;
      font-weight: 700;
      letter-spacing: -0.02em;
      background: linear-gradient(120deg, #5b8bf0, #2f5bb3);
      -webkit-background-clip: text;
      background-clip: text;
      -webkit-text-fill-color: transparent;
    }

    span {
      font-size: 12px;
      color: var(--muted);
    }
  }
}

// ==================== 右侧系统展示 ====================
.hero-right {
  flex: 1 1 0;
  min-width: 0;
  display: flex;
}

.showcase {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: clamp(20px, 2.6vh, 30px) clamp(18px, 2vw, 28px);
  border-radius: 32px;
  background: linear-gradient(158deg, rgba(59, 111, 224, 0.1), rgba(59, 111, 224, 0.03) 55%);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 26px 64px rgba(43, 79, 158, 0.12), inset 0 1px 0 rgba(255, 255, 255, 0.85);
}

// 浮动小组件
.float-chip {
  position: absolute;
  z-index: 5;
  padding: 6px 13px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: var(--primary);
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 10px 22px rgba(43, 79, 158, 0.18), inset 0 1px 0 #fff;
  animation: chipFloat 5s ease-in-out infinite;
  pointer-events: none;
}

.chip-1 {
  top: 14px;
  left: -14px;
}

.chip-2 {
  top: 46%;
  right: -16px;
  animation-delay: -1.6s;
}

.chip-3 {
  bottom: 96px;
  left: -18px;
  animation-delay: -3.2s;
}

@keyframes chipFloat {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-9px);
  }
}

.showcase-window {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border-radius: 20px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 18px 40px rgba(43, 79, 158, 0.16);
}

.window-bar {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 11px 16px;
  background: #f4f7fd;
  border-bottom: 1px solid rgba(59, 111, 224, 0.1);

  .win-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
  }

  .red {
    background: #ff6058;
  }

  .yellow {
    background: #ffbe2f;
  }

  .green {
    background: #2aca44;
  }

  .window-title {
    margin-left: 10px;
    font-size: 12.5px;
    font-weight: 600;
    color: #7c869c;
  }
}

.window-body {
  flex: 1;
  min-height: 0;
  padding: 18px;
  background: linear-gradient(180deg, #fbfdff, #f3f7fe);
}

.slide {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.slide-fade-enter-active {
  transition: opacity 0.45s $ease-apple, transform 0.45s $ease-apple;
}

.slide-fade-leave-active {
  transition: opacity 0.28s ease, transform 0.28s ease;
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateY(14px) scale(0.985);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.99);
}

// ---- 展示 1：场景对话 ----
.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .chat-scene {
    font-size: 13px;
    font-weight: 700;
    color: #2c3a5c;
  }

  .chat-badge {
    padding: 3px 10px;
    border-radius: 999px;
    font-size: 11px;
    font-weight: 600;
    color: var(--primary);
    background: rgba(59, 111, 224, 0.12);
  }
}

.bubble {
  max-width: 82%;
  padding: 10px 14px;
  border-radius: 16px;
  font-size: 13px;
  line-height: 1.55;
  animation: bubbleIn 0.55s $ease-apple both;

  &.ai {
    align-self: flex-start;
    color: #2c3a5c;
    background: #fff;
    border: 1px solid rgba(59, 111, 224, 0.14);
    border-bottom-left-radius: 5px;
    box-shadow: 0 6px 16px rgba(43, 79, 158, 0.08);

    em {
      display: block;
      margin-top: 4px;
      font-style: normal;
      font-size: 12px;
      color: var(--muted);
    }
  }

  &.me {
    align-self: flex-end;
    color: #fff;
    background: linear-gradient(135deg, #5b8bf0, #3b6fe0);
    border-bottom-right-radius: 5px;
    box-shadow: 0 8px 18px rgba(59, 111, 224, 0.28);
    animation-delay: 0.18s;
  }

  &.typing {
    display: flex;
    gap: 5px;
    padding: 12px 16px;
    animation-delay: 0.36s;

    i {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: #a9bce0;
      animation: typingDot 1.2s ease-in-out infinite;

      &:nth-child(2) {
        animation-delay: 0.18s;
      }

      &:nth-child(3) {
        animation-delay: 0.36s;
      }
    }
  }
}

@keyframes bubbleIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes typingDot {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.55;
  }
  30% {
    transform: translateY(-5px);
    opacity: 1;
  }
}

.chat-input {
  margin-top: auto;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid rgba(59, 111, 224, 0.16);
  font-size: 12.5px;
  color: var(--muted);

  .mic-dot {
    width: 9px;
    height: 9px;
    border-radius: 50%;
    background: #ff6a4d;
    box-shadow: 0 0 0 0 rgba(255, 106, 77, 0.5);
    animation: micPulse 1.6s ease-out infinite;
  }

  .wave {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 3px;

    i {
      width: 3px;
      height: 12px;
      border-radius: 2px;
      background: #8fb4f7;
      animation: waveBar 1s ease-in-out infinite;

      &:nth-child(2) {
        animation-delay: 0.12s;
      }

      &:nth-child(3) {
        animation-delay: 0.24s;
      }

      &:nth-child(4) {
        animation-delay: 0.36s;
      }

      &:nth-child(5) {
        animation-delay: 0.48s;
      }
    }
  }
}

@keyframes micPulse {
  0% {
    box-shadow: 0 0 0 0 rgba(255, 106, 77, 0.5);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(255, 106, 77, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(255, 106, 77, 0);
  }
}

@keyframes waveBar {
  0%,
  100% {
    transform: scaleY(0.5);
  }
  50% {
    transform: scaleY(1.5);
  }
}

// ---- 展示 2：入学智能测评 ----
.mock-test {
  .test-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 13px;
    font-weight: 700;
    color: #2c3a5c;

    .test-tag {
      padding: 3px 10px;
      border-radius: 999px;
      font-size: 11px;
      font-weight: 600;
      color: #fff;
      background: linear-gradient(135deg, #5b8bf0, #3b6fe0);
    }
  }

  .test-progress {
    height: 7px;
    border-radius: 999px;
    background: rgba(59, 111, 224, 0.14);
    overflow: hidden;

    span {
      display: block;
      height: 100%;
      border-radius: 999px;
      background: linear-gradient(90deg, #5b8bf0, #2f5bb3);
      animation: progressGrow 1.2s $ease-apple both;
    }
  }

  .test-image {
    position: relative;
    flex: 1;
    min-height: 92px;
    display: grid;
    place-items: center;
    border-radius: 14px;
    background: linear-gradient(140deg, #e9f0fe, #d6e3fb);
    border: 1px dashed rgba(59, 111, 224, 0.32);

    .img-hint {
      font-size: 12px;
      color: #6b7fa8;
    }

    .img-corner {
      position: absolute;
      width: 16px;
      height: 16px;
      border: 2px solid rgba(59, 111, 224, 0.5);
      animation: cornerBlink 2.4s ease-in-out infinite;
    }

    .tl {
      top: 10px;
      left: 10px;
      border-right: none;
      border-bottom: none;
      border-radius: 6px 0 0 0;
    }

    .tr {
      top: 10px;
      right: 10px;
      border-left: none;
      border-bottom: none;
      border-radius: 0 6px 0 0;
    }

    .bl {
      bottom: 10px;
      left: 10px;
      border-right: none;
      border-top: none;
      border-radius: 0 0 0 6px;
    }

    .br {
      bottom: 10px;
      right: 10px;
      border-left: none;
      border-top: none;
      border-radius: 0 0 6px 0;
    }
  }

  .test-question {
    font-size: 13px;
    font-weight: 600;
    color: #2c3a5c;
  }

  .test-answer {
    padding: 10px 14px;
    border-radius: 14px;
    font-size: 12.5px;
    color: #55617e;
    background: #fff;
    border: 1px solid rgba(59, 111, 224, 0.14);
    animation: bubbleIn 0.6s $ease-apple both 0.2s;
  }
}

@keyframes progressGrow {
  from {
    width: 0;
  }
  to {
    width: 66%;
  }
}

@keyframes cornerBlink {
  0%,
  100% {
    opacity: 0.35;
    transform: scale(0.94);
  }
  50% {
    opacity: 1;
    transform: scale(1.06);
  }
}

// ---- 展示 3：口语评分报表 ----
.mock-report {
  .report-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 13px;
    font-weight: 700;
    color: #2c3a5c;

    .level-chip {
      padding: 3px 12px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 700;
      color: #fff;
      background: linear-gradient(135deg, #22a06b, #148a56);
      animation: bubbleIn 0.6s $ease-spring both 0.5s;
    }
  }

  .bars {
    display: flex;
    flex-direction: column;
    gap: 11px;
    margin-top: 6px;
  }

  .bar-item {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 12px;

    .bar-label {
      flex: 0 0 40px;
      color: #55617e;
    }

    .bar-track {
      flex: 1;
      height: 9px;
      border-radius: 999px;
      background: rgba(59, 111, 224, 0.13);
      overflow: hidden;

      i {
        display: block;
        height: 100%;
        border-radius: 999px;
        background: linear-gradient(90deg, #7ba6f6, #3b6fe0);
        animation: barGrow 1.1s $ease-apple both;
      }
    }

    b {
      flex: 0 0 26px;
      text-align: right;
      font-weight: 700;
      color: var(--primary);
    }

    &:nth-child(2) .bar-track i {
      animation-delay: 0.1s;
    }

    &:nth-child(3) .bar-track i {
      animation-delay: 0.2s;
    }

    &:nth-child(4) .bar-track i {
      animation-delay: 0.3s;
    }
  }

  .report-tip {
    margin-top: auto;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 14px;
    border-radius: 14px;
    font-size: 12.5px;
    color: #6b7fa8;
    background: rgba(59, 111, 224, 0.08);
    animation: bubbleIn 0.6s $ease-apple both 0.55s;
  }
}

@keyframes barGrow {
  from {
    width: 0;
  }
  to {
    width: var(--w);
  }
}

// ---- 展示切换标签 ----
.showcase-tabs {
  display: flex;
  gap: 10px;
}

.showcase-tab {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 13px;
  border-radius: 15px;
  border: 1px solid rgba(59, 111, 224, 0.14);
  background: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  text-align: left;
  transition: transform 0.4s $ease-apple, box-shadow 0.4s $ease-apple, background 0.4s ease,
    border-color 0.4s ease;

  .tab-index {
    font-size: 11px;
    font-weight: 700;
    color: #a9b7d0;
  }

  .tab-text {
    display: flex;
    flex-direction: column;
    min-width: 0;

    strong {
      font-size: 13px;
      font-weight: 650;
      color: #3d4a68;
    }

    em {
      font-style: normal;
      font-size: 11px;
      color: var(--muted);
    }
  }

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 12px 26px rgba(43, 79, 158, 0.16);
    border-color: rgba(59, 111, 224, 0.32);
  }

  &.active {
    background: #fff;
    border-color: var(--primary);
    box-shadow: 0 12px 26px rgba(59, 111, 224, 0.24);

    .tab-index {
      color: var(--primary);
    }

    .tab-text strong {
      color: var(--primary);
    }
  }
}

// ==================== 通用区块标题 ====================
.section-head {
  text-align: center;
  margin-bottom: clamp(24px, 3.4vh, 42px);

  h2 {
    margin: 0;
    font-size: clamp(22px, 2vw, 31px);
    font-weight: 700;
    letter-spacing: -0.02em;
    color: #1b2745;
  }

  p {
    margin: 10px 0 0;
    font-size: 14.5px;
    color: #6b7590;
  }
}

// ==================== 核心功能三宫格 ====================
.feature-grid {
  margin-top: clamp(44px, 7vh, 88px);
  scroll-margin-top: 130px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: clamp(16px, 2vw, 26px);
}

.feature-card {
  padding: clamp(22px, 2.6vh, 32px) clamp(20px, 1.8vw, 28px);
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 16px 40px rgba(43, 79, 158, 0.09), inset 0 1px 0 #fff;
  transition: transform 0.5s $ease-apple, box-shadow 0.5s $ease-apple;

  &:hover {
    transform: translateY(-8px);
    box-shadow: 0 30px 60px rgba(43, 79, 158, 0.2), inset 0 1px 0 #fff;

    .card-icon {
      transform: scale(1.08) rotate(-6deg);
      background: linear-gradient(150deg, #5b8bf0, #3b6fe0);
      color: #fff;
    }
  }

  h3 {
    margin: 16px 0 8px;
    font-size: 17px;
    font-weight: 700;
    letter-spacing: -0.01em;
  }

  > p {
    margin: 0;
    font-size: 13.5px;
    line-height: 1.7;
    color: #6b7590;
  }
}

.card-icon {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  font-size: 22px;
  border-radius: 15px;
  color: var(--primary);
  background: linear-gradient(150deg, #f3f7ff, #dde8fd);
  box-shadow: inset 0 1px 0 #fff;
  transition: transform 0.5s $ease-spring, background 0.4s ease, color 0.4s ease;
}

.card-points {
  list-style: none;
  margin: 16px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 7px;

  li {
    position: relative;
    padding-left: 18px;
    font-size: 12.5px;
    color: #55617e;

    &::before {
      content: '';
      position: absolute;
      left: 2px;
      top: 7px;
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: linear-gradient(135deg, #6d9bf2, #3b6fe0);
    }
  }
}

// ==================== 使用流程 ====================
.steps {
  margin-top: clamp(44px, 7vh, 88px);
  scroll-margin-top: 130px;
}

.step-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: clamp(14px, 1.8vw, 24px);
}

.step-item {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 22px 20px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 14px 34px rgba(43, 79, 158, 0.08), inset 0 1px 0 #fff;
  transition: transform 0.5s $ease-apple, box-shadow 0.5s $ease-apple;

  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 26px 52px rgba(43, 79, 158, 0.18), inset 0 1px 0 #fff;
  }

  .step-num {
    width: 32px;
    height: 32px;
    display: grid;
    place-items: center;
    border-radius: 11px;
    font-size: 14px;
    font-weight: 700;
    color: #fff;
    background: linear-gradient(135deg, #5b8bf0, #2f5bb3);
    box-shadow: 0 8px 18px rgba(59, 111, 224, 0.3);
  }

  strong {
    font-size: 15px;
    font-weight: 700;
    letter-spacing: -0.01em;
  }

  .step-desc {
    font-size: 12.5px;
    color: var(--muted);
  }
}

// ==================== 结尾 CTA ====================
.cta {
  margin: clamp(48px, 8vh, 96px) 0 clamp(40px, 6vh, 72px);
  padding: clamp(34px, 5vh, 60px) 24px;
  border-radius: 32px;
  text-align: center;
  background: linear-gradient(150deg, rgba(59, 111, 224, 0.12), rgba(59, 111, 224, 0.04));
  border: 1px solid rgba(255, 255, 255, 0.85);
  box-shadow: 0 24px 60px rgba(43, 79, 158, 0.12), inset 0 1px 0 rgba(255, 255, 255, 0.9);

  h2 {
    margin: 0;
    font-size: clamp(21px, 2vw, 30px);
    font-weight: 700;
    letter-spacing: -0.02em;
    color: #1b2745;
  }

  p {
    margin: 12px 0 0;
    font-size: 14.5px;
    color: #66708b;
  }
}

.cta-actions {
  margin-top: 26px;
  display: flex;
  justify-content: center;
  gap: 14px;
  flex-wrap: wrap;
}

.cta-chips {
  margin-top: 22px;
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;

  span {
    padding: 7px 15px;
    border-radius: 999px;
    font-size: 12.5px;
    font-weight: 600;
    color: var(--primary);
    background: rgba(255, 255, 255, 0.9);
    box-shadow: 0 8px 18px rgba(43, 79, 158, 0.1), inset 0 1px 0 #fff;
    transition: transform 0.4s $ease-apple, box-shadow 0.4s $ease-apple;

    &:hover {
      transform: translateY(-3px);
      box-shadow: 0 14px 28px rgba(43, 79, 158, 0.18);
    }
  }
}

// ==================== 页脚 ====================
.landing-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 26px clamp(20px, 4vw, 64px) 34px;
  font-size: 12.5px;
  color: var(--muted);
  border-top: 1px solid rgba(59, 111, 224, 0.1);
  scroll-margin-top: 130px;
}

// ==================== 响应式 ====================
@media (max-width: 1080px) {
  .nav-links {
    display: none;
  }

  .grid,
  .step-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 900px) {
  .hero {
    flex-direction: column;
  }

  .chip-1,
  .chip-2,
  .chip-3 {
    display: none;
  }

  .showcase {
    min-height: 460px;
  }
}

@media (max-width: 620px) {
  .landing-nav {
    gap: 12px;
  }

  .nav-brand .brand-name {
    display: none;
  }

  .grid,
  .step-row {
    grid-template-columns: 1fr;
  }

  .cta-actions {
    flex-direction: column;
  }
}
</style>