# 版本更新说明 v1.2.0 — 口语水平预测 + 个性化推荐 + 效果指标量化

| 项 | 内容 |
| --- | --- |
| 版本号 | v1.2.0 |
| 发布日期 | 2026-09-20 |
| 涉及模块 | 后端 `backend-spring`、前端 `frontend` |
| 依赖变更 | **无**（未新增任何 Maven / npm 依赖） |
| 数据库变更 | **不新增表、不改表结构**（模型权重写入既有 `sys_config` 表） |
| 兼容性 | 向下兼容；需按「升级步骤」重建种子库后重启 |

---

## 1. 更新概述

本次更新围绕「让推荐真正个性化，并能被量化评估」这一目标，新增三块能力：

1. **口语水平预测模型**：用纯 Java 手写的 Softmax 回归（多项逻辑回归）从历史评测数据中学习，预测用户属于「初级 / 中级 / 高级」哪一档，并映射回系统既有 CEFR 等级（A1~C2）。
2. **个性化推荐**：替换首页原先「取前 N 条」的假推荐，用预测水平驱动**场景 / 学习资源 / 每日任务**三类推荐，返回推荐理由与匹配度。
3. **效果指标量化**：定义并计算**点击率、完成率、互动率、跳出率**四项运营指标，附口径说明、样本量检测与趋势数据，并在管理端新增看板页面。

---

## 2. 功能一：口语水平预测

### 2.1 样本与特征

- **样本粒度：会话**（`ConversationSession`）。种子库学员数少，按「用户」只有个位数样本；按会话聚合其 `AssessmentRecord` 均值可得数十条样本，且特征语义一致。
- **特征向量 6 维 + 偏置 = 7 维**，全部天然落在 0~1 区间，无需额外保存归一化参数：

| # | 特征 | 计算 |
| --- | --- | --- |
| 0 | 发音 | `pronAvg / 100` |
| 1 | 流利度 | `fluencyAvg / 100` |
| 2 | 反应 | `reactionAvg / 100` |
| 3 | 自然度 | `naturalAvg / 100` |
| 4 | 会话时长 | `min(durationSec / 600, 1)` |
| 5 | 用户发言数 | `min(userMsgCount / 10, 1)` |
| 6 | 偏置 | `1` |

- **标签自动生成**（无需人工标注）：`score = 发音×0.35 + 流利度×0.25 + 反应×0.2 + 自然度×0.2`，权重与 `DialogueService` 中既有会话聚合口径**严格一致**，避免预测等级与用户看到的历史得分自相矛盾。
- **分箱映射**：`score < 55` 初级（A1/A2）、`55 ≤ score < 80` 中级（B1/B2）、`score ≥ 80` 高级（C1/C2）。

### 2.2 算法（手写，零依赖）

`backend-spring/src/main/java/com/englishlearn/ml/SoftmaxRegression.java`：

| 参数 | 取值 |
| --- | --- |
| 模型 | Softmax 回归，`W[3][7]` |
| 损失 | 交叉熵 + L2 正则 |
| 学习率 / 迭代 | `lr = 0.1`，全批量梯度下降 `500` 轮 |
| L2 系数 | `λ = 0.01` |
| 数据集划分 | 固定随机种子 `SEED = 42`，7:3 划分训练/验证 |
| 输出 | 三档概率 + 验证集准确率 |
| 复杂度 | `O(iter·N·K·D)`；N≈50 时约 50 万次浮点运算，**毫秒级**，无性能瓶颈 |

### 2.3 冷启动与兜底（重要）

| 场景 | 行为 |
| --- | --- |
| 样本 `N < 20` 或存在空档位 | **不训练、不覆盖**已有模型，接口返回 `trained=false` 与原因说明 |
| 模型未训练 | 走**规则分箱**预测，`source = "rule"`，返回 one-hot 式概率（0.9/0.05/0.05） |
| 模型已训练 | `source = "model"` |
| 用户无任何会话 | 用入学测评的 `LearningPlan.levelCurrent` 兜底，再兜底到 `A2` |

保证任何数据量下接口都返回合理等级，不会出现随机权重导致的乱判。

### 2.4 持久化

模型权重以 JSON 写入既有 `sys_config` 表，key = `ml_level_model`（**不新建表**）：

```json
{
  "version": "v1", "trainedAt": "...", "sampleCount": 32, "accuracy": 90.0,
  "weights": [[7], [7], [7]], "bands": ["初级","中级","高级"], "featureNames": [...]
}
```

---

## 3. 功能二：个性化推荐

### 3.1 三策略融合

读取 `sys_config.system_config` 中 `recommend:{content, collab, model}` 三个既有开关（true=1 / false=0）作为权重，归一化后加权求和：

| 策略 | 说明 |
| --- | --- |
| `content` 基于内容 | 候选 CEFR 等级与预测等级的距离匹配：相同 1.0、相邻档 0.6、相隔 0.3 |
| `collab` 协同过滤 | Item-Based 余弦相似度；隐式反馈 `r(u,i) = 1 + log(会话数)` |
| `model` 模型预测 | `Σ_k P(档位_k) × match(档位_k, 候选等级)`，概率加权比硬分箱更平滑 |

**业务微调**：难度略高于当前水平 +0.05（最近发展区）；今日任务已包含的场景 ×0.3；近期反复练习 ×0.7（去重）。
**冷启动**：无行为数据的用户按全局热度（各场景会话总数）排序。
**性能**：相似度矩阵 `ConcurrentHashMap` + 5 分钟 TTL 缓存，避免每次请求全表扫描会话与评测表。

### 3.2 推荐对象

| 类型 | 数据来源 | 改造点 |
| --- | --- | --- |
| 场景 | `Scene.levelScope` | 替换 `SceneService.recommended()` 的原「取前 N 条」假推荐 |
| 学习资源 | `LearningResource.level` | 按预测水平匹配难度 |
| 每日任务 | `LearningPlan` + `DailyTask` | `PlanService.buildTasksForDate` 的场景/素材选材改由推荐结果决定，时长按档位自适应（初级 8 / 中级 12 / 高级 15 分钟） |

### 3.3 免登录降级

`/api/scenes/recommended` 保持**免登录**：未登录或 token 失效时按会话数返回热门场景（HTTP 200 正常渲染首页），已登录走个性化。为此新增 `AuthFacade.currentUser()`——静默返回 `null`，不抛 401。

---

## 4. 功能三：指标量化与检测标准

### 4.1 一期口径（基于现有业务数据，不依赖埋点）

| 指标 | 计算公式（一期） | 数据来源 |
| --- | --- | --- |
| 点击率 `ctr` | 已产生学习行为的每日任务数 / 每日任务总数（场景类：当天存在该 sceneId 的会话；资源/单词类：`done=1` 或当天有 `StudyRecord`） | `DailyTask` + `ConversationSession` + `StudyRecord` |
| 完成率 `completionRate` | `done=1` 任务数 / 任务总数 | `DailyTask` |
| 互动率 `interactionRate` | 窗口内有发帖/评论/点赞的用户数 / 窗口内活跃用户数 | `CommunityPost` + `CommunityComment` + `PostLike` |
| 跳出率 `bounceRate` | 「无用户发言」或 `durationSec < 30` 的会话数 / 会话总数 | `ConversationSession` + `ConversationMessage(speaker='user')` |

### 4.2 样本量检测（避免误导性数字）

- 普通指标最小分母 `MIN_DENOMINATOR = 5`；用户类指标 `MIN_USER_DENOMINATOR = 3`。
- 分母为 0 → 指标返回 `null`，前端渲染「—」；分母低于阈值 → `sample.insufficient = true`，前端显示「样本不足，仅供参考」。
- 响应同时返回 `definitions`（key / 名称 / 计算公式 / 数据来源）固化口径，以及按天 `trend` 趋势，在管理端直接呈现，兑现「检测标准」的书面化。
- `stage = "phase1"` 标识一期口径；二期接入埋点后替换为真实曝光/停留数据。

---

## 5. 接口变更清单

| 方法 | 路径 | 鉴权 | 状态 | 说明 |
| --- | --- | --- | --- | --- |
| GET | `/api/recommend/profile` | 登录 | 新增 | 水平预测详情：等级、档位、三档概率、置信度、特征、样本数、source |
| GET | `/api/recommend/overview?limit=` | 登录 | 新增 | 一次返回水平 + 场景/资源/任务三类推荐 |
| GET | `/api/recommend/scenes?limit=` | 登录 | 新增 | 仅场景推荐 |
| GET | `/api/recommend/resources?limit=` | 登录 | 新增 | 仅资源推荐 |
| GET | `/api/recommend/tasks?limit=` | 登录 | 新增 | 仅任务推荐 |
| GET | `/api/scenes/recommended?limit=` | **免登录** | **改造** | 已登录走个性化，未登录降级热门 |
| GET | `/api/admin/ml/status` | 管理员 | 新增 | 当前模型状态（版本、样本数、准确率、档位分布、训练下限） |
| POST | `/api/admin/ml/train` | 管理员 | 新增 | 触发训练，返回 `sampleCount / accuracy / distribution / trained` |
| GET | `/api/admin/metrics?days=14` | 管理员 | 新增 | 四指标 + 趋势 + 样本量 + 口径定义 |

响应信封、错误码沿用 `docs/api-contract.md` 既有约定。

---

## 6. 前端变更

| 文件 | 变更 |
| --- | --- |
| `api/modules/recommend.ts` | 新增：`recommendProfile/Overview/Scenes/Resources/Tasks`、`adminMetrics`、`adminTrainModel`、`adminModelStatus` |
| `types/api.ts` | 新增 `LevelPrediction`、`RecommendOverview`、`RecommendSceneItem/ResourceItem/TaskItem`、`ModelStatus`、`TrainResult`、`MetricDefinition`、`MetricTrend`、`AdminMetrics` |
| `composables/useTracker.ts` | 新增：一期 no-op 的 `track(type, payload)`，仅 DEV 控制台留痕 |
| `composables/useTaskJump.ts` | `startTask` 内插入 `track('click')` 钩子 |
| `stores/plan.ts` | `toggleTask` 内插入 `track('finish')` 钩子 |
| `views/home/Home.vue` | 右栏推荐区升级：顶部「AI 预测水平」胶囊卡（等级徽标 + 置信度进度条 + 来源标签）+ 场景/素材/任务三标签页（保留 `.scene-grid` / `.tone-0~3` 既有视觉，新增 `.reason-tag` 推荐理由徽标） |
| `views/admin/MetricsBoard.vue` | 新增：四指标卡 + `TrendChart` 趋势（7/14/30 天切换）+ 指标口径定义表 + 模型训练卡（一键重新训练） |
| `router/index.ts` | 新增 `/admin/metrics`（`requiresAdmin`） |
| `layouts/AdminLayout.vue` | 「运营总览」分组新增「推荐效果指标」菜单项 |

---

## 7. 升级步骤

> 关键：`DataSeeder.seed()` 有 `if (sceneRepository.count() > 0) return;` 判重守卫，已存在的库不会重播种子。**必须删库才能让扩种生效。**

```powershell
# 1. 停止后端进程（8080）
Stop-Process -Name java -Force -ErrorAction SilentlyContinue

# 2. 删除旧库（库内均为种子数据，会按新种子重建）
Remove-Item d:\english_platform\backend-spring\data\app.db

# 3. 启动后端（不要用根目录 启动后端.bat，其硬编码原作者离线 Maven 路径）
cd d:\english_platform\backend-spring
mvn -B spring-boot:run          # 日志确认 Started EnglishLearnApplication

# 4. 触发模型训练（管理员 13900000000 / admin123）
POST /api/admin/ml/train

# 5. 启动前端
cd d:\english_platform\frontend
npm run dev                     # http://localhost:5173
```

扩种内容：Momo / Leo / Cici 三名学员各 10 个 `sessionStatus="finished"` 的会话（每会话 3~5 条 `AssessmentRecord`，分数落在 A2 / B1 / B2 三档并体现进步趋势）+ 1 次跳出会话，配套 `StudyRecord`、`done` 混合的 `DailyTask`、社区评论与点赞。

---

## 8. 验收结果（实测）

| 项 | 结果 |
| --- | --- |
| `mvn -B compile` | 通过，`pom.xml` 无新增依赖 |
| `POST /api/admin/ml/train` | `trained=true`，`sampleCount=32`，`accuracy=90.0`，档位分布 初级 4 / 中级 22 / 高级 6 |
| `GET /api/recommend/overview`（学员 13800138000） | `band=中级 level=B1 score=69.6 confidence=61.0 source=model`，场景/资源/任务各 4 条，带 `reason` 与 `match` |
| `GET /api/scenes/recommended?limit=6` 未登录 | HTTP 200，降级热门；登录后不同学员结果不同 |
| `GET /api/admin/metrics?days=14` | `ctr=86.2 / completion=63.8 / interaction=100.0 / bounce=8.6`，`insufficient=false`，`definitions=4`，`trend=14` 天 |
| 前端 `npm run type-check` / `npm run build` | 均通过（0 错误） |

附：本次同时修复了仓库中遗留的 Git 冲突标记（`views/entrance/EntranceTest.vue`、`views/dialogue/DialogueRoom.vue`），此前会导致 `vite build` 失败。

---

## 9. 已知限制与二期规划

| 限制 | 说明 | 二期方案 |
| --- | --- | --- |
| 点击率非真实点击 | 一期用「是否产生学习行为」近似，分母不是曝光数 | 建 `user_event` 表 + `POST /api/track`，前端真实上报 |
| 跳出率非真实跳出 | 一期用「无发言或时长 < 30s」近似 | 改为「停留 < 10s 且无交互」 |
| 无曝光日志 | 无法计算曝光转化率 | 补曝光（expose）事件 |
| 训练需手动触发 | 目前由管理员点「重新训练」 | 可加定时任务按数据量自动重训 |
| 特征维度固定 6 维 | 未纳入词汇量、语法错误率等 | 数据积累后扩展特征并升 `FEATURE_VERSION` |

埋点调用点（`useTracker.track`）已在首页推荐点击、任务开始、任务完成处预留，二期接入时无需再改业务组件。

---

## 10. 主要文件变更

**后端新增**

- `ml/SoftmaxRegression.java`（手写 Softmax 回归）
- `service/LevelPredictService.java`（特征工程、训练、预测、模型持久化）
- `service/RecommendService.java`（三策略融合推荐）
- `service/MetricService.java`（四指标量化与样本量检测）
- `controller/RecommendController.java`

**后端修改**

- `controller/AdminController.java`（新增 `/admin/metrics`、`/admin/ml/status`、`/admin/ml/train`）
- `service/SceneService.java`（假推荐 → 个性化推荐 + 免登录降级）
- `service/PlanService.java`（任务选材与时长自适应）
- `security/AuthFacade.java`（新增静默 `currentUser()`）
- `seed/DataSeeder.java`（扩种训练与行为数据）
- 各 Repository 补充批量查询方法（防 N+1）

**前端新增/修改**：见第 6 节表格。
