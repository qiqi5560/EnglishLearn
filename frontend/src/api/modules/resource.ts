import { http } from '../request'
import type { Page, ResourceDto } from '@/types/api'

export interface ResourceQuery {
  type?: string
  category?: string
  level?: string
  keyword?: string
  page?: number
  pageSize?: number
}

export function listResources(params: ResourceQuery = {}) {
  return http.get<Page<ResourceDto>>('/resources', { ...params })
}

export function resourceDetail(id: number) {
  return http.get<ResourceDto>(`/resources/${id}`)
}
