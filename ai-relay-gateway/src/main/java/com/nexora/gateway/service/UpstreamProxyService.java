package com.nexora.gateway.service;

import com.nexora.common.enums.ProtocolType;
import com.nexora.common.enums.UpstreamOperation;
import com.nexora.common.model.*;
import com.nexora.common.util.UrlBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Proxies requests to upstream providers using WebClient (Reactor Netty).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpstreamProxyService {

    private final WebClient.Builder webClientBuilder;

    /**
     * Send a non-streaming chat completion request upstream.
     */
    public Mono<InternalChatResponse> proxyNonStream(
            InternalChatRequest request, ResolvedModelRoute route) {

        ProviderEndpointSnapshot endpoint = route.getEndpoint();
        String url = UrlBuilder.build(endpoint.getBaseUrl(), endpoint.getPathPrefix(),
                route.getUpstreamProtocol(), UpstreamOperation.CHAT_COMPLETIONS);

        Instant start = Instant.now();

        WebClient client = webClientBuilder
                .baseUrl(endpoint.getBaseUrl())
                .build();

        return client.post()
                .uri(buildUri(endpoint, route.getUpstreamProtocol(), UpstreamOperation.CHAT_COMPLETIONS))
                .headers(h -> addAuthHeader(h, endpoint))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(convertToUpstream(request, route))
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(Map.class)
                            .map(body -> parseUpstreamResponse(body, route, start));
                    } else {
                        return response.bodyToMono(String.class)
                            .flatMap(errorBody -> Mono.error(
                                new UpstreamException(response.statusCode().value(), errorBody)));
                    }
                });
    }

    /**
     * Proxy a streaming request — returns SSE flux.
     */
    public Flux<String> proxyStream(
            InternalChatRequest request, ResolvedModelRoute route) {

        ProviderEndpointSnapshot endpoint = route.getEndpoint();

        WebClient client = webClientBuilder
                .baseUrl(endpoint.getBaseUrl())
                .build();

        return client.post()
                .uri(buildUri(endpoint, route.getUpstreamProtocol(), UpstreamOperation.CHAT_COMPLETIONS))
                .headers(h -> addAuthHeader(h, endpoint))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(convertToUpstream(request, route))
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnError(e -> log.error("Stream error for request {}: {}", request.getRequestId(), e.getMessage()));
    }

    private String buildUri(ProviderEndpointSnapshot endpoint, ProtocolType protocol, UpstreamOperation op) {
        String prefix = endpoint.getPathPrefix() != null ? endpoint.getPathPrefix() : "";
        return "/" + switch (protocol) {
            case OPENAI -> "v1/chat/completions";
            case ANTHROPIC -> "v1/messages";
            default -> "v1/chat/completions";
        };
    }

    private void addAuthHeader(org.springframework.http.HttpHeaders headers, ProviderEndpointSnapshot endpoint) {
        if (!endpoint.getApiKeyEncrypted().startsWith("<")) {
            headers.set("Authorization", "Bearer " + endpoint.getApiKeyEncrypted());
        }
        if (endpoint.getExtraHeaders() != null) {
            endpoint.getExtraHeaders().forEach(headers::set);
        }
    }

    private Object convertToUpstream(InternalChatRequest request, ResolvedModelRoute route) {
        // Build upstream request based on protocol
        if (route.getUpstreamProtocol() == ProtocolType.ANTHROPIC) {
            return buildAnthropicUpstreamRequest(request, route);
        }
        return buildOpenAIUpstreamRequest(request, route);
    }

    private Map<String, Object> buildOpenAIUpstreamRequest(InternalChatRequest request, ResolvedModelRoute route) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("model", route.getUpstreamModel());
        body.put("messages", request.getMessages());
        body.put("stream", request.isStream());
        if (request.getTemperature() != null) body.put("temperature", request.getTemperature());
        if (request.getMaxTokens() != null) body.put("max_tokens", request.getMaxTokens());
        return body;
    }

    private Map<String, Object> buildAnthropicUpstreamRequest(InternalChatRequest request, ResolvedModelRoute route) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("model", route.getUpstreamModel());
        body.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 4096);
        body.put("messages", request.getMessages());
        body.put("stream", request.isStream());
        if (request.getSystem() != null) body.put("system", request.getSystem());
        if (request.getTemperature() != null) body.put("temperature", request.getTemperature());
        return body;
    }

    private InternalChatResponse parseUpstreamResponse(Map<String, Object> body, ResolvedModelRoute route, Instant start) {
        long latencyMs = Duration.between(start, Instant.now()).toMillis();

        InternalChatResponse.InternalChatResponseBuilder builder = InternalChatResponse.builder()
            .id(body.get("id") != null ? body.get("id").toString() : "")
            .model(body.get("model") != null ? body.get("model").toString() : route.getUpstreamModel())
            .latencyMs(latencyMs);

        // Parse usage
        if (body.containsKey("usage")) {
            Map<String, Object> usage = (Map<String, Object>) body.get("usage");
            builder.usage(InternalUsage.builder()
                .inputTokens(toLong(usage.get("prompt_tokens")))
                .outputTokens(toLong(usage.get("completion_tokens")))
                .totalTokens(toLong(usage.get("total_tokens")))
                .build());
        }

        return builder.build();
    }

    private long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        return 0;
    }

    public static class UpstreamException extends RuntimeException {
        private final int statusCode;
        public UpstreamException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }
        public int getStatusCode() { return statusCode; }
    }
}
