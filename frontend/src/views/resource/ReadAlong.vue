<template>
  <div class="read-along page">
    <AppHeader title="跟读训练" back />

    <main class="page-shell">
      <div class="read-grid">
        <!-- 左侧：主训练区 -->
        <section class="read-main">
          <div class="origin-card glass-card peek-host">
            <div class="peek-slot"><PeekMascot :size="48" /></div>
            <p class="card-label">原声例句</p>
            <p class="reading-text origin-text">{{ sentences[current] }}</p>
            <el-button link type="primary" class="play-link" @click="playOriginal">
              <el-icon><VideoPlay /></el-icon>&nbsp;播放原声
            </el-button>
          </div>

          <div class="record-card glass-card" :class="{ recording }">
            <AudioWave :recording="recording" />
            <el-button type="primary" circle size="large" class="record-btn" @click="onRecord">
              <el-icon :size="24"><Microphone /></el-icon>
            </el-button>
            <p class="record-hint text-muted">{{ recording ? '跟读中…' : '点击跟读本句' }}</p>

            <div class="nav-btns">
              <el-button :disabled="current === 0" @click="current--">上一句</el-button>
              <el-button type="primary" :disabled="current === sentences.length - 1" @click="current++">下一句</el-button>
            </div>
          </div>
        </section>

        <!-- 右侧：评分 / 进度栏 -->
        <aside class="read-side">
          <div class="side-card glass-card hover-lift">
            <p class="card-label">本句评分</p>
            <div class="sentence-scores">
              <ScoreRing v-for="d in scores" :key="d.label" :score="d.score" :label="d.label" :size="68" :stroke="6" />
            </div>
          </div>

          <div class="side-card glass-card">
            <p class="card-label">练习进度</p>
            <p class="progress-text">{{ current + 1 }} / {{ sentences.length }}</p>
            <span class="progress-track"><i :style="{ width: `${((current + 1) / sentences.length) * 100}%` }" /></span>
            <p class="side-tip text-muted">先听原声，再逐句跟读。评分维度包含发音与流利度。</p>
          </div>
        </aside>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
import PeekMascot from '@/components/base/PeekMascot.vue'
import AudioWave from '@/components/business/AudioWave.vue'
import ScoreRing from '@/components/base/ScoreRing.vue'

const current = ref(0)
const recording = ref(false)

const sentences = [
  'Good evening! Welcome to our restaurant.',
  'How many people are there in your party?',
  'Would you like something to drink first?',
]

const scores = [
  { label: '发音', score: 78 },
  { label: '流利度', score: 74 },
]

function playOriginal() {
  ElMessage.info('播放原声（接入 TTS 后实现）')
}

function onRecord() {
  recording.value = !recording.value
}
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

// ---------------- 桌面两栏 ----------------
.read-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 24px;
  align-items: start;
}

.read-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

// ---------------- 原声例句 ----------------
.origin-card {
  position: relative;
  padding: 34px 32px 30px;
}

.peek-slot {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
}

.card-label {
  margin: 0 0 10px;
  color: var(--muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.origin-text {
  margin: 0 0 6px;
  color: var(--ink);
  font-size: clamp(20px, 1.8vw, 26px);
  font-weight: 600;
}

.play-link {
  font-size: 13.5px;
  font-weight: 600;
}

// ---------------- 录音区 ----------------
.record-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 30px 28px;
}

.record-btn.el-button {
  width: 68px;
  height: 68px;
  background: var(--accent);
  border-color: var(--accent);
  box-shadow: 0 14px 30px rgba(255, 106, 77, 0.32);
  transition:
    transform 0.35s $ease-apple,
    box-shadow 0.35s ease;

  &:hover {
    transform: translateY(-2px) scale(1.05);
    box-shadow: 0 20px 40px rgba(255, 106, 77, 0.42);
  }
}

.record-card.recording .record-btn {
  animation: record-pulse 1.6s ease-in-out infinite;
}

@keyframes record-pulse {
  0%,
  100% {
    box-shadow: 0 14px 30px rgba(255, 106, 77, 0.32);
  }
  50% {
    box-shadow: 0 14px 30px rgba(255, 106, 77, 0.32), 0 0 0 14px rgba(255, 106, 77, 0.12);
  }
}

.record-hint {
  margin: 0;
  font-size: 13px;
}

.nav-btns {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  margin-top: 8px;
  padding-top: 20px;
  border-top: 1px solid var(--border);
}

// ---------------- 右侧栏 ----------------
.read-side {
  position: sticky;
  top: 84px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.side-card {
  padding: 24px 20px 22px;
}

.sentence-scores {
  display: flex;
  justify-content: center;
  gap: 26px;
  margin-top: 6px;
}

.progress-text {
  margin: 0 0 12px;
  color: var(--primary);
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}

.progress-track {
  display: block;
  height: 7px;
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

.side-tip {
  margin: 16px 0 0;
  padding-top: 14px;
  font-size: 12.5px;
  line-height: 1.7;
  border-top: 1px solid var(--border);
}

// ---------------- 响应式 ----------------
@media (max-width: 1000px) {
  .read-grid {
    grid-template-columns: 1fr;
  }

  .read-side {
    position: static;
  }

  .peek-slot {
    display: none;
  }
}
</style>