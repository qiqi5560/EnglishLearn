"""账号与鉴权接口。"""
import logging

from fastapi import APIRouter, HTTPException, status
from pydantic import BaseModel, ConfigDict, Field

from app.core.deps import CurrentUser, DbDep
from app.core.security import create_access_token
from app.models.user import User
from app.schemas import ok
from app.schemas.serializers import user_to_dict
from app.services.auth_service import (
    authenticate_by_password,
    check_sms_code,
    get_or_create_user,
    issue_sms_code,
    set_password,
    touch_login,
)
from app.services.plan_service import current_level_of, get_active_plan

logger = logging.getLogger("app.auth")
router = APIRouter(prefix="/auth", tags=["auth"])


def _phone_valid(phone: str) -> bool:
    return phone.isdigit() and 6 <= len(phone) <= 20


def _login_payload(db, user: User) -> dict:
    touch_login(user)
    db.commit()
    plan = get_active_plan(db, user)
    level = current_level_of(user, plan)
    token = create_access_token(user.user_id, user.user_role)
    return {"accessToken": token, "user": user_to_dict(user, level=level)}


class SendCodeIn(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)
    phone: str = Field(min_length=6, max_length=20)


@router.post("/send-code")
def send_code(body: SendCodeIn, db: DbDep):
    """发送登录/注册验证码（mock：固定验证码并打印到后端日志）。"""
    if not _phone_valid(body.phone):
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="手机号格式不正确")
    issue_sms_code(body.phone)
    return ok(message="验证码已发送（mock 固定为 123456，请查看后端日志）")


class LoginIn(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)
    phone: str = Field(min_length=6, max_length=20)
    code: str | None = None
    password: str | None = None
    nickname: str | None = None
    ageGroup: str | None = None


@router.post("/login")
def login(body: LoginIn, db: DbDep):
    """手机号验证码登录（未注册自动注册）或密码登录。"""
    if not _phone_valid(body.phone):
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="手机号格式不正确")

    if body.code is not None:  # 验证码登录
        if not check_sms_code(body.phone, body.code):
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="验证码错误或已过期，请重新获取")
        user = get_or_create_user(db, body.phone, nickname=body.nickname)
        if body.ageGroup in ("child", "k12", "adult", "senior"):
            user.age_group = body.ageGroup
    elif body.password is not None:  # 密码登录
        user = authenticate_by_password(db, body.phone, body.password)
        if user is None:
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="账号或密码错误")
    else:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="请提供验证码或密码")

    if user.status != 1:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="该账号已被停用，请联系管理员")
    logger.info("用户登录 phone=%s id=%s", body.phone, user.user_id)
    return ok(_login_payload(db, user), message="登录成功")


class RegisterIn(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)
    phone: str = Field(min_length=6, max_length=20)
    password: str = Field(min_length=6, max_length=64)
    nickname: str = Field(default="", max_length=50)
    ageGroup: str = "adult"


@router.post("/register")
def register(body: RegisterIn, db: DbDep):
    """密码方式注册（手机号已存在则直接走密码校验）。"""
    if not _phone_valid(body.phone):
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="手机号格式不正确")
    user = db.query(User).filter(User.phone == body.phone).first()
    if user is None:
        user = get_or_create_user(db, body.phone, nickname=body.nickname or None)
        set_password(user, body.password)
        user.age_group = body.ageGroup if body.ageGroup in ("child", "k12", "adult", "senior") else "adult"
        db.commit()
    elif user.password_hash is None:
        set_password(user, body.password)
        db.commit()
    elif not authenticate_by_password(db, body.phone, body.password):
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="该手机号已注册，密码错误")
    if user.status != 1:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="该账号已被停用")
    return ok(_login_payload(db, user), message="注册成功")


class AdminLoginIn(BaseModel):
    phone: str
    password: str


@router.post("/admin/login")
def admin_login(body: AdminLoginIn, db: DbDep):
    """管理员登录：仅 admin 角色可登录管理后台。"""
    user = authenticate_by_password(db, body.phone, body.password)
    if user is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="账号或密码错误")
    if user.user_role != "admin":
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="该账号不是管理员")
    if user.status != 1:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="该账号已被停用")
    logger.info("管理员登录 phone=%s id=%s", body.phone, user.user_id)
    return ok(_login_payload(db, user), message="登录成功")
