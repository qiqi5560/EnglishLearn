# EnglishLearn Backend (SpringBoot)

英语口语训练系统后端，由原 FastAPI 项目完整重构为 SpringBoot。接口与原后端**完全一致**，前端 Vue 无需改动。

## 技术栈

- SpringBoot 3.4.x + Spring Data JPA
- SQLite（`org.xerial:sqlite-jdbc` + Hibernate 社区方言 `SQLiteDialect`）
- Spring Security（BCrypt 密码加密）+ jjwt（JWT 鉴权）
- SpringDoc OpenAPI（接口文档）
- 策略模式 LLM 层：`MockLlmProvider` / `OllamaLlmProvider`

## 环境要求

- JDK 17+
- Maven 3.8+

## 快速启动

```bash
cd backend-spring
mvn spring-boot:run
```

启动成功后：

- 服务地址：`http://localhost:8080`
- 接口统一前缀：`/api`（如 `/api/health`）
- Swagger UI：`http://localhost:8080/api/docs`
- OpenAPI JSON：`http://localhost:8080/api/api-docs`

首次启动会**自动建表并灌入种子数据**（演示账号、场景、学习资源、社区帖子、学习记录）。

## 打包运行

```bash
mvn clean package -DskipTests
java -jar target/english-learn-backend-1.0.0.jar
```

## 配置说明

配置文件：[src/main/resources/application.yml](src/main/resources/application.yml)

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | `8080` | 监听端口 |
| `server.servlet.context-path` | `/api` | 接口统一前缀 |
| `spring.datasource.url` | `jdbc:sqlite:data/app.db` | SQLite 数据库文件（相对运行目录） |
| `spring.jpa.hibernate.ddl-auto` | `update` | 启动自动建表/更新表结构 |
| `app.jwt.secret` | `dev-secret-change-me-in-production` | JWT 密钥 |
| `app.jwt.expire-minutes` | `10080` | Token 有效期（7 天） |
| `app.sms.mock-code` | `123456` | mock 短信验证码（发送时打印到日志） |
| `llm.provider` | `mock` | LLM 提供方：`mock` / `ollama` |
| `llm.ollama.base-url` | `http://localhost:11434` | Ollama 服务地址 |
| `llm.ollama.model` | `qwen2.5:7b-instruct` | Ollama 模型名 |

## 演示账号

| 手机号 | 密码 | 角色 | 昵称 |
|--------|------|------|------|
| `13900000000` | `admin123` | 管理员 | 管理员 |
| `13800138000` | `123456` | 学员 | Momo |
| `13700000000` | `123456` | 学员 | Leo |
| `13600000000` | `123456` | 学员 | Cici |
| `13300000000` | `123456` | 学员（儿童） | 小明 |
| `13500000000` | `123456` | 监护人 | 王妈妈 |

短信验证码登录使用固定验证码 `123456`（发送验证码时会打印到后端日志）。

## 接口概览

| 模块 | 路径 | 说明 |
|------|------|------|
| 健康检查 | `GET /api/health` | 服务状态 |
| 鉴权 | `/api/auth/*` | 发送验证码、登录、注册、管理员登录 |
| 个人中心 | `/api/users/*` | 我的资料、改密码、绑定监护人 |
| 场景库 | `/api/scenes/*` | 场景列表、推荐、详情 |
| 学习资源 | `/api/resources/*` | 资源列表、详情 |
| 对话会话 | `/api/dialogues/*` | 创建会话、发消息、结束、小结 |
| 学习方案 | `/api/plans/*` | 入学测评、当前方案、每日任务 |
| 学习报表 | `/api/reports/*` | 成长曲线、能力雷达、统计 |
| 社区 | `/api/community/*` | 话题、帖子、点赞、评论 |
| 管理后台 | `/api/admin/*` | 看板、场景/资源/帖子/用户/配置管理（需管理员角色） |

完整接口字段见 Swagger UI：`/api/docs`。

## LLM 提供方切换

默认使用 mock（不依赖外部服务，直接返回固定回复）。切换到 Ollama：

1. 先确认 Ollama 已启动并已拉取模型：

   ```bash
   ollama pull qwen2.5:7b-instruct
   ```

2. 修改 `application.yml`：

   ```yaml
   llm:
     provider: ollama
   ```

两套提示词：场景对话 `reply`、口语评估 `evaluate`（评估强制输出 JSON）。

## 重置数据

删除数据库文件后重启即可重建表并重新灌入种子数据：

```bash
rm -f data/app.db   # Windows: del data\app.db
mvn spring-boot:run
```

## 项目结构

```
backend-spring/src/main/java/com/englishlearn/
├── controller/    # 10 个 REST 控制器
├── service/       # 业务逻辑（Auth/Scene/Resource/Dialogue/Plan/Report/Community/Admin）
├── repository/    # Spring Data JPA 数据访问接口
├── dto/           # 请求体 record + 响应序列化
├── entity/        # JPA 实体（与原 SQLAlchemy 表结构一致）
├── config/        # 数据源、Jackson、OpenAPI 配置
├── security/      # JWT、鉴权门面、Security 配置
├── llm/           # LLM 策略层（LlmProvider / Mock / Ollama）
├── seed/          # 启动自动灌入种子数据
└── common/        # 统一响应 / 业务异常 / 工具类
```

## 响应格式

所有接口统一返回：

```json
{ "code": 0, "message": "ok", "data": { } }
```

- `code = 0` 表示成功
- 错误时 `code` 与 HTTP 状态码一致（`401/403/404/422/500`），`message` 为错误说明，`data = null`

JWT 鉴权：请求头携带 `Authorization: Bearer <token>`；管理员接口校验角色，非管理员返回 `403`。