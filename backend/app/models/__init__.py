"""聚合导入全部 ORM 模型，保证 Base.metadata 完整。"""
from app.models.community import CommunityComment, CommunityPost, PostLike
from app.models.conversation import AssessmentRecord, ConversationMessage, ConversationSession
from app.models.plan import DailyTask, LearningPlan
from app.models.resource import LearningResource
from app.models.scene import Scene
from app.models.study import StudyRecord
from app.models.sysconfig import SysConfig
from app.models.user import User

__all__ = [
    "User",
    "Scene",
    "ConversationSession",
    "ConversationMessage",
    "AssessmentRecord",
    "LearningResource",
    "LearningPlan",
    "DailyTask",
    "StudyRecord",
    "CommunityPost",
    "CommunityComment",
    "PostLike",
    "SysConfig",
]
