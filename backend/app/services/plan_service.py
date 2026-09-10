"""个性化学习方案服务（F001）：入学测评判定、方案生成、每日任务闭环。"""
import json
import random
from datetime import date, datetime, timedelta, timezone

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.plan import DailyTask, LearningPlan
from app.models.resource import LearningResource
from app.models.scene import Scene
from app.models.user import User

LEVELS = ["A1", "A2", "B1", "B2", "C1", "C2"]

# 练习目标绑定：{"scene": 场景名} 绑定场景对话；{"resource": 素材标题关键词} 绑定跟读/精听；单词无模块不绑定
TaskBind = dict[str, str]

# 目标 → 每日任务模板（标题与前端原型展示一致，绑定值指向真实 scene/learning_resource）
_TASK_TEMPLATES: dict[str, list[tuple[str, str, int, TaskBind | None]]] = {
    "商务": [
        ("场景对话", "商务会议 · 10 分钟", 10, {"scene": "商务会议"}),
        ("跟读", "职场英语片段跟读", 15, {"resource": "职场"}),
        ("精听", "BBC 六分钟英语精听", 12, {"resource": "BBC"}),
        ("单词", "商务词汇复习 20 个", 8, None),
    ],
    "考试": [
        ("场景对话", "口语话题练习 · 10 分钟", 10, {"scene": "面试问答"}),
        ("跟读", "听力原文逐句跟读", 15, {"resource": "雅思"}),
        ("精听", "真题听力精听训练", 12, {"resource": "雅思"}),
        ("单词", "高频考试词汇复习", 8, None),
    ],
    "出国": [
        ("场景对话", "机场值机 · 10 分钟", 10, {"scene": "机场值机"}),
        ("跟读", "旅行英语片段跟读", 15, {"resource": "机场广播"}),
        ("精听", "机场广播精听训练", 12, {"resource": "机场广播"}),
        ("单词", "旅行词汇复习 20 个", 8, None),
    ],
    "兴趣": [
        ("场景对话", "餐厅点餐 · 10 分钟", 10, {"scene": "餐厅点餐"}),
        ("跟读", "老友记片段跟读", 15, {"resource": "老友记"}),
        ("精听", "BBC 六分钟英语精听", 12, {"resource": "BBC"}),
        ("单词", "日常词汇复习 20 个", 8, None),
    ],
}

GOAL_LIST = ("考试", "商务", "出国", "兴趣")


def _now() -> datetime:
    return datetime.now(timezone.utc).replace(tzinfo=None)


def _resolve_scene_id(db: Session, scene_name: str) -> int | None:
    """按场景名精确匹配上架场景；找不到返回 None（由前端兜底）。"""
    return db.scalar(
        select(Scene.scene_id).where(Scene.scene_name == scene_name, Scene.status == 1)
    )


def _resolve_resource_id(db: Session, keyword: str) -> int | None:
    """按标题关键词匹配上架素材；找不到返回 None（由前端兜底）。"""
    return db.scalar(
        select(LearningResource.resource_id)
        .where(LearningResource.title.contains(keyword), LearningResource.status == 1)
        .order_by(LearningResource.resource_id)
        .limit(1)
    )


def words_count(text: str) -> int:
    return len([w for w in text.split() if w])


def judge_level(answers: list[dict]) -> tuple[str, str]:
    """依据测评回答的文本丰富度做等级判定，返回 (level, 判定摘要)。

    真实场景应由大模型结合语音评测输出；此处启发式仅用于占位与联调。
    """
    if not answers:
        level = "A1"
    else:
        total_words = sum(words_count((a.get("text") or "") if isinstance(a, dict) else str(a)) for a in answers)
        avg = total_words / len(answers)
        if avg < 3:
            level = "A1"
        elif avg < 7:
            level = "A2"
        elif avg < 12:
            level = "B1"
        elif avg < 18:
            level = "B2"
        elif avg < 26:
            level = "C1"
        else:
            level = "C2"
    summary = f"基于本次 {len(answers)} 个问题的综合表现，初步判断为 {level} 水平。"
    return level, summary


def upsert_plan(db: Session, user: User, level: str, target_goal: str) -> LearningPlan:
    """创建或更新用户学习方案（单方案覆盖，随测评/调整更新）。"""
    plan = db.scalar(select(LearningPlan).where(LearningPlan.user_id == user.user_id))
    content = json.dumps(
        {
            "goal": target_goal,
            "stages": [
                {"stage": 1, "focus": "基础表达与场景开口", "tasks": 4},
                {"stage": 2, "focus": "流利度与句型丰富度提升", "tasks": 4},
                {"stage": 3, "focus": "实战综合演练与弱项专项", "tasks": 4},
            ],
        },
        ensure_ascii=False,
    )
    today = date.today()
    if plan is None:
        plan = LearningPlan(
            user_id=user.user_id,
            target_goal=target_goal,
            level_start=level,
            level_current=level,
            plan_content=content,
            plan_start=today,
            plan_status="active",
        )
        db.add(plan)
        db.flush()
    else:
        plan.target_goal = target_goal
        plan.level_start = plan.level_start or level
        plan.level_current = level
        plan.plan_content = content
        plan.plan_status = "active"
        plan.plan_start = plan.plan_start or today
        plan.update_time = _now()
    db.commit()
    db.refresh(plan)
    return plan


def build_tasks_for_date(db: Session, user: User, plan: LearningPlan, target_date: date) -> list[DailyTask]:
    goal = plan.target_goal if plan.target_goal in _TASK_TEMPLATES else "兴趣"
    templates = _TASK_TEMPLATES[goal]
    tasks: list[DailyTask] = []
    for t_type, title, minutes, bind in templates:
        if db.scalar(
            select(DailyTask).where(
                DailyTask.user_id == user.user_id,
                DailyTask.task_date == target_date,
                DailyTask.title == title,
            )
        ):
            continue
        scene_id = _resolve_scene_id(db, bind["scene"]) if bind and "scene" in bind else None
        resource_id = _resolve_resource_id(db, bind["resource"]) if bind and "resource" in bind else None
        task = DailyTask(
            user_id=user.user_id,
            plan_id=plan.plan_id,
            task_type=t_type,
            title=title,
            duration_min=minutes,
            scene_id=scene_id,
            resource_id=resource_id,
            done=False,
            task_date=target_date,
        )
        db.add(task)
        tasks.append(task)
    if tasks:
        db.commit()
    return tasks


def ensure_today_tasks(db: Session, user: User, plan: LearningPlan | None) -> list[DailyTask]:
    today = date.today()
    tasks = list(
        db.scalars(
            select(DailyTask)
            .where(DailyTask.user_id == user.user_id, DailyTask.task_date == today)
            .order_by(DailyTask.task_id)
        )
    )
    if not tasks and plan is not None:
        tasks = build_tasks_for_date(db, user, plan, today)
    return tasks


def get_active_plan(db: Session, user: User) -> LearningPlan | None:
    return db.scalar(
        select(LearningPlan)
        .where(LearningPlan.user_id == user.user_id, LearningPlan.plan_status == "active")
    )


def current_level_of(user: User, plan: LearningPlan | None) -> str | None:
    return plan.level_current if plan else None
