<template>
  <div class="listen-practice page">
    <AppHeader title="精听训练" back />

    <main class="page-shell">
      <div class="listen-grid">
        <!-- 左侧：主训练区 -->
        <section class="listen-main">
          <div class="player-card glass-card">
            <div class="controls">
              <el-button type="primary" circle :icon="playing ? 'VideoPause' : 'VideoPlay'" class="play-btn" @click="playing = !playing" />
              <el-slider v-model="speed" :min="0.5" :max="2" :step="0.25" class="speed-slider" />
              <span class="speed-value">{{ speed }}x</span>
            </div>
            <div class="switch-row">
              <span class="switch-item"><span>双语字幕</span><el-switch v-model="showSubtitle" /></span>
              <span class="switch-item"><span>听写挖空</span><el-switch v-model="showBlank" /></span>
            </div>
          </div>

          <div class="sentence-head">
            <h2 class="sentence-title">逐句精听</h2>
            <span class="text-muted">{{ sentences.length }} 句</span>
          </div>

          <div class="sentence-list stagger">
            <button v-for="(sentence, i) in sentences" :key="i" type="button" class="sentence" :class="{ active: current === i }" @click="current = i">
              <span class="sentence-index">{{ String(i + 1).padStart(2, '0') }}</span>
              <span class="sentence-body">
                <span v-if="showBlank" class="reading-text">{{ showSubtitle ? blankSentence(sentence) : sentence }}</span>
                <span v-else class="reading-text">{{ sentence }}</span>
                <span v-if="showSubtitle" class="sentence-zh text-muted">{{ zh[i] }}</span>
              </span>
              <el-icon class="sentence-play"><VideoPlay /></el-icon>
            </button>
          </div>
        </section>

        <!-- 右侧：信息 / 操作栏 -->
        <aside class="listen-side">
          <div class="side-card glass-card peek-host">
            <div class="peek-slot"><PeekMascot :size="44" /></div>
            <p class="side-label">当前句子</p>
            <p class="side-sentence reading-text">{{ sentences[current] }}</p>
            <p class="side-zh text-muted">{{ zh[current] }}</p>
            <div class="progress-row">
              <span class="progress-text">{{ current + 1 }} / {{ sentences.length }}</span>
              <span class="progress-track"><i :style="{ width: `${((current + 1) / sentences.length) * 100}%` }" /></span>
            </div>
          </div>

          <div class="side-card glass-card hover-lift">
            <p class="side-label">播放设置</p>
            <ul class="setting-list">
              <li><span>倍速</span><strong>{{ speed }}x</strong></li>
              <li><span>双语字幕</span><strong>{{ showSubtitle ? '已开启' : '已关闭' }}</strong></li>
              <li><span>听写挖空</span><strong>{{ showBlank ? '已开启' : '已关闭' }}</strong></li>
            </ul>
            <p class="side-tip text-muted">点击左侧任意句子即可定位精听，打开挖空模式可做听写自测。</p>
          </div>
        </aside>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AppHeader from '@/components/base/AppHeader.vue'
import PeekMascot from '@/components/base/PeekMascot.vue'

const playing = ref(false)
const speed = ref(1)
const showSubtitle = ref(true)
const showBlank = ref(false)
const current = ref(0)

const sentences = [
  'Good evening! Welcome to our restaurant.',
  'How many people are there in your party?',
  'Would you like something to drink first?',
  'Today we have a special on grilled salmon.',
]
const zh = ['晚上好！欢迎光临本餐厅。', '请问一共有几位？', '先来点喝的吗？', '今天我们有一道烤三文鱼特色菜。']

function blankSentence(s: string) {
  return s.replace(/\b\w{4,}\b/g, '____')
}
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

// ---------------- 桌面两栏 ----------------
.listen-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 24px;
  align-items: start;
}

.listen-main {
  min-width: 0;
}

// ---------------- 播放控制 ----------------
.player-card {
  padding: 20px 24px;
}

.controls {
  display: flex;
  align-items: center;
  gap: 18px;
}

.play-btn.el-button {
  width: 52px;
  height: 52px;
  font-size: 20px;
  background: var(--primary);
  border-color: var(--primary);
  box-shadow: 0 12px 26px rgba(59, 111, 224, 0.3);
  transition:
    transform 0.35s $ease-apple,
    box-shadow 0.35s ease;

  &:hover {
    transform: translateY(-2px) scale(1.04);
    box-shadow: 0 18px 34px rgba(59, 111, 224, 0.4);
  }
}

.speed-slider {
  flex: 1;
  min-width: 0;

  :deep(.el-slider__bar) {
    background: var(--primary);
  }

  :deep(.el-slider__button) {
    border-color: var(--primary);
  }
}

.speed-value {
  flex: 0 0 auto;
  min-width: 46px;
  color: var(--primary);
  font-size: 15px;
  font-weight: 700;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.switch-row {
  display: flex;
  align-items: center;
  gap: 28px;
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid var(--border);
}

.switch-item {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--ink);
  font-size: 13.5px;
}

// ---------------- 逐句列表 ----------------
.sentence-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin: 28px 0 14px;
}

.sentence-title {
  margin: 0;
  color: var(--ink);
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.sentence-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sentence {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  width: 100%;
  padding: 16px 20px;
  color: var(--ink);
  font: inherit;
  text-align: left;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition:
    transform 0.35s $ease-apple,
    border-color 0.3s ease,
    background 0.3s ease,
    box-shadow 0.35s ease;

  &:hover {
    transform: translateX(4px);
    border-color: rgba(59, 111, 224, 0.28);
    box-shadow: 0 12px 26px rgba(31, 42, 68, 0.09);

    .sentence-play {
      opacity: 1;
      transform: translateX(0);
    }
  }

  &.active {
    background: rgba(59, 111, 224, 0.07);
    border-color: rgba(59, 111, 224, 0.45);
    box-shadow: 0 14px 30px rgba(59, 111, 224, 0.14);

    .sentence-index {
      color: var(--primary);
    }

    .sentence-play {
      opacity: 1;
      transform: translateX(0);
    }
  }

  &:focus-visible {
    outline: 2px solid var(--primary);
    outline-offset: 2px;
  }
}

.sentence-index {
  flex: 0 0 auto;
  padding-top: 3px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  transition: color 0.3s ease;
}

.sentence-body {
  display: block;
  flex: 1;
  min-width: 0;

  .reading-text {
    font-size: 17px;
  }
}

.sentence-zh {
  display: block;
  margin-top: 5px;
  font-size: 13px;
}

.sentence-play {
  flex: 0 0 auto;
  margin-top: 2px;
  color: var(--primary);
  font-size: 16px;
  opacity: 0;
  transform: translateX(-4px);
  transition:
    opacity 0.3s ease,
    transform 0.45s $ease-spring;
}

// ---------------- 右侧信息栏 ----------------
.listen-side {
  position: sticky;
  top: 84px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.side-card {
  position: relative;
  padding: 24px 20px 20px;
}

.peek-slot {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
}

.side-label {
  margin: 0 0 10px;
  color: var(--muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.side-sentence {
  margin: 0;
  color: var(--ink);
  font-size: 16px;
}

.side-zh {
  margin: 6px 0 0;
  font-size: 13px;
}

.progress-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 18px;
}

.progress-text {
  flex: 0 0 auto;
  color: var(--primary);
  font-size: 12.5px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.progress-track {
  flex: 1;
  height: 6px;
  overflow: hidden;
  background: rgba(59, 111, 224, 0.14);
  border-radius: 999px;

  i {
    display: block;
    height: 100%;
    background: var(--primary);
    border-radius: 999px;
    transition: width 0.45s $ease-apple;
  }
}

.setting-list {
  display: flex;
  flex-direction: column;
  gap: 11px;
  margin: 0;
  padding: 0;
  list-style: none;

  li {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 13.5px;
  }

  span {
    color: var(--muted);
  }

  strong {
    color: var(--ink);
    font-weight: 600;
  }
}

.side-tip {
  margin: 16px 0 0;
  padding-top: 14px;
  font-size: 12.5px;
  line-height: 1.7;
  border-top: 1px solid var(--border);
}

// ---------------- 响应式 ----------------
@media (max-width: 1000px) {
  .listen-grid {
    grid-template-columns: 1fr;
  }

  .listen-side {
    position: static;
  }

  .peek-slot {
    display: none;
  }
}
</style>