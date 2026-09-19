import { http } from '../request'
import type {
  AdminActivity,
  AdminAuditLog,
  AdminCompute,
  AdminDashboard,
  AdminQuoteDto,
  AdminSystemOverview,
  AdminUsageReport,
  Page,
  PostDto,
  ResourceDto,
  SceneDto,
  SystemConfig,
  UserDto,
} from '@/types/api'

// 数据看板
export function dashboard() {
  return http.get<AdminDashboard>('/admin/dashboard')
}

// 运营洞察
export function adminUsageReport(params: { days?: number; keyword?: string }) {
  return http.get<AdminUsageReport>('/admin/usage-report', { ...params })
}

export function adminActivity(days?: number) {
  return http.get<AdminActivity>('/admin/activity', days ? { days } : undefined)
}

export function adminCompute() {
  return http.get<AdminCompute>('/admin/compute')
}

export function adminSystemOverview() {
  return http.get<AdminSystemOverview>('/admin/system')
}

/** 操作日志：module 为空表示全部模块 */
export function adminAuditLogs(params: { module?: string; limit?: number } = {}) {
  return http.get<AdminAuditLog>('/admin/audit-logs', { ...params })
}

// 名句素材管理
export function adminListQuotes(params: { keyword?: string; builtin?: number } = {}) {
  return http.get<AdminQuoteDto[]>('/admin/quotes', { ...params })
}

export interface AdminQuotePayload {
  title?: string
  source?: string
  category?: string
  level?: string
  textEn?: string
  textZh?: string
}

export function adminCreateQuote(payload: AdminQuotePayload) {
  return http.post<AdminQuoteDto>('/admin/quotes', payload)
}

export function adminUpdateQuote(id: number, payload: AdminQuotePayload) {
  return http.put<AdminQuoteDto>(`/admin/quotes/${id}`, payload)
}

export function adminDeleteQuote(id: number) {
  return http.delete<null>(`/admin/quotes/${id}`)
}

// 用户管理（扩展：重置密码）
export function adminResetPassword(id: number, payload?: { password?: string }) {
  return http.post<null>(`/admin/users/${id}/reset-password`, payload ?? {})
}

// 场景管理
export function adminListScenes(keyword?: string) {
  return http.get<SceneDto[]>('/admin/scenes', keyword ? { keyword } : undefined)
}

/** 场景新增 / 更新（与后端 SceneIn / SceneUpdateIn 对齐，仅传需要修改的字段） */
export interface AdminScenePayload {
  sceneName?: string
  sceneCategory?: string
  sceneDesc?: string
  levelScope?: string
  role?: string
  script?: string
  coverUrl?: string
  status?: number
}

export function adminCreateScene(payload: AdminScenePayload) {
  return http.post<SceneDto>('/admin/scenes', payload)
}

export function adminUpdateScene(id: number, payload: AdminScenePayload) {
  return http.put<SceneDto>(`/admin/scenes/${id}`, payload)
}

export function adminDeleteScene(id: number) {
  return http.delete<null>(`/admin/scenes/${id}`)
}

// 资源管理
export interface AdminResourcePayload {
  title: string
  type: string
  category: string
  level: string
  mediaUrl?: string
  durationSec?: number
  status?: number
}

export function adminListResources(params: { keyword?: string; page?: number; pageSize?: number }) {
  return http.get<Page<ResourceDto>>('/admin/resources', { ...params })
}

export function adminCreateResource(payload: AdminResourcePayload) {
  return http.post<ResourceDto>('/admin/resources', payload)
}

export function adminUpdateResource(id: number, payload: Partial<AdminResourcePayload>) {
  return http.put<ResourceDto>(`/admin/resources/${id}`, payload)
}

export function adminDeleteResource(id: number) {
  return http.delete<null>(`/admin/resources/${id}`)
}

// 社区管理
export function adminListPosts(params: { status?: number; page?: number; pageSize?: number }) {
  return http.get<Page<PostDto>>('/admin/posts', { ...params })
}

export function adminReviewPost(id: number, payload: { status?: number; isTop?: boolean }) {
  return http.put<PostDto>(`/admin/posts/${id}`, payload)
}

export function adminDeletePost(id: number) {
  return http.delete<null>(`/admin/posts/${id}`)
}

export function adminDeleteComment(id: number) {
  return http.delete<null>(`/admin/comments/${id}`)
}

/** 发帖处罚：days 为处罚天数（1 天起） */
export function adminBanUser(id: number, payload: { days: number; reason?: string }) {
  return http.post<UserDto>(`/admin/users/${id}/ban`, payload)
}

export function adminUnbanUser(id: number) {
  return http.post<UserDto>(`/admin/users/${id}/unban`, {})
}

// 用户管理
export function adminListUsers(params: {
  keyword?: string
  role?: string
  /** 发帖处罚筛选：true 只看处罚中，false 只看正常，不传为全部 */
  punished?: boolean
  page?: number
  pageSize?: number
}) {
  return http.get<Page<UserDto>>('/admin/users', { ...params })
}

export function adminUpdateUser(id: number, payload: { nickname?: string; status?: number; userRole?: string }) {
  return http.put<UserDto>(`/admin/users/${id}`, payload)
}

// 系统配置
export function getSystemConfigs() {
  return http.get<SystemConfig>('/admin/configs')
}

export function updateSystemConfigs(payload: Partial<SystemConfig>) {
  return http.put<SystemConfig>('/admin/configs', payload)
}
