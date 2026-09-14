<template>
  <div class="resource-browser">
    <div class="resource-search"><el-icon><Search /></el-icon><el-input v-model="keyword" placeholder="搜索素材标题" clearable /></div>
    <p v-if="keyword" class="search-status text-muted">正在搜索“{{ keyword }}”</p>
    <div v-loading="resourceStore.loading" class="resource-list">
      <ResourceCard v-for="resource in resourceStore.resourceList" :key="resource.id" :title="resource.title" :type="resource.type" :category="resource.category" :level="resource.level" :duration-sec="resource.durationSec ?? undefined" @click="$router.push(`/resource/${resource.id}`)" />
    </div>
    <div v-if="!resourceStore.loading && !resourceStore.resourceList.length" class="resource-empty"><span class="empty-mark">⌕</span><strong>{{ keyword ? '没有找到相关资源' : '暂无学习资源' }}</strong><p class="text-muted">{{ keyword ? '试试更短的关键词。' : '稍后再来看看新的学习内容。' }}</p></div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ResourceCard from '@/components/business/ResourceCard.vue'
import { useResourceStore } from '@/stores/resource'

const resourceStore = useResourceStore(); const route = useRoute(); const router = useRouter(); const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
let timer: ReturnType<typeof setTimeout> | undefined
watch(() => route.query.keyword, (value) => { if (typeof value === 'string' && value !== keyword.value) keyword.value = value })
watch(keyword, () => { if (timer) clearTimeout(timer); timer = setTimeout(() => { router.replace({ query: { ...route.query, keyword: keyword.value.trim() || undefined } }); resourceStore.loadResources({ keyword: keyword.value.trim() }) }, 300) })
onMounted(() => resourceStore.loadResources({ keyword: keyword.value.trim() }))
onUnmounted(() => { if (timer) clearTimeout(timer) })
</script>

<style scoped lang="scss">
.resource-search { display: flex; align-items: center; gap: 10px; padding: 2px 15px; background: #fff; border: 1px solid #e8e8ed; border-radius: 999px; }.resource-search > .el-icon { color: #86868b; font-size: 17px; }.resource-search :deep(.el-input__wrapper) { padding: 0; background: transparent; box-shadow: none; }.search-status { margin: 13px 2px 0; font-size: 12px; }.resource-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; min-height: 180px; margin-top: 18px; }.resource-empty { display: grid; place-items: center; padding: 58px 20px; text-align: center; }.empty-mark { display: grid; place-items: center; width: 48px; height: 48px; margin-bottom: 14px; color: #0071e3; background: #eaf3ff; border-radius: 50%; font-size: 27px; }.resource-empty strong { color: #171717; }.resource-empty p { margin: 5px 0 0; font-size: 13px; } @media (max-width: 700px) { .resource-list { grid-template-columns: 1fr; } }
</style>
