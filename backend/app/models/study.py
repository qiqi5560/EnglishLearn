"""学习记录表 study_record（SRS 表 8）。"""
from datetime import date, datetime

from sqlalchemy import Date, DateTime, ForeignKey, Integer, Numeric, String, func
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base

ACTION_TYPES = ("entrance_test", "scenario", "read_along", "listen", "resource")


class StudyRecord(Base):
    __tablename__ = "study_record"

    record_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.user_id"), index=True)
    session_id: Mapped[int | None] = mapped_column(
        Integer, ForeignKey("conversation_session.session_id"), nullable=True
    )
    action_type: Mapped[str] = mapped_column(String(30), index=True)
    duration_min: Mapped[int] = mapped_column(Integer, default=1)
    score: Mapped[float | None] = mapped_column(Numeric(4, 1), nullable=True)
    learn_date: Mapped[date] = mapped_column(Date, index=True)
    create_time: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())
