"""对话会话服务（F002/F003）：会话生命周期与消息收发。"""
import json
from datetime import datetime, timezone

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.conversation import AssessmentRecord, ConversationMessage, ConversationSession
from app.models.scene import Scene
from app.models.study import StudyRecord
from app.models.user import User
from app.services.llm import get_llm_provider

_MAX_CONTEXT = 6  # 注入 Provider 的最近上下文条数


def _now() -> datetime:
    return datetime.now(timezone.utc).replace(tzinfo=None)


def _recent_messages(db: Session, session_id: int, limit: int = _MAX_CONTEXT) -> list[ConversationMessage]:
    return list(
        db.scalars(
            select(ConversationMessage)
            .where(ConversationMessage.session_id == session_id)
            .order_by(ConversationMessage.message_id.desc())
            .limit(limit)
        )
    )[::-1]


def start_session(db: Session, user: User, scene: Scene | None, mode: str = "scenario") -> ConversationSession:
    session = ConversationSession(
        user_id=user.user_id,
        scene_id=scene.scene_id if scene else None,
        mode=mode,
        session_status="ongoing",
    )
    db.add(session)
    db.flush()

    provider = get_llm_provider()
    if scene is not None:
        role = (scene.role_setting or {}).get("role", "") if isinstance(scene.role_setting, dict) else ""
        en, zh = provider.opening(scene.scene_name, scene.scene_desc, role)
    else:
        en, zh = provider.opening("自由对话", "", "AI 教练")
    opening = ConversationMessage(
        session_id=session.session_id,
        speaker="ai",
        content_en=en,
        content_zh=zh,
    )
    db.add(opening)
    db.commit()
    db.refresh(session)
    return session


def send_message(
    db: Session, session: ConversationSession, content: str
) -> tuple[ConversationMessage, ConversationMessage, dict]:
    """落库用户消息 → Provider 生成回复并落库 → 启发式评测。返回 (用户消息, AI消息, liveScores)。"""
    if session.session_status != "ongoing":
        raise ValueError("会话已结束，无法继续发送消息")

    scene = session.scene
    user_msg = ConversationMessage(
        session_id=session.session_id, speaker="user", content_en=content.strip()
    )
    db.add(user_msg)
    db.flush()

    provider = get_llm_provider()
    history = _recent_messages(db, session.session_id)
    role = (scene.role_setting or {}).get("role", "") if scene and isinstance(scene.role_setting, dict) else ""
    en, zh = provider.reply(
        scene_name=scene.scene_name if scene else "自由对话",
        scene_desc=scene.scene_desc if scene else "",
        role=role,
        history=history,
        user_input=content,
    )
    ai_msg = ConversationMessage(
        session_id=session.session_id, speaker="ai", content_en=en, content_zh=zh
    )
    db.add(ai_msg)
    db.flush()

    eval_result = provider.evaluate(content)
    record = AssessmentRecord(
        session_id=session.session_id,
        message_id=user_msg.message_id,
        pron_score=eval_result.pron,
        fluency_score=eval_result.fluency,
        reaction_score=eval_result.reaction,
        natural_score=eval_result.natural,
        grammar_feedback=eval_result.grammar_feedback,
        phoneme_issues=eval_result.phoneme_issues,
        better_expression=eval_result.better_expression,
    )
    db.add(record)
    db.commit()

    live_scores = {
        "pron": eval_result.pron,
        "fluency": eval_result.fluency,
        "reaction": eval_result.reaction,
        "natural": eval_result.natural,
    }
    return user_msg, ai_msg, live_scores


def finish_session(db: Session, session: ConversationSession) -> dict:
    """结束会话并生成小结：汇总四维均分、写 ai_summary、沉淀学习记录。"""
    if session.session_status == "finished":
        return json.loads(session.ai_summary) if session.ai_summary else {"summary": {}}

    now = _now()
    duration_sec = max(0, int((now - session.start_time).total_seconds())) if session.start_time else 0
    session.end_time = now
    session.duration_sec = duration_sec
    session.session_status = "finished"

    messages = list(
        db.scalars(
            select(ConversationMessage)
            .where(ConversationMessage.session_id == session.session_id)
            .order_by(ConversationMessage.msg_time)
        )
    )
    assessments = list(
        db.scalars(
            select(AssessmentRecord)
            .where(AssessmentRecord.session_id == session.session_id)
            .order_by(AssessmentRecord.assess_id)
        )
    )
    provider = get_llm_provider()
    result = provider.summarize(messages, [])
    if assessments:
        def mean(key: str) -> float:
            vals = [float(getattr(a, key)) for a in assessments]
            return round(sum(vals) / len(vals), 1)

        result.total = round(mean("pron_score") * 0.35 + mean("fluency_score") * 0.25
                             + mean("reaction_score") * 0.2 + mean("natural_score") * 0.2, 1)
        result.pron, result.fluency = mean("pron_score"), mean("fluency_score")
        result.reaction, result.natural = mean("reaction_score"), mean("natural_score")

    payload = {
        "sessionId": session.session_id,
        "total": result.total,
        "dimensions": {
            "pron": result.pron,
            "fluency": result.fluency,
            "reaction": result.reaction,
            "natural": result.natural,
        },
        "highlights": result.highlights,
        "improvements": result.improvements,
        "suggestions": result.suggestions,
        "corrections": result.corrections,
        "feedbackText": result.feedback_text,
        "durationSec": duration_sec,
    }
    session.ai_summary = json.dumps(payload, ensure_ascii=False)

    # 沉淀学习记录（联动 F007 报表）
    learn_date = now.date()
    duration_min = max(1, (duration_sec + 59) // 60)
    db.add(
        StudyRecord(
            user_id=session.user_id,
            session_id=session.session_id,
            action_type="scenario",
            duration_min=duration_min,
            score=result.total,
            learn_date=learn_date,
        )
    )
    db.commit()
    return payload


def get_session_summary(db: Session, session: ConversationSession) -> dict:
    if not session.ai_summary:
        raise LookupError("会话尚未结束，暂无小结")
    return json.loads(session.ai_summary)
