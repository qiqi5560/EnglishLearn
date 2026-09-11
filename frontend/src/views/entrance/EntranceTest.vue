<template>
  <div class="entrance-test">
    <AppHeader title="入学水平测试" back />

    <div class="test-body">
      <!-- 学习目标：影响方案与每日任务生成 -->
      <el-card shadow="never" class="goal-card">
        <div class="goal-row">
          <span class="text-muted">我的主要目标</span>
          <el-select v-model="targetGoal" style="width: 140px">
            <el-option v-for="g in goals" :key="g" :label="g" :value="g" />
          </el-select>
        </div>
      </el-card>

      <el-steps :active="step" finish-status="success" align-center>
        <el-step title="自我介绍" />
        <el-step title="看图描述" />
        <el-step title="自由问答" />
      </el-steps>

      <el-card shadow="never" class="question-card">
        <div class="question-prompt">
          <p class="text-muted">AI 教练提问（第 {{ step + 1 }}/3 题）</p>
          <p class="question-text">{{ questions[step] }}</p>
        </div>

        <!-- 看图描述：随机展示静态题库中的图片 -->
        <div v-if="step === 1 && imageItem" class="image-area">
          <el-image :src="imageItem.image" fit="cover" class="question-image">
            <template #error>
              <div class="image-error">
                <el-icon><Picture /></el-icon>
                <span>请把图片放入 public/entrance-images，并在 data/entranceImages.ts 中配置</span>
              </div>
            </template>
          </el-image>
        </div>

        <el-input
          v-model="answers[step]"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="请用英文输入你的回答（或点击下方麦克风直接开口作答）"
        />

        <div class="record-area">
          <div class="record-wave">
            <AudioWave :recording="isListening" />
          </div>
          <el-button
            type="primary"
            circle
            size="large"
            class="record-btn"
            :class="{ recording: isListening }"
            @click="onToggleRecord"
          >
            <el-icon :size="24"><Microphone /></el-icon>
          </el-button>
          <p class="text-muted">{{ isListening ? '正在聆听，请用英语回答…' : '点击录音 / 使用文字输入' }}</p>
        </div>
      </el-card>

      <div class="test-actions">
        <el-button v-if="step > 0" @click="step--">上一步</el-button>
        <el-button v-else disabled style="visibility: hidden">上一步</el-button>
        <el-button v-if="step < 2" type="primary" @click="step++">下一题</el-button>
        <el-button v-else type="success" :loading="submitting" @click="onSubmit">提交测试</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
import AudioWave from '@/components/business/AudioWave.vue'
import { usePlanStore } from '@/stores/plan'
import { entranceImages, type EntranceImageItem } from '@/data/entranceImages'
import { useSpeech } from '@/composables/useSpeech'

const router = useRouter()
const planStore = usePlanStore()

const step = ref(0)
const submitting = ref(false)
const targetGoal = ref('兴趣')
const goals = ['考试', '商务', '出国', '兴趣']

const { isListening, recognitionSupported, start, stop } = useSpeech()

// 随机抽取一张「看图描述」图片与对应问题
const imageItem = ref<EntranceImageItem | null>(null)
if (entranceImages.length > 0) {
  imageItem.value = entranceImages[Math.floor(Math.random() * entranceImages.length)]
}

const questions = computed(() => [
  '请用英语简单介绍一下你自己。',
  imageItem.value?.question ?? '请用英语描述图片中的场景与细节。',
  '你周末喜欢做什么？为什么？',
])

const answers = ref<string[]>(['', '', ''])

function currentBlank() {
  for (let i = 0; i < answers.value.length; i += 1) {
    if (!answers.value[i]?.trim()) return i
  }
  return -1
}

function onToggleRecord() {
  if (isListening.value) {
    stop()
    return
  }
  if (!recognitionSupported.value) {
    ElMessage.warning('当前浏览器不支持语音识别，请使用 Chrome / Edge')
    return
  }
  const ok = start((text) => {
    const cur = answers.value[step.value]?.trim()
    answers.value[step.value] = cur ? `${cur} ${text}` : text
  })
  if (!ok) ElMessage.warning('无法启动语音识别，请改用文字输入')
}

async function onSubmit() {
  const blank = currentBlank()
  if (blank >= 0) {
    ElMessage.warning(`请先完成第 ${blank + 1} 题的回答`)
    step.value = blank
    return
  }
  submitting.value = true
  try {
    const res = await planStore.submitEntranceTest(
      answers.value.map((text, id) => ({ id: id + 1, text: text.trim() })),
      targetGoal.value,
    )
    ElMessage.success(`测评完成，当前水平 ${res.level}`)
    router.push('/entrance-test/result')
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.test-body {
  padding: 16px;
}

.goal-card {
  margin-bottom: 16px;
  border-radius: var(--radius-md);

  .goal-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
}

.question-card {
  margin-top: 20px;
  border-radius: var(--radius-md);

  .question-text {
    font-size: 16px;
    font-weight: 600;
  }
}

.image-area {
  margin: 12px 0;

  .question-image {
    width: 100%;
    height: 200px;
    border-radius: var(--radius-md);
  }

  .image-error {
    width: 100%;
    height: 200px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    color: var(--muted);
    font-size: 13px;
    background: var(--bg-soft, #f5f7fb);
    border-radius: var(--radius-md);
  }
}

.record-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px 0 8px;
}

.record-wave {
  width: 100%;
  max-width: 280px;
}

.record-btn {
  width: 60px;
  height: 60px;
  background: var(--accent);
  border-color: var(--accent);

  &.recording {
    animation: pulse 1.2s infinite;
  }
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(255, 106, 77, 0.5);
  }
  70% {
    box-shadow: 0 0 0 16px rgba(255, 106, 77, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(255, 106, 77, 0);
  }
}

.test-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
}
</style>