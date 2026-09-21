<template>
  <el-popover placement="bottom-end" :width="236" trigger="click" popper-class="share-popper">
    <template #reference>
      <button class="share-btn" type="button">
        <el-icon><Share /></el-icon>
        <span>分享</span>
      </button>
    </template>

    <div class="share-panel">
      <div class="share-title">分享到</div>
      <div class="share-grid">
        <button
          v-for="c in channels"
          :key="c.key"
          class="share-item"
          :class="{ 'is-done': lastChannel === c.key }"
          type="button"
          @click="onShare(c)"
        >
          <span class="share-dot" :style="{ background: c.color }">{{ c.label[0] }}</span>
          <span class="share-label">{{ c.label }}</span>
          <el-icon v-if="lastChannel === c.key" class="share-check"><Select /></el-icon>
        </button>
      </div>
      <p class="share-tip">链接已复制，去对应平台粘贴发布</p>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as socialApi from '@/api/modules/social'

const props = defineProps<{
  contentType: string
  contentId?: number | null
  /** 自定义分享链接，缺省时按内容类型拼装站内哈希路由 */
  url?: string
}>()

const channels = [
  { key: 'weibo', label: '微博', color: '#e6162d', tip: '已复制，去微博粘贴分享' },
  { key: 'xiaohongshu', label: '小红书', color: '#ff2442', tip: '已复制，去小红书粘贴发布' },
  { key: 'wechat', label: '微信', color: '#22c55e', tip: '已复制，去微信粘贴给好友' },
  { key: 'copy', label: '复制链接', color: '#3b6fe0', tip: '链接已复制' },
]

const lastChannel = ref('')

function buildUrl() {
  const base = typeof location !== 'undefined' ? location.origin : ''
  const id = props.contentId
  if (id == null) return `${base}/#/community`
  if (props.contentType === 'scene') return `${base}/#/scene/${id}`
  if (props.contentType === 'resource') return `${base}/#/resource/${id}`
  if (props.contentType === 'achievement') return `${base}/#/profile`
  return `${base}/#/community/post/${id}`
}

async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch {
    // 非安全上下文兜底
    const input = document.createElement('textarea')
    input.value = text
    input.style.position = 'fixed'
    input.style.opacity = '0'
    document.body.appendChild(input)
    input.select()
    const ok = document.execCommand('copy')
    document.body.removeChild(input)
    return ok
  }
}

async function onShare(channel: { key: string; label: string; tip: string }) {
  const link = props.url || buildUrl()
  const ok = await copyText(link)
  lastChannel.value = channel.key
  ElMessage.success(ok ? channel.tip : '复制失败，请手动复制页面链接')
  try {
    await socialApi.shareContent({
      contentType: props.contentType,
      contentId: props.contentId ?? null,
      channel: channel.key,
      url: link,
    })
  } catch {
    /* 留痕失败不影响分享动作 */
  }
}
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

.share-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 15px;
  border: 1px solid rgba(59, 111, 224, 0.24);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  color: var(--primary);
  cursor: pointer;
  transition:
    transform 0.35s $ease-apple,
    box-shadow 0.35s $ease-apple,
    background 0.3s ease;

  .el-icon {
    font-size: 15px;
    transition: transform 0.4s $ease-spring;
  }

  &:hover {
    transform: translateY(-2px);
    background: #fff;
    box-shadow: 0 10px 20px rgba(31, 42, 68, 0.12);

    .el-icon {
      transform: rotate(-12deg) scale(1.08);
    }
  }
}

.share-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--muted);
  margin-bottom: 10px;
}

.share-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.share-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.86);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
  cursor: pointer;
  transition:
    transform 0.32s $ease-apple,
    border-color 0.3s ease,
    background 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(59, 111, 224, 0.3);
    background: #fff;
  }

  &.is-done {
    border-color: rgba(34, 197, 94, 0.5);
    background: rgba(34, 197, 94, 0.08);
  }
}

.share-dot {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 8px;
  color: #fff;
  font-size: 11px;
  font-weight: 800;
}

.share-label {
  white-space: nowrap;
}

.share-check {
  margin-left: auto;
  color: var(--success);
  animation: pop-in 0.36s $ease-spring both;
}

.share-tip {
  margin: 10px 0 0;
  font-size: 11.5px;
  line-height: 1.6;
  color: var(--muted);
}

@keyframes pop-in {
  from {
    transform: scale(0.4);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
