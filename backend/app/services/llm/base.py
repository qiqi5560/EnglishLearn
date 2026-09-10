"""LLM Provider 抽象协议：路由层只依赖该协议，不感知具体实现。"""
from dataclasses import dataclass, field
from typing import Protocol

from app.models.conversation import ConversationMessage


@dataclass
class EvalResult:
    """一次对话消息的实时评测结果（F003 四维）。"""

    pron: float = 0.0
    fluency: float = 0.0
    reaction: float = 0.0
    natural: float = 0.0
    grammar_feedback: str | None = None
    phoneme_issues: list = field(default_factory=list)
    better_expression: str | None = None


@dataclass
class SummaryResult:
    """会话结束后的综合小结（F003 汇总 / F002 练习小结）。"""

    total: float = 0.0
    pron: float = 0.0
    fluency: float = 0.0
    reaction: float = 0.0
    natural: float = 0.0
    highlights: list = field(default_factory=list)
    improvements: list = field(default_factory=list)
    suggestions: list = field(default_factory=list)
    corrections: list = field(default_factory=list)
    feedback_text: str = ""


class LLMProvider(Protocol):
    """所有 Provider 需要实现的接口。场景、历史上下文均由调用方传入。"""

    name: str

    def opening(self, scene_name: str, scene_desc: str, role: str) -> tuple[str, str]:
        """会话开场白，返回 (英文, 中文)。"""

    def reply(
        self,
        *,
        scene_name: str,
        scene_desc: str,
        role: str,
        history: list[ConversationMessage],
        user_input: str,
    ) -> tuple[str, str]:
        """根据场景与最近上下文生成回复，返回 (英文, 中文)。"""

    def evaluate(self, user_input: str) -> EvalResult:
        """对用户单句输入做启发式评测（真实接入后替换为 ASR+模型评测）。"""

    def summarize(self, messages: list[ConversationMessage], evaluations: list[EvalResult]) -> SummaryResult:
        """聚合整场会话生成小结。"""
