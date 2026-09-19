<template>
  <div class="scene-detail page">
    <AppHeader title="场景详情" back />

    <div v-loading="loading" class="page-shell scene-shell">
      <template v-if="scene">
        <div class="scene-grid">
          <!-- 左栏：场景介绍 -->
          <div class="scene-main">
            <section class="hero-card glass-card">
              <span class="hero-glow" aria-hidden="true"></span>

              <div class="hero-tags">
                <el-tag effect="light" round>{{ scene.category }}</el-tag>
                <LevelTag :level="scene.level || 'A2'" />
              </div>

              <h1 class="hero-title">{{ scene.name }}</h1>
              <p class="hero-desc">{{ scene.desc }}</p>

              <div class="hero-meta stagger">
                <div class="meta-item">
                  <span class="meta-label">AI 角色</span>
                  <span class="meta-value">{{ scene.role || 'AI 搭档' }}</span>
                </div>
                <div class="meta-item">
                  <span class="meta-label">场景分类</span>
                  <span class="meta-value">{{ scene.category }}</span>
                </div>
                <div class="meta-item">
                  <span class="meta-label">难度等级</span>
                  <span class="meta-value">{{ scene.level || 'A2' }}</span>
                </div>
                <div class="meta-item">
                  <span class="meta-label">练习时长</span>
                  <span class="meta-value">10 分钟</span>
                </div>
              </div>
            </section>

            <section
              v-if="scene.roleSetting?.role || scene.roleSetting?.script"
              class="role-card glass-card hover-lift"
            >
              <div class="card-title">AI 角色设定</div>
              <p v-if="scene.roleSetting?.role" class="role-name">{{ scene.roleSetting.role }}</p>
              <p v-if="scene.roleSetting?.script" class="role-script reading-text">
                {{ scene.roleSetting.script }}
              </p>
            </section>
          </div>

          <!-- 右栏：操作卡 -->
          <aside class="scene-side">
            <section class="action-card glass-card hover-lift peek-host">
              <PeekMascot class="action-mascot" :size="58" />

              <div class="card-title">准备好开始了吗</div>
              <p class="action-desc">
                与「{{ scene.role || 'AI 搭档' }}」在《{{ scene.name }}》场景中多轮对话，每轮都会拿到发音、流利度、反应、自然度四维实时评分。
              </p>

              <div class="detail-actions">
                <el-button class="plan-btn" :type="addedToPlan ? 'success' : 'default'" :loading="addingToPlan" :disabled="addedToPlan" @click="onAddToPlan">
                  <el-icon><Calendar /></el-icon>&nbsp;{{ addedToPlan ? '已加入今日计划' : '加入每日计划' }}
                </el-button>
                <el-button type="primary" class="start-btn" :loading="starting" @click="onStart">
                  <el-icon><Microphone /></el-icon>&nbsp;开始对话练习
                </el-button>
              </div>

              <p class="action-foot text-muted">
                练习结束会生成综合评分与逐句纠错小结。
              </p>
            </section>
          </aside>
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
import PeekMascot from '@/components/base/PeekMascot.vue'
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
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);

.scene-shell {
  min-height: 60vh;
}

// 桌面两栏：主介绍 + 操作卡
.scene-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) minmax(0, 1fr);
  gap: 26px;
  align-items: start;
}

.scene-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

// ---------------- 场景主卡 ----------------
.hero-card {
  position: relative;
  overflow: hidden;
  padding: 34px 36px 30px;
}

.hero-glow {
  position: absolute;
  top: -160px;
  right: -130px;
  width: 380px;
  height: 380px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(59, 111, 224, 0.18), rgba(59, 111, 224, 0) 70%);
  pointer-events: none;
}

.hero-tags {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
}

.hero-title {
  position: relative;
  margin: 18px 0 0;
  font-size: 34px;
  font-weight: 700;
  letter-spacing: -0.035em;
  line-height: 1.2;
  color: var(--ink);
}

.hero-desc {
  position: relative;
  margin: 12px 0 0;
  max-width: 620px;
  font-size: 14.5px;
  line-height: 1.75;
  color: var(--muted);
}

.hero-meta {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 26px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--border);
  transition:
    transform 0.4s $ease-apple,
    box-shadow 0.4s $ease-apple;

  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-md);
  }

  .meta-label {
    font-size: 12px;
    font-weight: 600;
    color: var(--muted);
  }

  .meta-value {
    font-size: 15px;
    font-weight: 700;
    letter-spacing: -0.01em;
    color: var(--ink);
  }
}

// ---------------- 角色设定 ----------------
.role-card {
  padding: 26px 30px;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--ink);
}

.role-name {
  margin: 10px 0 0;
  font-size: 15px;
  font-weight: 650;
  color: var(--primary);
}

.role-script {
  margin: 8px 0 0;
  font-size: 14.5px;
  color: var(--ink);
  white-space: pre-line;
}

// ---------------- 右栏操作卡 ----------------
.scene-side {
  position: sticky;
  top: 90px;
  min-width: 0;
}

.action-card {
  position: relative;
  padding: 0 26px 26px;
  overflow: hidden;
}

.action-mascot {
  margin-bottom: 2px;
}

.action-desc {
  margin: 10px 0 0;
  font-size: 13.5px;
  line-height: 1.75;
  color: var(--muted);
}

.detail-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 22px;

  .el-button {
    width: 100%;
    height: 46px;
    margin-left: 0;
    border-radius: 999px;
    font-size: 15px;
    font-weight: 600;
    transition:
      transform 0.35s $ease-apple,
      box-shadow 0.35s $ease-apple;
  }

  .plan-btn:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }

  .start-btn {
    border-color: transparent;
    background: linear-gradient(135deg, var(--primary), var(--ink));
    box-shadow: 0 12px 26px rgba(59, 111, 224, 0.3);

    &:hover {
      transform: translateY(-3px);
      background: linear-gradient(135deg, var(--primary), var(--ink));
      box-shadow: 0 18px 34px rgba(59, 111, 224, 0.4);
    }
  }
}

.action-foot {
  margin: 16px 0 0;
  font-size: 12.5px;
  line-height: 1.6;
}

// ---------------- 窄屏降级 ----------------
@media (max-width: 900px) {
  .scene-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .scene-side {
    position: static;
  }

  .hero-card {
    padding: 24px 22px;
  }

  .hero-title {
    font-size: 26px;
  }

  .hero-meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>