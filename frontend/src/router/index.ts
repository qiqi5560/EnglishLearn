import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/welcome' },
  {
    path: '/welcome',
    name: 'Landing',
    component: () => import('@/views/landing/Landing.vue'),
    meta: { public: true, title: '首页' },
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { public: true, title: '登录 / 注册' },
  },
  {
    path: '/',
    component: () => import('@/layouts/AppLayout.vue'),
    children: [
      { path: 'home', name: 'Home', component: () => import('@/views/home/Home.vue'), meta: { title: '首页' } },
      { path: 'practice', name: 'Practice', component: () => import('@/views/practice/SceneBrowse.vue'), meta: { title: '练习' } },
      { path: 'quotes', name: 'Quotes', component: () => import('@/views/quote/QuoteRead.vue'), meta: { title: '名句跟读' } },
      { path: 'community', name: 'Community', component: () => import('@/views/community/Community.vue'), meta: { title: '社区' } },
      { path: 'report', name: 'Report', component: () => import('@/views/report/Report.vue'), meta: { title: '报表' } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/profile/Profile.vue'), meta: { title: '我的' } },
      { path: 'partners', name: 'Partners', component: () => import('@/views/profile/Partners.vue'), meta: { title: '搭子组队' } },
      { path: 'messages', name: 'Messages', component: () => import('@/views/social/Messages.vue'), meta: { title: '我的消息' } },
      { path: 'notifications', name: 'Notifications', component: () => import('@/views/social/Notifications.vue'), meta: { title: '互动通知' } },
      { path: 'user/:id', name: 'UserProfile', component: () => import('@/views/social/UserProfile.vue'), meta: { title: '用户主页' } },
      { path: 'entrance-test', name: 'EntranceTest', component: () => import('@/views/entrance/EntranceTest.vue'), meta: { title: '入学测试' } },
      { path: 'entrance-test/result', name: 'EntranceResult', component: () => import('@/views/entrance/EntranceResult.vue'), meta: { title: '测试结果' } },
      { path: 'plan', name: 'Plan', component: () => import('@/views/plan/PlanDetail.vue'), meta: { title: '学习方案' } },
      { path: 'scene/:id', name: 'SceneDetail', component: () => import('@/views/dialogue/SceneDetail.vue'), meta: { title: '场景详情' } },
      { path: 'dialogue/:sessionId', name: 'DialogueRoom', component: () => import('@/views/dialogue/DialogueRoom.vue'), meta: { title: '对话练习' } },
      { path: 'dialogue/:sessionId/summary', name: 'DialogueSummary', component: () => import('@/views/dialogue/DialogueSummary.vue'), meta: { title: '练习小结' } },
      { path: 'resource/:id', name: 'ResourceDetail', component: () => import('@/views/resource/ResourceDetail.vue'), meta: { title: '素材详情' } },
      { path: 'resource/:id/listen', name: 'ListenPractice', component: () => import('@/views/resource/ListenPractice.vue'), meta: { title: '精听训练' } },
      { path: 'resource/:id/read', name: 'ReadAlong', component: () => import('@/views/resource/ReadAlong.vue'), meta: { title: '跟读训练' } },
      { path: 'community/post/new', name: 'PostCreate', component: () => import('@/views/community/PostCreate.vue'), meta: { title: '发帖' } },
      { path: 'community/post/:id', name: 'PostDetail', component: () => import('@/views/community/PostDetail.vue'), meta: { title: '帖子详情' } },
      { path: 'community/pair', name: 'PairPractice', component: () => import('@/views/community/PairPractice.vue'), meta: { title: '结伴练习' } },
      { path: 'profile/settings', name: 'Settings', component: () => import('@/views/profile/Settings.vue'), meta: { title: '设置' } },
      { path: 'profile/guardian', name: 'Guardian', component: () => import('@/views/profile/Guardian.vue'), meta: { title: '家长管控' } },
    ],
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/admin/AdminLogin.vue'),
    meta: { public: true, title: '管理员登录' },
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAdmin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/AdminDashboard.vue'), meta: { title: '数据看板' } },
      { path: 'usage', name: 'AdminUsageReport', component: () => import('@/views/admin/UsageReport.vue'), meta: { title: '用户使用报表' } },
      { path: 'activity', name: 'AdminActivity', component: () => import('@/views/admin/ActivityStats.vue'), meta: { title: '日活动量' } },
      { path: 'metrics', name: 'AdminMetrics', component: () => import('@/views/admin/MetricsBoard.vue'), meta: { title: '推荐效果指标' } },
      { path: 'compute', name: 'AdminCompute', component: () => import('@/views/admin/ComputeMonitor.vue'), meta: { title: '算力监控' } },
      { path: 'system', name: 'AdminSystem', component: () => import('@/views/admin/SystemOverview.vue'), meta: { title: '系统概览' } },
      { path: 'quotes', name: 'AdminQuotes', component: () => import('@/views/admin/QuoteManage.vue'), meta: { title: '名句素材' } },
      { path: 'scenes', name: 'AdminScenes', component: () => import('@/views/admin/SceneManage.vue'), meta: { title: '场景管理' } },
      { path: 'resources', name: 'ResourceManage', component: () => import('@/views/admin/ResourceManage.vue'), meta: { title: '资源管理' } },
      { path: 'community', name: 'CommunityManage', component: () => import('@/views/admin/CommunityManage.vue'), meta: { title: '社区管理' } },
      { path: 'users', name: 'UserManage', component: () => import('@/views/admin/UserManage.vue'), meta: { title: '用户管理' } },
      { path: 'config', name: 'ConfigManage', component: () => import('@/views/admin/ConfigManage.vue'), meta: { title: '系统配置' } },
      { path: 'audit', name: 'AdminAuditLog', component: () => import('@/views/admin/AuditLog.vue'), meta: { title: '操作日志' } },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/home' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  document.title = to.meta.title ? `${to.meta.title as string} · 英语口语训练系统` : '英语口语训练系统'

  if (to.meta.public) return true
  if (!userStore.token) return { path: '/login', query: { redirect: to.fullPath } }
  if (to.meta.requiresAdmin && userStore.role !== 'admin') return { path: '/home' }
  return true
})

export default router
