package com.nexora.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Internal unified streaming chat chunk — represents one SSE delta.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalChatChunk {
    private String id;
    private String model;
    private int index;
    private String deltaRole;
    private String deltaContent;
    private String finishReason;
    private InternalToolCall toolCall;
    private InternalUsage usage;
    private Map<String, Object> raw;
}
