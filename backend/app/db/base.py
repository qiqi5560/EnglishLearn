"""ORM 声明基类与通用命名约定。"""
from datetime import datetime

from sqlalchemy import DateTime, func
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


class Base(DeclarativeBase):
    """所有 ORM 模型的基类。"""


class TimestampMixin:
    """通用时间戳字段（应用服务端 UTC 时间，保证跨时区一致）。"""

    create_time: Mapped[datetime] = mapped_column(DateTime, server_default=func.now(), comment="创建时间")
    update_time: Mapped[datetime] = mapped_column(
        DateTime, server_default=func.now(), onupdate=func.now(), comment="更新时间"
    )
