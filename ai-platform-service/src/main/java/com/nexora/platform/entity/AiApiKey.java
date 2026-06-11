package com.nexora.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_api_key")
public class AiApiKey {
    @TableId(type = IdType.AUTO)
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
