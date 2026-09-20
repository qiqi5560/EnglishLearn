import { useUserStore } from '@/stores/user'

/**
 * 埋点钩子（一期：空实现）。
 *
 * 二期将在这里把事件上报到 POST /api/track，届时四个运营指标
 * （点击率 / 完成率 / 互动率 / 跳出率）改用真实曝光与停留数据计算。
 * 一期保留调用点，是为了让后续接入时无需再改业务组件。
 *
 * @param type 事件类型：expose（曝光）/ click（点击）/ finish（完成）/ interact（互动）/ bounce（跳出）
 * @param payload 事件载荷
 */
export function track(type: string, payload?: Record<string, unknown>) {
  if (import.meta.env.DEV) {
    // 开发期仅在控制台留痕，便于确认调用点已接入
    const userId = (() => {
      try {
        return useUserStore().userInfo?.userId ?? null
      } catch {
        return null
      }
    })()
    console.debug('[track]', type, { userId, ...payload })
  }
}
