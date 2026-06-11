package com.nexora.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_provider_config")
public class AiProviderConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String providerName;
    private String providerType;
    private String displayName;
    private String ownerType;
    private Long ownerTenantId;
    private Boolean enabled;
    private String extraConfig;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
