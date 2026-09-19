<template>
  <div class="entrance-test page">
    <AppHeader title="入学水平测试" back>
      <template #right>
        <span class="step-chip">第 {{ step + 1 }} / 3 题</span>
      </template>
    </AppHeader>

    <div class="page-shell">
      <header class="test-hero">
        <div class="hero-copy">
          <h2>三题定级，找到你的起点</h2>
          <p class="text-muted">回答三个问题，AI 教练会给出 A1–C2 等级评价，并据此生成专属学习方案。</p>
        </div>
        <div class="goal-field">
          <span class="goal-label text-muted">我的主要目标</span>
          <el-select v-model="targetGoal" style="width: 156px">
            <el-option v-for="g in goals" :key="g" :label="g" :value="g" />
          </el-select>
        </div>
      </header>

      <el-steps :active="step" finish-status="success" align-center class="test-steps">
        <el-step title="自我介绍" />
        <el-step title="看图描述" />
        <el-step title="自由问答" />
      </el-steps>

<<<<<<< HEAD
      <div class="test-layout">
        <!-- 左栏：题目区 -->
        <section class="question-pane">
          <div class="glass-card sheen question-card peek-host">
            <PeekMascot :size="46" />

            <div class="question-prompt">
              <p class="prompt-tag text-muted">AI 教练提问 · 第 {{ step + 1 }}/3 题</p>
              <p class="question-text">{{ questions[step] }}</p>
            </div>
=======
      <el-card shadow="never" class="question-card">
        <div class="question-prompt">
          <p class="text-muted">AI 教练提问（第 {{ step + 1 }}/3 题）</p>
          <div class="question-line">
            <p class="question-text">{{ questions[step] }}</p>
            <el-button link type="primary" title="朗读题目" @click="speak(questions[step])">
              <el-icon :size="18"><Headset /></el-icon>
            </el-button>
          </div>
        </div>

        <!-- 看图描述：随机展示静态题库中的图片 -->
        <div v-if="step === 1 && imageItem" class="image-area">
          <el-image :src="imageItem.image" fit="contain" class="question-image">
            <template #error>
              <div class="image-error">
                <el-icon><Picture /></el-icon>
                <span>请把图片放入 public/entrance-images，并在 data/entranceImages.ts 中配置</span>
              </div>
            </template>
          </el-image>
        </div>
>>>>>>> f49b35abb4d8abb721b99c4328b673b2b8bd9129

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

            <div class="answer-label">
              <span>我的回答</span>
              <span class="text-muted">可用英文输入，也可点右侧麦克风开口作答</span>
            </div>

            <el-input
              v-model="answers[step]"
              type="textarea"
              :rows="7"
              resize="none"
              placeholder="请用英文输入你的回答（或点击右侧麦克风直接开口作答）"
            />
          </div>
        </section>

        <!-- 右栏：录音与操作 -->
        <aside class="side-pane stagger">
          <div class="glass-card record-card">
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
              <el-icon :size="26"><Microphone /></el-icon>
            </el-button>
            <p class="record-hint text-muted">
              {{ isListening ? '正在聆听，请用英语回答…' : '点击录音 / 使用文字输入' }}
            </p>
          </div>

          <div class="glass-card hover-lift tips-card">
            <div class="tips-title">作答小贴士</div>
            <ul class="tips-list">
              <li v-for="t in tips" :key="t">{{ t }}</li>
            </ul>
          </div>

          <div class="test-actions">
            <el-button v-if="step > 0" @click="step--">上一步</el-button>
            <el-button v-else disabled style="visibility: hidden">上一步</el-button>
            <el-button v-if="step < 2" type="primary" @click="step++">下一题</el-button>
            <el-button v-else type="success" :loading="submitting" @click="onSubmit">提交测试</el-button>
          </div>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
import AudioWave from '@/components/business/AudioWave.vue'
import PeekMascot from '@/components/base/PeekMascot.vue'
import { usePlanStore } from '@/stores/plan'
import { entranceImages, type EntranceImageItem } from '@/data/entranceImages'
import { useSpeech } from '@/composables/useSpeech'

const router = useRouter()
const planStore = usePlanStore()

const step = ref(0)
const submitting = ref(false)
const targetGoal = ref('兴趣')
const goals = ['考试', '商务', '出国', '兴趣']

<<<<<<< HEAD
// 纯展示文案
const tips = [
  '尽量用完整句子回答，评价结果会更准确',
  '不确定时先在心里构思，再开口说出来',
  '录音需要 Chrome / Edge 浏览器支持',
]

const { isListening, recognitionSupported, start, stop } = useSpeech()
=======
const { isListening, recognitionSupported, start, stop, speak } = useSpeech()
>>>>>>> f49b35abb4d8abb721b99c4328b673b2b8bd9129

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

// 切换题目时自动朗读，也可点小喇叭手动重听
watch(step, () => speak(questions.value[step.value]))
onMounted(() => speak(questions.value[0]))

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
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

.step-chip {
  padding: 7px 16px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  color: var(--primary);
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(31, 42, 68, 0.08);
  box-shadow: var(--shadow-sm);
}

// ---------------- 页头 ----------------
.test-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 28px;
  margin-bottom: 26px;

  h2 {
    margin: 0;
    font-size: 32px;
    font-weight: 700;
    letter-spacing: -0.03em;
  }

  p {
    margin: 10px 0 0;
    font-size: 14.5px;
    max-width: 520px;
  }
}

.goal-field {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px 10px 18px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-sm);

  .goal-label {
    font-size: 13px;
    font-weight: 600;
    white-space: nowrap;
  }
}

.test-steps {
  margin-bottom: 28px;
  padding: 20px 26px;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.68);
  border: 1px solid rgba(31, 42, 68, 0.06);
  backdrop-filter: blur(14px) saturate(150%);
}

// ---------------- 两栏布局 ----------------
.test-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 336px;
  gap: 26px;
  align-items: start;
}

.question-pane {
  min-width: 0;
}

.question-card {
  position: relative;
  padding: 26px 30px 30px;

<<<<<<< HEAD
  .question-prompt {
    padding-bottom: 18px;
    border-bottom: 1px solid var(--border);
=======
  .question-text {
    font-size: 16px;
    font-weight: 600;
  }

  .question-line {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}
>>>>>>> f49b35abb4d8abb721b99c4328b673b2b8bd9129

    .prompt-tag {
      margin: 0;
      font-size: 12.5px;
      font-weight: 600;
      letter-spacing: 0.04em;
      text-transform: uppercase;
    }

<<<<<<< HEAD
    .question-text {
      margin: 10px 0 0;
      font-size: 22px;
      font-weight: 700;
      line-height: 1.5;
      letter-spacing: -0.02em;
    }
  }

  .image-area {
    margin: 20px 0;

    .question-image {
      width: 100%;
      height: 280px;
      border-radius: var(--radius-md);
      overflow: hidden;
      box-shadow: var(--shadow-md);
    }

    .image-error {
      width: 100%;
      height: 280px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 8px;
      padding: 0 24px;
      text-align: center;
      color: var(--muted);
      font-size: 13px;
      background: var(--surface);
      border-radius: var(--radius-md);
    }
  }

  .answer-label {
=======
  // 高度随屏幕伸缩，小屏不撑爆、大屏不过矮
  $img-height: clamp(180px, 32vh, 340px);

  .question-image {
    width: 100%;
    height: $img-height;
    border-radius: var(--radius-md);
    background: var(--bg-soft, #f5f7fb);
  }

  .image-error {
    width: 100%;
    height: $img-height;
>>>>>>> f49b35abb4d8abb721b99c4328b673b2b8bd9129
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 16px;
    margin: 20px 0 10px;

    span:first-child {
      font-size: 15px;
      font-weight: 700;
      letter-spacing: -0.01em;
    }

    span:last-child {
      font-size: 12.5px;
    }
  }

  :deep(.el-textarea__inner) {
    border-radius: var(--radius-md);
    line-height: 1.75;
    font-size: 14.5px;
    padding: 14px 16px;
    background: rgba(255, 255, 255, 0.85);
  }
}

// ---------------- 右栏 ----------------
.side-pane {
  display: flex;
  flex-direction: column;
  gap: 18px;
  position: sticky;
  top: 96px;
}

.record-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 26px 22px;
  text-align: center;

  .record-wave {
    width: 100%;
    max-width: 240px;
  }

  .record-hint {
    margin: 0;
    font-size: 13px;
  }
}

.record-btn {
  width: 64px;
  height: 64px;
  background: var(--accent);
  border-color: var(--accent);
  transition:
    transform 0.4s $ease-spring,
    box-shadow 0.4s $ease-apple;

  &:hover {
    transform: scale(1.06);
    box-shadow: 0 14px 28px rgba(255, 106, 77, 0.32);
  }

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

.tips-card {
  padding: 20px 22px;

  .tips-title {
    font-size: 15px;
    font-weight: 700;
    letter-spacing: -0.01em;
    margin-bottom: 12px;
  }

  .tips-list {
    margin: 0;
    padding: 0;
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: 10px;

    li {
      position: relative;
      padding-left: 20px;
      font-size: 13px;
      line-height: 1.65;
      color: var(--muted);

      &::before {
        content: '';
        position: absolute;
        left: 2px;
        top: 7px;
        width: 7px;
        height: 7px;
        border-radius: 50%;
        background: linear-gradient(135deg, var(--primary), var(--accent));
      }
    }
  }
}

.test-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-top: 4px;

  .el-button {
    flex: 1;
    margin-left: 0;
    border-radius: 999px;
  }
}

// ---------------- 窄屏兜底 ----------------
@media (max-width: 900px) {
  .test-hero {
    flex-direction: column;
    align-items: flex-start;

    h2 {
      font-size: 24px;
    }
  }

  .test-layout {
    grid-template-columns: 1fr;
  }

  .side-pane {
    position: static;
  }

  .question-card {
    padding: 20px;

    .question-prompt .question-text {
      font-size: 18px;
    }
  }
}
</style>