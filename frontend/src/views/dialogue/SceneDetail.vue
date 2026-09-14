<template>
  <div class="scene-detail">
    <AppHeader title="场景详情" back />

    <div v-loading="loading" class="detail-body">
      <template v-if="scene">
        <div class="scene-hero" :style="{ background: 'linear-gradient(135deg, #3b6fe0, #9bb5f1)' }">
          <h2>{{ scene.name }}</h2>
          <div class="hero-tags">
            <el-tag>{{ scene.category }}</el-tag>
            <LevelTag :level="scene.level || 'A2'" />
          </div>
        </div>

        <el-card shadow="never">
          <div class="desc-title">场景简介</div>
          <p class="text-muted">{{ scene.desc }}</p>
          <el-divider />
          <div class="role-row">
            <span class="text-muted">AI 角色</span>
            <span>{{ scene.role || 'AI 搭档' }}</span>
          </div>
          <div class="role-row">
            <span class="text-muted">练习时长</span>
            <span>10 分钟</span>
          </div>
        </el-card>

        <div class="detail-actions">
          <el-button class="plan-btn" :type="addedToPlan ? 'success' : 'default'" :loading="addingToPlan" :disabled="addedToPlan" @click="onAddToPlan">
            <el-icon><Calendar /></el-icon>&nbsp;{{ addedToPlan ? '已加入今日计划' : '加入每日计划' }}
          </el-button>
          <el-button type="primary" class="start-btn" :loading="starting" @click="onStart">
          <el-icon><Microphone /></el-icon>&nbsp;开始对话练习
          </el-button>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="场景不存在或已下架">
        <el-button type="primary" @click="$router.push('/practice')">返回练习</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from '@/components/base/AppHeader.vue'
import LevelTag from '@/components/business/LevelTag.vue'
import { sceneDetail } from '@/api/modules/scene'
import { useSessionStore } from '@/stores/session'
import { usePlanStore } from '@/stores/plan'
import { ElMessage } from 'element-plus'
import type { SceneDto } from '@/types/api'

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()
const planStore = usePlanStore()

const scene = ref<SceneDto | null>(null)
const loading = ref(false)
const starting = ref(false)
const addingToPlan = ref(false)

const sceneId = Number(route.params.id)
const addedToPlan = ref(false)

async function onAddToPlan() {
  if (!scene.value || addingToPlan.value || addedToPlan.value) return
  addingToPlan.value = true
  try {
    await planStore.addSceneToPlan(scene.value.id)
    addedToPlan.value = true
    ElMessage.success('已加入今日计划')
  } catch (error: any) {
    if (error?.response?.status === 409) addedToPlan.value = true
    else if (error?.response?.status === 422) ElMessage.warning('今日场景任务已达上限')
  } finally { addingToPlan.value = false }
}

async function onStart() {
  if (!scene.value || starting.value) return
  starting.value = true
  try {
    await sessionStore.startScene({ id: scene.value.id, name: scene.value.name })
    router.push(`/dialogue/${sessionStore.sessionId}`)
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    starting.value = false
  }
}

onMounted(async () => {
  if (!Number.isInteger(sceneId)) return
  loading.value = true
  try {
    scene.value = await sceneDetail(sceneId)
    await planStore.loadTodayTasks().catch(() => null)
    addedToPlan.value = planStore.isSceneInTodayPlan(sceneId)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.detail-body {
  padding: 16px;
  min-height: 60vh;
}

.scene-hero {
  border-radius: var(--radius-lg);
  padding: 24px;
  color: #fff;
  margin-bottom: 16px;

  h2 {
    margin: 0 0 12px;
  }

  .hero-tags {
    display: flex;
    gap: 8px;
  }
}

.desc-title {
  font-weight: 700;
  margin-bottom: 8px;
}

.role-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
}

.start-btn {
  flex: 1;
}

.detail-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.plan-btn {
  min-width: 180px;
}
</style>
