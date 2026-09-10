"""v1 路由聚合。"""
from fastapi import APIRouter

from app.api.v1 import (
    admin,
    auth,
    community,
    dialogues,
    plans,
    reports,
    resources,
    scenes,
    users,
)

api_router = APIRouter()
api_router.include_router(auth.router)
api_router.include_router(users.router)
api_router.include_router(scenes.router)
api_router.include_router(resources.router)
api_router.include_router(dialogues.router)
api_router.include_router(plans.router)
api_router.include_router(community.router)
api_router.include_router(reports.router)
api_router.include_router(admin.router)
