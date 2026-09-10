import { http } from '../request'
import type { CommentDto, Page, PostDetailResult, PostDto } from '@/types/api'

export function listTopics() {
  return http.get<string[]>('/community/topics')
}

export interface PostQuery {
  topic?: string
  page?: number
  pageSize?: number
}

export function listPosts(params: PostQuery = {}) {
  return http.get<Page<PostDto>>('/community/posts', { ...params })
}

export function createPost(payload: { title: string; content: string; topic: string }) {
  return http.post<PostDto>('/community/posts', payload)
}

export function postDetail(id: number) {
  return http.get<PostDetailResult>(`/community/posts/${id}`)
}

export function toggleLike(id: number) {
  return http.post<{ liked: boolean; likes: number }>(`/community/posts/${id}/like`)
}

export function addComment(id: number, content: string) {
  return http.post<CommentDto>(`/community/posts/${id}/comments`, { content })
}

export function deletePost(id: number) {
  return http.delete<null>(`/community/posts/${id}`)
}
