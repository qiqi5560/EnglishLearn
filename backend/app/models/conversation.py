"""对话会话 / 消息 / 评测记录（SRS 表 3/4/5）。"""
import json
from datetime import datetime
from typing import Any

from sqlalchemy import JSON, DateTime, ForeignKey, Integer, Numeric, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base, TimestampMixin

SESSION_STATUS = ("ongoing", "finished")


class ConversationSession(Base, TimestampMixin):
    __tablename__ = "conversation_session"

    session_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.user_id"), index=True, nullable=False)
    scene_id: Mapped[int | None] = mapped_column(
        Integer, ForeignKey("scene.scene_id"), nullable=True, index=True
    )
    mode: Mapped[str] = mapped_column(String(20), default="scenario", comment="free/scenario/roleplay/read-along")
    start_time: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())
    end_time: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    duration_sec: Mapped[int | None] = mapped_column(Integer, nullable=True)
    session_status: Mapped[str] = mapped_column(String(20), default="ongoing", index=True)
    ai_summary: Mapped[str | None] = mapped_column(Text, nullable=True, comment="AI 小结 JSON")

    scene: Mapped["Scene"] = relationship(lazy="joined")  # noqa: F821
    messages: Mapped[list["ConversationMessage"]] = relationship(
        back_populates="session", cascade="all, delete-orphan", order_by="ConversationMessage.msg_time"
    )
    assessments: Mapped[list["AssessmentRecord"]] = relationship(
        back_populates="session", cascade="all, delete-orphan"
    )

    @property
    def summary_payload(self) -> dict[str, Any] | None:
        if not self.ai_summary:
            return None
        try:
            return json.loads(self.ai_summary)
        except json.JSONDecodeError:
            return None


class ConversationMessage(Base):
    __tablename__ = "conversation_message"

    message_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    session_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("conversation_session.session_id"), index=True, nullable=False
    )
    speaker: Mapped[str] = mapped_column(String(10), nullable=False, comment="user/ai")
    content_en: Mapped[str] = mapped_column(Text, nullable=False)
    content_zh: Mapped[str | None] = mapped_column(Text, nullable=True)
    audio_url: Mapped[str | None] = mapped_column(String(255), nullable=True)
    msg_time: Mapped[datetime] = mapped_column(DateTime, server_default=func.now(), index=True)

    session: Mapped[ConversationSession] = relationship(back_populates="messages")


class AssessmentRecord(Base, TimestampMixin):
    __tablename__ = "assessment_record"

    assess_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    session_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("conversation_session.session_id"), index=True, nullable=False
    )
    message_id: Mapped[int | None] = mapped_column(
        Integer, ForeignKey("conversation_message.message_id"), nullable=True, index=True
    )
    pron_score: Mapped[float] = mapped_column(Numeric(4, 1), default=0)
    fluency_score: Mapped[float] = mapped_column(Numeric(4, 1), default=0)
    reaction_score: Mapped[float] = mapped_column(Numeric(4, 1), default=0)
    natural_score: Mapped[float] = mapped_column(Numeric(4, 1), default=0)
    grammar_feedback: Mapped[str | None] = mapped_column(Text, nullable=True)
    phoneme_issues: Mapped[list | dict | None] = mapped_column(JSON, nullable=True)
    better_expression: Mapped[str | None] = mapped_column(Text, nullable=True)
    assess_time: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())

    session: Mapped[ConversationSession] = relationship(back_populates="assessments")
