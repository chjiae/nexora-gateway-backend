package com.nexora.common.util;

import com.nexora.common.enums.ProtocolType;
import com.nexora.common.enums.UpstreamOperation;

/**
 * Unified URL builder for upstream provider endpoints.
 * <p>
 * Avoids common pitfalls: /v1/v1 duplication, missing /v1, double slashes,
 * lost pathPrefix, wrong path for protocol.
 * </p>
 *
 * <pre>
 * Input:  baseUrl=https://api.openai.com/v1, op=CHAT_COMPLETIONS, protocol=OPENAI
 * Output: https://api.openai.com/v1/chat/completions
 *
 * Input:  baseUrl=https://api.anthropic.com, op=MESSAGES, protocol=ANTHROPIC
 * Output: https://api.anthropic.com/v1/messages
 *
 * Input:  baseUrl=https://api.deepseek.com/anthropic, op=MESSAGES, protocol=ANTHROPIC
 * Output: https://api.deepseek.com/anthropic/v1/messages
 *
 * Input:  baseUrl=https://gateway.example.com, op=CHAT_COMPLETIONS, protocol=OPENAI
 * Output: https://gateway.example.com/v1/chat/completions
 * </pre>
 */
public final class UrlBuilder {

    private UrlBuilder() {
        // utility class
    }

    /**
     * Build the full upstream URL for a given endpoint, protocol, and operation.
     */
    public static String build(String baseUrl, String pathPrefix, ProtocolType protocol, UpstreamOperation operation) {
        String url = stripTrailingSlash(baseUrl);
        String prefix = normalizePrefix(pathPrefix);
        if (!prefix.isEmpty()) {
            url = url + "/" + prefix;
        }

        String protocolVersion = resolveProtocolVersion(protocol);
        String operationPath = resolveOperationPath(operation);

        // Avoid /v1/v1 duplication: if baseUrl already ends with /v1, skip adding it again
        if (url.endsWith("/" + protocolVersion)) {
            url = url + "/" + operationPath;
        } else {
            url = url + "/" + protocolVersion + "/" + operationPath;
        }
        return url;
    }

    /**
     * Build without pathPrefix.
     */
    public static String build(String baseUrl, ProtocolType protocol, UpstreamOperation operation) {
        return build(baseUrl, null, protocol, operation);
    }

    private static String resolveProtocolVersion(ProtocolType protocol) {
        return switch (protocol) {
            case GEMINI -> "v1beta";
            default -> "v1";
        };
    }

    private static String resolveOperationPath(UpstreamOperation operation) {
        return switch (operation) {
            case MODELS -> "models";
            case CHAT_COMPLETIONS -> "chat/completions";
            case MESSAGES -> "messages";
            case RESPONSES -> "responses";
            case EMBEDDINGS -> "embeddings";
        };
    }

    private static String stripTrailingSlash(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        // Collapse consecutive slashes (preserve :// in scheme), then strip trailing
        return url.replaceAll("(?<!:)/{2,}", "/").replaceAll("/+$", "");
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        String normalized = prefix.trim();
        // Collapse consecutive slashes first, then strip leading and trailing
        normalized = normalized.replaceAll("/{2,}", "/");
        normalized = normalized.replaceAll("^/+", "");
        normalized = normalized.replaceAll("/+$", "");
        return normalized;
    }
}
