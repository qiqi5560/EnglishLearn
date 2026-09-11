<template>
  <div class="chat-bubble" :class="speaker">
    <div class="bubble-avatar">
      <el-avatar :size="34" :icon="speaker === 'ai' ? 'MagicStick' : 'User'" />
    </div>
    <div class="bubble-content">
      <div class="bubble-body">
        <div class="content-en reading-text">{{ contentEn }}</div>
        <div v-if="contentZh" class="content-zh">{{ contentZh }}</div>
      </div>
      <div class="bubble-meta">
        <el-button
          v-if="speaker === 'ai'"
          link
          size="small"
          @click="$emit('play')"
        >
          <el-icon><Headset /></el-icon> 播放
        </el-button>
        <span class="time">{{ time }}</span>
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  speaker: 'user' | 'ai'
  contentEn: string
  contentZh?: string
  time?: string
}>()

defineEmits<{ (e: 'play'): void }>()
</script>

<style scoped lang="scss">
.chat-bubble {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;

  &.user {
    flex-direction: row-reverse;

    .bubble-body {
      background: var(--primary);
      color: #fff;

      .content-zh {
        color: rgba(255, 255, 255, 0.85);
      }
    }
  }
}

.bubble-avatar {
  flex-shrink: 0;
}

.bubble-content {
  max-width: 76%;
  display: flex;
  flex-direction: column;
  align-items: flex-start;

  .user & {
    align-items: flex-end;
  }
}

.bubble-body {
  padding: 10px 14px;
  border-radius: 14px;
  background: var(--card);
  box-shadow: var(--shadow-sm);
}

.content-en {
  font-size: 15px;
}

.content-zh {
  margin-top: 4px;
  font-size: 13px;
  color: var(--muted);
}

.bubble-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;

  .time {
    font-size: 11px;
    color: var(--muted);
  }
}
</style>
