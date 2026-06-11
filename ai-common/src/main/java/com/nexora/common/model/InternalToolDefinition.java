package com.nexora.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Internal unified tool/function definition.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalToolDefinition {
    private String type;
    private String name;
    private String description;
    private Map<String, Object> parameters;
}
