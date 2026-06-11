package com.nexora.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private Long userId;
    private String username;
    private String email;
    private String phone;
    private String userType;
    private String ownerType;
    private Long ownerTenantId;
    private String avatarUrl;
    private String status;
    private List<String> roles;
    private List<String> permissions;
}
