package com.nexora.common.model;

import com.nexora.common.enums.ProtocolType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Resolved routing result for a model alias request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolvedModelRoute {
    private String modelAlias;
    private String tenantId;
    private String providerId;
    private String endpointId;
    private String upstreamModel;
    private ProtocolType upstreamProtocol;
    private List<ProtocolType> clientProtocolSupport;
    private int priority;
    private int weight;
    private String fallbackGroup;
    private boolean supportStream;
    private boolean supportTools;
    private boolean supportVision;
    private boolean supportReasoning;
    private int maxInputTokens;
    private int maxOutputTokens;
    private boolean enabled;

    /** The endpoint snapshot associated with this route */
    private ProviderEndpointSnapshot endpoint;
}
