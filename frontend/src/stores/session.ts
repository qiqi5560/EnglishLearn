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
  /** 口语评分是否正在后台计算 */
  const evaluating = ref(false)

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

    // 先立刻上屏用户消息，避免等待模型期间界面毫无反馈
    const optimisticId = -Date.now()
    messages.value.push({
      id: optimisticId,
      speaker: 'user',
      contentEn: content,
      time: new Date().toISOString(),
    })

    try {
      const res = await dialogueApi.sendMessage(sessionId.value, content)
      const idx = messages.value.findIndex((m) => m.id === optimisticId)
      if (idx >= 0) messages.value.splice(idx, 1, res.userMessage)
      else messages.value.push(res.userMessage)
      messages.value.push(res.aiMessage)
      if (res.liveScores) {
        liveScores.value = { ...res.liveScores } as LiveScores
      } else {
        // 评分后台计算中：不阻塞本次返回，异步轮询回填
        void pollAssessment(res.userMessage.id)
      }
      return { aiMessage: res.aiMessage, liveScores: liveScores.value }
    } catch (e) {
      // 失败时撤下乐观消息，输入内容由调用方恢复
      const idx = messages.value.findIndex((m) => m.id === optimisticId)
      if (idx >= 0) messages.value.splice(idx, 1)
      throw e
    } finally {
      sending.value = false
    }
  }

  /** 轮询该消息的口语评分，出分后回填到四维实时分区（最多约 30 秒） */
  async function pollAssessment(messageId: number) {
    evaluating.value = true
    const sid = sessionId.value
    try {
      for (let i = 0; i < 15 && sid === sessionId.value; i += 1) {
        await new Promise((resolve) => setTimeout(resolve, 2000))
        const res = await dialogueApi.getMessageAssessment(sid!, messageId)
        if (res.ready) {
          liveScores.value = {
            pron: res.pron ?? 0,
            fluency: res.fluency ?? 0,
            reaction: res.reaction ?? 0,
            natural: res.natural ?? 0,
          }
          return
        }
      }
    } catch {
      /* 评分获取失败不影响对话进行 */
    } finally {
      if (sid === sessionId.value) evaluating.value = false
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
    evaluating.value = false
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
    evaluating,
    startScene,
    startFree,
    loadSession,
    send,
    finish,
    loadSummary,
    reset,
  }
})
