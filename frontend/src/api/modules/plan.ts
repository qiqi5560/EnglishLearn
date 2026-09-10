import { http } from '../request'
import type {
  DailyTaskDto,
  EntranceTestResult,
  PlanDto,
  TodayTasksResult,
} from '@/types/api'

export interface EntranceAnswer {
  id: number
  text: string
}

export function submitEntranceTest(payload: { answers: EntranceAnswer[]; targetGoal?: string }) {
  return http.post<EntranceTestResult>('/plans/entrance-test', payload)
}

export function getCurrentPlan() {
  return http.get<PlanDto | null>('/plans/current')
}

export function getCurrentLevel() {
  return http.get<{ level: string | null }>('/plans/level')
}

export function generatePlan(payload: { targetGoal: string }) {
  return http.post<{ plan: PlanDto; tasks: DailyTaskDto[] }>('/plans/generate', payload)
}

export function getTodayTasks() {
  return http.get<TodayTasksResult>('/plans/today-tasks')
}

export function toggleTask(taskId: number, done: boolean) {
  return http.put<DailyTaskDto>(`/plans/tasks/${taskId}`, { done })
}
