package com.nexora.platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AiProviderEndpoint {
    private Long id;
    private Long providerId;
    private String endpointName;
    private String baseUrl;
    private String pathPrefix;
    private String supportedProtocols;
    private String defaultProtocol;
    private String authType;
    private String apiKeyEncrypted;
    private String extraHeaders;
    private Integer timeoutMs;
    private Integer connectTimeoutMs;
    private Integer readTimeoutMs;
    private Integer maxRetries;
    private Integer priority;
    private Integer weight;
    private String ownerType;
    private Long ownerTenantId;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
