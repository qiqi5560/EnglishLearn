"""FastAPI 启动入口。

确保以 backend 目录为工作目录执行（uvicorn 相对数据库路径依赖 cwd）。
"""
import os

# 保证以 backend 目录为工作目录，SQLite/.env 的相对路径稳定
os.chdir(os.path.dirname(os.path.abspath(__file__)))

import uvicorn

from app.core.config import settings

if __name__ == "__main__":
    uvicorn.run("app.main:app", host=settings.HOST, port=settings.PORT, reload=False)
