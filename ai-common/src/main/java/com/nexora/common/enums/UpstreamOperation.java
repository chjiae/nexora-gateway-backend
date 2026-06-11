package com.nexora.common.enums;

/**
 * Operation types for UrlBuilder to construct upstream URLs.
 * Each operation maps to a specific path segment on the upstream server.
 */
public enum UpstreamOperation {
    MODELS,
    CHAT_COMPLETIONS,
    MESSAGES,
    RESPONSES,
    EMBEDDINGS
}
