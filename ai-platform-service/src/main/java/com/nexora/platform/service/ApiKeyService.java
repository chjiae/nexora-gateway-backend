package com.nexora.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexora.platform.entity.AiApiKey;
import com.nexora.platform.mapper.AiApiKeyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private static final String KEY_PREFIX = "sk-nex-";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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

    public Page<AiApiKey> listApiKeys(Long userId, int page, int size) {
        LambdaQueryWrapper<AiApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiApiKey::getOwnerUserId, userId)
               .orderByDesc(AiApiKey::getCreateTime);
        return apiKeyMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Transactional
    public boolean disableApiKey(Long keyId, Long userId) {
        AiApiKey key = apiKeyMapper.selectById(keyId);
        if (key == null || !key.getOwnerUserId().equals(userId)) {
            return false;
        }
        key.setStatus("DISABLED");
        apiKeyMapper.updateById(key);
        log.info("Disabled API Key: id={}, userId={}", keyId, userId);
        return true;
    }

    public AiApiKey findByHash(String keyHash) {
        return apiKeyMapper.selectOne(
            new LambdaQueryWrapper<AiApiKey>().eq(AiApiKey::getKeyHash, keyHash)
        );
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
