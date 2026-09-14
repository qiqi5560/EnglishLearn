# -*- coding: utf-8 -*-
"""
pronunciation.py —— 音素级发音评测模块（本地 / 免费 / 离线）

作用：为「跟读练习」提供音素级打分与纠错，回答「哪个音读错了、错在哪」。

工作流程：
  1) G2P（字素→音素）：用 cmudict 词典把参考句子转成「目标音素序列」
  2) 声学识别：用本地 wav2vec2 音素模型（ONNX 推理）把录音识别成「实际音素序列」
  3) 对齐：两条音素序列做编辑距离对齐，逐个判定 读对 / 读错 / 漏读 / 多读
  4) 聚合：得到单词级与整句分数（准确度 / 完整度 / 流利度）以及错误清单

为什么必须做到音素级：
  ASR（whisper）的输出是「标准单词」，发音细节在识别过程中会被归一化掉，
  只能评语法用词，评不出发音；只有音素级声学模型才能发现
  「把 think 的 θ 读成了 s」这类真实的发音错误。

技术选型（全部本地、离线、无需 PyTorch）：
  - 声学模型：wav2vec2-xls-r-300m 在 TIMIT 上微调的音素识别模型（ONNX 量化版）
  - 推理引擎：onnxruntime（CPU 即可，复用项目已有依赖）
  - G2P 词典：cmudict（ARPAbet 音素集）

模型存放（统一在 D 盘的显眼位置，像 D:\Ollama 一样一眼可见）：
- 模型成品目录：D:\英语练习\AI\models\pronunciation\
  里面直接就是 model_quantized.onnx 和 vocab.json，打开目录就能看见模型本体，
  不再有 onnx\ 子目录和隐藏的 .cache。
- 首次运行会自动从 HuggingFace 镜像下载（约 300MB）并整理成上面的扁平结构，
  之后完全离线可用。
"""

import json
import os
import re
import shutil

# 必须【最先】导入 config：它负责设置 HF 镜像与缓存目录等环境变量，
# 这些变量必须在 huggingface_hub 被导入之前就位。
from config import (                   # noqa: E402
    PRON_MODEL_DIR,
    PRON_MODEL_REPO,
    PRON_MODEL_FILE,
    PRON_MODEL_FILE_FALLBACK,
    PRON_MODEL_PATH,
    PRON_VOCAB_PATH,
)

import numpy as np                     # noqa: E402
import onnxruntime as ort              # noqa: E402
from huggingface_hub import hf_hub_download   # noqa: E402

# 音频采样率（模型要求 16kHz 单声道）
SAMPLING_RATE = 16000
# wav2vec2 的卷积层把音频下采样 320 倍，因此每帧约 20ms
FRAME_STRIDE = 0.02

# ==================================================================
# 1. G2P：英文文本 → 目标音素序列
# ==================================================================

# ARPAbet（cmudict 使用的音素集）→ 声学模型输出的音素集
# 说明：模型音素表共 38 个，没有 ɔ(AO) 与 ʒ(ZH)，这里做近似归并
ARPABET_TO_MODEL = {
    "AA": "ɑ", "AE": "æ", "AH": "ə", "AO": "ɑ", "AW": "aʊ", "AY": "aɪ",
    "B": "b", "CH": "ʧ", "D": "d", "DH": "ð", "EH": "ɛ", "ER": "ɝ",
    "EY": "eɪ", "F": "f", "G": "g", "HH": "h", "IH": "ɪ", "IY": "i",
    "JH": "ʤ", "K": "k", "L": "l", "M": "m", "N": "n", "NG": "ŋ",
    "OW": "oʊ", "OY": "ɔɪ", "P": "p", "R": "ɹ", "S": "s", "SH": "ʃ",
    "T": "t", "TH": "θ", "UH": "ʊ", "UW": "u", "V": "v", "W": "w",
    "Y": "j", "Z": "z", "ZH": "ʃ",
}

# 常见音素的口语化提示，用于告诉用户「这个音该怎么发」
PHONEME_HINT = {
    "θ": "咬舌音 th（think）", "ð": "咬舌浊音 th（this）",
    "ɹ": "r 音（舌尖别碰到上颚）", "l": "l 音（舌尖抵上齿龈）",
    "v": "v 音（上齿轻咬下唇）", "w": "w 音（要圆唇）",
    "ʃ": "sh 音", "ʧ": "ch 音", "ʤ": "j 音", "ŋ": "ng 鼻音",
    "æ": "梅花音（cat）", "ə": "弱读的 schwa 音", "ɪ": "短 i 音（sit）",
    "i": "长 i 音（see）", "ʊ": "短 u 音（book）", "u": "长 u 音（food）",
    "ɑ": "a 音（father）", "ɛ": "短 e 音（bed）", "ɝ": "卷舌 er 音（bird）",
    "aɪ": "ai 音（my）", "aʊ": "au 音（now）", "eɪ": "ei 音（day）",
    "oʊ": "o 音（go）", "ɔɪ": "oi 音（boy）", "ɾ": "闪音（美式 water 的 t）",
}

_CMU = None


def _load_cmudict():
    """惰性加载 cmudict 发音词典（首次加载约 1 秒）"""
    global _CMU
    if _CMU is None:
        try:
            import cmudict
            _CMU = cmudict.dict()
        except Exception as e:      # pragma: no cover
            raise RuntimeError(
                "缺少发音词典依赖，请先执行: pip install cmudict"
            ) from e
    return _CMU


def _clean_word(raw):
    """去掉标点、转小写，得到用于查词典的单词"""
    return re.sub(r"[^a-zA-Z']", "", raw).strip("'").lower()


def text_to_phonemes(text):
    """
    把英文句子转成「目标音素序列」

    :param text: 英文句子（跟读素材）
    :return: (flat_phonemes, words, skipped)
        - flat_phonemes: 展平后的目标音素列表
        - words: [{"word": "hello", "start": 0, "end": 4}, ...]
                 记录每个单词在音素序列中占用的区间，便于回填单词级分数
        - skipped: 词典里查不到、被跳过的单词
    """
    cmu = _load_cmudict()
    flat, words, skipped = [], [], []

    for raw in text.split():
        word = _clean_word(raw)
        if not word:
            continue
        entries = cmu.get(word)
        if not entries:
            skipped.append(word)
            continue

        phonemes = []
        for p in entries[0]:                       # 多发音时取第一个
            key = re.sub(r"\d", "", p).upper()     # 去掉重音数字（AH0 → AH）
            mapped = ARPABET_TO_MODEL.get(key)
            if mapped:
                phonemes.append(mapped)
        if not phonemes:
            skipped.append(word)
            continue

        words.append({"word": word,
                      "start": len(flat),
                      "end": len(flat) + len(phonemes)})
        flat.extend(phonemes)

    return flat, words, skipped


# ==================================================================
# 2. 声学识别：录音 → 实际音素序列
# ==================================================================

def _softmax(x):
    x = x - x.max(axis=-1, keepdims=True)
    e = np.exp(x)
    return e / e.sum(axis=-1, keepdims=True)


def _prune_empty_dirs(root):
    """删除 root 下的空子目录（例如下载后剩下的 onnx\），保持目录扁平干净"""
    for dirpath, _, _ in os.walk(root, topdown=False):
        if dirpath != root and not os.listdir(dirpath):
            try:
                os.rmdir(dirpath)
            except OSError:
                pass


def _download_flat(repo, filename, target_path):
    """
    从 HuggingFace 下载单个文件，并「扁平」地落到 target_path。

    背景：hf_hub_download(..., local_dir=...) 会保留仓库内的子目录（如 onnx/），
    还会在 local_dir 下生成隐藏的 .cache。这里下载后把文件搬到目标位置，
    再清理掉 .cache 和空的子目录，保证目录一眼可见、没有多余层级。

    :return: 落地后的文件路径（= target_path）
    """
    os.makedirs(os.path.dirname(target_path), exist_ok=True)
    downloaded = hf_hub_download(repo_id=repo, filename=filename,
                                 local_dir=PRON_MODEL_DIR)
    if os.path.abspath(downloaded) != os.path.abspath(target_path):
        shutil.move(downloaded, target_path)
    shutil.rmtree(os.path.join(PRON_MODEL_DIR, ".cache"), ignore_errors=True)
    _prune_empty_dirs(PRON_MODEL_DIR)
    return target_path


class PhonemeRecognizer:
    """音素识别器：把录音识别成音素序列（本地 ONNX 推理，CPU 即可）"""

    # 候选模型文件：(仓库内路径, 扁平落点)
    _MODEL_CANDIDATES = (
        (PRON_MODEL_FILE, PRON_MODEL_PATH),
        (PRON_MODEL_FILE_FALLBACK,
         os.path.join(PRON_MODEL_DIR, os.path.basename(PRON_MODEL_FILE_FALLBACK))),
    )

    def __init__(self, repo=PRON_MODEL_REPO):
        model_path = self._ensure_model(repo)
        vocab_path = self._ensure_vocab(repo)

        with open(vocab_path, "r", encoding="utf-8") as f:
            vocab = json.load(f)                   # {音素: id}
        self.id2phoneme = {int(v): k for k, v in vocab.items()}
        self.blank_id = int(vocab.get("[PAD]", 0))  # CTC 的空白符号
        # 不参与比对的特殊符号
        self._ignore = {"[PAD]", "[UNK]", "|", " "}

        print(f"[发音评测] 正在加载音素识别模型（{os.path.basename(model_path)}）...")
        self.session = ort.InferenceSession(
            model_path, providers=["CPUExecutionProvider"]
        )
        self.input_names = [i.name for i in self.session.get_inputs()]
        print("[发音评测] 音素识别模型加载完成\n")

    @classmethod
    def _ensure_model(cls, repo):
        """确保 ONNX 模型文件已扁平落在 models\\pronunciation（缺失才下载）"""
        for _, target in cls._MODEL_CANDIDATES:
            if os.path.exists(target):
                print(f"[发音评测] 使用本地音素模型：{target}")
                return target

        last_err = None
        for filename, target in cls._MODEL_CANDIDATES:
            try:
                print(f"[发音评测] 首次使用，正在下载音素模型文件：{filename}")
                return _download_flat(repo, filename, target)
            except Exception as e:      # 某个量化版本不可用时自动换下一个
                last_err = e
        raise RuntimeError(f"音素识别模型下载失败: {last_err}")

    @staticmethod
    def _ensure_vocab(repo):
        """确保 vocab.json 已扁平落在 models\\pronunciation（缺失才下载）"""
        if os.path.exists(PRON_VOCAB_PATH):
            return PRON_VOCAB_PATH
        print("[发音评测] 首次使用，正在下载音素模型词表：vocab.json")
        return _download_flat(repo, "vocab.json", PRON_VOCAB_PATH)

    @staticmethod
    def _load_audio(wav_path):
        """解码音频 → 16kHz 单声道 float32，并做零均值单位方差归一化"""
        # 复用 faster-whisper 的解码器，避免再引入音频处理依赖
        from faster_whisper.audio import decode_audio
        audio = np.asarray(decode_audio(wav_path, sampling_rate=SAMPLING_RATE),
                           dtype=np.float32)
        if audio.size == 0:
            return audio
        return (audio - audio.mean()) / np.sqrt(audio.var() + 1e-7)

    def recognize(self, wav_path):
        """
        识别录音中的音素序列

        :param wav_path: 音频路径（wav/mp3/m4a 均可，内部统一解码为 16kHz）
        :return: (phonemes, duration)
            - phonemes: [{"phoneme": "θ", "start": 0.12, "end": 0.14, "conf": 0.93}, ...]
            - duration: 音频时长（秒），用于计算流利度
        """
        waveform = self._load_audio(wav_path)
        duration = round(len(waveform) / SAMPLING_RATE, 3)
        if waveform.size < 400:                    # 太短，直接认为没有有效语音
            return [], duration

        feed = {}
        for name in self.input_names:
            if "mask" in name.lower():
                feed[name] = np.ones((1, waveform.shape[0]), dtype=np.bool_)
            else:
                feed[name] = waveform[None, :].astype(np.float32)

        logits = self.session.run(None, feed)[0][0]      # [帧数, 音素数]
        probs = _softmax(logits)
        ids = probs.argmax(axis=-1)
        confs = probs.max(axis=-1)

        # CTC 贪心解码：先合并连续重复帧，再去掉空白符号
        phonemes = []
        prev = -1
        for t, idx in enumerate(ids):
            idx = int(idx)
            if idx != prev and idx != self.blank_id:
                ph = self.id2phoneme.get(idx)
                if ph and ph not in self._ignore:
                    phonemes.append({
                        "phoneme": ph,
                        "start": round(t * FRAME_STRIDE, 3),
                        "end": round((t + 1) * FRAME_STRIDE, 3),
                        "conf": round(float(confs[t]), 3),
                    })
            prev = idx
        return phonemes, duration


# ==================================================================
# 3. 对齐：目标音素 vs 实际音素
# ==================================================================

def align_phonemes(target, hyp):
    """
    用编辑距离（Levenshtein）对齐两条音素序列

    :param target: 目标音素列表（参考句子的标准发音）
    :param hyp:    实际音素列表（识别出来的发音）
    :return: [(op, i, j)]，op ∈ {match, sub, del, ins}
        - match: target[i] == hyp[j]   读对了
        - sub:   读错了（把 target[i] 读成了 hyp[j]）
        - del:   漏读（target[i] 没读出来）
        - ins:   多读（凭空多了 hyp[j]）
    """
    n, m = len(target), len(hyp)
    dp = np.zeros((n + 1, m + 1), dtype=np.int32)
    dp[:, 0] = np.arange(n + 1)
    dp[0, :] = np.arange(m + 1)

    for i in range(1, n + 1):
        for j in range(1, m + 1):
            cost = 0 if target[i - 1] == hyp[j - 1] else 1
            dp[i][j] = min(dp[i - 1][j] + 1,          # 漏读
                           dp[i][j - 1] + 1,          # 多读
                           dp[i - 1][j - 1] + cost)   # 读对 / 读错

    ops = []
    i, j = n, m
    while i > 0 or j > 0:
        if i > 0 and j > 0:
            cost = 0 if target[i - 1] == hyp[j - 1] else 1
            if dp[i][j] == dp[i - 1][j - 1] + cost:
                ops.append(("match" if cost == 0 else "sub", i - 1, j - 1))
                i, j = i - 1, j - 1
                continue
        if i > 0 and dp[i][j] == dp[i - 1][j] + 1:
            ops.append(("del", i - 1, -1))
            i -= 1
        else:
            ops.append(("ins", -1, j - 1))
            j -= 1
    ops.reverse()
    return ops


def _clamp(v, low=0.0, high=100.0):
    return max(low, min(high, v))


# ==================================================================
# 4. 对外接口：跟读打分
# ==================================================================

class PronunciationAssessor:
    """跟读发音评测器：输入「录音 + 参考文本」，输出音素级评分与纠错"""

    def __init__(self):
        self.recognizer = PhonemeRecognizer()

    def assess(self, wav_path, reference_text):
        """
        对一次跟读打分

        :param wav_path:       用户跟读的录音路径
        :param reference_text: 跟读的参考句子
        :return: dict
            overall       总分 0-100
            accuracy      准确度（读对的音素比例）
            completeness  完整度（读出来的音素比例）
            fluency       流利度（语速是否接近母语者）
            target        目标音素序列
            actual        识别到的音素序列
            words         单词级结果 [{word, score, phonemes:[{ph, status, hint}]}]
            errors        发音错误清单 [{type, expected, got, word, hint}]
            skipped       词典中查不到、未参与评测的单词
        """
        target, words, skipped = text_to_phonemes(reference_text)
        if not target:
            return {
                "overall": 0, "accuracy": 0, "completeness": 0, "fluency": 0,
                "target": [], "actual": [], "words": [], "errors": [],
                "skipped": skipped,
                "message": "参考句子中没有可评测的单词（不在发音词典中）",
            }

        hyp_info, duration = self.recognizer.recognize(wav_path)
        hyp = [h["phoneme"] for h in hyp_info]

        if not hyp:
            return {
                "overall": 0, "accuracy": 0, "completeness": 0, "fluency": 0,
                "target": target, "actual": [], "words": [], "errors": [],
                "skipped": skipped,
                "message": "没有识别到有效语音，请靠近麦克风再读一次",
            }

        ops = align_phonemes(target, hyp)

        # 建立「音素下标 → 所属单词」映射，便于把错误定位到具体单词
        phoneme_to_word = {}
        for w in words:
            for idx in range(w["start"], w["end"]):
                phoneme_to_word[idx] = w["word"]

        # ---- 逐音素状态 ----
        status = [None] * len(target)       # match / sub / del
        errors = []
        for op, i, j in ops:
            if op == "ins":
                errors.append({"type": "ins", "expected": "-", "got": hyp[j],
                               "word": "", "hint": "多读了一个音"})
                continue
            status[i] = op
            if op == "sub":
                errors.append({"type": "sub", "expected": target[i], "got": hyp[j],
                               "word": phoneme_to_word.get(i, ""),
                               "hint": PHONEME_HINT.get(target[i], "")})
            elif op == "del":
                errors.append({"type": "del", "expected": target[i], "got": "-",
                               "word": phoneme_to_word.get(i, ""),
                               "hint": PHONEME_HINT.get(target[i], "")})

        # ---- 单词级结果 ----
        word_results = []
        for w in words:
            seg = status[w["start"]:w["end"]]
            total = len(seg)
            correct = sum(1 for s in seg if s == "match")
            word_score = round(correct / total * 100) if total else 0
            word_results.append({
                "word": w["word"],
                "score": word_score,
                "phonemes": [
                    {"ph": target[w["start"] + k],
                     "status": seg[k],
                     "hint": PHONEME_HINT.get(target[w["start"] + k], "")}
                    for k in range(total)
                ],
            })
        # ---- 整句分数 ----
        n = len(target)
        n_match = status.count("match")
        n_sub = status.count("sub")
        n_del = status.count("del")
        n_ins = sum(1 for op, _, _ in ops if op == "ins")

        accuracy = _clamp(n_match / n * 100)
        completeness = _clamp((n_match + n_sub) / n * 100)

        # 流利度：用「音素数 / 时长」估算语速，英语母语者约 8~16 音素/秒
        rate = n / max(duration, 0.1)
        if rate < 8:
            fluency = _clamp(rate / 8 * 100)
        elif rate <= 16:
            fluency = 100.0
        else:
            fluency = _clamp(100 - (rate - 16) * 5, low=60)

        overall = _clamp(0.60 * accuracy + 0.25 * completeness + 0.15 * fluency)

        return {
            "overall": round(overall),
            "accuracy": round(accuracy),
            "completeness": round(completeness),
            "fluency": round(fluency),
            "target": target,
            "actual": hyp,
            "words": word_results,
            "errors": errors,
            "skipped": skipped,
            "duration": duration,
            "speed": round(rate, 1),
            "counts": {"match": n_match, "sub": n_sub, "del": n_del, "ins": n_ins},
            "message": "",
        }


def format_report(result):
    """
    把评测结果排版成便于终端阅读的中文报告

    :param result: PronunciationAssessor.assess() 的返回值
    :return: 多行字符串
    """
    lines = []
    lines.append("-" * 52)
    lines.append(f"总分 {result['overall']}/100   "
                 f"准确度 {result['accuracy']}  "
                 f"完整度 {result['completeness']}  "
                 f"流利度 {result['fluency']}")
    lines.append("-" * 52)

    if result.get("message"):
        lines.append(result["message"])

    # 单词级展示：读对的音素打 √，读错/漏读的标注
    for w in result.get("words", []):
        mark = "√" if w["score"] >= 80 else ("~" if w["score"] >= 50 else "×")
        detail = " ".join(
            f"{p['ph']}" if p["status"] == "match" else f"[{p['ph']}?]"
            for p in w["phonemes"]
        )
        lines.append(f"  {mark} {w['word']:<14} {w['score']:>3}分   {detail}")

    if result.get("errors"):
        lines.append("-" * 52)
        lines.append("需要重点纠正：")
        for e in result["errors"][:8]:
            if e["type"] == "sub":
                lines.append(f"  · 「{e['expected']}」读成了「{e['got']}」"
                             + (f"  → {e['hint']}" if e["hint"] else ""))
            elif e["type"] == "del":
                lines.append(f"  · 「{e['expected']}」漏读了"
                             + (f"  → {e['hint']}" if e["hint"] else ""))
            else:
                lines.append(f"  · 多读了一个音「{e['got']}」")

    if result.get("skipped"):
        lines.append(f"（以下单词不在发音词典中，未参与评分：{', '.join(result['skipped'])}）")

    lines.append("-" * 52)
    return "\n".join(lines)


if __name__ == "__main__":
    # 简易自测：python pronunciation.py <音频路径> "参考句子"
    import sys

    if len(sys.argv) < 3:
        print('用法: python pronunciation.py <音频文件> "参考句子"')
        sys.exit(1)

    assessor = PronunciationAssessor()
    print(format_report(assessor.assess(sys.argv[1], sys.argv[2])))
