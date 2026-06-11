package com.nexora.common.enums;

/**
 * Protocol type for upstream communication.
 * Protocol is NOT bound to Provider or Endpoint — it's determined by ModelRoute.upstreamProtocol.
 */
public enum ProtocolType {
    OPENAI,
    ANTHROPIC,
    RESPONSES,
    EMBEDDINGS,
    GEMINI
}
