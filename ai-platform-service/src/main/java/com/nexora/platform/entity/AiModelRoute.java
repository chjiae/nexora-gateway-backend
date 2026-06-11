package com.nexora.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_model_route")
public class AiModelRoute {
    @TableId(type = IdType.AUTO)
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
