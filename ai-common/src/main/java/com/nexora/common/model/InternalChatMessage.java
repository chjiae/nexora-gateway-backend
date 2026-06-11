package com.nexora.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Internal unified chat message — protocol-agnostic representation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalChatMessage {
    private String role;
    private String content;

    /** Tool calls from assistant (OpenAI: tool_calls, Anthropic: tool_use) */
    private List<InternalToolCall> toolCalls;

    /** Tool result from user/tool role (OpenAI: tool role, Anthropic: tool_result) */
    private String toolCallId;
    private String toolName;
    private List<Map<String, Object>> toolResults;

    /** Image content for vision models */
    private List<ImageContent> images;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageContent {
        private String url;
        private String base64Data;
        private String mediaType;
    }
}
