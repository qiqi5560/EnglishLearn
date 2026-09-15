// 与 FastAPI 后端统一响应信封对齐的共享类型
// 规则：code = 0 成功；401 未登录；403 无权限；404 不存在；422 参数错误；500 服务异常

export interface ApiEnvelope<T = unknown> {
  code: number
  message: string
  data: T
}

export interface Page<T> {
  list: T[]
  total: number
}

/** users 表（SRS 表 1） */
export interface UserDto {
  userId: number
  phone: string
  nickname: string
  avatarUrl?: string | null
  ageGroup: 'child' | 'k12' | 'adult' | 'senior'
  role: 'learner' | 'admin' | 'guardian'
  guardianId?: number | null
  status: number
  level?: string | null
  registerTime?: string | null
  lastLoginTime?: string | null
}

export interface LoginResult {
  accessToken: string
  user: UserDto
}

/** 学习搭子 */
export interface PartnerDto {
  userId: number
  nickname: string
  phone: string
  avatarUrl?: string | null
  level?: string | null
  addedTime?: string | null
}

/** scene 表（SRS 表 2） */
export interface SceneDto {
  id: number
  name: string
  category: string
  desc: string
  level: string
  role?: string
  roleSetting?: { role?: string; script?: string } | null
  coverUrl?: string | null
  status: number
}

/** learning_resource（SRS 表 7） */
export interface ResourceDto {
  id: number
  title: string
  type: string
  category: string
  level: string
  mediaUrl?: string | null
  durationSec?: number | null
  status: number
}

/** conversation_message（SRS 表 4） */
export interface ChatMessageDto {
  id: number
  speaker: 'user' | 'ai'
  contentEn: string
  contentZh?: string | null
  audioUrl?: string | null
  time: string
}

/** conversation_session（SRS 表 3） */
export interface SessionDto {
  sessionId: number
  sceneId?: number | null
  sceneName: string
  mode: string
  status: string
  startTime?: string | null
  endTime?: string | null
  durationSec?: number | null
  messages: ChatMessageDto[]
}

export interface SessionListItem {
  sessionId: number
  sceneName: string
  mode: string
  status: string
  total?: number | null
  durationSec?: number | null
  startTime?: string | null
  endTime?: string | null
}

/** 对话消息返回（用户消息 + AI 回复 + 实时四维） */
export interface MessageReplyResult {
  userMessage: ChatMessageDto
  aiMessage: ChatMessageDto
  /** 口语评分改为后台异步产出，发送时可能为 null，需用 polling 拉取 */
  liveScores: { pron: number; fluency: number; reaction: number; natural: number } | null
}

/** 某条消息的口语评分（异步） */
export interface MessageAssessmentDto {
  ready: boolean
  pron?: number
  fluency?: number
  reaction?: number
  natural?: number
  grammarFeedback?: string | null
  phonemeIssues?: Array<{ word: string; phoneme: string; note: string }> | null
  betterExpression?: string | null
}

/** assessment_record（SRS 表 5）小结 */
export interface CorrectionItem {
  type: string
  word: string
  correct: string
  note: string
}

export interface DialogueSummaryDto {
  sessionId: number
  total: number
  dimensions: { pron: number; fluency: number; reaction: number; natural: number }
  highlights: string[]
  improvements: string[]
  suggestions: string[]
  corrections: CorrectionItem[]
  feedbackText: string
  durationSec: number
}

/** learning_plan（SRS 表 6） */
export interface PlanDto {
  planId: number
  targetGoal: string
  levelStart: string
  levelCurrent: string
  planContent?: string
  planStart?: string | null
  planStatus: string
  updateTime?: string | null
}

export interface PlanStage {
  stage: number
  focus: string
  tasks: number
}

export interface ParsedPlanContent {
  goal?: string
  stages?: PlanStage[]
}

/** daily_task */
export interface DailyTaskDto {
  taskId: number
  type: string
  title: string
  durationMin: number
  /** 绑定的练习场景（场景对话任务；单词/未匹配为 null） */
  sceneId?: number | null
  /** 绑定的练习素材（跟读/精听任务；单词/未匹配为 null） */
  resourceId?: number | null
  done: boolean
  taskDate?: string
}

export interface TodayTasksResult {
  plan: PlanDto | null
  tasks: DailyTaskDto[]
}

export interface EntranceTestResult {
  level: string
  summary: string
  targetGoal: string
  plan: PlanDto
  tasks: DailyTaskDto[]
  speakingEval?: SpeakingEvalDto | null
}

/** 大模型对作答的四维口语评价 */
export interface SpeakingEvalDto {
  pron: number
  fluency: number
  reaction: number
  natural: number
  grammarFeedback?: string | null
  phonemeIssues?: { word: string; phoneme: string; note: string }[]
  betterExpression?: string | null
}

/** community_post */
export interface PostDto {
  id: number
  author: string
  authorId: number
  title: string
  content: string
  topic: string
  likes: number
  comments: number
  status: number
  isTop: boolean
  liked: boolean
  createTime: string
}

export interface CommentDto {
  id: number
  postId: number
  author: string
  content: string
  createTime: string
}

export interface PostDetailResult {
  post: PostDto
  comments: CommentDto[]
}

/** reports */
export interface RadarIndicator {
  name: string
  max: number
}

export interface ReportOverview {
  growth: { dates: string[]; scores: number[]; startDate: string }
  radar: { indicators: RadarIndicator[]; values: number[] }
  stats: {
    totalMinutes: number
    totalSessions: number
    activeDays: number
    avgScore: number | null
    currentLevel: string | null
  }
  recentSessions: SessionListItem[]
}

/** admin */
export interface DashboardStat {
  label: string
  value: string
  color: string
}

export interface AdminDashboard {
  stats: DashboardStat[]
  trend: { dates: string[]; values: number[] }
  todos: { type: string; desc: string; count: number }[]
}

export interface SystemConfig {
  speech: { slowSpeed: number; normalSpeed: number }
  recommend: { content: boolean; collab: boolean; model: boolean }
  audit: { content: boolean; manual: boolean }
}
