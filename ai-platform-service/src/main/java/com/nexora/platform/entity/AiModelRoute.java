package com.nexora.platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AiModelRoute {
    private Long id;
    private String modelAlias;
    private Long providerId;
    private Long endpointId;
    private String upstreamModel;
    private String upstreamProtocol;
    private String clientProtocolSupport;
    private Integer priority;
    private Integer weight;
    private String fallbackGroup;
    private Boolean supportStream;
    private Boolean supportTools;
    private Boolean supportVision;
    private Boolean supportReasoning;
    private Integer maxInputTokens;
    private Integer maxOutputTokens;
    private Long priceConfigId;
    private String ownerType;
    private Long ownerTenantId;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
