<template>
  <div class="messages page">
    <AppHeader title="我的消息" back>
      <template #right>
        <router-link to="/notifications" class="notice-link">
          <el-badge :value="socialStore.unread.notification" :hidden="!socialStore.unread.notification" :max="99">
            <span class="notice-host">
              <el-icon><Bell /></el-icon>
              <span>通知</span>
            </span>
          </el-badge>
        </router-link>
      </template>
    </AppHeader>

    <div class="page-shell msg-body">
      <div class="msg-layout glass-card" :class="{ 'is-chat': !!peer }">
        <!-- 左：会话列表 -->
        <aside class="conv-col">
          <div class="conv-head">
            <h3>会话</h3>
            <span class="text-muted">{{ conversations.length }} 位联系人</span>
          </div>

          <div v-loading="listLoading" class="conv-list">
            <button
              v-for="c in conversations"
              :key="c.peerId"
              class="conv-item"
              :class="{ 'is-active': peer?.userId === c.peerId }"
              type="button"
              @click="openPeer(c.peerId)"
            >
              <el-avatar :size="40" :style="{ background: avatarColor(c.peerName) }">
                {{ c.peerName[0]?.toUpperCase() }}
              </el-avatar>
              <div class="conv-text">
                <div class="conv-line">
                  <strong>{{ c.peerName }}</strong>
                  <span class="conv-time text-muted">{{ shortTime(c.lastTime) }}</span>
                </div>
                <div class="conv-preview text-muted">
                  <em v-if="c.lastMine">我：</em>{{ c.lastMessage }}
                </div>
              </div>
              <el-badge v-if="c.unread" :value="c.unread" :max="99" class="conv-badge" />
            </button>

            <el-empty
              v-if="!listLoading && !conversations.length"
              :image-size="64"
              description="还没有会话，去社区认识新同学"
            >
              <el-button type="primary" round @click="$router.push('/community')">逛逛社区</el-button>
            </el-empty>
          </div>
        </aside>

        <!-- 右：聊天窗 -->
        <section class="chat-col">
          <template v-if="peer">
            <header class="chat-head">
              <button class="chat-back" type="button" @click="peer = null">
                <el-icon><ArrowLeft /></el-icon>
              </button>
              <el-avatar :size="34" :style="{ background: avatarColor(peer.nickname) }">
                {{ peer.nickname[0]?.toUpperCase() }}
              </el-avatar>
              <strong class="chat-name">{{ peer.nickname }}</strong>
              <router-link class="chat-profile" :to="`/user/${peer.userId}`">主页</router-link>
            </header>

            <div ref="bodyRef" v-loading="loading" class="chat-body">
              <div v-for="m in messages" :key="m.id" class="bubble-row" :class="{ 'is-mine': m.mine }">
                <div class="bubble">{{ m.content }}</div>
                <span class="bubble-time text-muted">{{ shortTime(m.createTime) }}</span>
              </div>

              <el-empty
                v-if="!loading && !messages.length"
                :image-size="60"
                description="还没有聊天记录，打个招呼吧"
              />
            </div>

            <footer class="chat-input">
              <el-input
                v-model="draft"
                type="textarea"
                :rows="2"
                resize="none"
                maxlength="500"
                show-word-limit
                placeholder="输入消息，Enter 发送（Shift+Enter 换行）"
                @keydown.enter.exact.prevent="onSend"
              />
              <el-button type="primary" class="send-btn" :loading="sending" @click="onSend">发送</el-button>
            </footer>
          </template>

          <div v-else class="chat-empty">
            <PeekMascot :size="72" />
            <h3>选择一位联系人开始聊天</h3>
            <p class="text-muted">在社区或他人主页点击「发私信」，就能在这里继续交流。</p>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
import PeekMascot from '@/components/base/PeekMascot.vue'
import * as socialApi from '@/api/modules/social'
import { useSocialStore } from '@/stores/social'
import type { ChatMessageItem, ConversationDto, PeerDto } from '@/types/api'

const route = useRoute()
const socialStore = useSocialStore()

const conversations = ref<ConversationDto[]>([])
const peer = ref<PeerDto | null>(null)
const messages = ref<ChatMessageItem[]>([])
const draft = ref('')
const listLoading = ref(false)
const loading = ref(false)
const sending = ref(false)
const bodyRef = ref<HTMLElement | null>(null)

const AVATAR_COLORS = ['#3b6fe0', '#22a06b', '#ff6a4d', '#e8a23d', '#7a5af8', '#2bb3c0']

function avatarColor(name: string) {
  let sum = 0
  for (const ch of name) sum += ch.charCodeAt(0)
  return AVATAR_COLORS[sum % AVATAR_COLORS.length]
}

function shortTime(t?: string) {
  if (!t) return ''
  return t.length >= 16 ? t.slice(5, 16) : t
}

async function scrollBottom() {
  await nextTick()
  const el = bodyRef.value
  if (el) el.scrollTop = el.scrollHeight
}

async function loadConversations() {
  listLoading.value = true
  try {
    conversations.value = await socialApi.listConversations()
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    listLoading.value = false
  }
}

async function openPeer(peerId: number) {
  loading.value = true
  try {
    const res = await socialApi.messageHistory(peerId)
    peer.value = res.peer
    messages.value = res.list
    await scrollBottom()
    const conv = conversations.value.find((c) => c.peerId === peerId)
    if (conv && conv.unread) {
      socialStore.markMessageRead(conv.unread)
      conv.unread = 0
    }
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    loading.value = false
  }
}

async function onSend() {
  const text = draft.value.trim()
  if (!peer.value || !text) return
  sending.value = true
  try {
    await socialApi.sendMessage(peer.value.userId, text)
    draft.value = ''
    await openPeer(peer.value.userId)
    await loadConversations()
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    sending.value = false
  }
}

onMounted(async () => {
  await loadConversations()
  void socialStore.refreshUnread()
  const queryPeer = Number(route.query.peer)
  if (Number.isInteger(queryPeer) && queryPeer > 0) {
    await openPeer(queryPeer)
  } else if (conversations.value.length) {
    await openPeer(conversations.value[0].peerId)
  }
})
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

.msg-body {
  padding-bottom: 30px;
}

.notice-link {
  text-decoration: none;

  .notice-host {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    height: 34px;
    padding: 0 15px;
    border: 1px solid rgba(59, 111, 224, 0.24);
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.9);
    font-size: 13px;
    font-weight: 600;
    color: var(--primary);
    transition:
      transform 0.35s $ease-apple,
      box-shadow 0.35s $ease-apple;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 10px 20px rgba(31, 42, 68, 0.12);
    }
  }
}

.msg-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  min-height: 62vh;
  overflow: hidden;
}

// ---------------- 会话列表 ----------------
.conv-col {
  display: flex;
  flex-direction: column;
  min-width: 0;
  border-right: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.5);
}

.conv-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 20px 12px;

  h3 {
    margin: 0;
    font-size: 17px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }

  span {
    font-size: 12px;
  }
}

.conv-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.conv-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  border: none;
  border-radius: var(--radius-md);
  background: transparent;
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    background 0.3s ease,
    transform 0.35s $ease-apple;

  &:hover {
    background: rgba(59, 111, 224, 0.07);
    transform: translateX(3px);
  }

  &.is-active {
    background: rgba(59, 111, 224, 0.13);
  }

  .conv-text {
    flex: 1;
    min-width: 0;
  }

  .conv-line {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 8px;

    strong {
      font-size: 14px;
      font-weight: 700;
    }
  }

  .conv-time {
    font-size: 11.5px;
    flex-shrink: 0;
  }

  .conv-preview {
    margin-top: 3px;
    font-size: 12.5px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;

    em {
      font-style: normal;
      color: var(--primary);
    }
  }

  .conv-badge {
    flex-shrink: 0;
  }
}

// ---------------- 聊天窗 ----------------
.chat-col {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 22px;
  border-bottom: 1px solid var(--border);

  .chat-back {
    display: none;
    place-items: center;
    width: 30px;
    height: 30px;
    border: none;
    border-radius: 999px;
    background: rgba(59, 111, 224, 0.1);
    color: var(--primary);
    cursor: pointer;
  }

  .chat-name {
    font-size: 15px;
    font-weight: 700;
  }

  .chat-profile {
    margin-left: auto;
    font-size: 12.5px;
    font-weight: 600;
    color: var(--primary);
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }
}

.chat-body {
  flex: 1;
  min-height: 300px;
  max-height: 52vh;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  background:
    radial-gradient(900px 320px at 12% -10%, rgba(91, 139, 240, 0.09), transparent 70%),
    radial-gradient(700px 300px at 100% 110%, rgba(124, 92, 255, 0.07), transparent 70%);
}

.bubble-row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  animation: bubble-in 0.36s $ease-apple both;

  .bubble {
    max-width: min(560px, 74%);
    padding: 11px 15px;
    border-radius: 16px 16px 16px 4px;
    background: rgba(255, 255, 255, 0.95);
    border: 1px solid var(--border);
    font-size: 14.5px;
    line-height: 1.65;
    box-shadow: var(--shadow-sm);
  }

  .bubble-time {
    font-size: 11px;
  }

  &.is-mine {
    align-items: flex-end;

    .bubble {
      border: none;
      border-radius: 16px 16px 4px 16px;
      color: #fff;
      background: linear-gradient(135deg, #5b8bf0, #3b6fe0 55%, #2f5bb3);
      box-shadow: 0 10px 22px rgba(59, 111, 224, 0.24);
    }
  }
}

@keyframes bubble-in {
  from {
    opacity: 0;
    transform: translateY(8px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

.chat-input {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding: 14px 22px 18px;
  border-top: 1px solid var(--border);

  :deep(.el-textarea) {
    flex: 1;
  }

  :deep(.el-textarea__inner) {
    border-radius: var(--radius-md);
    font-size: 14.5px;
    line-height: 1.7;
    padding: 10px 14px;
  }

  .send-btn {
    height: 42px;
    padding: 0 26px;
    border-radius: 999px;
    font-weight: 600;
    transition:
      transform 0.35s $ease-spring,
      box-shadow 0.35s $ease-apple;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 14px 26px rgba(59, 111, 224, 0.3);
    }
  }
}

.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  text-align: center;

  h3 {
    margin: 12px 0 0;
    font-size: 19px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }

  p {
    margin: 0;
    font-size: 13.5px;
    max-width: 320px;
    line-height: 1.7;
  }
}

// ---------------- 窄屏：列表与聊天切换 ----------------
@media (max-width: 900px) {
  .msg-layout {
    grid-template-columns: 1fr;
  }

  .msg-layout.is-chat .conv-col {
    display: none;
  }

  .msg-layout:not(.is-chat) .chat-col {
    display: none;
  }

  .chat-head .chat-back {
    display: grid;
  }
}
</style>
