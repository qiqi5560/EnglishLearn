import { http } from '../request'
import type { DocBriefDto, DocDto, QuoteDto, ReadEvalDto } from '@/types/api'

export interface QuoteQuery {
  category?: string
  level?: string
  keyword?: string
}

export interface CreateQuotePayload {
  title: string
  source?: string
  category?: string
  level?: string
  textEn: string
  textZh?: string
}

export interface CreateDocPayload {
  title: string
  text: string
  sourceType?: string
}

// ---------------- 名句素材库 ----------------

export function listQuotes(params: QuoteQuery = {}) {
  return http.get<QuoteDto[]>('/quotes', { ...params })
}

export function createQuote(payload: CreateQuotePayload) {
  return http.post<QuoteDto>('/quotes', payload)
}

export function deleteQuote(quoteId: number) {
  return http.delete<null>(`/quotes/${quoteId}`)
}

/** 单段一键翻译：返回 { textEn, textZh } */
export function translateText(text: string) {
  return http.post<{ textEn: string; textZh: string }>('/quotes/translate', { text }, { timeout: 90000 })
}

// ---------------- 我的素材（导入阅读） ----------------

export function listDocs() {
  return http.get<DocBriefDto[]>('/quotes/docs')
}

export function docDetail(docId: number) {
  return http.get<DocDto>(`/quotes/docs/${docId}`)
}

export function deleteDoc(docId: number) {
  return http.delete<null>(`/quotes/docs/${docId}`)
}

/** 粘贴文本导入：后端自动分段 */
export function createDoc(payload: CreateDocPayload) {
  return http.post<DocDto>('/quotes/docs', payload, { timeout: 60000 })
}

/** 上传文件导入（txt / md / docx），后端解析并自动分段 */
export function uploadDoc(file: File) {
  const form = new FormData()
  form.append('file', file)
  return http.post<DocDto>('/quotes/docs/upload', form, { timeout: 90000 })
}

/** 整篇一键翻译；save=true 时把译文写回素材 */
export function translateDoc(docId: number, save = false) {
  return http.post<DocDto>(`/quotes/docs/${docId}/translate?save=${save}`, undefined, { timeout: 180000 })
}

// ---------------- AI 跟读评测 ----------------

export function evaluateReading(target: string, spoken: string) {
  return http.post<ReadEvalDto>('/quotes/evaluate', { target, spoken }, { timeout: 90000 })
}

/**
 * 音素级跟读评测：上传录音（WAV）让后端用本地音素模型逐音素打分。
 * 引擎不可用或录音无效时后端会报错，调用方应回退到 evaluateReading。
 */
export function evaluateReadingAudio(file: Blob, target: string) {
  const form = new FormData()
  form.append('file', file, 'reading.wav')
  form.append('target', target)
  return http.post<ReadEvalDto>('/quotes/evaluate-audio', form, { timeout: 90000 })
}
