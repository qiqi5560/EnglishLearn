# -*- coding: utf-8 -*-
"""
demo_chat.py —— 场景角色扮演【文字对话】演示

作用：体验完整"大模型场景扮演"核心玩法（只打字、不涉及语音）。
选择场景后，AI 会扮演角色用英文和你对话，并偶尔给出地道表达建议。

运行: python demo_chat.py
退出: 输入 exit 或 quit
"""

import sys

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

from llm import OllamaClient, split_feedback  # noqa: E402
from scene_prompts import SCENES, DEFAULT_SCENE  # noqa: E402


def choose_scene():
    """打印场景菜单让用户选择"""
    print("=" * 52)
    print("  AI 场景扮演 · 英语口语训练 (文字版)")
    print("=" * 52)
    print("请选择一个练习场景：")
    keys = list(SCENES.keys())
    for i, key in enumerate(keys, 1):
        print(f"  {i}. {SCENES[key]['name']}")
    print("  q. 退出")
    while True:
        try:
            choice = input("请输入序号: ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\n已退出。")
            return None
        if choice.lower() == "q":
            return None
        if choice.isdigit() and 1 <= int(choice) <= len(keys):
            return keys[int(choice) - 1]
        print("输入无效，请重新输入。")


def main():
    scene_key = choose_scene()
    if scene_key is None:
        print("已退出。")
        return

    scene = SCENES[scene_key]
    print(f"\n>>> 已进入场景：{scene['name']}")
    print(">>> 请输入英语内容与 AI 对话；输入 exit 退出。\n")

    client = OllamaClient()
    client.set_system_prompt(scene["system_prompt"])

    # 让 AI 先说一句开场白，营造真实感
    print("[AI] ", end="")
    print(client.chat("(start the conversation with a greeting)"))

    while True:
        try:
            user_text = input("\n[你] ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\n已退出。")
            break
        if not user_text:
            continue
        if user_text.lower() in ("exit", "quit"):
            print("再见！练口语要坚持哦。")
            break

        print("[AI] ", end="")
        try:
            answer = client.chat(user_text)
            speech, tips = split_feedback(answer)
            print(speech)
            for tip in tips:
                print(f"[提示] {tip}")
        except Exception as e:
            print(f"\n对话出错: {e}")
            print("请确认 Ollama 服务已启动（代码会自动启动，若失败请双击 D:\\Ollama\\start_ollama.bat）")


if __name__ == "__main__":
    main()
