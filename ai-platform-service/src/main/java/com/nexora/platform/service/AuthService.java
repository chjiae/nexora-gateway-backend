package com.nexora.platform.service;

import com.nexora.platform.dto.*;
import com.nexora.platform.entity.SysUser;
import com.nexora.platform.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final Map<String, UserSession> tokenStore = new HashMap<>();

    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.findByUsername(request.getUsername());

        // TODO: Use BCrypt in production
        if (user == null || !request.getPassword().equals(user.getPasswordHash())) {
            throw new RuntimeException("Invalid username or password");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("User account is not active");
        }

        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateLastLogin(user.getId(), user.getLastLoginTime());

        String token = UUID.randomUUID().toString();
        List<String> roles = resolveRoles(user);
        List<String> permissions = resolvePermissions(user);

        UserSession session = UserSession.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .userType(user.getUserType())
            .ownerType(user.getOwnerType())
            .ownerTenantId(user.getOwnerTenantId())
            .roles(roles)
            .permissions(permissions)
            .build();
        tokenStore.put(token, session);

        log.info("User logged in: userId={}, username={}", user.getId(), user.getUsername());

        return LoginResponse.builder()
            .token(token)
            .userId(user.getId())
            .username(user.getUsername())
            .userType(user.getUserType())
            .ownerType(user.getOwnerType())
            .ownerTenantId(user.getOwnerTenantId())
            .roles(roles)
            .permissions(permissions)
            .build();
    }

    public UserInfoResponse getCurrentUser(String token) {
        UserSession session = validateToken(token);
        SysUser user = userMapper.findById(session.getUserId());

        return UserInfoResponse.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .phone(user.getPhone())
            .userType(user.getUserType())
            .ownerType(user.getOwnerType())
            .ownerTenantId(user.getOwnerTenantId())
            .avatarUrl(user.getAvatarUrl())
            .status(user.getStatus())
            .roles(session.getRoles())
            .permissions(session.getPermissions())
            .build();
    }

    public MenuResponse getMenus(String token) {
        UserSession session = validateToken(token);

        List<MenuResponse.MenuItem> menus = new ArrayList<>();
        menus.add(MenuResponse.MenuItem.builder()
            .code("dashboard").name("Overview").path("/dashboard").icon("dashboard").build());

        if (session.getRoles().contains("PLATFORM_SUPER_ADMIN") || session.getRoles().contains("PLATFORM_ADMIN")) {
            menus.add(MenuResponse.MenuItem.builder()
                .code("tenant_mgmt").name("Tenants").path("/tenants").icon("building").build());
            menus.add(MenuResponse.MenuItem.builder()
                .code("provider_mgmt").name("Providers").path("/providers").icon("server").build());
            menus.add(MenuResponse.MenuItem.builder()
                .code("endpoint_mgmt").name("Endpoints").path("/endpoints").icon("plug").build());
            menus.add(MenuResponse.MenuItem.builder()
                .code("route_mgmt").name("Model Routes").path("/routes").icon("route").build());
            menus.add(MenuResponse.MenuItem.builder()
                .code("logs").name("Call Logs").path("/logs").icon("file-text").build());
            menus.add(MenuResponse.MenuItem.builder()
                .code("billing").name("Billing").path("/billing").icon("dollar").build());
        }

        if (session.getRoles().contains("TENANT_OWNER") || session.getRoles().contains("TENANT_ADMIN")) {
            menus.add(MenuResponse.MenuItem.builder()
                .code("tenant_providers").name("Providers").path("/providers").icon("server").build());
            menus.add(MenuResponse.MenuItem.builder()
                .code("tenant_endpoints").name("Endpoints").path("/endpoints").icon("plug").build());
            menus.add(MenuResponse.MenuItem.builder()
                .code("tenant_routes").name("Model Routes").path("/routes").icon("route").build());
        }

        menus.add(MenuResponse.MenuItem.builder()
            .code("apikey_mgmt").name("API Keys").path("/api-keys").icon("key").build());
        menus.add(MenuResponse.MenuItem.builder()
            .code("chat").name("Chat").path("/chat").icon("message").build());
        menus.add(MenuResponse.MenuItem.builder()
            .code("playground").name("Playground").path("/playground").icon("code").build());

        return MenuResponse.builder().menus(menus).build();
    }

    public void logout(String token) {
        tokenStore.remove(token);
    }

    public UserSession validateToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        UserSession session = tokenStore.get(token);
        if (session == null) {
            throw new RuntimeException("Invalid or expired token");
        }
        return session;
    }

    private List<String> resolveRoles(SysUser user) {
        return switch (user.getUserType()) {
            case "PLATFORM_ADMIN" -> List.of("PLATFORM_SUPER_ADMIN", "USER_DEVELOPER");
            case "TENANT_MEMBER" -> List.of("TENANT_ADMIN", "USER_DEVELOPER");
            case "PLATFORM_CUSTOMER" -> List.of("USER_DEVELOPER");
            case "TENANT_CUSTOMER" -> List.of("USER_DEVELOPER");
            default -> List.of("USER_DEVELOPER");
        };
    }

    private List<String> resolvePermissions(SysUser user) {
        return Arrays.asList("dashboard", "chat", "playground", "apikey", "providers", "endpoints", "routes", "logs", "billing");
    }
}
