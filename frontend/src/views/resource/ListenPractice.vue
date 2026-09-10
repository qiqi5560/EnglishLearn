<template>
  <div class="listen-practice">
    <AppHeader title="精听训练" back />

    <div class="listen-body">
      <!-- 播放控制 -->
      <el-card shadow="never">
        <div class="controls">
          <el-button type="primary" circle :icon="playing ? 'VideoPause' : 'VideoPlay'" @click="playing = !playing" />
          <el-slider v-model="speed" :min="0.5" :max="2" :step="0.25" style="flex: 1" />
          <span class="text-muted">{{ speed }}x</span>
        </div>
        <div class="switch-row">
          <span>双语字幕</span>
          <el-switch v-model="showSubtitle" />
          <span>听写挖空</span>
          <el-switch v-model="showBlank" />
        </div>
      </el-card>

      <!-- 逐句字幕 -->
      <div class="section-title">逐句精听</div>
      <el-card shadow="never">
        <div
          v-for="(sentence, i) in sentences"
          :key="i"
          class="sentence"
          :class="{ active: current === i }"
          @click="current = i"
        >
          <span v-if="showBlank" class="reading-text">
            {{ showSubtitle ? blankSentence(sentence) : sentence }}
          </span>
          <span v-else class="reading-text">{{ sentence }}</span>
          <span v-if="showSubtitle" class="sentence-zh text-muted">{{ zh[i] }}</span>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AppHeader from '@/components/base/AppHeader.vue'

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
.listen-body {
  padding: 16px;
}

.controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.switch-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  font-size: 13px;
}

.sentence {
  padding: 10px 4px;
  border-bottom: 1px solid var(--border);
  cursor: pointer;
  border-radius: 4px;

  &.active {
    background: var(--el-color-primary-light-9);
  }

  &:last-child {
    border-bottom: none;
  }
}

.sentence-zh {
  display: block;
  font-size: 13px;
  margin-top: 4px;
}
</style>
