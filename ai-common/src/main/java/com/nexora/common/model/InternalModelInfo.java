package com.nexora.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Internal model information for /v1/models response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalModelInfo {
    private String id;
    private String object;
    private long created;
    private String ownedBy;
    private List<String> supportedProtocols;
    private boolean supportStream;
    private boolean supportTools;
}
