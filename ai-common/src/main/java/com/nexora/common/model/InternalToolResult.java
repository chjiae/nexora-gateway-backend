package com.nexora.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Internal unified tool result — maps to OpenAI tool role message and Anthropic tool_result content block.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalToolResult {
    private String toolCallId;
    private String toolUseId;
    private String content;
    private Map<String, Object> output;
    private boolean isError;
}
