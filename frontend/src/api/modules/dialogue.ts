import { http } from '../request'
import type {
  DialogueSummaryDto,
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
  return http.post<MessageReplyResult>(`/dialogues/sessions/${sessionId}/messages`, { content })
}

export function finishSession(sessionId: number) {
  return http.post<DialogueSummaryDto>(`/dialogues/sessions/${sessionId}/finish`)
}

export function getSessionSummary(sessionId: number) {
  return http.get<DialogueSummaryDto>(`/dialogues/sessions/${sessionId}/summary`)
}
