<template>
  <div class="dialogue-room">
    <AppHeader :title="sessionStore.sceneName || '对话练习'">
      <template #right>
        <el-button link :type="autoSpeak ? 'primary' : 'info'" title="AI 回复后自动朗读" @click="toggleAutoSpeak">
          <el-icon :size="16"><Headset /></el-icon>
          {{ isSpeaking ? '朗读中' : autoSpeak ? '朗读开' : '朗读关' }}
        </el-button>
        <el-button link type="danger" :disabled="sessionStore.status === 'finished'" @click="onEnd">结束</el-button>
      </template>
    </AppHeader>

    <!-- 数字人（F005） -->
    <div class="human-area">
      <DigitalHuman role="AI 陪练" />
    </div>

    <!-- 实时四维评分（F003） -->
    <div class="score-area">
      <ScoreRing
        v-for="d in dimScores"
        :key="d.label"
        :score="d.score"
        :label="d.label"
        :size="52"
        :stroke="5"
      />
      <el-tag v-if="sessionStore.evaluating" size="small" type="info" effect="plain" class="eval-tag">
        评分中…
      </el-tag>
    </div>

    <!-- 对话消息流 -->
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

    <!-- 输入栏：支持文字 + 语音输入 -->
    <div class="input-bar">
      <el-input
        v-model="draft"
        class="text-input"
        placeholder="输入英文，或点击麦克风开口说话…"
        :disabled="sessionStore.sending || sessionStore.status === 'finished'"
        @keyup.enter="onSend"
      />
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
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
import DigitalHuman from '@/components/business/DigitalHuman.vue'
import ChatBubble from '@/components/business/ChatBubble.vue'
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
.dialogue-room {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.human-area {
  height: 160px;
  background: linear-gradient(180deg, #eef2fd, transparent);
}

.score-area {
  display: flex;
  justify-content: space-around;
  padding: 8px 16px;
  border-bottom: 1px solid var(--border);
}

.chat-panel {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px;
}

.typing {
  text-align: center;
  font-size: 12px;
}

.input-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: var(--card);
  border-top: 1px solid var(--border);

  .text-input {
    flex: 1;
  }

  .voice-btn {
    width: 46px;
    height: 46px;

    &.listening {
      color: #fff;
      background: var(--danger, #f56c6c);
      border-color: var(--danger, #f56c6c);
      animation: pulse 1.2s infinite;
    }
  }

  .send-btn {
    width: 46px;
    height: 46px;
    background: var(--accent);
    border-color: var(--accent);
  }
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.5);
  }
  70% {
    box-shadow: 0 0 0 16px rgba(245, 108, 108, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(245, 108, 108, 0);
  }
}
</style>
