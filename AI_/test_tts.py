# -*- coding: utf-8 -*-
"""
test_tts.py —— 英文语音合成(TTS)单模块测试

作用：验证 edge-tts 能否把英文文字合成为语音 mp3。
运行: python test_tts.py
成功标志: 电脑会弹出播放器，播放一段英文朗读（需联网，微软服务）。
"""

import sys

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

from tts import EdgeTTS  # noqa: E402


def main():
    print("=" * 50)
    print("  TTS 英文语音合成测试 (edge-tts, 需联网)")
    print("=" * 50)

    text = "Hello! Welcome to our restaurant. Would you like to see the menu?"
    print(f"要朗读的文字: {text}\n")

    tts = EdgeTTS()
    path = tts.speak(text, play=True)

    print(f"\n音频已生成: {path}")
    print("如果播放器里听到了流畅的英文女声，说明 TTS 正常！")


if __name__ == "__main__":
    main()
