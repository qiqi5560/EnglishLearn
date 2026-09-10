import { ref } from 'vue'
import { defineStore } from 'pinia'
import * as resourceApi from '@/api/modules/resource'
import type { ResourceDto } from '@/types/api'

export interface ResourceFilter {
  keyword: string
  type: string
  category: string
  level: string
}

export const useResourceStore = defineStore('resource', () => {
  const resourceList = ref<ResourceDto[]>([])
  const total = ref(0)
  const loading = ref(false)
  const filter = ref<ResourceFilter>({ keyword: '', type: '', category: '', level: '' })

  /** 按筛选条件从后端拉取资源列表 */
  async function loadResources(params: Partial<ResourceFilter> = {}, page = 1, pageSize = 20) {
    loading.value = true
    try {
      const res = await resourceApi.listResources({
        ...filter.value,
        ...params,
        page,
        pageSize,
      })
      resourceList.value = res.list
      total.value = res.total
      return res
    } finally {
      loading.value = false
    }
  }

  function setFilter(patch: Partial<ResourceFilter>) {
    filter.value = { ...filter.value, ...patch }
  }

  return { resourceList, total, loading, filter, loadResources, setFilter }
})
