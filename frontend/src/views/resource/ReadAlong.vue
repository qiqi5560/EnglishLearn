<template>
  <div class="read-along">
    <AppHeader title="跟读训练" back />

    <div class="read-body">
      <el-card shadow="never">
        <div class="original">
          <p class="text-muted">原声例句</p>
          <p class="reading-text origin-text">{{ sentences[current] }}</p>
          <el-button link type="primary" @click="playOriginal">
            <el-icon><VideoPlay /></el-icon>&nbsp;播放原声
          </el-button>
        </div>
      </el-card>

      <div class="record-section">
        <AudioWave :recording="recording" />
        <el-button type="primary" circle size="large" class="record-btn" @click="onRecord">
          <el-icon :size="24"><Microphone /></el-icon>
        </el-button>
        <p class="text-muted">{{ recording ? '跟读中…' : '点击跟读本句' }}</p>
      </div>

      <div class="section-title">本句评分</div>
      <div class="sentence-scores">
        <ScoreRing v-for="d in scores" :key="d.label" :score="d.score" :label="d.label" :size="52" :stroke="5" />
      </div>

      <div class="nav-btns">
        <el-button :disabled="current === 0" @click="current--">上一句</el-button>
        <el-button type="primary" :disabled="current === sentences.length - 1" @click="current++">下一句</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
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
.read-body {
  padding: 16px;
}

.origin-text {
  font-size: 18px;
  margin: 8px 0;
}

.record-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px 0;
}

.record-btn {
  width: 60px;
  height: 60px;
  background: var(--accent);
  border-color: var(--accent);
}

.sentence-scores {
  display: flex;
  justify-content: center;
  gap: 24px;
}

.nav-btns {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
}
</style>
