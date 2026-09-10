"""学习方案 learning_plan（SRS 表 6）与每日任务扩展表。"""
from datetime import date, datetime

from sqlalchemy import Date, DateTime, ForeignKey, Integer, String, Text, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base, TimestampMixin

PLAN_STATUS = ("active", "done", "expired")


class LearningPlan(Base, TimestampMixin):
    __tablename__ = "learning_plan"

    plan_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.user_id"), unique=True, index=True)
    target_goal: Mapped[str] = mapped_column(String(50), default="兴趣", comment="考试/商务/出国/兴趣")
    level_start: Mapped[str] = mapped_column(String(10), default="A1")
    level_current: Mapped[str] = mapped_column(String(10), default="A1")
    plan_content: Mapped[str] = mapped_column(Text, default="", comment="方案说明/阶段目标（JSON 文本）")
    plan_start: Mapped[date] = mapped_column(Date, nullable=False)
    plan_status: Mapped[str] = mapped_column(String(20), default="active", index=True)
    update_time: Mapped[datetime] = mapped_column(
        DateTime, server_default=func.now(), onupdate=func.now()
    )

    tasks: Mapped[list["DailyTask"]] = relationship(back_populates="plan", cascade="all, delete-orphan")


class DailyTask(Base):
    """每日任务（F001 执行闭环扩展表）。"""

    __tablename__ = "daily_task"
    __table_args__ = (UniqueConstraint("user_id", "task_date", "title", name="uq_task_day_title"),)

    task_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.user_id"), index=True)
    plan_id: Mapped[int | None] = mapped_column(Integer, ForeignKey("learning_plan.plan_id"), nullable=True)
    task_type: Mapped[str] = mapped_column(String(20), default="场景对话", comment="场景对话/跟读/精听/单词")
    title: Mapped[str] = mapped_column(String(100), nullable=False)
    duration_min: Mapped[int] = mapped_column(Integer, default=10)
    scene_id: Mapped[int | None] = mapped_column(
        Integer, ForeignKey("scene.scene_id"), nullable=True, index=True, comment="绑定练习场景（场景对话任务）"
    )
    resource_id: Mapped[int | None] = mapped_column(
        Integer,
        ForeignKey("learning_resource.resource_id"),
        nullable=True,
        index=True,
        comment="绑定练习素材（跟读/精听任务）",
    )
    done: Mapped[bool] = mapped_column(Integer, default=0)
    task_date: Mapped[date] = mapped_column(Date, index=True)
    create_time: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())

    plan: Mapped[LearningPlan] = relationship(back_populates="tasks")
