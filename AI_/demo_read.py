# -*- coding: utf-8 -*-
"""
demo_read.py —— 跟读纠错模式（音素级发音评测）

和 demo_voice.py 的「自由对话」不同，这里是「跟读」：
系统给你一句地道英文，你照着读，程序告诉你哪个音读错了、怎么改。

使用方法:
    python demo_read.py
1) 选一句跟读素材（或直接输入任意英文句子）
2) 按提示录音，对着麦克风朗读
3) 查看音素级评分与纠错提示

首次运行会自动下载音素识别模型（约 300MB）到 D 盘显眼目录
models\pronunciation（模型文件直接可见），之后完全离线可用。
录音设备正常即可（笔记本自带麦克风就行）。
"""

import os
import sys
import wave

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

import sounddevice as sd                        # noqa: E402

from pronunciation import PronunciationAssessor, format_report   # noqa: E402

AUDIO_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "audio")

# 跟读素材：日常高频句，刻意覆盖中国学生常见的发音难点
READING_SENTENCES = [
    "I would like a cup of coffee, please.",        # v / f / 弱读
    "Thank you very much for your help.",           # θ / v
    "Could you tell me where the restroom is?",     # r / l 区分
    "I think this is a very good idea.",            # θ / ð / v
    "How much does it cost to go to the airport?",  # 连读与弱读
]


def record_wav(path, seconds):
    """用麦克风录音 seconds 秒，保存为 16kHz 单声道 wav"""
    sample_rate = 16000
    print(f"[录音] 请对着麦克风跟读，录音 {seconds} 秒开始...")
    audio = sd.rec(int(seconds * sample_rate), samplerate=sample_rate,
                   channels=1, dtype="int16")
    sd.wait()
    with wave.open(path, "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(sample_rate)
        wf.writeframes(audio.tobytes())
    print("[录音] 完成。")


def main():
    print("=" * 52)
    print("  跟读纠错模式 · 音素级发音评测")
    print("=" * 52)
    print("请选择跟读素材：")
    for i, s in enumerate(READING_SENTENCES, 1):
        print(f"  {i}. {s}")
    print("  （也可以直接输入任意一句英文）")

    print("\n正在准备音素级评测模型，首次运行需下载（约300MB），请稍候...")
    try:
        assessor = PronunciationAssessor()
    except Exception as e:
        print(f"\n[错误] 音素评测模型加载失败：{e}")
        print("提示：可先用 demo_voice.py 的「清晰度参考分」模式练习。")
        return

    while True:
        try:
            choice = input("\n请输入序号或句子(输入 0 退出): ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\n已退出。")
            break

        if choice == "0":
            print("再见！练口语要坚持哦。")
            break
        if choice.isdigit() and 1 <= int(choice) <= len(READING_SENTENCES):
            reference = READING_SENTENCES[int(choice) - 1]
        elif choice:
            reference = choice
        else:
            print("输入无效，请重新输入。")
            continue

        secs_raw = input("录音秒数(回车默认6秒，长句可调大): ").strip()
        secs = int(secs_raw) if secs_raw.isdigit() and int(secs_raw) > 0 else 6

        wav_path = os.path.join(AUDIO_DIR, "read_input.wav")
        try:
            record_wav(wav_path, secs)
            print("\n[评测] 正在分析你的发音...")
            result = assessor.assess(wav_path, reference)
            print(f"\n参考句子: {reference}")
            print(format_report(result))
        except Exception as e:
            print(f"本轮出错: {e}")


if __name__ == "__main__":
    main()
