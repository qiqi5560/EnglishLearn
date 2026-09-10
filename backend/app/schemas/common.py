"""统一响应信封与公共序列化工具。

约定：所有接口返回 { code, data, message }，code=0 表示成功；
错误时 code 与 HTTP 状态码一致（401/403/404/422/500），由全局异常处理器封装。
"""
from datetime import date, datetime
from typing import Any

OK = 0


def ok(data: Any = None, message: str = "ok") -> dict:
    return {"code": OK, "message": message, "data": data}


def fail(code: int, message: str) -> dict:
    return {"code": code, "message": message, "data": None}


def iso(dt: datetime | date | None) -> str | None:
    if dt is None:
        return None
    if isinstance(dt, datetime):
        return dt.isoformat(sep=" ")
    return dt.isoformat()
