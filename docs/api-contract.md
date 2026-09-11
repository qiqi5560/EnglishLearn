# 前后端接口契约

本文档约定「英语口语训练系统」FastAPI 后端（`backend/`）与 Vue 前端（`frontend/`）之间的全部接口。后端启动后可在 `http://127.0.0.1:8080/docs` 查看 OpenAPI 交互文档。

## 1. 基础约定

- 请求前缀：`http://127.0.0.1:8080/api`
- 响应信封（HTTP 与 body.code 保持一致）：

```json
{ "code": 0, "message": "成功", "data": {} }
```

- 错误码：

| code | 含义 |
| --- | --- |
| 0 | 成功 |
| 401 | 未登录 / 令牌失效 / 验证码或原密码错误 |
| 403 | 无权限（非管理员访问管理端、账号被停用） |
| 404 | 资源不存在 |
| 409 | 状态冲突（如已结束会话继续发言、重复注册密码错误） |
| 422 | 参数校验失败 |
| 500 | 服务异常 |

- 鉴权：除标注「公开」的接口外，均需请求头 `Authorization: Bearer <accessToken>`。
- 前端 Axios 拦截器：`code !== 0` 统一提示 `message`；`401` 清理登录态并跳转登录页。

## 2. 账号与鉴权 `/api/auth`

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | `/auth/send-code` | 发送短信验证码（mock 固定 `123456`，打印到后端日志） | 公开 |
| POST | `/auth/login` | 登录：提供 `code` 走验证码登录（自动注册），提供 `password` 走密码登录 | 公开 |
| POST | `/auth/register` | 密码注册（已存在手机号校验密码后直接返回登录态） | 公开 |
| POST | `/auth/admin/login` | 管理员登录（非 admin 角色返回 403） | 公开 |

- `POST /auth/send-code` body：`{ "phone": "13800138000" }`
- `POST /auth/login` body（二选一）：

```json
{ "phone": "13800138000", "code": "123456" }
{ "phone": "13800138000", "password": "123456" }
```

- `POST /auth/register` body：`{ "phone": "...", "password": "≥6位", "nickname": "...", "ageGroup": "adult" }`
- 登录/注册成功返回 `data`：

```json
{
  "accessToken": "<jwt>",
  "user": { "userId": 1, "phone": "...", "nickname": "...", "avatarUrl": null,
            "ageGroup": "adult", "role": "learner", "guardianId": null,
            "status": 1, "level": "A2", "registerTime": "...", "lastLoginTime": "..." }
}
```

## 3. 个人中心 `/api/users`

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/users/me` | 获取当前用户 | ✓ |
| PUT | `/users/me` | 更新资料（`nickname` / `avatarUrl` / `ageGroup` / `guardianId`） | ✓ |
| PUT | `/users/me/password` | 修改密码：`{ "oldPassword", "newPassword" }` | ✓ |
| POST | `/users/me/bind-guardian` | 绑定监护人：`{ "guardianPhone", "code" }`（code 为监护人手机收到的验证码） | ✓ |

`ageGroup` 取值：`child | k12 | adult | senior`。更新资料成功后 `data` 返回更新后的完整用户对象。

## 4. 场景 `/api/scenes`（公开）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/scenes?category=&level=&keyword=&page=1&pageSize=20` | 场景列表（只返回 status=1） |
| GET | `/scenes/recommended?limit=6` | 首页推荐场景 |
| GET | `/scenes/{scene_id}` | 场景详情 |

返回对象：

```json
{ "id": 1, "name": "机场值机", "category": "出行", "desc": "...", "level": "A2",
  "role": "值机员", "roleSetting": { "role": "...", "script": "..." },
  "coverUrl": null, "status": 1 }
```

## 5. 学习资源 `/api/resources`（公开）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/resources?type=&category=&level=&keyword=&page=&pageSize=` | 资源列表（只返回 status=1） |
| GET | `/resources/{resource_id}` | 资源详情 |

返回对象：`{ "id", "title", "type", "category", "level", "mediaUrl", "durationSec", "status" }`。
`type` 语义（前端分类）：精听 / 跟读 / 口语 / 动画。

## 6. 对话会话 `/api/dialogues`

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | `/dialogues/sessions` | 创建会话：`{ "sceneId": 1, "mode": "scenario" }`；`sceneId` 缺省为自由对话 | ✓ |
| GET | `/dialogues/sessions?status=&page=&pageSize=` | 我的会话列表（含每场小结摘要） | ✓ |
| GET | `/dialogues/sessions/{session_id}` | 会话详情（含全部消息） | ✓ |
| POST | `/dialogues/sessions/{session_id}/messages` | 发消息：`{ "content": "英文" }` | ✓ |
| POST | `/dialogues/sessions/{session_id}/finish` | 结束会话并生成小结（幂等） | ✓ |
| GET | `/dialogues/sessions/{session_id}/summary` | 取小结（未结束时返回 409） | ✓ |

- 创建会话成功 `data`：`{ "sessionId", "sceneId", "sceneName", "mode", "status": "ongoing", "startTime", "messages": [开场白...] }`
- 发消息成功 `data`：

```json
{
  "userMessage": { "id", "speaker": "user", "contentEn": "...", "contentZh": null, "audioUrl": null, "time": "..." },
  "aiMessage":  { "id", "speaker": "ai", "contentEn": "...", "contentZh": "...", "audioUrl": null, "time": "..." },
  "liveScores": { "pron": 85, "fluency": 80, "reaction": 82, "natural": 78 }
}
```

- 结束/小结成功 `data`：

```json
{
  "sessionId": 1, "total": 6, "durationSec": 120,
  "dimensions": { "pron": 85, "fluency": 80, "reaction": 82, "natural": 78 },
  "highlights": ["..."], "improvements": ["..."], "suggestions": ["..."],
  "corrections": [{ "type": "发音", "word": "..." , "correct": "...", "note": "..." }],
  "feedbackText": "..."
}
```

当前 AI 回复与小结由后端 `services/llm/MockProvider` 规则式生成，接口形状已按真实大模型接入预留。

## 7. 学习方案与测评 `/api/plans`

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | `/plans/entrance-test` | 入学测评：`{ "answers": [{ "id", "text" }], "targetGoal" }`，后端判定等级并生成方案 | ✓ |
| GET | `/plans/current` | 当前有效方案（无方案返回 `data: null`） | ✓ |
| GET | `/plans/level` | 当前等级：`{ "level": "A2" | null }` | ✓ |
| POST | `/plans/generate` | 按目标重新生成方案：`{ "targetGoal" }` | ✓ |
| GET | `/plans/today-tasks` | 今日任务：`{ "plan": {...} | null, "tasks": [...] }`（无方案返回空任务） | ✓ |
| PUT | `/plans/tasks/{task_id}` | 勾选任务：`{ "done": true }`（仅限本人任务） | ✓ |

- `targetGoal` 合法取值：`考试 | 商务 | 出国 | 兴趣`
- 任务对象字段：`sceneId`/`resourceId` 为该任务绑定的真实练习对象（场景对话→scene、跟读/精听→learning_resource；单词任务及无匹配素材时为 `null`，由前端提示/兜底）
- 测评结果 `data`：

```json
{
  "level": "B1", "summary": "...", "targetGoal": "兴趣",
  "plan": { "planId", "targetGoal", "levelStart", "levelCurrent", "planContent", "planStart", "planStatus": "active", "updateTime" },
  "tasks": [{ "taskId", "type", "title", "durationMin", "sceneId": 1, "resourceId": null, "done": false, "taskDate" }]
}
```

## 8. 学习报表 `/api/reports`

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/reports/overview` | 成长曲线 + 能力雷达 + 统计 + 最近会话 | ✓ |

返回 `data`：

```json
{
  "growth": { "dates": ["01-01", "..."], "scores": [80, ...], "startDate": "..." },
  "radar": { "indicators": [{ "name": "发音", "max": 100 }, ...], "values": [84, 79, 82, 78] },
  "stats": { "totalMinutes", "totalSessions", "activeDays", "avgScore", "currentLevel" },
  "recentSessions": [{ "sessionId", "sceneName", "mode", "status", "total", "durationSec", "startTime" }]
}
```

## 9. 社区 `/api/community`

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/community/topics` | 话题列表（种子话题 + 已有话题去重） | 公开 |
| GET | `/community/posts?topic=&page=&pageSize=` | 帖子信息流（仅 status=1，按置顶/时间排序；返回 `liked`） | ✓ |
| POST | `/community/posts` | 发帖：`{ "title", "content", "topic" }`（发布即通过） | ✓ |
| GET | `/community/posts/{post_id}` | 帖子详情（他人隐藏非公开帖；本人可看自己的草稿/驳回） | ✓ |
| POST | `/community/posts/{post_id}/like` | 点赞 / 取消点赞（切换），返回 `{ "likes", "liked" }` | ✓ |
| POST | `/community/posts/{post_id}/comments` | 评论：`{ "content" }` | ✓ |
| DELETE | `/community/posts/{post_id}` | 删帖（本人或 admin） | ✓ |

- 帖子详情返回 `data`：`{ "post": {...}, "comments": [...] }`
- 帖子对象：

```json
{ "id": 1, "author": "Momo", "authorId": 2, "title": "...", "content": "...",
  "topic": "学习心得", "likes": 3, "comments": 2, "status": 1,
  "isTop": false, "liked": false, "createTime": "2026-01-01 10:00:00" }
```

## 10. 管理后台 `/api/admin`（全部要求 admin 角色）

### 数据看板

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/dashboard` | 4 项统计 + 近 7 日活跃 + 待办 |

返回 `data`：

```json
{
  "stats": [{ "label": "总用户数", "value": "1,234", "color": "var(--primary)" }, ...],
  "trend": { "dates": ["07-01", "..."], "values": [0, ...] },
  "todos": [{ "type": "资源审核", "desc": "新增素材待审核", "count": 0 }]
}
```

### 资源管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/resources?keyword=&page=&pageSize=` | 分页列表（含未上架） |
| POST | `/admin/resources` | 新增（管理端新增默认直接上架 status=1） |
| PUT | `/admin/resources/{resource_id}` | 编辑字段 / 上下架：`{ "title"?, "type"?, ..., "status": 0|1 }` |
| DELETE | `/admin/resources/{resource_id}` | 删除 |

### 社区审核

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/posts?status=0|1|2&page=&pageSize=` | 分页列表（status 不传为全部） |
| PUT | `/admin/posts/{post_id}` | 审核/置顶：`{ "status"?: 0|1|2, "isTop"?: boolean }` |
| DELETE | `/admin/posts/{post_id}` | 删除 |

帖子 status 语义：`0 待审核 / 1 已通过 / 2 已驳回`。

### 用户管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/users?keyword=&role=&page=&pageSize=` | 分页列表（附带等级） |
| PUT | `/admin/users/{user_id}` | 编辑昵称/角色/启停：`{ "nickname"?, "userRole"?, "status"?: 0|1 }` |

限制：不能停用自己、不能改自己的角色（返回 422）。

### 系统配置

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/configs` | 读取配置 |
| PUT | `/admin/configs` | 部分更新配置，返回合并后完整配置 |

配置结构（与功能 F006/F008 对应）：

```json
{
  "speech":    { "slowSpeed": 0.8, "normalSpeed": 1.2 },
  "recommend": { "content": true, "collab": true, "model": true },
  "audit":     { "content": true, "manual": true }
}
```

## 11. 前端消费方式

- 页面不直接散落 axios：接口集中在 `frontend/src/api/modules/*.ts`，通过 `frontend/src/stores/*` 的 action 暴露给页面。
- 共享 TS 类型位于 `frontend/src/types/api.ts`，字段命名与本文档 `data` 一一对应（camelCase）。
- 列表统一 `{ list: T[], total: number }`；页面通过 `page`/`pageSize` 参数分页。
