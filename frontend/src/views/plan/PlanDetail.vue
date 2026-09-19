<template>
  <div class="plan-detail page">
    <AppHeader title="学习方案" back />

    <div v-loading="loading" class="page-shell plan-shell">
      <template v-if="planStore.plan || planStore.dailyTasks.length">
        <div class="plan-grid">
          <!-- 左栏：学习方案与阶段目标 -->
          <div class="plan-main">
            <section class="overview-card glass-card">
              <div class="card-title">学习方案</div>
              <div class="summary-row">
                <span class="text-muted">学习目标</span>
                <el-tag effect="light" round>{{ planStore.targetGoal || '未制定' }}</el-tag>
              </div>
              <div class="summary-row">
                <span class="text-muted">当前等级</span>
                <LevelTag :level="planStore.level || 'A1'" />
              </div>
            </section>

            <section class="path-card glass-card">
              <div class="card-title">学习路径</div>
              <el-timeline>
                <el-timeline-item
                  v-for="(stage, i) in stages"
                  :key="i"
                  :timestamp="`第 ${stage.stage} 阶段 · ${stage.tasks} 项任务/天`"
                  type="primary"
                >
                  {{ stage.focus }}
                </el-timeline-item>
              </el-timeline>
            </section>
          </div>

          <!-- 右栏：今日任务与进度 -->
          <aside class="plan-side">
            <section class="task-card glass-card peek-host">
              <PeekMascot class="task-mascot" :size="56" />

              <div class="task-head">
                <div class="card-title">今日任务清单</div>
                <span v-if="planStore.dailyTasks.length" class="task-progress text-muted">
                  已完成 {{ doneCount }} / {{ planStore.dailyTasks.length }}
                </span>
              </div>

              <div v-if="planStore.dailyTasks.length" class="progress-track" aria-hidden="true">
                <i :style="{ width: `${progressPercent}%` }"></i>
              </div>

              <template v-if="planStore.dailyTasks.length">
                <div
                  v-for="task in planStore.dailyTasks"
                  :key="task.taskId"
                  class="task-row"
                  :class="{ 'is-done': task.done }"
                  @click="onStartTask(task)"
                >
                  <el-checkbox
                    class="task-check"
                    :model-value="task.done"
                    @click.stop
                    @change="(val: string | number | boolean) => onToggleTask(task, !!val)"
                  />
                  <div class="task-main">
                    <div class="task-title">{{ task.title }}</div>
                    <div class="task-meta">
                      <el-tag size="small" effect="plain">{{ task.type }}</el-tag>
                      <span class="text-muted">{{ task.durationMin }} 分钟</span>
                      <el-tag v-if="task.done" size="small" type="success" effect="light">已完成</el-tag>
                    </div>
                  </div>
                  <el-button size="small" class="go-btn" @click.stop="onStartTask(task)">
                    {{ task.done ? '再练一次' : '去练习' }}
                  </el-button>
                </div>
              </template>
              <el-empty
                v-else
                :image-size="60"
                description="今日任务将按学习方案自动生成，敬请期待"
              />
            </section>
          </aside>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="还没有学习方案，先完成入学测评">
        <el-button type="primary" @click="$router.push('/entrance-test')">开始测评</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
import PeekMascot from '@/components/base/PeekMascot.vue'
import LevelTag from '@/components/business/LevelTag.vue'
import { usePlanStore } from '@/stores/plan'
import { useTaskJump } from '@/composables/useTaskJump'
import type { DailyTaskDto } from '@/types/api'

const planStore = usePlanStore()
const { startTask } = useTaskJump()
const loading = ref(false)

const planContent = computed(() => {
  const raw = planStore.plan?.planContent
  if (!raw) return null
  try {
    return JSON.parse(raw) as { goal?: string; stages?: { stage: number; focus: string; tasks: number }[] }
  } catch {
    return null
  }
})

const stages = computed(() => planContent.value?.stages ?? [])

const doneCount = computed(() => planStore.dailyTasks.filter((t) => t.done).length)

/** 纯展示：今日任务完成进度百分比 */
const progressPercent = computed(() => {
  const total = planStore.dailyTasks.length
  return total ? Math.round((doneCount.value / total) * 100) : 0
})

async function onToggleTask(task: DailyTaskDto, val: boolean) {
  try {
    await planStore.toggleTask(task.taskId, val)
  } catch {
    ElMessage.error('任务状态更新失败，请稍后重试')
  }
}

/** 点任务行 / 行尾按钮：与首页一致，直达该任务绑定的练习 */
async function onStartTask(task: DailyTaskDto) {
  try {
    await startTask(task)
  } catch {
    /* 失败提示已由请求层统一弹出 */
  }
}

onMounted(async () => {
  loading.value = true
  try {
    await planStore.loadAll()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

.plan-shell {
  min-height: 60vh;
}

.plan-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.plan-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

.plan-side {
  min-width: 0;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--ink);
}

// ---------------- 学习方案总览 ----------------
.overview-card {
  padding: 26px 30px;
}

.summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 0;
  font-size: 14px;

  & + .summary-row {
    border-top: 1px solid var(--border);
  }

  &:first-of-type {
    margin-top: 12px;
  }
}

// ---------------- 学习路径 ----------------
.path-card {
  padding: 26px 30px;

  :deep(.el-timeline) {
    margin-top: 20px;
    padding-left: 4px;
  }

  :deep(.el-timeline-item__timestamp) {
    font-size: 12.5px;
    font-weight: 600;
    color: var(--muted);
  }

  :deep(.el-timeline-item__content) {
    font-size: 15px;
    font-weight: 650;
    color: var(--ink);
  }
}

// ---------------- 今日任务卡 ----------------
.task-card {
  position: relative;
  padding: 0 24px 22px;
  overflow: hidden;
}

.task-mascot {
  margin-bottom: 2px;
}

.task-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-top: 6px;
}

.task-progress {
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.progress-track {
  height: 8px;
  margin: 16px 0 6px;
  border-radius: 999px;
  background: rgba(31, 42, 68, 0.07);
  overflow: hidden;

  i {
    display: block;
    height: 100%;
    border-radius: 999px;
    background: var(--success);
    transition: width 0.6s $ease-apple;
  }
}

.task-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition:
    background 0.3s ease,
    transform 0.35s $ease-apple,
    box-shadow 0.35s $ease-apple;

  & + .task-row {
    border-top: 1px solid var(--border);
  }

  &:hover {
    background: rgba(255, 255, 255, 0.9);
    transform: translateX(3px);
    box-shadow: var(--shadow-sm);
  }

  &.is-done {
    opacity: 0.6;

    .task-title {
      text-decoration: line-through;
    }
  }
}

.task-check {
  flex-shrink: 0;
  margin-right: 0;
}

.task-main {
  flex: 1;
  min-width: 0;
}

.task-title {
  font-size: 15px;
  font-weight: 650;
  letter-spacing: -0.01em;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  font-size: 12px;
}

.go-btn {
  flex-shrink: 0;
  border-radius: 999px;
  transition:
    transform 0.35s $ease-spring,
    box-shadow 0.35s $ease-apple;

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-sm);
  }
}

// ---------------- 窄屏降级 ----------------
@media (max-width: 900px) {
  .plan-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .overview-card,
  .path-card {
    padding: 22px 20px;
  }

  .task-card {
    padding: 0 18px 18px;
  }
}
</style>