# -*- coding: utf-8 -*-
"""
demo_voice.py —— AI 英语陪练【全语音】演示（核心成果）

完整跑通: 你说英文 → ASR转文字 → 千问角色扮演 → TTS英文回复 → 播放给你听

使用方法:
    python demo_voice.py
1) 选择场景
2) AI 会用英文打招呼(同时出声)
3) 按回车 → 开始录音(默认6秒，可输入秒数) → 对着麦克风说英语
4) 等 AI 思考并说出回复(会自动播放)

录音设备需要正常(笔记本自带麦克风即可)。
退出: 在录音秒数提示处输入 0。

注意: 首次运行会联网下载 whisper 模型到D盘; TTS 每次需要联网(微软服务)。
"""

import sys
import os
import time

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

os.environ.setdefault("PYTHONUNBUFFERED", "1")

import numpy as np          # noqa: E402
import sounddevice as sd    # noqa: E402
import wave                 # noqa: E402

from asr import WhisperTranscriber, build_pronunciation_tip     # noqa: E402
from llm import OllamaClient, split_feedback                    # noqa: E402
from tts import EdgeTTS                # noqa: E402
from scene_prompts import SCENES, DEFAULT_SCENE  # noqa: E402


def record_wav(path, seconds):
    """用麦克风录音 seconds 秒，保存为 16kHz 单声道 wav"""
    sample_rate = 16000
    print(f"[录音] 请对着麦克风说英语，录音 {seconds} 秒开始...")
    audio = sd.rec(int(seconds * sample_rate), samplerate=sample_rate,
                   channels=1, dtype="int16")
    sd.wait()
    with wave.open(path, "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(sample_rate)
        wf.writeframes(audio.tobytes())
    print("[录音] 完成。")


def choose_scene():
    print("=" * 52)
    print("  AI 场景扮演 · 英语口语训练 (全语音版)")
    print("=" * 52)
    print("请选择一个练习场景：")
    keys = list(SCENES.keys())
    for i, key in enumerate(keys, 1):
        print(f"  {i}. {SCENES[key]['name']}")
    while True:
        choice = input("请输入序号: ").strip()
        if choice.isdigit() and 1 <= int(choice) <= len(keys):
            return keys[int(choice) - 1]
        print("输入无效，请重新输入。")


def main():
    scene_key = choose_scene()
    scene = SCENES[scene_key]
    print(f"\n>>> 已进入场景：{scene['name']}")
    print(">>> 正在准备 AI 三件套 (识别/对话/发声)，首次需要一些时间...\n")

    # 1. 组装三个能力
    recognizer = WhisperTranscriber()
    tts = EdgeTTS()
    client = OllamaClient()
    client.set_system_prompt(scene["system_prompt"])

    # 2. AI 开场（同时播放语音）
    print("[AI] 正在开场...")
    opening = client.chat("(start the conversation with a greeting)")
    print(f"[AI] {opening}")
    tts.speak(opening, play=True)

    # 3. 对话循环
    user_wav = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                            "audio", "user_input.wav")
    while True:
        try:
            inp = input("\n按回车开始录音(输入 0 退出; 输入数字可改秒数,默认6): ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\n已退出。")
            break
        if inp == "0":
            print("再见！练口语要坚持哦。")
            break
        if inp.isdigit():
            secs = int(inp)
        else:
            secs = 6

        try:
            record_wav(user_wav, secs)

            print("[1/3] 语音转文字中...")
            user_text, detail = recognizer.transcribe_with_score(user_wav)
            print(f"你说的是: {user_text}")
            if not user_text:
                print("没有识别到内容，请再说一次。")
                continue
            # 发音反馈：清晰度参考分 + 发音改进建议（本地生成，不朗读）
            print(f"[发音清晰度] {detail['score']}/100 （置信度参考分）")
            print(f"[发音建议] {build_pronunciation_tip(detail)}")

            print("[2/3] AI 思考中...")
            ai_text = client.chat(user_text)
            speech, tips = split_feedback(ai_text)
            print(f"[AI] {speech}")
            for tip in tips:
                print(f"[提示] {tip}")
            if not speech and not tips:   # 兜底：完全没解析出内容时，直接朗读原文
                speech = ai_text

            print("[3/3] AI 发声...")
            if speech:
                tts.speak(speech, play=True)
            else:
                # 口语测试场景：正文只有成绩报告（已拆到上面按文字显示），无需朗读
                print("(本轮没有需要朗读的内容，成绩报告见上方文字)")
        except Exception as e:
            print(f"本轮出错: {e}")
            time.sleep(1)


if __name__ == "__main__":
    main()
