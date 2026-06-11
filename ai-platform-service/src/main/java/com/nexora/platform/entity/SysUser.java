package com.nexora.platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysUser {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String passwordHash;
    private String userType;
    private String ownerType;
    private Long ownerTenantId;
    private String avatarUrl;
    private String status;
    private String extraInfo;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
