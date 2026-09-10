"""学习社区（F008）帖子 / 评论 / 点赞扩展表。"""
from datetime import datetime

from sqlalchemy import Boolean, DateTime, ForeignKey, Integer, String, Text, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base, TimestampMixin

# 帖子状态：1 正常 / 0 待审核（可扩展 2 违规）
POST_STATUS = (0, 1, 2)


class CommunityPost(Base, TimestampMixin):
    __tablename__ = "community_post"

    post_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.user_id"), index=True)
    title: Mapped[str] = mapped_column(String(100), nullable=False)
    content: Mapped[str] = mapped_column(Text, nullable=False)
    topic: Mapped[str] = mapped_column(String(30), index=True, default="学习心得")
    likes: Mapped[int] = mapped_column(Integer, default=0)
    comment_count: Mapped[int] = mapped_column(Integer, default=0)
    status: Mapped[int] = mapped_column(Integer, default=1, index=True, comment="1 正常 / 0 待审核 / 2 违规")
    is_top: Mapped[bool] = mapped_column(Boolean, default=False, index=True)
    create_time: Mapped[datetime] = mapped_column(DateTime, server_default=func.now(), index=True)

    author: Mapped["User"] = relationship(lazy="joined")  # noqa: F821
    comments: Mapped[list["CommunityComment"]] = relationship(
        back_populates="post", cascade="all, delete-orphan", order_by="CommunityComment.create_time"
    )


class CommunityComment(Base):
    __tablename__ = "community_comment"

    comment_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    post_id: Mapped[int] = mapped_column(Integer, ForeignKey("community_post.post_id"), index=True)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.user_id"), index=True)
    parent_id: Mapped[int | None] = mapped_column(Integer, nullable=True, comment="楼中楼回复，暂存根评论")
    content: Mapped[str] = mapped_column(Text, nullable=False)
    create_time: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())

    post: Mapped[CommunityPost] = relationship(back_populates="comments")
    author: Mapped["User"] = relationship(lazy="joined")  # noqa: F821


class PostLike(Base):
    """点赞记录：防止重复点赞。"""

    __tablename__ = "post_like"
    __table_args__ = (UniqueConstraint("user_id", "post_id", name="uq_like_user_post"),)

    like_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    post_id: Mapped[int] = mapped_column(Integer, ForeignKey("community_post.post_id"), index=True)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.user_id"), index=True)
    create_time: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())
