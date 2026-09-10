<template>
  <div class="pair-practice">
    <AppHeader title="结伴练习" back />

    <div class="pair-body">
      <el-card shadow="never" class="create-room">
        <p class="text-muted">发起一个练习房间，与真人搭子练口语</p>
        <el-button type="primary" @click="onCreate">创建房间</el-button>
      </el-card>

      <div class="section-title">进行中的房间</div>
      <el-card v-for="room in rooms" :key="room.id" shadow="never" class="room-card">
        <div class="room-info">
          <div class="room-title">{{ room.topic }}</div>
          <div class="text-muted">发起人：{{ room.owner }} · {{ room.level }} · {{ room.people }}/2 人</div>
        </div>
        <el-button size="small" type="success" @click="onJoin(room)">加入</el-button>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'

const rooms = [
  { id: 1, topic: '商务英语 · 模拟会议', owner: 'Leo', level: 'B1', people: 1 },
  { id: 2, topic: '雅思口语 · Part 3', owner: 'Momo', level: 'B2', people: 1 },
  { id: 3, topic: '日常闲聊 · 周末计划', owner: 'Cici', level: 'A2', people: 2 },
]

function onCreate() {
  ElMessage.success('房间创建成功（接入音视频通道后实现）')
}

function onJoin(room: any) {
  ElMessage.info(`加入「${room.topic}」房间`)
}
</script>

<style scoped lang="scss">
.pair-body {
  padding: 16px;
}

.create-room {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: var(--radius-md);

  p {
    margin: 0;
    font-size: 13px;
  }
}

.room-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: var(--radius-md);
  margin-bottom: 12px;

  .room-title {
    font-weight: 700;
    margin-bottom: 4px;
  }
}
</style>
