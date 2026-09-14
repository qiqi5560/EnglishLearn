# -*- coding: utf-8 -*-
"""
llm.py —— 大模型对话模块 (LLM)

作用：调用本地 Ollama 服务里的 qwen2.5:7b-instruct 模型，
配合"场景人设(system prompt)"，让 AI 扮演餐厅服务员、面试官等角色，
用英语和用户进行多轮对话练习。

贴心设计：
- 调用前会自动检测大模型服务是否在线；
- 【主机角色】若没在线，会自动用「模型目录=D:\Ollama\models」启动它，
  并监听 0.0.0.0:11434，让同一 WiFi / 本机热点下的组员也能连进来；
- 【组员角色】只需设置环境变量 LLM_SERVER_URL 指向组长电脑，
  代码不会去启动本机 Ollama（本机没模型，启动也没用），连不上时给出排查提示。
"""

import os
import re
import socket
import subprocess
import time
from urllib.parse import urlparse

import ollama

from config import (
    OLLAMA_HOST, OLLAMA_BIND, OLLAMA_MODELS_DIR, OLLAMA_EXE,
    LLM_MODEL, LLM_IS_REMOTE,
)


def _host_port(url=None):
    """从 http://ip:port 解析出 (ip, port)，供 TCP 探测用"""
    url = url or OLLAMA_HOST
    if "://" not in url:
        url = "http://" + url
    parsed = urlparse(url)
    return parsed.hostname or "127.0.0.1", parsed.port or 11434


def is_service_running():
    """通过 TCP 端口探测大模型服务是否可达（本机 / 局域网通用）"""
    host, port = _host_port()
    try:
        with socket.create_connection((host, port), timeout=2):
            return True
    except OSError:
        return False


def ensure_service():
    """
    确保大模型服务可用。

    - 组员角色(LLM_IS_REMOTE=True)：只检查能否连上组长电脑，连不上就抛错并给出
      排查清单，绝不启动本机 Ollama。
    - 主机角色：没运行就自动启动本机 Ollama，并监听 0.0.0.0，方便组员连进来。
    """
    if is_service_running():
        if LLM_IS_REMOTE:
            print(f"[LLM] 已连接远程大模型服务 ({OLLAMA_HOST})")
        else:
            print(f"[LLM] Ollama 服务已在运行 ({OLLAMA_HOST})")
        return

    if LLM_IS_REMOTE:
        raise RuntimeError(
            f"连不上远程大模型服务：{OLLAMA_HOST}\n"
            "请依次检查：\n"
            "  1) 组长电脑已启动 Ollama，且终端窗口保持打开；\n"
            "  2) 你和组长在同一个网络（同一 WiFi，或已连上组长开的热点）；\n"
            "  3) 组长电脑已放行防火墙 11434 端口（见 README 第 5 节）；\n"
            "  4) 环境变量 LLM_SERVER_URL 里的 IP 写对了（组长用 ipconfig 查询）。"
        )

    print(f"[LLM] 未检测到 Ollama 服务，正在自动启动（监听 {OLLAMA_BIND}，方便组员连接）...")
    env = dict(os.environ)
    env["OLLAMA_MODELS"] = OLLAMA_MODELS_DIR   # 让服务从 D 盘读取大模型
    env["OLLAMA_HOST"] = OLLAMA_BIND           # 关键：监听所有网卡，组员才连得上
    subprocess.Popen(
        [OLLAMA_EXE, "serve"],
        env=env,
        creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),  # 不弹黑窗
    )

    # 最多等待 20 秒，等服务端口起来
    for _ in range(40):
        if is_service_running():
            print(f"[LLM] Ollama 服务启动成功，监听 {OLLAMA_BIND}\n")
            return
        time.sleep(0.5)

    raise RuntimeError(
        "Ollama 服务启动失败。请手动双击项目里的 start_llm_server.bat "
        "（或 D:\\Ollama\\start_ollama.bat）后再试。"
    )


# AI 反馈行的前缀标记（支持多种写法，兼容不同 prompt 版本）
FEEDBACK_PREFIXES = ("(Fix)", "(Feedback)", "(Tip)")

# 用于定位反馈标记：不依赖换行，直接按"(Fix)/(Feedback)/(Tip)"出现的位置切分。
# 原因：7B 模型有时会把整份成绩报告输出成一整行（比如口语测试场景），
# 若只按换行拆分，报告会被误当成正文朗读出来、而文字提示区是空的。
_FEEDBACK_PATTERN = re.compile(r"\((?:Fix|Feedback|Tip)\)", re.IGNORECASE)


def split_feedback(text):
    """
    把 AI 的回复拆成「口语正文」和「文字提示」两部分。

    AI 会在对话正文之后给出以 (Fix) / (Feedback) / (Tip) 开头的纠错或建议；
    这些内容只用于文字展示，不应被 TTS 朗读出来。

    兼容两种排版，都能正确拆分：
    - 每个标记各占一行（常规陪练场景）
    - 多个标记挤在同一行（长报告常这样，例如口语测试的成绩报告）

    :param text: AI 的完整回复
    :return: (speech, tips)
        - speech: 用于朗读 / 对话展示的正文
        - tips:   文字提示列表（已去掉前缀标记）
    """
    segments = _FEEDBACK_PATTERN.split(text)
    # 第一个标记之前的内容是正文；其余每一段都是一条文字提示
    speech = " ".join(segments[0].split())
    tips = [" ".join(seg.split()) for seg in segments[1:] if seg.strip()]
    return speech, tips


class OllamaClient:
    """千问大模型对话客户端（多轮记忆，支持角色扮演）"""

    def __init__(self, model=LLM_MODEL):
        ensure_service()                      # 先保证服务在线
        self.client = ollama.Client(host=OLLAMA_HOST)
        self.model = model
        self.messages = []                    # 对话历史（让 AI 记住上下文）

    def set_system_prompt(self, prompt):
        """设定 AI 角色（人设）。prompt 是给 AI 看的英文扮演说明。"""
        self.messages = [{"role": "system", "content": prompt}]

    def reset(self):
        """清空对话历史，重新开始"""
        self.messages = []

    def chat(self, user_text):
        """
        发送一句话给 AI，返回 AI 的回复文本

        :param user_text: 用户说的英文内容
        :return: AI 的英文回复
        """
        self.messages.append({"role": "user", "content": user_text})
        resp = self.client.chat(model=self.model, messages=self.messages)
        ai_text = resp["message"]["content"].strip()
        self.messages.append({"role": "assistant", "content": ai_text})
        return ai_text


if __name__ == "__main__":
    # 简易自测：python llm.py
    client = OllamaClient()
    client.set_system_prompt(
        "You are a friendly English teacher. Reply in simple English."
    )
    print("AI:", client.chat("Hello! I want to practice my English with you."))
