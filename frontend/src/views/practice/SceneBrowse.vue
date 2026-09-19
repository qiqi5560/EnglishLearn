<template>
  <div class="practice page">
    <AppHeader title="练习" />
    <main class="page-shell">
      <header class="practice-intro peek-host">
        <div class="intro-peek"><PeekMascot variant="robot" :size="40" /></div>
        <p class="eyebrow">PRACTICE STUDIO</p>
        <h1>找到适合你的练习方式</h1>
        <p class="intro-copy">用真实场景和精选素材，把每一次练习变成自然表达。</p>
      </header>

      <div class="segmented-control" role="tablist" aria-label="练习类型">
        <button type="button" role="tab" :aria-selected="activeTab === 'scene'" :class="{ active: activeTab === 'scene' }" @click="setTab('scene')">场景对话</button>
        <button type="button" role="tab" :aria-selected="activeTab === 'resource'" :class="{ active: activeTab === 'resource' }" @click="setTab('resource')">学习资源</button>
      </div>

      <section v-if="activeTab === 'scene'" class="practice-section" aria-labelledby="scene-heading">
        <div class="section-head"><div><p class="eyebrow">SCENARIO PRACTICE</p><h2 id="scene-heading">场景对话</h2></div><span class="result-count text-muted">{{ scenes.length }} 个场景</span></div>

        <div class="filter-bar glass-card">
          <div class="filter-toolbar">
            <div class="filter-group"><span class="filter-label">分类</span><div class="chips" role="group" aria-label="场景分类"><button v-for="item in categories" :key="item.value" type="button" class="filter-chip" :class="{ active: category === item.value }" @click="category = item.value">{{ item.label }}</button></div></div>
            <div class="filter-group"><span class="filter-label">等级</span><div class="chips" role="group" aria-label="英语等级"><button v-for="item in levels" :key="item" type="button" class="filter-chip" :class="{ active: level === item }" @click="level = item">{{ item || '全部' }}</button></div></div>
          </div>
        </div>

        <div v-loading="loading" class="scene-body">
          <div class="scene-grid stagger">
            <SceneCard v-for="scene in scenes" :key="scene.id" :name="scene.name" :desc="scene.desc" :category="scene.category" :level="scene.level" :role="scene.role || 'AI 搭档'" :added="planStore.isSceneInTodayPlan(scene.id)" :adding="addingSceneId === scene.id" @click="$router.push(`/scene/${scene.id}`)" @add-to-plan="onAddToPlan(scene.id)" />
          </div>
        </div>
        <div v-if="!loading && !scenes.length" class="empty-state"><span class="empty-mark">⌁</span><strong>没有找到匹配场景</strong><p class="text-muted">试试更换分类或等级筛选。</p><button type="button" class="reset-button" @click="resetFilters">重置筛选</button></div>
      </section>

      <section v-else class="practice-section" aria-labelledby="resource-heading">
        <div class="section-head"><div><p class="eyebrow">LISTEN &amp; READ</p><h2 id="resource-heading">学习资源</h2></div><span class="result-count text-muted">{{ resourceStore.resourceList.length }} 项资源</span></div>
        <ResourceList />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from '@/components/base/AppHeader.vue'
import PeekMascot from '@/components/base/PeekMascot.vue'
import SceneCard from '@/components/business/SceneCard.vue'
import ResourceList from './ResourceList.vue'
import { listScenes } from '@/api/modules/scene'
import { useResourceStore } from '@/stores/resource'
import { usePlanStore } from '@/stores/plan'
import { ElMessage } from 'element-plus'
import type { SceneDto } from '@/types/api'

const route = useRoute(); const router = useRouter(); const resourceStore = useResourceStore(); const planStore = usePlanStore()
const activeTab = ref<'scene' | 'resource'>('scene'); const category = ref(''); const level = ref(''); const scenes = ref<SceneDto[]>([]); const loading = ref(false)
const categories = [{ label: '全部', value: '' }, { label: '生活', value: '生活' }, { label: '工作', value: '工作' }, { label: '学习', value: '学习' }, { label: '出行', value: '出行' }]
const levels = ['', 'A1', 'A2', 'B1', 'B2', 'C1', 'C2']
const addingSceneId = ref<number | null>(null)

async function onAddToPlan(sceneId: number) {
  if (addingSceneId.value || planStore.isSceneInTodayPlan(sceneId)) return
  addingSceneId.value = sceneId
  try { await planStore.addSceneToPlan(sceneId); ElMessage.success('已加入今日计划') }
  catch (error: any) { const status = error?.response?.status; if (status === 409) await planStore.loadTodayTasks(); else if (status === 422) ElMessage.warning('今日场景任务已达上限'); else if (status === 404) ElMessage.warning('请先完成入学测评或生成学习方案') }
  finally { addingSceneId.value = null }
}

function syncQuery() { router.replace({ query: { ...route.query, tab: activeTab.value, category: category.value || undefined, level: level.value || undefined } }) }
function setTab(tab: 'scene' | 'resource') { activeTab.value = tab; syncQuery(); if (tab === 'resource' && !resourceStore.resourceList.length) resourceStore.loadResources({ keyword: '' }) }
function resetFilters() { category.value = ''; level.value = '' }

watch(() => route.query, (query) => { if (query.tab === 'scene' || query.tab === 'resource') activeTab.value = query.tab; category.value = typeof query.category === 'string' ? query.category : ''; level.value = typeof query.level === 'string' ? query.level : '' }, { immediate: true, deep: true })
watch([category, level], () => { syncQuery(); loadScenes() })
async function loadScenes() { loading.value = true; try { const res = await listScenes({ category: category.value || undefined, level: level.value || undefined, page: 1, pageSize: 100 }); scenes.value = res.list } finally { loading.value = false } }
onMounted(() => { loadScenes(); planStore.loadTodayTasks().catch(() => null); if (activeTab.value === 'resource') resourceStore.loadResources({ keyword: '' }) })
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

.practice {
  background: var(--surface);
}

// ---------------- 页首（紧凑版） ----------------
.practice-intro {
  position: relative;
  max-width: 640px;
  padding-left: 58px;
}

// 机器人放在文案左侧留白里，不再压住右侧筛选按钮
.intro-peek {
  position: absolute;
  top: 0;
  left: 0;
}

.eyebrow {
  margin: 0 0 6px;
  color: var(--muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.practice-intro h1 {
  margin: 0;
  color: var(--ink);
  font-size: clamp(22px, 2vw, 27px);
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: -0.03em;
}

.intro-copy {
  max-width: 520px;
  margin: 8px 0 18px;
  color: var(--muted);
  font-size: 13.5px;
  line-height: 1.65;
}

// ---------------- 分段控件（胶囊） ----------------
.segmented-control {
  display: inline-flex;
  gap: 4px;
  margin-bottom: 22px;
  padding: 4px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--border);
  border-radius: 999px;
  box-shadow: var(--shadow-sm);

  button {
    min-height: 32px;
    padding: 0 18px;
    color: var(--muted);
    font: inherit;
    font-size: 13px;
    font-weight: 600;
    background: transparent;
    border: 0;
    border-radius: 999px;
    cursor: pointer;
    transition:
      color 0.3s ease,
      background 0.35s $ease-apple,
      box-shadow 0.35s ease;

    &:hover {
      color: var(--ink);
    }

    &.active {
      color: #fff;
      background: var(--primary);
      box-shadow: 0 8px 18px rgba(59, 111, 224, 0.28);
    }
  }
}

.segmented-control button:focus-visible,
.filter-chip:focus-visible,
.reset-button:focus-visible {
  outline: 2px solid var(--primary);
  outline-offset: 2px;
}

// ---------------- 区块标题 ----------------
.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;

  h2 {
    margin: 0;
    color: var(--ink);
    font-size: 18px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }

  .eyebrow {
    margin-bottom: 4px;
  }
}

.result-count {
  padding-bottom: 3px;
  font-size: 12.5px;
}

// ---------------- 桌面筛选工具条 ----------------
.filter-bar {
  position: relative;
  margin-bottom: 16px;
  padding: 14px 18px;
}

.filter-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 26px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;

  &:last-child {
    margin-left: auto;
  }
}

.filter-label {
  flex: 0 0 auto;
  color: var(--muted);
  font-size: 12px;
  font-weight: 600;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.filter-chip {
  padding: 5px 12px;
  color: var(--ink);
  font: inherit;
  font-size: 12.5px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid var(--border);
  border-radius: 999px;
  cursor: pointer;
  transition:
    transform 0.35s $ease-apple,
    color 0.3s ease,
    background 0.3s ease,
    border-color 0.3s ease,
    box-shadow 0.35s ease;

  &:hover {
    transform: translateY(-1px);
    border-color: rgba(59, 111, 224, 0.35);
    box-shadow: 0 8px 16px rgba(31, 42, 68, 0.08);
  }

  &.active {
    color: #fff;
    background: var(--primary);
    border-color: var(--primary);
    box-shadow: 0 8px 18px rgba(59, 111, 224, 0.26);
  }
}

// ---------------- 场景网格（更密的桌面网格） ----------------
.scene-body {
  min-height: 200px;
}

.scene-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

// ---------------- 空状态 ----------------
.empty-state {
  display: grid;
  place-items: center;
  padding: 60px 20px;
  text-align: center;

  strong {
    color: var(--ink);
    font-size: 16px;
  }

  p {
    margin: 6px 0 16px;
    font-size: 13.5px;
  }
}

.empty-mark {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  margin-bottom: 14px;
  color: var(--primary);
  font-size: 26px;
  background: rgba(59, 111, 224, 0.1);
  border-radius: 50%;
}

.reset-button {
  padding: 9px 18px;
  color: var(--primary);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  background: rgba(59, 111, 224, 0.08);
  border: 1px solid rgba(59, 111, 224, 0.25);
  border-radius: 999px;
  cursor: pointer;
  transition:
    transform 0.35s $ease-apple,
    background 0.3s ease,
    box-shadow 0.35s ease;

  &:hover {
    background: rgba(59, 111, 224, 0.14);
    transform: translateY(-2px);
    box-shadow: 0 10px 20px rgba(59, 111, 224, 0.2);
  }
}

// ---------------- 响应式（逐级降列） ----------------
@media (max-width: 1320px) {
  .scene-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1100px) {
  .scene-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .scene-grid {
    grid-template-columns: 1fr;
  }

  .filter-group:last-child {
    margin-left: 0;
  }

  .practice-intro {
    padding-left: 0;
  }

  .intro-peek {
    display: none;
  }
}
</style>