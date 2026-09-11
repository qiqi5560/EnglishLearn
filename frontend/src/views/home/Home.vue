<template>
  <div class="home page" v-loading="loading">
    <AppHeader title="首页" />

    <div class="home-body">
      <!-- 问候 + 等级 -->
      <section class="greeting">
        <div>
          <p class="text-muted">Hi，{{ userStore.userInfo?.nickname || '学习者' }} 👋</p>
          <h2>今天也要开口说英语</h2>
        </div>
        <LevelTag :level="currentLevel || 'A2'" />
      </section>

      <!-- 今日任务 -->
      <section>
        <div class="section-title task-section-head">
          <span>今日任务</span>
          <span v-if="planStore.dailyTasks.length" class="task-progress text-muted">
            已完成 {{ doneCount }} / {{ planStore.dailyTasks.length }}
          </span>
        </div>
        <el-card shadow="never" class="task-card">
          <template v-if="planStore.dailyTasks.length">
            <div
              v-for="task in planStore.dailyTasks"
              :key="task.taskId"
              class="task-item"
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
            description="完成入学测评，获取个性化每日任务"
          >
            <el-button type="primary" @click="$router.push('/entrance-test')">去测评</el-button>
          </el-empty>
        </el-card>
      </section>

      <!-- 推荐场景（F006 推荐位） -->
      <section>
        <div class="section-title">为你推荐</div>
        <el-empty v-if="!loading && !scenes.length" description="暂无推荐场景" :image-size="60" />
        <div v-else class="scene-grid">
          <SceneCard
            v-for="scene in scenes"
            :key="scene.id"
            :name="scene.name"
            :desc="scene.desc"
            :category="scene.category"
            :level="scene.level"
            :role="scene.role || 'AI 搭档'"
            @click="$router.push(`/scene/${scene.id}`)"
          />
        </div>
      </section>

      <!-- 学习方案入口 -->
      <section>
        <el-card shadow="never" class="plan-entry" @click="$router.push('/plan')">
          <div>
            <div class="plan-title">我的学习方案</div>
            <div class="text-muted">
              目标：{{ planStore.targetGoal || '尚未制定' }} · 当前等级 {{ currentLevel || '--' }}
            </div>
          </div>
          <el-button text type="primary">
            查看
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </el-card>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppHeader from '@/components/base/AppHeader.vue'
import SceneCard from '@/components/business/SceneCard.vue'
import LevelTag from '@/components/business/LevelTag.vue'
import { useUserStore } from '@/stores/user'
import { usePlanStore } from '@/stores/plan'
import { useTaskJump } from '@/composables/useTaskJump'
import { recommendedScenes } from '@/api/modules/scene'
import type { DailyTaskDto, SceneDto } from '@/types/api'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const planStore = usePlanStore()
const { startTask } = useTaskJump()

const loading = ref(false)
const scenes = ref<SceneDto[]>([])

const currentLevel = computed(() => planStore.level ?? userStore.level ?? null)

const doneCount = computed(() => planStore.dailyTasks.filter((t) => t.done).length)

async function onToggleTask(task: DailyTaskDto, val: boolean) {
  try {
    await planStore.toggleTask(task.taskId, val)
  } catch {
    ElMessage.error('任务状态更新失败，请稍后重试')
  }
}

/** 点任务行 / 行尾按钮：直达该任务绑定的练习 */
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
    const [sceneRes] = await Promise.all([
      recommendedScenes(4),
      planStore.loadTodayTasks().catch(() => null),
      userStore.fetchMe().catch(() => null),
    ])
    scenes.value = sceneRes ?? []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.home-body {
  padding: 0 16px;
}

.greeting {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0 4px;

  p {
    margin: 0;
    font-size: 13px;
  }

  h2 {
    margin: 4px 0 0;
    font-size: 20px;
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

.task-card {
  border-radius: var(--radius-md);
}

.task-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 2px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background-color 0.2s;

  & + .task-item {
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

.scene-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.plan-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20px;
  border-radius: var(--radius-md);
  cursor: pointer;

  .plan-title {
    font-weight: 700;
    margin-bottom: 4px;
  }
}
</style>
