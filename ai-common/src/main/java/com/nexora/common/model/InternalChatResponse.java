package com.nexora.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Internal unified non-streaming chat response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalChatResponse {
    private String id;
    private String model;
    private InternalChatMessage message;
    private String finishReason;
    private InternalUsage usage;
    private long latencyMs;
    private String systemFingerprint;
    private List<InternalChatMessage> choices;
}
