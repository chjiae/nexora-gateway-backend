package com.nexora.common.util;

import com.nexora.common.enums.ProtocolType;
import com.nexora.common.enums.UpstreamOperation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UrlBuilderTest {

    @Test
    void openAIBaseUrlWithChatCompletions() {
        String url = UrlBuilder.build("https://api.openai.com/v1",
                ProtocolType.OPENAI, UpstreamOperation.CHAT_COMPLETIONS);
        assertEquals("https://api.openai.com/v1/chat/completions", url);
        assertFalse(url.contains("/v1/v1"), "Must not duplicate /v1");
    }

    @Test
    void anthropicBaseUrlWithMessages() {
        String url = UrlBuilder.build("https://api.anthropic.com",
                ProtocolType.ANTHROPIC, UpstreamOperation.MESSAGES);
        assertEquals("https://api.anthropic.com/v1/messages", url);
    }

    @Test
    void deepseekAnthropicEndpointWithMessages() {
        String url = UrlBuilder.build("https://api.deepseek.com/anthropic",
                ProtocolType.ANTHROPIC, UpstreamOperation.MESSAGES);
        assertEquals("https://api.deepseek.com/anthropic/v1/messages", url);
    }

    @Test
    void gatewayBaseUrlWithChatCompletions() {
        String url = UrlBuilder.build("https://gateway.example.com",
                ProtocolType.OPENAI, UpstreamOperation.CHAT_COMPLETIONS);
        assertEquals("https://gateway.example.com/v1/chat/completions", url);
    }

    @Test
    void gatewayBaseUrlWithMessages() {
        String url = UrlBuilder.build("https://gateway.example.com",
                ProtocolType.ANTHROPIC, UpstreamOperation.MESSAGES);
        assertEquals("https://gateway.example.com/v1/messages", url);
    }

    @Test
    void modelsOperationOnOpenAI() {
        String url = UrlBuilder.build("https://api.openai.com/v1",
                ProtocolType.OPENAI, UpstreamOperation.MODELS);
        assertEquals("https://api.openai.com/v1/models", url);
    }

    @Test
    void modelsOperationWithoutV1InBase() {
        String url = UrlBuilder.build("https://api.anthropic.com",
                ProtocolType.ANTHROPIC, UpstreamOperation.MODELS);
        assertEquals("https://api.anthropic.com/v1/models", url);
    }

    @Test
    void withPathPrefix() {
        String url = UrlBuilder.build("https://gateway.example.com", "custom-prefix",
                ProtocolType.OPENAI, UpstreamOperation.CHAT_COMPLETIONS);
        assertEquals("https://gateway.example.com/custom-prefix/v1/chat/completions", url);
    }

    @Test
    void trailingSlashBaseUrlNormalized() {
        String url = UrlBuilder.build("https://api.openai.com/v1/",
                ProtocolType.OPENAI, UpstreamOperation.CHAT_COMPLETIONS);
        assertEquals("https://api.openai.com/v1/chat/completions", url);
    }

    @Test
    void emptyBaseUrlProducesRelativePath() {
        String url = UrlBuilder.build("", ProtocolType.OPENAI, UpstreamOperation.CHAT_COMPLETIONS);
        assertTrue(url.startsWith("/v1/"));
    }

    @Test
    void embeddingsOperationOnOpenAI() {
        String url = UrlBuilder.build("https://api.openai.com/v1",
                ProtocolType.EMBEDDINGS, UpstreamOperation.EMBEDDINGS);
        assertEquals("https://api.openai.com/v1/embeddings", url);
    }
}
