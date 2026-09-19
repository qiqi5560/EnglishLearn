<template>
  <div class="resource-detail page">
    <AppHeader title="素材详情" back />

    <main v-loading="loading" class="page-shell detail-shell">
      <template v-if="resource">
        <div class="detail-grid">
          <!-- 左侧：素材正文 / 信息 -->
          <section class="detail-main glass-card peek-host">
            <div class="peek-slot"><PeekMascot :size="50" /></div>
            <p class="eyebrow">LEARNING MATERIAL</p>
            <h1 class="resource-title">{{ resource.title }}</h1>
            <div class="resource-meta">
              <span class="meta-chip">{{ resource.type }}</span>
              <span class="meta-chip">{{ resource.category }}</span>
              <LevelTag :level="resource.level || 'B1'" />
              <span v-if="resource.durationSec" class="meta-text text-muted">{{ Math.floor(resource.durationSec / 60) }} 分钟</span>
            </div>
            <el-divider />
            <p class="resource-desc text-muted">这是一段适合精听与跟读的素材，支持倍速调节、双语字幕与听写挖空。</p>

            <div class="feature-list">
              <div class="feature-item">
                <el-icon><Headset /></el-icon>
                <div class="feature-text"><strong>逐句精听</strong><span>拆解每句发音，听写挖空自测</span></div>
              </div>
              <div class="feature-item">
                <el-icon><Microphone /></el-icon>
                <div class="feature-text"><strong>逐句跟读</strong><span>录音对比，实时四维评分</span></div>
              </div>
            </div>
          </section>

          <!-- 右侧：训练入口 + 元信息 -->
          <aside class="detail-side stagger">
            <div class="side-block">
              <h2 class="side-title">开始训练</h2>
              <button type="button" class="entry-card glass-card hover-lift" @click="go('listen')">
                <span class="entry-icon"><el-icon :size="22"><Headset /></el-icon></span>
                <span class="entry-text"><strong>精听训练</strong><em>逐句精听 · 听写挖空</em></span>
                <el-icon class="entry-arrow"><ArrowRight /></el-icon>
              </button>
              <button type="button" class="entry-card glass-card hover-lift" @click="go('read')">
                <span class="entry-icon accent"><el-icon :size="22"><Microphone /></el-icon></span>
                <span class="entry-text"><strong>跟读训练</strong><em>逐句跟读 · 实时评分</em></span>
                <el-icon class="entry-arrow"><ArrowRight /></el-icon>
              </button>
            </div>

            <div class="side-block info-block glass-card">
              <h2 class="side-title">素材信息</h2>
              <dl class="info-list">
                <div><dt>类型</dt><dd>{{ resource.type }}</dd></div>
                <div><dt>分类</dt><dd>{{ resource.category }}</dd></div>
                <div><dt>难度</dt><dd>{{ resource.level || 'B1' }}</dd></div>
                <div><dt>时长</dt><dd>{{ resource.durationSec ? Math.floor(resource.durationSec / 60) + ' 分钟' : '—' }}</dd></div>
              </dl>
            </div>
          </aside>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="素材不存在或已下架">
        <el-button type="primary" @click="$router.push('/practice')">返回练习</el-button>
      </el-empty>
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from '@/components/base/AppHeader.vue'
import PeekMascot from '@/components/base/PeekMascot.vue'
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
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

.detail-shell {
  min-height: 60vh;
}

// ---------------- 桌面两栏 ----------------
.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 24px;
  align-items: start;
}

.detail-main {
  position: relative;
  padding: 38px 36px 34px;
}

.peek-slot {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
}

.eyebrow {
  margin: 0 0 10px;
  color: var(--muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.resource-title {
  margin: 0;
  color: var(--ink);
  font-size: clamp(24px, 2.4vw, 32px);
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.2;
}

.resource-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
}

.meta-chip {
  padding: 5px 13px;
  color: var(--ink);
  font-size: 12.5px;
  font-weight: 600;
  background: rgba(59, 111, 224, 0.08);
  border: 1px solid rgba(59, 111, 224, 0.16);
  border-radius: 999px;
}

.meta-text {
  font-size: 13px;
}

.resource-desc {
  margin: 0;
  font-size: 15px;
  line-height: 1.85;
}

.detail-main :deep(.el-divider) {
  margin: 22px 0;
  border-color: var(--border);
}

// ---------------- 能力说明 ----------------
.feature-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 26px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  transition:
    transform 0.35s $ease-apple,
    border-color 0.3s ease,
    box-shadow 0.35s ease;

  &:hover {
    transform: translateY(-3px);
    border-color: rgba(59, 111, 224, 0.28);
    box-shadow: 0 14px 28px rgba(31, 42, 68, 0.1);

    .el-icon {
      transform: rotate(-6deg) scale(1.08);
    }
  }

  .el-icon {
    display: grid;
    flex: 0 0 auto;
    place-items: center;
    width: 40px;
    height: 40px;
    color: var(--primary);
    font-size: 19px;
    background: rgba(59, 111, 224, 0.1);
    border-radius: var(--radius-md);
    transition: transform 0.5s $ease-spring;
  }
}

.feature-text {
  min-width: 0;

  strong {
    display: block;
    color: var(--ink);
    font-size: 14px;
  }

  span {
    display: block;
    margin-top: 2px;
    color: var(--muted);
    font-size: 12.5px;
  }
}

// ---------------- 右侧栏 ----------------
.detail-side {
  position: sticky;
  top: 80px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.side-title {
  margin: 0 0 14px;
  color: var(--ink);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.entry-card {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 16px 18px;
  font: inherit;
  text-align: left;
  cursor: pointer;

  & + .entry-card {
    margin-top: 12px;
  }

  &:hover {
    .entry-icon {
      transform: rotate(-6deg) scale(1.08);
    }

    .entry-arrow {
      transform: translateX(4px);
    }
  }

  &:focus-visible {
    outline: 2px solid var(--primary);
    outline-offset: 2px;
  }
}

.entry-icon {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 44px;
  height: 44px;
  color: var(--primary);
  background: rgba(59, 111, 224, 0.12);
  border-radius: var(--radius-md);
  transition: transform 0.5s $ease-spring;

  &.accent {
    color: var(--accent);
    background: rgba(255, 106, 77, 0.12);
  }
}

.entry-text {
  flex: 1;
  min-width: 0;

  strong {
    display: block;
    color: var(--ink);
    font-size: 15px;
    font-weight: 700;
  }

  em {
    display: block;
    margin-top: 2px;
    color: var(--muted);
    font-size: 12.5px;
    font-style: normal;
  }
}

.entry-arrow {
  flex: 0 0 auto;
  color: var(--primary);
  font-size: 16px;
  transition: transform 0.45s $ease-spring;
}

.info-block {
  padding: 20px 18px;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 0;

  > div {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    font-size: 13.5px;
  }

  dt {
    color: var(--muted);
  }

  dd {
    margin: 0;
    color: var(--ink);
    font-weight: 600;
  }
}

// ---------------- 响应式 ----------------
@media (max-width: 900px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-side {
    position: static;
  }

  .detail-main {
    padding: 28px 22px 24px;
  }

  .feature-list {
    grid-template-columns: 1fr;
  }

  .peek-slot {
    display: none;
  }
}
</style>