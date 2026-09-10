"""管理后台接口（全部需要管理员权限）。"""
import json
from datetime import date, timedelta

from fastapi import APIRouter, HTTPException, Query, status
from pydantic import BaseModel, ConfigDict, Field
from sqlalchemy import func, select

from app.core.deps import AdminUser, DbDep
from app.models.community import CommunityPost
from app.models.conversation import ConversationSession
from app.models.plan import LearningPlan
from app.models.resource import LearningResource
from app.models.scene import Scene
from app.models.study import StudyRecord
from app.models.sysconfig import SysConfig
from app.models.user import User
from app.schemas import ok
from app.schemas.serializers import (
    config_to_dict,
    post_to_dict,
    resource_to_dict,
    scene_to_dict,
    user_to_dict,
)

router = APIRouter(prefix="/admin", tags=["admin"])

# ============================================================
# 数据看板
# ============================================================


@router.get("/dashboard")
def dashboard(db: DbDep, admin: AdminUser):
    user_total = db.scalar(select(func.count()).select_from(User)) or 0
    today = date.today()
    start = today - timedelta(days=7)

    today_sessions = db.scalar(
        select(func.count())
        .select_from(ConversationSession)
        .where(func.date(ConversationSession.start_time) == today.isoformat())
    ) or 0
    pending_res = db.scalar(
        select(func.count()).select_from(LearningResource).where(LearningResource.status == 0)
    ) or 0
    pending_posts = db.scalar(
        select(func.count()).select_from(CommunityPost).where(CommunityPost.status == 0)
    ) or 0

    # 近 7 日活跃（每日完成练习人次）
    rows = db.execute(
        select(StudyRecord.learn_date, func.count())
        .where(StudyRecord.learn_date >= start)
        .group_by(StudyRecord.learn_date)
    ).all()
    by_day = {d: int(c) for d, c in rows}
    dates, values = [], []
    for i in range(7):
        d = start + timedelta(days=i)
        dates.append(d.strftime("%m-%d"))
        values.append(by_day.get(d, 0))

    return ok(
        {
            "stats": [
                {"label": "总用户数", "value": f"{user_total:,}", "color": "var(--primary)"},
                {"label": "今日练习会话", "value": f"{today_sessions:,}", "color": "var(--success)"},
                {"label": "待审核资源", "value": str(pending_res), "color": "var(--warning)"},
                {"label": "待审核帖子", "value": str(pending_posts), "color": "var(--danger)"},
            ],
            "trend": {"dates": dates, "values": values},
            "todos": [
                {"type": "资源审核", "desc": "新增素材待审核", "count": pending_res},
                {"type": "社区审核", "desc": "用户举报帖子待处理", "count": pending_posts},
            ],
        }
    )


# ============================================================
# 场景管理
# ============================================================


@router.get("/scenes")
def admin_list_scenes(db: DbDep, admin: AdminUser, keyword: str | None = None):
    cond = []
    if keyword:
        cond.append(Scene.scene_name.contains(keyword))
    rows = db.scalars(select(Scene).where(*cond).order_by(Scene.scene_id)).all()
    return ok([scene_to_dict(s) for s in rows])


class SceneUpdateIn(BaseModel):
    sceneName: str | None = None
    sceneCategory: str | None = None
    sceneDesc: str | None = None
    levelScope: str | None = None
    coverUrl: str | None = None
    status: int | None = Field(default=None, ge=0, le=1)


@router.put("/scenes/{scene_id}")
def admin_update_scene(scene_id: int, body: SceneUpdateIn, db: DbDep, admin: AdminUser):
    scene = db.get(Scene, scene_id)
    if scene is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="场景不存在")
    data = body.model_dump(exclude_none=True)
    mapping = {
        "sceneName": "scene_name",
        "sceneCategory": "scene_category",
        "sceneDesc": "scene_desc",
        "levelScope": "level_scope",
        "coverUrl": "cover_url",
        "status": "status",
    }
    for alias, col in mapping.items():
        if alias in data:
            setattr(scene, col, data[alias])
    db.commit()
    return ok(scene_to_dict(scene), message="场景已更新")


# ============================================================
# 资源管理
# ============================================================


@router.get("/resources")
def admin_list_resources(
    db: DbDep,
    admin: AdminUser,
    keyword: str | None = None,
    page: int = Query(1, ge=1),
    pageSize: int = Query(10, ge=1, le=100),
):
    cond = []
    if keyword:
        cond.append(LearningResource.title.contains(keyword))
    total = db.scalar(select(func.count()).select_from(LearningResource).where(*cond)) or 0
    rows = db.scalars(
        select(LearningResource)
        .where(*cond)
        .order_by(LearningResource.resource_id.desc())
        .offset((page - 1) * pageSize)
        .limit(pageSize)
    ).all()
    return ok({"list": [resource_to_dict(r) for r in rows], "total": total})


class ResourceIn(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)
    title: str = Field(min_length=1, max_length=100)
    type: str = "剧集"
    category: str = "生活"
    level: str = "B1"
    mediaUrl: str | None = None
    durationSec: int | None = None
    status: int = 1


@router.post("/resources")
def admin_create_resource(body: ResourceIn, db: DbDep, admin: AdminUser):
    item = LearningResource(
        title=body.title,
        res_type=body.type,
        category=body.category,
        level=body.level,
        media_url=body.mediaUrl,
        duration_sec=body.durationSec,
        status=1 if body.status in (0, 1) else 1,
        uploader_id=admin.user_id,
    )
    db.add(item)
    db.commit()
    db.refresh(item)
    return ok(resource_to_dict(item), message="资源已添加")


class ResourceUpdateIn(BaseModel):
    title: str | None = None
    type: str | None = None
    category: str | None = None
    level: str | None = None
    mediaUrl: str | None = None
    durationSec: int | None = None
    status: int | None = Field(default=None, ge=0, le=1)


@router.put("/resources/{resource_id}")
def admin_update_resource(resource_id: int, body: ResourceUpdateIn, db: DbDep, admin: AdminUser):
    item = db.get(LearningResource, resource_id)
    if item is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="资源不存在")
    data = body.model_dump(exclude_none=True)
    mapping = {
        "title": "title",
        "type": "res_type",
        "category": "category",
        "level": "level",
        "mediaUrl": "media_url",
        "durationSec": "duration_sec",
        "status": "status",
    }
    for alias, col in mapping.items():
        if alias in data:
            setattr(item, col, data[alias])
    db.commit()
    return ok(resource_to_dict(item), message="资源已更新")


@router.delete("/resources/{resource_id}")
def admin_delete_resource(resource_id: int, db: DbDep, admin: AdminUser):
    item = db.get(LearningResource, resource_id)
    if item is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="资源不存在")
    db.delete(item)
    db.commit()
    return ok(message="资源已删除")


# ============================================================
# 社区管理
# ============================================================


@router.get("/posts")
def admin_list_posts(
    db: DbDep,
    admin: AdminUser,
    reviewStatus: int | None = Query(default=None, alias="status"),
    page: int = Query(1, ge=1),
    pageSize: int = Query(10, ge=1, le=100),
):
    cond = []
    if reviewStatus is not None:
        cond.append(CommunityPost.status == reviewStatus)
    total = db.scalar(select(func.count()).select_from(CommunityPost).where(*cond)) or 0
    rows = db.scalars(
        select(CommunityPost)
        .where(*cond)
        .order_by(CommunityPost.create_time.desc())
        .offset((page - 1) * pageSize)
        .limit(pageSize)
    ).all()
    return ok({"list": [post_to_dict(p) for p in rows], "total": total})


class PostReviewIn(BaseModel):
    status: int | None = Field(default=None, ge=0, le=2)
    isTop: bool | None = None


@router.put("/posts/{post_id}")
def admin_review_post(post_id: int, body: PostReviewIn, db: DbDep, admin: AdminUser):
    post = db.get(CommunityPost, post_id)
    if post is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="帖子不存在")
    if body.status is not None:
        post.status = body.status
    if body.isTop is not None:
        post.is_top = body.isTop
    db.commit()
    return ok(post_to_dict(post), message="操作成功")


@router.delete("/posts/{post_id}")
def admin_delete_post(post_id: int, db: DbDep, admin: AdminUser):
    post = db.get(CommunityPost, post_id)
    if post is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="帖子不存在")
    db.delete(post)
    db.commit()
    return ok(message="删除成功")


# ============================================================
# 用户管理
# ============================================================


@router.get("/users")
def admin_list_users(
    db: DbDep,
    admin: AdminUser,
    keyword: str | None = None,
    role: str | None = None,
    page: int = Query(1, ge=1),
    pageSize: int = Query(10, ge=1, le=100),
):
    cond = []
    if keyword:
        like = f"%{keyword}%"
        cond.append(User.phone.like(like) | User.nickname.like(like))
    if role:
        cond.append(User.user_role == role)
    total = db.scalar(select(func.count()).select_from(User).where(*cond)) or 0
    rows = db.scalars(
        select(User).where(*cond).order_by(User.user_id.desc()).offset((page - 1) * pageSize).limit(pageSize)
    ).all()
    plans = {
        p.user_id: p.level_current
        for p in db.scalars(
            select(LearningPlan).where(
                LearningPlan.plan_status == "active",
                LearningPlan.user_id.in_([u.user_id for u in rows]),
            )
        ).all()
    } if rows else {}
    return ok(
        {
            "list": [user_to_dict(u, level=plans.get(u.user_id)) for u in rows],
            "total": total,
        }
    )


class UserUpdateIn(BaseModel):
    nickname: str | None = None
    status: int | None = Field(default=None, ge=0, le=1)
    userRole: str | None = None


@router.put("/users/{user_id}")
def admin_update_user(user_id: int, body: UserUpdateIn, db: DbDep, admin: AdminUser):
    target = db.get(User, user_id)
    if target is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="用户不存在")
    if target.user_id == admin.user_id and body.status == 0:
        raise HTTPException(status_code=422, detail="不能停用自己")
    if body.nickname is not None:
        target.nickname = body.nickname.strip() or target.nickname
    if body.status is not None:
        target.status = body.status
    if body.userRole is not None:
        if target.user_id == admin.user_id and body.userRole != "admin":
            raise HTTPException(status_code=422, detail="不能修改自己的角色")
        target.user_role = body.userRole
    db.commit()
    return ok(user_to_dict(target), message="用户已更新")


# ============================================================
# 系统配置
# ============================================================

DEFAULT_CONFIG = {
    "speech": {"slowSpeed": 0.8, "normalSpeed": 1.2},
    "recommend": {"content": True, "collab": True, "model": True},
    "audit": {"content": True, "manual": True},
}


def _get_config(db: DbDep) -> SysConfig:
    cfg = db.scalar(select(SysConfig).where(SysConfig.key == "system_config"))
    if cfg is None:
        cfg = SysConfig(key="system_config", value=DEFAULT_CONFIG, remark="系统全局配置")
        db.add(cfg)
        db.commit()
    return cfg


@router.get("/configs")
def get_configs(db: DbDep, admin: AdminUser):
    cfg = _get_config(db)
    merged = {**DEFAULT_CONFIG, **(cfg.value or {})}
    for group in merged:
        if isinstance(merged[group], dict) and isinstance((cfg.value or {}).get(group), dict):
            merged[group] = {**DEFAULT_CONFIG[group], **merged[group]}
    return ok(merged)


@router.put("/configs")
def update_configs(body: dict, db: DbDep, admin: AdminUser):
    cfg = _get_config(db)
    old = cfg.value or {}
    merged = {**DEFAULT_CONFIG, **old}
    for group in merged:
        if isinstance(body.get(group), dict):
            merged[group] = {**merged.get(group, {}), **body[group]}
    cfg.value = merged
    db.commit()
    return ok(merged, message="配置已保存")
