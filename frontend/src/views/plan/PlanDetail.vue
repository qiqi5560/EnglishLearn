<template>
  <div class="plan-detail">
    <AppHeader title="学习方案" back />

    <div v-loading="loading" class="plan-body">
      <template v-if="planStore.plan || planStore.dailyTasks.length">
        <el-card shadow="never" class="plan-summary">
          <div class="summary-row">
            <span class="text-muted">学习目标</span>
            <el-tag>{{ planStore.targetGoal || '未制定' }}</el-tag>
          </div>
          <div class="summary-row">
            <span class="text-muted">当前等级</span>
            <LevelTag :level="planStore.level || 'A1'" />
          </div>
        </el-card>

        <div class="section-title">学习路径</div>
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

        <div class="section-title task-section-head">
          <span>今日任务清单</span>
          <span v-if="planStore.dailyTasks.length" class="task-progress text-muted">
            已完成 {{ doneCount }} / {{ planStore.dailyTasks.length }}
          </span>
        </div>
        <el-card shadow="never">
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
        </el-card>
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
.plan-body {
  padding: 16px;
  min-height: 60vh;
}

.plan-summary {
  border-radius: var(--radius-md);

  .summary-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 0;
  }
}

.task-section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.task-progress {
  font-size: 13px;
}

.task-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 2px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background-color 0.2s;

  & + .task-row {
    border-top: 1px solid var(--el-border-color-lighter);
  }

  &:active {
    background-color: var(--el-fill-color-light);
  }

  &.is-done {
    opacity: 0.62;

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
  font-weight: 600;
  color: var(--text-primary, #1f2d3d);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  font-size: 12px;
}

.go-btn {
  flex-shrink: 0;
}
</style>
