import { ref } from 'vue'
import { defineStore } from 'pinia'
import { reportOverview } from '@/api/modules/report'
import type { ReportOverview } from '@/types/api'

export const useReportStore = defineStore('report', () => {
  const overview = ref<ReportOverview | null>(null)
  const loading = ref(false)

  async function load() {
    loading.value = true
    try {
      overview.value = await reportOverview()
      return overview.value
    } finally {
      loading.value = false
    }
  }

  return { overview, loading, load }
})
