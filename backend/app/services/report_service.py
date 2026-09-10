"""学习报表服务（F007）：成长曲线 + 能力雷达 + 统计聚合。"""
import json
from datetime import date, timedelta

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models.conversation import AssessmentRecord, ConversationSession
from app.models.plan import LearningPlan
from app.models.study import StudyRecord
from app.models.user import User

RADAR_INDICATORS = [
    {"name": "发音", "key": "pron_score"},
    {"name": "流利度", "key": "fluency_score"},
    {"name": "反应", "key": "reaction_score"},
    {"name": "自然度", "key": "natural_score"},
]


def overview(db: Session, user: User, *, weeks: int = 8) -> dict:
    """聚合学习报表：近 N 周成长曲线、最新四维能力、累计统计。"""
    today = date.today()
    since = today - timedelta(weeks=weeks - 1, days=today.weekday())  # 对齐周一起始

    # 1) 成长曲线：按学习日聚合平均综合得分
    rows = db.execute(
        select(StudyRecord.learn_date, func.avg(StudyRecord.score))
        .where(StudyRecord.user_id == user.user_id, StudyRecord.score.is_not(None))
        .group_by(StudyRecord.learn_date)
        .order_by(StudyRecord.learn_date)
    ).all()
    by_day: dict[date, float] = {d: float(avg) for d, avg in rows}

    # 2) 累计统计
    total_minutes = db.scalar(
        select(func.coalesce(func.sum(StudyRecord.duration_min), 0)).where(
            StudyRecord.user_id == user.user_id
        )
    )
    total_sessions = db.scalar(
        select(func.count()).select_from(ConversationSession).where(
            ConversationSession.user_id == user.user_id,
            ConversationSession.session_status == "finished",
        )
    )
    active_days = db.scalar(
        select(func.count(func.distinct(StudyRecord.learn_date))).where(
            StudyRecord.user_id == user.user_id
        )
    )
    plan = db.scalar(
        select(LearningPlan).where(
            LearningPlan.user_id == user.user_id, LearningPlan.plan_status == "active"
        )
    )
    avg_score = db.scalar(
        select(func.avg(StudyRecord.score)).where(
            StudyRecord.user_id == user.user_id, StudyRecord.score.is_not(None)
        )
    )

    # 3) 能力雷达：取最近一次评测的四维分数
    latest_assess = db.scalar(
        select(AssessmentRecord)
        .join(ConversationSession, AssessmentRecord.session_id == ConversationSession.session_id)
        .where(ConversationSession.user_id == user.user_id)
        .order_by(AssessmentRecord.assess_id.desc())
        .limit(1)
    )
    if latest_assess is not None:
        radar_values = [
            float(getattr(latest_assess, item["key"])) for item in RADAR_INDICATORS
        ]
    else:
        radar_values = [0.0] * len(RADAR_INDICATORS)

    # 4) 最近完成会话（练习小结入口）
    recent = db.scalars(
        select(ConversationSession)
        .where(
            ConversationSession.user_id == user.user_id,
            ConversationSession.session_status == "finished",
        )
        .order_by(ConversationSession.end_time.desc())
        .limit(5)
    ).all()
    recent_list = []
    for s in recent:
        summary = {}
        if s.ai_summary:
            try:
                summary = json.loads(s.ai_summary)
            except json.JSONDecodeError:
                summary = {}
        recent_list.append(
            {
                "sessionId": s.session_id,
                "sceneName": s.scene.scene_name if s.scene else "自由对话",
                "total": summary.get("total", 0),
                "endTime": s.end_time.isoformat(sep=" ") if s.end_time else None,
            }
        )

    return {
        "growth": {
            "dates": [d.isoformat() for d in sorted(by_day)],
            "scores": [by_day[d] for d in sorted(by_day)],
            "startDate": since.isoformat(),
        },
        "radar": {
            "indicators": [{"name": i["name"], "max": 100} for i in RADAR_INDICATORS],
            "values": radar_values,
        },
        "stats": {
            "totalMinutes": int(total_minutes or 0),
            "totalSessions": int(total_sessions or 0),
            "activeDays": int(active_days or 0),
            "avgScore": round(float(avg_score), 1) if avg_score is not None else None,
            "currentLevel": plan.level_current if plan else None,
        },
        "recentSessions": recent_list,
    }
