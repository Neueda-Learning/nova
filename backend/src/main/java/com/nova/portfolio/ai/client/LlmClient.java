package com.nova.portfolio.ai.client;

/** Abstraction over a chat-completion style LLM provider so callers can swap providers. */
public interface LlmClient {

    /** Whether a provider key is configured; callers use this to decide on a fallback path. */
    boolean isAvailable();

    /** Single-turn completion; throws AiServiceException only for a genuinely failed call. */
    String complete(String systemPrompt, String userPrompt);
}
