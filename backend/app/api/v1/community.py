"""学习社区接口（F008）：帖子、点赞、评论、话题。"""
from fastapi import APIRouter, HTTPException, Query, status
from pydantic import BaseModel, ConfigDict, Field
from sqlalchemy import func, select

from app.core.deps import CurrentUser, DbDep
from app.models.community import CommunityComment, CommunityPost, PostLike
from app.schemas import ok
from app.schemas.serializers import comment_to_dict, post_to_dict

router = APIRouter(prefix="/community", tags=["community"])

DEFAULT_TOPICS = ["学习心得", "结伴练习", "资源分享", "提问求助"]


def _liked_set(db: DbDep, user: CurrentUser, post_ids: list[int]) -> set[int]:
    if not post_ids:
        return set()
    rows = db.scalars(select(PostLike.post_id).where(PostLike.user_id == user.user_id, PostLike.post_id.in_(post_ids))).all()
    return set(rows)


@router.get("/topics")
def topics(db: DbDep):
    existing = db.scalars(select(CommunityPost.topic).distinct()).all()
    merged = list(dict.fromkeys([t for t in existing if t] + DEFAULT_TOPICS))
    return ok(merged)


@router.get("/posts")
def list_posts(
    db: DbDep,
    user: CurrentUser,
    topic: str | None = Query(default=None),
    page: int = Query(default=1, ge=1),
    pageSize: int = Query(default=10, ge=1, le=50),
):
    cond = [CommunityPost.status == 1]
    if topic:
        cond.append(CommunityPost.topic == topic)
    total = db.scalar(select(func.count()).select_from(CommunityPost).where(*cond)) or 0
    rows = db.scalars(
        select(CommunityPost)
        .where(*cond)
        .order_by(CommunityPost.is_top.desc(), CommunityPost.create_time.desc())
        .offset((page - 1) * pageSize)
        .limit(pageSize)
    ).all()
    liked = _liked_set(db, user, [p.post_id for p in rows])
    return ok(
        {
            "list": [post_to_dict(p, liked=p.post_id in liked) for p in rows],
            "total": total,
        }
    )


class PostCreateIn(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)
    title: str = Field(min_length=1, max_length=100)
    content: str = Field(min_length=1)
    topic: str = "学习心得"


@router.post("/posts")
def create_post(body: PostCreateIn, db: DbDep, user: CurrentUser):
    post = CommunityPost(
        user_id=user.user_id,
        title=body.title.strip(),
        content=body.content.strip(),
        topic=body.topic.strip() or "学习心得",
        status=1,
    )
    db.add(post)
    db.commit()
    db.refresh(post)
    return ok(post_to_dict(post), message="发布成功")


@router.get("/posts/{post_id}")
def post_detail(post_id: int, db: DbDep, user: CurrentUser):
    post = db.get(CommunityPost, post_id)
    if post is None or (post.status != 1 and post.user_id != user.user_id):
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="帖子不存在")
    comments = list(
        db.scalars(
            select(CommunityComment)
            .where(CommunityComment.post_id == post_id)
            .order_by(CommunityComment.create_time)
        )
    )
    liked = (
        db.scalar(
            select(PostLike).where(PostLike.post_id == post_id, PostLike.user_id == user.user_id)
        )
        is not None
    )
    return ok(
        {
            "post": post_to_dict(post, liked=liked),
            "comments": [comment_to_dict(c) for c in comments],
        }
    )


@router.post("/posts/{post_id}/like")
def toggle_like(post_id: int, db: DbDep, user: CurrentUser):
    post = db.get(CommunityPost, post_id)
    if post is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="帖子不存在")
    existing = db.scalar(
        select(PostLike).where(PostLike.post_id == post_id, PostLike.user_id == user.user_id)
    )
    if existing:
        db.delete(existing)
        post.likes = max(0, post.likes - 1)
        liked = False
    else:
        db.add(PostLike(post_id=post_id, user_id=user.user_id))
        post.likes += 1
        liked = True
    db.commit()
    return ok({"liked": liked, "likes": post.likes})


class CommentIn(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)
    content: str = Field(min_length=1, max_length=500)


@router.post("/posts/{post_id}/comments")
def add_comment(post_id: int, body: CommentIn, db: DbDep, user: CurrentUser):
    post = db.get(CommunityPost, post_id)
    if post is None or post.status != 1:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="帖子不存在")
    comment = CommunityComment(
        post_id=post_id, user_id=user.user_id, content=body.content.strip()
    )
    post.comment_count += 1
    db.add(comment)
    db.commit()
    db.refresh(comment)
    return ok(comment_to_dict(comment), message="评论成功")


@router.delete("/posts/{post_id}")
def delete_post(post_id: int, db: DbDep, user: CurrentUser):
    post = db.get(CommunityPost, post_id)
    if post is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="帖子不存在")
    if post.user_id != user.user_id and user.user_role != "admin":
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="只能删除自己的帖子")
    db.delete(post)  # 评论级联删除
    db.commit()
    return ok(message="删除成功")
