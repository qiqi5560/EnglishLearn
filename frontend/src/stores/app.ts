import { ref } from 'vue'
import { defineStore } from 'pinia'

export type AgeMode = 'normal' | 'child' | 'senior'

export const useAppStore = defineStore('app', () => {
  // 全年龄段模式（F009）：普通 / 少儿 / 中老年
  const ageMode = ref<AgeMode>('normal')
  const globalLoading = ref(false)

  function setAgeMode(mode: AgeMode) {
    ageMode.value = mode
  }

  return { ageMode, globalLoading, setAgeMode }
})
