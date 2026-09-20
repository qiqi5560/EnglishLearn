import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useSessionStore } from '@/stores/session'
import { track } from '@/composables/useTracker'
import type { DailyTaskDto } from '@/types/api'

/** 任务类型 → 兜底跳转的练习页标签页 */
const FALLBACK_TAB: Record<string, 'scene' | 'resource'> = {
  场景对话: 'scene',
  跟读: 'resource',
  精听: 'resource',
  单词: 'resource',
}

/**
 * “每日任务 → 直达练习”统一入口：首页与学习方案页共用，避免两处跳转逻辑漂移。
 *
 * 映射规则：
 * - 场景对话：绑定 sceneId → 直接创建会话并进入对话房间
 * - 跟读 / 精听：绑定 resourceId → 直达对应素材的训练页
 * - 单词：无词汇模块，仅提示“功能建设中”
 * - 无绑定目标：兜底跳到对应分类的练习列表并提示
 */
export function useTaskJump() {
  const router = useRouter()
  const sessionStore = useSessionStore()

  function fallback(task: DailyTaskDto) {
    const tab = FALLBACK_TAB[task.type] ?? 'resource'
    ElMessage.warning('该任务暂未匹配到练习对象，已为你打开对应练习列表')
    router.push({ path: '/practice', query: { tab } })
  }

  async function startSceneTask(task: DailyTaskDto) {
    if (!task.sceneId) {
      fallback(task)
      return
    }
    await sessionStore.startScene({ id: task.sceneId, name: task.title })
    if (sessionStore.sessionId) {
      router.push(`/dialogue/${sessionStore.sessionId}`)
    }
  }

  function startTask(task: DailyTaskDto) {
    // 埋点钩子：任务点击进入行为，一期仅留痕，二期上报后用于真实点击率
    track('click', { taskId: task.taskId, taskType: task.type, sceneId: task.sceneId ?? null, resourceId: task.resourceId ?? null })
    switch (task.type) {
      case '场景对话':
        return startSceneTask(task)
      case '跟读':
        if (task.resourceId) {
          router.push(`/resource/${task.resourceId}/read`)
          return
        }
        return fallback(task)
      case '精听':
        if (task.resourceId) {
          router.push(`/resource/${task.resourceId}/listen`)
          return
        }
        return fallback(task)
      case '单词':
        ElMessage.info('单词复习功能建设中，敬请期待')
        return
      default:
        return fallback(task)
    }
  }

  return { startTask }
}
