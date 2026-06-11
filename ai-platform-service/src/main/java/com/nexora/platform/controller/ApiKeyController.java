package com.nexora.platform.controller;

import com.nexora.platform.dto.ApiResponse;
import com.nexora.platform.entity.AiApiKey;
import com.nexora.platform.service.ApiKeyService;
import com.nexora.platform.service.AuthService;
import com.nexora.platform.service.UserSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final AuthService authService;

    @PostMapping
    public ApiResponse<Map<String, Object>> create(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> body) {
        UserSession session = authService.validateToken(token);
        String keyName = body.getOrDefault("keyName", "Default Key");
        ApiKeyService.CreatedKey created = apiKeyService.createApiKey(
            session.getUserId(), session.getOwnerType(), session.getOwnerTenantId(), keyName);

        return ApiResponse.success(Map.of(
            "id", created.id(),
            "rawKey", created.rawKey(),
            "prefix", created.prefix(),
            "name", created.name(),
            "warning", "Store this key securely. It will not be shown again."
        ));
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int page) {
        UserSession session = authService.validateToken(token);
        List<AiApiKey> keys = apiKeyService.listApiKeys(session.getUserId(), page);
        return ApiResponse.success(Map.of("records", keys, "total", keys.size()));
    }

    @PutMapping("/{id}/disable")
    public ApiResponse<Void> disable(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        UserSession session = authService.validateToken(token);
        boolean ok = apiKeyService.disableApiKey(id, session.getUserId());
        if (!ok) {
            return ApiResponse.error(404, "API Key not found or not authorized");
        }
        return ApiResponse.success(null);
    }
}
