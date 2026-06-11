package com.nexora.common.enums;

/**
 * Provider type — identifies the vendor/service provider.
 * Provider does NOT dictate protocol; protocol is chosen at the route level.
 */
public enum ProviderType {
    OPENAI,
    ANTHROPIC,
    DEEPSEEK,
    OPENROUTER,
    QWEN,
    GEMINI,
    LITELLM,
    ONE_API,
    INTERNAL_GATEWAY,
    CUSTOM
}
