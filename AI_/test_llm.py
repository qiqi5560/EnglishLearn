# -*- coding: utf-8 -*-
"""
test_llm.py —— 大模型(LLM)单模块测试

作用：验证"千问 qwen2.5:7b-instruct"本地模型能否正常对话。
运行: python test_llm.py
成功标志: 终端能看到 AI 的英文自我介绍/回复。
"""

import sys

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

from llm import OllamaClient  # noqa: E402


def main():
    print("=" * 50)
    print("  LLM 大模型对话测试（qwen2.5:7b-instruct 本地运行）")
    print("=" * 50)
    print("正在连接本机 Ollama 服务...")

    client = OllamaClient()
    client.set_system_prompt(
        "You are Sam, a friendly waiter in a New York cafe. "
        "Reply in simple English with 1-2 short sentences."
    )

    # 连续问两句，测试多轮记忆
    for question in [
        "Hello! Good evening. Can I have a table for two, please?",
        "Great! What do you recommend on the menu today?",
    ]:
        print(f"\n[你] {question}")
        print("[AI] ", end="")
        answer = client.chat(question)
        print(answer)

    print("\n" + "=" * 50)
    print("测试通过! 本地大模型对话正常。")
    print("=" * 50)


if __name__ == "__main__":
    main()
