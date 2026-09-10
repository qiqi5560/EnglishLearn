"""账号服务：手机号自动注册/登录、密码注册、登录态维护。"""
import logging
from datetime import datetime, timezone

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.security import hash_password, verify_password
from app.models.user import User

logger = logging.getLogger("app.auth")

# 内存验证码：{phone: code}（仅演示；多实例/生产应使用 Redis 并设 TTL）
_sms_store: dict[str, str] = {}


def issue_sms_code(phone: str) -> str:
    from app.core.config import settings

    code = settings.MOCK_SMS_CODE
    _sms_store[phone] = code
    # mock：不接第三方短信，仅输出日志便于演示
    logger.info("[mock-sms] 向 %s 发送验证码：%s", phone, code)
    return code


def check_sms_code(phone: str, code: str) -> bool:
    expected = _sms_store.get(phone)
    return bool(expected) and expected == code


def get_or_create_user(db: Session, phone: str, *, role: str = "learner", nickname: str | None = None) -> User:
    user = db.scalar(select(User).where(User.phone == phone))
    if user is None:
        user = User(
            phone=phone,
            user_role=role,
            age_group="adult",
            nickname=nickname or f"学习者{phone[-4:]}",
            status=1,
        )
        db.add(user)
        db.flush()
        logger.info("注册新账号 phone=%s role=%s id=%s", phone, role, user.user_id)
    return user


def authenticate_by_password(db: Session, phone: str, password: str) -> User | None:
    user = db.scalar(select(User).where(User.phone == phone))
    if user is None or not user.password_hash:
        return None
    return user if verify_password(password, user.password_hash) else None


def set_password(user: User, password: str) -> None:
    user.password_hash = hash_password(password)


def touch_login(user: User) -> None:
    user.last_login_time = datetime.now(timezone.utc).replace(tzinfo=None)
