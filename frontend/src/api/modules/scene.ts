import { http } from '../request'
import type { Page, SceneDto } from '@/types/api'

export interface SceneQuery {
  category?: string
  level?: string
  keyword?: string
  page?: number
  pageSize?: number
}

export function listScenes(params: SceneQuery = {}) {
  return http.get<Page<SceneDto>>('/scenes', { ...params })
}

export function recommendedScenes(limit = 6) {
  return http.get<SceneDto[]>('/scenes/recommended', { limit })
}

export function sceneDetail(id: number) {
  return http.get<SceneDto>(`/scenes/${id}`)
}
