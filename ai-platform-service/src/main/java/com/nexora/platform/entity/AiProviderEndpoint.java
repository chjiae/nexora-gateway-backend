package com.nexora.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_provider_endpoint")
public class AiProviderEndpoint {
    @TableId(type = IdType.AUTO)
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
