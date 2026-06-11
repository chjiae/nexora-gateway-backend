package com.nexora.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal unified token usage — protocol-agnostic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalUsage {
    private long inputTokens;
    private long outputTokens;
    private long totalTokens;

    /** Cache-related tokens (Anthropic) */
    private long cacheCreationInputTokens;
    private long cacheReadInputTokens;

    public static InternalUsage empty() {
        return InternalUsage.builder().build();
    }
}
