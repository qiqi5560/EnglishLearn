import { ref } from 'vue'
import { defineStore } from 'pinia'
import * as planApi from '@/api/modules/plan'
import { track } from '@/composables/useTracker'
import type { DailyTaskDto, EntranceTestResult, PlanDto } from '@/types/api'

export const usePlanStore = defineStore('plan', () => {
  const level = ref<string | null>(null)
  const targetGoal = ref('')
  const plan = ref<PlanDto | null>(null)
  const dailyTasks = ref<DailyTaskDto[]>([])
  const entranceResult = ref<EntranceTestResult | null>(null)
  const loading = ref(false)

  function applyPlan(p: PlanDto | null) {
    plan.value = p
    level.value = p?.levelCurrent ?? null
    targetGoal.value = p?.targetGoal ?? ''
  }

  /** 加载当前方案 + 今日任务 */
  async function loadAll() {
    loading.value = true
    try {
      const [cur, tasks] = await Promise.all([planApi.getCurrentPlan(), planApi.getTodayTasks()])
      applyPlan(cur)
      if (tasks.plan && !cur) applyPlan(tasks.plan)
      dailyTasks.value = tasks.tasks
    } finally {
      loading.value = false
    }
  }

  async function loadCurrentPlan() {
    const cur = await planApi.getCurrentPlan()
    applyPlan(cur)
    return cur
  }

  async function loadTodayTasks() {
    const res = await planApi.getTodayTasks()
    if (res.plan && !plan.value) applyPlan(res.plan)
    dailyTasks.value = res.tasks
    return res.tasks
  }

  /** 入学测评提交：后端判定等级并生成方案 */
  async function submitEntranceTest(answers: planApi.EntranceAnswer[], targetGoalInput?: string) {
    const res = await planApi.submitEntranceTest({ answers, targetGoal: targetGoalInput })
    applyPlan(res.plan)
    dailyTasks.value = res.tasks
    level.value = res.level
    targetGoal.value = res.targetGoal
    entranceResult.value = res
    return res
  }

  /** 调整学习目标，重建当前方案 */
  async function regenerate(targetGoalInput: string) {
    const res = await planApi.generatePlan({ targetGoal: targetGoalInput })
    applyPlan(res.plan)
    dailyTasks.value = res.tasks
  }

  /** 勾选每日任务（乐观更新 + 落库） */
  async function toggleTask(taskId: number, done: boolean) {
    const prev = dailyTasks.value.find((t) => t.taskId === taskId)
    if (prev) prev.done = done
    // 埋点钩子：任务完成行为，一期仅留痕，二期上报后用于真实完成率
    track(done ? 'finish' : 'unfinish', { taskId, taskType: prev?.type ?? null })
    try {
      const updated = await planApi.toggleTask(taskId, done)
      const cur = dailyTasks.value.find((t) => t.taskId === taskId)
      if (cur) cur.done = updated.done
    } catch {
      if (prev) prev.done = !done
      throw new Error('任务状态更新失败')
    }
  }

  function isSceneInTodayPlan(sceneId: number) {
    return dailyTasks.value.some((task) => task.sceneId === sceneId)
  }

  async function addSceneToPlan(sceneId: number) {
    const task = await planApi.addSceneToPlan(sceneId)
    const index = dailyTasks.value.findIndex((item) => item.taskId === task.taskId)
    if (index >= 0) dailyTasks.value[index] = task
    else dailyTasks.value.push(task)
    return task
  }

  return {
    level,
    targetGoal,
    plan,
    dailyTasks,
    entranceResult,
    loading,
    loadAll,
    loadCurrentPlan,
    loadTodayTasks,
    submitEntranceTest,
    regenerate,
    toggleTask,
    isSceneInTodayPlan,
    addSceneToPlan,
  }
})
