"""用户表 users（SRS 表 1）。"""
from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Integer, String, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base, TimestampMixin

# 年龄段：少儿 / K12 / 成人 / 中老年
AGE_GROUPS = ("child", "k12", "adult", "senior")
# 角色：学习者 / 管理员 / 监护人
USER_ROLES = ("learner", "admin", "guardian")


class User(Base, TimestampMixin):
    __tablename__ = "users"

    user_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    phone: Mapped[str] = mapped_column(String(20), unique=True, index=True, nullable=False, comment="登录手机号")
    password_hash: Mapped[str | None] = mapped_column(String(128), nullable=True, comment="密码哈希")
    nickname: Mapped[str] = mapped_column(String(50), default="", comment="昵称")
    avatar_url: Mapped[str | None] = mapped_column(String(255), nullable=True, comment="头像")
    age_group: Mapped[str] = mapped_column(String(20), default="adult", comment="年龄段")
    user_role: Mapped[str] = mapped_column(String(20), default="learner", index=True, comment="角色")
    guardian_id: Mapped[int | None] = mapped_column(
        Integer, ForeignKey("users.user_id"), nullable=True, comment="监护人 user_id"
    )
    status: Mapped[int] = mapped_column(Integer, default=1, index=True, comment="0 停用 / 1 正常")
    register_time: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())
    last_login_time: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)

    guardian: Mapped["User | None"] = relationship(remote_side="User.user_id", back_populates="wards")
    wards: Mapped[list["User"]] = relationship(remote_side="User.guardian_id", back_populates="guardian")
