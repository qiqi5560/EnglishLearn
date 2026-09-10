# 英语口语训练系统 · 后端服务（FastAPI）

为前端 Vue 原型提供真实数据服务：账号与鉴权、场景库、学习资源、对话会话与小结、入学测评与学习方案、学习报表、社区内容、管理后台。前端所有页面均已切换为真实接口调用。

- 监听端口：`8080`
- 接口前缀：`/api`（与 `frontend/vite.config.ts` 的 `/api` 代理对齐）
- 接口文档（OpenAPI）：启动后访问 `http://127.0.0.1:8080/docs`

## 技术栈

Python 3.14 · FastAPI · SQLAlchemy 2.0 · SQLite · Pydantic v2 · PyJWT · bcrypt

## 快速开始

```bash
cd backend

# 1. 创建虚拟环境并安装依赖（仅首次）
python -m venv .venv
.venv\Scripts\activate            # Windows
# source .venv/bin/activate       # macOS / Linux
pip install -r requirements.txt

# 2. 可选：按需复制环境配置（不配置时使用默认值）
copy .env.example .env

# 3. 启动（启动时自动建表；空库自动灌入种子数据）
python run.py
```

首次启动会自动创建 `data/app.db`，若数据库为空则写入种子数据（场景 / 资源 / 帖子 / 演示账号 / Momo 学习记录），保证前端首次联调即有演示内容。删除 `data/app.db` 可重置为初始状态。

## 演示账号与验证码

| 账号 | 手机号 | 密码 | 角色 |
| --- | --- | --- | --- |
| 管理员 | 13900000000 | admin123 | admin |
| Momo（学习者） | 13800138000 | 123456 | learner |
| 其余种子用户 | 手机号见日志 | 123456 | learner / guardian |

- 短信验证码为 mock：**固定 `123456`**，调用 `/auth/send-code` 后会打印到后端日志，不接真实短信。
- 验证码登录时手机号未注册会自动注册。
- 学习者账号在 `/auth/login` 密码登录；管理员必须走 `/auth/admin/login`（非 admin 返回 403）。

## 目录结构

```
backend/
├── run.py                    # 启动入口（uvicorn, 8080）
├── requirements.txt
├── .env.example
├── app/
│   ├── main.py               # 应用实例、CORS、路由注册、建表 + 种子
│   ├── core/                 # 配置(config)、JWT/bcrypt(security)、依赖注入(deps)
│   ├── db/                   # DeclarativeBase + 引擎/会话
│   ├── models/               # user/scene/conversation/resource/plan/study/community/sysconfig
│   ├── schemas/              # 统一响应信封 ok()/fail()、序列化器
│   ├── api/v1/               # auth/users/scenes/resources/dialogues/plans/community/reports/admin
│   ├── services/             # 业务服务
│   │   └── llm/              # LLM 适配层（当前 MockProvider 规则式占位）
│   └── seed/                 # 种子数据（与前端原型数据一致）
```

## LLM 适配层（AI 能力扩展点）

对话回复与评测统一通过 `app/services/llm/base.py` 抽象接口派发，当前实现为 `mock_provider.py`（关键词规则英文回复 + 四维评分）。后续接入真实大模型时，新增一个实现该接口的 Provider 并在配置中切换即可，路由与前端无需改动。

- `reply(scene, history, user_input)` → 返回 AI 英文回复与中文释义
- `evaluate(history)` → 返回四维评分（pron/fluency/reaction/natural）、纠错、亮点与建议

## 关键约定

- 统一响应信封：`{ "code": number, "message": string, "data": unknown }`，`code = 0` 表示成功。
- 错误码：`0` 成功；`401` 未登录/凭证错误；`403` 无权限/账号停用；`404` 不存在；`409` 状态冲突；`422` 参数不合法；`500` 服务异常。
- 鉴权：`Authorization: Bearer <accessToken>`；管理端接口额外要求 `admin` 角色。
- 完整接口契约见 `docs/api-contract.md`。
