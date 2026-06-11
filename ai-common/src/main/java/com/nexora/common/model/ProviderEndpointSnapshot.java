package com.nexora.common.model;

import com.nexora.common.enums.ProtocolType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Lightweight snapshot of a ProviderEndpoint for relay hot-path use (from Redis cache).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderEndpointSnapshot {
    private String endpointId;
    private String providerId;
    private String providerName;
    private String endpointName;
    private String baseUrl;
    private String pathPrefix;
    private List<ProtocolType> supportedProtocols;
    private ProtocolType defaultProtocol;
    private String authType;
    private String apiKeyEncrypted;
    private Map<String, String> extraHeaders;
    private int timeoutMs;
    private int connectTimeoutMs;
    private int readTimeoutMs;
    private int maxRetries;
    private int priority;
    private int weight;
    private boolean enabled;
    private String healthStatus;
    private String ownerType;
    private String ownerTenantId;
}
