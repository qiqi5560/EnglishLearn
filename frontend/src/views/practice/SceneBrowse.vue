<template>
  <div class="practice page">
    <AppHeader title="练习" />
    <main class="practice-shell">
      <header class="practice-intro">
        <p class="eyebrow">PRACTICE STUDIO</p>
        <h1>找到适合你的<br class="mobile-break" />练习方式</h1>
        <p class="intro-copy">用真实场景和精选素材，把每一次练习变成自然表达。</p>
      </header>

      <div class="segmented-control" role="tablist" aria-label="练习类型">
        <button type="button" role="tab" :aria-selected="activeTab === 'scene'" :class="{ active: activeTab === 'scene' }" @click="setTab('scene')">场景对话</button>
        <button type="button" role="tab" :aria-selected="activeTab === 'resource'" :class="{ active: activeTab === 'resource' }" @click="setTab('resource')">学习资源</button>
      </div>

      <section v-if="activeTab === 'scene'" class="practice-section" aria-labelledby="scene-heading">
        <div class="section-head"><div><p class="eyebrow">SCENARIO PRACTICE</p><h2 id="scene-heading">场景对话</h2></div><span class="result-count text-muted">{{ scenes.length }} 个场景</span></div>
        <div class="filter-groups">
          <div class="filter-group"><span class="filter-label">分类</span><div class="chips" role="group" aria-label="场景分类"><button v-for="item in categories" :key="item.value" type="button" class="filter-chip" :class="{ active: category === item.value }" @click="category = item.value">{{ item.label }}</button></div></div>
          <div class="filter-group"><span class="filter-label">等级</span><div class="chips" role="group" aria-label="英语等级"><button v-for="item in levels" :key="item" type="button" class="filter-chip" :class="{ active: level === item }" @click="level = item">{{ item || '全部' }}</button></div></div>
        </div>
        <div v-loading="loading" class="scene-grid">
          <SceneCard v-for="scene in scenes" :key="scene.id" :name="scene.name" :desc="scene.desc" :category="scene.category" :level="scene.level" :role="scene.role || 'AI 搭档'" :added="planStore.isSceneInTodayPlan(scene.id)" :adding="addingSceneId === scene.id" @click="$router.push(`/scene/${scene.id}`)" @add-to-plan="onAddToPlan(scene.id)" />
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
.practice { --practice-ink: #171717; --practice-muted: #86868b; --practice-line: #e8e8ed; --practice-accent: #0071e3; background: #f5f5f7; }.practice.page { padding-bottom: 0; }.practice-shell { width: min(1180px, calc(100% - 40px)); margin: 0 auto; padding: 54px 0 24px; }.eyebrow { margin: 0 0 9px; color: var(--practice-muted); font-size: 11px; font-weight: 700; letter-spacing: .14em; text-transform: uppercase; }.practice-intro h1 { margin: 0; color: var(--practice-ink); font-size: clamp(42px, 6vw, 72px); line-height: 1.02; letter-spacing: -.05em; }.intro-copy { max-width: 440px; margin: 20px 0 34px; color: #6e6e73; font-size: 17px; }.mobile-break { display: none; }.segmented-control { display: flex; width: min(100%, 440px); margin-bottom: 52px; padding: 4px; background: #e8e8ed; border-radius: 12px; }.segmented-control button { flex: 1; min-height: 38px; border: 0; border-radius: 9px; color: #6e6e73; background: transparent; font: inherit; cursor: pointer; transition: background .2s, color .2s, box-shadow .2s; }.segmented-control button.active { color: var(--practice-ink); background: #fff; box-shadow: 0 1px 4px rgba(0,0,0,.09); font-weight: 600; }.segmented-control button:focus-visible, .filter-chip:focus-visible, .reset-button:focus-visible { outline: 2px solid var(--practice-accent); outline-offset: 2px; }.section-head { display: flex; align-items: end; justify-content: space-between; margin-bottom: 20px; }.section-head h2 { margin: 0; color: var(--practice-ink); font-size: 30px; letter-spacing: -.045em; }.result-count { font-size: 13px; }.filter-groups { display: grid; gap: 13px; margin-bottom: 24px; }.filter-group { display: flex; align-items: center; gap: 14px; min-width: 0; }.filter-label { flex: 0 0 32px; color: var(--practice-muted); font-size: 12px; }.chips { display: flex; gap: 7px; overflow-x: auto; scrollbar-width: none; }.chips::-webkit-scrollbar { display: none; }.filter-chip { flex: 0 0 auto; padding: 7px 13px; color: #6e6e73; background: #fff; border: 1px solid var(--practice-line); border-radius: 999px; font: inherit; font-size: 12px; cursor: pointer; transition: background .2s, color .2s, border .2s; }.filter-chip.active { color: #fff; background: var(--practice-ink); border-color: var(--practice-ink); }.scene-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; min-height: 180px; }.empty-state { display: grid; place-items: center; padding: 58px 20px; text-align: center; }.empty-mark { display: grid; place-items: center; width: 48px; height: 48px; margin-bottom: 14px; color: var(--practice-accent); background: #eaf3ff; border-radius: 50%; font-size: 27px; }.empty-state strong { color: var(--practice-ink); }.empty-state p { margin: 5px 0 16px; font-size: 13px; }.reset-button { padding: 8px 14px; color: var(--practice-accent); background: transparent; border: 1px solid #b9d9f7; border-radius: 999px; font: inherit; font-size: 12px; cursor: pointer; }
@media (max-width: 900px) { .practice-shell { width: min(100% - 32px, 680px); padding-top: 32px; }.scene-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 560px) { .practice-shell { width: calc(100% - 32px); padding: 25px 0 16px; }.mobile-break { display: initial; }.intro-copy { margin: 17px 0 25px; font-size: 15px; }.segmented-control { width: 100%; margin-bottom: 35px; }.section-head h2 { font-size: 26px; }.filter-group { align-items: flex-start; }.filter-label { padding-top: 8px; }.scene-grid { display: flex; overflow-x: auto; grid-template-columns: none; gap: 12px; margin-right: -16px; padding-right: 16px; scrollbar-width: none; scroll-snap-type: x mandatory; }.scene-grid > * { flex: 0 0 78%; scroll-snap-align: start; } }
</style>
