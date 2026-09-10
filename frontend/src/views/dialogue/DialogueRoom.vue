<template>
  <div class="dialogue-room">
    <AppHeader :title="sessionStore.sceneName || '对话练习'">
      <template #right>
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
      />
      <div v-if="sessionStore.sending" class="typing text-muted">AI 正在回复…</div>
      <el-empty v-if="!loading && !sessionStore.sending && !sessionStore.messages.length" description="还没有对话消息" />
    </div>

    <!-- 输入栏（真实模型接入前以文字模拟语音输入） -->
    <div class="input-bar">
      <el-input
        v-model="draft"
        class="text-input"
        placeholder="输入英文后发送…"
        :disabled="sessionStore.sending || sessionStore.status === 'finished'"
        @keyup.enter="onSend"
      />
      <el-button
        type="primary"
        circle
        class="mic-btn"
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

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()

const draft = ref('')
const loading = ref(false)
const chatPanel = ref<HTMLElement | null>(null)

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
  () => scrollToBottom(),
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

  .mic-btn {
    width: 46px;
    height: 46px;
    background: var(--accent);
    border-color: var(--accent);
  }
}
</style>
