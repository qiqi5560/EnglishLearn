// ============================================================
// Mock 数据（后端不在本次范围，用静态数据驱动占位页面）
// ============================================================

export interface Scene {
  id: number
  name: string
  desc: string
  category: string
  level: string
  role: string
}

export interface Resource {
  id: number
  title: string
  type: string
  category: string
  level: string
  durationSec: number
}

export interface Post {
  id: number
  author: string
  avatarColor: string
  title: string
  content: string
  likes: number
  comments: number
  topic: string
}

export const scenes: Scene[] = [
  { id: 1, name: '餐厅点餐', desc: '在餐厅点餐、询问推荐与结账', category: '生活', level: 'A2', role: '服务员' },
  { id: 2, name: '商务会议', desc: '参与英文商务会议并发表观点', category: '工作', level: 'B2', role: '同事' },
  { id: 3, name: '机场值机', desc: '办理登机手续、托运行李', category: '出行', level: 'A2', role: '地勤' },
  { id: 4, name: '课堂讨论', desc: '围绕主题进行课堂讨论与提问', category: '学习', level: 'B1', role: '同学' },
  { id: 5, name: '酒店入住', desc: '办理入住、咨询设施与服务', category: '出行', level: 'A2', role: '前台' },
  { id: 6, name: '面试问答', desc: '模拟英文面试自我介绍与问答', category: '工作', level: 'B2', role: '面试官' },
]

export const resources: Resource[] = [
  { id: 1, title: 'BBC 六分钟英语 · 职场礼仪', type: '新闻', category: '职场', level: 'B1', durationSec: 360 },
  { id: 2, title: 'Friends 老友记 · 第一季片段', type: '剧集', category: '生活', level: 'A2', durationSec: 240 },
  { id: 3, title: 'TED · 高效学习的秘密', type: '播客', category: '学习', level: 'B2', durationSec: 600 },
  { id: 4, title: '小王子 · 有声书第一章', type: '有声书', category: '生活', level: 'A2', durationSec: 480 },
  { id: 5, title: '雅思口语 · Part 2 话题训练', type: '新闻', category: '雅思', level: 'B2', durationSec: 300 },
  { id: 6, title: '机场广播 · 值机通知', type: '新闻', category: '出行', level: 'A2', durationSec: 180 },
]

export const posts: Post[] = [
  {
    id: 1,
    author: 'Momo',
    avatarColor: '#3b6fe0',
    title: '坚持跟读 30 天，我的发音变化',
    content: '每天跟读老友记片段 15 分钟，第 30 天发音评分从 62 涨到了 81，分享一下方法……',
    likes: 128,
    comments: 32,
    topic: '学习心得',
  },
  {
    id: 2,
    author: 'Leo',
    avatarColor: '#22a06b',
    title: '寻找口语搭子，每晚 8 点',
    content: 'B1 水平，想练商务英语，有没有一起结伴练习的小伙伴？',
    likes: 45,
    comments: 18,
    topic: '结伴练习',
  },
  {
    id: 3,
    author: 'Cici',
    avatarColor: '#ff6a4d',
    title: 'AI 场景对话真的太适合社恐了',
    content: '在餐厅点餐场景里练了 20 分钟，不用怕尴尬，出错也会温柔纠正……',
    likes: 96,
    comments: 24,
    topic: '学习心得',
  },
]

export const dailyTasks = [
  { id: 1, type: '场景对话', title: '餐厅点餐 · 10 分钟', done: true, durationMin: 10 },
  { id: 2, type: '跟读', title: '老友记片段跟读', done: false, durationMin: 15 },
  { id: 3, type: '精听', title: 'BBC 六分钟英语精听', done: false, durationMin: 12 },
  { id: 4, type: '单词', title: '职场词汇复习 20 个', done: false, durationMin: 8 },
]

export const reportData = {
  growthDates: ['08-02', '08-09', '08-16', '08-23', '08-30', '09-06'],
  growthScores: [58, 63, 61, 70, 74, 81],
  radarIndicators: [
    { name: '发音', max: 100 },
    { name: '流利度', max: 100 },
    { name: '语法', max: 100 },
    { name: '词汇', max: 100 },
  ],
  radarValues: [76, 68, 72, 64],
}
