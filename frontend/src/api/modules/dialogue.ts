import { http } from '../request'
import type {
  DialogueSummaryDto,
  MessageAssessmentDto,
  MessageReplyResult,
  Page,
  SessionDto,
  SessionListItem,
} from '@/types/api'

export function createSession(payload: { sceneId?: number; mode?: string }) {
  return http.post<SessionDto>('/dialogues/sessions', payload)
}

export function listMySessions(params: { status?: string; page?: number; pageSize?: number }) {
  return http.get<Page<SessionListItem>>('/dialogues/sessions', { ...params })
}

export function getSession(sessionId: number) {
  return http.get<{ session: SessionDto }>(`/dialogues/sessions/${sessionId}`)
}

export function sendMessage(sessionId: number, content: string) {
  // LLM 生成较慢，单独放宽该请求的超时时间（150s），避免热点模型慢导致超时
  return http.post<MessageReplyResult>(
    `/dialogues/sessions/${sessionId}/messages`,
    { content },
    { timeout: 150000 },
  )
}

export function finishSession(sessionId: number) {
  return http.post<DialogueSummaryDto>(`/dialogues/sessions/${sessionId}/finish`)
}

export function getSessionSummary(sessionId: number) {
  return http.get<DialogueSummaryDto>(`/dialogues/sessions/${sessionId}/summary`)
}

/** 拉取某条用户消息的口语评分（后端异步产出，ready 为 true 表示已出分） */
export function getMessageAssessment(sessionId: number, messageId: number) {
  return http.get<MessageAssessmentDto>(`/dialogues/sessions/${sessionId}/messages/${messageId}/assessment`)
}
