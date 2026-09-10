"""对话场景表 scene（SRS 表 2）。"""
from typing import Any

from sqlalchemy import JSON, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base, TimestampMixin


class Scene(Base, TimestampMixin):
    __tablename__ = "scene"

    scene_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    scene_name: Mapped[str] = mapped_column(String(50), nullable=False, comment="场景名称")
    scene_category: Mapped[str] = mapped_column(String(20), index=True, default="生活", comment="分类")
    scene_desc: Mapped[str] = mapped_column(String(255), default="", comment="描述")
    level_scope: Mapped[str] = mapped_column(String(20), default="A1-C2", comment="适配等级范围")
    role_setting: Mapped[dict[str, Any]] = mapped_column(JSON, default=dict, comment="AI 角色设定与剧本")
    cover_url: Mapped[str | None] = mapped_column(String(255), nullable=True, comment="封面")
    status: Mapped[int] = mapped_column(Integer, default=1, index=True, comment="0 下架 / 1 上架")
