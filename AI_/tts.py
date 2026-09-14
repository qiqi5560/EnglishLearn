# -*- coding: utf-8 -*-
"""
tts.py —— 英文语音合成模块 (TTS / Text To Speech)

作用：把一段英文文字，合成为自然的英文语音 mp3（微软 edge-tts 免费服务）。
例如 AI 回复 "Welcome to our restaurant!"  →  生成一段英文朗读的 mp3。

注意：
- edge-tts 调用微软在线语音服务，使用时【需要联网】；
- 生成的文件保存在 D:\英语练习\AI\audio\ 目录下（不占用 C 盘）。
"""

import asyncio
import os
import time

import edge_tts

from config import AUDIO_DIR, TTS_VOICE


class EdgeTTS:
    """英文语音合成器"""

    def __init__(self, voice=TTS_VOICE):
        self.voice = voice

    async def _save_async(self, text: str, out_path: str):
        communicate = edge_tts.Communicate(text, self.voice)
        await communicate.save(out_path)

    def speak_to_file(self, text: str, filename: str = None) -> str:
        """
        把英文文字合成 mp3 并保存到 audio 目录

        :param text: 要朗读的英文文字
        :param filename: 可选自定义文件名；默认自动取名
        :return: 生成文件的完整路径
        """
        os.makedirs(AUDIO_DIR, exist_ok=True)
        if filename is None:
            filename = f"tts_{time.strftime('%Y%m%d_%H%M%S')}.mp3"
        out_path = os.path.join(AUDIO_DIR, filename)
        asyncio.run(self._save_async(text, out_path))
        return out_path

    def speak(self, text: str, play: bool = True) -> str:
        """
        合成语音，并可立即用系统默认播放器试听

        :param text: 要朗读的英文
        :param play: True=自动打开系统播放器试听
        :return: mp3 文件路径
        """
        if not text.strip():
            raise ValueError("要朗读的文本不能为空")
        path = self.speak_to_file(text)
        print(f"[TTS] 语音已生成: {path}")
        if play and os.path.exists(path):
            os.startfile(path)   # Windows 打开默认播放器播放
        return path


if __name__ == "__main__":
    # 简易自测：python tts.py
    tts = EdgeTTS()
    tts.speak("Hello! Welcome to our restaurant. Would you like to see the menu?")
    print("测试完成，请听播放器里的英文发音。")
