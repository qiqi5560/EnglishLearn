"""种子数据：首次启动时灌入与前端原型一致的演示数据。

迁移来源：frontend/src/mock/index.ts 的场景/资源/帖子/任务/报表数据。
包含：6 个场景、6 个学习资源、3 篇社区帖子、演示账号（含报表历史）、系统配置。
"""
import json
import logging
from datetime import date, datetime, timedelta, timezone

from sqlalchemy import func, select

from app.core.security import hash_password
from app.db.session import SessionLocal
from app.models.community import CommunityComment, CommunityPost, PostLike  # noqa: F401
from app.models.conversation import AssessmentRecord, ConversationMessage, ConversationSession
from app.models.plan import DailyTask, LearningPlan
from app.models.resource import LearningResource
from app.models.scene import Scene
from app.models.study import StudyRecord
from app.models.sysconfig import SysConfig
from app.models.user import User
from app.services.plan_service import build_tasks_for_date

logger = logging.getLogger("app.seed")

SCENES = [
    {"name": "餐厅点餐", "desc": "在餐厅点餐、询问推荐与结账", "category": "生活", "level": "A2", "role": "服务员"},
    {"name": "商务会议", "desc": "参与英文商务会议并发表观点", "category": "工作", "level": "B2", "role": "同事"},
    {"name": "机场值机", "desc": "办理登机手续、托运行李", "category": "出行", "level": "A2", "role": "地勤"},
    {"name": "课堂讨论", "desc": "围绕主题进行课堂讨论与提问", "category": "学习", "level": "B1", "role": "同学"},
    {"name": "酒店入住", "desc": "办理入住、咨询设施与服务", "category": "出行", "level": "A2", "role": "前台"},
    {"name": "面试问答", "desc": "模拟英文面试自我介绍与问答", "category": "工作", "level": "B2", "role": "面试官"},
]

RESOURCES = [
    {"title": "BBC 六分钟英语 · 职场礼仪", "type": "新闻", "category": "职场", "level": "B1", "duration": 360},
    {"title": "Friends 老友记 · 第一季片段", "type": "剧集", "category": "生活", "level": "A2", "duration": 240},
    {"title": "TED · 高效学习的秘密", "type": "播客", "category": "学习", "level": "B2", "duration": 600},
    {"title": "小王子 · 有声书第一章", "type": "有声书", "category": "生活", "level": "A2", "duration": 480},
    {"title": "雅思口语 · Part 2 话题训练", "type": "新闻", "category": "雅思", "level": "B2", "duration": 300},
    {"title": "机场广播 · 值机通知", "type": "新闻", "category": "出行", "level": "A2", "duration": 180},
]

POSTS = [
    {
        "phone": "13800138000",
        "topic": "学习心得",
        "title": "坚持跟读 30 天，我的发音变化",
        "content": "每天跟读老友记片段 15 分钟，第 30 天发音评分从 62 涨到了 81，分享一下方法……",
        "likes": 128,
    },
    {
        "phone": "13700000000",
        "topic": "结伴练习",
        "title": "寻找口语搭子，每晚 8 点",
        "content": "B1 水平，想练商务英语，有没有一起结伴练习的小伙伴？",
        "likes": 45,
    },
    {
        "phone": "13600000000",
        "topic": "学习心得",
        "title": "AI 场景对话真的太适合社恐了",
        "content": "在餐厅点餐场景里练了 20 分钟，不用怕尴尬，出错也会温柔纠正……",
        "likes": 96,
    },
]

POST_COMMENTS = {
    "坚持跟读 30 天，我的发音变化": [
        ("13600000000", "太棒了！请问你用的是什么评分工具呀？"),
        ("13700000000", "同感，坚持最重要，一起加油！"),
    ],
    "寻找口语搭子，每晚 8 点": [
        ("13800138000", "举手！我 A2，也想练商务方向"),
        ("13600000000", "每晚 8 点有点早，能改 9 点吗"),
    ],
    "AI 场景对话真的太适合社恐了": [
        ("13800138000", "哈哈哈同社恐，我练的餐厅场景"),
        ("13700000000", "错了也不尴尬，这点最棒"),
    ],
}


def _now() -> datetime:
    return datetime.now(timezone.utc).replace(tzinfo=None)


def _seed_users(db) -> dict[str, User]:
    """返回 {phone: user} 并创建各类演示账号。"""
    pwd = hash_password("123456")
    admin_pwd = hash_password("admin123")
    specs = [
        {"phone": "13900000000", "nick": "管理员", "role": "admin", "age": "adult", "pwd": admin_pwd},
        {"phone": "13800138000", "nick": "Momo", "role": "learner", "age": "adult", "pwd": pwd},
        {"phone": "13700000000", "nick": "Leo", "role": "learner", "age": "adult", "pwd": pwd},
        {"phone": "13600000000", "nick": "Cici", "role": "learner", "age": "adult", "pwd": pwd},
        {"phone": "13300000000", "nick": "小明", "role": "learner", "age": "child", "pwd": pwd},
        {"phone": "13500000000", "nick": "王妈妈", "role": "guardian", "age": "adult", "pwd": pwd},
    ]
    result: dict[str, User] = {}
    for spec in specs:
        user = User(
            phone=spec["phone"],
            password_hash=spec["pwd"],
            nickname=spec["nick"],
            user_role=spec["role"],
            age_group=spec["age"],
            status=1,
            register_time=_now(),
        )
        db.add(user)
        result[spec["phone"]] = user
    db.flush()
    logger.info("seed: 创建演示账号 %s 个", len(specs))
    return result


def _seed_scenes(db) -> None:
    for s in SCENES:
        db.add(
            Scene(
                scene_name=s["name"],
                scene_category=s["category"],
                scene_desc=s["desc"],
                level_scope=s["level"],
                role_setting={"role": s["role"], "script": f"{s['name']}情境对话练习"},
                status=1,
            )
        )
    db.flush()


def _seed_resources(db) -> None:
    for r in RESOURCES:
        db.add(
            LearningResource(
                title=r["title"],
                res_type=r["type"],
                category=r["category"],
                level=r["level"],
                duration_sec=r["duration"],
                status=1,
            )
        )
    db.flush()


def _seed_posts(db, users: dict[str, User]) -> None:
    for spec in POSTS:
        author = users[spec["phone"]]
        post = CommunityPost(
            user_id=author.user_id,
            title=spec["title"],
            content=spec["content"],
            topic=spec["topic"],
            likes=spec["likes"],
            status=1,
        )
        db.add(post)
        db.flush()
        comments = POST_COMMENTS[spec["title"]]
        for c_phone, text in comments:
            c_user = users[c_phone]
            db.add(
                CommunityComment(
                    post_id=post.post_id, user_id=c_user.user_id, content=text
                )
            )
        post.comment_count = len(comments)
    db.flush()


def _seed_plans_and_learning(db, users: dict[str, User]) -> None:
    """为演示学习者创建学习方案 + 历史学习记录 + 已完成会话，支撑报表/看板展示。"""
    today = date.today()
    plan_specs = [
        ("13800138000", "兴趣", "A2", 81),
        ("13700000000", "商务", "B1", 68),
        ("13600000000", "考试", "B2", 74),
    ]
    plans: dict[str, LearningPlan] = {}
    for phone, goal, level, _ in plan_specs:
        plan = LearningPlan(
            user_id=users[phone].user_id,
            target_goal=goal,
            level_start=level,
            level_current=level,
            plan_content=json.dumps(
                {
                    "goal": goal,
                    "stages": [
                        {"stage": 1, "focus": "基础表达与场景开口", "tasks": 4},
                        {"stage": 2, "focus": "流利度与句型丰富度提升", "tasks": 4},
                        {"stage": 3, "focus": "实战综合演练与弱项专项", "tasks": 4},
                    ],
                },
                ensure_ascii=False,
            ),
            plan_start=today - timedelta(days=30),
            plan_status="active",
        )
        db.add(plan)
        plans[phone] = plan
    db.flush()

    momo = users["13800138000"]
    build_tasks_for_date(db, momo, plans["13800138000"], today)

    # Momo 近 8 周学习记录：模拟成长曲线（分数 58 → 81，与前端原型一致）
    score_seq = [58, 63, 61, 70, 74, 78, 79, 81]
    now = _now()
    saturday = today - timedelta(days=(today.weekday() + 2) % 7 + 7 * 0)
    # 取最近的周六
    for i, score in enumerate(score_seq):
        d = saturday - timedelta(weeks=7 - i)
        db.add(
            StudyRecord(
                user_id=momo.user_id,
                action_type="scenario",
                duration_min=12 + (i % 3) * 2,
                score=score,
                learn_date=d,
            )
        )

    # Momo 最近两场已完成会话（含评测与小结），支撑报表“最近练习”与能力雷达
    recent = [
        (now - timedelta(days=2), 76.5, {"pron": 76, "fluency": 68, "reaction": 72, "natural": 64}),
        (now - timedelta(days=1), 81.0, {"pron": 84, "fluency": 79, "reaction": 82, "natural": 78}),
    ]
    scene_ids = {s.scene_name: s for s in db.scalars(select(Scene)).all()}
    for end_dt, total, dims in recent:
        session = ConversationSession(
            user_id=momo.user_id,
            scene_id=scene_ids["餐厅点餐"].scene_id,
            mode="scenario",
            start_time=end_dt - timedelta(minutes=12),
            end_time=end_dt,
            duration_sec=720,
            session_status="finished",
        )
        db.add(session)
        db.flush()
        db.add(
            ConversationMessage(
                session_id=session.session_id, speaker="ai",
                content_en="Good evening! Welcome to our restaurant. How many people are in your party?",
                content_zh="晚上好！欢迎光临本餐厅，请问一共几位？",
                msg_time=end_dt - timedelta(minutes=12),
            )
        )
        db.add(
            ConversationMessage(
                session_id=session.session_id, speaker="user",
                content_en="Hello, a table for two please. Could you recommend today's special?",
                content_zh="你好，两位。能推荐一下今天的特色菜吗？",
                msg_time=end_dt - timedelta(minutes=11),
            )
        )
        assessment = AssessmentRecord(
            session_id=session.session_id,
            pron_score=dims["pron"],
            fluency_score=dims["fluency"],
            reaction_score=dims["reaction"],
            natural_score=dims["natural"],
            grammar_feedback="句子结构清晰，注意形容词顺序。",
            phoneme_issues=[{"word": "special", "phoneme": "/ˈspeʃl/", "note": "元音弱读"}],
            assess_time=end_dt - timedelta(minutes=11),
        )
        db.add(assessment)
        payload = {
            "sessionId": session.session_id,
            "total": total,
            "dimensions": dims,
            "highlights": ["表达自然，能围绕场景主动开口并回应对方。"],
            "improvements": ["连读与弱读可继续打磨。"],
            "suggestions": ["回听本场录音并复练 2 次。"],
            "corrections": [],
            "feedbackText": f"综合表现不错，最终得分 {total}。",
            "durationSec": 720,
        }
        session.ai_summary = json.dumps(payload, ensure_ascii=False)
    db.flush()


def _seed_configs(db) -> None:
    db.add(
        SysConfig(
            key="system_config",
            value={
                "speech": {"slowSpeed": 0.8, "normalSpeed": 1.2},
                "recommend": {"content": True, "collab": True, "model": True},
                "audit": {"content": True, "manual": True},
            },
            remark="系统全局配置",
        )
    )
    db.flush()


def run_seed() -> bool:
    """空库时灌入种子数据；返回是否执行了初始化。"""
    with SessionLocal() as db:
        if db.scalar(select(func.count()).select_from(Scene)):
            return False
        users = _seed_users(db)
        _seed_scenes(db)
        _seed_resources(db)
        _seed_posts(db, users)
        _seed_plans_and_learning(db, users)
        _seed_configs(db)
        db.commit()
        logger.info("seed: 演示数据初始化完成")
        return True
