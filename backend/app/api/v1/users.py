"""学习者个人中心接口：资料、密码、监护人绑定（F009）。"""
from fastapi import APIRouter, HTTPException, status
from pydantic import BaseModel, ConfigDict

from app.core.deps import CurrentUser, DbDep
from app.models.user import User
from app.schemas import ok
from app.schemas.serializers import user_to_dict
from app.services.auth_service import check_sms_code, get_or_create_user, verify_password
from app.services.plan_service import current_level_of, get_active_plan

router = APIRouter(prefix="/users", tags=["users"])


def _user_payload(db, user: User) -> dict:
    plan = get_active_plan(db, user)
    return user_to_dict(user, level=current_level_of(user, plan))


@router.get("/me")
def get_me(db: DbDep, user: CurrentUser):
    return ok(_user_payload(db, user))


class UpdateMeIn(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)
    nickname: str | None = None
    avatarUrl: str | None = None
    ageGroup: str | None = None
    guardianId: int | None = None


@router.put("/me")
def update_me(body: UpdateMeIn, db: DbDep, user: CurrentUser):
    if body.nickname is not None:
        if not body.nickname.strip():
            raise HTTPException(status_code=422, detail="昵称不能为空")
        user.nickname = body.nickname.strip()
    if body.avatarUrl is not None:
        user.avatar_url = body.avatarUrl
    if body.ageGroup is not None:
        if body.ageGroup not in ("child", "k12", "adult", "senior"):
            raise HTTPException(status_code=422, detail="年龄段取值不合法")
        user.age_group = body.ageGroup
    if body.guardianId is not None:
        guardian = db.get(User, body.guardianId)
        if guardian is None or guardian.user_role != "guardian":
            raise HTTPException(status_code=422, detail="指定的监护人不存在")
        user.guardian_id = guardian.user_id
    db.commit()
    return ok(_user_payload(db, user), message="资料已更新")


class PasswordIn(BaseModel):
    oldPassword: str
    newPassword: str = ...


@router.put("/me/password")
def change_password(body: PasswordIn, db: DbDep, user: CurrentUser):
    if len(body.newPassword) < 6:
        raise HTTPException(status_code=422, detail="新密码至少 6 位")
    if user.password_hash and not verify_password(body.oldPassword, user.password_hash):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="原密码错误")
    from app.core.security import hash_password

    user.password_hash = hash_password(body.newPassword)
    db.commit()
    return ok(message="密码修改成功")


class BindGuardianIn(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)
    guardianPhone: str
    code: str


@router.post("/me/bind-guardian")
def bind_guardian(body: BindGuardianIn, db: DbDep, user: CurrentUser):
    """少儿/中老年用户绑定监护人账号（监护人手机号 + 监护人验证码）。"""
    guardian = get_or_create_user(db, body.guardianPhone, role="guardian", nickname="监护人")
    if not check_sms_code(body.guardianPhone, body.code):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="监护人验证码错误或已过期")
    guardian.user_role = "guardian"
    user.guardian_id = guardian.user_id
    db.commit()
    return ok(_user_payload(db, user), message="绑定监护人成功")
