# -*- coding: utf-8 -*-
"""
test_asr.py —— 语音转文字(ASR)单模块测试

作用：验证 faster-whisper 能否把英文语音转成文字。
运行方式(两种)：
  1) python test_asr.py                # 自动先让AI念一句英文→再转写(全自动闭环测试, 需联网生成样例)
  2) python test_asr.py 某个.wav文件    # 用自己的音频测试

成功标志: 终端打印出的识别文本与原文基本一致。

注意: 第一次运行会自动联网下载 whisper 语音模型(small 约460MB)
      到 D:\英语练习\AI\models\whisper-small (D盘显眼目录, 模型文件直接可见),
      之后无需再下载。
"""

import sys
import os

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

from asr import WhisperTranscriber  # noqa: E402


def main():
    print("=" * 50)
    print("  ASR 语音转文字测试 (faster-whisper)")
    print("=" * 50)

    # 1. 决定测试音频: 有参数用参数, 没参数则用 edge-tts 现场合成一句英文
    if len(sys.argv) >= 2:
        audio_path = sys.argv[1]
        expected = "(你提供的音频，无法预知原文)"
    else:
        from tts import EdgeTTS

        sample = ("Welcome to the Sunshine Cafe. "
                  "Can I get you something to drink?")
        print(f"先生成一句英文样例用于测试: {sample}\n")
        audio_path = EdgeTTS().speak_to_file(sample, filename="asr_test_sample.mp3")
        expected = sample

    # 2. 检查文件
    if not os.path.exists(audio_path):
        print(f"错误: 找不到音频文件 {audio_path}")
        sys.exit(1)
    print(f"测试音频: {audio_path}\n")

    # 3. 加载模型并转写（首次会自动下载模型到 D 盘 models/whisper-small）
    recognizer = WhisperTranscriber()
    print("正在转写，请稍候...\n")
    result, detail = recognizer.transcribe_with_score(audio_path)

    print("-" * 50)
    print("识别结果:", result)
    print("参考原文:", expected)
    print(f"发音清晰度参考分: {detail['score']}/100 (平均对数概率 {detail['avg_logprob']})")
    if detail.get("weak_words"):
        print(f"可能不够清楚的词: {', '.join(detail['weak_words'])}")
    print("-" * 50)
    print("ASR 测试完成！")


if __name__ == "__main__":
    main()
