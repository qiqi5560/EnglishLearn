import { ref } from 'vue'
import { defineStore } from 'pinia'
import * as dialogueApi from '@/api/modules/dialogue'
import type { ChatMessageDto, DialogueSummaryDto } from '@/types/api'

export interface LiveScores {
  pron: number
  fluency: number
  reaction: number
  natural: number
}

const EMPTY_SCORES: LiveScores = { pron: 0, fluency: 0, reaction: 0, natural: 0 }

export const useSessionStore = defineStore('session', () => {
  const sessionId = ref<number | null>(null)
  const sceneId = ref<number | null>(null)
  const sceneName = ref('')
  const status = ref('ongoing')
  const messages = ref<ChatMessageDto[]>([])
  const liveScores = ref<LiveScores>({ ...EMPTY_SCORES })
  const summary = ref<DialogueSummaryDto | null>(null)
  const sending = ref(false)

  /** 创建场景会话（F002）：后端返回开场白 */
  async function startScene(scene: { id: number; name: string }, mode = 'scenario') {
    const res = await dialogueApi.createSession({ sceneId: scene.id, mode })
    sessionId.value = res.sessionId
    sceneId.value = res.sceneId ?? scene.id
    sceneName.value = res.sceneName
    status.value = res.status
    messages.value = res.messages
    liveScores.value = { ...EMPTY_SCORES }
    summary.value = null
    return res
  }

  /** 发起自由对话 */
  async function startFree(mode = 'free') {
    const res = await dialogueApi.createSession({ sceneId: undefined, mode })
    sessionId.value = res.sessionId
    sceneId.value = null
    sceneName.value = res.sceneName
    status.value = res.status
    messages.value = res.messages
    liveScores.value = { ...EMPTY_SCORES }
    return res
  }

  /** 恢复历史会话 */
  async function loadSession(sid: number) {
    const { session } = await dialogueApi.getSession(sid)
    sessionId.value = session.sessionId
    sceneId.value = session.sceneId ?? null
    sceneName.value = session.sceneName
    status.value = session.status
    messages.value = session.messages
    return session
  }

  /** 发送一条用户消息：返回 AI 回复与实时评分 */
  async function send(content: string): Promise<{ aiMessage: ChatMessageDto; liveScores: LiveScores }> {
    if (!sessionId.value) throw new Error('会话尚未创建')
    sending.value = true
    try {
      const res = await dialogueApi.sendMessage(sessionId.value, content)
      messages.value.push(res.userMessage, res.aiMessage)
      liveScores.value = { ...res.liveScores } as LiveScores
      return { aiMessage: res.aiMessage, liveScores: liveScores.value }
    } finally {
      sending.value = false
    }
  }

  /** 结束会话并生成小结 */
  async function finish() {
    if (!sessionId.value) throw new Error('会话尚未创建')
    const payload = await dialogueApi.finishSession(sessionId.value)
    summary.value = payload
    status.value = 'finished'
    return payload
  }

  /** 重新拉取小结 */
  async function loadSummary(sid?: number) {
    const target = sid ?? sessionId.value
    if (!target) throw new Error('缺少会话 ID')
    summary.value = await dialogueApi.getSessionSummary(target)
    return summary.value
  }

  function reset() {
    sessionId.value = null
    sceneId.value = null
    sceneName.value = ''
    status.value = 'ongoing'
    messages.value = []
    liveScores.value = { ...EMPTY_SCORES }
    summary.value = null
    sending.value = false
  }

  return {
    sessionId,
    sceneId,
    sceneName,
    status,
    messages,
    liveScores,
    summary,
    sending,
    startScene,
    startFree,
    loadSession,
    send,
    finish,
    loadSummary,
    reset,
  }
})
