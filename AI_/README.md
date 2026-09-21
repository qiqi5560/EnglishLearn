# AI 端 —— 基于大模型场景扮演的英语口语训练系统

> 本目录为小组成员 **D（瑞驰）** 负责的 **AI 部分**，提供三大能力：
> **语音转文字 (ASR)** + **大模型场景对话 (LLM)** + **英文语音合成 (TTS)**。

---

## 1. 能力与技术栈

| 能力 | 用途 | 技术方案 |
| --- | --- | --- |
| ASR 语音转文字 | 把学习者说的英文转成文字，并给出**发音清晰度参考分** | **faster-whisper**（small 模型，CPU 推理） |
| 发音评测 | 跟读打分：**音素级**定位「哪个音读错了、该怎么改」 | **wav2vec2 音素识别模型（ONNX）+ cmudict 词典**（本地离线，无需 PyTorch） |
| LLM 大模型对话 | AI 角色扮演、场景对话陪练，并给出语法/用词改进建议 | **Ollama + qwen2.5:1.5b-instruct**（本地运行） |
| TTS 英文发声 | 把 AI 回复朗读成英文 | **edge-tts**（微软免费在线语音，需联网） |

> 组长原方案中的 openai-whisper 已替换为 faster-whisper：
> 本机无 NVIDIA 独显，faster-whisper 在 CPU 上识别速度快约 4 倍，且不依赖庞大的 PyTorch。
> 代码中已做 **GPU 自动检测**：有 NVIDIA 显卡用 CUDA，没有则自动退回 CPU。

### 处理链路

```
你说的英文 ──▶ [ASR] 转文字+清晰度分 ──▶ [LLM] 场景角色扮演+语法建议 ──▶ AI英文回复
                                                                      │
                                      文字对话(demo_chat) ◀──────────┘
                                      [TTS] 合成英文语音 ──▶ 播放(demo_voice)

跟读纠错(另一条线)：参考句子 + 你的跟读录音 ──▶ [发音评测] 音素级打分与纠错(demo_read)
```

---

## 2. 目录结构

```
AI/
├── config.py            # 全局配置：所有路径/模型名/缓存位置（改这里即可）
├── asr.py               # ASR 模块（GPU/CPU 自动检测 + 发音清晰度参考分）
├── pronunciation.py     # 音素级发音评测模块（本地 ONNX 音素模型 + cmudict 词典）
├── llm.py               # LLM 模块（自动拉起 Ollama 服务）
├── tts.py               # TTS 模块
├── scene_prompts.py     # 口语场景剧本库（18 个场景，含"口语测试·模拟考试"）+ 反馈指令
├── test_llm.py          # 大模型对话单测
├── test_asr.py          # 语音转文字单测（可自动生成样例语音自测闭环）
├── test_tts.py          # 英文发声单测
├── test_pronunciation.py# 音素级发音评测单测（自动生成标准音自测闭环）
├── demo_chat.py         # 文字版场景扮演 demo（不依赖麦克风）
├── demo_voice.py        # 全语音版 demo（录音→识别→对话→发声）
├── demo_read.py         # 跟读纠错 demo（跟读一句，音素级指出哪个音读错）
├── install_deps.bat     # 一键装依赖（并把 pip 下载缓存固定到 D 盘 pip_cache）
├── start_llm_server.bat # [主机专用] 启动大模型服务并允许局域网组员连接
├── requirements.txt     # 依赖清单（组长/其他成员一键复现环境）
├── README.md            # 本文件
├── .gitignore           # 排除 .venv/models/pip_cache/audio 等大文件
├── models/              # [本地自动生成] 所有模型（D盘显眼目录, 不上传）
│   ├── whisper-small/   #   whisper 语音识别模型(约460MB)：model.bin 等文件直接可见
│   └── pronunciation/   #   音素识别模型(约300MB)：model_quantized.onnx、vocab.json 直接可见
├── pip_cache/           # [本地自动生成] pip 依赖下载缓存（D盘, 不上传）
├── audio/               # [本地自动生成] 录音/TTS 输出 (不上传)
└── .venv/               # [本地自动生成] Python3.11 虚拟环境 (不上传)
```

### 东西都放在哪？（全部在 D 盘，不在 C 盘留痕）

| 内容 | 位置 | 说明 |
| --- | --- | --- |
| 语音识别模型 | `d:\英语练习\AI\models\whisper-small\` | 打开就能看见 `model.bin` 等文件 |
| 音素评测模型 | `d:\英语练习\AI\models\pronunciation\` | 打开就能看见 `model_quantized.onnx`、`vocab.json` |
| pip 依赖缓存 | `d:\英语练习\AI\pip_cache\` | 由 `.venv\pip.ini` 指定，装依赖不再写 C 盘 |
| 大模型（千问） | `d:\Ollama\models\` | 由 Ollama 的 `OLLAMA_MODELS` 指定 |
| 录音 / TTS 输出 | `d:\英语练习\AI\audio\` | — |

> 模型目录是**扁平**的：没有 `snapshots\<哈希>` 层层嵌套，也没有隐藏的 `.cache`，
> 直接就能看见模型本体。想换识别模型档位，只改 `config.py` 里的 `WHISPER_MODEL`，
> 目录名（`whisper-<档位>`）与下载来源会自动跟着变。

**开发机环境要求**：Windows + Python 3.11；Ollama 程序在 `D:\Ollama\ollama`；
大模型存放于 `D:\Ollama\models`（通过用户环境变量 `OLLAMA_MODELS` 指定，避免占 C 盘）。

---

## 3. 一键复现环境（组长或组员在新电脑上操作）

```bash
# 1) 安装 Ollama 并拉取组长指定模型（程序与模型建议按 D:\Ollama 布局）
ollama pull qwen2.5:1.5b-instruct

# 2) 创建 Python 3.11 虚拟环境并激活
py -3.11 -m venv .venv
.venv\Scripts\activate        # Windows PowerShell
# source .venv/bin/activate   # macOS / Linux

# 3) 安装依赖（pip 下载缓存会自动落到 D 盘 pip_cache）
pip install -r requirements.txt
```

> 更省事的方式：直接**双击 `install_deps.bat`**。它会自动创建 `.venv`、
> 把 pip 下载缓存固定到 `d:\英语练习\AI\pip_cache`，再安装依赖。
> 项目已在 `.venv\pip.ini` 里写好 `cache-dir`，只要激活本虚拟环境，
> 手动执行 `pip install` 同样不会往 C 盘写。

> 首次运行 `test_asr.py` 或 `demo_voice.py` 会自动下载 whisper 模型
> （约 460MB，保存到 `models\whisper-small`，之后离线可用）。模型下载已配置国内镜像源，
> 国内网络无需代理即可下载。

---

## 4. 使用与测试方法

```bash
python test_llm.py      # 测试千问大模型本地对话（先确认 Ollama 服务已启动）
python test_tts.py      # 测试英文发声（会弹出播放器试听，需联网）
python test_asr.py      # 测试语音转文字（自动用TTS生成样例再识别，自测闭环）
python test_pronunciation.py  # 测试音素级发音评测（自动生成标准音自测闭环）

python demo_chat.py     # 体验：文字版 AI 场景扮演练口语（18 个场景可选）
python demo_voice.py    # 体验：全语音版！对着麦克风说英语，
                        # 程序自动 识别→AI扮演回复→英文朗读出来
python demo_read.py     # 体验：跟读纠错！跟读一句英文，
                        # 程序给出音素级评分，指出哪个音读错、该怎么改
```

运行前先确保 Ollama 服务已启动：
- 代码 `llm.py` 会**自动检测并启动** Ollama 服务（模型目录自动指向 D 盘）；
- 若自动启动失败，可手动双击 `D:\Ollama\start_ollama.bat`。

### 场景列表（共 18 个）

**陪练场景（17 个）**：餐厅点餐、机场值机、酒店入住、求职面试、初次见面、
商场购物、看病就医、银行办事、街头问路、打车出行、电话预约、租房看房、
客服退换货、校园咨询、城市观光、找兼职、日常闲聊。
每个场景 AI 扮演一个角色陪聊，并在每轮回复后给一句 `(Fix)` 语法/用词纠错。

**口语测试 · 模拟考试（1 个）**：`oral_exam`，AI 扮演考官 Mrs. Carter。
与陪练场景有 3 点不同：

| 区别 | 陪练场景 | 口语测试场景 |
| --- | --- | --- |
| 纠错时机 | 每轮都给 `(Fix)` | 考试中**不纠错**，保证像真实考试 |
| 出题方式 | 自由聊天 | 固定 **5 道题**，一次一问，答太短会追问一句 |
| 结果 | 逐句纠错 | 考完输出**成绩报告**：流利度/语法/词汇/内容/总分 + 优点 + 问题 + 建议 |

> 成绩报告每行以 `(Feedback)` / `(Fix)` 开头，会被 `llm.split_feedback()` 识别为
> **文字提示**——只在屏幕上显示，不会被 TTS 朗读（念分数很奇怪）。
> 想结束考试时，直接输入 `finish` 或 `I am done` 即可拿到报告。

---

## 5. 局域网共享大模型（组员连组长电脑，无需各自下载 1.5B 模型）

本项目是**本地使用**的，不需要上云。若只有组长一台电脑装了大模型，
其他成员可以**直接连组长电脑上的 Ollama**，模型只在组长机器上跑一份。

### 5.1 组长（主机）要做的 3 件事

```powershell
# ① 放行防火墙 11434 端口（管理员 PowerShell，只需执行一次）
New-NetFirewallRule -DisplayName "Ollama 11434" -Direction Inbound -Protocol TCP -LocalPort 11434 -Action Allow

# ② 双击项目里的 start_llm_server.bat 启动服务
#    它会把 Ollama 设为监听 0.0.0.0:11434（所有网卡），并【保持窗口开启不要关】

# ③ 查自己的 IP，发给组员
ipconfig        # 连热点时通常是 192.168.137.1；连 WiFi 时看"无线局域网适配器 IPv4"
```

### 5.2 组员要做的 2 件事

```powershell
# ① 设置环境变量指向组长电脑（IP 换成组长给的，设置后需重开终端）
setx LLM_SERVER_URL "http://192.168.137.1:11434"

# ② 直接跑 demo，代码会自动连到组长的大模型
python demo_chat.py
```

> 组员**不需要**下载 `qwen2.5:1.5b-instruct`（省约 1GB），也**不需要**本机装 Ollama 模型。
> 组员电脑上的 `llm.py` 检测到是远程地址后，不会去启动本机服务。

### 5.3 网络怎么选

| 方式 | 说明 |
| --- | --- |
| **组长开热点**（推荐） | 组员连组长热点，组长 IP 稳定是 `192.168.137.1`，不依赖教室 WiFi |
| 同一 WiFi / 教室局域网 | 组员连同一网络，用组长 `ipconfig` 查到的 IPv4 地址 |
| 网线直连 / 交换机 | 同上，IP 用 `ipconfig` 查 |

> 注意：**同一时间只能组长一台机器跑模型**，多人并发提问会排队变慢（1.5B 模型 CPU 推理约 1~3 秒一条）。
> 演示时建议一个一个来，或提前打好招呼。

### 5.4 连不上怎么排查

1. 组长终端是否还开着？（关了服务就停了）
2. 两边是否在同一网络？（组员连的是组长热点吗？）
3. 组长是否执行过上面第 ① 步的防火墙命令？
4. 组员 `LLM_SERVER_URL` 里的 IP 是否和组长 `ipconfig` 查到的一致？（热点 IP 可能变）
5. 在组员电脑上测试连通性：`Test-NetConnection 192.168.137.1 -Port 11434`

---

## 6. 常见问题

| 问题 | 处理办法 |
| --- | --- |
| edge-tts 报网络错误 | TTS 依赖微软在线服务，需联网重试 |
| whisper 模型下载失败 | 已内置国内镜像(hf-mirror)，若仍失败检查网络后重跑即可 |
| 音素模型下载失败 | 首次运行 `demo_read.py` 会下载约 300MB 音素模型（同样走国内镜像），失败重跑即可 |
| pip 提示 `pip.ini` 编码警告 | 用 `install_deps.bat` 装依赖（它用环境变量指定缓存目录，不受文件编码影响） |
| 模型文件位置不对 | 一切路径都在 `config.py` 里，改完重跑即可；模型缺失会自动重新下载并整理成扁平结构 |
| 发音评分偏差 | 音素评测基于本地模型，分数仅供参考；换用质量好一点的麦克风会明显更准 |
| 大模型回复慢 | 本机无独显，1.5B 模型 CPU 推理约 1~3 秒一条属正常；演示够用 |
| demo_voice 录不到声音 | 检查系统麦克风权限（Windows 设置→隐私→麦克风） |
| 组员连不上大模型 | 见第 5.4 节排查；确认组长已用 `start_llm_server.bat` 启动并放行防火墙 |

## 7. 对组长整合的说明

- 本目录为独立 AI 端代码，不含任何模型/venv/音频大文件（见 `.gitignore`）；
- 依赖通过 `requirements.txt` 复现，环境变量要求见 config.py 注释；
- 后续如需前后端调用，可在现有 `asr.py / llm.py / tts.py` 之上封装 FastAPI 服务
  （每个模块均提供独立的类方法与简单调用接口，解耦良好）。
