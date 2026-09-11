import { http } from '../request'
import type {
  AdminDashboard,
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

// 场景管理
export function adminListScenes(keyword?: string) {
  return http.get<SceneDto[]>('/admin/scenes', keyword ? { keyword } : undefined)
}

export function adminUpdateScene(id: number, payload: Partial<SceneDto>) {
  return http.put<SceneDto>(`/admin/scenes/${id}`, payload)
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

// 用户管理
export function adminListUsers(params: { keyword?: string; role?: string; page?: number; pageSize?: number }) {
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
