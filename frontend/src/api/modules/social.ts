import { http } from '../request'
import type {
  ConversationDto,
  ConversationHistoryResult,
  FollowResult,
  FollowUserDto,
  NotificationListResult,
  ShareResult,
  UnreadResult,
  UserProfileResult,
} from '@/types/api'

/* ---------------- 站内信 ---------------- */

export function listConversations() {
  return http.get<ConversationDto[]>('/messages/conversations')
}

export function messageHistory(peerId: number, limit = 50) {
  return http.get<ConversationHistoryResult>('/messages/history', { peerId, limit })
}

export function sendMessage(peerId: number, content: string) {
  return http.post<{ id: number; content: string; createTime: string }>('/messages/send', { peerId, content })
}

export function readMessages(peerId: number) {
  return http.post<{ updated: number }>('/messages/read', { peerId })
}

export function fetchUnread() {
  return http.get<UnreadResult>('/messages/unread')
}

/* ---------------- 系统通知 ---------------- */

export function listNotifications(type?: string) {
  return http.get<NotificationListResult>('/notifications/list', type ? { type } : undefined)
}

export function readNotifications(ids?: number[]) {
  return http.post<null>('/notifications/read', { ids: ids ?? [] })
}

/* ---------------- 关注与他人主页 ---------------- */

export function userProfile(userId: number) {
  return http.get<UserProfileResult>(`/users/${userId}/profile`)
}

export function followUser(userId: number) {
  return http.post<FollowResult>(`/users/${userId}/follow`)
}

export function unfollowUser(userId: number) {
  return http.delete<FollowResult>(`/users/${userId}/follow`)
}

export function listFollowers(userId: number) {
  return http.get<FollowUserDto[]>(`/users/${userId}/followers`)
}

export function listFollowing(userId: number) {
  return http.get<FollowUserDto[]>(`/users/${userId}/following`)
}

/* ---------------- 分享 ---------------- */

export interface SharePayload {
  contentType: string
  contentId?: number | null
  channel?: string
  url?: string
}

export function shareContent(payload: SharePayload) {
  return http.post<ShareResult>('/share', payload)
}
