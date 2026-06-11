package com.nexora.platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AiApiKey {
    private Long id;
    private String keyHash;
    private String keyPrefix;
    private String keyName;
    private Long ownerUserId;
    private String ownerType;
    private Long ownerTenantId;
    private String status;
    private String allowedModels;
    private Integer rateLimitRpm;
    private Integer rateLimitTpm;
    private Long maxQuotaTokens;
    private Long usedQuotaTokens;
    private LocalDateTime expireTime;
    private LocalDateTime lastUsedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
