package com.nexora.platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Tenant {
    private Long id;
    private String tenantName;
    private String tenantCode;
    private String status;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private Integer maxUsers;
    private Integer maxProviders;
    private String extraConfig;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
