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

        <el-button type="primary" class="start-btn" :loading="starting" @click="onStart">
          <el-icon><Microphone /></el-icon>&nbsp;开始对话练习
        </el-button>
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
import type { SceneDto } from '@/types/api'

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()

const scene = ref<SceneDto | null>(null)
const loading = ref(false)
const starting = ref(false)

const sceneId = Number(route.params.id)

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
  width: 100%;
  margin-top: 20px;
}
</style>
