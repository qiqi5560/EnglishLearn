"""对话场景浏览接口（F002 场景库）。"""
from fastapi import APIRouter, HTTPException, Query, status
from sqlalchemy import func, select

from app.core.deps import DbDep
from app.models.scene import Scene
from app.schemas import ok
from app.schemas.serializers import scene_to_dict

router = APIRouter(prefix="/scenes", tags=["scenes"])


@router.get("")
def list_scenes(
    db: DbDep,
    category: str | None = Query(default=None, description="生活/工作/学习/出行"),
    level: str | None = Query(default=None),
    keyword: str | None = Query(default=None),
    page: int = Query(default=1, ge=1),
    pageSize: int = Query(default=20, ge=1, le=100),
):
    cond = [Scene.status == 1]
    if category:
        cond.append(Scene.scene_category == category)
    if level:
        cond.append(Scene.level_scope == level)
    if keyword:
        cond.append(Scene.scene_name.contains(keyword))
    total = db.scalar(select(func.count()).select_from(Scene).where(*cond)) or 0
    rows = db.scalars(
        select(Scene).where(*cond).order_by(Scene.scene_id).offset((page - 1) * pageSize).limit(pageSize)
    ).all()
    return ok({"list": [scene_to_dict(s) for s in rows], "total": total})


@router.get("/recommended")
def recommended_scenes(db: DbDep, limit: int = Query(default=6, ge=1, le=20)):
    """首页推荐场景（当前为用户等级外置规则，后续可结合 F006 推荐策略）。"""
    rows = db.scalars(
        select(Scene).where(Scene.status == 1).order_by(Scene.scene_id).limit(limit)
    ).all()
    return ok([scene_to_dict(s) for s in rows])


@router.get("/{scene_id}")
def scene_detail(scene_id: int, db: DbDep):
    scene = db.get(Scene, scene_id)
    if scene is None or scene.status != 1:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="场景不存在或已下架")
    return ok(scene_to_dict(scene))
