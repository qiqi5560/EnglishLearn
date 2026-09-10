"""学习资源接口（F004）。"""
from fastapi import APIRouter, HTTPException, Query, status
from sqlalchemy import func, select

from app.core.deps import DbDep
from app.models.resource import LearningResource
from app.schemas import ok
from app.schemas.serializers import resource_to_dict

router = APIRouter(prefix="/resources", tags=["resources"])


@router.get("")
def list_resources(
    db: DbDep,
    type: str | None = Query(default=None, alias="type", description="电影/剧集/新闻/播客/有声书"),
    category: str | None = Query(default=None),
    level: str | None = Query(default=None),
    keyword: str | None = Query(default=None),
    page: int = Query(default=1, ge=1),
    pageSize: int = Query(default=20, ge=1, le=100),
):
    cond = [LearningResource.status == 1]
    if type:
        cond.append(LearningResource.res_type == type)
    if category:
        cond.append(LearningResource.category == category)
    if level:
        cond.append(LearningResource.level == level)
    if keyword:
        cond.append(LearningResource.title.contains(keyword))
    total = db.scalar(select(func.count()).select_from(LearningResource).where(*cond)) or 0
    rows = db.scalars(
        select(LearningResource)
        .where(*cond)
        .order_by(LearningResource.resource_id)
        .offset((page - 1) * pageSize)
        .limit(pageSize)
    ).all()
    return ok({"list": [resource_to_dict(r) for r in rows], "total": total})


@router.get("/{resource_id}")
def resource_detail(resource_id: int, db: DbDep):
    item = db.get(LearningResource, resource_id)
    if item is None or item.status != 1:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="资源不存在或已下架")
    return ok(resource_to_dict(item))
