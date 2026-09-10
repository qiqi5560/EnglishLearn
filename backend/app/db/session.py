"""数据库引擎与会话工厂。"""
from pathlib import Path

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from app.core.config import settings

_db_url = settings.DATABASE_URL
if _db_url.startswith("sqlite"):
    # 确保 SQLite 文件所在目录存在；SQLite 单文件写并发需关闭同线程限制
    raw = _db_url.replace("sqlite:///", "")
    if raw and raw != ":memory:" and not raw.startswith("file:"):
        Path(raw).parent.mkdir(parents=True, exist_ok=True)
    connect_args = {"check_same_thread": False}
else:
    connect_args = {}

engine = create_engine(_db_url, connect_args=connect_args, pool_pre_ping=True)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
