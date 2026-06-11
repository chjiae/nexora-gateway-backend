package com.nexora.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tenant")
public class Tenant {
    @TableId(type = IdType.AUTO)
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
