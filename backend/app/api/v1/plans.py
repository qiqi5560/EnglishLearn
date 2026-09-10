"""个性化方案接口（F001）：入学测评、当前方案、每日任务闭环。"""
from fastapi import APIRouter, HTTPException, Query, status
from pydantic import BaseModel, ConfigDict, Field
from sqlalchemy import select

from app.core.deps import CurrentUser, DbDep
from app.models.plan import DailyTask, LearningPlan
from app.schemas import ok
from app.schemas.serializers import plan_to_dict, task_to_dict
from app.services.plan_service import (
    GOAL_LIST,
    build_tasks_for_date,
    ensure_today_tasks,
    get_active_plan,
    judge_level,
    upsert_plan,
)

router = APIRouter(prefix="/plans", tags=["plans"])


class EntranceAnswer(BaseModel):
    id: int
    text: str = ""


class EntranceTestIn(BaseModel):
    answers: list[EntranceAnswer] = Field(default_factory=list, min_length=1)
    targetGoal: str | None = None


@router.post("/entrance-test")
def submit_entrance_test(body: EntranceTestIn, db: DbDep, user: CurrentUser):
    """提交入学测评：判定等级并生成/更新学习方案与今日任务。"""
    goal = body.targetGoal or "兴趣"
    if goal not in GOAL_LIST:
        raise HTTPException(status_code=422, detail=f"目标取值应为：{'/'.join(GOAL_LIST)}")
    level, summary_text = judge_level([a.model_dump() for a in body.answers])
    plan = upsert_plan(db, user, level=level, target_goal=goal)
    tasks = ensure_today_tasks(db, user, plan)
    return ok(
        {
            "level": level,
            "summary": summary_text,
            "targetGoal": goal,
            "plan": plan_to_dict(plan),
            "tasks": [task_to_dict(t) for t in tasks],
        },
        message="测评完成，方案已生成",
    )


@router.get("/current")
def get_current_plan(db: DbDep, user: CurrentUser):
    plan = get_active_plan(db, user)
    return ok(plan_to_dict(plan) if plan else None)


@router.get("/level")
def get_current_level(db: DbDep, user: CurrentUser):
    plan = get_active_plan(db, user)
    return ok({"level": plan.level_current if plan else None})


class GeneratePlanIn(BaseModel):
    targetGoal: str
    dailyMinutes: int | None = Field(default=None, ge=5, le=180)


@router.post("/generate")
def generate_plan(body: GeneratePlanIn, db: DbDep, user: CurrentUser):
    """按目标重新生成方案（未测评用户按 A1 起评）。"""
    if body.targetGoal not in GOAL_LIST:
        raise HTTPException(status_code=422, detail=f"目标取值应为：{'/'.join(GOAL_LIST)}")
    old = get_active_plan(db, user)
    current_level = old.level_current if old else "A1"
    plan = upsert_plan(db, user, level=current_level, target_goal=body.targetGoal)
    tasks = ensure_today_tasks(db, user, plan)
    return ok(
        {"plan": plan_to_dict(plan), "tasks": [task_to_dict(t) for t in tasks]},
        message="方案已更新",
    )


@router.get("/today-tasks")
def get_today_tasks(db: DbDep, user: CurrentUser):
    plan = get_active_plan(db, user)
    if plan is None:
        return ok({"plan": None, "tasks": []})
    tasks = ensure_today_tasks(db, user, plan)
    return ok({"plan": plan_to_dict(plan), "tasks": [task_to_dict(t) for t in tasks]})


class TaskUpdateIn(BaseModel):
    done: bool = True


@router.put("/tasks/{task_id}")
def toggle_task(task_id: int, body: TaskUpdateIn, db: DbDep, user: CurrentUser):
    task = db.get(DailyTask, task_id)
    if task is None or task.user_id != user.user_id:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="任务不存在")
    task.done = 1 if body.done else 0
    db.commit()
    return ok(task_to_dict(task), message="任务状态已更新")
