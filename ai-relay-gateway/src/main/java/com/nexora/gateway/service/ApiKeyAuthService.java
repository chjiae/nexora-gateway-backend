package com.nexora.gateway.service;

import com.nexora.common.model.ProviderEndpointSnapshot;
import com.nexora.common.model.ResolvedModelRoute;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight API Key authentication — all data from Redis snapshots.
 * No database queries on the hot path.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyAuthService {

    private final Map<String, AuthResult> localCache = new ConcurrentHashMap<>();

    /**
     * Validate an API key and return the associated user/tenant info.
     */
    public Mono<AuthResult> validate(String rawKey) {
        String keyHash = hashKey(rawKey);

        AuthResult cached = localCache.get(keyHash);
        if (cached != null) {
            if (!"ACTIVE".equals(cached.status)) {
                return Mono.error(new AuthException("API key is disabled"));
            }
            return Mono.just(cached);
        }

        // In production, look up from Redis cache snapshot.
        // For MVP with seed data, return a default auth result.
        return Mono.error(new AuthException("API key not found in cache. Config publish required."));
    }

    public void updateCache(Map<String, AuthResult> newEntries) {
        localCache.putAll(newEntries);
    }

    public static String hashKey(String rawKey) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(rawKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash key", e);
        }
    }

    public record AuthResult(
        String keyHash,
        String keyPrefix,
        Long userId,
        Long tenantId,
        String ownerType,
        String status,
        String allowedModels
    ) {}

    public static class AuthException extends RuntimeException {
        public AuthException(String message) {
            super(message);
        }
    }
}
