import { http } from '../request'
import type { ReportOverview } from '@/types/api'

export function reportOverview() {
  return http.get<ReportOverview>('/reports/overview')
}
