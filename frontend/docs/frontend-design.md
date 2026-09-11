# 前端设计文档

> 基于大模型场景扮演的英语口语训练系统 —— 前端页面与组件结构设计
> 依据文档：`基于大模型场景扮演的英语口语训练系统_Software Requirement Specification_V1.0.docx`
> 版本：V1.0 ｜ 日期：2026-09-08

---

## 1. 技术选型

| 维度 | 选型 | 说明 |
| --- | --- | --- |
| 框架 | Vue 3（`<script setup>` + Composition API） | 当前主流，长期维护 |
| 组件库 | Element Plus | 官方 Vue 3 版，覆盖表单/表格/反馈等 |
| 构建工具 | Vite 5 | 极速冷启动与 HMR |
| 语言 | TypeScript | 类型安全，真实项目标准 |
| 状态管理 | Pinia | Vue 3 官方推荐 |
| 路由 | Vue Router 4 | 支持嵌套路由、路由守卫 |
| 图表 | ECharts + vue-echarts | F007 成长曲线 / 能力雷达图 |
| 请求 | Axios | 统一封装（后端不在本次范围，仅预留层） |
| 样式 | SCSS + CSS 变量 | Element Plus 主题定制 + 全年龄段主题切换 |

> 说明：实时对话（WSS）、ASR/TTS、数字人渲染均依赖后端/AI 端接口，前端只预留调用层与占位组件，不在本次实现范围。

---

## 2. 视觉设计方向

**设计立意**：口语学习的本质是「开口对话」。视觉上以「语音波形 / 对话气泡」为核心母题，突出「开口说」这一关键动作；整体气质温暖、可信、有活力，同时为全年龄段（少儿游戏化 → 中老年大字体）预留主题切换能力。

### 2.1 色彩令牌（Design Tokens）

| 令牌 | 色值 | 用途 |
| --- | --- | --- |
| `--ink` | `#1F2A44` | 主文字 / 标题（深蓝黑，替代纯黑） |
| `--primary` | `#3B6FE0` | 品牌蓝，导航 / 链接 / 主按钮 |
| `--accent` | `#FF6A4D` | 暖珊瑚，「录音 / 开口说」关键动作、CTA |
| `--success` | `#22A06B` | 得分 / 完成 / 达标 |
| `--warning` | `#E8A23D` | 待改进 / 提醒 |
| `--surface` | `#F6F7FB` | 页面背景（冷调纸张，避免模板化暖奶油） |
| `--card` | `#FFFFFF` | 卡片 / 面板 |
| `--muted` | `#8A93A6` | 次要文字 / 占位 |

### 2.2 字体令牌

| 角色 | 字体 | 用途 |
| --- | --- | --- |
| UI / 界面 | `Plus Jakarta Sans`（回退系统无衬线） | 导航、按钮、标签 |
| 学习内容 / 英文 | `Source Serif 4`（回退衬线） | 精听跟读原文、阅读段落（衬线更易长时间阅读） |
| 中文 | 系统默认（PingFang SC / Microsoft YaHei） | 中文正文 |

> 两族字体明确区分：UI 用几何无衬线，学习素材正文用衬线——契合「英语阅读/精听」的产品属性。

### 2.3 布局与交互原则

- **响应式 H5 优先**：移动端底部 Tab 导航为主；≥768px 自动切换为侧边栏 + 内容区。
- **学习路径短**：核心链路「测评 → 方案 → 对话 → 反馈」≤3 次点击可达。
- **「开口说」是最高层级动作**：录音按钮固定为暖珊瑚色、大触控区，全站唯一强调色动作。
- **全年龄段主题**：通过 CSS 变量 + 一个 `ageMode` 状态切换（普通 / 少儿 / 中老年），中老年模式放大字号、提高对比度。
- **无障碍**：可见键盘焦点、尊重 `prefers-reduced-motion`。

---

## 3. 信息架构与页面清单

### 3.1 学习者端（响应式 H5）

底部 Tab 导航（移动端）5 个入口：

| Tab | 路由 | 页面 | 对应功能 |
| --- | --- | --- | --- |
| 首页 | `/home` | 首页 | F006 推荐、今日任务、学习方案摘要 |
| 练习 | `/practice` | 练习中心 | F002 场景对话 + F004 学习资源入口 |
| 社区 | `/community` | 学习社区 | F008 信息流 |
| 报表 | `/report` | 学习报表 | F007 成长曲线/雷达图 |
| 我的 | `/profile` | 个人中心 | 设置、家长管控、学习记录 |

非 Tab 页面（压栈进入）：

| 页面 | 路由 | 对应功能/用例 |
| --- | --- | --- |
| 登录 / 注册 | `/login` | UC-01（手机号+第三方+监护人绑定） |
| 入学水平测试 | `/entrance-test` | UC-02 / F001 |
| 测试结果 | `/entrance-test/result` | UC-02 |
| 个性化学习方案 | `/plan` | UC-03 / F001 |
| 场景详情 | `/scene/:id` | UC-04 / F002 |
| 对话练习室 | `/dialogue/:sessionId` | UC-04/05/07 / F002+F003+F005 |
| 练习小结/评测报告 | `/dialogue/:sessionId/summary` | UC-05 / F003 |
| 素材详情 | `/resource/:id` | UC-06 / F004 |
| 精听训练 | `/resource/:id/listen` | F004 |
| 跟读训练 | `/resource/:id/read` | F004 |
| 帖子详情 | `/community/post/:id` | UC-10 / F008 |
| 发帖 | `/community/post/new` | UC-10 / F008 |
| 结伴练习 | `/community/pair` | UC-10 / F008 |
| 设置 | `/profile/settings` | F009 模式切换等 |
| 家长管控 | `/profile/guardian` | UC-11 / F009 |

### 3.2 管理员端（桌面 Web）

| 页面 | 路由 | 对应功能/用例 |
| --- | --- | --- |
| 管理员登录 | `/admin/login` | UC-12 |
| 数据看板 | `/admin/dashboard` | 可管理性需求 |
| 学习资源管理 | `/admin/resources` | UC-12 / F004 |
| 社区内容管理 | `/admin/community` | UC-12 / F008 |
| 用户管理 | `/admin/users` | UC-12 |
| 系统配置 | `/admin/config` | 场景/角色/推荐/语速档位 |

---

## 4. 路由设计

```
/                          → redirect /home
/login                     → 登录注册（含第三方、监护人绑定）
(需登录，AppLayout)
  /home                    → 首页
  /practice                → 练习中心（场景 + 资源双 Tab）
  /community               → 学习社区
  /report                  → 学习报表
  /profile                 → 我的
  /entrance-test           → 入学水平测试
  /entrance-test/result    → 测试结果
  /plan                    → 个性化学习方案
  /scene/:id               → 场景详情
  /dialogue/:sessionId     → 对话练习室（核心）
  /dialogue/:sessionId/summary → 小结与评测报告
  /resource/:id            → 素材详情
  /resource/:id/listen     → 精听训练
  /resource/:id/read       → 跟读训练
  /community/post/:id      → 帖子详情
  /community/post/new      → 发帖
  /community/pair          → 结伴练习
  /profile/settings        → 设置
  /profile/guardian        → 家长管控
/admin
  /admin/login             → 管理员登录
  (需 admin 权限，AdminLayout)
    /admin/dashboard       → 数据看板
    /admin/resources       → 学习资源管理
    /admin/community       → 社区内容管理
    /admin/users           → 用户管理
    /admin/config          → 系统配置
```

路由守卫：`beforeEach` 校验 `userStore.token`；`/admin/**` 校验 `userStore.role === 'admin'`；未登录跳 `/login`。

---

## 5. 组件结构

### 5.1 组件分层

```
components/
  base/        # 无业务、全局复用
  business/    # 业务组件（跨页面复用）
  (views 内部) # 页面私有组件，就近存放
```

### 5.2 共享组件清单

| 组件 | 说明 | 用到的 Element Plus |
| --- | --- | --- |
| `AppHeader` | 顶部栏（返回/标题/搜索/设置） | `el-page-header`、`el-input` |
| `AppTabBar` | 移动端底部导航 | 自定义 + `el-badge` |
| `PageContainer` | 页面容器（统一留白/滚动/安全区） | `el-scrollbar` |
| `SceneCard` | 场景卡片（封面/名称/等级/角色） | `el-card`、`el-tag` |
| `ResourceCard` | 资源卡片（标题/类型/难度/时长） | `el-card`、`el-tag` |
| `ScoreRing` | 四维评分环/分数徽标 | `el-progress`（type=circle） |
| `ScoreBar` | 横向分项得分条 | `el-progress` |
| `LevelTag` | 英语等级标签（A1–C2） | `el-tag` |
| `ChatBubble` | 对话气泡（用户/AI/双语字幕） | 自定义 |
| `AudioWave` | 录音波形/播放状态占位 | 自定义（预留 ASR） |
| `DigitalHuman` | AI 数字人占位容器 | 自定义（预留 SDK 挂载） |
| `EmptyState` | 空状态（引导动作） | `el-empty` |
| `RadarChart` | 能力雷达图 | ECharts（`vue-echarts`） |
| `GrowthLine` | 成长曲线 | ECharts |
| `PostCard` | 社区帖子卡片 | `el-card`、`el-avatar` |

### 5.3 页面 → 组件映射（关键页面）

**对话练习室 `/dialogue/:sessionId`（核心页面）**
```
DialogueRoom
├─ AppHeader（场景名、结束按钮）
├─ ChatPanel（消息流）
│   ├─ ChatBubble（AI：content_en + content_zh + 播放）
│   └─ ChatBubble（用户：语音转写文本 + 评分角标）
├─ ScorePanel（实时四维：ScoreRing × 4）
├─ DigitalHuman（数字人形象，F005 增强）
└─ InputBar（录音按钮 AudioWave + 文本输入 + 打断）
```

**学习报表 `/report`**
```
Report
├─ AppHeader
├─ SummaryCards（学习时长/次数/得分 指标卡）
├─ GrowthLine（成长曲线）
├─ RadarChart（发音/流利度/语法/词汇 雷达）
└─ ExportButton（报表导出）
```

---

## 6. Element Plus 组件映射

| 页面/场景 | 使用的 Element Plus 组件 |
| --- | --- |
| 登录注册 | `el-form` `el-input` `el-button` `el-checkbox` `el-tabs`（手机号/第三方） `el-link` |
| 入学测试 | `el-steps`（引导进度） `el-dialog` `el-result` `el-progress` |
| 学习方案 | `el-timeline`（学习路径） `el-card` `el-tag` `el-checkbox`（任务勾选） |
| 场景/资源列表 | `el-tabs` `el-input`（搜索） `el-select`（分类/难度筛选） `el-card` `el-empty` `el-skeleton` |
| 对话练习室 | `el-dialog`（结束确认） `el-message` `el-progress` `el-tooltip` |
| 评测报告 | `el-progress` `el-descriptions` `el-collapse` `el-tag`（问题标红/建议） |
| 精听/跟读 | `el-slider`（进度/倍速） `el-switch`（字幕/挖空） `el-radio-group`（倍速档） |
| 学习报表 | `el-card`（指标卡） `el-date-picker`（时间维度） `el-dropdown`（导出） + ECharts |
| 社区 | `el-input`（发帖） `el-upload`（图片） `el-avatar` `el-card` `el-tabs`（信息流/结伴） `el-rate` |
| 个人中心/设置 | `el-avatar` `el-menu`/`el-cell` 风格列表 `el-switch` `el-slider` `el-radio-group`（模式切换） |
| 管理员后台 | `el-table` `el-pagination` `el-form` `el-upload` `el-tag`（状态） `el-drawer` `el-dialog` |

---

## 7. 状态管理（Pinia）

| Store | 状态 | 说明 |
| --- | --- | --- |
| `useUserStore` | `token`、`userInfo`、`role`、`ageGroup`、`guardianId` | 登录态与用户画像 |
| `useAppStore` | `ageMode`（normal/child/senior）、`theme`、`loading` | 全年龄段模式与全局 UI 状态 |
| `usePlanStore` | `currentPlan`、`dailyTasks`、`level` | F001 学习方案与等级 |
| `useSessionStore` | `currentSession`、`messages`、`liveScores` | F002 对话会话与实时评测 |
| `useResourceStore` | `resourceList`、`filter` | F004 素材库缓存 |

> `ageMode` 切换驱动根元素 class（`.mode-child` / `.mode-senior`），通过 CSS 变量覆盖字号、对比度与配色，实现 F009 全年龄段适配。

---

## 8. 目录结构

```
frontend/
├── index.html
├── package.json
├── vite.config.ts
├── tsconfig.json
├── README.md
├── docs/
│   └── frontend-design.md          # 本文档
└── src/
    ├── main.ts                     # 入口（挂载 ElementPlus/Pinia/Router）
    ├── App.vue                     # 根组件
    ├── env.d.ts
    ├── api/                        # 接口层（预留，后端不在范围）
    │   ├── request.ts              # Axios 封装
    │   └── modules/                # 各模块 API（占位）
    ├── router/
    │   └── index.ts                # 路由 + 守卫
    ├── stores/
    │   ├── index.ts
    │   ├── user.ts
    │   ├── app.ts
    │   ├── plan.ts
    │   ├── session.ts
    │   └── resource.ts
    ├── styles/
    │   ├── index.scss              # 设计令牌 + 全局样式
    │   └── element.scss            # Element Plus 主题覆盖
    ├── layouts/
    │   ├── AppLayout.vue           # 学习者端布局（底部 Tab）
    │   └── AdminLayout.vue         # 管理员端布局（侧边栏）
    ├── components/
    │   ├── base/                   # 基础组件
    │   │   ├── AppHeader.vue
    │   │   ├── AppTabBar.vue
    │   │   ├── PageContainer.vue
    │   │   ├── EmptyState.vue
    │   │   └── ScoreRing.vue
    │   └── business/               # 业务组件
    │       ├── SceneCard.vue
    │       ├── ResourceCard.vue
    │       ├── ChatBubble.vue
    │       ├── AudioWave.vue
    │       ├── DigitalHuman.vue
    │       ├── LevelTag.vue
    │       └── chart/
    │           ├── RadarChart.vue
    │           └── GrowthLine.vue
    └── views/
        ├── auth/Login.vue
        ├── home/Home.vue
        ├── practice/SceneBrowse.vue
        ├── practice/ResourceList.vue
        ├── entrance/EntranceTest.vue
        ├── entrance/EntranceResult.vue
        ├── plan/PlanDetail.vue
        ├── dialogue/SceneDetail.vue
        ├── dialogue/DialogueRoom.vue
        ├── dialogue/DialogueSummary.vue
        ├── resource/ResourceDetail.vue
        ├── resource/ListenPractice.vue
        ├── resource/ReadAlong.vue
        ├── community/Community.vue
        ├── community/PostDetail.vue
        ├── community/PostCreate.vue
        ├── community/PairPractice.vue
        ├── report/Report.vue
        ├── profile/Profile.vue
        ├── profile/Settings.vue
        ├── profile/Guardian.vue
        └── admin/
            ├── AdminLogin.vue
            ├── AdminDashboard.vue
            ├── ResourceManage.vue
            ├── CommunityManage.vue
            ├── UserManage.vue
            └── ConfigManage.vue
```

---

## 9. 分阶段实施建议（对齐 P0/P1/P2）

- **阶段一（P0，MVP）**：登录注册、入学测试、学习方案、场景对话练习室、实时评测小结 —— 打通「测评→方案→对话→反馈」闭环。
- **阶段二（P1）**：学习资源（精听/跟读）、AI 数字人、智能推荐位。
- **阶段三（P2）**：学习报表（ECharts）、学习社区、全年龄段模式、管理员后台。
