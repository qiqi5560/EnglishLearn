"""对话会话接口（F002/F003）：创建会话、收发消息、实时评测、结束与小结。"""
import json

from fastapi import APIRouter, HTTPException, Query, status
from pydantic import BaseModel, ConfigDict
from sqlalchemy import select

from app.core.deps import CurrentUser, DbDep
from app.models.conversation import ConversationMessage, ConversationSession
from app.models.scene import Scene
from app.schemas import ok
from app.schemas.serializers import message_to_dict, session_to_dict
from app.services.dialogue_service import finish_session, send_message, start_session

router = APIRouter(prefix="/dialogues", tags=["dialogues"])


def _owned_session(db: DbDep, session_id: int, user: CurrentUser) -> ConversationSession:
    session = db.get(ConversationSession, session_id)
    if session is None or session.user_id != user.user_id:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="会话不存在")
    return session


class SessionCreateIn(BaseModel):
    sceneId: int | None = None
    mode: str = "scenario"


@router.post("/sessions")
def create_session(body: SessionCreateIn, db: DbDep, user: CurrentUser):
    scene = None
    if body.sceneId is not None:
        scene = db.get(Scene, body.sceneId)
        if scene is None or scene.status != 1:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="场景不存在或已下架")
    session = start_session(db, user, scene, mode=body.mode or "scenario")
    messages = list(
        db.scalars(
            select(ConversationMessage)
            .where(ConversationMessage.session_id == session.session_id)
            .order_by(ConversationMessage.msg_time)
        )
    )
    return ok(session_to_dict(session, messages), message="会话已创建")


@router.get("/sessions")
def my_sessions(
    db: DbDep,
    user: CurrentUser,
    status_flag: str | None = Query(default=None, alias="status"),
    page: int = Query(default=1, ge=1),
    pageSize: int = Query(default=20, ge=1, le=100),
):
    cond = [ConversationSession.user_id == user.user_id]
    if status_flag:
        cond.append(ConversationSession.session_status == status_flag)
    rows = db.scalars(
        select(ConversationSession)
        .where(*cond)
        .order_by(ConversationSession.session_id.desc())
        .offset((page - 1) * pageSize)
        .limit(pageSize)
    ).all()
    items = []
    for s in rows:
        summary = {}
        if s.ai_summary:
            try:
                summary = json.loads(s.ai_summary)
            except json.JSONDecodeError:
                summary = {}
        items.append(
            {
                "sessionId": s.session_id,
                "sceneName": s.scene.scene_name if s.scene else "自由对话",
                "mode": s.mode,
                "status": s.session_status,
                "total": summary.get("total"),
                "durationSec": s.duration_sec,
                "startTime": s.start_time.isoformat(sep=" ") if s.start_time else None,
                "endTime": s.end_time.isoformat(sep=" ") if s.end_time else None,
            }
        )
    return ok({"list": items, "total": len(items)})


@router.get("/sessions/{session_id}")
def session_messages(session_id: int, db: DbDep, user: CurrentUser):
    session = _owned_session(db, session_id, user)
    messages = list(
        db.scalars(
            select(ConversationMessage)
            .where(ConversationMessage.session_id == session_id)
            .order_by(ConversationMessage.msg_time)
        )
    )
    return ok({"session": session_to_dict(session, messages)})


class MessageIn(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)
    content: str


@router.post("/sessions/{session_id}/messages")
def post_message(session_id: int, body: MessageIn, db: DbDep, user: CurrentUser):
    session = _owned_session(db, session_id, user)
    if not body.content.strip():
        raise HTTPException(status_code=422, detail="消息内容不能为空")
    try:
        user_msg, ai_msg, live_scores = send_message(db, session, body.content)
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail=str(exc))
    return ok(
        {
            "userMessage": message_to_dict(user_msg),
            "aiMessage": message_to_dict(ai_msg),
            "liveScores": live_scores,
        },
        message="ok",
    )


@router.post("/sessions/{session_id}/finish")
def end_session(session_id: int, db: DbDep, user: CurrentUser):
    session = _owned_session(db, session_id, user)
    payload = finish_session(db, session)
    return ok(payload, message="会话已结束，小结已生成")


@router.get("/sessions/{session_id}/summary")
def session_summary(session_id: int, db: DbDep, user: CurrentUser):
    session = _owned_session(db, session_id, user)
    from app.services.dialogue_service import get_session_summary

    try:
        payload = get_session_summary(db, session)
    except LookupError as exc:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail=str(exc))
    return ok(payload)
