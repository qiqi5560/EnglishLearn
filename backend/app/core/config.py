"""全局配置：从环境变量 / .env 读取。"""
from functools import lru_cache
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

BASE_DIR = Path(__file__).resolve().parents[2]  # backend/


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=BASE_DIR / ".env", env_file_encoding="utf-8", extra="ignore"
    )

    APP_NAME: str = "英语口语训练系统 API"
    DEBUG: bool = True
    API_PREFIX: str = "/api"
    HOST: str = "0.0.0.0"
    PORT: int = 8080
    CORS_ORIGINS: str = "http://localhost:5173,http://127.0.0.1:5173"

    # 数据库（默认 SQLite：backend/data/app.db）
    DATABASE_URL: str = "sqlite:///./data/app.db"

    # JWT
    SECRET_KEY: str = "dev-secret-change-me-in-production"
    JWT_ALGORITHM: str = "HS256"
    JWT_EXPIRE_MINUTES: int = 60 * 24 * 7  # 7 天

    # 短信验证码（mock：固定值并打印到日志，不接第三方短信）
    MOCK_SMS_CODE: str = "123456"
    SMS_EXPIRE_MINUTES: int = 5

    # LLM 提供方（本次仅 mock；扩展真实模型时新增 provider 并切换）
    LLM_PROVIDER: str = "mock"


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
