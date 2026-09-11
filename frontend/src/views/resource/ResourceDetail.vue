<template>
  <div class="resource-detail">
    <AppHeader title="素材详情" back />

    <div v-loading="loading" class="detail-body">
      <template v-if="resource">
        <el-card shadow="never">
          <div class="resource-title">{{ resource.title }}</div>
          <div class="resource-meta">
            <el-tag size="small" effect="plain">{{ resource.type }}</el-tag>
            <el-tag size="small" type="info" effect="plain">{{ resource.category }}</el-tag>
            <LevelTag :level="resource.level || 'B1'" />
            <span v-if="resource.durationSec" class="text-muted">
              {{ Math.floor(resource.durationSec / 60) }} 分钟
            </span>
          </div>
          <el-divider />
          <p class="text-muted">这是一段适合精听与跟读的素材，支持倍速调节、双语字幕与听写挖空。</p>
        </el-card>

        <div class="training-entry">
          <el-card shadow="hover" class="entry-card" @click="go('listen')">
            <el-icon :size="28" color="#3b6fe0"><Headset /></el-icon>
            <div class="entry-name">精听训练</div>
            <div class="text-muted">逐句精听 · 听写挖空</div>
          </el-card>
          <el-card shadow="hover" class="entry-card" @click="go('read')">
            <el-icon :size="28" color="#ff6a4d"><Microphone /></el-icon>
            <div class="entry-name">跟读训练</div>
            <div class="text-muted">逐句跟读 · 实时评分</div>
          </el-card>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="素材不存在或已下架">
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
import { resourceDetail } from '@/api/modules/resource'
import type { ResourceDto } from '@/types/api'

const route = useRoute()
const router = useRouter()
const resource = ref<ResourceDto | null>(null)
const loading = ref(false)

function go(mode: 'listen' | 'read') {
  router.push(`/resource/${route.params.id}/${mode}`)
}

onMounted(async () => {
  const rid = Number(route.params.id)
  if (!Number.isInteger(rid)) return
  loading.value = true
  try {
    resource.value = await resourceDetail(rid)
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

.resource-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 8px;
}

.resource-meta {
  display: flex;
  gap: 8px;
  align-items: center;
}

.training-entry {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 16px;
}

.entry-card {
  text-align: center;
  cursor: pointer;

  .entry-name {
    font-weight: 700;
    margin: 8px 0 4px;
  }
}
</style>
