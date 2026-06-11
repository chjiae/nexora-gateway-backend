package com.nexora.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Internal unified tool call — maps to OpenAI tool_calls and Anthropic tool_use.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalToolCall {
    private String id;
    private String type;
    private String name;
    private String functionName;
    private Map<String, Object> arguments;
    private int index;
}
