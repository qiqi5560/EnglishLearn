<template>
  <div class="resource-browser">
    <div class="resource-search glass-card">
      <el-icon class="search-icon"><Search /></el-icon>
      <el-input v-model="keyword" placeholder="搜索素材标题" clearable />
      <span v-if="keyword" class="search-count text-muted">{{ resourceStore.resourceList.length }} 项</span>
    </div>
    <p v-if="keyword" class="search-status text-muted">正在搜索“{{ keyword }}”</p>
    <div v-loading="resourceStore.loading" class="resource-body">
      <div class="resource-list stagger">
        <ResourceCard v-for="resource in resourceStore.resourceList" :key="resource.id" :title="resource.title" :type="resource.type" :category="resource.category" :level="resource.level" :duration-sec="resource.durationSec ?? undefined" @click="$router.push(`/resource/${resource.id}`)" />
      </div>
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
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);

// ---------------- 桌面搜索胶囊 ----------------
.resource-search {
  display: flex;
  align-items: center;
  gap: 10px;
  max-width: 460px;
  padding: 4px 18px;
  border-radius: 999px;
  transition:
    border-color 0.3s ease,
    box-shadow 0.35s $ease-apple;

  &:focus-within {
    border-color: rgba(59, 111, 224, 0.4);
    box-shadow: 0 14px 34px rgba(59, 111, 224, 0.16);
  }
}

.search-icon {
  color: var(--muted);
  font-size: 17px;
}

.resource-search :deep(.el-input__wrapper) {
  padding: 6px 0;
  background: transparent;
  box-shadow: none;
}

.search-count {
  margin-left: auto;
  font-size: 12.5px;
  white-space: nowrap;
}

.search-status {
  margin: 14px 2px 0;
  font-size: 12.5px;
}

// ---------------- 资源网格 ----------------
.resource-body {
  min-height: 200px;
  margin-top: 18px;
}

.resource-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

// ---------------- 空状态 ----------------
.resource-empty {
  display: grid;
  place-items: center;
  padding: 60px 20px;
  text-align: center;

  strong {
    color: var(--ink);
    font-size: 16px;
  }

  p {
    margin: 6px 0 0;
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

@media (max-width: 900px) {
  .resource-list {
    grid-template-columns: 1fr;
  }
}
</style>