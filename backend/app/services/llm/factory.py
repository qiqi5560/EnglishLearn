"""Provider 工厂：按配置返回对应实现（后续接入真实模型在此注册）。"""
from app.core.config import settings
from app.services.llm.base import LLMProvider
from app.services.llm.mock_provider import MockProvider


def get_llm_provider() -> LLMProvider:
    provider = settings.LLM_PROVIDER.lower()
    if provider == "mock":
        return MockProvider()
    # 未来扩展示例：
    # if provider == "openai":
    #     return OpenAIProvider(settings.OPENAI_API_KEY, settings.OPENAI_BASE_URL, ...)
    raise ValueError(f"未知 LLM_PROVIDER: {settings.LLM_PROVIDER}")
