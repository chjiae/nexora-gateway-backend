package com.nexora.platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AiProviderConfig {
    private Long id;
    private String providerName;
    private String providerType;
    private String displayName;
    private String ownerType;
    private Long ownerTenantId;
    private Boolean enabled;
    private String extraConfig;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
