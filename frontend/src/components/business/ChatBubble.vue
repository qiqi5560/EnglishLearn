<template>
  <div class="chat-bubble" :class="speaker">
    <div class="bubble-avatar">
      <el-avatar :size="40" :icon="speaker === 'ai' ? 'MagicStick' : 'User'" />
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
          class="play-btn"
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
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

.chat-bubble {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 20px;

  &:hover .bubble-avatar {
    transform: translateY(-3px) scale(1.06);
    box-shadow: 0 12px 26px rgba(31, 42, 68, 0.16);
  }

  &:hover .bubble-body {
    box-shadow: 0 14px 32px rgba(31, 42, 68, 0.12);
  }
}

.bubble-avatar {
  flex-shrink: 0;
  border-radius: 50%;
  transition:
    transform 0.5s $ease-spring,
    box-shadow 0.4s $ease-apple;

  :deep(.el-avatar) {
    background: var(--card);
    color: var(--primary);
    border: 1px solid var(--border);
  }
}

.chat-bubble.ai :deep(.el-avatar) {
  color: #fff;
  background: linear-gradient(150deg, var(--primary), var(--ink));
  border-color: transparent;
  box-shadow: 0 6px 16px rgba(59, 111, 224, 0.3);
}

.chat-bubble.user:hover .bubble-avatar {
  box-shadow: 0 12px 26px rgba(59, 111, 224, 0.2);
}

.bubble-content {
  max-width: 74%;
  display: flex;
  flex-direction: column;
  align-items: flex-start;

  .user & {
    align-items: flex-end;
  }
}

.bubble-body {
  padding: 13px 18px;
  border-radius: 20px 20px 20px 6px;
  background: var(--card);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
  transition:
    transform 0.35s $ease-apple,
    box-shadow 0.35s $ease-apple,
    border-color 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(59, 111, 224, 0.24);
  }

  .user & {
    color: #fff;
    background: var(--primary);
    border-color: transparent;
    border-radius: 20px 20px 6px 20px;
    box-shadow: 0 10px 24px rgba(59, 111, 224, 0.26);

    .content-zh {
      color: rgba(255, 255, 255, 0.84);
    }
  }
}

.content-en {
  font-size: 15px;
  line-height: 1.6;
}

.content-zh {
  margin-top: 5px;
  font-size: 13px;
  line-height: 1.55;
  color: var(--muted);
}

.bubble-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 6px;

  .time {
    font-size: 11.5px;
    color: var(--muted);
  }

  .play-btn {
    padding: 0;
    font-size: 12.5px;
    font-weight: 600;
    color: var(--primary);

    :deep(.el-icon) {
      transition: transform 0.4s $ease-spring;
    }

    &:hover :deep(.el-icon) {
      transform: translateX(2px) scale(1.12);
    }
  }
}
</style>