package com.nexora.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Internal unified chat request — the canonical form after protocol adaptation.
 * All client protocols (OpenAI, Anthropic) are normalized into this model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalChatRequest {
    private String requestId;
    private String model;
    private List<InternalChatMessage> messages;
    private String system;

    private Double temperature;
    private Double topP;
    private Integer maxTokens;
    private Integer maxOutputTokens;

    private boolean stream;
    private String streamMode;

    private List<InternalToolDefinition> tools;
    private String toolChoice;
    private boolean parallelToolCalls;

    private String stop;
    private List<String> stopSequences;

    private Double presencePenalty;
    private Double frequencyPenalty;
    private Integer seed;
    private String user;

    private Map<String, Object> metadata;

    /** The client protocol that produced this request (for response adaptation) */
    private String clientProtocol;

    /** Thinking/reasoning budget (Claude extended thinking) */
    private Integer thinkingBudgetTokens;
}
