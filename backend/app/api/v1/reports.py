"""学习报表接口（F007）。"""
from fastapi import APIRouter

from app.core.deps import CurrentUser, DbDep
from app.schemas import ok
from app.services.report_service import overview

router = APIRouter(prefix="/reports", tags=["reports"])


@router.get("/overview")
def report_overview(db: DbDep, user: CurrentUser):
    return ok(overview(db, user))
