"""系统配置表 sys_config（管理端 F004/F008/F006 等）。"""
from datetime import datetime
from typing import Any

from sqlalchemy import JSON, DateTime, String, func
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class SysConfig(Base):
    __tablename__ = "sys_config"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    key: Mapped[str] = mapped_column(String(50), unique=True, index=True)
    value: Mapped[Any] = mapped_column(JSON, default=dict)
    remark: Mapped[str | None] = mapped_column(String(255), nullable=True)
    update_time: Mapped[datetime] = mapped_column(DateTime, server_default=func.now(), onupdate=func.now())
