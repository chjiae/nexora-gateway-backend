package com.nexora.platform.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {
    private Long userId;
    private String username;
    private String userType;
    private String ownerType;
    private Long ownerTenantId;
    private List<String> roles;
    private List<String> permissions;
}
