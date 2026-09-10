<template>
  <div class="practice page">
    <AppHeader title="练习" />

    <div class="practice-body">
      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane label="场景对话" name="scene">
          <div class="filter-row">
            <el-select v-model="category" placeholder="分类" clearable size="small" style="width: 120px">
              <el-option label="生活" value="生活" />
              <el-option label="工作" value="工作" />
              <el-option label="学习" value="学习" />
              <el-option label="出行" value="出行" />
            </el-select>
            <el-select v-model="level" placeholder="等级" clearable size="small" style="width: 120px">
              <el-option v-for="l in ['A1', 'A2', 'B1', 'B2', 'C1', 'C2']" :key="l" :label="l" :value="l" />
            </el-select>
          </div>
          <div v-loading="loading" class="scene-grid">
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
          <el-empty v-if="!loading && !scenes.length" description="暂无符合条件的场景" />
        </el-tab-pane>

        <el-tab-pane label="学习资源" name="resource">
          <ResourceList />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/base/AppHeader.vue'
import SceneCard from '@/components/business/SceneCard.vue'
import ResourceList from './ResourceList.vue'
import { listScenes } from '@/api/modules/scene'
import type { SceneDto } from '@/types/api'

const route = useRoute()
const activeTab = ref<'scene' | 'resource'>('scene')
const category = ref('')
const level = ref('')
const scenes = ref<SceneDto[]>([])
const loading = ref(false)

// 支持 ?tab=scene|resource 从外部（如每日任务兜底）定位到对应标签页
watch(
  () => route.query.tab,
  (tab) => {
    if (tab === 'scene' || tab === 'resource') activeTab.value = tab
  },
  { immediate: true },
)

async function loadScenes() {
  loading.value = true
  try {
    const res = await listScenes({
      category: category.value || undefined,
      level: level.value || undefined,
      page: 1,
      pageSize: 100,
    })
    scenes.value = res.list
  } finally {
    loading.value = false
  }
}

watch([category, level], () => {
  loadScenes()
})

onMounted(loadScenes)
</script>

<style scoped lang="scss">
.practice-body {
  padding: 0 16px;
}

.filter-row {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.scene-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  min-height: 80px;
}
</style>
