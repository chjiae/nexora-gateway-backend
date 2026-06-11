package com.nexora.platform.service;

import com.nexora.platform.entity.AiApiKey;
import com.nexora.platform.mapper.AiApiKeyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private static final String KEY_PREFIX = "sk-nex-";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int PAGE_SIZE = 20;

    private final AiApiKeyMapper apiKeyMapper;

    @Transactional
    public CreatedKey createApiKey(Long userId, String ownerType, Long ownerTenantId, String keyName) {
        String rawKey = generateRawKey();
        String keyHash = hashKey(rawKey);
        String keyPrefix = rawKey.substring(0, Math.min(12, rawKey.length()));

        AiApiKey apiKey = new AiApiKey();
        apiKey.setKeyHash(keyHash);
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setKeyName(keyName);
        apiKey.setOwnerUserId(userId);
        apiKey.setOwnerType(ownerType);
        apiKey.setOwnerTenantId(ownerTenantId);
        apiKey.setStatus("ACTIVE");
        apiKeyMapper.insert(apiKey);

        log.info("Created API Key: id={}, prefix={}, userId={}", apiKey.getId(), keyPrefix, userId);
        return new CreatedKey(apiKey.getId(), rawKey, keyPrefix, keyName);
    }

    public List<AiApiKey> listApiKeys(Long userId, int page) {
        int offset = (page - 1) * PAGE_SIZE;
        return apiKeyMapper.findByUserId(userId, offset, PAGE_SIZE);
    }

    @Transactional
    public boolean disableApiKey(Long keyId, Long userId) {
        AiApiKey key = apiKeyMapper.findById(keyId);
        if (key == null || !key.getOwnerUserId().equals(userId)) {
            return false;
        }
        apiKeyMapper.updateStatus(keyId, "DISABLED");
        log.info("Disabled API Key: id={}, userId={}", keyId, userId);
        return true;
    }

    public AiApiKey findByHash(String keyHash) {
        return apiKeyMapper.findByHash(keyHash);
    }

    public static String hashKey(String rawKey) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash API key", e);
        }
    }

    private String generateRawKey() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record CreatedKey(Long id, String rawKey, String prefix, String name) {}
}
