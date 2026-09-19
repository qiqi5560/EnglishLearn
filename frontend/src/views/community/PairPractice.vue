<template>
  <div class="pair-practice page">
    <AppHeader title="结伴练习" back>
      <template #right>
        <span class="header-chip">{{ rooms.length }} 个房间进行中</span>
      </template>
    </AppHeader>

    <div class="page-shell pair-body">
      <header class="pair-hero">
        <h2>找一位搭子，把英语说出来</h2>
        <p class="text-muted">加入进行中的房间，或自己发起一个话题，两人一组开口练习。</p>
      </header>

      <div class="pair-layout">
        <!-- 左栏：房间列表 -->
        <main class="room-col">
          <div class="room-head">
            <h3>进行中的房间</h3>
            <span class="text-muted">共 {{ rooms.length }} 个</span>
          </div>

          <div class="room-list stagger">
            <article v-for="room in rooms" :key="room.id" class="glass-card hover-lift room-card">
              <span class="room-flag"><el-icon><Microphone /></el-icon></span>

              <div class="room-info">
                <div class="room-title">{{ room.topic }}</div>
                <div class="room-meta text-muted">
                  <span>发起人：{{ room.owner }}</span>
                  <span class="dot"></span>
                  <span>水平 {{ room.level }}</span>
                  <span class="dot"></span>
                  <span>{{ room.people }}/2 人</span>
                </div>
              </div>

              <el-button size="small" type="success" class="join-btn" @click="onJoin(room)">加入</el-button>
            </article>
          </div>
        </main>

        <!-- 右栏：发房间 + 练习引导 -->
        <aside class="side-col stagger">
          <div class="glass-card peek-host create-card">
            <PeekMascot :size="50" />
            <h3>发起一个练习房间</h3>
            <p class="text-muted">选一个你熟悉的话题，等一位水平相近的搭子加入。</p>
            <el-button type="primary" class="create-btn" @click="onCreate">创建房间</el-button>
          </div>

          <div class="glass-card guide-card">
            <div class="side-title">练习流程</div>
            <ol class="guide-list">
              <li v-for="(g, i) in guides" :key="g">
                <span class="guide-index">{{ i + 1 }}</span>
                <span class="guide-text">{{ g }}</span>
              </li>
            </ol>
          </div>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
import PeekMascot from '@/components/base/PeekMascot.vue'

const rooms = [
  { id: 1, topic: '商务英语 · 模拟会议', owner: 'Leo', level: 'B1', people: 1 },
  { id: 2, topic: '雅思口语 · Part 3', owner: 'Momo', level: 'B2', people: 1 },
  { id: 3, topic: '日常闲聊 · 周末计划', owner: 'Cici', level: 'A2', people: 2 },
]

// 纯展示文案
const guides = [
  '进入房间后先用英文互相自我介绍',
  '围绕房间话题轮流表达，每人 2–3 分钟',
  '结束后互相给一句反馈，再约下一次',
]

function onCreate() {
  ElMessage.success('房间创建成功（接入音视频通道后实现）')
}

function onJoin(room: any) {
  ElMessage.info(`加入「${room.topic}」房间`)
}
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

.header-chip {
  padding: 7px 16px;
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--success);
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(34, 160, 107, 0.24);
  box-shadow: var(--shadow-sm);
}

// ---------------- 页头 ----------------
.pair-hero {
  margin-bottom: 26px;

  h2 {
    margin: 0;
    font-size: 32px;
    font-weight: 700;
    letter-spacing: -0.03em;
  }

  p {
    margin: 10px 0 0;
    font-size: 14.5px;
    max-width: 560px;
  }
}

// ---------------- 两栏布局 ----------------
.pair-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 28px;
  align-items: start;
}

.room-col {
  min-width: 0;
}

.room-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;

  h3 {
    margin: 0;
    font-size: 20px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }

  span {
    font-size: 12.5px;
  }
}

.room-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.room-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 22px;

  .room-flag {
    flex: 0 0 auto;
    width: 44px;
    height: 44px;
    display: grid;
    place-items: center;
    font-size: 21px;
    border-radius: 14px;
    color: var(--primary);
    background: rgba(59, 111, 224, 0.1);
    transition: transform 0.5s $ease-spring;
  }

  &:hover .room-flag {
    transform: scale(1.08) rotate(-6deg);
  }

  .room-info {
    flex: 1;
    min-width: 0;
  }

  .room-title {
    font-size: 16px;
    font-weight: 700;
    letter-spacing: -0.01em;
  }

  .room-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 5px;
    font-size: 12.5px;

    .dot {
      width: 3px;
      height: 3px;
      border-radius: 50%;
      background: var(--muted);
      opacity: 0.6;
    }
  }

  .join-btn {
    flex: 0 0 auto;
    height: 36px;
    padding: 0 20px;
    border-radius: 999px;
    font-weight: 600;
    transition:
      transform 0.35s $ease-apple,
      box-shadow 0.35s $ease-apple;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 12px 24px rgba(34, 160, 107, 0.28);
    }
  }
}

// ---------------- 右栏 ----------------
.side-col {
  display: flex;
  flex-direction: column;
  gap: 18px;
  position: sticky;
  top: 96px;
}

.side-title {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.01em;
  margin-bottom: 14px;
}

.create-card {
  position: relative;
  padding: 22px 22px 24px;

  h3 {
    margin: 14px 0 8px;
    font-size: 18px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }

  p {
    margin: 0;
    font-size: 13px;
    line-height: 1.7;
  }

  .create-btn {
    margin-top: 18px;
    width: 100%;
    height: 46px;
    border-radius: 999px;
    font-size: 15px;
    font-weight: 600;
    transition:
      transform 0.35s $ease-apple,
      box-shadow 0.35s $ease-apple;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 16px 30px rgba(59, 111, 224, 0.34);
    }
  }
}

.guide-card {
  padding: 22px 22px 24px;

  .guide-list {
    margin: 0;
    padding: 0;
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: 12px;

    li {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      font-size: 13px;
      line-height: 1.65;
      color: var(--muted);
    }

    .guide-index {
      flex: 0 0 auto;
      width: 22px;
      height: 22px;
      display: grid;
      place-items: center;
      border-radius: 8px;
      font-size: 12px;
      font-weight: 700;
      color: var(--primary);
      background: rgba(59, 111, 224, 0.1);
    }
  }
}

// ---------------- 窄屏兜底 ----------------
@media (max-width: 900px) {
  .pair-hero h2 {
    font-size: 24px;
  }

  .pair-layout {
    grid-template-columns: 1fr;
  }

  .side-col {
    position: static;
  }

  .room-card {
    padding: 14px 16px;
    gap: 12px;
  }
}
</style>