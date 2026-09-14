# -*- coding: utf-8 -*-
"""
asr.py —— 语音转文字模块 (ASR / Automatic Speech Recognition)

作用：把录音文件里的英文语音，转成英文文字。
例如你对着麦克风说 "I'd like a coffee."  →  得到文字 "I'd like a coffee."

硬件自适应（组长要求的功能）：
- 自动检测电脑是否有 NVIDIA GPU（CUDA）
  - 有 → device="cuda"，用 GPU 推理（更快）
  - 无 → device="cpu"，用 CPU 推理（当前这台电脑无独显，会走这里）

模型存放（统一在 D 盘的显眼位置，像 D:\Ollama 一样一眼可见）：
- 模型成品目录：D:\英语练习\AI\models\whisper-small\
  里面直接就是 model.bin / config.json / tokenizer.json / vocabulary.txt，
  打开目录就能看见模型本体，不再有哈希子目录和隐藏的 .cache。
- 第一次使用会自动联网下载（small 档约 460MB），下载完自动整理成上面的
  扁平结构，之后离线即可用。
"""

import os
import shutil

# 必须【最先】导入 config：它负责设置 HF 镜像与缓存目录等环境变量，
# 这些变量必须在 huggingface_hub / faster_whisper 被导入之前就位。
from config import (
    WHISPER_MODEL,
    WHISPER_MODEL_FILE,
    WHISPER_MODEL_PATH,
    WHISPER_MODEL_PATTERNS,
    WHISPER_MODEL_REPO,
    ASR_LANGUAGE,
)

import ctranslate2
from faster_whisper import WhisperModel

# 单词级置信度阈值：低于该值的单词视为「可能没被听清」（越低越严格，减少误报）
WEAK_WORD_PROB = 0.5


def detect_device():
    """
    自动检测计算设备（GPU 检测接口）

    返回: (device, compute_type)
    - 检测到 NVIDIA GPU  -> ("cuda", "float16")  用显卡加速
    - 未检测到 GPU      -> ("cpu", "int8")       退回 CPU 运行
    """
    try:
        if ctranslate2.get_cuda_device_count() > 0:
            print("[设备检测] 检测到 NVIDIA GPU -> 使用 GPU (CUDA) 加速")
            return "cuda", "float16"
    except Exception:
        pass
    print("[设备检测] 未检测到可用 GPU -> 自动使用 CPU 运行")
    return "cpu", "int8"


def ensure_whisper_model():
    """
    确保语音识别模型已经落在「显眼」的扁平目录里（缺失才下载）。

    目录形如：D:\英语练习\AI\models\whisper-small\
        model.bin  config.json  tokenizer.json  vocabulary.txt
    这正是 CTranslate2 可直接加载的模型目录，所以不用再走 HF 的哈希缓存。

    :return: 模型成品目录路径
    """
    model_file = os.path.join(WHISPER_MODEL_PATH, WHISPER_MODEL_FILE)
    if os.path.exists(model_file):
        print(f"[ASR] 使用本地模型目录：{WHISPER_MODEL_PATH}")
        return WHISPER_MODEL_PATH

    from huggingface_hub import snapshot_download

    print(f"[ASR] 首次使用，正在下载语音识别模型 {WHISPER_MODEL}（约 460MB）...")
    print(f"[ASR] 下载目标目录：{WHISPER_MODEL_PATH}")
    snapshot_download(
        repo_id=WHISPER_MODEL_REPO,
        local_dir=WHISPER_MODEL_PATH,
        allow_patterns=WHISPER_MODEL_PATTERNS,
    )
    # local_dir 模式会额外生成一个只放元数据的隐藏 .cache，
    # 删掉它，目录才是真正干净、一眼可见的
    shutil.rmtree(os.path.join(WHISPER_MODEL_PATH, ".cache"), ignore_errors=True)
    print("[ASR] 模型已就位\n")
    return WHISPER_MODEL_PATH


def compute_clarity_score(segments_info):
    """
    发音清晰度参考分（0-100）

    原理：whisper 识别每个语音片段时，会给一个 avg_logprob（平均对数概率），
    表示"模型对这段识别结果有多确定"。说得越清楚，模型越确定，该值越接近 0；
    发音含糊 / 噪声大时，该值会明显偏低（通常落在 [-1, 0] 之间）。

    映射：score = (1 + avg_logprob) * 100，再截断到 [0, 100]；
    多个片段按「时长」加权平均，避免短片段权重过大。

    注意：这是基于识别置信度的【参考分】，会受语速、背景噪声、麦克风质量影响，
    并非严格的发音评测（发音评测需要音素级对齐，属于后续可扩展方向）。

    :param segments_info: 片段信息列表，每项含 start/end/avg_logprob/no_speech_prob
    :return: (score, avg_logprob, no_speech_prob, note)
    """
    if not segments_info:
        return 0, None, None, "未检测到有效语音"

    total_w = 0.0
    weighted_logprob = 0.0
    weighted_no_speech = 0.0
    for s in segments_info:
        w = max(s["end"] - s["start"], 0.01)   # 权重=片段时长，最小 0.01 秒防止除零
        total_w += w
        weighted_logprob += s["avg_logprob"] * w
        weighted_no_speech += s["no_speech_prob"] * w

    avg_logprob = weighted_logprob / total_w
    no_speech_prob = weighted_no_speech / total_w
    score = round(max(0.0, min(1.0, 1.0 + avg_logprob)) * 100)

    note = "疑似静音或噪声，本分数仅供参考" if no_speech_prob > 0.5 else ""
    return score, round(avg_logprob, 4), round(no_speech_prob, 4), note


def build_pronunciation_tip(detail):
    """
    根据识别结果生成「发音 / 清晰度」反馈。

    说明：本反馈基于 whisper 的识别置信度，反映的是「说得多清楚、多容易被识别」，
    会受语速、口音、背景噪声、麦克风质量影响，并非音素级的发音纠错。

    :param detail: transcribe_with_score 返回的 detail 字典
    :return: 反馈文字（中文，用于终端提示，不参与 TTS 朗读）
    """
    score = detail.get("score", 0)
    weak = detail.get("weak_words", [])

    if score >= 85:
        tip = "整体很清晰，继续保持。"
    elif score >= 65:
        tip = "整体清晰，个别词再读重一点会更准。"
    elif score >= 45:
        tip = "部分内容识别得不太确定。"
    else:
        tip = "这段识别得比较吃力，可能是语速偏快或环境偏吵。"

    # 只在确实有词没被听清时才点名，避免无差别地劝人「靠近麦克风」
    if weak:
        tip += " 这几个词没被听清：" + "、".join(weak[:5]) + "，可以单独再读一遍。"
    if detail.get("note"):
        tip += f"（{detail['note']}）"
    return tip


class WhisperTranscriber:
    """语音转文字器：把音频文件转成英文文本"""

    def __init__(self, model_size=WHISPER_MODEL, language=ASR_LANGUAGE):
        device, compute_type = detect_device()
        print(f"[ASR] 正在加载语音识别模型 {model_size}（模型目录: {WHISPER_MODEL_PATH}）...")
        self.model = WhisperModel(
            ensure_whisper_model(),   # 关键：直接加载 D 盘显眼目录里的模型成品
            device=device,
            compute_type=compute_type,
        )
        self.language = language
        print("[ASR] 语音识别模型加载完成，可以开始转写\n")

    def transcribe(self, audio_path):
        """
        识别一段音频，返回文字（保持简单接口，兼容原有调用）

        :param audio_path: 音频文件路径（wav/mp3/m4a 等均可）
        :return: 识别出的英文字符串
        """
        text, _ = self.transcribe_with_score(audio_path)
        return text

    def transcribe_with_score(self, audio_path):
        """
        识别一段音频，并额外给出「发音清晰度参考分」和单词级发音线索

        :param audio_path: 音频文件路径（wav/mp3/m4a 等均可）
        :return: (text, detail)
            - text:   识别出的英文字符串
            - detail: dict，包含
                score           0-100 清晰度参考分（非严格发音评测，仅供参考）
                avg_logprob     加权平均对数概率（越接近 0 越清晰）
                no_speech_prob  加权平均静音概率（越高越可能是静音/噪声）
                note            文字提示（如无有效语音、疑似噪声）
                segments        每个语音片段的分项信息
                weak_words      置信度偏低的单词（可能发音不够清楚）
        """
        # word_timestamps=True 让每个片段附带「单词级」置信度，用于定位发音不清的词
        segments, _ = self.model.transcribe(
            audio_path, language=self.language, word_timestamps=True
        )

        raw_texts = []      # 保留原始文本用于拼接，避免丢失单词间空格
        seg_list = []
        weak_words = []
        for seg in segments:
            raw_texts.append(seg.text)
            seg_list.append({
                "text": seg.text.strip(),
                "start": round(seg.start, 2),
                "end": round(seg.end, 2),
                "avg_logprob": round(seg.avg_logprob, 4),
                "no_speech_prob": round(seg.no_speech_prob, 4),
            })
            for w in (getattr(seg, "words", None) or []):
                word = w.word.strip()
                if word and w.probability < WEAK_WORD_PROB and word not in weak_words:
                    weak_words.append(word)
        text = "".join(raw_texts).strip()

        score, avg_logprob, no_speech_prob, note = compute_clarity_score(seg_list)
        return text, {
            "score": score,
            "avg_logprob": avg_logprob,
            "no_speech_prob": no_speech_prob,
            "note": note,
            "segments": seg_list,
            "weak_words": weak_words,
        }


if __name__ == "__main__":
    # 简易自测：python asr.py 音频文件路径
    import sys

    if len(sys.argv) < 2:
        print("用法: python asr.py <音频文件路径>")
        sys.exit(1)
    recognizer = WhisperTranscriber()
    text, detail = recognizer.transcribe_with_score(sys.argv[1])
    print("识别结果：", text)
    print(f"发音清晰度参考分：{detail['score']}/100")
