package com.nexora.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal error representation — can be converted to OpenAI or Anthropic error format.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalError {
    private String code;
    private String message;
    private String type;
    private String param;
    private int statusCode;
    private String requestId;
}
