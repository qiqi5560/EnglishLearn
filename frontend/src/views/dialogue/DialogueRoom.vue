<template>
  <div class="dialogue-room page">
    <AppHeader :title="sessionStore.sceneName || '对话练习'">
      <template #right>
        <el-button link :type="autoSpeak ? 'primary' : 'info'" title="AI 回复后自动朗读" @click="toggleAutoSpeak">
          <el-icon :size="16"><Headset /></el-icon>
          {{ isSpeaking ? '朗读中' : autoSpeak ? '朗读开' : '朗读关' }}
        </el-button>
        <el-button link type="danger" :disabled="sessionStore.status === 'finished'" @click="onEnd">结束</el-button>
      </template>
    </AppHeader>

    <div v-loading="loading" class="page-shell room-shell">
      <div class="room-grid">
        <!-- ==================== 左栏：数字人 / 实时评分 / 场景信息 ==================== -->
        <aside class="room-rail">
          <section class="rail-card human-card glass-card hover-lift">
            <DigitalHuman role="AI 陪练" />
          </section>

          <!-- 实时四维评分（F003） -->
          <section class="rail-card score-card glass-card">
            <div class="rail-title">实时评分</div>
            <div class="score-grid">
              <ScoreRing
                v-for="d in dimScores"
                :key="d.label"
                :score="d.score"
                :label="d.label"
                :size="70"
                :stroke="6"
              />
            </div>
            <el-tag v-if="sessionStore.evaluating" size="small" type="info" effect="plain" class="eval-tag">
              评分中…
            </el-tag>
          </section>

          <section class="rail-card scene-card glass-card">
            <div class="rail-title">本场景信息</div>
            <div class="scene-row">
              <span class="text-muted">场景</span>
              <span class="scene-value">{{ sessionStore.sceneName || '自由对话' }}</span>
            </div>
            <div class="scene-row">
              <span class="text-muted">状态</span>
              <span class="scene-value">{{ statusText }}</span>
            </div>
            <div class="scene-row">
              <span class="text-muted">对话消息</span>
              <span class="scene-value">{{ sessionStore.messages.length }} 条</span>
            </div>
          </section>
        </aside>

        <!-- ==================== 中栏：消息流 + 输入条 ==================== -->
        <section class="room-main">
          <div class="chat-card glass-card sheen">
            <div class="chat-head">
              <div class="chat-head-text">
                <h2 class="chat-title">{{ sessionStore.sceneName || '自由对话' }}</h2>
                <span class="chat-sub text-muted">多轮英文对话 · 每轮更新四维实时评分</span>
              </div>
              <span class="chat-state" :class="{ 'is-finished': sessionStore.status === 'finished' }">
                {{ statusText }}
              </span>
            </div>

            <div ref="chatPanel" class="chat-panel">
              <ChatBubble
                v-for="msg in sessionStore.messages"
                :key="msg.id"
                :speaker="msg.speaker"
                :content-en="msg.contentEn"
                :content-zh="msg.contentZh || undefined"
                :time="displayTime(msg.time)"
                @play="onPlay(msg)"
              />
              <div v-if="sessionStore.sending" class="typing text-muted">AI 正在回复…</div>
              <el-empty v-if="!loading && !sessionStore.sending && !sessionStore.messages.length" description="还没有对话消息" />
            </div>
          </div>

          <div class="input-area">
            <div class="input-bar glass-card">
              <el-input
                v-model="draft"
                class="text-input"
                placeholder="输入英文，或点击麦克风开口说话…"
                :disabled="sessionStore.sending || sessionStore.status === 'finished'"
                @keyup.enter="onSend"
              />
              <AudioWave v-if="isListening" class="mic-wave" :recording="true" />
              <el-button
                circle
                class="voice-btn"
                :class="{ listening: isListening }"
                :disabled="sessionStore.status === 'finished'"
                @click="onMicClick"
              >
                <el-icon :size="22"><Microphone /></el-icon>
              </el-button>
              <el-button
                type="primary"
                circle
                class="send-btn"
                :loading="sessionStore.sending"
                :disabled="sessionStore.status === 'finished'"
                @click="onSend"
              >
                <el-icon :size="22"><Promotion /></el-icon>
              </el-button>
            </div>
            <p class="input-hint text-muted">按 Enter 发送 · 点击麦克风可用语音说出你的回答</p>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
import DigitalHuman from '@/components/business/DigitalHuman.vue'
import ChatBubble from '@/components/business/ChatBubble.vue'
import AudioWave from '@/components/business/AudioWave.vue'
import ScoreRing from '@/components/base/ScoreRing.vue'
import { useSessionStore } from '@/stores/session'
import { useSpeech } from '@/composables/useSpeech'
import type { ChatMessageDto } from '@/types/api'

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()

const draft = ref('')
const loading = ref(false)
const autoSpeak = ref(true)
const lastPlayedId = ref<number | null>(null)
const chatPanel = ref<HTMLElement | null>(null)
const { isListening, isSpeaking, recognitionSupported, start, stop, speak, stopSpeaking } = useSpeech()

const dimScores = computed(() => [
  { label: '发音', score: sessionStore.liveScores.pron },
  { label: '流利度', score: sessionStore.liveScores.fluency },
  { label: '反应', score: sessionStore.liveScores.reaction },
  { label: '自然度', score: sessionStore.liveScores.natural },
])

/** 纯展示：会话状态文案 */
const statusText = computed(() => {
  if (sessionStore.status === 'finished') return '已结束'
  return sessionStore.sending ? 'AI 回复中' : '进行中'
})

function displayTime(t?: string) {
  if (!t) return ''
  return t.length >= 16 ? t.slice(11, 16) : t
}

function scrollToBottom() {
  nextTick(() => {
    if (chatPanel.value) chatPanel.value.scrollTop = chatPanel.value.scrollHeight
  })
}

async function onSend() {
  const text = draft.value.trim()
  if (!text) {
    ElMessage.warning('请输入要说的话')
    return
  }
  if (sessionStore.sending || sessionStore.status === 'finished') return
  draft.value = ''
  try {
    await sessionStore.send(text)
  } catch {
    draft.value = text
  }
}

function onPlay(msg: ChatMessageDto) {
  speak(msg.contentEn)
}

/** AI 回复后自动朗读最新一条，同一条只播一次 */
function autoPlayLatest() {
  if (!autoSpeak.value) return
  const list = sessionStore.messages
  if (!list.length) return
  const last = list[list.length - 1]
  if (last.speaker !== 'ai' || !last.contentEn?.trim()) return
  if (lastPlayedId.value === last.id) return
  lastPlayedId.value = last.id
  speak(last.contentEn)
}

function toggleAutoSpeak() {
  autoSpeak.value = !autoSpeak.value
  if (autoSpeak.value) {
    lastPlayedId.value = null
    autoPlayLatest()
  } else {
    stopSpeaking()
  }
  ElMessage.success(autoSpeak.value ? '已开启自动朗读' : '已关闭自动朗读')
}

function onMicClick() {
  if (isListening.value) {
    stop()
    return
  }
  if (sessionStore.sending || sessionStore.status === 'finished') return
  if (!recognitionSupported.value) {
    ElMessage.warning('当前浏览器不支持语音识别，请使用 Chrome / Edge')
    return
  }
  const ok = start((text) => {
    draft.value = text
    onSend()
  })
  if (!ok) ElMessage.warning('无法启动语音识别，请改用文字输入')
}

function onEnd() {
  ElMessageBox.confirm('确定结束本次练习并查看小结吗？', '结束练习', {
    confirmButtonText: '结束并查看小结',
    cancelButtonText: '继续练习',
  })
    .then(async () => {
      try {
        await sessionStore.finish()
        router.replace(`/dialogue/${sessionStore.sessionId}/summary`)
      } catch {
        /* 错误提示已由请求层统一弹出 */
      }
    })
    .catch(() => {})
}

watch(
  () => sessionStore.messages.length,
  () => {
    scrollToBottom()
    autoPlayLatest()
  },
)

onMounted(async () => {
  const sid = Number(route.params.sessionId)
  if (!Number.isInteger(sid)) {
    router.replace('/practice')
    return
  }
  if (sessionStore.sessionId !== sid) {
    loading.value = true
    try {
      await sessionStore.loadSession(sid)
    } catch {
      /* 错误提示已由请求层统一弹出 */
    } finally {
      loading.value = false
    }
  }
  scrollToBottom()
  autoPlayLatest()
})
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

// 整页不滚动：高度撑满 .app-main，只有消息区内部滚动
.dialogue-room {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.room-shell.page-shell {
  flex: 1;
  min-height: 0;
  display: flex;
  padding: 22px 0 26px;
}

// 左栏 + 中栏 的桌面三区布局
.room-grid {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 292px minmax(0, 1fr);
  gap: 22px;
}

// ---------------- 左栏 ----------------
.room-rail {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 2px;
}

.rail-card {
  flex-shrink: 0;
  padding: 20px 22px;
}

.human-card {
  padding: 22px 22px 18px;
}

.rail-title {
  margin-bottom: 14px;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--muted);
}

.score-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px 10px;
}

.scene-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  font-size: 13.5px;

  & + .scene-row {
    border-top: 1px solid var(--border);
  }

  .scene-value {
    font-weight: 650;
    color: var(--ink);
    text-align: right;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

// ---------------- 中栏 ----------------
.room-main {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
}

.chat-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 26px 16px;
  border-bottom: 1px solid var(--border);
}

.chat-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.025em;
  color: var(--ink);
}

.chat-sub {
  display: block;
  margin-top: 3px;
  font-size: 12.5px;
}

.chat-state {
  flex-shrink: 0;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 650;
  color: var(--primary);
  background: rgba(59, 111, 224, 0.1);

  &.is-finished {
    color: var(--muted);
    background: rgba(31, 42, 68, 0.06);
  }
}

.chat-panel {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 24px 26px 8px;
  scroll-behavior: smooth;
}

.typing {
  text-align: center;
  font-size: 12.5px;
  padding: 4px 0 12px;
}

// ---------------- 输入区 ----------------
.input-area {
  flex-shrink: 0;
}

.input-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;

  .text-input {
    flex: 1;

    :deep(.el-input__wrapper) {
      border-radius: 999px;
      padding: 6px 18px;
      box-shadow: none;
      background: rgba(31, 42, 68, 0.04);
      transition: box-shadow 0.3s ease, background 0.3s ease;

      &.is-focus {
        background: var(--card);
        box-shadow: 0 0 0 2px rgba(59, 111, 224, 0.28);
      }
    }

    :deep(.el-input__inner) {
      font-size: 14.5px;
    }
  }

  .mic-wave {
    flex: 0 0 132px;
    width: 132px;
  }

  .voice-btn,
  .send-btn {
    flex-shrink: 0;
    width: 46px;
    height: 46px;
    transition:
      transform 0.35s $ease-spring,
      box-shadow 0.35s $ease-apple;
  }

  .voice-btn {
    color: var(--primary);
    background: rgba(59, 111, 224, 0.08);
    border-color: transparent;

    &:hover {
      transform: translateY(-2px) scale(1.05);
      background: rgba(59, 111, 224, 0.16);
    }

    &.listening {
      color: #fff;
      background: var(--danger);
      border-color: transparent;
      animation: pulse 1.2s infinite;
    }
  }

  .send-btn {
    background: var(--accent);
    border-color: transparent;
    box-shadow: 0 10px 22px rgba(255, 106, 77, 0.28);

    &:hover {
      transform: translateY(-2px) scale(1.05);
      background: var(--accent);
      box-shadow: 0 16px 30px rgba(255, 106, 77, 0.38);
    }
  }
}

.input-hint {
  margin: 10px 2px 0;
  font-size: 12px;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(229, 72, 77, 0.5);
  }
  70% {
    box-shadow: 0 0 0 16px rgba(229, 72, 77, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(229, 72, 77, 0);
  }
}

// ---------------- 窄屏降级为单列，交还页面滚动 ----------------
@media (max-width: 900px) {
  .dialogue-room {
    height: auto;
    overflow: visible;
  }

  .room-shell.page-shell {
    display: block;
    padding: 16px 0 32px;
  }

  .room-grid {
    display: flex;
    flex-direction: column;
  }

  .room-rail {
    order: 2;
    overflow: visible;
  }

  .room-main {
    order: 1;
  }

  .chat-panel {
    height: 58vh;
    flex: none;
  }

  .rail-card {
    padding: 18px;
  }
}
</style>