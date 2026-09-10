"""LLM 适配层。

本次仅实现规则式占位 Provider；后续接入真实大模型时：
1. 新增实现（如 OpenAIProvider），实现 base.LLMProvider 协议；
2. 在 app/services/llm/factory.py 的 get_llm_provider 中注册；
3. 通过配置 LLM_PROVIDER 切换，路由层零改动。
"""
from app.services.llm.factory import get_llm_provider

__all__ = ["get_llm_provider"]
