"""ORM → 前端 JSON 序列化（字段使用 camelCase，与前端 TS 类型对齐）。"""
from app.models.community import CommunityComment, CommunityPost
from app.models.conversation import AssessmentRecord, ConversationMessage, ConversationSession
from app.models.plan import DailyTask, LearningPlan
from app.models.resource import LearningResource
from app.models.scene import Scene
from app.models.sysconfig import SysConfig
from app.models.user import User
from app.schemas.common import iso


def user_to_dict(user: User, level: str | None = None) -> dict:
    return {
        "userId": user.user_id,
        "phone": user.phone,
        "nickname": user.nickname,
        "avatarUrl": user.avatar_url,
        "ageGroup": user.age_group,
        "role": user.user_role,
        "guardianId": user.guardian_id,
        "status": user.status,
        "level": level,
        "registerTime": iso(user.register_time),
        "lastLoginTime": iso(user.last_login_time),
    }


def scene_to_dict(scene: Scene) -> dict:
    return {
        "id": scene.scene_id,
        "name": scene.scene_name,
        "category": scene.scene_category,
        "desc": scene.scene_desc,
        "level": scene.level_scope,
        "role": (scene.role_setting or {}).get("role", "") if isinstance(scene.role_setting, dict) else "",
        "roleSetting": scene.role_setting,
        "coverUrl": scene.cover_url,
        "status": scene.status,
    }


def resource_to_dict(r: LearningResource) -> dict:
    return {
        "id": r.resource_id,
        "title": r.title,
        "type": r.res_type,
        "category": r.category,
        "level": r.level,
        "mediaUrl": r.media_url,
        "durationSec": r.duration_sec,
        "status": r.status,
    }


def message_to_dict(m: ConversationMessage) -> dict:
    return {
        "id": m.message_id,
        "speaker": m.speaker,
        "contentEn": m.content_en,
        "contentZh": m.content_zh,
        "audioUrl": m.audio_url,
        "time": iso(m.msg_time),
    }


def session_to_dict(s: ConversationSession, messages: list[ConversationMessage]) -> dict:
    return {
        "sessionId": s.session_id,
        "sceneId": s.scene_id,
        "sceneName": s.scene.scene_name if s.scene else "",
        "mode": s.mode,
        "status": s.session_status,
        "startTime": iso(s.start_time),
        "endTime": iso(s.end_time),
        "durationSec": s.duration_sec,
        "messages": [message_to_dict(m) for m in messages],
    }


def assessment_to_dict(a: AssessmentRecord) -> dict:
    return {
        "pron": float(a.pron_score),
        "fluency": float(a.fluency_score),
        "reaction": float(a.reaction_score),
        "natural": float(a.natural_score),
        "grammarFeedback": a.grammar_feedback,
        "phonemeIssues": a.phoneme_issues,
        "betterExpression": a.better_expression,
    }


def plan_to_dict(p: LearningPlan) -> dict:
    return {
        "planId": p.plan_id,
        "targetGoal": p.target_goal,
        "levelStart": p.level_start,
        "levelCurrent": p.level_current,
        "planContent": p.plan_content,
        "planStart": iso(p.plan_start),
        "planStatus": p.plan_status,
        "updateTime": iso(p.update_time),
    }


def task_to_dict(t: DailyTask) -> dict:
    return {
        "taskId": t.task_id,
        "type": t.task_type,
        "title": t.title,
        "durationMin": t.duration_min,
        "sceneId": t.scene_id,
        "resourceId": t.resource_id,
        "done": bool(t.done),
        "taskDate": iso(t.task_date),
    }


def post_to_dict(p: CommunityPost, liked: bool = False) -> dict:
    return {
        "id": p.post_id,
        "author": p.author.nickname or "用户",
        "authorId": p.user_id,
        "title": p.title,
        "content": p.content,
        "topic": p.topic,
        "likes": p.likes,
        "comments": p.comment_count,
        "status": p.status,
        "isTop": bool(p.is_top),
        "liked": liked,
        "createTime": iso(p.create_time),
    }


def comment_to_dict(c: CommunityComment) -> dict:
    return {
        "id": c.comment_id,
        "postId": c.post_id,
        "author": c.author.nickname or "用户",
        "content": c.content,
        "createTime": iso(c.create_time),
    }


def config_to_dict(c: SysConfig) -> dict:
    return {
        "key": c.key,
        "value": c.value,
        "remark": c.remark,
    }
