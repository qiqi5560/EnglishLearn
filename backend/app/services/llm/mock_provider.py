"""规则式占位 LLM Provider。

未接入真实大模型前，用它按关键词返回预设英文回复与启发式评分，
保证前端对话流程可用、调用形状与真实接入保持一致。

注意：这些规则仅用于联调演示，正式接入大模型后应整体替换。
"""
import re
import random
import statistics

from app.models.conversation import ConversationMessage
from app.services.llm.base import EvalResult, LLMProvider, SummaryResult

# 通用搭话回复（英文, 中文）
_GENERIC_REPLIES: list[tuple[str, str]] = [
    ("I see. That is really interesting. Could you tell me a bit more?",
     "我明白了，这很有意思。你能再多说一点吗？"),
    ("Good point! What would you do in that situation?",
     "说得好！那种情况下你会怎么做呢？"),
    ("Thanks for sharing. What happened next?",
     "谢谢分享。接下来发生了什么？"),
    ("That sounds great. How often do you practice English?",
     "听起来不错。你多久练一次英语呢？"),
    ("I totally get it. Could you give me an example?",
     "完全理解。你能举个例子吗？"),
]

# 关键词（小写） → 回复
_KEYWORD_REPLIES: list[tuple[list[str], tuple[str, str]]] = [
    (
        ["restaurant", "menu", "order", "food", "eat", "dish", "delicious"],
        (
            "Sure! I would recommend our grilled salmon — it is today's special. Would you like anything to drink?",
            "好的！我推荐我们的香煎三文鱼，是今天的特色菜。你想喝点什么吗？",
        ),
    ),
    (
        ["meeting", "project", "report", "deadline", "schedule", "plan"],
        (
            "Absolutely, let's keep the meeting focused. Shall we review the project timeline first?",
            "当然，我们让会议聚焦一些。先回顾一下项目时间表好吗？",
        ),
    ),
    (
        ["check-in", "boarding", "luggage", "flight", "airport", "gate"],
        (
            "Of course. May I see your passport and booking reference, please? Your luggage will be checked to the final destination.",
            "好的。请出示您的护照和订票号。您的行李将直接托运到最终目的地。",
        ),
    ),
    (
        ["hotel", "room", "reservation", "check in", "key", "breakfast"],
        (
            "Welcome! You have a confirmed reservation. Here is your room key, and breakfast is served from 7 to 10.",
            "欢迎光临！您有确认的预订。这是您的房卡，早餐供应时间是 7 点到 10 点。",
        ),
    ),
    (
        ["interview", "experience", "skill", "job", "work"],
        (
            "Great answer. Could you describe a challenge you overcame at work, and what you learned from it?",
            "回答得很好。能描述一个你在工作中克服的挑战，以及你从中学会了什么吗？",
        ),
    ),
    (
        ["class", "homework", "teacher", "question", "study", "lesson"],
        (
            "That's a good question for today's discussion. Let's open it up — what do the rest of you think?",
            "这是今天讨论中的好问题。我们开放一下——大家觉得呢？",
        ),
    ),
    (
        ["hello", "hi ", "hey", "good morning", "good afternoon", "nice to meet"],
        (
            "Hello! It's great to practice with you today. How are you doing?",
            "你好！今天和你一起练习很开心。你最近怎么样？",
        ),
    ),
    (
        ["thank", "thanks"],
        (
            "You are very welcome! Is there anything else I can help you with?",
            "不客气！还有什么我可以帮你的吗？",
        ),
    ),
    (
        ["bye", "goodbye", "see you", "that's all", "that is all"],
        (
            "It was a pleasure talking with you. See you next time!",
            "和你聊天很愉快。下次见！",
        ),
    ),
]


class MockProvider:
    """占位实现：规则回复 + 启发式评分。"""

    name: str = "mock"

    def opening(self, scene_name: str, scene_desc: str, role: str) -> tuple[str, str]:
        openers = {
            "餐厅点餐": ("Good evening! Welcome to our restaurant. How many people are in your party?",
                         "晚上好！欢迎光临本餐厅，请问一共几位？"),
            "商务会议": ("Good morning, everyone. Let's get started. The first item is our Q3 project review. "
                         "Who would like to share the update?",
                         "大家早上好，我们开始吧。第一项是第三季度项目回顾，谁来分享一下进展？"),
            "机场值机": ("Hello, welcome to check-in. May I see your passport, please?",
                         "您好，欢迎办理值机手续，请出示您的护照。"),
            "课堂讨论": ("Morning, class! Today we will talk about how technology changes our life. "
                         "Does anyone have a thought?",
                         "同学们早！今天我们聊聊科技如何改变生活。有谁想说说的吗？"),
            "酒店入住": ("Welcome to our hotel. Do you have a reservation with us today?",
                         "欢迎光临我们酒店，请问您今天有预订吗？"),
            "面试问答": ("Thank you for coming. To begin, could you briefly introduce yourself?",
                         "感谢你来参加面试。首先，请简单介绍一下自己。"),
        }
        fallback_en = f"Hello! I am {role}. Let's start our conversation about {scene_name}. How are you today?"
        fallback_zh = f"你好！我是{role}。让我们开始关于「{scene_name}」的对话吧，今天过得怎么样？"
        return openers.get(scene_name, (fallback_en, fallback_zh))

    def reply(
        self,
        *,
        scene_name: str,
        scene_desc: str,
        role: str,
        history: list[ConversationMessage],
        user_input: str,
    ) -> tuple[str, str]:
        text = (user_input or "").strip().lower()
        for keywords, pair in _KEYWORD_REPLIES:
            if any(k in text for k in keywords):
                return pair
        return random.choice(_GENERIC_REPLIES)

    def evaluate(self, user_input: str) -> EvalResult:
        """启发式四维评分：词数/长度反映表达完整性。"""
        words = [w for w in re.split(r"[^A-Za-z']+", user_input or "") if w]
        wc = len(words)
        # 基础区间 + 长度奖励，保证可解释
        base = min(88, 62 + wc * 2)
        noise = random.uniform(-4, 4)
        pron = min(96, max(55, round(base + noise, 1)))
        fluency = min(96, max(55, round(base + (0 if wc >= 6 else -6) + random.uniform(-3, 3), 1)))
        reaction = min(96, max(55, round(base + 4 + random.uniform(-3, 3), 1)))
        natural = min(96, max(55, round(base + random.uniform(-5, 5), 1)))

        # 常见发音易错点（演示用，接入 ASR 后替换）
        issues: list[dict] = []
        lowered = (user_input or "").lower()
        if "would" in lowered:
            issues.append({"word": "would", "phoneme": "/wʊd/", "note": "注意 /wʊd/ 的短元音与连读"})
        if "the" in lowered:
            issues.append({"word": "the", "phoneme": "/ðə/", "note": "th 需舌尖轻触上齿"})
        grammar = "I noticed a small issue, but your meaning was clear. Keep speaking naturally."
        if wc >= 3:
            grammar = "Good sentence structure overall. Try to keep your sentences connected."

        better: str | None = None
        if "want to" in lowered and wc >= 2:
            better = "You can say: \"I'd like to...\" to sound more polite."
        return EvalResult(
            pron=pron,
            fluency=fluency,
            reaction=reaction,
            natural=natural,
            grammar_feedback=grammar,
            phoneme_issues=issues,
            better_expression=better,
        )

    def summarize(
        self, messages: list[ConversationMessage], evaluations: list[EvalResult]
    ) -> SummaryResult:
        if evaluations:
            pron = round(statistics.mean(e.pron for e in evaluations), 1)
            fluency = round(statistics.mean(e.fluency for e in evaluations), 1)
            reaction = round(statistics.mean(e.reaction for e in evaluations), 1)
            natural = round(statistics.mean(e.natural for e in evaluations), 1)
        else:
            pron = fluency = reaction = natural = 70.0
        total = round((pron * 0.35 + fluency * 0.25 + reaction * 0.2 + natural * 0.2), 1)

        user_msgs = [m for m in messages if m.speaker == "user"]
        highlights = ["表达自然，能围绕场景主动开口并回应对方。", "话题保持连贯，敢于完整表达自己的想法。"]
        if len(user_msgs) >= 4:
            highlights.insert(1, f"本场完成了 {len(user_msgs)} 次有效开口，练习量充足。")
        improvements: list[str] = []
        if pron < 80:
            improvements.append("部分单词发音不够清晰，建议放慢语速逐词练习。")
        if fluency < 78:
            improvements.append("注意句间停顿与连读，保持稳定语流。")
        if not improvements:
            improvements.append("可进一步丰富句型，尝试使用更地道的表达。")
        suggestions = [
            "回听本场录音，标记卡壳与犹豫的句子并重复练习 3 次。",
            "跟随「场景」对应素材做一遍慢速跟读，强化发音。",
        ]
        corrections: list[dict] = []
        for e in evaluations:
            for issue in e.phoneme_issues:
                corrections.append(
                    {
                        "type": "发音",
                        "word": issue.get("word", ""),
                        "correct": issue.get("phoneme", ""),
                        "note": issue.get("note", ""),
                    }
                )
            if e.better_expression:
                corrections.append({"type": "地道表达", "word": "", "correct": e.better_expression, "note": ""})
        # 去重，最多取 4 条
        seen: set[str] = set()
        dedup: list[dict] = []
        for c in corrections:
            key = f"{c['type']}-{c['word']}-{c['correct']}"
            if key not in seen:
                seen.add(key)
                dedup.append(c)
            if len(dedup) >= 4:
                break
        feedback_text = (
            f"综合表现不错，最终得分 {total}。继续保持每天开口练习，"
            f"重点突破{'、'.join(improvements[:2])}"
        )
        return SummaryResult(
            total=total,
            pron=pron,
            fluency=fluency,
            reaction=reaction,
            natural=natural,
            highlights=highlights,
            improvements=improvements,
            suggestions=suggestions,
            corrections=dedup,
            feedback_text=feedback_text,
        )
