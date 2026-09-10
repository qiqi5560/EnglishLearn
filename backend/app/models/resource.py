"""学习资源表 learning_resource（SRS 表 7）。"""
from sqlalchemy import ForeignKey, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base, TimestampMixin


class LearningResource(Base, TimestampMixin):
    __tablename__ = "learning_resource"

    resource_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    title: Mapped[str] = mapped_column(String(100), nullable=False, comment="标题")
    res_type: Mapped[str] = mapped_column(String(20), default="剧集", comment="电影/剧集/新闻/播客/有声书")
    category: Mapped[str] = mapped_column(String(30), index=True, default="生活", comment="职场/雅思/生活/出行")
    level: Mapped[str] = mapped_column(String(10), default="B1", index=True)
    media_url: Mapped[str | None] = mapped_column(String(255), nullable=True, comment="音视频地址")
    duration_sec: Mapped[int | None] = mapped_column(Integer, nullable=True)
    uploader_id: Mapped[int | None] = mapped_column(
        Integer, ForeignKey("users.user_id"), nullable=True
    )
    status: Mapped[int] = mapped_column(Integer, default=1, index=True, comment="0 下架 / 1 上架")
