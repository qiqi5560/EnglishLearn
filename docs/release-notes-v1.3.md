# 版本更新说明 v1.3.0 — 社交互动（关注 / 私信 / 通知 / 分享）

| 项 | 内容 |
| --- | --- |
| 版本号 | v1.3.0 |
| 发布日期 | 2026-09-21 |
| 涉及模块 | 后端 `backend-spring`、前端 `frontend` |
| 依赖变更 | **无**（未新增任何 Maven / npm 依赖） |
| 数据库变更 | 新增 4 张表 `user_follow` / `user_message` / `user_notification` / `share_record`；`user` 表新增 `bio` 字段（`ddl-auto: update` 自动建表加列） |
| 兼容性 | 向下兼容；已存在的库不会重播社交种子，需按「升级步骤」删库重建 |

---

## 1. 更新概述

本次更新为平台补齐「人与人之间的连接」，共四块能力：

1. **关注关系**：单向关注模型，他人公开主页、粉丝/关注列表、关注与取关。
2. **站内私信**：会话列表 + 聊天历史 + 发送私信 + 标记已读 + 未读数。
3. **互动通知**：社区点赞 / 评论 / 回复三类通知自动生成，已读标记与未读红点。
4. **内容分享**：复制链接留痕，按渠道统计（不接入第三方 SDK）。

配套前端新增 3 个社交页面、1 个分享组件、1 个全局未读红点（30 秒轮询），并在社区与个人主页打通入口。

---

## 2. 数据模型

| 表 / 字段 | 说明 |
| --- | --- |
| `user_follow` | `follower_id`、`followee_id`（单向关注，唯一性由 `existsByFollowerIdAndFolloweeId` + `deleteByFollowerIdAndFolloweeId` 保证） |
| `user_message` | `sender_id`、`receiver_id`、`content`、`read_flag`、`create_time` |
| `user_notification` | `user_id`（接收者）、`actor_id`（触发者）、`type`、`target_type`、`target_id`、`content`、`read_flag` |
| `share_record` | `user_id`、`content_type`、`content_id`、`channel`、`share_url` |
| `user.bio` | 个性签名，`varchar(100)`，他人主页展示，`PUT /api/users/me` 可维护（超长自动截断到 100 字） |

通知类型常量（`NotificationService`）：`like` / `comment` / `reply`。
分享渠道白名单（`ShareService.CHANNELS`）：`weibo` / `xiaohongshu` / `wechat` / `copy`，非法值兜底为 `copy`。

---

## 3. 接口清单（全部需登录）

### 3.1 关注与他人主页（`UsersController`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/users/{userId}/profile` | 公开主页：`user`（不含手机号等敏感字段）+ `stats` + `achievements` + `followers/following` 计数 + `isSelf` + `followingByMe` + `followedMe` + `posts`（最近 10 篇公开帖） |
| POST | `/api/users/{userId}/follow` | 关注，幂等；返回 `followingByMe`、`already`、`followers` |
| DELETE | `/api/users/{userId}/follow` | 取关；返回 `followingByMe`、`removed`、`followers` |
| GET | `/api/users/{userId}/followers` | 粉丝列表（最多 100 条） |
| GET | `/api/users/{userId}/following` | 关注列表（最多 100 条） |

规则：不能关注自己（422）；列表项附带「我是否关注了 TA」状态，便于直接关注/取关。

### 3.2 站内信（`MessageController`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/messages/conversations` | 会话列表（按最近消息聚合，含对方资料、最后一条消息、未读数） |
| GET | `/api/messages/history?peerId=&limit=` | 聊天历史，`limit` 默认 50、上限 100，打开会话时自动标记对方发来的消息为已读 |
| POST | `/api/messages/send` | 发送私信 `{ peerId, content }`，最长 500 字（422 超限） |
| POST | `/api/messages/read` | 将某会话中对方发来的消息标记已读，返回 `updated` |
| GET | `/api/messages/unread` | 红点数据源：`{ message, notification, total }` |

### 3.3 互动通知（`NotificationController`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/notifications/list?type=` | 通知列表，`type` 可过滤 `like` / `comment` / `reply`，空则全部 |
| POST | `/api/notifications/read` | 标记已读，`ids` 为空或缺失表示全部已读 |
| GET | `/api/notifications/unread` | 仅通知未读数 |

### 3.4 分享（`ShareController`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/share` | 记录一次分享 `{ contentType, contentId, channel, url }`，返回 `channel` 等结果 |
| GET | `/api/share/mine` | 我的分享记录 |

说明：分享为「复制链接 + 留痕统计」，不接入微博/微信等第三方 SDK，前端用剪贴板 API（失败降级 `execCommand('copy')`）完成复制。

---

## 4. 通知产生时机（社区改造）

`CommunityService` 新增插桩：

| 动作 | 通知 |
| --- | --- |
| 点赞帖子 | 楼主收到 `like` 通知：「X 赞了你的帖子《…》」 |
| 评论帖子 | 楼主收到 `comment` 通知：「X 评论了你的帖子《…》：片段」 |
| 评论同帖 | 同帖下最近一位**其他**评论者收到 `reply` 通知 |
| 取消点赞 | 对应 `like` 通知由 `revokeInteraction` 撤销 |

自己的行为不会给自己发通知；通知正文最长 200 字，帖子标题最长 20 字、评论片段 40 字（超出加省略号）。

---

## 5. 前端变更

| 文件 | 变更 |
| --- | --- |
| `api/modules/social.ts` | 新增：会话/历史/发送/已读/未读、通知列表/已读、他人主页/关注/取关/粉丝/关注列表、分享 |
| `stores/social.ts` | 新增：`unread` 状态、`refreshUnread`、`startPolling/stopPolling`（`POLL_INTERVAL = 30_000`）、`markMessageRead`、`markNotificationRead` 本地红点消减 |
| `components/business/SharePanel.vue` | 新增：气泡分享面板（微博 / 小红书 / 微信 / 复制链接），复制成功后上报 `POST /api/share`，带「已复制」勾选态 |
| `views/social/Messages.vue` | 新增：私信页（会话列表 + 聊天窗 + 发送） |
| `views/social/Notifications.vue` | 新增：通知页（全部 / 点赞 / 评论 / 回复 分页签 + 一键已读） |
| `views/social/UserProfile.vue` | 新增：他人主页（头像、签名、学习统计、成就、粉丝/关注、动态、关注按钮，可跳转私信） |
| `router/index.ts` | 新增 `/messages`、`/notifications`、`/user/:id` 三条路由 |
| `layouts/AppLayout.vue` | 侧栏新增「消息」入口 + `el-badge` 未读红点（合计私信与通知），挂载时启动轮询、卸载时停止 |
| `views/community/Community.vue` | 帖子作者昵称可点击跳转 `/user/{id}`（悬停变主色 + 下划线） |
| `views/community/PostDetail.vue` | 作者昵称/头像可点击、「查看主页」按钮、帖子下方接入 `SharePanel` |
| `views/profile/Profile.vue` | 编辑资料新增「个性签名」输入（100 字计数），保存到 `bio` |
| `views/report/Report.vue` | 报表页头部接入 `SharePanel`（`contentType="achievement"`） |
| `types/api.ts` | 新增社交相关类型（约 146 行） |
| `api/modules/auth.ts` | `UpdateProfilePayload` 新增 `bio` |

---

## 6. 种子数据（社交演示）

`DataSeeder.seedSocial()`（守卫：`chatRepository.count() > 0` 时跳过）：

- 为 Momo / Leo / Cici 写入个性签名。
- 关注关系 4 条：Momo ↔ Leo 互关、Cici → Momo、Leo → Cici。
- 私信 6 条：Leo 与 Cici 发给 Momo 的 3 条保持 `readFlag=0`（红点有内容可看），Momo 的回复为已读。
- 通知 3 条：以 Momo 的首篇帖子为目标，Leo 点赞（未读）、Cici 评论（未读）、Leo 回复（已读）。
- 分享记录 2 条：Momo 分享帖子（`weibo`）、Leo 分享成就（`wechat`）。

---

## 7. 升级步骤

> `DataSeeder.seed()` 有 `if (sceneRepository.count() > 0) return;` 判重守卫，已存在的库不会重播种子。**必须删库才能让社交种子生效。**

```powershell
# 1. 停止后端进程（8080）
Stop-Process -Name java -Force -ErrorAction SilentlyContinue

# 2. 删除旧库（表结构由 ddl-auto: update 自动重建，含新增 4 张社交表与 user.bio）
Remove-Item d:\english_platform\backend-spring\data\app.db

# 3. 启动后端（不要用根目录 启动后端.bat，其硬编码原作者离线 Maven 路径）
cd d:\english_platform\backend-spring
mvn -B spring-boot:run          # 日志出现 seed: 社交演示数据初始化完成

# 4. 启动前端
cd d:\english_platform\frontend
npm run dev                     # http://localhost:5173
```

演示账号：学员 `13800138000 / 123456`（Momo，含未读私信与通知）、`13700000000`（Leo）、`13600000000`（Cici）、管理员 `13900000000 / admin123`。

---

## 8. 验收结果（实测）

| 项 | 结果 |
| --- | --- |
| `mvn -B spring-boot:run` | 启动正常，日志打印「seed: 社交演示数据初始化完成（关注 4 条 / 私信 6 条 / 通知 3 条 / 分享 2 条）」 |
| `POST /api/auth/login` | 正常签发 token |
| 前端 `npm run type-check` | 通过（0 错误） |
| 前端 `npm run build` | 通过 |
| 页面冒烟 | http://localhost:5173 返回 200，侧栏「消息」红点可渲染 |

---

## 9. 已知限制与后续规划

| 限制 | 说明 | 后续方案 |
| --- | --- | --- |
| 未读数靠轮询 | 30 秒轮询 `/api/messages/unread`，非实时 | 接入 WebSocket / SSE 推送 |
| 私信无附件与表情 | 仅纯文本 | 支持图片、表情、语音 |
| 通知仅社区互动 | 暂未覆盖系统公告、关注事件 | 扩展 `type`，关注时产生 `follow` 通知 |
| 分享不直连第三方 | 只复制链接与留痕 | 对接各平台 SDK 真分享 |
| 无黑名单/隐私开关 | 任何登录用户可互相关注与私信 | 增加隐私设置与举报拦截 |
| 关注列表上限 100 | `findTop100…` 截断 | 改为分页接口 |

---

## 10. 主要文件变更

**后端新增**

- `entity/UserFollow.java`、`entity/UserMessage.java`、`entity/UserNotification.java`、`entity/ShareRecord.java`
- `repository/UserFollowRepository.java`、`UserMessageRepository.java`、`UserNotificationRepository.java`、`ShareRecordRepository.java`
- `service/SocialProfileService.java`、`MessageService.java`、`NotificationService.java`、`ShareService.java`
- `controller/MessageController.java`、`NotificationController.java`、`ShareController.java`
- `dto/SocialDtos.java`

**后端修改**

- `controller/UsersController.java`（他人主页、关注/取关、粉丝/关注列表）
- `entity/User.java`、`dto/UserDtos.java`、`dto/Dtos.java`（`bio` 字段贯通）
- `service/CommunityService.java`（点赞/评论/回复通知插桩与撤销）
- `repository/CommunityPostRepository.java`（他人主页帖子数与动态查询）
- `seed/DataSeeder.java`（`seedSocial()` 社交演示数据）

**前端新增/修改**：见第 5 节表格。
