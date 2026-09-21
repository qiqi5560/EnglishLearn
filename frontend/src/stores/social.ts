import { ref } from 'vue'
import { defineStore } from 'pinia'
import * as socialApi from '@/api/modules/social'
import { useUserStore } from '@/stores/user'

const POLL_INTERVAL = 30_000

/**
 * 社交未读红点：定时轮询 /messages/unread（私信 + 系统通知）。
 * 不引入 WebSocket，轮询失败静默降级，不影响页面使用。
 */
export const useSocialStore = defineStore('social', () => {
  const unread = ref({ message: 0, notification: 0, total: 0 })

  let timer: number | null = null

  async function refreshUnread() {
    const userStore = useUserStore()
    if (!userStore.token) return
    try {
      unread.value = await socialApi.fetchUnread()
    } catch {
      // 静默降级：红点不刷新不影响主流程
    }
  }

  function startPolling(intervalMs: number = POLL_INTERVAL) {
    stopPolling()
    void refreshUnread()
    timer = window.setInterval(() => void refreshUnread(), intervalMs)
  }

  function stopPolling() {
    if (timer !== null) {
      window.clearInterval(timer)
      timer = null
    }
  }

  /** 站内已读后把本地红点扣掉，体验更即时（下次轮询校正） */
  function markMessageRead(count?: number) {
    const delta = count ?? unread.value.message
    unread.value = {
      ...unread.value,
      message: Math.max(0, unread.value.message - delta),
      total: Math.max(0, unread.value.total - delta),
    }
  }

  function markNotificationRead(count?: number) {
    const delta = count ?? unread.value.notification
    unread.value = {
      ...unread.value,
      notification: Math.max(0, unread.value.notification - delta),
      total: Math.max(0, unread.value.total - delta),
    }
  }

  return { unread, refreshUnread, startPolling, stopPolling, markMessageRead, markNotificationRead }
})
