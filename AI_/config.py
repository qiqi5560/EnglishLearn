# -*- coding: utf-8 -*-
"""
config.py —— AI 端全局配置中心

作用：集中管理所有路径、模型名称、服务地址、缓存位置。
以后要改路径 / 换模型，只需要改这一个文件即可，其他模块不用动。

统一存放原则（重要）：
    本项目所有「下载来的东西」都放在 D 盘的显眼位置，绝不在 C 盘留痕：
      - 模型成品   D:\英语练习\AI\models\whisper-small、...\models\pronunciation
      - 依赖缓存   D:\英语练习\AI\pip_cache
      - HF 元数据  D:\英语练习\AI\.cache\huggingface（仅 KB 级，不含模型权重）

注意：本文件要「最先导入」，因为它必须在 huggingface_hub / faster_whisper
等库被导入之前设置好缓存类环境变量，所以下面第三方的 import 一律排在
`from config import ...` 之后。
"""

import os

# ------------------------------------------------------------------
# 路径区（本项目全部文件都放在 D 盘，不占用 C 盘）
# ------------------------------------------------------------------

# AI 端代码根目录（本文件所在目录 = D:\英语练习\AI）
AI_DIR = os.path.dirname(os.path.abspath(__file__))

# 音频目录：存放录音输入文件 与 AI 说出的 mp3
AUDIO_DIR = os.path.join(AI_DIR, "audio")

# 所有模型的「显眼」总目录：打开 models 就能直接看见模型文件夹，没有哈希目录
MODELS_DIR = os.path.join(AI_DIR, "models")

# pip 依赖下载缓存（放 D 盘，以后 pip install 不再往 C 盘写）
PIP_CACHE_DIR = os.path.join(AI_DIR, "pip_cache")

# HuggingFace 兜底缓存（只存 KB 级元数据/锁文件，模型权重一律下到 MODELS_DIR）
HF_HOME_DIR = os.path.join(AI_DIR, ".cache", "huggingface")

# ------------------------------------------------------------------
# 缓存类环境变量：必须赶在任何 HuggingFace 库导入之前设置，所以统一放这里
# ------------------------------------------------------------------

# 国内网络环境下 HuggingFace 官方源不稳定，统一切换到国内镜像站下载模型
os.environ.setdefault("HF_ENDPOINT", "https://hf-mirror.com")
# 新版 HuggingFace 默认走 Xet 加速通道，国内镜像不支持(会401)，强制走传统HTTP
os.environ.setdefault("HF_HUB_DISABLE_XET", "1")
os.environ.setdefault("HF_HUB_DISABLE_SYMLINKS_WARNING", "1")
# 缓存出口固定到 D 盘，杜绝任何库偷偷往 C:\Users\<用户名>\.cache 写东西
os.environ.setdefault("HF_HOME", HF_HOME_DIR)
# pip 缓存兜底（主力是 .venv\pip.ini，这里再兜一层）
os.environ.setdefault("PIP_CACHE_DIR", PIP_CACHE_DIR)

# ------------------------------------------------------------------
# 语音识别 ASR（faster-whisper）：模型扁平放在 models\whisper-small
# ------------------------------------------------------------------

# 模型档位: tiny < base < small < medium < large-v3（越大越准、越慢、越占空间）
WHISPER_MODEL = "small"

# 下载来源（与档位联动，换档位时自动换仓库）
WHISPER_MODEL_REPO = f"Systran/faster-whisper-{WHISPER_MODEL}"

# 模型成品目录：里面直接放 model.bin / config.json / tokenizer.json / vocabulary.txt
WHISPER_MODEL_PATH = os.path.join(MODELS_DIR, f"whisper-{WHISPER_MODEL}")

# 判断「模型是否已就绪」的标志文件
WHISPER_MODEL_FILE = "model.bin"

# 只下载模型本体需要的文件，避免把仓库里的 README 等杂物也拉下来
WHISPER_MODEL_PATTERNS = [
    "model.bin", "config.json", "tokenizer.json",
    "vocabulary.txt", "vocabulary.json", "preprocessor_config.json",
]

# 识别语言: 本项目练的是英语口语，固定识别英语，准确率更高
ASR_LANGUAGE = "en"

# ------------------------------------------------------------------
# 大模型对话 LLM（Ollama 本地服务）
# ------------------------------------------------------------------

# Ollama 程序位置与模型存放位置（都在 D 盘）
OLLAMA_EXE = r"D:\Ollama\ollama\ollama.exe"
OLLAMA_MODELS_DIR = r"D:\Ollama\models"
LLM_MODEL = "qwen2.5:1.5b-instruct"      # 组长指定使用的千问模型（1.5b，速度快约 4 倍）

# --- 大模型服务地址（弹性配置：代码不写死，靠环境变量切换）---
# 本机自己用      -> 什么都不用设，默认连 http://127.0.0.1:11434
# 组员连组长电脑  -> 设环境变量 LLM_SERVER_URL=http://<组长IP>:11434
# 说明：故意不叫 "OLLAMA_HOST"，避免和 Ollama 官方同名环境变量互相干扰
OLLAMA_HOST = os.environ.get("LLM_SERVER_URL", "http://127.0.0.1:11434").rstrip("/")

# 本机启动 Ollama 服务时监听哪个网卡（仅"主机"角色需要关心）：
#   0.0.0.0:11434   -> 监听所有网卡，同一 WiFi / 本机热点下的组员才能连进来
#   127.0.0.1:11434 -> 只允许本机访问（不想被别人连时改这里）
OLLAMA_BIND = os.environ.get("LLM_SERVER_BIND", "0.0.0.0:11434")


def _is_local_host(url):
    """判断一个服务地址是否指向本机"""
    from urllib.parse import urlparse
    host = (urlparse(url).hostname or "").lower()
    return host in ("", "127.0.0.1", "localhost", "0.0.0.0", "::1")


# True  = 当前是"组员"角色，连接别人（组长）电脑上的大模型
# False = 当前是"主机"角色，用本机自己的大模型
LLM_IS_REMOTE = not _is_local_host(OLLAMA_HOST)

# ------------------------------------------------------------------
# 英文语音合成 TTS（edge-tts，微软免费服务，需要联网）
# ------------------------------------------------------------------

TTS_VOICE = "en-US-AriaNeural"           # 微软英文女声（自然清晰）

# ------------------------------------------------------------------
# 发音评测（音素级，本地 ONNX 推理，不需要联网 / 不需要 PyTorch）
# ------------------------------------------------------------------

# 音素识别模型成品目录：模型文件直接放在这一层，不再有 onnx\ 子目录和 .cache
PRON_MODEL_DIR = os.path.join(MODELS_DIR, "pronunciation")

# 音素识别模型：wav2vec2-xls-r-300m 在 TIMIT 数据集上微调，输出音素而非文字
# 说明：选它是因为它的音素表（38 个 IPA 音素）能和 cmudict 词典的音素一一对应
PRON_MODEL_REPO = "proclivitystudios/vitouphy-wav2vec2-xls-r-300m-timit-phoneme-ONNX"

# 优先使用 int8 量化版（CPU 推理更快、体积更小，约 300MB）
PRON_MODEL_FILE = "onnx/model_quantized.onnx"

# 量化版不可用时的备选（4bit 量化）
PRON_MODEL_FILE_FALLBACK = "onnx/model_q4.onnx"

# 扁平落点：下载后把 onnx\ 里的文件上移一层，做到「打开目录就能看见模型」
PRON_MODEL_PATH = os.path.join(PRON_MODEL_DIR, "model_quantized.onnx")
PRON_VOCAB_PATH = os.path.join(PRON_MODEL_DIR, "vocab.json")

# 自动确保目录存在
for _dir in (AUDIO_DIR, MODELS_DIR, WHISPER_MODEL_PATH, PRON_MODEL_DIR,
             PIP_CACHE_DIR, HF_HOME_DIR):
    os.makedirs(_dir, exist_ok=True)
