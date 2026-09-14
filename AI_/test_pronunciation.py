# -*- coding: utf-8 -*-
"""
test_pronunciation.py —— 音素级发音评测自测

作用：验证「音素级发音评测」链路是否可用。
运行方式(两种)：
  1) python test_pronunciation.py
     自动用 edge-tts 生成一句标准英文当作「完美跟读」，再评测（全自动闭环，需联网）
  2) python test_pronunciation.py 某个.wav "参考句子"
     用自己的录音测试，能看到真实的发音问题

成功标志：标准 TTS 语音应拿到接近满分的成绩，并逐词显示音素判定。

注意：第一次运行会自动从 HuggingFace 镜像下载音素识别模型（约 300MB）
      到 D:\英语练习\AI\models\pronunciation（模型文件直接放在该目录下，
      没有 onnx 子目录和隐藏的 .cache），之后完全离线可用。
"""

import sys
import os

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

from pronunciation import PronunciationAssessor, format_report   # noqa: E402


def main():
    print("=" * 52)
    print("  音素级发音评测测试 (wav2vec2 + cmudict, 本地 ONNX)")
    print("=" * 52)

    # 1. 决定测试音频：有参数用参数，否则用 TTS 现场合成一句标准英文
    if len(sys.argv) >= 3:
        audio_path, reference = sys.argv[1], sys.argv[2]
    else:
        from tts import EdgeTTS

        reference = "I would like a cup of coffee, please."
        print(f"先生成一句标准英文作为跟读样例:\n  {reference}\n")
        audio_path = EdgeTTS().speak_to_file(reference,
                                             filename="pron_test_sample.mp3")

    if not os.path.exists(audio_path):
        print(f"错误: 找不到音频文件 {audio_path}")
        sys.exit(1)

    print(f"测试音频: {audio_path}")
    print("首次运行会自动下载音素识别模型(约300MB)，请稍候...\n")

    # 2. 评测
    assessor = PronunciationAssessor()
    result = assessor.assess(audio_path, reference)

    print(f"参考句子: {reference}")
    print(format_report(result))
    print("提示: 上面用的是标准 TTS 语音，分数应接近满分；")
    print("      换成你自己的录音，就能看到真实的发音问题了。")
    print("发音评测测试完成！")


if __name__ == "__main__":
    main()
