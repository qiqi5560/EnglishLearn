"""FastAPI 应用入口：CORS、统一响应信封、建表 + 种子数据。"""
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.api.v1.router import api_router
from app.core.config import settings
from app.db.base import Base
from app.db.session import engine
from app.schemas.common import fail
from app.seed.seed import run_seed

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(name)s %(levelname)s %(message)s")
logger = logging.getLogger("app")


@asynccontextmanager
async def lifespan(_: FastAPI):
    # 启动时自动建表；空库时灌入演示种子数据
    Base.metadata.create_all(bind=engine)
    seeded = run_seed()
    if seeded:
        logger.info("数据库为空，演示数据已就绪")
    else:
        logger.info("检测到已有数据，跳过种子初始化")
    yield


app = FastAPI(
    title=settings.APP_NAME,
    version="1.0.0",
    description="基于大模型场景扮演的英语口语训练系统 —— FastAPI 后端",
    lifespan=lifespan,
)

# CORS：本地前端开发（vite 5173 经代理访问时同源；直连场景放开）
_cors_origins = [o.strip() for o in settings.CORS_ORIGINS.split(",") if o.strip()]
app.add_middleware(
    CORSMiddleware,
    allow_origins=_cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(api_router, prefix=settings.API_PREFIX)


@app.get("/api/health", tags=["system"])
def health():
    return {"code": 0, "message": "ok", "data": {"service": "backend", "status": "running"}}


@app.exception_handler(HTTPException)
async def http_exception_handler(_: Request, exc: HTTPException):
    return JSONResponse(
        status_code=exc.status_code,
        content=fail(code=exc.status_code, message=str(exc.detail)),
    )


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(_: Request, exc: RequestValidationError):
    first = exc.errors()[0] if exc.errors() else {}
    loc = ".".join(str(x) for x in first.get("loc", []) if x not in ("body", "query", "path"))
    msg = first.get("msg", "参数校验失败")
    text = f"参数 {loc} {msg}" if loc else f"参数校验失败：{msg}"
    return JSONResponse(
        status_code=422,
        content=fail(code=422, message=text),
    )


@app.exception_handler(Exception)
async def unhandled_exception_handler(_: Request, exc: Exception):
    logger.exception("未处理异常: %s", exc)
    return JSONResponse(
        status_code=500,
        content=fail(code=500, message="服务器内部错误，请稍后重试"),
    )
