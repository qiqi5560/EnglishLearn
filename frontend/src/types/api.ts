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
  /** 发帖处罚 */
  banUntil?: string | null
  banReason?: string | null
  punished?: boolean
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
  liveScores: { pron: number; fluency: number; reaction: number; natural: number }
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

/** ==================== 管理后台：运营洞察 ==================== */

/** 用户使用报表 */
export interface AdminUsageSummary {
  days: number
  totalUsers: number
  activeUsers: number
  newUsers: number
  totalSessions: number
  totalDurationMin: number
  avgScore: number
  activeRate: number
  punishedUsers: number
}

export interface AdminUsageRow {
  userId: number
  nickname?: string | null
  phone: string
  role: string
  level?: string | null
  status: number
  sessionCount: number
  durationMin: number
  studyCount: number
  avgScore?: number | null
  lastLoginTime?: string | null
  registerTime?: string | null
  active: boolean
  punished?: boolean
  banUntil?: string | null
  banReason?: string | null
}

export interface AdminUsageReport {
  summary: AdminUsageSummary
  /** 近 12 个月注册分布 */
  monthly: { months: string[]; values: number[]; peak: number }
  list: AdminUsageRow[]
}

/** 日活动量 */
export interface AdminActivity {
  summary: {
    days: number
    avgActive: number
    peakActive: number
    todayActive: number
    todaySessions: number
  }
  trend: {
    dates: string[]
    activeUsers: number[]
    sessions: number[]
    studyRecords: number[]
    newUsers: number[]
    durationMin: number[]
  }
  list: {
    date: string
    activeUsers: number
    newUsers: number
    sessions: number
    studyRecords: number
    durationMin: number
  }[]
}

/** 算力监控 */
export interface AdminCompute {
  provider: {
    provider: string
    model: string
    baseUrl: string
    reachable: boolean
    degraded: boolean
  }
  totalCalls: number
  failedCalls: number
  successCalls: number
  successRate: number
  avgLatencyMs: number
  maxLatencyMs: number
  windowCalls: number
  methods: { method: string; count: number; failed: number; avgLatencyMs: number }[]
  timeline: { labels: string[]; values: number[] }
  recent: { time: string; method: string; provider: string; latencyMs: number; ok: boolean; note: string }[]
  checkedAt: string
}

/** 系统概览 */
export interface AdminSystemOverview {
  runtime: {
    uptimeSec: number
    heapUsedMb: number
    heapMaxMb: number
    heapUsedPercent: number
    threads: number
    processors: number
    javaVersion: string
    os: string
    startedAt: string
  }
  database: {
    file: string
    sizeMb: number
    tables: { name: string; count: number }[]
  }
  audit: {
    total: number
    recent: {
      time: string
      operator: string
      operatorId?: number | null
      module: string
      action: string
      detail: string
    }[]
  }
  status: string
}

/** 操作日志条目 */
export interface AuditLogEntry {
  time: string
  operator: string
  operatorId?: number | null
  module: string
  action: string
  detail: string
}

/** 操作日志模块统计 */
export interface AuditLogModuleStat {
  name: string
  count: number
}

/** 管理员操作日志 */
export interface AdminAuditLog {
  total: number
  modules: AuditLogModuleStat[]
  list: AuditLogEntry[]
}

/** 管理员维护的名句素材 */
export interface AdminQuoteDto {
  quoteId: number
  title: string
  source?: string | null
  category?: string | null
  level?: string | null
  textEn: string
  textZh?: string | null
  builtin: boolean
  ownerId?: number | null
  wordCount: number
}

/** ==================== 名句跟读 ==================== */

/** 名句素材（内置 15 条 + 用户自建） */
export interface QuoteDto {
  quoteId: number
  title: string
  source: string
  category: string
  level: string
  textEn: string
  textZh?: string | null
  builtin: boolean
  wordCount: number
  createTime: string
}

/** 导入素材的段落：en 原文，zh 译文（未翻译为 null） */
export interface ParagraphDto {
  en: string
  zh?: string | null
}

/** 导入素材（列表项，不含段落） */
export interface DocBriefDto {
  docId: number
  title: string
  sourceType: string
  paragraphCount: number
  createTime: string
}

/** 导入素材（详情，含段落） */
export interface DocDto {
  docId: number
  title: string
  sourceType: string
  paragraphCount: number
  paragraphs: ParagraphDto[]
  createTime: string
}

/** AI 跟读评测结果 */
export interface ReadEvalDto {
  total: number
  pron: number
  fluency: number
  natural: number
  completion: number
  accuracy: number
  missingWords: string[]
  feedback: string
  tips: string[]
}
