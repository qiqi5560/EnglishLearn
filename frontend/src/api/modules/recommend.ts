import { http } from '../request'
import type {
  AdminMetrics,
  LevelPrediction,
  ModelStatus,
  RecommendOverview,
  RecommendResourceItem,
  RecommendSceneItem,
  RecommendTaskItem,
  TrainResult,
} from '@/types/api'

/** 水平预测详情 */
export function recommendProfile() {
  return http.get<LevelPrediction>('/recommend/profile')
}

/** 一次拿到水平预测 + 场景 / 资源 / 任务三类推荐 */
export function recommendOverview(limit = 6) {
  return http.get<RecommendOverview>('/recommend/overview', { limit })
}

export function recommendScenes(limit = 6) {
  return http.get<RecommendSceneItem[]>('/recommend/scenes', { limit })
}

export function recommendResources(limit = 6) {
  return http.get<RecommendResourceItem[]>('/recommend/resources', { limit })
}

export function recommendTasks(limit = 6) {
  return http.get<RecommendTaskItem[]>('/recommend/tasks', { limit })
}

/** 管理端：四项推荐效果指标 */
export function adminMetrics(days = 14) {
  return http.get<AdminMetrics>('/admin/metrics', { days })
}

/** 管理端：触发口语水平预测模型训练 */
export function adminTrainModel() {
  return http.post<TrainResult>('/admin/ml/train')
}

/** 管理端：模型状态 */
export function adminModelStatus() {
  return http.get<ModelStatus>('/admin/ml/status')
}
