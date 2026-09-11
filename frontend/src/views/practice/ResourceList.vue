<template>
  <div>
    <el-input v-model="keyword" placeholder="搜索素材标题" clearable size="small">
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
    </el-input>

    <div v-loading="resourceStore.loading" class="resource-list">
      <ResourceCard
        v-for="r in resourceStore.resourceList"
        :key="r.id"
        :title="r.title"
        :type="r.type"
        :category="r.category"
        :level="r.level"
        :duration-sec="r.durationSec ?? undefined"
        @click="$router.push(`/resource/${r.id}`)"
      />
    </div>
    <el-empty v-if="!resourceStore.loading && !resourceStore.resourceList.length" description="暂无学习资源" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import ResourceCard from '@/components/business/ResourceCard.vue'
import { useResourceStore } from '@/stores/resource'

const resourceStore = useResourceStore()
const keyword = ref('')

let timer: ReturnType<typeof setTimeout> | undefined

watch(keyword, () => {
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => {
    resourceStore.loadResources({ keyword: keyword.value.trim() })
  }, 300)
})

onMounted(() => {
  resourceStore.loadResources({ keyword: '' })
})

onUnmounted(() => {
  if (timer) clearTimeout(timer)
})
</script>

<style scoped lang="scss">
.resource-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
  min-height: 80px;
}
</style>
